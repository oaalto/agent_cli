# 01 — Settings session logs and transcripts help panel

**Parent:** `prd.md`

**What to build:** In **Tools → Agent CLI** settings, a fixed **Session logs & transcripts** section appears below the agent configuration table and above the per-row detail panel (MCP, environment variables). It is always visible regardless of which agent row is selected.

**Session diagnostics** hint (muted label, matching existing MCP/env hint style): explains that plugin diagnostics go to `idea.log`; open via **Help → Show Log in Explorer** (macOS: **Show Log in Finder**); grep `idea.log` for `[agent-cli:…]` to match transcript errors. No button for the IDE log.

**Session transcript file** hint plus **Open transcript folder** button: explains ACP Client stores plain-text transcripts under `.idea/agent-cli/transcripts/` (workspace-local, not VCS); Terminal (PTY Passthrough) does not create plugin transcript files — use terminal scrollback or the external agent CLI. When a focused open project with a base path exists, hint includes that project’s display name; button creates the transcript directory if missing and reveals it in the system file manager. When no suitable project is open, hint tells the user to open a project and the button is disabled. I/O or reveal failures show a short error dialog without crashing settings. Button and hint state refresh when the settings page is opened (no live project-focus listener in v1).

Centralize transcript directory path on the existing **Session transcript file** store companion so settings and persistence share one path constant. Unit-test the directory resolver (normalized path ending in `.idea/agent-cli/transcripts`). No wiki, CONTEXT, or ADR updates.

**Blocked by:** None — can start immediately

**Status:** done

- [x] **Session logs & transcripts** section is placed between the agent table toolbar and the per-row detail panel; selecting different agent rows does not change observability copy or controls
- [x] IDE log hint uses disabled-foreground label styling consistent with existing settings hints; copy covers `idea.log`, Help menu path (platform-appropriate Explorer/Finder wording), and `[agent-cli:…]` grep; no IDE-log open button; no `AGENT_CLI_LOG` / registry toggle documentation
- [x] Transcript hint explains ACP Client vs Terminal (PTY Passthrough), `.idea/agent-cli/transcripts/`, and shows focused project name when `basePath` is available
- [x] With no open project (or no project `basePath`), transcript hint explains why and **Open transcript folder** is disabled
- [x] **Open transcript folder** resolves directory via shared transcript file store companion, creates directory if missing, reveals in file manager via platform API; failures show error dialog
- [x] Focused project resolution uses first open project from open-project manager when multiple projects are open
- [x] Transcript directory resolver unit test asserts path shape for a given project base path (prior art: transcript file store tests)
- [x] `CHANGELOG.md` updated for user-visible settings change
- [x] `./gradlew qualityGate` passes
- [x] Manual QA: settings with project open (hint + button work); settings with no project (button disabled); section stable when changing agent row selection
