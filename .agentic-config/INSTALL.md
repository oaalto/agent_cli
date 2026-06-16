# Install external skills and Pi packages

This bundle was generated with **upstream** skills and/or Pi extensions. Repo-local agent files (`docs/`, rules, skills) are already at the project root; run the installer to pull external dependencies.

## Prerequisites

`install.sh` runs an **environment check** before any install step. It verifies every tool required by your `install-plan.json` and prints docs URLs plus example install commands for anything missing.

| Tool                                   | Needed for                                                                       | Install docs                                                                                      |
| -------------------------------------- | -------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------- |
| `jq`                                   | Reading `install-plan.json`                                                      | https://jqlang.org/download/                                                                      |
| [Node.js](https://nodejs.org/) + `npx` | `npx skills add …` ([vercel-labs/skills](https://github.com/vercel-labs/skills)) | https://nodejs.org/en/download/                                                                   |
| [Pi](https://pi.dev) CLI               | `pi install …`                                                                   | https://pi.dev/docs/latest/usage#cli-reference — `npm install -g @earendil-works/pi-coding-agent` |

Other tools (`curl`, `wget`, `git`) appear only if your plan includes custom shell steps that need them.

## Steps

From your **project root** (parent of `.agentic-config/`):

```bash
chmod +x .agentic-config/install.sh
./.agentic-config/install.sh
```

The script prints `[ok]` or `[MISSING]` for each required tool. Fix missing tools, then run again.

Preview commands without executing (env check still runs):

```bash
./.agentic-config/install.sh --dry-run
```

Skip the environment check (not recommended):

```bash
./.agentic-config/install.sh --skip-env-check
```

Windows (PowerShell, from project root):

```powershell
.\.agentic-config\install.ps1
```

Use `-SkipEnvCheck` to bypass the upfront check on Windows.

## What gets installed

See `.agentic-config/install-plan.json` for the exact command list. Typical entries:

- **Pi packages** — `pi install -l npm:…` (project-local under `.pi/settings.json`)
- **Engineering skills** — `npx -y skills@latest add <owner/repo> --skill <name> --agent <agent> -y`
- **Custom shell** — freeform commands you added in the Agentic Development Configurator (rules fetched via `curl`, hooks, etc.)

Custom **rules** marked “bundle” in the Agentic Development Configurator are already inside the zip (not run by this script).

## After install

- **Pi**: run `/reload` in an interactive session.
- **Cursor**: restart or reload if skills do not appear under `.agents/skills/`.

Edit `docs/agents/*.md` directly later; re-run `./.agentic-config/install.sh` only when adding new upstream skills or Pi packages.

## Cleaning up

Install metadata lives under `.agentic-config/` (`install.sh`, `install.ps1`, `install-plan.json`, `INSTALL.md`, `manifest.json`, and `USAGE.md` when included). The installer does not remove these automatically.

When you no longer need install metadata or onboarding docs:

```bash
rm -rf .agentic-config
```

Keep `.agentic-config/` if you expect to re-run the installer or audit selections via `manifest.json`.
