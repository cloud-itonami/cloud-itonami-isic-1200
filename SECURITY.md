# Security Policy

This is a blueprint-stage project for tobacco manufacturing coordination. Security is paramount at all stages.

## Audit Trail

- All operations are logged to an append-only audit ledger.
- No batch state changes without Governor approval.
- All escalations (compliance concerns) are auditable and never silently suppressed.

## Regulatory Guardrails

**HARD INVARIANTS** (never bypassed):
1. Processing-line control is exclusively human/robot-operator domain — the LLM never actuates equipment directly.
2. Health warning compliance certification requires licensed human review — LLM cannot sign off.
3. Excise-tax compliance requires licensed compliance officer sign-off — LLM provides transparency only.
4. Facility permit must be valid and current before any batch logging — verified unconditionally.

## Reporting Security Issues

If you discover a security issue, please open a private security advisory or contact the cloud-itonami maintainers.

## Dependencies

This project uses minimal dependencies:
- `langgraph` (LLM state machine) — community vetted, zero-trust audit logging.
- `clj-kondo` (linter) — pure function, no side effects.

No external tobacco-industry APIs are used. All validation is self-contained.
