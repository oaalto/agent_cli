---
name: to-prd
description: Turn the current conversation context into a PRD and save it under docs/prds/<feature_name>/. Use when user wants to create a PRD from the current context.
---

This skill takes the current conversation context and codebase understanding and produces a PRD. Do NOT interview the user — synthesize what you already know.

## Project context (read before writing)

This repo is the **Agent CLI** IntelliJ Platform plugin: it runs external agent CLIs in a dedicated editor tab (PTY passthrough or ACP client mode). Product home is in-tab UX — not JetBrains AI Chat.

**Consult these before exploring or naming things:**

| Need | Read |
| --- | --- |
| Domain vocabulary | [`CONTEXT.md`](../../../CONTEXT.md) at repo root — use glossary terms exactly; do not invent synonyms |
| Architecture & ADRs | [`docs/wiki/subsystems/architecture.md`](../../../docs/wiki/subsystems/architecture.md), [`docs/adr/`](../../../docs/adr/) |
| Wiki index | [`docs/wiki/index.md`](../../../docs/wiki/index.md), [`docs/wiki/path-map.json`](../../../docs/wiki/path-map.json) |
| Planning conventions | [`docs/agents/issue-tracker.md`](../../../docs/agents/issue-tracker.md) |
| Quality gates | [`docs/wiki/workflows/quality-gate.md`](../../../docs/wiki/workflows/quality-gate.md) — PRDs should assume `./gradlew qualityGate` for verification |
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

If a PRD contradicts an ADR, call it out explicitly and say why reopening may be warranted.

## Process

1. **Gather context** — Work from conversation context. If the user names a feature, use that as `<feature_name>`; otherwise derive a short kebab-case slug from the problem (e.g. `acp-transcript-search`).

2. **Explore the repo** (if not already done) — Read `CONTEXT.md`, relevant ADRs, and up to 3 wiki pages matching the feature area. Use domain glossary vocabulary throughout the PRD.

3. **Sketch modules** — Identify which vertical slices and deep modules will be built or modified. A **deep module** encapsulates substantial functionality behind a simple, stable, testable interface.

   Check with the user that these modules match their expectations. Check which modules they want tests written for.

4. **Write the PRD** using the template below.

5. **Save the PRD** to:

   ```
   docs/prds/<feature_name>/prd.md
   ```

   - Create the directory if it does not exist.
   - Use kebab-case for `<feature_name>`.
   - Do **not** publish to GitHub/GitLab unless the user explicitly redirects — this repo's issue tracker is the markdown tree under `docs/prds/` and `docs/issues/`.
   - After saving, mention the path so the user can review. Downstream work splits via `/to-issues` into `docs/issues/<feature_name>/<slice-slug>.md`.

6. **Changelog** — If the PRD records a new accepted decision or user-visible intent, note in the PRD that implementers should update `CHANGELOG.md` when code ships (do not edit changelog from this skill unless the user asks).

## PRD template

<prd-template>

## Status

One of: `draft` | `in review` | `accepted` | `superseded`

Default new PRDs to `draft`.

## Problem Statement

The problem that the user is facing, from the user's perspective.

## Solution

The solution to the problem, from the user's perspective.

## User Stories

A LONG, numbered list of user stories. Each user story should be in the format of:

1. As an <actor>, I want a <feature>, so that <benefit>

<user-story-example>
1. As a mobile bank customer, I want to see balance on my accounts, so that I can make better informed decisions about my spending
</user-story-example>

This list of user stories should be extremely extensive and cover all aspects of the feature.

## Implementation Decisions

A list of implementation decisions that were made. This can include:

- Which vertical slice(s) own the work (`pty/`, `acp/`, `worktree/`, `settings/`)
- The modules that will be built/modified
- The interfaces of those modules that will be modified
- Technical clarifications from the developer
- Architectural decisions (note ADR alignment or conflicts)
- Schema changes
- API contracts
- Specific interactions

Do NOT include specific file paths or code snippets. They may end up being outdated very quickly.

Exception: if a prototype produced a snippet that encodes a decision more precisely than prose can (state machine, reducer, schema, type shape), inline it within the relevant decision and note briefly that it came from a prototype. Trim to the decision-rich parts — not a working demo, just the important bits.

## Testing Decisions

A list of testing decisions that were made. Include:

- A description of what makes a good test (only test external behavior, not implementation details)
- Which modules will be tested
- Prior art for the tests (i.e. similar types of tests in the codebase)
- Verification expectation: `./gradlew qualityGate` passes after implementation

## Out of Scope

A description of the things that are out of scope for this PRD.

## Further Notes

Any further notes about the feature. Link related ADRs or wiki pages by title/path when useful.

</prd-template>
