## Rules index

| key | apply | scope | path |
| --- | --- | --- | --- |
| workflow-gates | scoped | "**/*" | .agents/rules/workflow-gates.md |
| documentation | scoped | "**/*" | .agents/rules/documentation.md |
| testing | scoped | "**/test/**" | .agents/rules/testing.md |
| dependency-boundaries | scoped | "**/*" | .agents/rules/dependency-boundaries.md |
| api-design-basics | scoped | "**/*" | .agents/rules/api-design-basics.md |
| result-handling | scoped | "**/*" | .agents/rules/result-handling.md |
| logging-practices | scoped | "**/*" | .agents/rules/logging-practices.md |
| runtime-handoff | scoped | "**/*" | .agents/rules/runtime-handoff.md |
| warning-hygiene | scoped | "**/*" | .agents/rules/warning-hygiene.md |
| current-state | scoped | "**/*" | .agents/rules/current-state.md |
| changelog | always | — | inline |
| code-format | scoped | "**/*" | .agents/rules/code-format.md |
| commit | always | — | inline |
| decision-making | always | — | inline |
| functional-programming | scoped | "**/*" | .agents/rules/functional-programming.md |
| role | always | — | inline |
| signature | always | — | inline |
| strict-output-execution | always | — | inline |
| vertical-slice-boundaries | scoped | "**/slices/**" | .agents/rules/vertical-slice-boundaries.md |
| domain-language | scoped | "**/*" | .agents/rules/domain-language.md |
| adr-discipline | scoped | "**/*" | .agents/rules/adr-discipline.md |
| wiki-consultation | scoped | "**/*" | .agents/rules/wiki-consultation.md |
| definition-of-done | always | — | inline |

## How project rules apply

Precedence (highest first):

1. Explicit user instructions in the current conversation
2. Task-scoped rules whose scope matches files you are changing
3. Global rules marked **always** in the Rules index (inlined below)
4. Default agent behavior

### Scoped rule loading policy

Before editing files in a task, **load** (read) each scoped rule file from the **Rules index** whose **scope** matches your target paths. Use the path in the index — do not assume scoped rule text from memory. Always-apply rules inlined below are in effect without a separate read.

### changelog

# Changelog

When you change any of the files in the repository, update [`CHANGELOG.md`](CHANGELOG.md) in the **same change**.
## What to record

- **What**: files/areas touched and user-visible behavior.
- **Why**: the problem solved, product intent, or constraint; one short sentence per bullet is enough.
- **Who**: include user attribution as `made by: <actual user name>`, but replace the placeholder with the real name before writing the changelog (for example: `made by: Olli Aalto`).
- **AI metadata (if available)**: include the assisting agent/tool and model, e.g. `made with: Cursor`, `model: gpt-5.3-codex-high`. If unknown, do not guess.
## What **not** to record

- Do **not** record changes to files which are ignored by `.gitignore`.
## How to update

1. **Read `.gitignore` first.** Before writing or appending a changelog entry, read the repo-root [`.gitignore`](.gitignore). For each changed path, verify it is not ignored (prefer `git check-ignore -v <path>` when git is available). Skip the changelog update entirely if the change only touches ignored paths. For mixed changes, log only non-ignored paths and do not mention ignored paths in bullets.

2. **Use one date block per calendar day.** At most one `## YYYY-MM-DD` header per calendar day (no `(previous)` suffixes, no duplicate date headers). If today's section already exists at the top of the file, append bullets to it — do not add another date header.

3. **Use one category heading per date.** Under each date, use at most one of each category heading, in this fixed order (omit empty categories):
   - `### Added`
   - `### Changed`
   - `### Fixed`
   - `### Removed`
   - `### Security`
   - `### Documentation`

   Do not use other `###` headings under a date block. Use **Documentation** for wiki, CONTEXT, ADR, and other docs-only changes with no runtime behavior change; if a change is both code and docs, use the behavioral category and mention docs in the bullet.

4. **Canonical skeleton** (omit empty categories):

   ```markdown
   ## YYYY-MM-DD

   ### Added

   - **Short title** (`package/`): What changed and why. made by: Name. made with: Tool. model: id

   ### Changed

   - ...

   ### Fixed

   - ...

   ### Removed

   - ...

   ### Security

   - ...

   ### Documentation

   - ...
   ```

5. **Parallel branches and merges.** Keep a stable per-day skeleton so parallel branch updates merge cleanly: append new bullets under the matching category in today's existing block. If a merge leaves duplicate `## YYYY-MM-DD` blocks or duplicate category headings, consolidate them into one date block with one heading per category before finishing the merge. Preserve existing bullet text; do not reword historical entries during consolidation.

6. **Other rules.** New dated sections go at the **top** of the changelog (dated sections only — no `Unreleased` section). Call out **breaking changes** explicitly. Do **not** log secret values, API keys, or internal credentials. Do **not** leave literal placeholders in entries (for example `<actual user name>`). If the real user name is unknown, ask the user.

Completion requirements for changelog updates are in the `definition-of-done` rule.

### commit

# Commit Strategy

When the user asks for commit(s), organize changes into logical, self-contained commits.

- **Split unrelated changes:** If edits address different concerns (for example feature work, refactors, docs-only updates), use multiple commits.
- **Keep commits cohesive:** Each commit should represent one clear intent and avoid mixing unrelated modifications.
- **Prefer reviewable units:** Structure commits so each can be reviewed, understood, and reverted independently.
- **Preserve buildability:** Avoid commit splits that leave intermediate commits in a broken or inconsistent state.
- **Combine when cohesive:** If changes are tightly coupled and serve one purpose, keep them in a single commit.

## Before Committing

- Group files by intent/scope.
- Confirm no unrelated files are included in each commit.
- Use clear commit messages that describe the purpose of each group.

### decision-making

# Decision Making and User Guidance

- **Multiple implementation choices:** When multiple valid approaches exist, present the options and ask the user which one they prefer.
- **No assumptions:** Do not assume one choice is better without user input. Trade-offs depend on project constraints, performance goals, and architecture.

### role

# Role

Act as a highly-skilled professional software engineer.

- Deliver high-quality, robust code.
- Be proactive about preventing regressions.
- Verify that new changes do not break existing behavior unless the behavior change is intentional.
- Keep answers and internal reasoning concise and to the point.
- Prefer surgical, minimal changes; avoid scope creep and unnecessary edits.
- If unexpected file changes are present, assume they are intentional user edits and do not revert them.

### signature

# Signature attribution

When output includes an AI-agent/tool signature or attribution (for example Cursor, Claude, Gemini, ChatGPT, Copilot, or similar), also include a user attribution line.

Use this format:

`made by: <actual user name>`

## Trigger examples

- `made with: ...`
- `built with: ...`
- `generated by: ...`
- `powered by: ...`
- `authored by: ...`
- Any signature/credits/footer text that attributes output to an AI agent or model/tool

## Formatting guidance

- Keep the user attribution in the same signature/credit/footer area.
- Match the surrounding style when possible (case, punctuation, separators).
- Do not remove existing attribution; add user attribution alongside it.
- If the actual user name is unknown, ask for it instead of inventing one.

### strict-output-execution

# Strict Output and Execution

## 1. Strict Output Rules

- Never greet, apologize, or explain reasoning.
- Output only the requested artifact.
- Return only raw code or modified blocks.
- Omit conversational filler before, during, or after code blocks.
- Never praise the user for raising issues, asking for reasoning, or clarifying questions.

## 2. Format Expectations

- **Bad:** "Here is the updated function..." `[code]` "I fixed the loop. Let me know if you need anything else!"
- **Good:** `[code]`
- Keep code self-documenting and comments brief.
- Do not write comments that state the obvious.

## 3. Context and Scope

- Restrict context to the immediate task.
- Prefer surgical and small changes to the codebase; avoid scope creep and unnecessary changes.
- Pull external documentation from raw markdown endpoints or `llms.txt` files when possible, instead of standard HTML pages.

## 4. Execution and Verification

- Prefer `rg` filtering first when reading large files, command output, or test logs.
- Run `[INSERT_TEST_COMMAND_HERE]` to verify modifications.
- Upon successful execution, terminate the response immediately with `DONE`.
- Never summarize test results or verification steps.
- When unexpected file changes appear, assume they are intentional user edits and never revert them.

### definition-of-done

# Definition of Done

Work is incomplete until required surrounding updates for this change are done.
## Refactoring

- Update related legacy code to use introduced patterns/utilities when required for correctness or consistency.
## Changelog

- Update `CHANGELOG.md` in the same change when repository files change. See the `changelog` rule for format and `.gitignore` filtering.
## Wiki

- When durable knowledge changes: run `/wiki-update` or append an `update`, `ingest`, or `skip` entry to `docs/wiki/log.md`.
- Before commit, check `docs/wiki/path-map.json` when present and run mechanical wiki lint when available.
- See the `wiki-consultation` rule and the `wiki` skill for operations and evidence rules.
## Documentation

- Keep docs aligned with behavior changes. See the `documentation` rule when editing.
## Tests

- Cover new behavior and regressions; run the project test suite. See the `testing` rule when editing tests.
## Workflow gates

- Format → build/typecheck → lint → test must pass before marking done. See the `workflow-gates` rule.

## Agent skills

### Issue tracker

Planning artifacts in Git; aligns with `/to-prd` + `docs/prd/`. See `docs/agents/issue-tracker.md`.

### Triage labels

Canonical triage roles mapped to this repo's tracker labels. See `docs/agents/triage-labels.md`.

### Domain docs

Single `CONTEXT.md` at repo root. See `docs/agents/domain.md`.
