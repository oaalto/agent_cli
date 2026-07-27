# 04 — Session operations composition root

**Parent:** `prd.md`

**What to build:** An explicit factory that is the visible composition root for all `ClientSessionOperations` dependencies — scope, VFS, permission, deep filesystem module, and terminal registry. The inline companion `create()` construction moves here so reviewers can see what is wired when a session opens.

**Blocked by:** 03 — SDK adapter filesystem delegation

**Status:** done

- [ ] `AcpClientSessionOperationsFactory` (name may vary during implementation) accepts editor context and returns a fully wired `ClientSessionOperations`.
- [ ] The factory constructs the deep filesystem module and passes it into the SDK adapter; inline dependency construction is removed from the adapter companion.
- [ ] A factory test verifies the returned instance is non-null and filesystem/terminal/permission dependencies are correctly composed (types and non-null wiring).
- [ ] `./gradlew qualityGate` passes.
