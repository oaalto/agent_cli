## Status

ready-for-agent

## Problem Statement

`acp-session-transcript-persistence` shipped **Session transcript file** write, debounced snapshot, and plain-line restore (ADR 0004 PR2). `SessionTranscriptCoordinator` is the documented facade — bind session id, trigger debounced writes, restore on resume, error decoration, diagnostics clipboard — but **coordinator behaviour is tested only through its parts**:

| Piece | Tested | Gap |
| --- | --- | --- |
| `TranscriptFileStore` | read/write/legacy path | — |
| `DebouncedTranscriptSnapshotWriter` | debounce, flush, no-id guard | — |
| `TranscriptTextSerializer` | per-block-type lines | plans omitted by design |
| `TranscriptViewController.restorePlainLines` | `> ` prefix mapping | not full restore flow |
| `SessionTranscriptCoordinator` | **none end-to-end** | bind → write → read → restore chain |

The editor still owns `snapshotBlocksOnEdt` at the persistence boundary — EDT complexity leaks into UI rather than the coordinator interface. Restore is **lossy** by design (plain lines only; no `PlanBlock`, tool bodies, usage footer) but the round-trip contract is implicit across four modules with no single **interface** documenting what is preserved vs discarded.

**Deletion test:** Removing `SessionTranscriptCoordinator` would scatter bind/debounce/restore/error handling across `AcpAgentEditor` and view controller — the coordinator earns its keep, but it is **shallow** until its interface is the test surface for the full round-trip.

## Solution

Deepen `SessionTranscriptCoordinator` as the single **Session transcript file** module:

1. **Coordinator integration tests** — drive bind → blocks-changed → debounced write → file on disk → restore → plain lines in view, without Swing editor or live ACP process.
2. **Explicit round-trip contract** — document in coordinator KDoc (and tests) which block types serialize, which restore as plain lines, and which are intentionally lossy.
3. **Optional:** Move `snapshotBlocksOnEdt` behind coordinator callback so editor passes `() -> List<TranscriptBlock>` without owning EDT policy (only if it reduces editor coupling without new abstractions).

No change to on-disk format or user-visible restore policy in v1 unless a bug is found — focus is **locality** and **test leverage**.

## User Stories

1. As a user resuming an ACP session, I want prior conversation restored from the **Session transcript file** exactly as today, so that deepening tests do not change restore UX.
2. As a user editing with streaming agent text, I want debounced snapshots to persist settled content, so that coordinator tests verify debounce without flaky timing in UI tests.
3. As a developer changing serialization rules, I want one coordinator test to fail if bind/write/restore breaks, so that I do not run the full editor to catch regressions.
4. As a developer adding a new `TranscriptBlock` type, I want the round-trip contract table updated in one place, so that serializer and restore policy stay aligned.
5. As a user, I want plan blocks to remain omitted from the file per ADR 0004, so that v1 deepening does not expand scope to structured restore.
6. As a developer fixing restore prefix rules, I want tests to assert `> ` user echo mapping through coordinator.restore, so that view controller details are not tested in isolation only.
7. As a user starting a session before `acpSessionId` is known, I want in-memory buffer flush-to-disk tested at coordinator level, so that pre-id prompts are not lost.
8. As a developer implementing correlation tokens on errors, I want coordinator error decoration covered alongside file write failures, so that diagnostics path stays in the facade.
9. As a user copying session diagnostics, I want clipboard bundle assembly to remain in coordinator, so that tests can assert bundle shape without editor.
10. As a developer, I want fake file store and fake view callback in tests, so that CI does not touch real `.idea/` paths.
11. As a user with a legacy flat `{sessionId}.txt` file, I want restore through coordinator to still read legacy paths, so that integration test includes legacy fixture.
12. As a user force-quitting the IDE, I want the last debounced snapshot to reflect recent blocks, so that flush-on-dispose behaviour is tested if implemented.

## Implementation Decisions

### Modules to modify

- `SessionTranscriptCoordinator` — clarify public interface for bind, onBlocksChanged, restoreIfPresent, dispose; optional EDT snapshot injection.
- `AcpAgentEditor` — optional: delegate block snapshot to coordinator-supplied callback.

### Modules to create (tests)

- `SessionTranscriptCoordinatorTest` — fake `TranscriptFileStore` or temp dir, fake restore sink, scheduler control for debounce.

### Round-trip contract (v1 — codify in tests)

| Block family | Serialize | Restore |
| --- | --- | --- |
| User echo | `> line` | plain line |
| Agent text (final/streaming finalized) | plain lines | plain lines |
| Tool call | `[tool: name …]` header | plain line |
| Plan | omitted | — |
| Error / stderr / auth | formatted plain | plain line |
| Usage footer | omitted (or single line if already serialized) | — |

### Seam design

```
SessionTranscriptCoordinator
  ├─ TranscriptFileStore (adapter — real or fake in tests)
  ├─ DebouncedTranscriptSnapshotWriter (internal)
  ├─ TranscriptTextSerializer (internal)
  └─ restoreSink: (List<String>) -> Unit  // test captures; prod → TranscriptViewController
```

**Test seam:** Coordinator constructor accepts file store + restore sink + test scheduler for debounce.

### ADR alignment

- **ADR 0004:** PR2 scope preserved; no structured block reconstruction.
- Does not contradict **ADR 0007** (finalize policy separate from persistence).

## Testing Decisions

### What to test

- bind(sessionId) → onBlocksChanged → advance debounce → file contains expected plain text.
- restoreIfPresent → restore sink receives lines in order with correct user prefix.
- Pre-session buffer: blocks before bind → bind → file includes buffered content.
- Missing file → restore no-op, no error.
- Legacy filename read path (if store supports).
- Error on write → correlation token / error callback if coordinator owns decoration.

### Modules to test

- **New:** `SessionTranscriptCoordinatorTest`
- **Keep:** existing unit tests for store, writer, serializer, view restore.

### Prior art

- `DebouncedTranscriptSnapshotWriterTest` — scheduler/fake time patterns.
- `TranscriptFileStoreTest` — temp directory fixtures.
- `TranscriptPanelTestHarness` — higher-layer integration (complementary).

### Verification

`./gradlew qualityGate` passes.

## Out of Scope

- Structured restore (rebuild `TranscriptBlock` graph from disk) — rejected in ADR 0004.
- Serializing plan panels or full tool payloads to file.
- PTY mode transcript files.
- Changing debounce interval or file path layout.
- Wiki automation (optional manual wiki update post-ship).

## Further Notes

- **Recommendation strength:** Worth exploring — maintainability and test locality; not a user-facing feature unless bugs found.
- **Builds on:** completed `acp-session-transcript-persistence`.
- **Pairs with:** `acp-session-loop-integration-tests` for complementary coverage (session loop vs file round-trip).
