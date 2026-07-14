(ns tobaccomfg.operation
  "Tobacco manufacturing operations: the actual dispatch layer that executes
  approved proposals.

  Dispatch rules:
    - `:log-production-batch` -> record batch to production ledger (human sign-off)
    - `:schedule-maintenance` -> propose equipment maintenance (coordination)
    - `:flag-compliance-concern` -> escalate regulatory flag (always -> human)
    - `:coordinate-shipment` -> outbound shipment coordination (human sign-off)

  All dispatch is to audit-ledger only. No direct equipment control (that is
  licensed plant operator exclusive)."
  (:require [tobaccomfg.store :as store]))

(defn run-operation
  "Execute an approved operation and write the audit trail.
  Returns the updated batch record."
  [store batch-id op-type details]
  (case op-type
    :log-production-batch
    (store/update-batch
     store batch-id
     {:production-logged? true
      :logged-at (str (js/Date.))}
     {:t :operation-log-production-batch
      :subject batch-id
      :details details})

    :schedule-maintenance
    (store/update-batch
     store batch-id
     {}
     {:t :operation-schedule-maintenance
      :subject batch-id
      :maintenance-request details})

    :flag-compliance-concern
    (store/update-batch
     store batch-id
     {:compliance-concern-raised? true}
     {:t :operation-flag-compliance-concern
      :subject batch-id
      :concern details})

    :coordinate-shipment
    (store/update-batch
     store batch-id
     {:shipment-finalized? true
      :finalized-at (str (js/Date.))}
     {:t :operation-coordinate-shipment
      :subject batch-id
      :shipment-details details})

    ;; default
    (throw (ex-info "Unknown operation" {:op op-type}))))
