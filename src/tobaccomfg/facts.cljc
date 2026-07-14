(ns tobaccomfg.facts
  "Reference facts for tobacco manufacturing: jurisdiction requirements,
  regulatory compliance checkpoints, product categories, and excise-tax
  tracking. This namespace contains pure lookup functions for regulatory
  compliance checks -- the Governor calls these to validate proposals against
  jurisdiction requirements.

  CRITICAL: This actor NEVER certifies health warnings, excise tax compliance,
  or marketing claims. Those require licensed compliance officer sign-off.
  This namespace supports coordination only."
  (:require [clojure.string :as str]))

(def jurisdictions
  "Tobacco manufacturing jurisdictions and their regulatory/excise-tax
  documentation requirements."
  {"US"
   {:id "US"
    :name "United States (FDA/TTB)"
    :excise-tax-applicability true
    :required-evidence
    [:batch-manifest           ;; product batch identification
     :ingredient-declaration   ;; tobacco/additive ingredients
     :packaging-label-draft    ;; label design for review
     :facility-registration    ;; manufacturing facility permit
     :production-log           ;; batch production record
     :shipment-authorization]} ;; TTB export/domestic routing

   "JP"
   {:id "JP"
    :name "日本 (JT/財務省)"
    :excise-tax-applicability true
    :required-evidence
    [:batch-manifest
     :ingredient-declaration
     :packaging-label-draft
     :facility-registration
     :production-log
     :shipment-authorization]}

   "EU"
   {:id "EU"
    :name "European Union (EMSA/TPDA)"
    :excise-tax-applicability true
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

(defn excise-tax-applicable? [jurisdiction-id]
  "Check if jurisdiction applies excise tax on tobacco products."
  (when-let [j (jurisdiction-by-id jurisdiction-id)]
    (:excise-tax-applicability j false)))
