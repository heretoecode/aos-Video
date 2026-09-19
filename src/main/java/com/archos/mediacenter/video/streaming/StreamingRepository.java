// Copyright 2026. Licensed under the Apache License, Version 2.0.
package com.archos.mediacenter.video.streaming;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Base64;
import androidx.preference.PreferenceManager;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/** Country-specific availability. No rental/purchase offers are exposed to the UI. */
public final class StreamingRepository {
    /** Streaming launch integration is paused for this testing edition. */
    public static final boolean LINKS_AVAILABLE = true;
    public static final String ENABLED = "streaming_enabled";
    public static final String COUNTRY = "streaming_country";
    public static final String PROVIDERS = "streaming_providers_";
    public static final String PREFERRED = "streaming_preferred_";
    public static final ExecutorService IO = Executors.newFixedThreadPool(2);
    private static final OkHttpClient HTTP = new OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS).readTimeout(12, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS).build();
    private static final long TTL = TimeUnit.HOURS.toMillis(6);
    private static final Map<String, Cached> CACHE = new LinkedHashMap<String, Cached>(32, .75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, Cached> e) { return size() > 80; }
    };
    private static class Cached {
        final long at = System.currentTimeMillis(); final Availability value;
        Cached(Availability value) { this.value = value; }
    }
    public static final class Provider {
        public final int id; public final String name; public final String logo;
        public Provider(int id, String name) { this(id, name, ""); }
        public Provider(int id, String name, String logo) { this.id = id; this.name = name; this.logo = logo; }
    }
    public static final class Offer {
        public final Provider provider; public final String type;
        public String url = "";
        Offer(Provider provider, String type) { this.provider = provider; this.type = type; }
    }
    public static final class Availability {
        public final List<Offer> offers;
        public final String watchUrl;
        Availability(List<Offer> offers, String watchUrl) { this.offers = offers; this.watchUrl = watchUrl; }
    }
    public static SharedPreferences prefs(Context c) { return PreferenceManager.getDefaultSharedPreferences(c); }
    public static String country(Context c) {
        String country = prefs(c).getString(COUNTRY, "IE");
        return country != null && country.matches("[A-Z]{2}") ? country : "IE";
    }
    public static Set<String> selected(Context c) {
        return new HashSet<>(prefs(c).getStringSet(PROVIDERS + country(c), Collections.emptySet()));
    }
    public static String preferred(Context c) { return prefs(c).getString(PREFERRED + country(c), ""); }
    public static boolean allowedType(String type) {
        return "flatrate".equals(type) || "free".equals(type) || "ads".equals(type);
    }
    private static JSONObject api(Context context, String path, String country) throws Exception {
        Uri.Builder url = Uri.parse("https://api.themoviedb.org/3/" + path).buildUpon()
                .appendQueryParameter("api_key", context.getString(com.archos.medialib.R.string.tmdb_api_key))
                .appendQueryParameter("language", Locale.getDefault().toLanguageTag());
        if (country != null) url.appendQueryParameter("watch_region", country);
        return new JSONObject(get(url.build().toString(), 2 * 1024 * 1024));
    }
    private static String get(String url, int limit) throws IOException {
        if (Thread.currentThread().isInterrupted()) throw new IOException("Cancelled");
        try (Response response = HTTP.newCall(new Request.Builder().url(url)
                .header("User-Agent", "NOVA-Mark/2 (Android TV)").build()).execute()) {
            if (!response.isSuccessful() || response.body() == null) throw new IOException("Availability service unavailable");
            // Bound both API and HTML responses rather than loading an unlimited body.
            java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
            java.io.InputStream input = response.body().byteStream();
            byte[] chunk = new byte[8192];
            int count;
            while ((count = input.read(chunk)) != -1) {
                if (Thread.currentThread().isInterrupted()) throw new IOException("Cancelled");
                if (buffer.size() + count > limit) throw new IOException("Response too large");
                buffer.write(chunk, 0, count);
            }
            byte[] data = buffer.toByteArray();
            if (data.length > limit) throw new IOException("Response too large");
            return new String(data, StandardCharsets.UTF_8);
        }
    }
    public static List<Provider> providers(Context context, String country) throws Exception {
        Map<Integer, Provider> result = new HashMap<>();
        Map<Integer, Integer> ranks = new HashMap<>();
        for (String kind : new String[]{"movie", "tv"}) {
            JSONArray entries = api(context, "watch/providers/" + kind, country).getJSONArray("results");
            for (int i = 0; i < entries.length(); i++) {
                JSONObject p = entries.getJSONObject(i);
                int id = p.getInt("provider_id");
                JSONObject priorities = p.optJSONObject("display_priorities");
                int rank = priorities == null ? p.optInt("display_priority", 10000) : priorities.optInt(country, 10000);
                ranks.merge(id, rank, Math::min);
                result.put(id, new Provider(id, p.getString("provider_name"), p.optString("logo_path")));
            }
        }
        List<Provider> sorted = new ArrayList<>(result.values());
        sorted.sort(Comparator.comparingInt((Provider p) -> ranks.getOrDefault(p.id, 10000))
                .thenComparing(p -> p.name.toLowerCase(Locale.ROOT)));
        return sorted;
    }
    public static Availability load(Context context, String kind, long id, String country) throws Exception {
        return load(context, kind, id, country, -1);
    }
    public static Availability load(Context context, String kind, long id, String country, int season) throws Exception {
        if (!("movie".equals(kind) || "tv".equals(kind)) || id <= 0) throw new IOException("Missing title ID");
        String key = kind + ":" + id + ":" + country + ":" + season;
        synchronized (CACHE) {
            Cached c = CACHE.get(key);
            if (c != null && System.currentTimeMillis() - c.at < TTL) return c.value;
        }
        JSONObject response = api(context, kind + "/" + id + (season >= 0 && "tv".equals(kind) ? "/season/" + season : "") + "/watch/providers", null);
        Availability availability = parseAvailability(response, country);
        synchronized (CACHE) { CACHE.put(key, new Cached(availability)); }
        return availability;
    }
    /** Title-link enrichment happens on selection; availability is displayed immediately. */
    public static String titleLink(Context context, String kind, long id, String country, Availability availability, int providerId) {
        if (!availability.offers.isEmpty() && isTmdbWatchUrl(availability.watchUrl, kind, id)) {
            try {
                // The documented TMDb watch page contains the actual JustWatch title links.
                // This enrichment is optional: a changed page must never break local playback.
                String url = Uri.parse(availability.watchUrl).buildUpon().clearQuery()
                        .appendQueryParameter("locale", country).build().toString();
                Map<String, Integer> providerIds = new HashMap<>();
                for (Offer offer : availability.offers) providerIds.put(offer.provider.name, offer.provider.id);
                Map<Integer, String> links = parseWatchLinks(get(url, 2 * 1024 * 1024), country, providerIds);
                return links.getOrDefault(providerId, "");
            } catch (Exception ignored) { /* use the explicitly labelled watch-page fallback */ }
        }

        return "";
    }
    public static void invalidate() { synchronized (CACHE) { CACHE.clear(); } }
    static boolean isTmdbWatchUrl(String url, String kind, long id) {
        Uri u = Uri.parse(url);
        if (!"https".equals(u.getScheme()) || !"www.themoviedb.org".equals(u.getHost())) return false;
        List<String> path = u.getPathSegments();
        return path.size() == 3 && kind.equals(path.get(0))
                && (path.get(1).equals(Long.toString(id)) || path.get(1).startsWith(id + "-"))
                && "watch".equals(path.get(2));
    }
    public static Availability parseAvailability(JSONObject root, String country) throws Exception {
        JSONObject regions = root.optJSONObject("results");
        JSONObject region = regions == null ? null : regions.optJSONObject(country);
        if (region == null) return new Availability(Collections.emptyList(), "");
        Map<Integer, Offer> offers = new LinkedHashMap<>();
        for (String type : new String[]{"flatrate", "free", "ads"}) {
            JSONArray entries = region.optJSONArray(type);
            if (entries == null) continue;
            for (int i = 0; i < entries.length(); i++) {
                JSONObject p = entries.optJSONObject(i);
                if (p == null || p.optInt("provider_id") <= 0 || p.optString("provider_name").isEmpty()) continue;
                int id = p.getInt("provider_id");
                if (!offers.containsKey(id)) offers.put(id, new Offer(new Provider(id, p.getString("provider_name"),p.optString("logo_path")), type));
            }
        }
        return new Availability(new ArrayList<>(offers.values()), region.optString("link"));
    }
    public static List<Offer> filter(Availability availability, Set<String> selected, String preferred) {
        List<Offer> result = new ArrayList<>();
        for (Offer o : availability.offers) if (selected.contains(Integer.toString(o.provider.id))) result.add(o);
        result.sort(Comparator.comparingInt((Offer o) -> Integer.toString(o.provider.id).equals(preferred) ? 0 : 1)
                .thenComparing(o -> o.provider.name));
        return result;
    }
    private static final Pattern WATCH_LINK = Pattern.compile("https://click\\.justwatch\\.com/[^\\s\"'<>]+");
    /** Extract only country-matched, non-transactional links with explicit provider IDs. */
    public static Map<Integer, String> parseWatchLinks(String html, String country) {
        return parseWatchLinks(html, country, Collections.emptyMap());
    }
    static Map<Integer, String> parseWatchLinks(String html, String country, Map<String, Integer> providerIds) {
        Map<Integer, String> links = new HashMap<>();
        Matcher matcher = WATCH_LINK.matcher(html);
        while (matcher.find()) {
            try {
                Uri link = Uri.parse(matcher.group().replace("&amp;", "&"));
                if (!country.equalsIgnoreCase(link.getQueryParameter("uct_country"))) continue;
                String target = link.getQueryParameter("r");
                if (!safeWebUrl(target)) continue;
                String encoded = link.getQueryParameter("cx");
                if (encoded == null || encoded.length() > 16000) continue;
                JSONObject context = new JSONObject(new String(Base64.decode(encoded, Base64.URL_SAFE), StandardCharsets.UTF_8));
                JSONArray data = context.getJSONArray("data");
                for (int i = 0; i < data.length(); i++) {
                    JSONObject item = data.getJSONObject(i);
                    if (!item.optString("schema").contains("/clickout_context/")) continue;
                    JSONObject offer = item.getJSONObject("data");
                    if (allowedType(offer.optString("monetizationType")) && offer.optInt("providerId") > 0) {
                        // TMDb and JustWatch can assign different IDs to the same named provider.
                        String providerName = offer.optString("provider");
                        if (!providerIds.isEmpty() && !providerIds.containsKey(providerName)) continue;
                        int providerId = providerIds.getOrDefault(providerName, offer.getInt("providerId"));
                        links.putIfAbsent(providerId, target);
                    }
                }
            } catch (Exception ignored) { /* malformed individual offer: ignore it */ }
        }
        return links;
    }
    /** Resolve an affiliate redirect only when the viewer selects the offer. */
    public static String resolveTitleUrl(String url) {
        if (!safeWebUrl(url)) return "";
        String host = Uri.parse(url).getHost();
        // Only known link redirectors need expansion; normal provider links are left intact.
        if (host == null || !(host.endsWith(".bn5x.net") || host.endsWith(".pxf.io") || host.equals("click.justwatch.com"))) return url;
        OkHttpClient redirects = HTTP.newBuilder().followRedirects(false).followSslRedirects(false).build();
        String current = url;
        for (int i = 0; i < 5; i++) {
            try (Response response = redirects.newCall(new Request.Builder().url(current).head().build()).execute()) {
                String location = response.header("Location");
                if (response.code() < 300 || response.code() >= 400 || location == null) return current;
                okhttp3.HttpUrl next = response.request().url().resolve(location);
                if (next == null || !safeWebUrl(next.toString())) return url;
                current = next.toString();
            } catch (Exception e) { return url; }
        }
        return current;
    }
    public static boolean safeWebUrl(String url) {
        if (url == null || url.length() > 12000) return false;
        Uri u = Uri.parse(url);
        String host = u.getHost();
        return "https".equals(u.getScheme()) && host != null && host.contains(".")
                && u.getUserInfo() == null && !host.equals("localhost")
                && !host.matches("[0-9.]+") && !host.endsWith(".local");
    }
    private StreamingRepository() { }
}
