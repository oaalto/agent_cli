## Status

implemented

## Problem Statement

Users and developers troubleshooting Agent CLI sessions lack discoverable guidance for where conversation history and diagnostic detail live on disk.

**Session diagnostics** (tiered output in the IDE log file `idea.log`) is documented only by filename in glossary and ADR material — not how to open the log from the IDE. Users must already know IntelliJ's **Help → Show Log in Explorer** (or macOS **Show Log in Finder**) workflow.

**Session transcript file** persistence (ACP Client only) writes plain-text conversation snapshots under each project's `.idea/agent-cli/transcripts/` directory. That path is defined in ADR 0004 and implemented by the transcript file store, but there is no in-product way to open the folder. Users who configure agents in **Tools → Agent CLI** settings have no pointer to transcript storage or to the fact that **Terminal (PTY Passthrough)** launch mode does not use plugin transcript files.

Copy session diagnostics and correlation-token grep workflows exist in the ACP editor, but settings — the natural place to learn about logging and file locations — is silent.

## Solution

Add a fixed **Session logs & transcripts** help section to the Agent CLI settings page, placed **below the agent configuration table and above the per-row detail panel** (MCP toggles, environment variables). The section is always visible and independent of which agent row is selected.

### Session diagnostics (IDE log)

A disabled-foreground hint label (matching existing MCP/env hint styling) explains:

- Plugin **Session diagnostics** are written to the IDE log file `idea.log`.
- Open the log folder via **Help → Show Log in Explorer** (macOS: **Show Log in Finder**).
- Grep for `[agent-cli:…]` to match transcript error lines to log detail.

No button — the platform Help menu is the canonical affordance. No filesystem path documentation (sandbox vs installed IDE paths remain platform-defined).

### Session transcript file

A hint label plus **Open transcript folder** button:

- Explains **Session transcript file** is **ACP Client** only; files live at `.idea/agent-cli/transcripts/` (workspace-local, not VCS).
- Explains **Terminal (PTY Passthrough)** does not create plugin transcript files — use terminal scrollback or the external agent CLI's own history.
- Shows the **focused open project** name when a project with a base path is available (e.g. "for the focused project **agent_cli**").
- When no project is open: hint says to open a project; button disabled.

Button behavior:

- Resolve transcript directory for the focused open project's base path (reuse the same relative directory constant as the transcript file store).
- Create the directory if missing, then reveal it in the system file manager (platform reveal-in-explorer action).
- On I/O or reveal failure: show a short error dialog; do not crash settings.

Button label enabled state refreshes when settings UI is shown or when project focus may have changed (refresh on settings panel open is sufficient for v1).

### Documentation boundary

This feature delivers **settings UI copy and open-folder behavior only**. No updates to `CONTEXT.md`, ADR, wiki, or developer guide for log paths.

## User Stories

1. As a user configuring agents in **Tools → Agent CLI**, I want to see how to open the IDE log, so that I can find **Session diagnostics** without searching JetBrains documentation.
2. As a user who sees `[agent-cli:xxxx]` on a transcript error, I want settings to remind me to grep `idea.log` for that token, so that I can connect user-visible errors to developer log detail.
3. As a user running **ACP Client** sessions, I want settings to explain where **Session transcript file** entries are stored, so that I can read saved conversations outside the editor.
4. As a user with a project open, I want a button that opens the transcript folder for that project, so that I do not have to navigate `.idea/agent-cli/transcripts/` manually.
5. As a user with multiple projects open, I want the transcript button to target the **focused** project, so that I open the folder for the workspace I am actually working in.
6. As a user with no project open, I want the transcript button disabled with a clear message, so that I understand why open-folder is unavailable.
7. As a user who has never run an ACP session, I want **Open transcript folder** to still work by creating the directory, so that I can confirm where files will appear.
8. As a user running agents in **Terminal (PTY Passthrough)** mode, I want settings to state that plugin transcript files are not created, so that I do not hunt for missing `.txt` files.
9. As a user in **Terminal** mode, I want settings to point me to terminal scrollback or agent-side history, so that I know where conversation records live for that launch mode.
10. As a developer supporting users, I want observability pointers in settings rather than only in ADR/glossary, so that self-service troubleshooting reduces support noise.
11. As a user, I want the logs/transcripts section separate from per-agent MCP and env settings, so that observability help is not tied to the selected configuration row.
12. As a user on macOS, I want the IDE log hint to mention **Show Log in Finder**, so that the menu label matches my platform.
13. As a user on Windows, I want the IDE log hint to mention **Show Log in Explorer**, so that the menu label matches my platform.
14. As a user, I want hint text styled like existing settings hints (muted/disabled foreground), so that the panel visually matches MCP and environment help.
15. As a user opening settings from any project context, I want transcript path help to use domain terms **Session transcript file** and **Session diagnostics**, so that wording matches the rest of the plugin and ADR 0004.
16. As a user, I do not want settings to document `AGENT_CLI_LOG` or registry debug toggles in this section, so that the panel stays focused on finding logs and files.
17. As a user, I do not want a second button to open the IDE log folder, so that the UI does not duplicate the platform Help menu.
18. As a developer, I want transcript directory resolution centralized on the existing file-store path constant, so that settings and persistence never diverge on path shape.
19. As a user whose focused project has no filesystem base path, I want the transcript button disabled, so that the plugin does not throw when opening settings.
20. As a user who clicks **Open transcript folder** when reveal fails, I want a readable error message, so that I know the operation did not succeed silently.

## Implementation Decisions

### Placement and layout

- Extend the Agent CLI settings root layout from two regions (table toolbar center, detail south) to three: table toolbar center, **observability help** between table and detail, detail south unchanged.
- Observability block is **not** part of the per-row detail panel; it does not change when the user selects different agent configurations.

### UI components

- Reuse the existing pattern: `JBLabel` hints with `JBUI.CurrentTheme.Label.disabledForeground()`.
- Add a horizontal row or small panel: transcript hint label(s) plus `JButton("Open transcript folder")`.
- Optional section title label (e.g. "Session logs & transcripts") or rely on hint content only — implementer may use a single composite hint if a title adds clutter; prefer consistency with detail panel which uses unlabeled hint rows.

### Focused project resolution

- Application-scoped settings have no single project in context; resolve **focused open project** via open-project manager (first open project if multiple — matches agreed grill decision).
- Project display name in hint: use project name (not full base path) for readability.
- Button enabled when `project.basePath` is non-null.

### Transcript directory and reveal

- Expose a public or internal companion on the transcript file store module: `resolveTranscriptsDirectory(projectBasePath): Path` returning the parent directory of session files (not a session-specific file path).
- On button click: `Files.createDirectories` if missing, then reveal via IntelliJ platform API (`ShowFilePathAction`, `RevealFileAction`, or equivalent — use platform convention already available in the IntelliJ Platform dependency).
- Do not add a new dependency for file-manager integration.

### Copy and terminology

- Use **ACP Client** and **Terminal (PTY Passthrough)** / settings display labels **ACP** and **Terminal** consistently with the launch mode combo.
- Use **Session diagnostics** and **Session transcript file** as glossary terms (ADR 0004).
- IDE log hint includes correlation-token grep line; omit tier-2/3 toggle documentation.

### Lifecycle

- Refresh focused-project label and button enabled state when `createComponent` runs and optionally when settings page is reopened (`disposeUIResources` / recreate pattern already used by configurable).
- ponytail: no project-focus listener in v1 — refresh on settings open is enough; upgrade path is subscribe to project manager topic if button state feels stale.

### Modules touched

- Agent settings UI factory (new observability panel builder, layout hook).
- Agent settings configurable (wire panel into root layout; button handler with project resolution and error dialog).
- Transcript file store (directory resolver companion — minimal surface).

## Testing Decisions

### Seam (single primary)

Test at the **transcript directory resolver** seam on the file store companion:

- Given a project base path, resolver returns normalized path ending in `.idea/agent-cli/transcripts`.
- Pure function; no Swing.

Optional second seam if extractable without heaviness: **focused project picker** for settings (given a list of open projects, returns first with base path or null). Only add if button wiring is hard to test otherwise; prefer one seam.

UI layout and hint strings are not unit-tested (manual QA in sandbox IDE).

### What makes a good test

- Assert external behavior (path shape), not Swing hierarchy or label text.
- No headless UI tests for settings configurable in v1.

### Prior art

- `TranscriptFileStoreTest` — temp-directory file I/O and path resolution patterns.
- Settings package historically relies on manual QA; no existing settings UI unit tests.

### Manual QA

1. Open **Tools → Agent CLI** with a project open → hint shows project name; button opens/creates transcript folder.
2. Close all projects → button disabled; hint mentions opening a project.
3. Verify IDE log hint text references Help menu and `[agent-cli:…]` grep.
4. Verify section appears above MCP/env detail panel and does not change when selecting agent rows.

## Out of Scope

- Documenting absolute `idea.log` filesystem paths (sandbox `runIde` vs installed IDE).
- `AGENT_CLI_LOG`, `AGENT_CLI_DEBUG`, or registry toggle help in settings.
- A button or action to open the IDE log folder (Help menu only).
- Wiki, `CONTEXT.md`, ADR, or `docs/development.md` updates.
- Per-project picker UI when multiple projects are open.
- Linking observability section visibility to selected agent launch mode (hints always visible; copy explains ACP vs Terminal).
- Automatic transcript folder open from ACP editor (only settings button).
- Listing individual `acpSessionId` transcript files in settings.

## Further Notes

- Parent observability model: [ADR 0004](../../adr/0004-session-observability.md). This feature is discoverability only; it does not change persistence or logging behavior.
- Complements **Copy session diagnostics** (ACP editor action) and correlation tokens from PR2 transcript persistence — settings is the "where are the files" entry point; editor action remains the "paste this bundle" entry point.
- Grill session decisions (2026-07-24) are authoritative for this spec: focused project, fixed section placement, create-if-missing reveal, minimal IDE log hint plus correlation grep, dynamic project name, settings-only docs.
