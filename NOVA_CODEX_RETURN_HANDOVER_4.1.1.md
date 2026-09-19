# SUPERNOVA / Nova Preview 4.1.1 — return handover

Date: 19 September 2026. Corrective implementation over Preview 4.1. This is a development hand-back, not physical Nvidia Shield acceptance.

## Authority and baseline

- Read every file in `Nova_Codex_Handover_2026-09-19_4.1.1_FINAL.zip` before code changes: READ FIRST, CODEX PROMPT, manifest and all three PROJECT_DOCUMENTATION files. Manifest hashes matched. ZIP SHA-256: `3270b017f1fb0a7357685852a884fa9bc689b5580a48791bb3517e19012e031e`.
- This package contained no APPROVED_REFERENCES images or QA videos. Its written physical-QA findings controlled this pass; no claim of a new pixel-for-pixel comparison with absent references is made. Older handover requirements were not independently merged.
- Repository: `heretoecode/aos-Video`, branch `codex/apk-build-fixes`. Started from documentation head `c12ec84e44643563f0f3af31bf795e873a7f573f`; actual 4.1 APK source `12f864a9bf56709afba069a234b22dd765f887d4`.
- MediaLib pinned at `5758074049bc153ac4967b4ecd21886469270076`, with the bounded existing build patch extended for WebDAV eligibility. FileCore pinned at `c4b760c55102c72d45f68b0d16999fc1f4f013a7`.
- No 4.2 implementation or unrelated feature pass started.

## Build and delivery identity

- Version: **6.4.63-mark.4.1.1-preview**; versionCode **6040077** (4.1: 6040076).
- Source commit: `75cfbf2fb7970912de5660a53c925c5b7ee7d67f`.
- Build: https://github.com/heretoecode/aos-Video/actions/runs/35451721845 — success.
- Package: `org.courville.nova.markpreview`, unchanged. Optimised universal release APK, existing preview signing/update path.
- Certificate SHA-256: `89ac087ed6f989c90482d4a999f80511fe6ceee26ef1b9c37a142a9f00d39a5a`, same as 4.1.
- APK: `NOVA_Preview_4.1.1.apk`, **82,314,749 bytes**.
- APK SHA-256: `6849af8484f04e6d4820a02a57ad01e20257ce560169fc8822dd8bb248dee07d`.
- APK Drive: https://drive.google.com/file/d/123e0QkdRuj32bM7_-ZROvRY-AKaKggJr/view — `Nova video player preview / APKs to Test`.
- Reports/return bundle: established `Handovers` folder. Existing cumulative `NOVA_Release_Notes.txt` updated in place with 4.1.1 first and prior release history preserved.
- Native ABIs: arm64-v8a, armeabi-v7a, x86, x86_64. No signing-key or database-schema migration.

## What I expected the user to see

1. Install over 4.1 without uninstalling. Existing library, saved preferences, rows, providers and playback history remain. Launch shows a clean SUPERNOVA surface until the first Home composition is ready, then a short fade. There is no artificial minimum display time.
2. Featured keeps its local-library candidates and elongated carousel markers. Title/action geometry is stable; Play and Resume occupy the same width. Genuine title-logo artwork appears when TMDb provides a suitable image, otherwise readable plain text remains. Loading/HUD use cached logos only and do not wait for online artwork.
3. Customise Home opens a compact management dialog. Existing reorder/show-hide remains. Settings → Home & Discovery → Clear Watch Next asks for confirmation and removes membership only; hiding still retains membership/history.
4. Details has Play/Resume, Information and Actions, plus existing conditional working actions. Add to Row is inside Actions. Information is a full-screen two-column information surface, without duplicate playback actions.
5. Network & Files has a normal-sized Scan Library action and restrained dark depth. Its top navigation, and Search navigation, stay transparent while scrolling. Actual file-browser list rows have a dark background, consistent height, separators, path hierarchy and a visible focus treatment.
6. Settings → Library exposes automatic startup scanning and interval controls. Previously absent settings initialise to startup enabled / 15 minutes; explicitly saved Off or another interval is preserved. Scheduled WebDAV requests no longer depend on the inactive-LAN-server flag. Manual scanner/import behaviour is retained.
7. Local Search accepts Shogun as well as Shōgun, matching indexed show/title/episode/path text without accent sensitivity. Existing library visibility/filter choices still apply.
8. Streaming provider ticks save as each choice changes, including when leaving with Back. Choices update in place without rebuilding the entire dialog. Genuine returned provider artwork/availability can reach existing Details actions; no service availability is invented.
9. HUD track summaries are concise; full technical names remain in choosers. Transport buttons are centred and evenly sized. Speed/delay panels use a compact common anchor; track selections update the current dialog. TV end-time wording is “Episode ends”. Up Next has a small dark media presentation with the same countdown/Play Now/Cancel behaviour.
10. Backup export warns that private credentials are included. The destination is visibly in progress until a closed archive and destination checksum have been verified. Where the storage provider supports rename, the final name becomes `.zip`; otherwise a clear message explains the retained in-progress name. Restore recovers configuration/databases and downloads missing ordinary artwork again in the background.

## Requirement status and code changes

“Implemented; user QA pending” means shipped code, not a claim that the physical regression has been reproduced or accepted as fixed.

| Requirement | Implementation / important components | Status |
|---|---|---|
| Startup composition | `PreviewStartupSurface`, `EntryActivity`, `MainActivityLeanback`, `MainFragment`, `PreviewPages`, `preview_startup.xml`: matching launch/window/composition surface, first-data pre-draw release and 160 ms fade | Implemented; populated Shield launch QA pending |
| Featured presentation | `PreviewPages`: narrower fixed title frame, fixed 116 dp primary action, restrained content alpha transition; existing backdrop crossfade/local candidates/markers retained | Implemented; approved-reference/Shield visual acceptance pending |
| Official title artwork | New `OfficialTitleArtwork`; consumed by Featured, `PreviewMoviePage`, `PreviewPlaybackLoading`, `PlayerActivity`/`PlayerController` | Implemented with actual TMDb images; live logo coverage QA pending |
| Home Rows / Clear Watch Next | `PreviewHomeRows`, `PreviewSettings`: compact 620×410 dp maximum dialog; confirmation-only clearing of membership | Implemented; full remote QA pending |
| Library List separators | `PreviewLibraryColumns`: restrained horizontal separator contrast | Implemented; existing grid/list model preserved |
| Details / Information / Actions | `PreviewMoviePage`, `PreviewIcon`, `PreviewPlaybackInfo`: Add to Row secondary, semantic action names, two-column information layout, stale List placeholder removed | Implemented; populated metadata/long-content QA pending |
| Network / real file browser | `PreviewPages`, `TopNavigation`, `PreviewAccent`, `ListingFragment`, `ListPresenter`: compact scan action, transparent utility navigation, neutral gradient, 54 dp file rows/path treatment | Implemented; specialist/optional classic surfaces remain (audit) |
| Automatic WebDAV | `PreviewAutoScanPolicy`, `CustomApplication`, Settings controls, `.github/build/medialib-preview40.patch` | Corrected source-level trigger/eligibility gaps; physical persistent failure remains unconfirmed |
| Indexed Search | New `PreviewSearchText`, `PreviewSearch`: Unicode-normalised background matching before full model mapping | Implemented; real Shōgun index and large-library performance QA pending |
| Streaming chain | `PreviewPreferenceDialogs`, `PreviewDialog`, `StreamingPreferences`, `StreamingRepository`, `StreamingActions`: immediate persistence, stable keys, in-place checks, retained real logo paths, consistent disabled default | Implemented persistence/presentation corrections; actual Ireland offers/handoff unverified |
| Settings refinement | `PreviewSettings`: 54 dp rows, balanced help column, activity-normalised contextual-help lookup | Implemented; category/Legacy architecture and underlying settings retained |
| Playback HUD / panels | `PreviewTrackLabel`, `PlayerActivity`, `PlayerController`, `PreviewPlaybackMenus`, `AudioSpeedTVPicker`, `AudioDelayTVPicker`, `SubtitleDelayTVPicker`, `TimerDelayTVPicker`, controller XML and 10-second vectors | Implemented presentation corrections; actual decoder/track/menu/Shield QA pending |
| Up Next | `PreviewUpNext`: restrained rounded translucent panel and available artwork, concise countdown | Visual changes only; PASSED timing/selection algorithm deliberately preserved |
| Migration backup | `MigrationBackup`, `MediaLibraryBackupService`, `SafeBackup`, `VideoPreferencesCommon`: named configuration, verified temporary publication, artwork exclusion/recovery and rollback refinement | Implemented; actual SAF/new-device migration not tested |

## Root-cause findings and limits

### Automatic WebDAV scanning

The existing automatic path reads `auto_rescan_on_app_restart` (absent → false) and `AUTO_RESCAN_PERIOD` (absent → zero). Foreground polling and scheduled jobs therefore can remain disabled on an otherwise functioning setup; the redesigned Settings did not expose those controls. Separately, scheduled scans consult inactive-server state, while manual force-scans bypass that test. This explains concrete differences between automatic and manual runtime paths, but no copy of Mark's actual preferences, server state or Shield log was available to prove which applied on his device.

The correction initialises only absent preferences, exposes both controls, re-establishes the existing scheduled job and exempts HTTP-based WebDAV from the inactive-LAN-server exclusion. Existing local/VPN/mobile WebDAV eligibility, queued-batch accounting and the prior foreground retry remain. Actual discovery/import, protocol implementation, source credentials, scan batching and PASSED manual scanning were not rewritten. Existing `SupernovaScan` diagnostics report trigger configuration/busy/timing. Explicit Off is not silently overridden; verify the two Settings controls during user QA.

### Search

SQLite's previous title LIKE stage could reject diacritic variants before Java ever saw them. The query now retains the existing library/mode/visibility filters but supplies matching candidates to a cancellable background normaliser. NFKD folding removes combining marks and handles case plus common non-decomposing characters; title, episode title and indexed path are matched before expensive full model mapping. This fixes a reproducible source-level accent mismatch. A deliberately hidden/excluded title remains excluded. Reading a large index per query requires real-library performance QA.

### Provider persistence and availability

The multi-select wrapper previously rebuilt its dialog after each selection and persisted only when Done was selected. Back lost those pending changes. Dynamic provider preference keys were also assigned after attaching to the preference hierarchy, preventing normal initial hydration. The correction persists each accepted toggle, assigns keys before attachment and updates visible checkmarks without closing the window.

The existing chain was traced: local movie TMDb ID or episode parent-show ID → configured country/season provider lookup → selected provider IDs → genuine provider actions → existing title handoff/optional JustWatch fallback. Real returned logo paths were being discarded; they now survive parsing. The global enable default now agrees with the disabled Settings default. No subscription/account detection, guessed Ireland availability, unsupported episode service offer or remote Search catalogue was added. An empty season response is not relabelled as a confirmed episode offer. Shōgun/MobLand Ireland results and installed-provider handoff still require real-service QA.

### Featured and title logos

The 4.1 model exposed posters/backdrops, not a separate logo asset. New reusable lookup uses the existing legitimate TMDb image endpoints (`movie/{id}/images`, `tv/{id}/images`) and existing app API configuration. Genuine PNG logos are selected by UI language, then English/neutral preference; downloaded assets are bounded and atomically cached. Missing results use text. Network/decode work runs off the main thread. The original text remains the accessibility title. Playback loading/HUD only consult memory/disk, so artwork does not delay playback startup. A first-ever direct playback without a cached logo uses text; not every title has an official supported PNG.

Featured also used a content-dependent primary action width; Play/Resume now share the same footprint. Text and artwork retain existing geometry rather than rebuilding the global page. Main Details renders episode title context alongside series-logo fallback without substituting a series rating for an episode rating.

### Playback presentation

Track summaries previously inherited long technical labels. They now derive concise language/format/channel information from actual selected-track metadata. Full choice labels are retained. The centre cluster is explicitly centred with equally sized controls and corrected vector centres. The Preview child-panel adapter previously overwrote measured height with a fixed large panel and recreated selection dialogs; it now honours measured content and updates checks in place. Existing speed/delay handlers, anchored Back restoration, native decoder, resume/journey and Up Next transition logic were deliberately preserved.

### Backup and restore

The old Create Document flow gave the provider a final `.zip` name before copying, so a file manager could show a final-looking zero-byte archive. Export now uses `.zip.in-progress`, validates the closed staged ZIP, verifies destination SHA-256 after closing and renames only then where supported. Cross-provider atomic rename cannot be guaranteed; unsupported providers keep the verified in-progress name with an explicit manual-rename message.

Existing media/source/credential/shortcut databases and typed default settings remain. Other persisted account/configuration preference files are included; known discovery/artwork caches and transient player launch identity are excluded. Downloaded poster/backdrop files with known HTTP(S) origins and generated scraper episode stills are omitted. Unknown/non-download poster/backdrop files are retained to avoid losing personal artwork. Restore uses the existing staged validation/database swap, validates named preferences before mutation and restores exact previous preference keys on rollback. A pending background recovery downloads default posters/backdrops and genuine episode stills, updating only the still path; offline failures retry on return. It does not reset playback or rescrape watch state.

Credential audit: FileCore's saved network credential database uses portable legacy obfuscation, not a device-bound Android Keystore key. It can be copied between installations, but the export must be treated as private credentials-bearing data; it is not strongly encrypted. Unsaved temporary credentials and Android URI permission grants cannot be transferred. External path changes, expired tokens/provider sessions, grant renewal and new-device DB compatibility remain migration constraints. Existing restore requires a compatible media database version. Unknown surplus artwork may make an archive larger than a theoretically minimal configuration-only backup.

## Deliberate preservation / non-changes

- No playback-engine rewrite, protocol/discovery rewrite, new streaming backend, analytics/telemetry, generated logos, fake availability or unsupported player controls.
- PASSED 4.1 series journey, unfinished-final-episode Continue Watching, bookmark persistence, auto-next/countdown/Play/Cancel, recap skip and Watch Next hiding/membership algorithms retained.
- Existing global top navigation and category architecture retained. No permanent application sidebar. Existing preferences, defaults explicitly saved by the user, Legacy implementations and Projector Mode behaviour retained.
- No database schema migration or package/signing reset. Existing library row memberships continue using existing local IDs.
- New source references do not authorise deferred 4.2 work.

## Validation actually performed

- PASS: final-source signed debug and optimised release compiled and generated. Universal APK downloaded and checked against the runner SHA-256; artifact ZIP digests/integrity checked.
- PASS: **33 targeted tests, 0 failures, 0 skipped**, reported execution 14.715 seconds (Gradle test step 50 seconds). Covered Unicode Search, genuine logo selection, concise audio labels, explicit scan Off, existing provider choices, series journey/resume, rows, Movie/episode metadata, page render/focus, Up Next and closed backup validation.
- PASS: API 28 x86_64 emulator at 1920×1080/320 dpi: Preview enabled, cold launch, reinstall with preference retained, restart, warm return, Settings categories, Movies, TV Shows and local Search no-match query.
- PASS: optimised release installed over the signed diagnostic build with Preview still enabled, launched and restarted. No FATAL application exceptions in retained final route/release logs. Certificate matched the existing Preview pin.
- This is a diagnostic-to-release emulator upgrade, **not** a test of the user's physical 4.1 database upgrade. Same package/certificate and increasing versionCode support that intended update path.
- Release lint: **0 errors, 1,396 warnings**, inventory retained in CI diagnostics. No exhaustive regression, live streaming/WebDAV or physical playback test was performed.
- Reviewed available final release/Settings captures and source/fixture renders for Home, Details, List, Network, Rows and playback surfaces. Source review covered changes not exercised with real media. See the UI audit for limitations.

No physical Nvidia Shield, personal NAS/WebDAV endpoint, Ireland provider-account session or real user library was available. Test fixture titles/artwork are test-only and are not shipped as fake library data.

## NOT TESTED — USER QA REQUIRED

1. Actual 4.1 → 4.1.1 install-over with Mark's database, source credentials, preferences and rows. Do not uninstall as part of the normal update.
2. Cold/warm Shield startup composition, populated Featured artwork and title transitions; official-logo availability on actual titles and cached loading/HUD reuse; no flash on first frame/return.
3. Automatic WebDAV: check startup/interval Settings, add a real file, wait one configured interval while foreground, cold restart and offline recovery. Verify import and Home Recently Added. Manual Scan must still work. Saved explicit Off remains Off.
4. Search Shogun/Shōgun and MobLand against the real indexed library, show/episode results and remote keyboard performance; long/large-library searches.
5. Ireland provider choices after Back, restart and country round-trip; actual TMDb season/title offers, artwork, provider app/browser/JustWatch handoff.
6. Full D-pad flow through compact Rows, confirmation Clear/Cancel, Details Actions/Information, real file browsers, contextual Settings and list separators. Hiding must retain membership; Clear must remove membership only.
7. Real audio/subtitle track names and live changes, speed/delay panels, anchors/Back focus, current/end clock, Up Next near completion, recap skip and Continue Watching after next episode. Preserve all PASSED baseline behaviour.
8. Actual SAF backup to each used provider, file-manager naming while writing, restore to a fresh/second installation, credentials/history/rows/provider recovery, Android grant renewal, offline artwork retry and interrupted/failing restore recovery. Keep an independent existing backup until accepted.
9. Hardware playback reliability, A/V sync, HDR/audio output and Projector Mode were not re-tested; the working engine path was not intentionally changed.

## Usage and implementation cycles

Exact starting/ending Codex allowance, credits and calculable consumption were **unavailable**. No credit figure is estimated.

- Stage A commit `fd83f1a06c3d314f29aa4ecb85039e3743ad2617`, run `35448072900`: compilation found an ambiguous `Movie` import in the new title-artwork helper; corrected explicitly.
- Stage B commit `e0ea41c27e84e399bb1f6ac02b9a73ba71d7b57c`, run `35448560838`: signed debug compilation and 33 targeted checks passed; install/reinstall/restart/warm return and Settings opened. A later UIAutomator capture returned a null root. Saved logs contained no app FATAL exception. The run stopped before release generation.
- Stage C commit `3df69dc1738803a0403727423998b3091c080934`, run `35451321551`: debug compilation passed. Robolectric's Android dependency download failed with a connection reset before two test classes could run; this was not a failing application assertion. It added bounded fresh-dump retries, still-image restoration and exact preference rollback.
- Stage D commit `75358ec07feea3aa7a361c5ccde693dd22183575` corrected the retained native speed/audio-delay/subtitle-delay/timer pickers' Preview focus backgrounds; their classic styling/handlers remain. Its build was superseded by the final in-app notes update.
- Final source commit `75cfbf2fb7970912de5660a53c925c5b7ee7d67f` includes the current 4.1.1 About notes. Final build outcome is recorded above.
- Most investigation effort: persistent automatic-scan eligibility/defaults versus working manual import, provider preference lifecycle, genuine title artwork, and portable/verified backup. CI dependency assembly, compilation, emulator and release optimisation dominate build time. No broad new test infrastructure or whole-app regression campaign was undertaken.

## Later-pass suggestions (not completed work)

- Use Shield QA/logs to resolve any remaining automatic-scan discrepancy before changing the importer.
- Expand supported title-logo formats only if real missing-title coverage justifies it; do not invent artwork.
- Consider a future encrypted portable-backup format and explicit source-path remapping, separately scoped and reviewed.
- Refine remaining specialist/system/optional classic surfaces listed in the UI audit only in an authorised later pass.
