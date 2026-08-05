## Status

implemented

**Triage:** `implemented`

## Problem Statement

Agent markdown fence normalization — repairing malformed ` ``` ` lines from streaming LLM output (merged language tags, citation-style `line:line:path` fences, mid-line openings, inline closings) ran on **two divergent paths**:

- **Streaming path:** `AgentTextRowAdapter` applied `normalizeAgentFences` to plain text in a `JTextPane` with a cursor character appended.
- **Final path:** `TranscriptContentRenderer` normalized via `ContentRenderOptions.applyFenceNormalization`, but `TranscriptMarkdownRenderer.parseToBlocks` also called `normalizeAgentFences` — double application.

When a new fence edge case was fixed in one path only, users saw the streaming preview layout disagree with the finalized render. CHANGELOG history showed repeated fence fixes touching both modules independently.

## Solution

Unify fence normalization behind one **deep module** — `TranscriptAgentFenceNormalizer` — with a single public entry:

```kotlin
internal fun normalizeAgentFences(text: String): String
```

**Production call graph:**

```
StreamingAgentText → AgentTextRowAdapter → normalizeAgentFences → JTextPane text + CURSOR_CHAR
FinalAgentText     → TranscriptContentRenderer (AGENT_TEXT) → normalizeAgentFences → parseToBlocks → body parts
Tool text (markdown-like) → TranscriptContentRenderer (forToolMarkdownText) → normalizeAgentFences when gated
```

Normalization rules, citation regex tables, and language-prefix ordering live in one file with one test suite. `TranscriptMarkdownRenderer.parseToBlocks` does **not** normalize — content renderer is the sole pre-parse hook for final/tool markdown.

Document normalization invariants in module KDoc:

1. Idempotent on already-valid GFM fences.
2. Preserves non-fence prose verbatim except required splits.
3. Safe on partial input during streaming (no throws on unclosed fence; auto-close at EOF).

**Streaming vs final parity** means fence **text** parity only — streaming stays plain monospace `JTextPane`; final uses highlighted body parts. Layout jump from plain-text streaming to code widgets is out of scope.

## User Stories

1. As a developer fixing citation-style ` ```3:10:path/File.kt` fences, I want one function to patch, so that streaming and final renders match after the fix.
2. As a developer fixing merged opening fences (` ```kotlinfun main`), I want tests in one module, so that I do not duplicate fixtures in binder and renderer tests.
3. As a user watching streaming agent output, I want fence previews to match the finalized code blocks, so that layout does not jump on prompt completion (fence text parity).
4. As a user reading Kotlin agent output with Cursor citation fences, I want syntax highlighting in final render, so that normalized fences carry correct language ids.
5. As a developer adding a new malformed fence pattern from production logs, I want a single regression test case, so that both paths inherit it automatically.
6. As a maintainer, I want normalization applied before content renderer parse in one documented order: normalize → render, so that pipeline diagrams are accurate.
7. As a developer on `TranscriptContentRenderer`, I want to call normalization as the first pipeline stage, so that tool and agent text share fence rules when applicable.
8. As a user with mid-line fence openings (`text before ```kotlin`), I want the split to appear during streaming, not only after finalize, so that long responses remain readable while streaming.
9. As a developer debugging inline closing fences, I want idempotent normalization, so that double-application during stream chunks does not corrupt text.
10. As an AI agent, I want wiki to reference one normalizer module, so that fence behaviour is discoverable.
11. As a reviewer, I want grep to show zero duplicate `normalizeAgentFences(` outside allowlist, so that drift is structurally impossible.
12. As a user pasting agent output with Windows line endings, I want normalization to handle `\r\n`, so that fences split correctly cross-platform.
13. As a developer writing property tests, I want normalize(normalize(x)) == normalize(x), so that streaming chunk boundaries can re-normalize safely.
14. As a product owner, I want fewer fence-related changelog entries after unification, so that maintenance cost drops.
15. As a developer coordinating finalize policy work, I want finalize to trigger re-render through the same normalized text stream, so that finalize does not change fence semantics.
16. As a user reading tool card text bodies with fences, I want optional application of agent fence rules to tool text when patterns match, so that tool output benefits from the same repairs (via `ContentRenderOptions.forToolMarkdownText`).
17. As a tester, I want fixtures from prior `TranscriptAgentFenceNormalizer` tests consolidated, so that coverage is not lost in migration.
18. As a developer, I want malformed partial fences at stream end to render best-effort plain text, so that normalization never blanks the streaming pane.

## Implementation Decisions

### Ownership

- **Vertical slice:** ACP transcript text preprocessing — `TranscriptAgentFenceNormalizer` deepened; streaming adapter and content renderer are callers only.

### Call graph (implemented)

```
StreamingAgentText → AgentTextRowAdapter.bindStreamingAgent → normalizeAgentFences → JTextPane text
FinalAgentText     → TranscriptContentRenderer (AGENT_TEXT) → normalizeAgentFences → parseToBlocks
Tool markdown text → TranscriptContentRenderer (forToolMarkdownText) → normalizeAgentFences when gated
```

### Modules modified

- **Normalizer:** consolidated regex/split helpers; KDoc invariants; single entry point `normalizeAgentFences`.
- **TranscriptBlockLabelBinder:** removed (dead code after row-adapter split; logic lives in `SimpleTextRowAdapter` / `AgentTextRowAdapter`).
- **TranscriptMarkdownRenderer:** removed duplicate pre-parse normalize from `parseToBlocks`.
- **TranscriptToolCallContentRenderer:** tool text uses `ContentRenderOptions.forToolMarkdownText`.

### Streaming chunk safety

- Normalizer tolerates incomplete trailing fences — auto-closes at EOF without throwing.
- Callers re-normalize the full accumulated buffer on each streaming bind.

### Tool card text

- `ContentRenderOptions.forToolMarkdownText(text)` enables `applyFenceNormalization` when text contains `` ``` `` or `likelyContainsMarkdown` is true.

### Seam for testing

**Primary test seam:** `normalizeAgentFences` — pure string in/out.

Secondary: `TranscriptContentRendererTest` agent/tool malformed-fence integration; `AgentFenceNormalizationConstructionTest` CI allowlist guard.

### ADR alignment

- No ADR — presentation-layer text repair aligned with ADR 0001 transcript UX goals.

### Dependencies

- Landed after [acp-transcript-content-renderer](../acp-transcript-content-renderer/prd.md).
- Landed before [acp-transcript-package-restructure](../acp-transcript-package-restructure/prd.md).

## Testing Decisions

### What makes a good test

- String fixtures: input markdown → expected normalized string.
- Idempotency test on all fixtures.
- Streaming simulation: concatenated chunks → same result as normalize(fullText).
- `\r\n` input fixture.

### Modules tested

- `TranscriptAgentFenceNormalizer` — expanded tests.
- `TranscriptContentRenderer` — agent + tool malformed-fence integration.
- `AgentFenceNormalizationConstructionTest` — production allowlist guard.

### Verification

- `./gradlew qualityGate` passes.
- `AgentFenceNormalizationConstructionTest` enforces `normalizeAgentFences(` allowlist.

## Out of Scope

- Full markdown parsing or AST changes.
- Syntax highlighting language resolution (`TranscriptFenceLanguageResolver`).
- PTY transcript.
- Changing cursor character behaviour.
- Streaming markdown parse / widget parity with final render.
- Panel harness fence scenario (planned in [panel integration PRD](../acp-transcript-panel-integration-tests/prd.md)).

## Further Notes

- Grill accepted 2026-08-05.
- Parent consolidation PRD noted shared `TranscriptStreamingCursor` dual ancestry; this PRD addresses the fence half of streaming vs final divergence.
