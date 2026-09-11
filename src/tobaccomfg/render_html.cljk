(ns tobaccomfg.render-html
  "Build-time HTML renderer for `docs/samples/operator-console.html`.

  Closes flagship checklist item 2 (com-junkawasaki/root ADR-2607189300).
  This repo previously had NO operator-console demo and no generator —
  only a product-face `docs/index.html` (DADS marketing surface), which
  is NOT a governor-driven demo. This namespace drives the REAL actor
  stack (`tobaccomfg.governor` + `tobaccomfg.operation` +
  `tobaccomfg.store`) through a scenario built from this repo's own
  test/sim batch shapes and renders the result deterministically —
  no invented numbers, no timestamps in the page content,
  byte-identical across reruns against the same seed.

  Architectural note (honest, not papered over): this vertical is the
  older `run-operation` / direct-`governor/check` shape (template edn
  §\"~26 run-operation repos\"), NOT the 290-repo langgraph StateGraph
  cluster. There is no `op/build` / `g/run*` here — `tobaccomfg.operation`
  is a pure dispatch layer and `tobaccomfg.advisor` is still a stub.
  REAL means every cell traces to a real `governor/check` verdict and/or
  a real `store` mutation after the scenario ran, not that a StateGraph
  was present. Extending this to a full langgraph OperationActor is a
  separate maturity step (README still correctly claims `:blueprint`
  maturity for the LLM advisor half).

  Usage: `clojure -M:dev:render-html [out-file]`
  (default `docs/samples/operator-console.html`)."
  (:require [jp-go-dds.skin]
            [kotoba.lang.text :as str]
            [tobaccomfg.store :as store]
            [tobaccomfg.governor :as governor]
            [tobaccomfg.operation :as operation]))

;; ----------------------------- seed (real field shapes from
;; tobaccomfg.sim / tobaccomfg.governor-test — keywords for evidence
;; checklist match facts/required-evidence, NOT the string form that
;; sim.cljc still carries as a latent bug) -----------------------------

(def ^:private jp-evidence
  "Exact keyword set required by `facts/jurisdictions` \"JP\"
  `:required-evidence` — ground truth from facts.cljc, not invented."
  [:batch-manifest :ingredient-declaration :packaging-label-draft
   :facility-registration :production-log :shipment-authorization])

(def ^:private demo-batches
  "Seed directory. Every field is one this governor actually reads
  (`get-batch` -> facility-permit / evidence-checklist / product-id /
  batch-quantity / ingredients-list / compliance-concern-raised? /
  production-logged? / shipment-finalized? / jurisdiction)."
  {"batch-1"
   {:product-id "cigarettes-12mg"
    :product-type "cigarettes"
    :batch-quantity 50000
    :jurisdiction "JP"
    :facility-permit-valid? true
    :ingredients-list ["tobacco-leaf" "paper" "water"]
    :evidence-checklist jp-evidence
    :compliance-concern-raised? false}

   "batch-2"
   {:product-id "cigarettes-12mg"
    :product-type "cigarettes"
    :batch-quantity 20000
    :jurisdiction "JP"
    :facility-permit-valid? false
    :ingredients-list ["tobacco-leaf" "paper"]
    :evidence-checklist jp-evidence
    :compliance-concern-raised? false}

   "batch-3"
   {:product-id "cigars-premium"
    :product-type "cigars"
    :batch-quantity 5000
    :jurisdiction "JP"
    :facility-permit-valid? true
    :ingredients-list ["tobacco-leaf"]
    ;; deliberately incomplete — missing packaging-label-draft etc.
    :evidence-checklist [:batch-manifest :ingredient-declaration]
    :compliance-concern-raised? false}

   "batch-4"
   {:product-id "pipe-tobacco-blend"
    :product-type "pipe-tobacco"
    :batch-quantity 1000
    :jurisdiction "JP"
    :facility-permit-valid? true
    :ingredients-list ["tobacco-leaf" "casing"]
    :evidence-checklist jp-evidence
    :compliance-concern-raised? true
    :compliance-concern-resolved? false}

   "batch-5"
   {;; incomplete manifest: no product-id, no ingredients-list
    :batch-quantity 100
    :jurisdiction "JP"
    :facility-permit-valid? true
    :evidence-checklist jp-evidence
    :compliance-concern-raised? false}})

(def ^:private operator
  {:actor-id "op-1" :actor-role :plant-operations-coordinator})

(defn- propose!
  "Drive one request through the REAL `governor/check`. On hard hold,
  write `governor/hold-fact` to the audit ledger. On escalate, write
  `:approval-requested`, execute `operation/run-operation` (the real
  dispatch layer — human sign-off), then write `:approval-granted`.
  On ok (auto-commit path), run-operation + write `:committed`.
  Returns a map describing the real outcome — no field invented."
  [st request proposal]
  (let [verdict (governor/check request operator proposal st)
        subject (:subject request)
        op (:op request)]
    (cond
      (:hard? verdict)
      (do (store/append-audit! st (governor/hold-fact request operator verdict))
          {:outcome :hard-hold :verdict verdict :subject subject :op op})

      (:escalate? verdict)
      (do (store/append-audit!
           st
           {:t :approval-requested
            :op op
            :actor (:actor-id operator)
            :subject subject
            :disposition :escalate
            :basis (if (:high-stakes? verdict) [:actuation] [:low-confidence])
            :confidence (:confidence verdict)})
          (operation/run-operation st subject op
                                   {:proposal proposal :approved-by "op-1"})
          (store/append-audit!
           st
           {:t :approval-granted
            :op op
            :actor "op-1"
            :subject subject
            :disposition :commit
            :basis (or (:cites proposal) [])})
          {:outcome :approved-and-committed :verdict verdict
           :subject subject :op op})

      (:ok? verdict)
      (do (operation/run-operation st subject op {:proposal proposal})
          (store/append-audit!
           st
           {:t :committed
            :op op
            :actor (:actor-id operator)
            :subject subject
            :disposition :commit
            :basis (or (:cites proposal) [])})
          {:outcome :auto-committed :verdict verdict
           :subject subject :op op})

      :else
      (do (store/append-audit! st (governor/hold-fact request operator verdict))
          {:outcome :hold :verdict verdict :subject subject :op op}))))

(defn run-demo!
  "Runs a freshly seeded store through a scenario mixing every disposition
  this actor's governor can actually reach via `governor/check` +
  `operation/run-operation`:

  Clean / escalate paths (batch-1, fully JP-compliant seed):
    - `:log-production-batch` is high-stakes → ALWAYS escalates when
      clean (governor `high-stakes`) → human approves → committed.
    - `:schedule-maintenance` is NOT high-stakes and has no hard checks
      → auto-commits at high confidence (real code path, not the README's
      aspirational 'always escalate' prose for maintenance).
    - `:coordinate-shipment` is high-stakes → escalates → human approves.

  HARD-hold paths (never reach a human — each a DISTINCT real rule from
  `tobaccomfg.governor`):
    - batch-2 log → `:facility-permit-invalid`
    - batch-3 log → `:evidence-incomplete`
    - batch-4 log → `:compliance-concern-unresolved`
    - batch-5 log → `:batch-manifest-incomplete`
    - batch-1 re-log after commit → `:already-logged`
    - batch-1 re-ship after finalize → `:shipment-already-finalized`
    - empty-cites proposal on batch-1 flag → `:no-spec-basis`

  Returns the resulting store — every field `render` reads is real
  governor/store output."
  []
  (let [st (store/mem-store {:initial-batches demo-batches})
        clean-proposal (fn [op]
                         {:cites ["JT-1200-001"]
                          :value {:jurisdiction "JP"}
                          :confidence 0.85
                          :stake op
                          :summary (str "demo " (name op))})]

    ;; batch-1 clean escalate→approve for production log
    (propose! st
              {:op :log-production-batch :subject "batch-1"
               :stake :log-production-batch}
              (clean-proposal :log-production-batch))

    ;; schedule-maintenance auto-commits (not high-stakes)
    (propose! st
              {:op :schedule-maintenance :subject "batch-1"
               :stake :schedule-maintenance}
              (clean-proposal :schedule-maintenance))

    ;; shipment escalate→approve
    (propose! st
              {:op :coordinate-shipment :subject "batch-1"
               :stake :coordinate-shipment}
              (clean-proposal :coordinate-shipment))

    ;; HARD: facility permit invalid
    (propose! st
              {:op :log-production-batch :subject "batch-2"
               :stake :log-production-batch}
              (clean-proposal :log-production-batch))

    ;; HARD: evidence incomplete
    (propose! st
              {:op :log-production-batch :subject "batch-3"
               :stake :log-production-batch}
              (clean-proposal :log-production-batch))

    ;; HARD: compliance concern unresolved
    (propose! st
              {:op :log-production-batch :subject "batch-4"
               :stake :log-production-batch}
              (clean-proposal :log-production-batch))

    ;; HARD: batch manifest incomplete
    (propose! st
              {:op :log-production-batch :subject "batch-5"
               :stake :log-production-batch}
              (clean-proposal :log-production-batch))

    ;; HARD: already logged (batch-1 was committed above)
    (propose! st
              {:op :log-production-batch :subject "batch-1"
               :stake :log-production-batch}
              (clean-proposal :log-production-batch))

    ;; HARD: shipment already finalized
    (propose! st
              {:op :coordinate-shipment :subject "batch-1"
               :stake :coordinate-shipment}
              (clean-proposal :coordinate-shipment))

    ;; HARD: no-spec-basis (empty cites on a cites-required op)
    (propose! st
              {:op :flag-compliance-concern :subject "batch-1"
               :stake :flag-compliance-concern}
              {:cites []
               :value {}
               :confidence 0.9
               :stake :flag-compliance-concern})
    st))

;; ----------------------------- rendering -----------------------------

(defn- esc [v]
  (-> (str v)
      (str/replace "&" "&amp;")
      (str/replace "<" "&lt;")
      (str/replace ">" "&gt;")))

(defn- decision-facts
  "Only governor/decision facts — exclude `:operation-*` dispatch facts
  so status-cell reflects the governance outcome, not the post-approval
  dispatch log line."
  [ledger]
  (filter (comp #{:committed :approval-granted :approval-requested
                  :governor-hold}
                :t)
          ledger))

(defn- last-fact-for [ledger subject-id]
  (last (filter #(= (:subject %) subject-id) (decision-facts ledger))))

(defn- status-cell [ledger subject-id]
  (let [f (last-fact-for ledger subject-id)]
    (cond
      (nil? f) "<span class=\"muted\">no activity</span>"
      (= :committed (:t f)) "<span class=\"ok\">committed</span>"
      (= :approval-granted (:t f)) "<span class=\"ok\">approved &amp; committed</span>"
      (= :governor-hold (:t f))
      (let [rule (or (-> f :basis first)
                     (-> f :violations first :rule))]
        (str "<span class=\"critical\">HARD hold &middot; "
             (esc (name (or rule :unknown))) "</span>"))
      (= :approval-requested (:t f)) "<span class=\"warn\">awaiting approval</span>"
      :else "<span class=\"muted\">in progress</span>")))

(defn- batch-row [ledger {:keys [id product-id product-type batch-quantity
                                  jurisdiction facility-permit-valid?
                                  production-logged? shipment-finalized?
                                  compliance-concern-raised?
                                  compliance-concern-resolved?
                                  evidence-checklist ingredients-list]}]
  (format "        <tr><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td></tr>"
          (esc id)
          (esc (or product-id "—"))
          (esc (or product-type "—"))
          (esc (or batch-quantity "—"))
          (esc (or jurisdiction "—"))
          (if facility-permit-valid?
            "<span class=\"ok\">valid</span>"
            "<span class=\"err\">invalid</span>")
          (esc (count evidence-checklist))
          (if production-logged?
            "<span class=\"ok\">yes</span>"
            "<span class=\"muted\">no</span>")
          (cond
            (and compliance-concern-raised? (not compliance-concern-resolved?))
            "<span class=\"critical\">open</span>"
            compliance-concern-raised?
            "<span class=\"ok\">resolved</span>"
            :else "<span class=\"muted\">none</span>")
          (status-cell ledger id)))

(defn- ledger-row [{:keys [t op subject disposition basis violations]}]
  (format "        <tr><td>%s</td><td><code>%s</code></td><td>%s</td><td>%s</td></tr>"
          (esc (name (or t :n-a)))
          (esc (name (or op :n-a)))
          (esc (or subject ""))
          (esc (or (some->> basis (map #(if (keyword? %) (name %) (str %)))
                            (str/join ", "))
                   (some->> violations (map :rule) (map name) (str/join ", "))
                   (some-> disposition name)
                   ""))))

(def ^:private action-gate-rows
  ;; Static description of this actor's own op contract (README Ops +
  ;; governor.cljc as implemented) — documentation of fixed behavior,
  ;; not runtime telemetry.
  ["        <tr><td><code>:log-production-batch</code></td><td><span class=\"warn\">ALWAYS human approval when clean (high-stakes) &middot; HARD hold on evidence/permit/manifest/compliance/already-logged</span></td></tr>"
   "        <tr><td><code>:schedule-maintenance</code></td><td><span class=\"ok\">auto-commit when clean (not high-stakes; no hard equipment checks in this vertical)</span></td></tr>"
   "        <tr><td><code>:flag-compliance-concern</code></td><td><span class=\"warn\">requires jurisdiction citation &middot; HARD hold on :no-spec-basis</span></td></tr>"
   "        <tr><td><code>:coordinate-shipment</code></td><td><span class=\"warn\">ALWAYS human approval when clean (high-stakes) &middot; HARD hold on :shipment-already-finalized</span></td></tr>"])

(defn render
  "Renders the full operator-console.html document from a store `db`
  that has already run `run-demo!` (or any other real scenario)."
  [db]
  (let [ledger (vec (store/ledger db))
        batches (store/all-batches db)
        batch-rows (str/join "\n" (map (partial batch-row ledger) batches))
        ;; show decision facts first for readability, then dispatch facts
        shown-ledger (concat (decision-facts ledger)
                             (remove (comp #{:committed :approval-granted
                                             :approval-requested :governor-hold}
                                           :t)
                                     ledger))
        ledger-rows (str/join "\n" (map ledger-row shown-ledger))]
    (str
     "<html><head><meta charset=\"utf-8\"><title>cloud-itonami-isic-1200 &middot; tobacco products manufacturing</title><style>"
     (jp-go-dds.skin/dds+skin)
     "</style></head><body>\n"
     "<header class=\"bar\">\n"
     "  <h1>Tobacco products manufacturing (ISIC 1200) — Operator Console</h1>\n"
     "  <span class=\"badge\">read-only sample · governor-gated · processing-line actuation permanently out of scope · health-warning / excise certification always human</span>\n"
     "</header>\n"
     "<main>\n"
     "  <section class=\"card\">\n"
     "    <h2>Production batches</h2>\n"
     "    <p class=\"muted\">Demo snapshot — build-time-generated from <code>tobaccomfg.store</code> via <code>tobaccomfg.render-html</code> (<code>clojure -M:dev:render-html</code>), regenerated from the real governor. Permit / evidence / logged columns are store ground truth the governor independently re-derives — never trusted from a proposal's own report. Timestamps written by <code>operation/run-operation</code> are intentionally omitted from this page for determinism.</p>\n"
     "    <table>\n"
     "      <thead><tr><th>Batch</th><th>Product</th><th>Type</th><th>Qty</th><th>Jurisdiction</th><th>Facility permit</th><th>Evidence items</th><th>Logged</th><th>Compliance flag</th><th>Last decision</th></tr></thead>\n"
     "      <tbody>\n"
     batch-rows "\n"
     "      </tbody>\n"
     "    </table>\n"
     "  </section>\n"
     "  <section class=\"card\">\n"
     "    <h2>Action gate (Tobacco Manufacturing Governor)</h2>\n"
     "    <p class=\"muted\">HARD holds cannot be overridden by any human approval. Processing-line equipment operation, health-warning certification, and excise-tax sign-off are permanently out of scope for this actor.</p>\n"
     "    <table>\n"
     "      <thead><tr><th>Op</th><th>Gate</th></tr></thead>\n"
     "      <tbody>\n"
     (str/join "\n" action-gate-rows) "\n"
     "      </tbody>\n"
     "    </table>\n"
     "  </section>\n"
     "  <section class=\"card\">\n"
     "    <h2>Audit ledger (this run)</h2>\n"
     "    <p class=\"muted\">Append-only decision-fact log — every proposal, hold, approval and commit this scenario produced, plus the dispatch facts <code>operation/run-operation</code> wrote.</p>\n"
     "    <table>\n"
     "      <thead><tr><th>Fact</th><th>Op</th><th>Subject</th><th>Basis</th></tr></thead>\n"
     "      <tbody>\n"
     ledger-rows "\n"
     "      </tbody>\n"
     "    </table>\n"
     "  </section>\n"
     "</main>\n"
     "</body></html>\n")))

(defn -main [& args]
  (let [out (or (first args) "docs/samples/operator-console.html")
        db (run-demo!)
        html (render db)
        out-file (java.io.File. out)]
    (.. out-file getParentFile mkdirs)
    (spit out-file html)
    (println "wrote" out "(" (count (store/ledger db)) "ledger facts,"
             (count (store/all-batches db)) "batches )")))
