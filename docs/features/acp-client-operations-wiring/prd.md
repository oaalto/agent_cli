## Status

implemented

## Problem Statement

`AcpClientSessionOperationsImpl` is the plugin's adapter for the ACP SDK's `ClientSessionOperations` interface. For filesystem operations (`fsReadTextFile`, `fsWriteTextFile`), the implementation is a **shallow orchestrator**: it manually chains three separate modules — `ScopedFileSystemOperations` (scope resolution), `PermissionCoordinator` (write consent), and `IdeScopedFileSystemAccess` (IDE VFS I/O) — with no single deep module owning the end-to-end policy. The caller-facing interface (`ClientSessionOperations`) is wide; the filesystem path's behaviour (scope → permission → VFS → line slicing → JSON-RPC error mapping) is spread across four types with no concentrated **locality**.

Composition is **buried** in the session connect path. `AcpSessionControllerImpl.openSession()` constructs a `ClientOperationsFactory` lambda that calls `AcpClientSessionOperationsImpl.create(context)`, and the companion `create()` method instantiates all filesystem and permission dependencies inline from `AcpEditorContext`. There is no visible composition root or injectable seam — the wiring is implicit, duplicated knowledge between the session controller and the operations factory, and hard to substitute in tests.

**Test coverage is uneven and the highest-risk module is untested.** `ScopedFileSystemOperations` (4 tests) and `SessionScopeResolver` (3 tests) cover pure scope logic. `PermissionCoordinator` has one test (remembered allow-always). `IdeScopedFileSystemAccess` has **zero tests** — it depends on IntelliJ `Project`, `LocalFileSystem`, `FileDocumentManager`, `WriteAction`, and Git ignore checks, making it untestable through its current interface without a platform fixture. `AcpClientSessionOperationsImpl` has **zero tests**; the only `AcpSessionControllerTest` exercises dispose via a recording stub and never touches operations wiring or filesystem behaviour. Real bugs in the chained path (permission denied after scope pass, VFS read-only rejection, line slicing edge cases, error code mapping) have no test surface.

This undermines ADR 0001's constraint that filesystem client ops are scoped to project/worktree root — the policy exists in code but is not guarded by tests at the integration seam where the agent actually calls in.

## Solution

Deepen the filesystem client-op path into a single **deep module** with a small interface that owns scope resolution, write permission, and IDE VFS access behind one seam. Relocate composition out of `AcpSessionControllerImpl.openSession()` into an explicit factory or composition root that is visible, testable, and documented.

The deepening:

1. **Introduce a `SessionFilesystemOperations` module** (name TBD during grilling) — interface covers `readText(path, line?, limit?)` and `writeText(path, content)` returning a sealed result (success / scoped-out / permission-denied / vfs-failure). Implementation composes `ScopedFileSystemOperations`, `PermissionCoordinator`, and `IdeScopedFileSystemAccess` internally. `AcpClientSessionOperationsImpl` delegates `fsReadTextFile` / `fsWriteTextFile` to this module and only maps results to SDK response types / `JsonRpcException`.
2. **Extract a composition root** — replace inline `AcpClientSessionOperationsImpl.create(editorContext)` with a dedicated factory (e.g. `AcpClientSessionOperationsFactory`) that receives `AcpEditorContext` and returns a fully wired `ClientSessionOperations`. `AcpSessionControllerImpl` depends on the factory interface, not on concrete dependency construction.
3. **Define a VFS seam at the interface** — `IdeScopedFileSystemAccess` becomes the production adapter behind an interface (e.g. `ScopedFileSystemAccess`) so unit tests can use an in-memory filesystem adapter without IntelliJ platform fixtures. The deletion test: if the interface is removed, scope + permission + VFS chaining reappears in `AcpClientSessionOperationsImpl` — confirming the module earns its keep.
4. **Preserve terminal and permission notify paths** — terminal ops and `requestPermissions` / `notify` stay in `AcpClientSessionOperationsImpl`; this PRD scopes only filesystem deepening and wiring extraction.

## User Stories

1. As a developer fixing a filesystem scope bug, I want all scope → permission → VFS logic in one module, so that I change behaviour in one place with full **locality**.
2. As a developer adding a new filesystem policy (e.g. additional write block reason), I want to extend the deep module's implementation without touching the session controller or SDK adapter, so that the seam stays stable.
3. As a developer writing tests, I want to exercise filesystem client ops through the deep module's interface with an in-memory VFS adapter, so that I can verify behaviour without starting the IntelliJ platform.
4. As a developer reviewing session wiring, I want a visible composition root that lists all `ClientSessionOperations` dependencies, so that I understand what is constructed when a session opens.
5. As a security-conscious user, I want out-of-scope paths, permission denials, and read-only/ignored-file blocks to be tested end-to-end at the module interface, so that ADR 0001's scope constraint is regression-guarded.
6. As a developer mocking session operations in controller tests, I want to inject a test double factory at the session controller seam, so that I can verify `openSession` invokes the factory without constructing real filesystem dependencies.
7. As a maintainer onboarding to the ACP slice, I want the wiki's "Session operations" section to name one deep module for filesystem I/O, so that I do not trace four files to understand a single `fsReadTextFile` call.
8. As a developer handling WSL path normalization, I want scope resolution tests to remain in `ScopedFileSystemOperations` / `SessionScopeResolver` while integration tests cover the composed path, so that pure path logic and orchestration are tested at the right level.

## Implementation Decisions

### Ownership

- **Vertical slice:** `acp/` — all affected files live under `com.oaalto.agent.acp` and `com.oaalto.agent.acp.filesystem`.
- **No changes to** `pty/`, `worktree/` (except if `SessionScopeResolver` callers need import path updates), or `settings/`.

### Modules to modify

| Module | Role after deepening |
| --- | --- |
| **`SessionFilesystemOperations`** (new) | Deep module: scope + permission + VFS behind small interface |
| **`ScopedFileSystemAccess`** (new interface) | Seam for IDE VFS; production adapter wraps `IdeScopedFileSystemAccess` |
| **`IdeScopedFileSystemAccess`** | Becomes production adapter; implementation unchanged initially |
| **`ScopedFileSystemOperations`** | Unchanged pure scope module; consumed internally by deep module |
| **`PermissionCoordinator`** | Unchanged; write permission called internally by deep module |
| **`AcpClientSessionOperationsImpl`** | Thins to SDK adapter: delegates fs ops to deep module |
| **`AcpClientSessionOperationsFactory`** (new) | Explicit composition root for all `ClientSessionOperations` dependencies |
| **`AcpSessionControllerImpl`** | `openSession()` receives factory via constructor or `AcpEditorContext`; lambda calls factory, not `create()` companion |

### Composition root relocation

Current wiring (buried):

```kotlin
// AcpSessionControllerImpl.openSession()
val operationsFactory = ClientOperationsFactory { _, _ ->
    AcpClientSessionOperationsImpl.create(context)
}

// AcpClientSessionOperationsImpl.create()
ScopedFileSystemOperations.create(scopeRoot, projectBasePath)
IdeScopedFileSystemAccess(editorContext.project)
editorContext.permissionCoordinator()
editorContext.terminalSessionRegistry
```

Target wiring (explicit):

```kotlin
// AcpClientSessionOperationsFactory (new)
fun create(context: AcpEditorContext): ClientSessionOperations

// AcpSessionControllerImpl.openSession()
val operationsFactory = ClientOperationsFactory { _, _ ->
    sessionOperationsFactory.create(context)
}
```

Factory is constructed once per editor session (alongside `AcpEditorContext`) or injected into `AcpSessionControllerImpl` — exact lifetime decided during implementation; the PRD requires visibility, not a specific DI framework.

### Deep module interface (sketch)

```kotlin
sealed class SessionFilesystemResult {
    data class Success(val content: String) : SessionFilesystemResult()
    data class Failure(val reason: FailureReason, val message: String) : SessionFilesystemResult()
    enum class FailureReason { OUT_OF_SCOPE, PERMISSION_DENIED, VFS_ERROR }
}

interface SessionFilesystemOperations {
    suspend fun readText(path: String, line: UInt?, limit: UInt?): SessionFilesystemResult
    suspend fun writeText(path: String, content: String): SessionFilesystemResult
}
```

`AcpClientSessionOperationsImpl` maps `SessionFilesystemResult` → `ReadTextFileResponse` / `WriteTextFileResponse` / `JsonRpcException`. Line slicing (`sliceLines`) moves into the deep module or a package-private helper co-located with it — not left split between adapter and scope module.

### VFS seam

- **Interface:** `ScopedFileSystemAccess` with `readText(resolved: Path)` and `writeText(resolved: Path, content: String)` returning the existing `AccessResult` sealed type (or a renamed platform-neutral equivalent).
- **Production adapter:** thin wrapper over `IdeScopedFileSystemAccess`.
- **Test adapter:** in-memory `Map<Path, String>` with optional read-only / ignored flags for block-reason tests.
- **Deletion test:** removing `SessionFilesystemOperations` forces `AcpClientSessionOperationsImpl` to re-chain scope, permission, and VFS — confirming the deep module concentrates complexity.

### Why this works (leverage and locality)

- **Leverage:** Callers of `ClientSessionOperations` (the ACP SDK) see one `fsReadTextFile` / `fsWriteTextFile`; maintainers see one module owning filesystem policy.
- **Locality:** Scope failures, permission denials, VFS errors, and line slicing change in one implementation class.
- **Test surface:** The deep module's interface is the test surface — not Swing, not stdio transport, not `ClientSessionOperations`'s full SDK surface.

### ADR alignment

- **ADR 0001** (custom ACP client): Strengthens the "filesystem client ops scoped to project/worktree root" constraint with testable locality. No protocol or UX model changes. Aligned.
- **ADR 0002** (Kotlin ACP SDK): `ClientSessionOperations` remains the SDK boundary; internal deepening does not change SDK types. Aligned.
- **ADR 0003** (per-project agent selection): `PermissionCoordinator` already keys on `configurationId` from `AcpEditorContext`; factory passes this through unchanged. Aligned.

## Testing Decisions

### What to test

- **Deep module interface (primary):** observable filesystem outcomes — in-scope read success, out-of-scope rejection, write permission denied, VFS read-only / git-ignored block, line/limit slicing.
- **VFS seam:** production adapter smoke-tested only if a lightweight platform fixture exists; otherwise all VFS policy tests run against the in-memory test adapter.
- **Composition root:** factory produces a `ClientSessionOperations` instance; session controller test verifies factory is invoked on `openSession` (with injectable test factory).
- **Regression:** existing `ScopedFileSystemOperationsTest`, `SessionScopeResolverTest`, and `PermissionCoordinatorTest` continue to pass unchanged — pure modules stay tested at unit level.

### Modules to test

| Module | Test file (new or extended) | Focus |
| --- | --- | --- |
| `SessionFilesystemOperations` | `SessionFilesystemOperationsTest.kt` | End-to-end fs policy via in-memory VFS adapter |
| `ScopedFileSystemAccess` (test adapter) | co-located in test source | Read-only, ignored-file simulation |
| `AcpClientSessionOperationsFactory` | `AcpClientSessionOperationsFactoryTest.kt` | Wiring completeness (dependencies not null / correct types) |
| `AcpClientSessionOperationsImpl` | `AcpClientSessionOperationsImplTest.kt` | SDK mapping only: result → response / `JsonRpcException` |
| `AcpSessionControllerImpl` | extend `AcpSessionControllerTest.kt` | Factory injection seam (optional, if constructor change is small) |

### Prior art

- `ScopedFileSystemOperationsTest` — temp-directory scope tests; pattern for pure path logic.
- `SessionScopeResolverTest` — WSL path normalization; stays separate from orchestration tests.
- `PermissionCoordinatorTest` — `runBlocking` + fake `PermissionPromptUi`; reuse fake prompt pattern for write-permission scenarios in deep module tests.

### Verification

`./gradlew qualityGate` passes after implementation.

## Out of Scope

- Terminal operations deepening (`terminalCreate`, `terminalOutput`, etc.) — separate candidate; terminal registry wiring may move to the same factory but behaviour is unchanged.
- `notify()` / `requestPermissions()` routing changes.
- Changes to `SessionScopeResolver` or WSL path-mapping logic (already tested).
- IntelliJ platform integration tests for real VFS (`IdeScopedFileSystemAccess` against live `Project`) — deferred unless a minimal fixture is already available.
- MCP bridging, auth flow, or transcript rendering.
- PTY Passthrough mode — not affected.
- Extracting `sliceLines` into a shared utility outside the deep module unless duplication appears elsewhere.

## Further Notes

- **Recommendation strength:** Worth exploring — friction is real (buried wiring, untested VFS path) but terminal ops in the same class may warrant a follow-on PRD if filesystem deepening succeeds.
- **Shallow vs deep today:** `AcpClientSessionOperationsImpl` passes the deletion test for the class itself (terminal + notify + permissions would remain), but fails it for the filesystem sub-path — deleting the fs methods would scatter scope/permission/VFS chaining with no owner.
- **Two adapters = real seam:** `ScopedFileSystemOperations` (pure) + `IdeScopedFileSystemAccess` (platform) are already separate; the missing piece is a deep module composing them with permission policy and a testable VFS interface.
- Implementers should update `CHANGELOG.md` under `### Changed` when the code ships.
- Related wiki page: [ACP client subsystem](../../wiki/subsystems/acp-client.md) — update the "Session operations" section to reference the deep module after implementation.
- Related ADR: [ADR 0001 — filesystem scoped to project/worktree root](../../adr/0001-custom-acp-client-in-plugin.md).
