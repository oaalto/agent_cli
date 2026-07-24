# 01 — TranscriptTextSerializer

**Parent:** `prd.md`

**What to build:** A dedicated **TranscriptTextSerializer** that maps an ordered snapshot of **TranscriptModel** blocks to a single plain-text string (newline-separated lines) without depending on Swing. Apply explicit content policy: user prompts as `> …`, agent text as normalized plain lines, plain lines and stderr as shown in the UI (e.g. `[stderr] …`), errors as plain error lines, one header line per tool call (`[tool: name …]` with title/name/status only), omit plan blocks, tool payloads/results/diffs, usage/cost footer, and markdown/HTML structure. Reuse or delegate existing text-extraction helpers where they match user-visible plain meaning. Document thought/reasoning block inclusion or omission as a deliberate serializer policy aligned with **Transcript** plain conventions.

**Blocked by:** None — can start immediately

**Status:** ready-for-agent

- [ ] **TranscriptTextSerializer** converts block lists to plain text per PRD content policy (user prefix, agent text, stderr, errors, tool headers only, plan/payload/usage omission)
- [ ] Serializer is pure: reads block snapshot only; no Swing or editor dependencies
- [ ] Multiple tools, empty model, streaming vs final agent text, and auth-failure plain lines covered by unit tests using **TranscriptModel** test builders (prior art from **TranscriptBlockViewFactory** / **TranscriptModel** tests)
- [ ] Thought/reasoning handling is explicit and tested (included or omitted per documented policy)
- [ ] `./gradlew qualityGate` passes
