# 04 — Plain-text restore on resumed session open

**Parent:** `prd.md`

**What to build:** On **ACP Client** editor open with a resumed session and known `acpSessionId`, read the **Session transcript file** via **TranscriptFileStore** and replay content as plain **Transcript** lines through existing plain-line / append pathways — no **TranscriptBlock** graph reconstruction, no parsing `[tool: …]` into tool cards, no plan or markdown rebuild. Restored history appears as plain lines; subsequent live **SessionUpdate** traffic uses the normal block pipeline with full live rendering. Missing, empty, or normally formatted plain files succeed without blocking session use.

**Blocked by:** 02 — TranscriptFileStore

**Status:** ready-for-agent

- [ ] Resumed **ACP Client** session open reads transcript file for `acpSessionId` before or during transcript initialization
- [ ] File lines replay as plain transcript history via existing plain-line injection path (not block factory reconstruction)
- [ ] Missing file → empty transcript, normal live behavior; empty file → restore succeeds with no user-visible error
- [ ] After restore, new agent replies and tool events render with full live **Transcript** block UI
- [ ] **PTY Passthrough** editors skip read/restore entirely
- [ ] Optional but valuable: behavioral test replaying plain lines into **TranscriptViewController** and asserting visible plain history without block reconstruction
- [ ] `./gradlew qualityGate` passes
