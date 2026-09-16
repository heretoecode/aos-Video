package com.archos.mediacenter.video.utils;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import com.archos.mediascraper.*;
import com.archos.mediascraper.xml.ShowScraper4;
import org.json.*;
import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import okhttp3.*;

public final class DirectShowLookup {
    private static final OkHttpClient HTTP = new OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS).callTimeout(20, TimeUnit.SECONDS).build();

    /** null means an ordinary title query, not an identifier. */
    public static ScrapeSearchResult find(Context c, String input) throws Exception {
        String value = identifier(input);
        if (value == null) return null;
        String language = Scraper.getLanguage(c);
        int id;
        if (value.startsWith("tt")) {
            JSONObject found = request(c, "find/" + value, true);
            JSONArray shows = found.optJSONArray("tv_results");
            if (shows == null || shows.length() == 0)
                throw new IOException("No TMDb TV series is linked to that IMDb ID.");
            id = shows.getJSONObject(0).getInt("id");
        } else {
            try { id = Integer.parseInt(value); }
            catch (NumberFormatException e) { throw new IOException("That TMDb ID is too large."); }
        }
        JSONObject show = request(c, "tv/" + id, false);
        SearchResult result = new SearchResult();
        result.setTvShow(); result.setId(id); result.setTitle(show.getString("name"));
        result.setOriginalTitle(show.optString("original_name", show.getString("name")));
        result.setLanguage(language); result.setFile(Uri.parse("/manual-show.avi"));
        result.setScraper(new ShowScraper4(c));
        result.setOriginSearchSeason(1); result.setOriginSearchEpisode(1);
        if (!show.isNull("poster_path")) result.setPosterPath(show.getString("poster_path"));
        Bundle extra = new Bundle();
        extra.putString(com.archos.mediascraper.ShowUtils.SEASON, "1");
        extra.putString(com.archos.mediascraper.ShowUtils.EPNUM, "1");
        result.setExtra(extra);
        return new ScrapeSearchResult(Collections.singletonList(result), false, ScrapeStatus.OKAY, null);
    }

    public static String identifier(String input) {
        String s = input.trim();
        if (s.matches("[1-9][0-9]{0,9}") || s.matches("tt[0-9]{5,12}")) return s;
        Uri u = Uri.parse(s);
        String host = u.getHost();
        if (!("https".equals(u.getScheme()) || "http".equals(u.getScheme())) || host == null) return null;
        java.util.List<String> parts = u.getPathSegments();
        if ((host.equals("themoviedb.org") || host.equals("www.themoviedb.org")) && parts.size() >= 2 && parts.get(0).equals("tv")) {
            String id = parts.get(1).split("-")[0];
            return id.matches("[1-9][0-9]{0,9}") ? id : null;
        }
        if ((host.equals("imdb.com") || host.equals("www.imdb.com") || host.equals("m.imdb.com"))
                && parts.size() >= 2 && parts.get(0).equals("title") && parts.get(1).matches("tt[0-9]{5,12}")) return parts.get(1);
        return null;
    }

    private static JSONObject request(Context c, String path, boolean find) throws Exception {
        Uri.Builder url = Uri.parse("https://api.themoviedb.org/3/" + path).buildUpon()
            .appendQueryParameter("api_key", c.getString(com.archos.medialib.R.string.tmdb_api_key))
            .appendQueryParameter("language", Scraper.getLanguage(c));
        if (find) url.appendQueryParameter("external_source", "imdb_id");
        try (Response r = HTTP.newCall(new Request.Builder().url(url.build().toString()).build()).execute()) {
            if (r.code() == 404) throw new IOException("TV series ID not found.");
            if (!r.isSuccessful() || r.body() == null) throw new IOException("Could not connect to TMDb. Please retry.");
            if (r.body().contentLength() > 2097152) throw new IOException("Unexpected TMDb response.");
            return new JSONObject(r.body().string());
        }
    }
    private DirectShowLookup() {}
}
