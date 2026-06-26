---
name: to-issues
description: Break a plan, spec, or PRD into independently-grabbable issues using tracer-bullet vertical slices and save them under docs/issues/<feature_name>/. Use when user wants to convert a plan into issues, create implementation tickets, or break down work into issues.
---

# To Issues

Break a plan into independently-grabbable issues using vertical slices (tracer bullets).

## Project context (read before writing)

This repo is the **Agent CLI** IntelliJ Platform plugin: it runs external agent CLIs in a dedicated editor tab (PTY passthrough or ACP client mode). Product home is in-tab UX — not JetBrains AI Chat.

**Consult these before exploring or naming things:**

| Need | Read |
| --- | --- |
| Domain vocabulary | [`CONTEXT.md`](../../../CONTEXT.md) at repo root — use glossary terms exactly; do not invent synonyms |
| Architecture & ADRs | [`docs/wiki/subsystems/architecture.md`](../../../docs/wiki/subsystems/architecture.md), [`docs/adr/`](../../../docs/adr/) |
| Wiki index | [`docs/wiki/index.md`](../../../docs/wiki/index.md), [`docs/wiki/path-map.json`](../../../docs/wiki/path-map.json) |
| Planning conventions | [`docs/agents/issue-tracker.md`](../../../docs/agents/issue-tracker.md) |
| Triage labels | [`docs/agents/triage-labels.md`](../../../docs/agents/triage-labels.md) |
| Quality gates | [`docs/wiki/workflows/quality-gate.md`](../../../docs/wiki/workflows/quality-gate.md) — slices should assume `./gradlew qualityGate` for verification |
| Domain doc policy | [`docs/agents/domain.md`](../../../docs/agents/domain.md) |

**Vertical slices** under `com.oaalto.agent` — place new work in the slice that owns the concern:

- `pty/` — PTY Passthrough editor and launch path
- `acp/` — ACP client, session loop, transcript/shell UI
- `worktree/` — Git worktree orchestration (mode-agnostic)
- `settings/` — Shared configuration and launch mode

**Hard boundaries from accepted ADRs:**

- Plugin implements its own ACP **client** in-process; do not delegate to JetBrains AI Chat.
- ACP transport uses the Kotlin ACP SDK in `agent/acp/`.
- **Agent configuration** is IDE-wide (`agentSettings.xml`); **selected agent** is per-`Project` via `AgentConfigurationSelector`.
- `agentSettings.xml` is source of truth; `acp.json` is import/export only.
- Worktrees store `acpSessionId` for ACP resume; PTY uses CLI flag-based resume.

If a slice contradicts an ADR, call it out explicitly and say why reopening may be warranted.

## Process

### 1. Gather context

Work from whatever is already in the conversation context. If the user passes a PRD or slice path (for example `docs/prds/<feature_name>/prd.md`), read it in full. If they pass an external issue URL, treat it as supplementary historical context only.

Derive `<feature_name>` from the PRD directory, user input, or a short kebab-case slug from the feature (must match the PRD folder when one exists).

### 2. Explore the codebase (optional)

If you have not already explored the codebase, do so to understand the current state of the code. Read `CONTEXT.md`, relevant ADRs, and up to 3 wiki pages matching the feature area. Issue titles and descriptions should use the project's domain glossary vocabulary, and respect ADRs in the area you're touching.

### 3. Draft vertical slices

Break the plan into **tracer bullet** issues. Each issue is a thin vertical slice that cuts through ALL integration layers end-to-end, NOT a horizontal slice of one layer.

Slices may be **HITL** or **AFK**. HITL slices require human interaction, such as an architectural decision or a design review. AFK slices can be implemented and merged without human interaction. Prefer AFK over HITL where possible.

<vertical-slice-rules>
- Each slice delivers a narrow but COMPLETE path through every layer (UI, protocol, persistence, tests as applicable)
- A completed slice is demoable or verifiable on its own
- Prefer many thin slices over few thick ones
- Name which vertical slice owns the work (`pty/`, `acp/`, `worktree/`, `settings/`)
</vertical-slice-rules>

### 4. Quiz the user

Present the proposed breakdown as a numbered list. For each slice, show:

- **Title**: short descriptive name
- **Slug**: kebab-case filename stem for `<slice-slug>.md`
- **Type**: HITL / AFK
- **Blocked by**: which other slices (if any) must complete first
- **User stories covered**: which user stories this addresses (if the source material has them)

Ask the user:

- Does the granularity feel right? (too coarse / too fine)
- Are the dependency relationships correct?
- Should any slices be merged or split further?
- Are the correct slices marked as HITL and AFK?

Iterate until the user approves the breakdown.

### 5. Save the slices

For each approved slice, write a markdown file using the template below.

**Save location:**

```
docs/issues/<feature_name>/<slice-slug>.md
```

- Create the directory if it does not exist.
- Use kebab-case for `<feature_name>` and `<slice-slug>`.
- Do **not** publish to GitHub/GitLab unless the user explicitly redirects — this repo's issue tracker is the markdown tree under `docs/prds/` and `docs/issues/`.
- Write slices in dependency order (blockers first) so **Blocked by** can reference real sibling paths.
- AFK slices: set triage to `ready-for-agent`. HITL slices: set triage to `ready-for-human`.
- After saving, list the paths so the user can review.
- Do NOT close or modify the parent PRD unless the user asks.

## Slice template

<issue-template>

## Status

One of: `draft` | `ready-for-agent` | `ready-for-human` | `done` | `wontfix`

Default AFK slices to `ready-for-agent`; HITL slices to `ready-for-human`.

## Parent

Link to the PRD: `docs/prds/<feature_name>/prd.md`

Omit this section only when there is no parent PRD.

## What to build

A concise description of this vertical slice. Describe the end-to-end behavior, not layer-by-layer implementation.

Note which vertical slice owns the work (`pty/`, `acp/`, `worktree/`, `settings/`).

Avoid specific file paths or code snippets — they go stale fast. Exception: if a prototype produced a snippet that encodes a decision more precisely than prose can (state machine, reducer, schema, type shape), inline it here and note briefly that it came from a prototype. Trim to the decision-rich parts — not a working demo, just the important bits.

## Acceptance criteria

- [ ] Criterion 1
- [ ] Criterion 2
- [ ] Criterion 3
- [ ] `./gradlew qualityGate` passes

## Blocked by

- Path to blocking slice (for example `docs/issues/<feature_name>/<other-slice-slug>.md`)

Or "None - can start immediately" if no blockers.

</issue-template>
