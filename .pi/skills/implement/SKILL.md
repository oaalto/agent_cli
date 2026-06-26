---
name: implement
description: "Implement a piece of work based on a PRD or set of issues."
disable-model-invocation: true
---

Implement the work described by the user in the PRD or issues.

Use `/tdd` where possible, at pre-agreed seams.

Run typechecking regularly, single test files regularly, and the full test suite once at the end.

## Review

Before review, run mechanical gates:

```text
./gradlew qualityGate
```

Load and follow the [review skill](../../../.agents/skills/review/SKILL.md) via `/review`. Do not freestyle a review — use that skill's priorities, rules, and output format.

Address blocking findings. If review surfaces documentation or wiki gaps tied to the work, fix them in the same change.
