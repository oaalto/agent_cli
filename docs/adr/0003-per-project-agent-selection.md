# ADR 0003: Per-project agent selection

- **Status:** Accepted
- **Date:** 2026-06-17

## Context

The toolbar agent selector (`SelectAgentConfigurationActionGroup`) reads and writes `selectedConfigurationId` on `AgentSettingsState`, an application-level persistent component (`agentSettings.xml`). Changing the selected agent in one project window therefore changes it in every other open window — including unrelated repositories.

The same field also drives the Settings UI **Default** column. Toolbar selection and global default are conflated: picking an agent from the toolbar changes what Settings shows as default, and vice versa.

Users expect IDE-wide **agent configuration definitions** (binary paths, launch mode, MCP toggles) to be shared, but the **runtime choice** of which named configuration to run to be scoped to the project they are working in.

## Decision

Split **Default agent configuration** (global) from **Selected agent** (per project).

### Persistence

| Concept | Storage | Scope |
|---------|---------|-------|
| Agent configuration definitions | `agentSettings.xml` via `AgentSettingsState` | Application (IDE-wide) |
| Default agent configuration | `defaultConfigurationId` in `agentSettings.xml` | Application (IDE-wide) |
| Selected agent | Workspace-scoped project state | Per `Project`, user-local (not VCS) |

Rename the former `selectedConfigurationId` in `agentSettings.xml` to `defaultConfigurationId`, with backward-compatible load of the legacy field name.

Per-project selection is stored in a project-level `PersistentStateComponent` using workspace file storage (`StoragePathMacros.WORKSPACE_FILE`).

### Resolution

Introduce `AgentConfigurationSelector` as the single facade for runtime selection. All toolbar, Run Agent, and Open Editor call sites use:

- `getSelectedConfiguration(project)`
- `setSelectedConfiguration(project, id)`

The facade owns the full resolution chain:

1. Read the project's saved configuration ID.
2. If unset, seed from **Default agent configuration** and persist (covers upgrade migration and first access).
3. If the saved ID no longer exists, silently fall back to **Default agent configuration**, then the first available configuration, and rewrite the project's saved ID to the resolved value.
4. Return the resolved `AgentCliConfiguration` from the global catalog.

Changing the toolbar **Selected agent** does **not** update **Default agent configuration**. Settings **Default** column writes only `defaultConfigurationId`.

### Migration

On upgrade from pre-split storage:

- The legacy global `selectedConfigurationId` becomes `defaultConfigurationId`.
- Each project seeds its **Selected agent** from that default on first access when the project has no saved selection yet.

This preserves existing toolbar behavior for current users while fixing cross-window coupling going forward.

## Alternatives considered

1. **Keep global selection, scope by repository root** — Rejected. Surprising when two `Project` instances point at the same repo; does not match IntelliJ project-scoped UI state conventions.
2. **Per-project selection only, no global default** — Rejected. Removes a clear IDE-wide fallback and makes Settings **Default** meaningless.
3. **Team-shared selection in a committed `.idea/` file** — Rejected. Agent choice is personal tooling preference; global config binary paths may differ across machines.
4. **Stale ID: resolve dynamically without rewrite** — Rejected. Leaves ghost state and repeats resolution on every access.
5. **Stale ID: notify user on fallback** — Rejected. Adds UI noise for an edge case; silent rewrite is sufficient.
6. **Split logic across call sites** — Rejected. Fallback, seed, and rewrite rules would drift across toolbar, Run Agent, and Open Editor.
7. **Extend `AgentSettingsState` with project methods** — Rejected. Mixes application-level catalog with per-project selection concerns.

## Consequences

### Positive

- Two project windows can use different selected agents simultaneously
- Settings **Default** and toolbar selection have clear, independent meanings
- Upgrade path preserves current behavior via seed-on-first-access
- Central facade keeps resolution consistent and testable

### Negative

- New project-level service and facade to maintain
- Call sites that today use `AgentSettingsState.getSelectedConfiguration()` must pass `Project`
- Settings UI must stop using runtime selection helpers for the **Default** column

### Neutral

- Worktrees continue to store `configurationId` at creation time; pending launch validation is unchanged
- Open agent editor tabs remain bound to the configuration ID embedded in `AgentVirtualFile`
- Permission memory stays application-scoped in `agentSettings.xml`
