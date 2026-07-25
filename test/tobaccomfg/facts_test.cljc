(ns tobaccomfg.facts-test
  (:require [clojure.test :refer [deftest is testing]]
            [tobaccomfg.facts :as facts]))

(deftest jurisdiction-by-id
  (testing "US jurisdiction -> found"
    (is (facts/jurisdiction-by-id "US")))

  (testing "JP jurisdiction -> found"
    (is (facts/jurisdiction-by-id "JP")))

  (testing "EU jurisdiction -> found"
    (is (facts/jurisdiction-by-id "EU")))

  (testing "Unknown jurisdiction -> nil"
    (is (nil? (facts/jurisdiction-by-id "UNKNOWN")))))

(deftest required-evidence-satisfied?
  (testing "Complete evidence checklist -> true"
    (is (facts/required-evidence-satisfied?
         "US"
         [:batch-manifest :ingredient-declaration :packaging-label-draft
          :facility-registration :production-log :shipment-authorization])))

  (testing "Missing one required item -> false"
    (is (not (facts/required-evidence-satisfied?
              "US"
              [:batch-manifest :ingredient-declaration :packaging-label-draft
               :facility-registration :production-log]))))

  (testing "Unknown jurisdiction -> false"
    (is (not (facts/required-evidence-satisfied?
              "UNKNOWN"
              [:batch-manifest :ingredient-declaration])))))

(deftest product-category-by-id
  (testing "Cigarettes -> found"
    (let [p (facts/product-category-by-id "cigarettes")]
      (is p)
      (is (:requires-excise-filing p))))

  (testing "Cigars -> found"
    (is (facts/product-category-by-id "cigars")))

  (testing "Smokeless -> found"
    (is (facts/product-category-by-id "smokeless")))

  (testing "Unknown product -> nil"
    (is (nil? (facts/product-category-by-id "unknown")))))

(deftest excise-tax-applicable?
  (testing "US excise tax -> true"
    (is (facts/excise-tax-applicable? "US")))

  (testing "JP excise tax -> true"
    (is (facts/excise-tax-applicable? "JP")))

  (testing "EU excise tax -> true"
    (is (facts/excise-tax-applicable? "EU")))

  (testing "Unknown jurisdiction -> false"
    (is (not (facts/excise-tax-applicable? "UNKNOWN")))))

;; ───────── Verified primary-source citations (2026-07-25) ─────────

(deftest every-jurisdiction-rests-on-a-fetched-primary-source
  (doseq [id (keys facts/jurisdictions)]
    (is (facts/cited? id)
        (str id " must carry a legal-basis, an http(s) provenance URL and verbatim text")))
  (is (nil? (facts/spec-basis "XX")))
  (is (false? (facts/cited? "XX"))))

(deftest citation-coverage-is-honest
  (let [c (facts/citation-coverage)]
    (is (= 3 (:jurisdictions c)))
    (is (= 3 (:cited c)))
    (is (= ["EU" "JP" "US"] (:cited-jurisdictions c)))
    (is (= [] (:uncited-jurisdictions c)))))

(deftest eu-authority-is-not-the-maritime-safety-agency
  (testing "EMSA is the European Maritime Safety Agency, not a tobacco regulator"
    (let [eu (facts/jurisdiction-by-id "EU")]
      (is (nil? (re-find #"EMSA|TPDA" (:name eu)))
          "the previous name cited two bodies with no tobacco mandate")
      (is (re-find #"2014/40/EU" (:name eu)))
      (is (re-find #"32014L0040" (:provenance eu))))))

(deftest jp-authority-is-the-ministry-not-the-manufacturer
  (testing "JT (Japan Tobacco) is a manufacturer; the mandate is 財務省's"
    (let [jp (facts/jurisdiction-by-id "JP")]
      (is (nil? (re-find #"JT" (:name jp)))
          "naming a private company as the jurisdiction authority inverts who regulates whom")
      (is (re-find #"財務省" (:name jp)))
      (is (re-find #"359AC0000000068" (:provenance jp))))))

(deftest eu-tpd-numbers-match-the-directive
  (let [limits (:statutory-limits (facts/jurisdiction-by-id "EU"))]
    (is (= 65.0 (:combined-warning-min-coverage-pct limits)) "Art. 10(1)(c)")
    (is (= "2019-05-20" (:traceability-cigarettes-from limits)) "Art. 15(13)")
    (is (= "2024-05-20" (:traceability-other-products-from limits)) "Art. 15(13)")
    (is (true? (:security-feature-required? limits)) "Art. 16(1)")))

(deftest track-and-trace-across-all-categories-is-grounded
  (testing "Art. 15(13) extended traceability to non-cigarette products from 2024-05-20"
    ;; This is WHY :track-and-trace is true for cigars/pipe/smokeless too,
    ;; rather than cigarettes-only -- the phase-in has already elapsed.
    (is (= "2024-05-20"
           (-> (facts/jurisdiction-by-id "EU") :statutory-limits
               :traceability-other-products-from)))
    (doseq [id ["cigarettes" "cigars" "pipe-tobacco" "smokeless"]]
      (is (true? (:track-and-trace (facts/product-category-by-id id)))
          (str id " is in scope of Art. 15 traceability as of today")))))

(deftest jp-warning-wording-is-delegated-not-asserted
  (testing "第39条 delegates the warning text to 財務省令, so this actor must not state it"
    (let [limits (:statutory-limits (facts/jurisdiction-by-id "JP"))]
      (is (= :財務省令 (:warning-text-set-by limits)))
      (is (false? (:warning-text-asserted-here? limits))))))

(deftest excise-tax-docstring-is-attached
  (testing "the docstring sat after the arg vector and was silently discarded"
    (is (re-find #"excise tax"
                 (:doc (meta #'facts/excise-tax-applicable?)))
        "a misplaced docstring is a discarded string literal, not documentation"))
  (testing "behaviour unchanged"
    (is (true? (facts/excise-tax-applicable? "US")))
    (is (nil? (facts/excise-tax-applicable? "XX")))))
