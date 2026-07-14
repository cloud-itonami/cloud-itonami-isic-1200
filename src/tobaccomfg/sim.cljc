(ns tobaccomfg.sim
  "Tobacco manufacturing simulation driver for local testing.
  clojure -M:dev:run to execute."
  (:require [tobaccomfg.store :as store]
            [tobaccomfg.governor :as governor]
            [tobaccomfg.operation :as operation]))

(def demo-batch
  "A sample batch for testing."
  {"batch-1"
   {:product-id "cigarettes-12mg"
    :product-type "cigarettes"
    :batch-quantity 50000
    :jurisdiction "JP"
    :facility-permit-valid? true
    :ingredients-list ["tobacco-leaf" "paper" "water"]
    :evidence-checklist ["batch-manifest" "ingredient-declaration"
                         "packaging-label-draft" "facility-registration"
                         "production-log" "shipment-authorization"]
    :compliance-concern-raised? false}})

(defn -main
  "Run a demo scenario."
  [& _args]
  (println "=== Tobacco Manufacturing Demo ===")
  (let [st (store/mem-store {:initial-batches demo-batch})
        batch-id "batch-1"
        proposal {:value {:jurisdiction "JP"}
                  :cites ["JT-1200-001"]
                  :confidence 0.85}
        request {:op :log-production-batch :subject batch-id :stake :log-production-batch}
        verdict (governor/check request {} proposal st)]
    (println "\nBatch:" (store/get-batch st batch-id))
    (println "\nGovernor Verdict:" verdict)
    (if (:ok? verdict)
      (do (println "\n✓ Proposal approved (requires human sign-off for high-stakes)")
          (operation/run-operation st batch-id :log-production-batch {}))
      (do (println "\n✗ Proposal held")
          (println "Violations:" (:violations verdict))))))
