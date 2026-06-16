# install.ps1 fixes

- What changed

- Created a backup of the original script at `.agentic-config/install.ps1.bak`.
- Replaced `.agentic-config/install.ps1` with a hardened version that:
  - Guards against null `requires` arrays to avoid iteration errors.
  - Avoids accidental script termination caused by `Write-Error` inside `catch`/env-check paths; uses non-terminating host output and explicit `exit 1` where appropriate.
  - Makes step invocation safer by supporting structured `exe` + `args` properties on plan steps and falling back to `command` strings when needed.
  - Emits clearer fail/warn output for missing prerequisites and fails explicitly unless `-Force` is provided.

- Why

- The original script used `Write-Error` combined with `$ErrorActionPreference = "Stop"`, which caused the script to abort immediately when a step failed or an env check found missing tools; this prevented later steps from running and produced confusing output.
- Iterating `$step.requires` without checking for null caused potential runtime errors if `install-plan.json` contained steps without a `requires` field.
- Executing raw command strings with `Invoke-Expression` is brittle and can be an injection vector if `install-plan.json` is edited; allowing structured `exe`+`args` reduces that risk and makes argument passing more robust.
- The original generated script also contained a non-ASCII em-dash (`—`) in the `npx` help string which triggers a parse error in some PowerShell environments; that character was present in the backup `.agentic-config/install.ps1.bak` and was replaced with an ASCII hyphen.

- Files changed

- `.agentic-config/install.ps1` — hardened installer logic.
- `.agentic-config/install.ps1.bak` — backup of the original file.
- `CHANGELOG.md` — added a short fixed-entry (keeps repository change history).

made by: Olli Aalto
made with: Cursor
model: gpt-5-mini
