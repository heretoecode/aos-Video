# NOVA_CODEX_RETURN_HANDOVER

## Build identity

- Date: 17 September 2026.
- Delivery status: **test APK ready; whole-app visual unification remains PARTIAL** (explicit legacy inventory below).
- Version: **6.4.63-mark.4.0-preview**; versionCode **6040075**.
- APK: `NOVA_Preview_4.0.apk`; 82,282,584 bytes; optimised universal release.
- Package: `org.courville.nova.markpreview`.
- APK source commit: `345636900b577bfec90a22d117fe9b6d062b9f2b`. Documentation may be committed separately after this source revision.
- Build: [35248362010](https://github.com/heretoecode/aos-Video/actions/runs/35248362010) — successful.
- APK SHA-256: `5b441c1d9c39c527ac130e46aba597ea86d0f25913206a27645488ea2515d4ad`.
- Signing certificate SHA-256: `89ac087ed6f989c90482d4a999f80511fe6ceee26ef1b9c37a142a9f00d39a5a`.
- ABIs: armeabi-v7a, arm64-v8a, x86, x86_64.
- Google Drive delivery: [NOVA_Preview_4.0.apk](https://drive.google.com/file/d/1HxNU7u-WLKw82sY2IOX2aLO1EspG_wj5/view) in Nova video player preview → APKs to Test; return reports/archive in Handovers.


Authority (input SHA-256 `eb039b8d38fd715ba2056ccdaa49278cf6b7833eeceaece6e9eefac4b6d68b90`): `Nova_Codex_Handover_2026-09-17_4.0_FINAL(1).zip`, including all current documentation, six approved/QA images and non-conflicting predecessor evidence. The 4.0 written specification supersedes older plans. No requirements were independently merged from the attached 3.6 package.

Repository: `https://github.com/heretoecode/aos-Video`, branch `codex/apk-build-fixes`. Delivered 3.9 baseline: `3c1e34a62c39b12c52b90e4ab7dc2618ab340bb1`. Continue from the APK source commit recorded above, not an old ZIP's code.

## Requirement status

DONE below means implemented in this APK and checked as described; it does not mean physically accepted on a Shield. **Whole-app visual unification remains PARTIAL.** No physical TV/NAS/media QA was performed.

| 4.0 requirement | Status | Evidence / remaining qualification |
|---|---|---|
| Visible Movie/Episode delete confirmation | DONE | Preview Delete opens Cancel/Delete with Cancel initially selected, then invokes existing delete path. |
| Physical delete before library removal / failure safety | DONE; physical QA open | FileCore `delete()` and Android permission result retained; no remote pre-unindex. False/pending results do not claim success. Local existence check and mixed-batch failure accounting added. Local/WebDAV/SMB/USB actual deletion not tested. |
| Top navigation destination/selection desynchronisation | DONE | MainActivityLeanback `onNewIntent` + MainFragment consume explicit tab on reused singleTask activity. |
| Network Up-focus premature exit | DONE | First content group alone can leave to navigation; non-focusable group headers skipped. New Scan Library button is uppermost root content. |
| Movie/TV header recovery from first grid row | DONE | Explicit first-row Up returns controls and scrolls to header; existing stable logical anchors retained. |
| Subtitle nested-menu return/focus | DONE; hardware QA open | Native TVCardDialog hosted in compact Dialog with explicit parent return and HUD origin focus; null-focus guard retained. |
| Play from beginning resumes | DONE; playback QA open | Existing seek-to-zero followed by native player start; no playback engine replacement. |
| Audio Boost / repeated menu changes stay open | DONE; hardware QA open | Existing action runs, choices/checked state refresh in place; nested picker replaces parent and Back restores it. |
| Preferred/English subtitles first / Other languages | DONE; real tracks QA open | Actual subtitle-track language metadata; external localised names and HI suffix recognised. Other language tracks retained in submenu. |
| Automatic network scan trigger | DONE; NAS/lifecycle QA open | Foreground offline/busy retry, preference-change trigger and bounded MediaLib scheduling patch. Last-scan timestamp only after work is queued. Existing source/period/start settings remain authoritative. |
| Manual scan entry and full Settings scan | DONE | Network & Files → Scan Library requests existing local scanner and indexed-network scheduler. Settings → Library → Full Library Scan includes network plus existing local/unmatched-description work. No unsupported quick/incremental claim. |
| Shared Movie/TV configurable List View | DONE | Compact shared columns, show/hide/reorder, sortable headers, per-section preferences. Standard Details on Select. |
| Storage totals / average episode size | DONE for reliable local DB metadata | TV count is represented episode media files, not scraper catalogue episode count. Combined byte sum / count; incomplete totals explicitly lower-bound, average omitted if incomplete. No per-file network stats. |
| Home hero spacing / Next / idle rotation | DONE | Fixed title/plot/action geometry; about 30 seconds idle rotation; paused/reset by interaction with hero. Existing Continue Watching behaviour preserved. |
| Details hierarchy and compact circular cast | DONE | Shared Movie/TV/Episode left-poster composition, human metadata separate from technical pills; compact circular cast portraits and adjacent names/roles. |
| Episode-specific metadata/rating | DONE | S/E, actual air date, duration, episode score; absent values omitted. Parent genres allowed; series score never substituted as episode score. Targeted test distinguishes parent 9.8 versus episode 7.4. |
| Factual provider availability beside local playback | DONE via existing backend; provider QA open | Compact actual provider logo/action chips; asynchronous updates restore chip focus; no selectable fake loading action. Existing app/deep-link/watch-page handoff only. |
| Settings rail + category cards + standard preference dialogs | DONE for main/category/More/licences presentation | Focus previews category, card grid, compact list/multiselect/text controls, deepest Back hierarchy. Native specialised dialogs remain mixed (see audit). |
| Settings compatibility / Streaming / BitTorrent / OpenSubtitles / Updates | DONE at source-audit level | Existing Streaming enabled controls restored; default remains false. BitTorrent controls dimmed, values/code retained. OpenSubtitles under Subtitles. Inert Updates UI removed. Full key/behaviour audit in UI report. |
| Build information / bundled What's New | DONE | Installed version/code/SHA/date/build type/ABI, copyable report and bundled notes. Unknown upstream commit explicitly unknown; no updater. |
| Search redesign | DONE for local library | Existing local DB query semantics, debounced worker, compact recycled results, normal Details route, query/selection state. No Recent Searches or remote catalogue result universe. Optional streaming enrichment not implemented. |
| Startup/loading improvements | PRESERVED / bounded | Existing early asynchronous cached Home shell from 3.9 retained; no artificial splash or online startup dependency. New aggregates stay on snapshot worker. v40 snapshot cache rebuild means first updated launch may have no old cache. No architecture rewrite/performance acceptance claimed. |
| Whole-app recursive UI/interaction audit | DONE as source audit; visual unification PARTIAL | `NOVA_UI_AUDIT_REPORT.md` explicitly inventories primary routes, nested actions, settings, playback, errors and alternative UI. Remaining legacy utilities are listed; not all were run. |
| Safe finishing touches | PARTIAL | Clock on secondary screens, stable provider focus, safe truncation, scan repeat guard, meaningful unknown-value handling and Back hierarchy. No blanket claim of guarding every action or eliminating all device-specific focus defects. |
| Season-level storage / provider accounts / major upstream changes | NOT DONE — deliberately deferred | Explicitly outside 4.0. Add to List remains disabled; no invented seek thumbnails, playback abilities or provider quality. |

## Changes made and architectural decisions

### Navigation and focus

- `leanback/MainActivityLeanback.java`, `MainFragment.java`: handle new navigation intent for the existing singleTask activity and synchronise actual page/highlight. Consume explicit request on resume as needed.
- `PreviewFocusRecycler.java`, `PreviewSources.java`, `PreviewPages.java`: deterministic content boundaries, skip source headings, recover library controls at first row. Preserve 3.9 Home/episode-row logical focus rather than replacing their implementation.
- `TopNavigation.java`, `overlay/Overlay.java`: secondary-screen clock, avoid duplicate main Overlay clock. Existing artwork/scroll scrim preserved.

### Delete safety and scanner scheduling

- `browser/Delete.java`: replace premature database removal with physical FileCore deletion first; preserve native local/Q permission flow. Local null is pending; FileCore remote null denotes success. File presence checked after positive local/system result. Failed batch member retained until final outcome; counters synchronised. Sidecars only follow successful main deletion. Existing protected-folder and optional folder cleanup logic remains.
- `details/VideoDetailsFragment.java`: visible Preview Cancel/Delete confirmation; failure feedback leaves recoverable Details. Existing actions not replaced with an unindex-only workaround.
- `CustomApplication.java`: preference/lifecycle-driven retry when configured startup/period scan is due, without enabling scanning by default.
- `.github/build/medialib-preview40.patch`: narrow `NetworkAutoRefresh` patch applied to pinned MediaLib `5758074049bc153ac4967b4ecd21886469270076` during CI. WebDAV can be scheduled on a connected network; other protocols retain LAN/VPN gate. `AUTO_RESCAN_LAST_SCAN` written after eligible scan scheduling. Working discovery/import/batch/offline deletion logic not rewritten.
- `PreviewLibraryScan.java`, `VideoPreferencesCommon.java`: manual local + indexed-network entry; existing Full Library Scan extended to include network. Generic label is honest about scan semantics.

### Tables, metadata and Details

- `PreviewLibraryLoader.java`, `browser/adapters/object/Video.java`: expose measured dimensions, collect sizes/runtime/codecs/added/modified/bitrate from existing local cursor; aggregate series once on loader worker. UI cache schema version becomes v40; no media database migration.
- `PreviewLibraryColumns.java`: shared compact row/header rendering and per-section column state; header sorting, D-pad reorder, unknown values handled explicitly. Select delegates to existing Details.
- `PreviewMoviePage.java`: shared left-poster hero; actual EpisodeTags air date/runtime fill missing Video fields; S/E/date/runtime/actual rating/genres separated from technical badges. Cast uses compact round portraits. Full synopsis accessible from More. Real provider actions beside local Play; observers cleaned up and focus restored across their updates.
- `StreamingRepository.java`, `StreamingPreferences.java`, `StreamingActions.java`, `StreamingActionPresenter.java`: restore existing availability/preferences, compact offer rendering/menus, keep provider authentication/discovery out of scope. Existing opt-in default false retained.

### Settings, Search and utility presentation

- `PreviewSettings.java`, `VideoSettingsFragment.java`, `VideoSettingsActivity.java`, `preview_preference.xml`: internal category rail plus compact card grid, focus-driven category content, disabled preference skipping and Back hierarchy.
- `VideoSettingsMoreLeanbackFragment.java`, `VideoSettingsLicencesFragment.java`: full-width compact sub-setting/licence cards over existing preference objects/actions.
- `PreviewPreferenceDialogs.java`: standard list, multiselect and text-edit wrappers retain original change listeners/persistence semantics.
- `PreviewBuildInfo.java`, `VideoPreferencesCommon.java`, `build.gradle`, `assets/preview-whats-new.txt`: 4.0 identity and factual bundled notes. Removed stale Edition 3.5 About label/dialog; attribution logos fit inside their allotted badge width.
- `PreviewSearch.java`, `VideoSearchActivity.java`: existing local query loaders/mapping on cancellable worker; recycled compact results, human metadata, no diagnostic clutter/Recent Searches. Existing global ACTION_VIEW search result route and voice key preserved. NO_EXTRACT_UI prevents the keyboard from replacing the app with a full-screen generic editor where honoured by the installed IME.
- `PreviewDialog.java`, OpenSubtitles/network credential dialogs and artwork tools: bounded Slate styling/read panel, existing form widgets and validation callbacks retained. This is a safe presentation wrapper, not a rewrite of authentication or file operations.

### Playback

- `PlayerActivity.java`: restart actually starts playback; subtitle language grouping uses real metadata, including external localised language labels; audio-delay label corrected.
- `PreviewPlaybackMenus.java`, `PlayerController.java`, `TVCardDialog.java`, `TVMenuItem.java`: compact native-picker hosting, explicit parent return, stable choice focus, repeated adjustment actions stay open, restrained focus treatment. Native player/backend/adjustment callbacks preserved.
- `PreviewPlaybackInfo.java`: readable Slate panel over bright video. Existing approved transport HUD, clock/end time, direct Continue Watching and first-frame transition retained.

The Settings card adapter deliberately extends the public pinned AndroidX PreferenceGroupAdapter (preference 1.2.1). Its library-group restriction is suppressed only on the adapter/grid methods; this is not a platform hidden-API bypass. Re-evaluate that compatibility dependency on AndroidX upgrades. No global lint disable/baseline was added.

## Deliberate non-changes

- No playback-engine, WebDAV importer, media provider schema, credential storage or bulk library rewrite.
- No new account connection backend, global streaming discovery, telemetry, analytics, update service, fake provider quality or placeholder data.
- Existing Android TV launcher-channel sort preferences retained: `ChannelManager` still consumes them even though Preview libraries have separate sorting.
- Existing capability-gated preferences remain subject to their previous Android/device/account rules. Presentation does not promise every setting is active on every Shield/Android version.
- Legacy scan GuidedStep configuration, metadata identification, account authentication, subtitle download, backup/import/export, Android permissions and alternative classic/mobile routes remain listed in the audit. Their working underlying operations were preserved rather than reimplemented during this pass.

## New issues discovered and corrected

- An empty legacy category had the same translated title (“Subtitles”) as the new populated category. The rail selected the empty one; selecting the non-empty category fixes the missing rail entry.
- Service-attribution About preferences had no actual action (their web calls were commented out). They now remain informational and non-focusable rather than appearing actionable.
- External subtitle language helper returns localised names, sometimes with “(HI)”, not always ISO codes. Preferred/English grouping now handles those real return values.
- A mixed delete batch could end in success after an earlier failure; final failure accounting now preserves that failure.
- Database file size can be missing; average episode size must not silently divide a partial byte sum as if complete.
- Provider availability had a selectable loading no-op. Pending offers now remain absent.
- One staged compile failed because a provider-menu replacement also affected the fallback method; corrected before the final build. No failed APK was delivered.

- Release lint exposed an API-24 Html overload with minSdk 23 and literal constants; changed to HtmlCompat and typed constants. Pinned AndroidX adapter restriction handled explicitly as described above.
- Live review caught stale Edition 3.5 About identity and clipped attribution art; corrected.
- Search screenshots initially showed the system full-screen editor; requested non-extract keyboard mode and final smoke captures submit the query before review.

## Visual/reference deviations

- Translucent Slate surfaces approximate frost without live GPU blur.
- Fit-inside thumbnails/posters intentionally letterbox non-matching source ratios instead of cropping; missing artwork is not manufactured.
- Reliable HDR metadata is not available in the existing database for every item, so optional HDR cells remain blank rather than guessing.
- The column chooser uses D-pad Left/Right to reorder. Selecting all optional columns may truncate; defaults follow the compact supplied table.
- Specialist native controls remain inside some compact wrappers; remaining full legacy surfaces and exact reasons are enumerated in `NOVA_UI_AUDIT_REPORT.md`. **This pass is not a full conversion of every nested native utility to the approved board.**
- The screenshots of Series/Episode Details were behavioural evidence; actual episode metadata is shown without inventing parent-series ratings or unavailable fields.

## Testing performed

- Final signed debug and optimised universal release compilation/package succeeded. Release lint dependency passed with 0 errors and 1,381 warnings; warnings were not broadly cleaned up in this focused pass. R8/resource shrinking enabled.
- Six targeted Robolectric checks passed: 64-bit TV year; Continue Watching resume/progression; page/focus rendering; Movie Details playback action/rendering; Episode rating/genre/runtime fallback; TV size/count/average and unknown-last sort.
- API 28 x86_64 emulator: Preview launch, reinstall, restart and warm return; Settings and selected categories; Movies, TV Shows and local Search query. No fatal startup/route exception in captured checks.
- Signed debug-to-release upgrade with Preview enabled and release restart passed. Signature verified against the existing Preview certificate; four native ABI directories present. APK hash matched downloaded bytes.
- Bounded visual review: approved 4.0 references versus staged fixtures/live screenshots. Final About labels/logos and non-extract Search reviewed; no extensive screenshot regression suite.
- Physical Shield playback/network/deletion and populated-library acceptance: NOT TESTED — USER QA REQUIRED.

Visual review used the six 4.0 references, staged native layout fixtures (with clearly synthetic test-only artwork/data) and live emulator screenshots. Render fixtures are not proof of real-file playback, network access, actual metadata correctness across a user's library or physical Shield fidelity. No production fake media was added. The phone-system-image emulator can display its own status bar over the TV layout during startup captures; those captures validate process startup, not physical Shield full-screen composition.

## NOT TESTED — USER QA REQUIRED

1. Upgrade from your real 3.9 install with its full database/preferences; first cold start and subsequent cached start times.
2. Delete a disposable local movie and a disposable WebDAV title; cancel confirmation/Android permission; denied deletion must preserve library entry. USB/SMB and mixed batch/folder cleanup need separate checks.
3. Top-nav matrix from Settings/Search/Details, Network Up at each group, library first-row Up, Back restoration and rapid repeated selection.
4. Sample Movie sizes and TV episode counts/summed sizes/average against the files actually indexed; missing sizes, mixed formats, all column chooser/reorder/header sort combinations and large libraries.
5. Episode S/E/date/runtime, missing episode rating versus known series score, inherited genre; real cast/artwork and provider logo placement.
6. Start/resume/restart a real video; Audio Boost and repeated values; subtitle delays/style/size/language grouping and Back to visible HUD focus; HDR/Dolby Vision/passthrough/frame-rate switching and first real frame.
7. Automatic scan after app return, network restoration and configured periodic time; new WebDAV import on Recently Added; offline NAS must retain indexed media.
8. Home idle rotation and manual Next focus; Continue Watching recency/series-boundary progression (working 3.9 paths preserved).
9. Search with real titles, voice on Shield, opening Details/Back, query changes and large result sets; no provider-only search results.
10. Settings value persistence, account/provider handoff, all native credential/subtitle/network/backup utility screens, long/localised text and Android system dialogues.

## Upgrade/data risks

- Preview package and established personal signing identity are retained; versionCode increases from 6040074 to 6040075. Install over existing Preview rather than uninstalling. Signature mismatch should block installation, not be bypassed.
- No media database migration or credential schema change. FileCore remains pinned at `c4b760c55102c72d45f68b0d16999fc1f4f013a7`; the native/prebuilt revisions remain those in `.github/build/nova-ci.xml`. Existing saved values/defaults preserved by the new presentation, subject to pre-existing device validation. New column settings are separate Movie/TV keys.
- Disposable snapshot cache path changes from v37 to v40. First launch rebuilds from the existing local database; an old cached shell may therefore not populate immediately. Subsequent launches reuse the new cache.
- Release optimisation is now enabled, unlike 3.9's diagnostic APK. Reflection/native interoperability is covered only by the bounded smoke checks; specialised devices/backends still require QA.
- File deletion touches the physical delete/permission/DB-commit sequence. Test with disposable files first; associated-file and optional folder cleanup use preserved existing logic.
- The Settings Streaming switch keeps its existing false default and saved selections. The retained availability handler has a legacy true fallback if that dynamic key has never been initialised; no new provider accounts or discovery universe were added. The UI does not claim accounts are connected or that availability guarantees subscription/playability.

## Known regressions/risks

No claim that all reported hardware problems are conclusively resolved. Highest-priority acceptance remains physical deletion, subtitle nested-menu focus and automatic network scan/recent imports. A native dialog may still have legacy spacing or focus differences on a Shield. Missing database metadata is deliberately omitted; no repeated network stat storm is used to fill it. Opening many optional columns on a narrow interface can truncate.

## Legacy UI audit summary

See `NOVA_UI_AUDIT_REPORT.md` for the recursive inventory and complete preference → behaviour → Active/Disabled → reason table. Remaining legacy/mixed surfaces include scan scheduling/share selection, metadata identification, saved-credential manager, subtitle download/wizard, authentication, backup/import/export progress and confirmations, provider-internal controls, Android permission/keyboard/chooser UI and classic/mobile/filtered collection/list alternatives. Their retention is explicitly documented, not presented as completed reskinning.

## What I expected the user to see

- Home keeps the same design direction, stable hero controls and existing Continue Watching behaviour; Featured can rotate after idle time without stealing hero interaction.
- Movies/TV keep the fixed portrait grid and offer a compact real-data table via List View. Columns can be toggled, reordered and sorted. TV size is the sum of indexed episode-file sizes; complete averages appear as Avg. ep. size.
- Movie, Series and Episode Details share a compact left-poster layout. Episode has S/E, date/runtime and its own rating when available; genres sit near the title, technical badges are separate. Cast portraits are smaller circles with names beside them.
- Network & Files has one clearly labelled Scan Library entry. Navigation and first-row Up should no longer jump to an unrelated section.
- Settings has a left category rail and right card grid, with Subtitles present and Streaming controls restored. BitTorrent/legacy incompatible controls are dimmed. About exposes real Build information and What's New.
- Search is a compact local-library screen with useful title metadata and normal Details/Back behaviour, without Recent Searches.
- Playback overlays are smaller and readable. Multiple adjustments stay in the menu; nested Back returns to the parent/HUD; Play from beginning actually starts at zero.
- Delete presents a visible confirmation; unsuccessful physical deletion should not simply make a title disappear from the library.

## Build reproduction and next-developer entry points

Use the recorded APK source commit on `codex/apk-build-fixes` and `.github/workflows/build-preview-apk.yml`. The workflow resolves pinned repositories from `.github/build/nova-ci.xml`, overlays the triggering Video commit, applies `.github/build/medialib-preview40.patch` to MediaLib, then runs `prepare-preview.py` for the existing isolated Preview package/provider authorities. A plain standalone Video checkout without these steps is not the delivered build tree. The same personal signing key is required for an upgrade; do not regenerate a key or change the Preview package to bypass an installation failure.

The preview validation path runs the signed debug compile and six targeted tests/renders, emulator smoke, then `assembleNoamazonRelease -PmarkSigning -Puniversal -PmarkPreview` and a signed debug-to-release upgrade/restart check. Broad regression tests are not required for this pass. The assemble tasks also invoke their pre-existing lint dependency. CI signing material is not included in the return archive. The workflow’s generated validation summary retains a stale sentence saying lint was deferred; the actual release task ran lint and the passing task/log is authoritative.

The bundled original 4.0 input is reference/specification, not an instruction to redo the pass. The return archive records the exact shipped state and outstanding limitations. Do not start a new feature pass without Mark's next scope/QA report.

## Suggested later work — not implemented in this pass

- Convert the explicitly inventoried GuidedStep/authentication/subtitle/metadata/backup utility layouts after real-device behaviour is captured.
- Add a reliable stored HDR field only with an agreed metadata pipeline; do not infer it from filenames.
- Consider a horizontally scrollable all-columns mode if users need many optional columns at once.
- Season-level storage breakdown and next-unwatched-file size intelligence remain deferred.
- Provider account authentication, broad discovery, Add to List backend and an update service require separate approved scope.
- Profile startup against the actual library after this pass; no aggressive upstream/decoder/dependency rewrite was attempted.

## Exact changed paths relative to delivered Preview 3.9

- `.github/build/check-preview-startup.sh`
- `.github/build/medialib-preview40.patch`
- `.github/workflows/build-preview-apk.yml`
- `assets/preview-whats-new.txt`
- `build.gradle`
- `res/layout/preview_preference.xml`
- `src/main/java/com/archos/mediacenter/video/CustomApplication.java`
- `src/main/java/com/archos/mediacenter/video/browser/Delete.java`
- `src/main/java/com/archos/mediacenter/video/browser/adapters/object/Video.java`
- `src/main/java/com/archos/mediacenter/video/leanback/MainActivityLeanback.java`
- `src/main/java/com/archos/mediacenter/video/leanback/MainFragment.java`
- `src/main/java/com/archos/mediacenter/video/leanback/PreviewDialog.java`
- `src/main/java/com/archos/mediacenter/video/leanback/PreviewFocusRecycler.java`
- `src/main/java/com/archos/mediacenter/video/leanback/PreviewLibraryColumns.java`
- `src/main/java/com/archos/mediacenter/video/leanback/PreviewLibraryLoader.java`
- `src/main/java/com/archos/mediacenter/video/leanback/PreviewLibraryScan.java`
- `src/main/java/com/archos/mediacenter/video/leanback/PreviewPages.java`
- `src/main/java/com/archos/mediacenter/video/leanback/PreviewSources.java`
- `src/main/java/com/archos/mediacenter/video/leanback/TopNavigation.java`
- `src/main/java/com/archos/mediacenter/video/leanback/details/PreviewMoviePage.java`
- `src/main/java/com/archos/mediacenter/video/leanback/details/VideoDetailsFragment.java`
- `src/main/java/com/archos/mediacenter/video/leanback/network/NetworkServerCredentialsDialog.java`
- `src/main/java/com/archos/mediacenter/video/leanback/network/ftp/FtpServerCredentialsDialog.java`
- `src/main/java/com/archos/mediacenter/video/leanback/network/smb/SmbServerCredentialsDialog.java`
- `src/main/java/com/archos/mediacenter/video/leanback/overlay/Overlay.java`
- `src/main/java/com/archos/mediacenter/video/leanback/presenter/ListPresenter.java`
- `src/main/java/com/archos/mediacenter/video/leanback/search/PreviewSearch.java`
- `src/main/java/com/archos/mediacenter/video/leanback/search/VideoSearchActivity.java`
- `src/main/java/com/archos/mediacenter/video/leanback/settings/PreviewBuildInfo.java`
- `src/main/java/com/archos/mediacenter/video/leanback/settings/PreviewPreferenceDialogs.java`
- `src/main/java/com/archos/mediacenter/video/leanback/settings/PreviewSettings.java`
- `src/main/java/com/archos/mediacenter/video/leanback/settings/VideoSettingsActivity.java`
- `src/main/java/com/archos/mediacenter/video/leanback/settings/VideoSettingsFragment.java`
- `src/main/java/com/archos/mediacenter/video/leanback/settings/VideoSettingsLicencesFragment.java`
- `src/main/java/com/archos/mediacenter/video/leanback/settings/VideoSettingsMoreLeanbackFragment.java`
- `src/main/java/com/archos/mediacenter/video/player/PlayerActivity.java`
- `src/main/java/com/archos/mediacenter/video/player/PlayerController.java`
- `src/main/java/com/archos/mediacenter/video/player/PreviewPlaybackInfo.java`
- `src/main/java/com/archos/mediacenter/video/player/PreviewPlaybackMenus.java`
- `src/main/java/com/archos/mediacenter/video/player/tvmenu/TVCardDialog.java`
- `src/main/java/com/archos/mediacenter/video/player/tvmenu/TVMenuItem.java`
- `src/main/java/com/archos/mediacenter/video/streaming/StreamingActionPresenter.java`
- `src/main/java/com/archos/mediacenter/video/streaming/StreamingActions.java`
- `src/main/java/com/archos/mediacenter/video/streaming/StreamingPreferences.java`
- `src/main/java/com/archos/mediacenter/video/streaming/StreamingRepository.java`
- `src/main/java/com/archos/mediacenter/video/utils/OpenSubtitlesCredentialsDialog.java`
- `src/main/java/com/archos/mediacenter/video/utils/VideoPreferencesCommon.java`
- `src/main/java/com/archos/mediacenter/video/utils/credentialsmanager/CredentialsEditorDialog.java`
- `src/test/java/com/archos/mediacenter/video/leanback/PreviewMoviePageTest.java`
- `src/test/java/com/archos/mediacenter/video/leanback/PreviewPagesTest.java`
