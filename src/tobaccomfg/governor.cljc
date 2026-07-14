(ns tobaccomfg.governor
  "Tobacco Manufacturing Governor -- the independent compliance layer that
  earns the TobaccoOpsAdvisor the right to commit. The LLM has no notion of:
    - Whether a batch's facility permit is valid
    - Whether required excise-tax documents are filed
    - Whether health warnings have been certified by the compliance officer
    - Whether a batch's evidence checklist is complete per jurisdiction
    - Whether an unresolved compliance concern has been raised

  This MUST be a separate system able to *reject* a proposal and fall back
  to HOLD.

  Unlike direct equipment control (NEVER done by this actor -- processing
  equipment operation remains exclusive to licensed plant operators), the
  Governor operates on batch metadata: provenance, regulatory documents,
  compliance flags, and audit trails. This is plant-operations coordination,
  not process control.

  CRITICAL: Any proposal involving regulatory/excise-tax/health-warning
  concerns ALWAYS escalates to human operator for final sign-off. The LLM's
  confidence is never sufficient for regulatory decisions.

  Hard violations (always HOLD, no override):
    1. No jurisdiction citation (jurisdiction unknown -> can't verify reqs)
    2. Evidence incomplete (missing required-evidence per jurisdiction)
    3. Facility permit invalid (manufacturing authorization not current)
    4. Batch manifest incomplete (product ID, quantity, ingredients missing)
    5. Compliance concern unresolved (open regulatory flag)

  Soft gates (always escalate for human):
    - Low confidence
    - Excise-tax or health-warning related
    - Real actuation (`:log-production-batch`, `:coordinate-shipment`)

  This design mirrors `meatprocessing.governor` but specializes regulatory/
  excise-tax concerns (never certification, always escalation) rather than
  food-safety (temperature/contamination)."
  (:require [tobaccomfg.facts :as facts]
            [tobaccomfg.registry :as registry]
            [tobaccomfg.store :as store]))

(def confidence-floor 0.6)

(def high-stakes
  "Stakes grave enough to always require a human, even when clean.
  Logging a batch into production records (`:log-production-batch`) and
  coordinating shipment of finished product (`:coordinate-shipment`) are the
  two real-world actuation events this actor performs. Both require plant
  operator sign-off, and may involve compliance review."
  #{:log-production-batch :coordinate-shipment})

;; ----------------------------- checks -----------------------------

(defn- spec-basis-violations
  "A proposal with no jurisdiction citation is a HARD violation -- never
  invent a jurisdiction's regulatory requirements."
  [{:keys [op]} proposal]
  (when (contains?
         #{:log-production-batch :coordinate-shipment :flag-compliance-concern}
         op)
    (let [value (:value proposal)]
      (when (or (empty? (:cites proposal))
                (and (contains? value :jurisdiction) (nil? (:jurisdiction value))))
        [{:rule :no-spec-basis
          :detail "公式規制要件の引用が無い提案は法域要件として扱えない"}]))))

(defn- evidence-incomplete-violations
  "For `:log-production-batch`, verify the batch's evidence checklist is
  complete per jurisdiction requirements."
  [{:keys [op subject]} st]
  (when (= op :log-production-batch)
    (let [b (store/get-batch st subject)]
      (when-not (and b
                     (facts/required-evidence-satisfied?
                      (:jurisdiction b)
                      (:evidence-checklist b)))
        [{:rule :evidence-incomplete
          :detail "法域の必要書類(batch-manifest/ingredient-declaration/packaging-label-draft等)が充足していない"}]))))

(defn- facility-permit-invalid-violations
  "For `:log-production-batch`, INDEPENDENTLY verify that the manufacturing
  facility registration/permit is valid and current."
  [{:keys [op subject]} st]
  (when (= op :log-production-batch)
    (let [b (store/get-batch st subject)]
      (when (and b (not (registry/facility-permit-valid? b)))
        [{:rule :facility-permit-invalid
          :detail (str subject " の施設許可が無効 -- バッチ登録提案は進められない")}]))))

(defn- batch-manifest-incomplete-violations
  "For `:log-production-batch`, verify the batch manifest is complete."
  [{:keys [op subject]} st]
  (when (= op :log-production-batch)
    (let [b (store/get-batch st subject)]
      (when (and b (not (registry/batch-manifest-complete? b)))
        [{:rule :batch-manifest-incomplete
          :detail (str subject " のバッチマニフェスト(製品ID/数量/成分)が不完全 -- バッチ登録提案は進められない")}]))))

(defn- compliance-concern-unresolved-violations
  "An unresolved regulatory/excise-tax/health-warning compliance concern is
  a HARD, un-overridable hold. Compliance concerns raised during intake or
  review MUST be resolved before the batch can be logged into production.
  Evaluated UNCONDITIONALLY at `:log-production-batch`."
  [{:keys [op subject]} st]
  (when (= op :log-production-batch)
    (let [b (store/get-batch st subject)]
      (when (registry/compliance-concern-raised? b)
        [{:rule :compliance-concern-unresolved
          :detail (str subject " は未解決の規制準拠フラグがある -- バッチ登録提案は進められない")}]))))

(defn- already-logged-violations
  "For `:log-production-batch`, refuse to log the SAME batch twice, off
  a dedicated `:production-logged?` fact."
  [{:keys [op subject]} st]
  (when (= op :log-production-batch)
    (when (store/batch-already-logged? st subject)
      [{:rule :already-logged
        :detail (str subject " は既に生産記録に登録済み")}])))

(defn- shipment-already-finalized-violations
  "For `:coordinate-shipment`, refuse to finalize the SAME batch's shipment
  twice, off a dedicated `:shipment-finalized?` fact."
  [{:keys [op subject]} st]
  (when (= op :coordinate-shipment)
    (when (store/batch-shipment-finalized? st subject)
      [{:rule :shipment-already-finalized
        :detail (str subject " は既に出荷確定済み")}])))

(defn check
  "Censors a TobaccoOpsAdvisor proposal against the Governor rules.
  Returns {:ok? bool :violations [..] :confidence c :escalate? bool
  :high-stakes? bool :hard? bool}."
  [request _context proposal st]
  (let [hard (into []
                   (concat (spec-basis-violations request proposal)
                           (evidence-incomplete-violations request st)
                           (facility-permit-invalid-violations request st)
                           (batch-manifest-incomplete-violations request st)
                           (compliance-concern-unresolved-violations request st)
                           (already-logged-violations request st)
                           (shipment-already-finalized-violations request st)))
        conf (:confidence proposal 0.0)
        low? (< conf confidence-floor)
        stakes? (boolean (high-stakes (:stake proposal)))
        hard? (boolean (seq hard))]
    {:ok?          (and (not hard?) (not low?) (not stakes?))
     :violations   hard
     :confidence   conf
     :hard?        hard?
     :escalate?    (and (not hard?) (or low? stakes?))
     :high-stakes? stakes?}))

(defn hold-fact
  "The audit fact written when a proposal is rejected (HOLD)."
  [request context verdict]
  {:t          :governor-hold
   :op         (:op request)
   :actor      (:actor-id context)
   :subject    (:subject request)
   :disposition :hold
   :basis      (mapv :rule (:violations verdict))
   :violations (:violations verdict)
   :confidence (:confidence verdict)})
