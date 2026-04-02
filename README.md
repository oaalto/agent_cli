# Agent CLI

`Agent CLI` is an IntelliJ plugin that runs your local agent command inside a dedicated IDE tab.

It is useful when you want to keep agent runs inside your project workspace, with a terminal experience directly in IntelliJ.

## What this plugin does

Ship faster without leaving IntelliJ. `Agent CLI` gives you a focused, in-editor control center for local AI coding workflows, from quick runs to isolated worktree sessions.

### Version 1.0

- Define one or more agent configurations in IDE settings.
- Pick the active agent from the toolbar using `Select Agent`.
- Launch the agent in a dedicated `Agent` tab backed by IntelliJ Terminal.

### Version 2.0

- Upgrades `Run Agent` to a split action:
  - Main click runs immediately in the current project.
  - Dropdown unlocks worktree actions (new, open/resume, delete).
- Adds managed Git worktree workflows for cleaner isolation and parallel agent runs.
- Adds multi-CLI resume support (Cursor, Claude, Gemini, Codex) with per-tool resume semantics.
- Adds first-class WSL execution targeting for Windows setups.

Each configuration supports:

- `Name` - label shown in the toolbar/tab
- `Execution Target` - where to run the command (`LOCAL` or `WSL`)
- `WSL Distribution` - optional distro name when target is `WSL` (for example `Ubuntu`)
- `Binary Path` - command path (required)
- `Arguments` - optional CLI args
- `Working Directory` - optional directory (defaults to current project directory)

## Installation

1. Open this repository's **Releases** page.
2. Download the plugin asset (`.zip` or `.jar`) from the latest release.
3. In IntelliJ, open `Settings -> Plugins`.
4. Click the gear icon and choose `Install Plugin from Disk...`.
5. Select the downloaded file and restart IntelliJ.

## How to use

1. Open `Settings -> Tools -> Agent CLI`.
2. Add a configuration with at least a valid `Name` and `Binary Path`.
3. Mark one configuration as `Default` (if you have multiple).
4. In the toolbar, choose the configuration from `Select Agent`.
5. Click `Run Agent` (main click) to run in the current project.
6. Use the `Run Agent` dropdown for new worktree runs and reopening/resuming previous worktree runs.
7. Use the opened `Agent` tab to interact with the running command.

### Worktree behavior

- The plugin currently supports worktree runs for single-repository projects only.
- New worktrees are created as sibling directories of the repository root under:
  - `<repoRootParent>/<repoName>-agent-worktrees/`
- New branch naming follows:
  - `agent/<config-slug>/<timestamp>`
- The plugin keeps created worktrees until you delete them from the `Run Agent` dropdown.
- Nested worktrees inside the project are intentionally avoided.

### Session resume support

- Worktree creation/open works for any configured agent CLI.
- Session resume is currently supported for:
  - Cursor CLI binaries: `cursor-agent`, `agent` (`--continue`)
  - Claude Code binary: `claude` (`--continue`)
  - Gemini CLI binary: `gemini` (`--resume`)
  - OpenAI Codex binary: `codex` (`resume --last`)
- If the selected configuration is unsupported for resume, the dropdown shows `Open ...` entries instead of `Resume ...`.
- For Cursor CLI resume, if no previous chats are found, the plugin automatically falls back to a normal non-resume start.

### Running in WSL2 on Windows

To run a Linux-installed agent from a Windows IDE:

1. Set `Execution Target` to `WSL`.
2. Set `Binary Path` to the Linux binary path (for example `/usr/local/bin/codex`).
3. Optionally set `WSL Distribution` (for example `Ubuntu-24.04`) to target a non-default distro.
   - If this is empty and your project/working directory is a UNC WSL path (`\\wsl.localhost\\...`), the distro is inferred automatically.
4. Set `Working Directory` to one of:
   - Linux path (for example `/home/you/project`)
   - WSL UNC path (for example `\\wsl.localhost\Ubuntu\home\you\project`)
   - Windows path (for example `D:\project`, mapped to `/mnt/d/project`)

When `Execution Target` is `WSL`, the plugin launches:

- `wsl.exe [--distribution <distro>] --cd <linuxDir> -- <linuxBinaryPath> <args...>`

## Screenshots

Settings page:

![Agent CLI Settings](docs/Screenshot%20Settings.png)

Running agent tab:

![Agent CLI Running](docs/Screenshot%20Agent.png)

## Requirements and notes

- Compatible with IntelliJ builds from `253` and newer.
- The configured binary must exist and be executable.
- If `Working Directory` is empty, the plugin uses the current project root.
- For `WSL` target, Linux binary/path validation happens at runtime inside WSL.

## Developer documentation

Developer and contributor docs are in `docs/development.md`.

## License

This project is licensed under the Apache License 2.0. See `LICENSE`.