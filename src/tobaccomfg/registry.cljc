(ns tobaccomfg.registry
  "Tobacco manufacturing registry: pure functions for regulatory compliance
  validation, batch tracking, and excise-tax coordination.

  This module NEVER makes or influences marketing/health claims. All validation
  is objective: evidence checklist completeness, batch tracking, production
  log audit, and escalation flagging only."
  (:require [tobaccomfg.facts :as facts]))

(defn batch-already-logged?
  "Check if a batch has already been logged into production records.
  Prevents duplicate logging of the same batch."
  [batch]
  (boolean (:production-logged? batch)))

(defn shipment-already-finalized?
  "Check if a batch's shipment has already been finalized.
  Prevents duplicate shipment coordination."
  [batch]
  (boolean (:shipment-finalized? batch)))

(defn compliance-concern-raised?
  "Check if an unresolved excise-tax or labeling compliance concern has been raised."
  [batch]
  (and (true? (:compliance-concern-raised? batch))
       (not (true? (:compliance-concern-resolved? batch)))))

(defn facility-permit-valid?
  "Check if manufacturing facility registration/permit is current.
  Non-compliance with facility registration is an unconditional hold."
  [batch]
  (boolean (:facility-permit-valid? batch true)))

(defn batch-manifest-complete?
  "Check if batch manifest (product type, quantity, ingredients) is complete."
  [batch]
  (and (:product-id batch)
       (:batch-quantity batch)
       (:ingredients-list batch)))

(defn track-and-trace-eligible?
  "Check if batch qualifies for track-and-trace requirements."
  [product-type jurisdiction-id]
  (when-let [pc (facts/product-category-by-id product-type)]
    (when (:track-and-trace pc)
      (let [j (facts/jurisdiction-by-id jurisdiction-id)]
        (boolean j)))))

(defn health-warning-required?
  "Check if product category requires health warning certification.
  NOTE: This function only CHECKS the requirement; it never certifies.
  Certification is human/compliance-officer exclusive."
  [product-type]
  (when-let [pc (facts/product-category-by-id product-type)]
    (:requires-health-warning pc false)))

(defn excise-tax-required?
  "Check if excise tax filing is required for this product + jurisdiction.
  NOTE: This function only CHECKS the requirement; it never certifies tax
  compliance. Tax certification is human/compliance-officer exclusive."
  [product-type jurisdiction-id]
  (and (when-let [pc (facts/product-category-by-id product-type)]
         (:requires-excise-filing pc false))
       (facts/excise-tax-applicable? jurisdiction-id)))
