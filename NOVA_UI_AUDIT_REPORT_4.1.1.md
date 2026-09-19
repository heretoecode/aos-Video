# SUPERNOVA / Nova Preview 4.1.1 — UI audit

19 September 2026. Scope: the corrective 4.1.1 handover over the 4.1 baseline. This report distinguishes source/render inspection from physical Shield acceptance. No approved image/video reference files were included in the authoritative 4.1.1 ZIP; its written QA findings are the comparison criteria. Exact reference fidelity cannot be certified from absent reference assets.

## Corrected surfaces

| Surface | Correction | Review / remaining validation |
|---|---|---|
| Launch/Home composition | Shared dark SUPERNOVA launch surface; first ready Home pre-draw releases it with short fade | Emulator startup checked; populated Shield timing/flash acceptance required |
| Featured | Fixed title/primary-action footprint, content fade, genuine reusable logo pipeline | Fixture layout inspected; live logos/real backdrop transitions require Shield QA |
| Home row editor | Compact bounded dialog, scrollable rows, existing D-pad reorder/hide | Fixture render and membership test; actual dialog size/remote flow needs device QA |
| Clear Watch Next | Confirmed operation in Home Settings, independent of hide | Source checked; destructive membership action not exercised against user's data |
| Library List | More legible restrained horizontal separators, preserved artwork/focus | Fixture render reviewed; populated wide-table readability requires QA |
| Details | Information/Actions names; secondary Add to Row; genuine title logo/text; two-column information | Movie/episode fixture renders reviewed; full cast/metadata scroll and image availability require QA |
| Network landing | Compact 158 dp scan action, actual status, neutral dark gradient | Fixture render reviewed; live scanning requires QA |
| Network/Search navigation | Retains transparent treatment while scrolling | Source-reviewed; device scroll composition requires QA |
| Actual file-browser list | Dark resume background, one-line path/breadcrumb, compact rows/icons/separators/focus | Source-reviewed; real sources/path widths/context actions require QA |
| Settings | Existing three panels tightened; focused setting explanation reaches correct activity help panel | Real emulator Home & Discovery capture reviewed; all categories/remote focus require QA |
| Provider picker | Immediate persistence, stable hydration, in-place checks | Preference tests passed; live provider picker/country data requires QA |
| Playback HUD | Concise selected tracks, centred equal transport targets, corrected 10s icons, episode end wording, cached logos | HUD fixture reviewed; runtime text populated by player not fully exercised |
| Playback child panels | Content measurement, restrained rows, retained anchor; update selections in place | Fixture panels/source reviewed; real speed/delay/track and Back paths require QA |
| Up Next | Rounded dark compact media card, optional artwork, clear countdown/Play/Cancel | Source-reviewed; timing algorithm preserved, actual overlay requires QA |
| Backup UI | Privacy explanation, in-progress publication and verified success message | Source and quick archive checks; native SAF provider behaviour requires QA |

## Remaining legacy or mixed surfaces / deliberate deferrals

| Surface | Current behaviour | Reason / risk |
|---|---|---|
| Optional classic file-browser grid | Existing user-selectable grid presentation remains; primary Preview file list modernised | Do not delete working view modes or rewrite file operations in corrective pass |
| Android document picker, keyboard, URI grant screens | Platform/provider UI remains | Required system ownership and storage permissions; cannot safely restyle third-party UI |
| Network/account credential entry and specialist native preferences | Existing handlers/dialogs retained, shared styling where already supported | Preserve functional login/protocol/advanced controls; complete bespoke redesign not claimed |
| Legacy Settings category | Existing obsolete-for-Preview settings stay disabled/dimmed and non-selectable; saved values/code retained | Inherited compatibility policy; no new removal/default reset |
| External provider app/browser/trailer surfaces | Native destination controls remain | Supernova does not own their interfaces or guarantee a service's deep-link behaviour |
| Playback speed/delay internals | Native picker logic within compact Preview host; some slider/value styling remains native | Preserve working playback settings/Back behaviour; not a new player implementation |
| Row naming/validation | Styled native text-entry dialog and system keyboard | Real user names; no unnecessary replacement input infrastructure |
| Missing official PNG/logo or uncached playback logo | Plain title text | Genuine metadata only; cached-only playback cannot fetch just to decorate startup |
| Default/custom accent coverage | Common surfaces honour the existing accent; some specialist/system elements retain native colours | Existing architecture retained; app-wide bespoke theme rewrite deferred |
| Backup rename unsupported by storage provider | Verified archive keeps in-progress suffix and explicit manual-rename notice | Atomic rename is provider-dependent; no false claim of final publication |

## Settings changed in this pass

| Preference | Underlying behaviour | Active/Disabled | Reason |
|---|---|---|---|
| Scan network library on startup | Existing NetworkAutoRefresh startup path | Active | Previously absent preference initialises true; explicit saved value preserved |
| Automatic network scan interval | Existing integer period and JobScheduler/foreground trigger | Active | Previously absent period initialises 15 minutes; Off/custom interval preserved |
| Clear Watch Next | Remove explicit Watch Next membership after confirmation | Active | New deliberate destructive operation; library/history untouched |
| Streaming country/providers/preferred provider | Existing region-scoped provider backend/configuration | Active when applicable | Keys set before hierarchy attach; each accepted selection persists |
| Existing Home sources, rows, Appearance, playback, accounts and advanced controls | Retained 4.1 keys/handlers | Existing state retained | No new compatibility removal or reset |
| Existing Legacy category entries | Retained underlying legacy handlers/preferences | Disabled in Preview as before | No new settings disabled in 4.1.1; previous audit remains implementation history |

## Visual deviations and limits

- No 4.1.1 reference images were supplied, so no pixel-perfect approval is asserted. The written scope was implemented with the existing Supernova geometry and visual language.
- Fixture images use test-only placeholder media; they demonstrate composition, not genuine title artwork/real-library density. The row-editor test renderer lays out a full fixture canvas and is not evidence of the actual Window's bounded size.
- Playback fixture clock/text may be explicitly set by the test, whereas runtime episode wording and track labels are populated by PlayerActivity/PlayerController; physical player verification is still required.
- Logo availability, PNG-only selection, cached-only playback, optional classic grid and platform-owned surfaces are intentional, documented limitations.
- The final empty-library release capture on the phone-based API 28 emulator exposes its Android system status bar over the top of navigation. The Settings capture is fullscreen. This is recorded as an observed emulator presentation discrepancy; fullscreen/top-inset behaviour on the Nvidia Shield must be checked rather than assumed correct from the launch test.
- No obvious source/fixture defect was knowingly retained to declare physical acceptance. User QA remains authoritative for the persistent Shield-only reports.

## NOT TESTED — USER QA REQUIRED

Populated Shield cold startup and flashes; real logo/title/background transitions; Home Rows reorder/confirmation; long Information content; actual local/WebDAV/SMB browsers and focus; transparent utility navigation during scrolling; country/provider retention and offer actions; real audio/subtitle/speed/delay/Back/Up Next overlays; native storage export/restore; all previously PASSED behaviours on the installed upgrade.

No deferred 4.2 UI was implemented. See `NOVA_CODEX_RETURN_HANDOVER_4.1.1.md` for root causes, exact build identity, tests and migration limitations.
