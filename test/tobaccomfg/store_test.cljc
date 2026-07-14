(ns tobaccomfg.store-test
  (:require [clojure.test :refer [deftest is testing]]
            [tobaccomfg.store :as store]))

(deftest mem-store
  (testing "Create empty store -> ok"
    (let [st (store/mem-store)]
      (is st)
      (is (map? @st))))

  (testing "Create store with initial batches -> found"
    (let [st (store/mem-store
              {:initial-batches
               {"batch-1" {:product-id "cigarettes-12mg"}}})
          b (store/get-batch st "batch-1")]
      (is b)
      (is (= "cigarettes-12mg" (:product-id b))))))

(deftest get-batch
  (testing "Get existing batch -> found"
    (let [st (store/mem-store
              {:initial-batches
               {"batch-1" {:product-id "cigarettes"}}})
          b (store/get-batch st "batch-1")]
      (is b)
      (is (= "cigarettes" (:product-id b)))))

  (testing "Get nonexistent batch -> nil"
    (let [st (store/mem-store)]
      (is (nil? (store/get-batch st "batch-nonexistent"))))))

(deftest update-batch
  (testing "Update batch -> reflected"
    (let [st (store/mem-store
              {:initial-batches
               {"batch-1" {:product-id "cigarettes"}}})
          fact {:t :test-fact}
          updated (store/update-batch st "batch-1" {:status "updated"} fact)]
      (is (= "updated" (:status updated)))
      (is (= "cigarettes" (:product-id updated)))
      (is (contains? (set (store/get-audit-log st)) fact))))

  (testing "Update adds to audit log -> found"
    (let [st (store/mem-store
              {:initial-batches {"batch-1" {}}})
          fact {:t :test-audit}
          _ (store/update-batch st "batch-1" {} fact)
          log (store/get-audit-log st)]
      (is (some #(= fact %) log)))))

(deftest batch-already-logged?
  (testing "Not logged -> false"
    (let [st (store/mem-store
              {:initial-batches {"batch-1" {:production-logged? false}}})]
      (is (not (store/batch-already-logged? st "batch-1")))))

  (testing "Logged -> true"
    (let [st (store/mem-store
              {:initial-batches {"batch-1" {:production-logged? true}}})]
      (is (store/batch-already-logged? st "batch-1")))))

(deftest batch-shipment-finalized?
  (testing "Not finalized -> false"
    (let [st (store/mem-store
              {:initial-batches {"batch-1" {:shipment-finalized? false}}})]
      (is (not (store/batch-shipment-finalized? st "batch-1")))))

  (testing "Finalized -> true"
    (let [st (store/mem-store
              {:initial-batches {"batch-1" {:shipment-finalized? true}}})]
      (is (store/batch-shipment-finalized? st "batch-1")))))

(deftest get-audit-log
  (testing "Empty log -> empty list"
    (let [st (store/mem-store)]
      (is (empty? (store/get-audit-log st)))))

  (testing "With facts -> found in log"
    (let [st (store/mem-store)
          fact1 {:t :fact1}
          fact2 {:t :fact2}]
      (store/update-batch st "batch-1" {} fact1)
      (store/update-batch st "batch-1" {} fact2)
      (let [log (store/get-audit-log st)]
        (is (= 2 (count log)))
        (is (some #(= fact1 %) log))
        (is (some #(= fact2 %) log))))))
