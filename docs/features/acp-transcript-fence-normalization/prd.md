## Status

ready-for-agent

**Triage:** `ready-for-agent`

## Problem Statement

Agent markdown fence normalization — repairing malformed ` ``` ` lines from streaming LLM output (merged language tags, citation-style `line:line:path` fences, mid-line openings, inline closings) — runs on **two divergent paths**:

- **Streaming path:** `TranscriptBlockLabelBinder` applies `normalizeAgentFences` to plain text shown in a `JTextPane` with a cursor character appended.
- **Final path:** `TranscriptMarkdownRenderer` applies normalization (or partial normalization) before IntelliJ markdown parse when building `FinalAgentText` body parts.

When a new fence edge case is fixed in one path only, users see the streaming preview layout disagree with the finalized render — or code blocks appear after finalize that were inline text while streaming. CHANGELOG history shows repeated fence fixes touching both modules independently. The **interface** for “agent text ready for display or parse” is shallow; **locality** is split across binder and renderer.

## Solution

Unify fence normalization behind one **deep module** — `TranscriptAgentFenceNormalizer` (existing name, deepened) — with a single public entry:

```kotlin
internal fun normalizeAgentMarkdownFences(text: String): String
```

Both streaming binder and final markdown renderer call this entry exclusively. Normalization rules, citation regex tables, and language-prefix ordering live in one file with one test suite. No duplicate pre-parse hooks.

Optional: expose `normalizeForStreamingDisplay(text)` as alias only if streaming needs cursor-safe behaviour — default is one function unless measurement proves divergence.

Document normalization invariants in module KDoc:

1. Idempotent on already-valid GFM fences.
2. Preserves non-fence prose verbatim except required splits.
3. Safe on partial input during streaming (no throws on unclosed fence).

## User Stories

1. As a developer fixing citation-style ` ```3:10:path/File.kt` fences, I want one function to patch, so that streaming and final renders match after the fix.
2. As a developer fixing merged opening fences (` ```kotlinfun main`), I want tests in one module, so that I do not duplicate fixtures in binder and renderer tests.
3. As a user watching streaming agent output, I want fence previews to match the finalized code blocks, so that layout does not jump on prompt completion.
4. As a user reading Kotlin agent output with Cursor citation fences, I want syntax highlighting in final render, so that normalized fences carry correct language ids.
5. As a developer adding a new malformed fence pattern from production logs, I want a single regression test case, so that both paths inherit it automatically.
6. As a maintainer, I want normalization applied before content renderer parse in one documented order: normalize → render, so that pipeline diagrams are accurate.
7. As a developer on `TranscriptContentRenderer`, I want to call normalization as the first pipeline stage, so that tool and agent text share fence rules when applicable.
8. As a user with mid-line fence openings (`text before ```kotlin`), I want the split to appear during streaming, not only after finalize, so that long responses remain readable while streaming.
9. As a developer debugging inline closing fences, I want idempotent normalization, so that double-application during stream chunks does not corrupt text.
10. As an AI agent, I want wiki to reference one normalizer module, so that fence behaviour is discoverable.
11. As a reviewer, I want grep to show zero duplicate fence regex outside normalizer, so that drift is structurally impossible.
12. As a user pasting agent output with Windows line endings, I want normalization to handle `\r\n`, so that fences split correctly cross-platform.
13. As a developer writing property tests, I want normalize(normalize(x)) == normalize(x), so that streaming chunk boundaries can re-normalize safely.
14. As a product owner, I want fewer fence-related changelog entries after unification, so that maintenance cost drops.
15. As a developer coordinating finalize policy work, I want finalize to trigger re-render through the same normalized text stream, so that finalize does not change fence semantics.
16. As a user reading tool card text bodies with fences, I want optional application of agent fence rules to tool text when patterns match, so that tool output benefits from the same repairs (behind same function, gated by caller).
17. As a tester, I want fixtures from prior `TranscriptAgentFenceNormalizer` tests consolidated, so that coverage is not lost in migration.
18. As a developer, I want malformed partial fences at stream end to render best-effort plain text, so that normalization never blanks the streaming pane.

## Implementation Decisions

### Ownership

- **Vertical slice:** ACP transcript text preprocessing — `TranscriptAgentFenceNormalizer` deepened; binder and markdown renderer become callers only.

### Call graph (target)

```
StreamingAgentText → bindStreamingAgent → normalizeAgentMarkdownFences → JTextPane text
FinalAgentText     → content renderer   → normalizeAgentMarkdownFences → markdown parse
```

### Modules to modify

- **Normalizer:** consolidate all regex/split helpers; export single entry point.
- **Block label binder:** remove inline duplicate logic; call normalizer.
- **Markdown renderer / content renderer:** remove duplicate pre-parse; call normalizer before parse.

### Streaming chunk safety

- Normalizer must tolerate incomplete trailing fences (no closing ```) — return text unchanged or partially repaired without throwing.
- Document behaviour for unclosed fence at stream end in tests.

### Tool card text

- Tool text bodies may call the same normalizer when content is markdown-like; gate via content renderer options (`applyAgentFenceNormalization: Boolean`) default true for agent, heuristic or true for tool text bodies.

### Seam for testing

**Primary test seam:** `normalizeAgentMarkdownFences` — pure string in/out, no Swing, no parser.

Secondary: a small integration test through content renderer verifying normalized fences become `TranscriptBodyPart.Code` with expected language.

### ADR alignment

- No ADR conflict — presentation-layer text repair. Aligned with ADR 0001 transcript UX goals.

### Dependencies

- **Land after** [acp-transcript-content-renderer](../acp-transcript-content-renderer/prd.md): content renderer calls `normalizeAgentMarkdownFences` when `ContentRenderOptions.applyFenceNormalization` is true; fence PRD then makes streaming binder and final path share that entry exclusively.
- Can land first as mechanical consolidation with immediate regression value only if content renderer does not yet exist — preferred order is content-renderer first with internal normalizer call, then fence PRD deletes duplicate hooks.

## Testing Decisions

### What makes a good test

- String fixtures: input markdown → expected normalized string; cover citation, merged open/close, mid-line open, inline close cases from CHANGELOG history.
- Idempotency test on all fixtures.
- Streaming simulation: concatenate chunks, normalize full buffer vs incremental — document expected strategy (normalize full accumulated text each bind).

### Modules to test

- `TranscriptAgentFenceNormalizer` — expand existing tests; absorb duplicate cases from markdown renderer tests if any.

### Prior art

- Existing `TranscriptAgentFenceNormalizer` unit tests (if present) and changelog-driven edge cases.
- `TranscriptMarkdownRendererTest` fence cases — ensure they pass through normalizer first.

### Verification

- `./gradlew qualityGate` passes.
- Grep audit: fence regex literals only in normalizer module.

## Out of Scope

- Full markdown parsing or AST changes.
- Syntax highlighting language resolution (`TranscriptFenceLanguageResolver`) — separate concern after normalization yields clean fence headers.
- PTY transcript.
- Changing cursor character behaviour in streaming binder.
- New fence syntax from agents not yet seen in production (only consolidate existing rules).

## Further Notes

- Architecture review strength: **Worth exploring** — frequent changelog touch area; unification reduces dual-fix class.
- Parent consolidation PRD noted shared `TranscriptStreamingCursor` dual ancestry; this PRD addresses the fence half of streaming vs final divergence.
