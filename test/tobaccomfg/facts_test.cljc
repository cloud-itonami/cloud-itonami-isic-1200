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
