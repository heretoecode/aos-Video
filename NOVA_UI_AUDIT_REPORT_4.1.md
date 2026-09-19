# SUPERNOVA Preview 4.1 — UI and preference audit

19 September 2026. Source build: 12f864a9bf56709afba069a234b22dd765f887d4. Authority: `Nova_Codex_Handover_2026-09-19_4.1_FINAL.zip`. No current approved-reference image was supplied outside the deferred 4.2 folder. This report uses written 4.1 requirements and bounded implementation/live-emulator review; it does not certify physical Shield appearance or interactions.

## Surface inventory

| Surface | 4.1 disposition | Remaining limits / QA |
|---|---|---|
| Global navigation | SUPERNOVA text; Home/Movies/TV/Network; gap; Search/Settings/clock. Top navigation retained, current/focus distinct. | Launcher/package unchanged. Upstream legal/engine attribution remains Nova. Long translations/overscan need QA. |
| Home Featured | Fixed geometry, local candidate carousel, factual context, Play/More Info, markers, source switches. | No real logo field in current metadata model; text fallback. Offline/live chart mixtures and TV remote feel need QA. |
| Home rows | Dedicated full-screen Customise Home; all system rows movable/hideable; Watch Next/custom membership; Add to Row. | Empty rows omitted, not populated with fake cards. Native device keyboard remains. Real library/focus restoration needs QA. |
| Movie/TV grid | Existing fixed poster/card geometry and controls preserved; more outer control spacing. | Unusual artwork safely fits/letterboxes. No destructive cropping or image-sized grid introduced. |
| List view | Straight row tint/separators, accent focus, existing configurable columns/storage metadata. | Many selected columns/long titles can truncate; exhaustive combinations not tested. |
| Details | Main metadata, short plot, real actions, circular cast and episodes preserved; deep technical content in More Info. | Stable focus keys/restoration added for asynchronous Cast/Related refreshes; horizontal row boundaries retained. Physical row focus, metadata cases and real trailers need QA. |
| More Info | Full-screen informational extension; full plot/cast/crew/technical/file data as available; no duplicate media actions. | No fabricated reception/reviews. No new information backend. |
| Playback loading | Cached backdrop/dark readability treatment; actual title, episode context, Preparing playback…. | Genuine logo unavailable; text fallback. Native first-frame/platform display switching governs final handoff. |
| HUD | Direct selected Audio/Subtitles, central episode/10-second transport, Speed/Info/More; existing clock/end time preserved. | No invented seek thumbnails/chapters/unsupported controls. Real playback and focus require Shield QA. |
| Playback menus | Shared compact top-right footprint with child/parent focus restoration; native actions retained. | Specialist native flows reached from an action can retain their own implementation. No live playback matrix tested. |
| Up Next | Compact overlay, genuine known next episode, timing metadata/fallback, countdown/Play Now/Cancel. | Existing series/binge mode and cached next episode required. Up explicitly selects the visible prompt; Left/Right/Select and Down-to-HUD are routed without leaking seek/pause keys. Countdown/remote reachability still needs real-device QA. |
| Settings | Internal category rail, centre controls, right contextual help; accent icons/switches; Legacy at bottom. | Native preference widgets/handlers retained. No global left sidebar. System capability gates still apply. |
| Accent & Colour | Presets plus RGB custom controls; shared focus/nav/progress/utility surfaces respond. | Not a whole-app recolouring. Some older specialist controls retain original theme; artwork stays neutral. |
| Network & Files | Existing destination structure, utility gradient and embedded scan status. | Real scanner/source status requires actual sources. Native scan configuration remains. |
| Storage list/folder actions | Dark list/focus, fitted icons; compact real Open/Rescan/Remove/index actions. | Authentication, permission, offline failure and all source types need QA. |
| Delete all episodes | Warning icon, explanation, Cancel default focus, destructive colour; original deletion safety retained. | Physical deletion/cancel/denial/mixed failures not exercised on user storage. |
| Action feedback | Bottom-right non-focusable auto-dismiss notice where an Activity is available. | Background services retain Android Toast/notification fallback; not every old toast was replaced. |
| Search | Existing local-library search preserved, utility gradient aligned. | No unrelated search redesign or deferred diagnostics/history/catalogue added. |
| Trailer | Existing working real-metadata trailer path preserved. | Provider/web player internal controls remain provider-owned; no new trailer engine. |
| Streaming | Existing indexed-title availability/handoff and Settings accessible, global user switch respected. | No provider accounts/new backend/guessed quality. User's previous switch remains unchanged. |

## Preference audit — what Active and Disabled mean

**Active** means the existing handler remains reachable subject to its original device/account/capability gates. **Disabled in Legacy** means the redesigned Settings presentation is dimmed, non-selectable and skipped by D-pad; the saved value and underlying code remain. It does not mean the behaviour is forcibly switched off. Opening Settings does not reset decoder/audio/software-decoding values. This is a code/dependency audit, not a claim that every option was runtime tested.

The retained handler inventory from the previous audit was reused where unchanged. The following 4.1 disposition supersedes older audit statements about Streaming, Projector Mode, row sorting and BitTorrent availability.

| Preference / key | Underlying behaviour found | Active/Disabled | Reason / 4.1 treatment |
|---|---|---|---|
| Home rows (`preview_home_editor`, `preview_home_rows41`) | Explicit local membership, order and visibility; no playback mutation. | Active, new | Dedicated row editor from Home and Settings. |
| Featured Recently Added / Trending / Popular (`preview_featured_recent`, `_trending`, `_popular`) | Select local candidates independently of visible Home rows. | Active, new | All initially enabled, local fallback when charts unavailable. |
| Accent & Colour (`preview_accent_picker`, `preview_accent41`) | Common Preview focus/selected/progress/utility styling. | Active, new | Replaces presentation of old colour-theme picker. |
| UI language (`ui_lang`) | Existing locale/reload handler. | Active | Moved to Appearance; empty General removed. |
| Remember library views (`remember_library_views`) | Separate Movie/TV view, filters, sort/order. | Active | Existing behaviour retained. |
| Build / What's New / About | Actual installed version and bundled text; existing copy/info handlers. | Active | Updated 4.1 identity/content. |
| Always start TV (`always_leanback_on_tv_key`) | Entry/UI-choice routing. | Disabled in Legacy | Obsolete normal Preview control; saved routing choice/code retained. |
| Try new UI (`try_new_ui`) | Selects redesigned versus classic presentation. | Disabled in Legacy | Presentation control retired; saved enabled value and underlying gate retained. |
| Reset last played (`reset_last_played_row`, `reset_last_played_section` if present) | Existing destructive DB/history reset action/category. | Disabled in Legacy | No accidental reset when managing new Home rows. |
| Movie row sort (`preferences_movie_sort_order`) | Classic Home/channel ordering. | Disabled in Legacy | Preview has its own sort; channel consumer and saved value retained. |
| TV row sort (`preferences_tv_show_sort_order`) | Classic Home/channel ordering. | Disabled in Legacy | Same reason; no underlying code removal. |
| Animation row sort (`preferences_animes_sort_order`) | Classic animation/channel ordering. | Disabled in Legacy | Not a new Preview row sort control. |
| Smart Recent (`smart_recently_rows`) | Classic recent-row grouping queries. | Disabled in Legacy | New Home builds its own rows. |
| Separate animation (`separate_anime_movie_show`) | Classic collection/movie/TV grouping. | Disabled in Legacy | Preview combined libraries retained. |
| Last added (`show_last_added_row`) | Classic Home row visibility. | Disabled in Legacy | New row model controls Preview visibility. |
| Last played (`show_last_played_row`) | Classic Home row visibility. | Disabled in Legacy | Saved history not deleted. |
| Watching/up next (`show_watching_up_next_row`) | Classic Home row visibility. | Disabled in Legacy | New Continue Watching row visibility is independent. |
| All movies (`show_all_movies_row`) | Classic Home row visibility. | Disabled in Legacy | Preview full library remains a destination. |
| All TV shows (`show_all_tv_shows_row`) | Classic Home row visibility. | Disabled in Legacy | Same. |
| All animations (`show_all_animes_row`) | Classic Home row visibility. | Disabled in Legacy | Same. |
| Documentaries (`show_documentaries`) | Classic Home documentary row. | Disabled in Legacy | No older handover feature pulled into 4.1. |
| Hide trailer row (`hide_trailer_row`) | Classic Details trailer-row presentation. | Disabled in Legacy | Preview uses conditional real Trailer action. |
| Rating rows (`show_by_rating`) | Classic Home rating browse rows. | Disabled in Legacy | No corresponding current Preview row. |
| Projector Mode (`player_projector_mode_key`) | `SurfaceController` aligns video/effects `TOP | CENTER_HORIZONTAL` versus normal centre. | Disabled in Legacy | Existing saved value still applies. Explicit false now resets stale alignment; no code deleted. Hardware/output QA pending. |
| Old colour theme (`app_theme`) | `ThemeManager` colours retained native/classic routes. | Disabled in Legacy | Preview uses Accent & Colour; old saved theme remains for those consumers. |
| Download path (`preferences_torrent_path`) | Existing torrent download directory. | Disabled in Legacy | Already unavailable in Preview; not exposed as working. |
| Blocklist (`preferences_torrent_blocklist`) | Existing torrent blocklist configuration. | Disabled in Legacy | Same; no torrent implementation removed. |
| Decoder / software decoding (`dec_choice`, `force_software_decoding`) | Existing MediaFactory/player backend selection. | Active | No reset/default change; no speculative reliability rewrite. |
| Audio interface/decoder (`audio_interface_choice`, `audio_decoder_choice`) | Existing native output/backend selection. | Active | Preserved. |
| Passthrough (`force_audio_passthrough_multiple`, `force_passthrough`) | Existing capability-gated HDMI/audio output configuration. | Active | Physical Shield/receiver QA required. |
| Dynamic audio delay / parser sync / spatialisation / downmix | Native timing/parser/platform/downmix configuration. | Active | No guessed AV-offset change. |
| Refresh rate / Dolby Vision / 3D / display cutouts | Existing display/effect/format configuration. | Active | Saved values and device gates retained. |
| Playback speed / AudioTrack speed (`playback_speed`, `audio_speed_audiotrack`) | Existing tempo/output path. | Active | Moved to Playback where appropriate; HUD reuses handlers. |
| Preferred/original audio (`favAudioLang`, `prefer_original_audio_track`) | Track selection policy. | Active | Direct HUD selection does not erase global policy. |
| Stream buffer / max I-frame (`stream_buffer_size`, `stream_max_iframe_size`) | Native buffer configuration. | Active | Unchanged. |
| External player (`allow_3rd_party_player`) | Existing player chooser/delegation. | Active | No replacement player introduced. |
| Pause controls / negative remaining time (`hide_controls_on_pause`, `make_time_negative`) | HUD timeout/time formatting. | Active | Existing policy preserved. |
| UI mode / zoom / phone resume box / brightness | Existing classic/phone/routing/density behaviour. | Active where platform permits | Not silently removed merely because TV Preview does not use every control. |
| Hide watched / ignore articles (`hide_watched`, `sort_ignore_articles`) | Supported classic browsers/playlists/channel sorting. | Active | Preview full grid uses separate query/sort; do not promise these filter every Preview view. |
| OpenSubtitles credentials / preferred/download languages / encoding / default hide | Existing subtitle authentication/download/selection/decoding. | Active | Retained in Subtitles; no new subtitle platform. |
| Trakt sign-in/out / scrobble / resume / force push/pull | Existing account and synchronisation services. | Active | Existing confirmations/account gates retained. |
| Streaming enabled / country / selected/preferred providers / search/show-all / refresh | Existing availability repository/preferences and provider handoff for indexed titles. | Active | 4.0 backend flag already enabled; previous user choice retained, not forcibly enabled. No invented accounts/data. |
| Saved credentials / SMB implementation/discovery / SFTP / VPN/mobile | Existing FileCore/network/credential handlers. | Active | Working subsystem retained. |
| Network bookmarks (`network_bookmarks`) | Existing external bookmark sidecar policy. | Active | Normal pause/stop honours it; periodic safety writes remain local. |
| Remote thumbnails / NFO parse / stored metadata / automatic scrape / scrape language | Existing scanner/scraper/presenter paths. | Active | No new metadata pipeline. |
| Rescrape all/movies/collections / sort-title rebuild / NFO export | Existing explicit metadata maintenance actions. | Active | Original handlers/confirmations retained. |
| Scan Library | Local and indexed-network scanner request. | Active | Current page action; embedded status. |
| Full Library Scan (`rescan_storage`) | Local/indexed-network scan plus retry unmatched descriptions. | Active | Summary now explains difference; not blindly renamed Partial Scan. |
| Startup/period/time/per-source automatic scans | Existing `NetworkAutoRefresh`, scheduler and source rescan eligibility. | Active | Fresh request timestamp reset; settings/defaults/source scope unchanged. |
| Media DB / full-library export | Existing export workflows; full backup gets staged/archive/destination verification. | Active | Fix for empty/unreadable output; no 4.2 redesign. |
| Library import / restore | Existing confirmed restore workflow. | Active | Not executed on user data; no new restore backend. |
| Display all files / adult scrape | Existing browser/scraper filters. | Active | Preserved capability and saved values. |
| Provider attribution (`preferences_video_os`, `_tmdb`, `_trakt`) | Informational attribution. | Disabled/non-focusable information | No fake action; attribution text retained. |
| Sponsor / licences / acknowledgements / advanced mode | Existing gated links/notices/advanced handlers. | Active where applicable | Legal/upstream text retained; no new purchase/integration backend. |
| Updates / unimplemented integration controls | No working updater/integration handler. | Inert Updates entry omitted; unavailable controls stay unavailable | No functional-looking no-op added. |

## Remaining legacy/mixed surfaces and deliberate deferrals

| Surface | Why retained | Risk / later work |
|---|---|---|
| Android document picker and TV keyboard | System-owned URI permissions and input method. | Provider/IME varies by device. Never claimed pixel-perfect Nova replacement. |
| Backup/import/export operational dialogs/notifications | Existing background service and destructive restore safety. | Export pipeline fixed; full Backup & Restore UX is explicitly 4.2. |
| Network scan configuration GuidedStep screens | Working schedule/per-source preference handlers. | Embedded progress modernised, configuration not rewritten. |
| Share credentials and account/authentication flows | Sensitive existing authentication/permission semantics. | Native fields/panels retain platform layout. No telemetry or account invention. |
| Metadata identification/artwork selection | Existing scraper workflow and asynchronous results. | Slate styling can be applied by common helper, but not a complete flow redesign. |
| Subtitle downloader/credentials/specialised adjustments | Existing service and player controllers. | Common More hierarchy/anchor updated; service-owned dialogs may remain mixed. |
| Native specialist player panels and external player | Existing advanced format/audio/display behaviour. | No unsupported function substituted; hardware QA required. |
| Trailer provider controls | Web/provider-owned presentation. | Preserved working trailer rather than reimplementing its engine. |
| Classic/mobile UI | Still selected by retained routing preference on applicable devices. | Not deleted. This pass targets redesigned Android TV Preview. |
| Some Toasts/system notifications and theme colours | Background contexts/retained specialist handlers. | Whole-app feedback/accent unification is partial, not misreported complete. |

## Review and validation limits

Reviewed actual emulator Settings (including Streaming/Home categories) and bounded fixture renders for Home, Movies/TV, list, Details, HUD and menus. Corrected obsolete animation sort in Home settings, non-accent preference icon tint, and deep codec/audio pills on Main Details after checkpoint review. The final row editor render showed reordered Watch Next with the Hidden control focused; its real dialog handlers passed the add/reorder/hide/preserve-membership test. The final Movie Details render confirmed only the compact measured-resolution pill remains on the main page. Build evidence is recorded in the return handover.

No supplied current 4.1 pixel reference was available for a direct image comparison. Frost uses restrained translucency/gradient rather than live GPU blur; real title logos are unavailable in the current data model. These are explicit limitations, not generated substitutes.

**NOT TESTED — USER QA REQUIRED:** physical Shield overscan/density, long/localised text, every D-pad route, real keyboard creation/rename, nested playback adjustments and Up Next, HDR/refresh rate/passthrough, all credentials/services/providers, actual scan/import/export/deletion, preference combinations and classic/mobile alternatives. No whole-app visual or hardware acceptance is claimed.
