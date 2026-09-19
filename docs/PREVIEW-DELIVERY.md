# Preview delivery policy — current 4.1 checkpoint

The authoritative handover controls each pass. Mark performs physical Shield QA. Use implement/fix → compile → minimum relevant checks → final APK → return handover → user testing. Do not start a new pass after delivery.

The current workflow runs `validation: preview` by default. It compiles a signed debug checkpoint, runs quick targeted checks and emulator Preview startup/routes, then builds the optimised universal release (`APK_VARIANT: release`), verifies signed install-over/restart and the pinned certificate/ABIs. The delivered 4.1 APK is a release build, not a debug APK. Three earlier 4.1 implementation checkpoints used debug output only.

Do not treat this as physical hardware validation or a populated previous-version migration test. The debug-to-release emulator upgrade preserves Preview enablement; actual 4.0 user data, network sources and playback remain user QA. Keep package/signing identity and increase versionCode for future releases. Never disable a failing gate instead of fixing its cause.

Use existing cheap tests relevant to concrete changed behaviour. Do not expand into exhaustive remote, network, playback or screenshot regression before Preview delivery. `validation: full` remains an optional milestone path; it is not required merely because a Preview changes playback or storage. Superseded branch builds cancel automatically; documentation-only commits do not produce another APK.

Upload the verified APK promptly to the existing Drive APKs to Test folder. Update the running release notes in place with latest entry first and preserved history. Record exact source/version/hash, actual checks, limitations and NOT TESTED — USER QA REQUIRED. Exact usage credits must be reported unavailable if no counter is exposed.

Current hand-back: `../NOVA_CODEX_RETURN_HANDOVER_4.1.md`; audit: `../NOVA_UI_AUDIT_REPORT_4.1.md`. No 4.2 implementation started.
