(ns tobaccomfg.governor-test
  (:require [clojure.test :refer [deftest is are testing]]
            [tobaccomfg.governor :as governor]
            [tobaccomfg.store :as store]))

(deftest spec-basis-violations
  (testing "No spec-basis in value -> violation"
    (let [proposal {:cites [] :value {}}
          request {:op :log-production-batch}
          violations (#'governor/spec-basis-violations request proposal)]
      (is (seq violations))
      (is (= :no-spec-basis (-> violations first :rule)))))

  (testing "Spec-basis provided -> no violation"
    (let [proposal {:cites ["JT-1200-001"] :value {:jurisdiction "JP"}}
          request {:op :log-production-batch}
          violations (#'governor/spec-basis-violations request proposal)]
      (is (empty? violations))))

  (testing "Only applies to real operations"
    (let [proposal {:cites [] :value {}}
          request {:op :schedule-maintenance}  ; not a critical op
          violations (#'governor/spec-basis-violations request proposal)]
      (is (empty? violations)))))

(deftest facility-permit-invalid-violations
  (testing "Valid facility permit -> no violation"
    (let [st (store/mem-store
              {:initial-batches
               {"batch-1" {:facility-permit-valid? true
                          :product-id "cigarettes-12mg"
                          :batch-quantity 50000
                          :ingredients-list ["tobacco" "paper"]}}})
          request {:op :log-production-batch :subject "batch-1"}
          violations (#'governor/facility-permit-invalid-violations request st)]
      (is (empty? violations))))

  (testing "Invalid facility permit -> violation"
    (let [st (store/mem-store
              {:initial-batches
               {"batch-2" {:facility-permit-valid? false
                          :product-id "cigarettes-12mg"}}})
          request {:op :log-production-batch :subject "batch-2"}
          violations (#'governor/facility-permit-invalid-violations request st)]
      (is (seq violations))
      (is (= :facility-permit-invalid (-> violations first :rule))))))

(deftest batch-manifest-incomplete-violations
  (testing "Complete manifest -> no violation"
    (let [st (store/mem-store
              {:initial-batches
               {"batch-1" {:product-id "cigarettes-12mg"
                          :batch-quantity 50000
                          :ingredients-list ["tobacco" "paper"]}}})
          request {:op :log-production-batch :subject "batch-1"}
          violations (#'governor/batch-manifest-incomplete-violations request st)]
      (is (empty? violations))))

  (testing "Missing product-id -> violation"
    (let [st (store/mem-store
              {:initial-batches
               {"batch-2" {:batch-quantity 50000
                          :ingredients-list ["tobacco" "paper"]}}})
          request {:op :log-production-batch :subject "batch-2"}
          violations (#'governor/batch-manifest-incomplete-violations request st)]
      (is (seq violations))
      (is (= :batch-manifest-incomplete (-> violations first :rule)))))

  (testing "Missing ingredients-list -> violation"
    (let [st (store/mem-store
              {:initial-batches
               {"batch-3" {:product-id "cigarettes-12mg"
                          :batch-quantity 50000}}})
          request {:op :log-production-batch :subject "batch-3"}
          violations (#'governor/batch-manifest-incomplete-violations request st)]
      (is (seq violations))
      (is (= :batch-manifest-incomplete (-> violations first :rule))))))

(deftest compliance-concern-unresolved-violations
  (testing "No compliance concern -> no violation"
    (let [st (store/mem-store
              {:initial-batches
               {"batch-1" {:compliance-concern-raised? false}}})
          request {:op :log-production-batch :subject "batch-1"}
          violations (#'governor/compliance-concern-unresolved-violations request st)]
      (is (empty? violations))))

  (testing "Unresolved compliance concern -> violation"
    (let [st (store/mem-store
              {:initial-batches
               {"batch-2" {:compliance-concern-raised? true
                          :compliance-concern-resolved? false}}})
          request {:op :log-production-batch :subject "batch-2"}
          violations (#'governor/compliance-concern-unresolved-violations request st)]
      (is (seq violations))
      (is (= :compliance-concern-unresolved (-> violations first :rule)))))

  (testing "Resolved compliance concern -> no violation"
    (let [st (store/mem-store
              {:initial-batches
               {"batch-3" {:compliance-concern-raised? true
                          :compliance-concern-resolved? true}}})
          request {:op :log-production-batch :subject "batch-3"}
          violations (#'governor/compliance-concern-unresolved-violations request st)]
      (is (empty? violations)))))

(deftest already-logged-violations
  (testing "Batch not yet logged -> no violation"
    (let [st (store/mem-store
              {:initial-batches
               {"batch-1" {:production-logged? false}}})
          request {:op :log-production-batch :subject "batch-1"}
          violations (#'governor/already-logged-violations request st)]
      (is (empty? violations))))

  (testing "Already logged batch -> violation"
    (let [st (store/mem-store
              {:initial-batches
               {"batch-2" {:production-logged? true}}})
          request {:op :log-production-batch :subject "batch-2"}
          violations (#'governor/already-logged-violations request st)]
      (is (seq violations))
      (is (= :already-logged (-> violations first :rule))))))

(deftest governor-check-integration
  (testing "Clean proposal -> ok"
    (let [st (store/mem-store
              {:initial-batches
               {"batch-1" {:product-id "cigarettes-12mg"
                          :batch-quantity 50000
                          :ingredients-list ["tobacco" "paper"]
                          :facility-permit-valid? true
                          :compliance-concern-raised? false
                          :jurisdiction "JP"
                          :evidence-checklist [:batch-manifest :ingredient-declaration
                                               :packaging-label-draft :facility-registration
                                               :production-log :shipment-authorization]}}})
          request {:op :log-production-batch :subject "batch-1"}
          proposal {:cites ["JT-1200-001"] :value {:jurisdiction "JP"} :confidence 0.85 :stake :log-production-batch}
          verdict (governor/check request {} proposal st)]
      (is (not (:ok? verdict)))  ; high-stakes always escalates
      (is (:escalate? verdict))  ; but escalate, not hold
      (is (empty? (:violations verdict)))))

  (testing "Multiple violations -> hold"
    (let [st (store/mem-store
              {:initial-batches
               {"batch-2" {:facility-permit-valid? false
                          :product-id "cigarettes-12mg"}}})
          request {:op :log-production-batch :subject "batch-2"}
          proposal {:cites [] :value {} :confidence 0.85}
          verdict (governor/check request {} proposal st)]
      (is (not (:ok? verdict)))
      (is (:hard? verdict))
      (is (seq (:violations verdict))))))
