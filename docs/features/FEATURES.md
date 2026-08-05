# Features

Master list of features tracked in `docs/features/`. PRD status (`## Status` section) and per-ticket `**Status:**` in slice files are authoritative; this page is the rollup and ordering guide.

**Active** features are ordered by **implementation priority** (top = next). Agents maintain this order — do not ask the human to set it. When adding or updating a feature, read PRDs, ticket blocking edges, ADRs, and cross-feature dependencies; insert or re-order so the list reflects what should ship next.

**Implemented** features are listed separately, most recently completed first.

Last updated: 2026-08-05

## Summary

| Metric | Count |
| --- | --- |
| Ready-for-agent features | 5 |
| Ready-for-agent tickets | 15 |
| Completed features | 17 |

---

## Active (implementation order)

| Feature | Status | Tickets | Rationale | PRD |
| --- | --- | --- | --- | --- |
| [plan-panel-renderer-removal](plan-panel-renderer-removal/) | ready-for-agent | [01](plan-panel-renderer-removal/01-delete-legacy-plan-panel-renderer.md) | Quick deletion win; removes dead `PlanPanelRenderer` parallel to ADR 0005 row adapters | [prd](plan-panel-renderer-removal/prd.md) |
| [acp-session-loop-integration-tests](acp-session-loop-integration-tests/) | ready-for-agent | [01](acp-session-loop-integration-tests/01-harness-new-session-start.md) → [04](acp-session-loop-integration-tests/04-dispose-cancel-lifecycle.md); [05](acp-session-loop-integration-tests/05-auth-bootstrap-stretch.md) (stretch) | Headless harness for full `AcpSessionController` connect→bootstrap→resume→prompt chain; enables safer editor thinning | [prd](acp-session-loop-integration-tests/prd.md) |
| [acp-agent-editor-thinning](acp-agent-editor-thinning/) | ready-for-agent | [01](acp-agent-editor-thinning/01-executor-only-finalize-on-prompt.md) · [02](acp-agent-editor-thinning/02-thin-session-start-delegate.md) · [03](acp-agent-editor-thinning/03-editor-thinning-verification-and-ship.md) | Completes editor→controller separation; orchestrator wiring shipped | [prd](acp-agent-editor-thinning/prd.md) |
| [launch-argument-unification](launch-argument-unification/) | ready-for-agent | [01](launch-argument-unification/01-kernel-launch-argument-resolver.md) · [02](launch-argument-unification/02-acp-launch-adapter-argument-resolver.md) · [03](launch-argument-unification/03-pty-launch-adapter-wsl-env-parity.md) · [04](launch-argument-unification/04-worktree-resume-probe-kernel-alignment.md) | Cross-slice leverage; shared executable validation and WSL env parity after `AgentLaunchResolver` | [prd](launch-argument-unification/prd.md) |
| [session-transcript-coordinator-deepening](session-transcript-coordinator-deepening/) | ready-for-agent | [01](session-transcript-coordinator-deepening/01-coordinator-test-seam-injection.md) → [07](session-transcript-coordinator-deepening/07-edt-snapshot-delegation-optional.md) | Coordinator round-trip integration tests; explicit lossy restore contract; optional EDT delegation | [prd](session-transcript-coordinator-deepening/prd.md) |

#### acp-session-loop-integration-tests tickets

| Ticket | Blocked by |
| --- | --- |
| [01 — Harness and new-session start integration](acp-session-loop-integration-tests/01-harness-new-session-start.md) | — |
| [02 — Resume load path integration](acp-session-loop-integration-tests/02-resume-load-path.md) | 01 |
| [03 — Prompt loop and ingestion integration](acp-session-loop-integration-tests/03-prompt-loop-ingestion.md) | 01 |
| [04 — Dispose and cancel lifecycle integration](acp-session-loop-integration-tests/04-dispose-cancel-lifecycle.md) | 03 |
| [05 — Auth-required bootstrap integration (stretch)](acp-session-loop-integration-tests/05-auth-bootstrap-stretch.md) | 01 |

#### launch-argument-unification tickets

| Ticket | Blocked by |
| --- | --- |
| [01 — Kernel `LaunchArgumentResolver` with unit tests](launch-argument-unification/01-kernel-launch-argument-resolver.md) | — |
| [02 — ACP launch adapter uses argument resolver](launch-argument-unification/02-acp-launch-adapter-argument-resolver.md) | 01 |
| [03 — PTY launch adapter uses argument resolver with WSL env parity](launch-argument-unification/03-pty-launch-adapter-wsl-env-parity.md) | 01 |
| [04 — Worktree resume probe uses kernel command base](launch-argument-unification/04-worktree-resume-probe-kernel-alignment.md) | 02, 03 |

#### session-transcript-coordinator-deepening tickets

| Ticket | Blocked by |
| --- | --- |
| [01 — Coordinator test seam injection](session-transcript-coordinator-deepening/01-coordinator-test-seam-injection.md) | — |
| [02 — Bind-write-restore round-trip](session-transcript-coordinator-deepening/02-bind-write-restore-round-trip.md) | 01 |
| [03 — Pre-id buffer and missing-file restore](session-transcript-coordinator-deepening/03-pre-id-buffer-and-missing-file.md) | 01 |
| [04 — Legacy transcript restore](session-transcript-coordinator-deepening/04-legacy-transcript-restore.md) | 01 |
| [05 — Error paths and diagnostics clipboard](session-transcript-coordinator-deepening/05-error-paths-and-diagnostics-clipboard.md) | 01 |
| [06 — Round-trip contract KDoc](session-transcript-coordinator-deepening/06-round-trip-contract-kdoc.md) | 02, 03, 04, 05 |
| [07 — EDT snapshot delegation (optional)](session-transcript-coordinator-deepening/07-edt-snapshot-delegation-optional.md) | 02 |

#### acp-agent-editor-thinning tickets

| Ticket | Blocked by |
| --- | --- |
| [01 — Executor-only finalize on prompt submit](acp-agent-editor-thinning/01-executor-only-finalize-on-prompt.md) | — |
| [02 — Thin session start delegate](acp-agent-editor-thinning/02-thin-session-start-delegate.md) | — |
| [03 — Editor thinning verification and ship](acp-agent-editor-thinning/03-editor-thinning-verification-and-ship.md) | 01, 02 |

---

## Architecture conflicts

| Features | Note |
| --- | --- |
| `acp-session-loop-integration-tests` ↔ `acp-agent-editor-thinning` | Session-loop harness should land before editor thinning refactors for CI guardrails |
| `acp-agent-editor-thinning` ↔ `session-transcript-coordinator-deepening` | Editor keeps `SessionTranscriptCoordinator` bind/restore wiring; coordinator deepening is optional follow-up per PRD out-of-scope |

---

## Implemented

| Feature | Tickets | Notes |
| --- | --- | --- |
| [worktree-orchestrator-production-wiring](worktree-orchestrator-production-wiring/) | — | Shipped (2026-08-05, `ae78169`); `worktreeRecordId` through lifecycle → orchestrator; editor persist removed |
| [acp-transcript-package-restructure](acp-transcript-package-restructure/) | 01–07 | All `done` (2026-08-05); `transcript/{model,render,view,theme}/` repackage + `TranscriptPackageDependencyTest` CI guard |
| [acp-transcript-fence-normalization](acp-transcript-fence-normalization/) | 01–03 | All `done` (2026-08-05); single `normalizeAgentFences` seam, tool-text gating, CI construction guard |
| [acp-transcript-finalize-policy](acp-transcript-finalize-policy/) | 01–02 | All `done` (2026-08-05); `TranscriptFinalizePolicy` + CI construction guard |
| [acp-transcript-panel-integration-tests](acp-transcript-panel-integration-tests/) | 01 | Harness + review follow-ups `done` (2026-08-05); mounted-panel golden scenarios on ViewController apply path |
| [acp-transcript-block-view-decomposition](acp-transcript-block-view-decomposition/) | 01–08 | All `done` (2026-08-05); adapter registry + 4 row adapters, mapper, test gaps closed in ticket 08 |
| [acp-transcript-content-renderer](acp-transcript-content-renderer/) | 01–03 | All `done` (2026-08-04); unified dual markdown→body-part pipelines |
| [worktree-pending-launch-handoff](worktree-pending-launch-handoff/) | 01–02 | All `done` (2026-08-03); `WorktreePendingLaunchHandoff` deep module |
| [acp-session-resume-orchestration](acp-session-resume-orchestration/) | 01–04 | All `done`; resume orchestration extracted to `AcpSessionResumeOrchestrator` |
| [acp-session-controller-deepening](acp-session-controller-deepening/) | 01–07 | All `done`; controller API shrunk to `start` / `prompt` / `cancelPrompt` / `dispose` |
| [transcript-pipeline-consolidation](transcript-pipeline-consolidation/) | 01–03 | All `done` (2026-08-03) |
| [acp-client-operations-wiring](acp-client-operations-wiring/) | 01–06 | All `done`; `SessionFilesystemOperations` deep module |
| [acp-transcript-block-alignment-fix](acp-transcript-block-alignment-fix/) | 01–02 | All `done` |
| [acp-session-transcript-persistence](acp-session-transcript-persistence/) | 01–06 | All `done` |
| [unified-launch-resolution](unified-launch-resolution/) | 01–05 | All `done` |
| [tiered-session-diagnostics](tiered-session-diagnostics/) | 01–03 | All `done` |
| [settings-observability-help](settings-observability-help/) | 01 | Ticket `done` |

---

## How to use this document

1. Pick the next feature from **Active**.
2. Open its PRD, then implement tickets in order respecting **Blocked by** chains.
3. When all tickets in a feature are complete, flip each ticket's `**Status:**` to `done` and move the feature to **Implemented**.
4. Update this file and `CHANGELOG.md` when feature status changes materially.

Agents: follow **implement** and **to-tickets** skills; see `docs/agents/issue-tracker.md` for master-list rules.
