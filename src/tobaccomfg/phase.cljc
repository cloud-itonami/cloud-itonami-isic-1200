(ns tobaccomfg.phase
  "Tobacco manufacturing batch lifecycle phases.

  Operating states for a batch:
    - intake: batch received, initial compliance review
    - process: active processing (equipment under licensed operator control)
    - package: final packaging, label verification
    - audit: compliance audit, excise-tax verification
    - shipment: outbound coordination")

(def phases
  {"intake" {:id "intake" :next "process" :requires-escalation false}
   "process" {:id "process" :next "package" :requires-escalation false}
   "package" {:id "package" :next "audit" :requires-escalation false}
   "audit" {:id "audit" :next "shipment" :requires-escalation true}
   "shipment" {:id "shipment" :next nil :requires-escalation true}})

(defn phase-by-id [id]
  (get phases id))

(defn next-phase [current-id]
  (when-let [p (phase-by-id current-id)]
    (:next p)))

(defn requires-escalation? [phase-id]
  (when-let [p (phase-by-id phase-id)]
    (:requires-escalation p false)))
