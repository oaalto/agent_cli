# 01 — ScopedFileSystemAccess VFS seam

**Parent:** `prd.md`

**What to build:** A testable VFS boundary for IDE filesystem I/O. The platform-specific access type becomes a production adapter behind a small interface; session operations depend on the interface instead of the concrete IDE type. Runtime behaviour for agent read/write is unchanged — this is expand-only prefactoring that unlocks in-memory test doubles.

**Blocked by:** None — can start immediately

**Status:** done

- [ ] A `ScopedFileSystemAccess` interface exposes read and write against resolved paths, returning the existing success/failure result shape (or a platform-neutral equivalent).
- [ ] The current IDE VFS implementation is a thin production adapter with no intentional behaviour change.
- [ ] Session operations accept the interface type; existing filesystem client-op behaviour is preserved.
- [ ] `./gradlew qualityGate` passes with no new functional regressions.
