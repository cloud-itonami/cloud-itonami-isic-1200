(ns tobaccomfg.store
  "In-memory batch and shipment store for testing. Production deployments
  will use a durable, audited state backend.")

(defn mem-store
  "Create an in-memory store for testing.
  Accepts {:initial-batches {batch-id batch-record} ...}."
  [& [opts]]
  (let [initial-batches (or (:initial-batches opts) {})]
    (atom
     {:batches (into {} initial-batches)
      :audit-log []})))

(defn get-batch
  "Retrieve a batch by ID from the store."
  [store batch-id]
  (get-in @store [:batches batch-id]))

(defn update-batch
  "Update a batch record in the store and log the change."
  [store batch-id updates fact]
  (swap! store (fn [s]
                 (-> s
                     (update-in [:batches batch-id] merge updates)
                     (update :audit-log conj fact))))
  (get-batch store batch-id))

(defn batch-already-logged?
  "Check if batch has been logged to production records."
  [store batch-id]
  (-> (get-batch store batch-id)
      :production-logged?
      boolean))

(defn batch-shipment-finalized?
  "Check if batch shipment has been finalized."
  [store batch-id]
  (-> (get-batch store batch-id)
      :shipment-finalized?
      boolean))

(defn get-audit-log
  "Retrieve the audit log from the store."
  [store]
  (:audit-log @store []))
