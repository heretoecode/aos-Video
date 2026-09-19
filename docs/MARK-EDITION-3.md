# Preview 3.5 — cinematic browsing and movie details

Version 6.4.63-mark.3.5-preview, code 6040070. Same isolated package and pinned signing identity as Preview 3.4.

Home, Movies and TV Shows share a backdrop with transparent top navigation and a slate gradient. Home adds a featured title, native Play/More Info, Continue Watching, Recently Added, public Trakt Trending/Popular matched to library IDs, Recently Watched and genre-based library recommendations. Movies and TV Shows have conditional Continue Watching, dynamic genre/year filters, title/date/date-added and Trakt sorting in both directions, and grid/list views. Trakt's first 200 entries per chart/type are cached for six hours; unranked library titles remain available after ranked ones. No library or watched history is uploaded by discovery.

Movie details provides cinematic title/plot/metadata, native playback and More actions, cast names/roles, source/technical details, similar library movies and in-app YouTube trailer embeds with an explicit external fallback. Cast portraits and additional rating providers are not available from the existing database and are not fabricated. Complete native file/subtitle/artwork controls remain accessible. Network & Files, Settings and the player HUD are deferred.

Validation is recorded with the delivered build; this source note is not a claim that the APK has passed. Preserve startup, classic fallback, signing and upgrade checks. Mark has confirmed 3.4 startup/basic navigation on Shield; the new 3.5 layout and playback still require Shield acceptance.
