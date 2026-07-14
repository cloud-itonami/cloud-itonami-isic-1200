(ns tobaccomfg.advisor
  "Tobacco Manufacturing Advisor stub.

  In production, this will be a langgraph-clj StateGraph that dispatches
  to an LLM, collects structured context (batch history, regulatory status),
  and drafts proposals for the Governor to evaluate.

  For now: skeleton for future implementation."
  (:require [tobaccomfg.governor :as governor]))

(defn advisor-fn
  "Stub advisor function.
  In production: LLM-driven proposal drafting against Governor.
  For now: returns a placeholder."
  [_state _context]
  {:status :advisor-stub})
