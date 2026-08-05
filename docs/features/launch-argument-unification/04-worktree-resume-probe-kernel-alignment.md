# 04 — Worktree resume probe uses kernel command base

**Parent:** `prd.md`

**What to build:** `CursorResumeProbe` stops building probe commands independently of launch validation. Where applicable, it consumes the same kernel-resolved executable path and WSL command base (including environment-variable contract) that PTY and ACP launchers use after tickets 02–03, so resume probe results match runtime launch for the same configuration and worktree path. `PtyResumeStrategy` plan selection stays unchanged; it may pass kernel-resolved argument tails into the probe request. `WorktreeLaunchCoordinator` remains mode-agnostic — it continues calling kernel launch resolution only, without reintroducing slice-specific validation.

**Blocked by:** 02 — ACP launch adapter uses argument resolver; 03 — PTY launch adapter uses argument resolver with WSL env parity

**Status:** ready-for-agent

- [ ] `CursorResumeProbe` local and WSL probe commands use kernel-resolved binary path and WSL env/command base consistent with launch adapters for the same `ResumeContext` inputs
- [ ] Probe behaviour for `--continue` fallback (including "No previous chats found") is unchanged from the user's perspective
- [ ] Integration-style unit test verifies probe command base matches launcher output for LOCAL and WSL fixtures sharing the same configuration
- [ ] `WorktreeLaunchCoordinatorTest` and `PtyResumeStrategyTest` remain green; no silent regression to pre-kernel WSL env omission
- [ ] `./gradlew qualityGate` passes
