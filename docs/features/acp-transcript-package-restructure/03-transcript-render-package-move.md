# 03 — Transcript render package move

**Parent:** `prd.md`

**What to build:** Relocate the full markdown/HTML content-render pipeline (content renderer, markdown renderer, block converter, HTML/table builders, tool-call renderers, fence normalizer, fence language resolver, text truncation, plain-text renderer helpers) into `transcript/render/` with mirrored tests. Update all importers including ingestion (still outside `model/` until ticket 04) and row adapters. `./gradlew qualityGate` proves the render seam is directory-bounded.

**Blocked by:** 01 — Transcript theme package move; 02 — Transcript model core package move

**Status:** done

- [x] All content-render types and tests live under `transcript/render`
- [x] `TranscriptContentRenderer.renderMarkdownText` remains the canonical markdown→body-part entry; callers compile through new package paths
- [x] Fence normalizer construction guard test still passes (filename allowlist unchanged)
- [x] Render package sources contain no `javax.swing` imports
- [x] `./gradlew qualityGate` passes with moves-only diffs
