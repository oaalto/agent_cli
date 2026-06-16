# Code Formatting

- **File endings:** Every source file should end with a single newline character.
- **Blank lines:** Use at most one consecutive blank line to separate logical blocks of code.
- **Diffs:** When presenting changes across multiple files, show each file in a separate code block and use unified diff format.

## Examples / repository specifics

For this repository (Kotlin / Gradle plugin) prefer the Gradle wrapper and ktlint:

```text
./gradlew ktlintFormat
./gradlew ktlintCheck
```
