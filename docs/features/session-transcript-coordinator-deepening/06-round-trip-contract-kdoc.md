# 06 — Round-trip contract KDoc

**Parent:** `prd.md`

**What to build:** Document the v1 lossy round-trip contract in `SessionTranscriptCoordinator` KDoc (and brief cross-reference in serializer if needed): which `TranscriptBlock` families serialize to the **Session transcript file**, which restore as plain lines, and which are intentionally omitted (plans, usage footer, full tool payloads). The table aligns with tickets 02–05 tests so serializer and restore policy stay discoverable in one place when adding new block types.

**Blocked by:** 02 — Bind-write-restore round-trip integration test; 03 — Pre-id buffer and missing-file restore; 04 — Legacy transcript restore; 05 — Error paths and diagnostics clipboard

**Status:** ready-for-agent

- [ ] KDoc on coordinator (primary) states serialize vs restore vs omitted for user echo, agent text, tool header, plan, error/stderr/auth, usage footer
- [ ] Contract matches behaviour asserted in coordinator integration tests; no drift from ADR 0004 plain-line restore scope
- [ ] `./gradlew qualityGate` passes
