(ns tobaccomfg.registry-test
  (:require [clojure.test :refer [deftest is testing]]
            [tobaccomfg.registry :as registry]))

(deftest batch-already-logged?
  (testing "Not logged -> false"
    (is (not (registry/batch-already-logged? {:production-logged? false}))))

  (testing "Logged -> true"
    (is (registry/batch-already-logged? {:production-logged? true}))))

(deftest shipment-already-finalized?
  (testing "Not finalized -> false"
    (is (not (registry/shipment-already-finalized? {:shipment-finalized? false}))))

  (testing "Finalized -> true"
    (is (registry/shipment-already-finalized? {:shipment-finalized? true}))))

(deftest compliance-concern-raised?
  (testing "No concern raised -> false"
    (is (not (registry/compliance-concern-raised?
              {:compliance-concern-raised? false}))))

  (testing "Concern raised but resolved -> false"
    (is (not (registry/compliance-concern-raised?
              {:compliance-concern-raised? true :compliance-concern-resolved? true}))))

  (testing "Concern raised and unresolved -> true"
    (is (registry/compliance-concern-raised?
         {:compliance-concern-raised? true :compliance-concern-resolved? false}))))

(deftest facility-permit-valid?
  (testing "Valid permit -> true"
    (is (registry/facility-permit-valid? {:facility-permit-valid? true})))

  (testing "Invalid permit -> false"
    (is (not (registry/facility-permit-valid? {:facility-permit-valid? false}))))

  (testing "Default (no permit key) -> true"
    (is (registry/facility-permit-valid? {}))))

(deftest batch-manifest-complete?
  (testing "Complete manifest -> true"
    (is (registry/batch-manifest-complete?
         {:product-id "cigarettes-12mg"
          :batch-quantity 50000
          :ingredients-list ["tobacco" "paper"]})))

  (testing "Missing product-id -> false"
    (is (not (registry/batch-manifest-complete?
              {:batch-quantity 50000
               :ingredients-list ["tobacco" "paper"]}))))

  (testing "Missing batch-quantity -> false"
    (is (not (registry/batch-manifest-complete?
              {:product-id "cigarettes-12mg"
               :ingredients-list ["tobacco" "paper"]}))))

  (testing "Missing ingredients-list -> false"
    (is (not (registry/batch-manifest-complete?
              {:product-id "cigarettes-12mg"
               :batch-quantity 50000})))))

(deftest health-warning-required?
  (testing "Cigarettes -> true"
    (is (registry/health-warning-required? "cigarettes")))

  (testing "Cigars -> true"
    (is (registry/health-warning-required? "cigars")))

  (testing "Pipe tobacco -> true"
    (is (registry/health-warning-required? "pipe-tobacco")))

  (testing "Unknown product -> false"
    (is (not (registry/health-warning-required? "unknown")))))

(deftest excise-tax-required?
  (testing "Cigarettes + US -> true"
    (is (registry/excise-tax-required? "cigarettes" "US")))

  (testing "Cigarettes + JP -> true"
    (is (registry/excise-tax-required? "cigarettes" "JP")))

  (testing "Cigarettes + EU -> true"
    (is (registry/excise-tax-required? "cigarettes" "EU")))

  (testing "Unknown product -> false"
    (is (not (registry/excise-tax-required? "unknown" "US"))))

  (testing "Unknown jurisdiction -> false"
    (is (not (registry/excise-tax-required? "cigarettes" "UNKNOWN")))))
