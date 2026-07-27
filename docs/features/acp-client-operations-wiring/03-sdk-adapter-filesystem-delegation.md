# 03 — SDK adapter filesystem delegation

**Parent:** `prd.md`

**What to build:** The ACP SDK adapter thins its filesystem path: `fsReadTextFile` and `fsWriteTextFile` delegate to the deep module and only map sealed results to SDK response types or `JsonRpcException`. Terminal operations, `notify`, and `requestPermissions` stay in the adapter unchanged.

**Blocked by:** 02 — SessionFilesystemOperations deep module

**Status:** done

- [ ] Filesystem SDK methods delegate to `SessionFilesystemOperations`; scope/permission/VFS chaining is no longer duplicated in the adapter.
- [ ] Success maps to `ReadTextFileResponse` / `WriteTextFileResponse`; failures map to `JsonRpcException` with appropriate messages.
- [ ] Tests verify SDK mapping only (success responses, each failure reason → exception) using a test double for the deep module.
- [ ] Terminal and permission/notify behaviour is unchanged.
- [ ] `./gradlew qualityGate` passes.
