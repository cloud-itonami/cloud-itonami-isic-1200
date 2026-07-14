# cloud-itonami-isic-1200

Open Business Blueprint for **ISIC 1200**: manufacture of tobacco products — the representative *regulated manufacturing* vertical of the 衣食住 scaffold batch (ADR-2607122200).

**Maturity: `:blueprint`** — this repository publishes the business blueprint only. There is **no actor implementation yet**, and none is claimed. ISIC division 10-12 (food) and extended manufacturing sits in **rollout Wave 3 (production/robotics)** of the reverse-toposort plan (ADR-2607121000): implementation is gated on the robotics premise (ADR-2607011000) — a real robot fleet plus an independent governor with an accident-free audit ledger. Publishing the blueprint now is deliberate ammunition loading for when that gate opens (ADR-2607122100 Track A).

## What the implemented actor will be

**TobaccoOps-LLM ⊣ Tobacco Manufacturing Governor** — the fleet-standard pattern: the advisor LLM drafts batch logging, equipment maintenance scheduling, and excise-tax/compliance-concern escalation; the independent `:tobacco-manufacturing-governor` (a keyword unique fleet-wide) gates every action; physical-domain work (processing, packaging, warehouse handling) is executed by robots under `kotoba-lang/robotics` safety classes, never dispatched directly by the LLM. Regulatory actions (health-warning compliance, excise-tax certification) ALWAYS require licensed compliance officer sign-off.

Operating states: `intake → process → package → audit → shipment`.

## Scope & Exclusions

**IN SCOPE:**
- Batch logging and traceability coordination
- Equipment maintenance scheduling proposals
- Excise-tax and labeling compliance concern flagging (always escalates to human)
- Outbound shipment coordination

**STRICTLY OUT OF SCOPE (human exclusive, never actor control):**
- Processing-line equipment operation
- Health warning label certification
- Excise-tax compliance sign-off
- Marketing or health claims

## Why open

AGPL-3.0-or-later, forkable by any qualified manufacturer, so tobacco producers never surrender production and traceability data to a closed SaaS. Part of the [cloud-itonami](https://itonami.cloud) open business fleet.
