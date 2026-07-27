# 02 — SessionFilesystemOperations deep module

**Parent:** `prd.md`

**What to build:** A single deep module that owns the full filesystem client-op policy chain — scope resolution, write permission, VFS access, and line/limit slicing — behind a small interface returning a sealed success/failure result. An in-memory VFS test adapter simulates read-only and version-control-ignored blocks so policy can be verified without the IntelliJ platform.

**Blocked by:** 01 — ScopedFileSystemAccess VFS seam

**Status:** done

- [ ] `SessionFilesystemOperations` exposes `readText` and `writeText` and returns a sealed result distinguishing success, out-of-scope, permission-denied, and VFS failure.
- [ ] The implementation composes scope resolution, write permission, and VFS access internally; line slicing lives with this module, not split across callers.
- [ ] An in-memory test adapter for `ScopedFileSystemAccess` supports normal reads/writes plus read-only and ignored-file simulation.
- [ ] Tests cover: in-scope read success, out-of-scope rejection, write permission denied, VFS read-only block, VFS ignored-file block, and line/limit slicing edge cases.
- [ ] Existing unit tests for pure scope and permission modules continue to pass unchanged.
- [ ] `./gradlew qualityGate` passes.
