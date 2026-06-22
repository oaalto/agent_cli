# Warning Hygiene

- **Warning-clean changes:** Code you add or modify must pass the project's warning/lint checks without introducing new warnings.
- **Treat warnings as quality signals:** Do not ignore warning output during implementation and review.
- **Fix before suppressing:** Prefer code changes that remove the warning over suppression directives.
- **Use local, narrow suppression only when unavoidable:** If suppression is necessary, scope it to the smallest possible block and document why.
- **No broad disablement:** Do not disable warnings globally, for entire files, or for wide code regions as a default workaround.


# Warning Hygiene

## Zero-Suppression Policy

**No `@Suppress` annotations are permitted anywhere in the codebase.**

This covers all suppression forms:
- Kotlin `@Suppress("...")` annotations (detekt rule names, compiler warnings, ktlint rule names)
- Detekt `// @Suppress(...)` comment-based suppressions
- Any suppression mechanism that silences a tool warning without fixing the underlying issue

## Rationale

- **Fix, don't hide:** Every suppression masks a real quality signal that can degrade over time.
- **Suppressions rot:** A suppression valid under today's tool config may hide a regression after a dependency or rule update.
- **Narrow doesn't justify:** Even scoped suppressions create audit debt — future readers have no way to verify the suppression is still needed.

## Enforcement

- **Pre-commit hook** (`.git/hooks/pre-commit`, sourced from `scripts/pre-commit`): scans staged files for `@Suppress` and blocks the commit.
- **CI / qualityGate:** The pre-commit check is the enforcement mechanism; `qualityGate` does not have a separate suppression scan. Contributors must run `scripts/pre-commit` (or the installed git hook) before push.
- **Workarounds:** If a compiler or tool limitation genuinely requires suppression, file an issue to track removal and use `--no-verify` only as a temporary bypass. Such cases are exceptional and require review.

## Migration (legacy code)

Existing `@Suppress` annotations in the codebase must be removed as part of the first change that touches each file. Do not add new suppressions.
