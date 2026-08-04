# 02 — Tool call row adapter

**Parent:** `prd.md`

**What to build:** Extract `ToolCallRowAdapter` as a thin wrapper around the existing collapsible tool panel. Tool cards expand, collapse, lazy-build bodies, and dispose highlighted code editors exactly as today — but creation, update, and disposal dispatch through the coordinator instead of inline factory branches. `onToolToggle` is injected at adapter construction, not via `RowContext`.

**Blocked by:** 01 — Row adapter scaffold and simple text extraction

**Status:** done

- [x] `ToolCallRowAdapter` `matches` on collapsible tool panel + `ToolCallBlock`; `create`/`update`/`dispose` delegate to existing panel `bind` and code-component disposal
- [x] Coordinator dispatches tool blocks to `ToolCallRowAdapter`; inline tool handling removed from factory
- [x] Per-row column-width resize listener and shared sizing helpers unchanged in behaviour
- [x] Headless EDT tests mount tool adapter output: collapsed default, expand reveals body, child counts stable across update, code editors disposed on `dispose`
- [x] `./gradlew qualityGate` passes; zero visible tool-card UX change
