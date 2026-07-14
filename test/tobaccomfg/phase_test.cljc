(ns tobaccomfg.phase-test
  (:require [clojure.test :refer [deftest is testing]]
            [tobaccomfg.phase :as phase]))

(deftest phase-by-id
  (testing "Intake phase -> found"
    (is (phase/phase-by-id "intake")))

  (testing "Process phase -> found"
    (is (phase/phase-by-id "process")))

  (testing "Shipment phase -> found"
    (is (phase/phase-by-id "shipment")))

  (testing "Unknown phase -> nil"
    (is (nil? (phase/phase-by-id "unknown")))))

(deftest next-phase
  (testing "Intake -> process"
    (is (= "process" (phase/next-phase "intake"))))

  (testing "Process -> package"
    (is (= "package" (phase/next-phase "process"))))

  (testing "Shipment -> nil (terminal)"
    (is (nil? (phase/next-phase "shipment"))))

  (testing "Unknown -> nil"
    (is (nil? (phase/next-phase "unknown")))))

(deftest requires-escalation?
  (testing "Intake -> false"
    (is (not (phase/requires-escalation? "intake"))))

  (testing "Audit -> true"
    (is (phase/requires-escalation? "audit")))

  (testing "Shipment -> true"
    (is (phase/requires-escalation? "shipment"))))
