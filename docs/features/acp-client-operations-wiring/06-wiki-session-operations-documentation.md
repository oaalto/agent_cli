# 06 — Wiki session operations documentation

**Parent:** `prd.md`

**What to build:** The ACP client wiki's "Session operations" section names the deep filesystem module and composition root so maintainers tracing a single `fsReadTextFile` call know where policy lives — without reading four scattered types.

**Blocked by:** 05 — Session controller factory injection

**Status:** done

- [ ] The ACP client subsystem wiki page references `SessionFilesystemOperations` as the deep module for filesystem I/O policy.
- [ ] The wiki describes the composition root as the place where `ClientSessionOperations` dependencies are assembled.
- [ ] Documentation aligns with ADR 0001's project/worktree scope constraint and notes the test surface at the deep module interface.
- [ ] `docs/wiki/log.md` records the wiki update per project wiki conventions.
