package com.archos.mediacenter.video.streaming;

import android.app.Application;
import android.net.Uri;
import android.util.Base64;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28)
public class StreamingRepositoryTest {
    private static String provider(int id, String name) {
        return "{\"provider_id\":" + id + ",\"provider_name\":\"" + name + "\"}";
    }
    @Test public void excludesTransactionsAndWrongCountryAndDeduplicates() throws Exception {
        JSONObject root = new JSONObject("{\"results\":{\"IE\":{\"flatrate\":[" + provider(8,"Netflix")
                + "],\"ads\":[" + provider(8,"Netflix") + "," + provider(9,"Free service")
                + "],\"rent\":[" + provider(10,"Rental") + "],\"buy\":[" + provider(11,"Shop")
                + "]},\"GB\":{\"flatrate\":[" + provider(12,"UK only") + "]}}}");
        StreamingRepository.Availability result = StreamingRepository.parseAvailability(root, "IE");
        assertEquals(2, result.offers.size());
        assertEquals(8, result.offers.get(0).provider.id);
        assertEquals(9, result.offers.get(1).provider.id);
        assertTrue(StreamingRepository.parseAvailability(root,"FR").offers.isEmpty());
    }
    @Test public void onlySelectedServicesAndPreferredAvailableProviderFirst() throws Exception {
        StreamingRepository.Availability result = StreamingRepository.parseAvailability(new JSONObject(
                "{\"results\":{\"IE\":{\"flatrate\":[" + provider(8,"Netflix") + "," + provider(9,"Amazon") + "," + provider(10,"Other") + "]}}}"),"IE");
        List<StreamingRepository.Offer> offers = StreamingRepository.filter(result,new HashSet<>(Arrays.asList("8","9")),"8");
        assertEquals(2,offers.size()); assertEquals(8,offers.get(0).provider.id);
        assertTrue(StreamingRepository.filter(result,Collections.emptySet(),"8").isEmpty());
        assertEquals(9,StreamingRepository.filter(result,new HashSet<>(Arrays.asList("8","9")),"999").get(0).provider.id);
    }
    private String link(int provider, String type, String country, String target) throws Exception {
        String context = "{\"data\":[{\"schema\":\"iglu:com.justwatch/clickout_context/jsonschema/1-3-2\",\"data\":{\"providerId\":" + provider + ",\"monetizationType\":\"" + type + "\"}}]}";
        return "<a href=\"" + Uri.parse("https://click.justwatch.com/a").buildUpon()
                .appendQueryParameter("cx",Base64.encodeToString(context.getBytes(StandardCharsets.UTF_8),Base64.URL_SAFE | Base64.NO_WRAP))
                .appendQueryParameter("r",target).appendQueryParameter("uct_country",country).build().toString().replace("&","&amp;") + "\">Watch</a>";
    }
    @Test public void extractsExactHttpsTitleLinkAndDecodesHtmlAmpersands() throws Exception {
        String url="https://www.netflix.com/title/12345?source=tv&lang=en";
        Map<Integer,String> result=StreamingRepository.parseWatchLinks(link(8,"flatrate","ie",url),"IE");
        assertEquals(url,result.get(8));
    }
    @Test public void rejectsRentalLinksEvenForSelectedSubscriptionProvider() throws Exception {
        String html=link(8,"rent","ie","https://www.netflix.com/title/123")
                + link(9,"buy","ie","https://example.com/movie")
                + link(10,"flatrate","gb","https://example.com/movie")
                + link(11,"free","ie","javascript:alert(1)")
                + link(12,"ads","ie","https://127.0.0.1/internal")
                + "<a href=\"https://click.justwatch.com/a?cx=broken&r=bad\">Broken</a>";
        assertTrue(StreamingRepository.parseWatchLinks(html,"IE").isEmpty());
    }
    @Test public void restrictsEnrichmentToExactTmdbTitleAndType() {
        assertTrue(StreamingRepository.isTmdbWatchUrl("https://www.themoviedb.org/movie/550/watch?locale=IE","movie",550));
        assertTrue(StreamingRepository.isTmdbWatchUrl("https://www.themoviedb.org/tv/123-example/watch","tv",123));
        assertFalse(StreamingRepository.isTmdbWatchUrl("https://www.themoviedb.org/movie/550/watch","tv",550));
        assertFalse(StreamingRepository.isTmdbWatchUrl("https://www.themoviedb.org/movie/5500/watch","movie",550));
        assertFalse(StreamingRepository.isTmdbWatchUrl("https://evil.example/movie/550/watch","movie",550));
    }
    @Test public void reconcilesDifferentTmdbAndJustWatchProviderIds() throws Exception {
        String html = link(2706, "flatrate", "ie", "https://www.disneyplus.com/movies/test/abc");
        // Add the provider name to the same explicit clickout context as live TMDb pages.
        Uri u = Uri.parse(html.substring(html.indexOf("https://"), html.indexOf("\">" )).replace("&amp;", "&"));
        JSONObject cx = new JSONObject(new String(Base64.decode(u.getQueryParameter("cx"), Base64.URL_SAFE), StandardCharsets.UTF_8));
        cx.getJSONArray("data").getJSONObject(0).getJSONObject("data").put("provider", "Disney Plus");
        String named = u.buildUpon().clearQuery().appendQueryParameter("cx", Base64.encodeToString(cx.toString().getBytes(StandardCharsets.UTF_8), Base64.URL_SAFE | Base64.NO_WRAP))
                .appendQueryParameter("r", "https://www.disneyplus.com/movies/test/abc").appendQueryParameter("uct_country", "ie").build().toString();
        Map<Integer,String> result = StreamingRepository.parseWatchLinks(named, "IE", Collections.singletonMap("Disney Plus",337));
        assertTrue(result.containsKey(337)); assertFalse(result.containsKey(2706));
    }
    @Test public void rejectsUnsafeTargets() {
        for (String url:Arrays.asList("file:///data/data/test", "intent://app", "http://example.com", "https://user:pass@example.com", "https://192.168.1.1"))
            assertFalse(url,StreamingRepository.safeWebUrl(url));
        assertTrue(StreamingRepository.safeWebUrl("https://www.disneyplus.com/movies/title/abc"));
    }
}
