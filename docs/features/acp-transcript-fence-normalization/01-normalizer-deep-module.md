# 01 — Fence normalizer deep module

**Parent:** `prd.md`

**What to build:** Deepen `TranscriptAgentFenceNormalizer` as the single pure-string seam for agent fence repair. Document invariants in KDoc and expand regression coverage so streaming chunk re-normalization and cross-platform line endings are safe before callers are consolidated.

**Blocked by:** None — can start immediately

**Status:** done

- [x] `normalizeAgentFences(text: String): String` is the sole public entry; citation regex tables and language-prefix ordering live only in the normalizer module
- [x] KDoc documents three invariants: idempotent on valid GFM fences; preserves non-fence prose except required splits; safe on partial input (auto-close trailing unclosed fence at EOF, no throw)
- [x] Existing malformed-fence fixtures remain covered (merged language tags, citation-style fences, mid-line openings, inline closings, prose-only fence markers)
- [x] Idempotency: `normalize(normalize(x)) == normalize(x)` over fixture set
- [x] `\r\n` input normalizes to LF-only output with correct fence splits
- [x] Streaming simulation: incremental chunk concatenation → same result as `normalize(fullText)`
- [x] `./gradlew qualityGate` passes
