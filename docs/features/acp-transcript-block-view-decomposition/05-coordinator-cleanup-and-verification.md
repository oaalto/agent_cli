# 05 — Coordinator cleanup and verification

**Parent:** `prd.md`

**What to build:** Complete the decomposition: factory is a ~100-line stateless coordinator registering all four adapters in fixed order, with no remaining inline row logic. Shrink coordinator integration tests to dispatch smoke; migrated cases live on adapter-scoped tests. Confirm cyclomatic complexity is distributed — coordinator `update()` stays under threshold without new suppressions.

**Blocked by:** 03 — Plan row adapter; 04 — Body-part widget mapper and agent text row adapter

**Status:** done

- [x] Factory contains only adapter registration, `RowContext` assembly, dispatch to `create`/`update`/`dispose`, and mismatch logging — no private row widget classes remain
- [x] `TranscriptPanel` public contract unchanged (`create`, `update`, `disposeRow` signatures and `blockId` reuse map ownership stay in panel)
- [x] `TranscriptBlockViewFactoryTest` reduced to coordinator dispatch smoke (adapter order, mismatch logging); row behaviour cases live on per-adapter tests
- [x] No new detekt suppressions on coordinator `update()`; adapter files each under threshold or locally justified
- [x] `./gradlew qualityGate` passes; full regression suite green; zero visible transcript UX change across all block families
