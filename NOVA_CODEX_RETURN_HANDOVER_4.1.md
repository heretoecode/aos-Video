# SUPERNOVA / Nova Preview 4.1 — return handover

Date: 19 September 2026. This report is a factual implementation hand-back, not physical-device acceptance. The signed Preview APK has been built and delivered for user testing; the unverified regressions below remain open until user QA.

## Authority and starting point

- Authority: `Nova_Codex_Handover_2026-09-19_4.1_FINAL.zip`, SHA-256 `2519be139fdd52950a4d5aee1908b4fc0df8921909e43e001f2aceb91dbb6ae0`. Read all supplied files before implementation. Its eight manifest checksums matched.
- There was no `APPROVED_REFERENCES` directory or current 4.1 reference board in that package. The only supplied image was in `DEFERRED_4.2`; it was reviewed as deferred context, not implemented. The written 4.1 specification controlled this pass.
- Continued `heretoecode/aos-Video`, branch `codex/apk-build-fixes`. Starting branch head `8b6178fa624edf85752b9f6a8dad2d81610e15d4`; delivered 4.0 APK source `345636900b577bfec90a22d117fe9b6d062b9f2b`.
- No older handover scope was independently merged. No 4.2 feature pass started.

## Build and delivery identity

- Version: **6.4.63-mark.4.1-preview**; versionCode **6040076** (4.0: 6040075).
- Package: **org.courville.nova.markpreview**, unchanged. Existing signing path retained.
- Certificate SHA-256: `89ac087ed6f989c90482d4a999f80511fe6ceee26ef1b9c37a142a9f00d39a5a`.
- Final APK source: 12f864a9bf56709afba069a234b22dd765f887d4.
- Final build: https://github.com/heretoecode/aos-Video/actions/runs/35436554201.
- APK: `NOVA_Preview_4.1.apk`, optimised universal release, R8/resource shrinking.
- APK SHA-256: 2562f755bc45df913f87290dcab76ae0fd07642618b33c2a146f69312e376a15.
- MediaLib remains `5758074049bc153ac4967b4ecd21886469270076` plus the existing `.github/build/medialib-preview40.patch`. FileCore remains `c4b760c55102c72d45f68b0d16999fc1f4f013a7`.
- APK Drive link: https://drive.google.com/file/d/1Dk4rNNgVeHTQQ0woVV-5YUahIYDKfqky/view
- APK size: 82,305,201 bytes.
- Drive: `Nova video player preview / APKs to Test`; reports and return bundle in `Handovers`. Cumulative `NOVA_Release_Notes.txt` updated in place, preserving prior history.

## What I expected the user to see

1. Install over 4.0. Open the redesigned interface with the same library and preferences. The in-app top bar says SUPERNOVA; launcher icon/package remain unchanged. Search precedes Settings on the right.
2. Home has a stable Featured area with factual local-library context, Play and More Info, and a dot/elongated-marker carousel. At the bottom, Customise Home opens a dedicated row editor. Select a row, move it with Up/Down, Select to finish; Back returns Home. Hide preserves contents/history.
3. Long-press a title, or use Details, to Add to Row. Watch Next requires no playback. Create a named custom row using the device keyboard; rename/delete it in the row editor. Empty rows are not filled with invented data.
4. Continue Watching retains an unfinished final episode. Briefly sampling a later episode saves that file's resume but does not immediately move the series journey. The series Details action uses that same journey and says Resume for an unfinished current episode.
5. Details shows essential metadata, a short synopsis and actions. More Info opens a full-screen informational extension with full plot, cast/crew, technical/file data where available; Back restores the More Info action.
6. Settings has a category rail, controls and a help panel. Appearance offers Accent & Colour. Deprecated controls are preserved in a dimmed, non-focusable Legacy category.
7. Playback shows the restrained Preparing playback… presentation. HUD has direct Audio/Subtitles and selection labels, central transport and secondary Speed/Info/More. Nested menus share an anchor. Up Next appears near the end in existing series/binge playback when the next local episode is known.
8. Network & Files retains its page structure with embedded scan status. Storage browsing is a dark list; indexed-folder actions use a compact panel. Backup success requires a complete readable archive and destination verification.

## Implementation, decisions and limits

### Home, navigation and accent — implemented

`TopNavigation`, `PreviewPages`, `PreviewHomeRows` (new), `PreviewAccent` (new), `PreviewBackdrop`, `PreviewCardPresenter`, `PreviewLibraryColumns`, `PreviewSearch`, `PreviewSources`, `PreviewNotice` (new).

- Text branding, Search/Settings ordering, status-only clock and selected/focused distinction. Retained global top navigation and its scrolled scrim; Settings rail is internal only.
- Featured interleaves/deduplicates local Recently Added and matching cached/live Trakt Trending/Popular signals, bounded to eight candidates. Independent source switches; local fallback when online charts are unavailable. Hiding Recently Added does not remove that Featured source.
- Two actions, fixed title/synopsis geometry, useful metadata, no technical hero badges. Left from Play/right from More Info changes the carousel; automatic rotation waits about 30 seconds without hero interaction. No fake logo or chart data.
- Default-preference JSON holds ordered system/custom rows, visibility and explicit membership. Watch Next defaults enabled; empty rows are omitted. System rows cannot be deleted. Custom row names are limited to 40 characters.
- Continue Watching dismissal records the activity baseline and preserves bookmarks. Actual playback clears dismissal; merely focusing or opening Details does not. Qualifying titles are suppressed from Recently Added, including dismissed titles until they cease qualifying. Completed series regain ordinary Recently Added eligibility.
- Accent presets: Slate Blue, Cyan, Emerald, Purple, Amber, Red; Custom RGB sliders. Applied to common focus surfaces, selected controls, top-bar underline, switches, progress and utility gradients. Media artwork remains untouched. Some retained specialist/native surfaces still use their own theme (audit below).
- Library control sizes and poster geometry retained; added outer spacing. List rows use separators/tint instead of rounded boxes. Remembered section artwork is requested immediately on returning from a utility; decoding/cache timing still needs device QA.

### Series journey, Continue Watching and Resume — implemented, device acceptance pending

`PreviewSeriesJourney` (new), `PreviewLibraryLoader`, `PlayerService`, `DbUtils`, `TvshowFragment`, `PreviewMoviePage`.

- Root cause: file bookmark/last-played and percentage-based watched status were being used directly as the series journey. A positive final-episode bookmark could be excluded by the seen flag; a brief later-episode visit could take over progression.
- Central committed cursor is separate from each file bookmark. Threshold is `max(60 seconds, min(180 seconds, 10% runtime))`. It counts sampled advancing playback time, excluding seeking/stalls, rather than treating a seek to a late position as intent. Existing one-second player tick supplies samples.
- True completion and explicit Mark Watched are strong signals; normal visits commit only after the threshold, except updating the already committed episode. Manual mark events are consumed once. First 4.1 selection migrates existing progress; attempts first made in 4.1 do not masquerade as old progress.
- Positive resume takes precedence over the percentage-seen flag. The completion sentinel remains authoritative. Current unfinished episode can therefore remain Continue Watching even if it is the last available episode.
- Home and series primary action consume the central selection; episode cards retain their own real file progress. A one-episode series with an existing unfinished bookmark gets Resume.
- Auto-next now updates the Intent's video ID/payload as well as service state. Manual previous/next and early Up Next use a non-completion transition; they do not falsely mark the old episode completed.
- Row/journey state lives in the existing default preferences so the existing Settings backup includes it. Local database schema and native bookmark representation are unchanged.
- Limit: if the committed episode disappears from the indexed library, there is no new speculative recovery/jump policy. Re-indexing/removed-file behaviour needs QA. Optional Reset Progress was not added; it was conditional scope, not a required new backend.

### Playback reliability — safeguards implemented; original hardware causes not reproduced

`PlayerService`, `PlayerActivity`, `DetailsBackdropController`.

- Re-enabled the existing approximately 30-second resume checkpoint callback. It uses the existing serial `IndexHelper` writer and immutable snapshot path. Periodic writes are local; normal pause/stop policy still controls network sidecars, avoiding a new network write every 30 seconds.
- Saving is gated on a real started playback session and applied starting position. On a reported player failure, the last valid sampled position is preserved rather than accepting a reset/invalid decoder clock. Error logs include error/qualifier, state, duration, last position and resume source.
- Preserved existing decoder, passthrough, refresh-rate, seek/resume selection, Trakt and first-frame machinery. No speculative audio offset or decoder rewrite.
- Details backdrop cancellation no longer clears an already composed image on stop. Preview player exit uses no extra activity transition, avoiding the identified artwork-clear/rebind source of flicker.
- A/V desync and unexplained returns to Details were **not reproduced** here. The pass does not claim their root cause or a complete fix. Decoder/platform logs and exact file/output conditions from Shield remain necessary if they recur.
- A forced process kill/power loss can still lose progress since the last completed checkpoint. This is not a crash-proof transactional playback recorder; neither async preference writes nor remote sidecars can be guaranteed after process death.

### Playback HUD, menus, loading and Up Next — implemented, real playback QA pending

`player_controller_experimental.xml`, new Previous/Next/Speed/Info vector resources, `PlayerController`, `PlayerActivity`, `PreviewPlaybackLoading`, `PreviewPlaybackMenus`, `PreviewUpNext` (new).

- Top-left actual title plus Season • Episode/name; preserved current-time/end-time sizing and existing calculation. Bottom-left real track controls and labels; centre previous/rewind 10/play/forward 10/next; right speed/info/more. Episode previous/next is conditional on known local adjacent episodes.
- One 350 × 310 dp top-right menu footprint (32 dp right / 74 dp top); child header/Back restores parent and originating row. Repeated adjustments and existing handlers retained. Back closes visible menu/HUD before allowing a later Back to exit.
- Preparing playback… uses cached backdrop/title and episode context only, retaining existing first-frame handoff without a new minimum display time.
- Up Next uses existing series/binge mode and a known next local episode. Genuine credits/outro start in the latter half is preferred; fallback is the last 5% clamped to 10–30 seconds. Countdown is 15 seconds, pauses with playback, and supports Play Now/Cancel. Cancel cancels that queued advance, not the user's saved play-mode preference. Early transition preserves unfinished resume semantics.
- Final input review found the existing HUD intercepts D-pad arrows, making a non-modal Up Next panel hard to reach. Up now explicitly enters the visible prompt (with an Options hint), Left/Right selects Play Now/Cancel, Down returns HUD; press/release are consumed together so choosing an action cannot also pause/seek. Back cancels a focused prompt, including the predictive-Back route. The prompt does not steal focus merely by appearing.
- Limit: no official title-logo field is available in the current media model/cache, so this implementation uses clean text. No network logo lookup, generated logo, seek thumbnail or unsupported transport feature was invented. If no next episode is known/cached, controls/prompt are omitted. Hardware timing, remote reachability and transitions remain QA items.

### Details — implemented

`PreviewMoviePage`, `TvshowFragment`.

- Essential year/season/episode/runtime/rating/genres remain human-readable; episode rating does not inherit series rating. Constrained synopsis precedes actions. Deep codec/audio/file information moved to More Info; measured resolution can remain on Main Details.
- More Info is full-screen and informational, with real available plot/cast/crew/technical/file sections; no duplicate Play/Trailer/Add actions or fabricated reception. Restores originating focus when dismissed.
- Found Cast/Related rebuilding views on metadata/snapshot updates without preserving the focused card. Added stable card keys, logical focus restoration and bounded horizontal navigation; episode rows retain their existing handling.
- Existing circular cast, episode rows, genuine Trailer and native More actions retained. Add to Row replaces the placeholder. Live metadata and full navigation require device QA.

### Settings and Streaming — implemented using retained handlers

`PreviewSettings`, `PreviewAccent`, `PreviewBuildInfo`, `VideoPreferencesCommon`, English `strings.xml`, bundled `preview-whats-new.txt`.

- Three-panel layout; language moved to Appearance; empty General omitted. Added Rows, Featured switches and Accent & Colour; obsolete Home/TV/start/sort/theme controls moved to Legacy.
- Legacy entries are disabled/non-selectable only in this presentation. No key, saved value, default or underlying preference implementation was deleted/reset. Projector Mode investigation found top-centred video/effect surface alignment versus normal centring; code remains and saved value still applies. Player setup now explicitly applies both true and false, avoiding stale alignment.
- Existing Streaming capability was already enabled by the 4.0 source and its preferences restored; 4.1 keeps it accessible. It remains user-switch controlled (not forcibly switched on), factual availability/handoff for indexed titles only. No provider accounts, new playback backend, guessed quality or remote catalogue were added.
- Full preference disposition and remaining native controls are in `NOVA_UI_AUDIT_REPORT_4.1.md`. Upstream legal/sponsor/engine attribution is deliberately not rebranded; launcher branding is deferred.

### Network, storage and export — implemented; real sources/SAF QA pending

`CustomApplication`, `PreviewLibraryScan`, `PreviewPages`, `ListingFragment`, `ListPresenter`, `MetaFileListPresenter`, `NetworkShortcutDetailsFragment`, `SeasonFragment`, `PreviewDialog`, `PreviewNotice`, `MediaLibraryBackupService`, `VerifiedBackup` (new).

- Automatic scan finding: a fresh lifecycle request could retain the previous request timestamp; the old scan completion could immediately acknowledge/clear it. New requests reset that timestamp. `SupernovaScan` logs startup/due/busy/request decisions. Existing startup switch, schedule, connectivity retry and per-source selection remain. The pinned 4.0 scheduler patch is retained; WebDAV discovery/import/deletion safeguards are untouched.
- The physical cold-restart WebDAV failure was not reproduced without the user's source. This bounded fix/instrumentation must not be represented as a confirmed Shield auto-scan fix.
- Scan Library requests the existing local/indexed-network scan. Full Library Scan additionally retries unmatched descriptions through the existing scraper path. It is not a partial-versus-full source distinction; labels were not blindly changed to Partial Scan.
- Embedded Network & Files status shows scanner/scraper activity/counts. Storage uses existing list handlers, modern focus and fitted icons. Fixed a shadowed/uninitialised presenter context. Folder panel delegates existing Open/Rescan/Remove/index actions. Delete-all warning keeps the existing physical-delete/failed-item safety.
- Activity feedback uses small non-focusable auto-dismiss notices; background-service feedback retains a Toast fallback. Native OS notifications and some legacy route feedback remain.
- Export stages to a temporary file, closes/fsyncs, checks required non-empty entries plus every entry's size/CRC, and publishes only a valid ZIP. Captures destination URI, obtains persistent permission where supported, starts through foreground-service path, verifies byte count and SHA-256 readback. Failed newly-created picker documents are cleaned up; valid local staged backup remains available on copy failure.
- Tests validate a non-zero readable archive fixture and failures. They do not prove backup/restore against Mark's actual database or every Android document provider. Full Backup & Restore redesign remains 4.2.

### Because You Watched — audited, engine deliberately unchanged

`PreviewPages` chooses the recent playback seed and calls `PreviewDiscovery.similar`. That function intersects normalised genres, excludes the seed and sorts matching local movies/shows by date added descending. This is a simple local genre heuristic, not a trained recommendation system or personalised Trakt recommendations. No new recommendation backend or ranking promise was introduced.

## Actual verification and build cycles

No Android SDK/device was available locally; builds and Android API 28 x86_64 emulator checks ran through the existing GitHub Actions workflow. Physical Nvidia Shield was not accessed.

| Cycle | Source / run | Actual result |
|---|---|---|
| 1 — journey/checkpoints/export | `de378b054e3ebf47cd16c8950de091b6852a7a93` / `35414480703` | Debug compile, targeted tests, startup/reinstall/restart/warm return and selected routes passed. |
| 2 — Home/Details/Settings | `6de8499db06d887be0785b9a5d50742b985a1b5b` / `35414911199` | Same checkpoint checks passed; rendered and live Settings review found obsolete animation sort/deep Details pills, corrected subsequently. |
| 3 — playback/storage | `45524006c67247ff1fb06de6e3c64d518596e9a5` / `35415355650` | Debug compile, targeted tests and emulator route/startup checks passed. |
| 4 — first optimised release attempt | `9f1557529a0113e2b2a242788c952d180ea5133d` / `35435811234` | Debug compile, 25 targeted tests (0 failures/0 skipped; 11.486 s) and emulator routes passed. Release lint stopped on one new WrongConstant error in PreviewAccent; no APK delivered from this attempt. Numeric orientation replaced with LinearLayout.VERTICAL; lint was not bypassed. |
| 5 — lint correction checkpoint | `641b0059a2c44487aa54578ac78467b50be5ce77` / `35436348692` | Debug compile and quick tests passed; automatically cancelled at emulator setup when final focus/input correction superseded it. No delivery from this checkpoint. |
| 6 — final signed release | `12f864a9bf56709afba069a234b22dd765f887d4` / `35436554201` | PASS: signed debug and optimised universal release compiled/generated; 26 targeted tests, 0 failures/0 skipped (13.816 s); API 28 x86_64 Preview startup/reinstall/restart/warm-return and selected Home/Movies/TV/Settings/Search routes; signed debug-to-release install-over and release restart; pinned signature and four native ABIs; downloaded archive/APK hashes and ZIP integrity verified. No physical Shield tests. |

Targeted tests cover the final-positive-bookmark case, meaningful-intent threshold/jump, completion progression, 64-bit TV premiere year, Details playback delegation, episode-specific metadata, local TV storage aggregation, archive creation/failure, and final row/Up Next/resume-policy checks where recorded by the final build. Render fixtures contain synthetic media only for tests; none was added to the shipped library.

Bounded visual review examined fixture Home/grid/list/Details/HUD/menu layouts and live emulator Settings categories. This is not a populated Shield screenshot comparison or physical playback test. No exhaustive D-pad, full regression, protocol matrix or new broad test infrastructure was used.

## Acceptance checklist

PASS is limited to the evidence stated; code inspection alone is not runtime acceptance. “NOT TESTED” below means **NOT TESTED — USER QA REQUIRED**.

| # | Requirement | Result / evidence |
|---|---|---|
| 1 | Install over actual 4.0 and launch | NOT TESTED on existing 4.0 user data. Same package/certificate, incremented code; final emulator upgrade evidence recorded above. |
| 2 | Global top nav | PASS: rendered pages and live emulator routes retain top bar; Settings-only rail. |
| 3 | Featured | PASS for fixture rendering/actions/geometry; live Trakt intersection, auto/manual remote feel NOT TESTED. |
| 4 | Reorder/hide all Home rows | PASS: Watch Next add-without-playback, reorder/hide and preserved membership exercised through real dialog handlers in Robolectric; populated Home return/focus NOT TESTED. |
| 5 | Custom row keyboard/rename/delete | NOT TESTED with TV keyboard; handlers implemented, no physical IME available. |
| 6 | Watch Next without playback | PASS: Watch Next add-without-playback, reorder/hide and preserved membership exercised through real dialog handlers in Robolectric; actual Shield long-press/remove flow NOT TESTED. |
| 7 | Final unfinished episode | PASS: targeted positive-bookmark/seen-flag regression fixture; Widow's Bay real file NOT TESTED. |
| 8 | Brief later episode | PASS: record/select threshold fixture; real timed playback/auto-next NOT TESTED. |
| 9 | Dynamic series Resume | NOT TESTED on Dark Matter library; central consumer implemented, Movie Resume delegation test passed. |
| 10 | Playback Back hierarchy | NOT TESTED during real playback; corrected controller/menu paths inspected; Up Next selection key-consumption fixture passed (not a full menu Back runtime test). |
| 11 | Anchored submenus | NOT TESTED as live player interaction; shared window geometry inspected and layout renders reviewed. |
| 12 | Up Next | PASS for real-credits-versus-fallback trigger calculation and remote selection/release consumption fixtures; real playback/countdown/cancel/remote focus NOT TESTED. |
| 13 | Abnormal-exit resume | NOT TESTED with WebDAV/decoder failure. Periodic save/error guard implemented; resume-policy tests recorded above. |
| 14 | Exit frame | NOT TESTED with real video; identified artwork clearing removed. |
| 15 | Automatic scan | NOT TESTED against user WebDAV; stale acknowledgement finding/fix and instrumentation documented above. |
| 16 | Manual scan | NOT TESTED with mounted/network media; existing scanner delegation retained. |
| 17 | Non-zero readable export | PASS: verified ZIP fixture and corrupt/missing-data failure paths. Actual database/SAF/restore NOT TESTED. |
| 18 | Storage/folder actions | NOT TESTED on physical/network storage; working action handlers preserved. |
| 19 | Embedded scan progress | NOT TESTED with a live network scan; status UI remains in existing page. |
| 20 | Local-only Streaming | NOT TESTED with live provider lookup; existing indexed-title integration retained and Settings route opened. |
| 21 | Legacy retains behaviour | PASS for source audit and live Settings opening/dimmed presentation; every preference/output behaviour NOT TESTED. |
| 22 | SUPERNOVA/package | PASS for visible rendered/live header and pinned package/signing verification; launcher assets unchanged. |

## Risks, limitations and targeted user QA

- Install over Preview 4.0 without uninstalling. No database/credential schema migration. Disposable UI cache version changes to v41 and is rebuilt; first launch may briefly need local data refresh. Older v40 cached UI is not imported as truth.
- New preference-backed row memberships use local title keys. Deleting/reimporting a file or restoring a library with changed IDs may orphan membership. Do not promise portability across unrelated databases.
- Legacy being disabled does not turn its underlying saved behaviour off. In particular saved Projector Mode and classic theme/channel preferences remain meaningful on retained routes.
- Check Widow's Bay final episode, Dark Matter one-episode Resume, briefly opening a later episode, true completion, auto-next immediate exit and CW dismissal/resume. Confirm ordering after actual playback.
- Check HUD Back/Down/idle, Audio/Subtitles selections and children, previous/next, Up Next Play Now/Cancel, pause during countdown, first-frame/exit and clock/end time. Include passthrough/refresh-rate/HDR and the files that previously desynchronised/stopped.
- Add a WebDAV file; exercise configured startup/period and manual scan, offline recovery and Recently Added. Capture `SupernovaScan` and playback-failure logs if the issue remains. No credentials should be shared in logs.
- Export to the actual preferred document provider; check a non-zero readable ZIP. Restore only as a deliberate separate QA action with a recoverable backup; no destructive restore test was run here.
- Check row keyboard/naming/rename/delete, focus on Home return, hidden row contents, accent contrast, long metadata/localisation, storage/folder/delete cancellation/failure and provider availability.

## Intentional deferrals and exact presentation limits

- No 4.2 Collections, profiles, library intelligence, expanded subtitle services, backup redesign, icon/launcher redesign or diagnostic search feature.
- No new recommendation engine, provider accounts, streaming catalogue/decoder, fabricated media assets/metadata or telemetry.
- No supplied current 4.1 image was silently replaced by a new mock-up. Text logos substitute only because no genuine logo field exists; translucent shapes/gradients approximate frost without live blur.
- Whole-app visual unification remains partial: system picker/keyboard, account/credential flows, specialist scan/metadata/subtitle/native controls and classic/mobile alternatives are inventoried in the UI audit.
- Later pass suggestions (not shipped): user-QA-led playback/scan root-cause follow-up; robust ID remapping for custom memberships after library replacement; optional episode-only Reset Progress; consider removal of proven-unused Legacy controls only after dependency/device audit. No such new pass began.

## Codex usage

Starting allowance/credits: unavailable. Ending allowance/credits: unavailable. Calculable consumption: unavailable. The environment exposed no reliable credit/token billing counter; no estimate is supplied.

Significant cycles are listed above. Most costly/complex areas were separation of per-file progress from committed series intent, existing native-player session/next-ID handling, full multi-repository Android/R8 builds and emulator setup, and backup DB/URI lifecycle investigation. No parallel sub-agents were used.
