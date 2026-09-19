# NOVA — Mark’s Edition 2

Version: **6.4.63-mark.2**, version code **6040064**. Based on NOVA 6.4.63.

## Changes

- **Documentaries in the left navigation.** A dedicated native browse row opens the documentary TV-show grid. The entry stays visible before scanning and can be hidden with the existing Documentaries preference. The previous duplicate TV-row tile is removed. It uses scraped documentary genre metadata; documentary films are not included in this TV-show category.
- **Slate blue theme.** Dark blue backgrounds, slate panels and lighter focus highlights, retaining NOVA’s native sidebar, poster/detail card, action strip and right-side settings overlay. Applied once on upgrading this personal build; Blue and Black remain selectable.
- **Streaming services in Settings.** Country, multiple providers, preferred provider, enable/disable and provider-list refresh. Provider selections and preferred service are retained separately for each country. Provider names are loaded from TMDb’s regional movie and TV lists.
- **Compact streaming actions.** Film, show and episode detail pages show local playback plus one available preferred provider and “More providers” for alternatives among the selected services. Subscription, free and ad-supported offers are included; rental and purchase offers are excluded.
- **Provider hand-off.** Availability uses exact TMDb IDs. Title links from TMDb’s public watch page are used where available, targeting installed provider apps before Android’s normal link handler. “Play on…” is used for a supplied title link; “Open…” and a fallback explanation are used otherwise. NOVA does not play DRM-protected streams or grant subscriptions. Providers may open a title page or request profile selection instead of immediately playing.
- **Resilience.** Off-main-thread lookup, response limits/timeouts, six-hour availability cache, duplicate removal, lifecycle cancellation, per-country filtering and retry states. Missing apps and unavailable links display a fallback instead of crashing. Private mode suppresses lookups. Local playback remains independent.
- **Build identity.** About shows Mark’s personal build, edition, date and version, with these release notes available in the app.

## Getting started

1. Open **Preferences → Streaming services**.
2. Choose **Country** (Ireland is the initial default), then **My providers** and optionally **Preferred provider**.
3. Open a scraped film or TV title. Use local playback, the available provider action, or **More providers**.
4. Select **Documentaries** in the left navigation for documentary TV shows.

## Limits and compatibility

- Streaming availability is supplied by **JustWatch via TMDb**. TV results describe the show, not guaranteed availability of every season/episode. Listings can be out of date.
- Title-link enrichment depends on the public TMDb watch-page format. It is optional and falls back gracefully if that page changes or is unavailable. It does not use a private JustWatch API or an invented Netflix ID.
- Some Shield apps do not accept title links. Opening a provider may require its app/browser and a valid subscription. No provider credentials are stored by this integration.
- Country/provider choices are settings; availability is an in-memory cache and is not part of library backups.
- Custom collection storage remains groundwork from Edition 1; this update does not introduce collection-management UI.
- This is a universal non-Amazon **debug-signed personal build**, with the existing `org.courville.nova` application ID. The previous workflow did not retain its signing key. The first Edition 2 build therefore needs a backup/reinstall when Android rejects the old signature. The workflow now caches the development key for subsequent builds; that cache is not permanent release-key storage.
- Automated verification cannot establish behaviour on a physical Shield or inside proprietary streaming apps. Device playback and provider hand-off still need device verification.

## Data references

- [TMDb watch providers and required JustWatch attribution](https://developer.themoviedb.org/reference/movie-watch-providers)
- [TMDb movie provider catalogue](https://developer.themoviedb.org/reference/watch-providers-movie-list)
- [TMDb TV provider catalogue](https://developer.themoviedb.org/reference/watch-providers-tv-list)
