(ns tobaccomfg.facts
  "Reference facts for tobacco manufacturing: jurisdiction requirements,
  regulatory compliance checkpoints, product categories, and excise-tax
  tracking. This namespace contains pure lookup functions for regulatory
  compliance checks -- the Governor calls these to validate proposals against
  jurisdiction requirements.

  CRITICAL: This actor NEVER certifies health warnings, excise tax compliance,
  or marketing claims. Those require licensed compliance officer sign-off.
  This namespace supports coordination only.

  CITATION PROVENANCE (2026-07-25). Every `:legal-basis` / `:provenance` /
  `:verbatim` below was read out of a directly-fetched official primary
  source and re-grepped against the raw markup -- not recalled, and not
  taken from a fetch summary.

  CORRECTIONS -- TWO MISATTRIBUTED AUTHORITIES. Both jurisdiction names were
  wrong, and wrong in a way that pointed at bodies with no tobacco mandate:

    1. EU was `\"European Union (EMSA/TPDA)\"`. **EMSA is the European
       Maritime Safety Agency** -- nothing to do with tobacco -- and \"TPDA\"
       is not a body at all. The instrument is Directive 2014/40/EU (the
       Tobacco Products Directive), administered by the European
       Commission. Corrected.
    2. JP was `\"日本 (JT/財務省)\"`. **JT (Japan Tobacco) is a manufacturer,
       not a regulator** -- naming a private company as the jurisdiction
       authority inverts who regulates whom. たばこ事業法 puts the mandate on
       財務大臣/財務省 (the warning wording itself is set by 財務省令).
       Corrected to 財務省.

  Sources:

    - EU: Directive 2014/40/EU via EUR-Lex (CELEX:32014L0040). Art. 10(1)(c)
      requires combined health warnings that `\"cover 65 % of both the
      external front and back surface of the unit packet and any outside
      packaging\"`. Art. 15(13) phases traceability in: `\"Paragraphs 1 to 10
      shall apply to cigarettes and roll-your-own tobacco from 20 May 2019
      and to tobacco products other than cigarettes and roll-your-own
      tobacco from 20 May 2024.\"` -- so as of today every product category
      here is in scope, which is why `:track-and-trace` is true across
      `product-categories` rather than cigarettes-only.
    - JP: たばこ事業法（昭和59年法律第68号、e-Gov law_id 359AC0000000068）第39条
      （注意表示）第1項:「会社又は特定販売業者は、製造たばこで財務省令で定めるものを
      販売の用に供するために製造し、又は輸入した場合には、当該製造たばこを販売する時
      までに、当該製造たばこに、消費者に対し製造たばこの消費と健康との関係に関して
      注意を促すための財務省令で定める文言を、財務省令で定めるところにより、表示
      しなければならない。」第40条 では財務大臣が広告指針を示しうる。
    - US: 27 CFR 40.61 (TTB, Department of the Treasury) via govinfo.gov
      official CFR XML: `\"every person who manufactures tobacco products
      must qualify for, and obtain, a permit as a manufacturer of tobacco
      products in accordance with the provisions of this part\"` -- the basis
      for this catalog's `:facility-registration` evidence item.

  DISCLOSED SCOPE LIMIT: these citations ground the DOCUMENTATION and
  authority facts this actor coordinates around. They are not, and must not
  be read as, certification of health-warning or excise compliance -- see
  the CRITICAL note above."
  (:require [kotoba.lang.text :as str]
            [clojure.set]))

(def jurisdictions
  "Tobacco manufacturing jurisdictions and their regulatory/excise-tax
  documentation requirements."
  {"US"
   {:id "US"
    :name "United States (FDA/TTB)"
    :excise-tax-applicability true
    :legal-basis "27 CFR 40.61 (TTB, Department of the Treasury) — every manufacturer of tobacco products must qualify for and obtain a manufacturer's permit; 27 CFR Part 40 covers the manufacture of tobacco products, cigarette papers and tubes, and processed tobacco"
    :provenance "https://www.govinfo.gov/content/pkg/CFR-2024-title27-vol2/xml/CFR-2024-title27-vol2-sec40-61.xml"
    :verbatim
    {:permit "Except as otherwise provided in paragraph (b) of this section, every person who manufactures tobacco products must qualify for, and obtain, a permit as a manufacturer of tobacco products in accordance with the provisions of this part."
     :part-scope "This part contains regulations relating to the manufacture of tobacco products, cigarette papers and tubes, and processed tobacco"}
    :required-evidence
    [:batch-manifest           ;; product batch identification
     :ingredient-declaration   ;; tobacco/additive ingredients
     :packaging-label-draft    ;; label design for review
     :facility-registration    ;; manufacturing facility permit
     :production-log           ;; batch production record
     :shipment-authorization]} ;; TTB export/domestic routing

   "JP"
   {:id "JP"
    ;; Corrected 2026-07-25: was "日本 (JT/財務省)". JT (Japan Tobacco) is a
    ;; manufacturer, not a regulator -- the mandate is 財務大臣/財務省's.
    :name "日本（財務省／たばこ事業法）"
    :excise-tax-applicability true
    :legal-basis "たばこ事業法（昭和59年法律第68号）第39条（注意表示）— 会社又は特定販売業者は、財務省令で定める注意文言を財務省令で定めるところにより表示しなければならない。第40条 — 財務大臣は製造たばこ広告の指針を示し、従わない者に勧告できる"
    :provenance "https://laws.e-gov.go.jp/api/2/law_data/359AC0000000068"
    :verbatim
    {:warning-label "会社又は特定販売業者は、製造たばこで財務省令で定めるものを販売の用に供するために製造し、又は輸入した場合には、当該製造たばこを販売する時までに、当該製造たばこに、消費者に対し製造たばこの消費と健康との関係に関して注意を促すための財務省令で定める文言を、財務省令で定めるところにより、表示しなければならない。"
     :advertising "財務大臣は、前項の規定の趣旨に照らして必要があると認める場合には、あらかじめ、財政制度等審議会の意見を聴いて、製造たばこに係る広告を行う者に対し、当該広告を行う際の指針を示すことができる。"}
    :statutory-limits
    ;; The warning WORDING is delegated to 財務省令, so this actor must not
    ;; assert a specific text -- only that the duty exists and who sets it.
    {:warning-text-set-by :財務省令
     :warning-text-asserted-here? false}
    :required-evidence
    [:batch-manifest
     :ingredient-declaration
     :packaging-label-draft
     :facility-registration
     :production-log
     :shipment-authorization]}

   "EU"
   {:id "EU"
    ;; Corrected 2026-07-25: was "European Union (EMSA/TPDA)". EMSA is the
    ;; European Maritime Safety Agency; "TPDA" is not a body. The instrument
    ;; is Directive 2014/40/EU, administered by the European Commission.
    :name "European Union (European Commission / Directive 2014/40/EU)"
    :excise-tax-applicability true
    :legal-basis "Directive 2014/40/EU (Tobacco Products Directive) Art. 10(1)(c) — combined health warnings must cover 65 % of both the external front and back surface of the unit packet and any outside packaging; Art. 15 — unique identifier traceability, Art. 16 — security feature"
    :provenance "https://eur-lex.europa.eu/legal-content/EN/TXT/HTML/?uri=CELEX:32014L0040"
    :verbatim
    {:warning-coverage "cover 65 % of both the external front and back surface of the unit packet and any outside packaging"
     :traceability-phase-in "Paragraphs 1 to 10 shall apply to cigarettes and roll-your-own tobacco from 20 May 2019 and to tobacco products other than cigarettes and roll-your-own tobacco from 20 May 2024."}
    :statutory-limits
    {:combined-warning-min-coverage-pct 65.0        ; Art. 10(1)(c)
     :traceability-cigarettes-from "2019-05-20"     ; Art. 15(13)
     :traceability-other-products-from "2024-05-20" ; Art. 15(13)
     :security-feature-required? true}              ; Art. 16(1)
    :required-evidence
    [:batch-manifest
     :ingredient-declaration
     :packaging-label-draft
     :facility-registration
     :production-log
     :shipment-authorization
     :track-and-trace-id]}})

(defn jurisdiction-by-id [id]
  (get jurisdictions id))

(defn required-evidence-satisfied?
  "Verify that all required-evidence items are present in the batch's
  compliance checklist. Returns true only if every item in the jurisdiction's
  required-evidence list is present."
  [jurisdiction-id checklist]
  (let [j (jurisdiction-by-id jurisdiction-id)]
    (if-not j
      false
      (let [required (set (:required-evidence j))
            present (set checklist)]
        (clojure.set/subset? required present)))))

(def product-categories
  "Valid tobacco product categories for tracking and regulatory compliance."
  {"cigarettes"
   {:id "cigarettes"
    :name "タバコ巻紙"
    :requires-excise-filing true
    :requires-health-warning true
    :track-and-trace true}

   "cigars"
   {:id "cigars"
    :name "葉巻"
    :requires-excise-filing true
    :requires-health-warning true
    :track-and-trace true}

   "pipe-tobacco"
   {:id "pipe-tobacco"
    :name "パイプタバコ"
    :requires-excise-filing true
    :requires-health-warning true
    :track-and-trace true}

   "smokeless"
   {:id "smokeless"
    :name "無煙タバコ"
    :requires-excise-filing true
    :requires-health-warning true
    :track-and-trace true}})

(defn product-category-by-id [id]
  (get product-categories id))

(defn excise-tax-applicable?
  "Check if jurisdiction applies excise tax on tobacco products.

  The docstring used to sit AFTER the argument vector, which makes it a
  discarded string literal in the body rather than documentation -- the text
  was silently lost from `(doc excise-tax-applicable?)` and clj-kondo flagged
  it as both a misplaced docstring and an unused value. Fixed 2026-07-25."
  [jurisdiction-id]
  (when-let [j (jurisdiction-by-id jurisdiction-id)]
    (:excise-tax-applicability j false)))

(defn spec-basis
  "The verified primary-source citation for `jurisdiction-id`, or nil when
  the jurisdiction is unknown. Never synthesizes a citation."
  [jurisdiction-id]
  (when-let [j (jurisdiction-by-id jurisdiction-id)]
    (select-keys j [:legal-basis :provenance :verbatim :statutory-limits])))

(defn cited?
  "True only when `jurisdiction-id` carries a non-blank `:legal-basis`, a
  `:provenance` that is a real absolute http(s) URL, and at least one
  `:verbatim` quote. A named statute with no fetchable source is not cited."
  [jurisdiction-id]
  (let [{:keys [legal-basis provenance verbatim]} (spec-basis jurisdiction-id)]
    (boolean (and (string? legal-basis) (not (str/blank? legal-basis))
                  (string? provenance) (str/starts-with? provenance "http")
                  (map? verbatim) (seq verbatim)))))

(defn citation-coverage
  "Honest citation coverage over `jurisdictions`."
  []
  (let [ids (keys jurisdictions)
        cited (filter cited? ids)]
    {:jurisdictions (count jurisdictions)
     :cited (count cited)
     :cited-jurisdictions (vec (sort cited))
     :uncited-jurisdictions (vec (sort (remove cited? ids)))
     :note (str "cloud-itonami-isic-1200: " (count cited) "/" (count jurisdictions)
                " jurisdictions rest on a directly-fetched official source "
                "(govinfo.gov CFR XML, EUR-Lex, e-Gov law_data API). Two "
                "authority names were corrected from bodies with no tobacco "
                "mandate: EMSA is the European Maritime Safety Agency, and JT "
                "is a manufacturer rather than a regulator. These citations "
                "ground documentation/authority facts only -- this actor never "
                "certifies health-warning or excise compliance. Extend only "
                "from a real fetched source; never fabricate a legal-basis, "
                "provenance URL or quote.")}))
