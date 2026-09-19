package com.archos.mediacenter.video.leanback;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import android.app.Application;
import java.util.*;
import org.json.*;
import com.archos.mediacenter.video.browser.adapters.object.*;
import com.archos.mediacenter.video.leanback.PreviewLibraryLoader.*;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(application=Application.class,sdk=28)
public class PreviewDiscoveryTest {
    private Entry movie(long id,String title,String date){Movie movie=new Movie(id,"/film"+id,title,id,"Plot",2024,7,"",null,100000,0,0,0,false,false,false,false,id,0,1920,1080,null,null,null,null,0,1,1000,0);Entry e=new Entry(movie,id,0,"Drama");e.releaseDate=date;return e;}
    @Test public void chartsMatchIdsNotNamesAndKeepFilmShowNamespacesSeparate() throws Exception {
        PreviewDiscovery charts=new PreviewDiscovery();PreviewDiscovery.parse(new JSONArray("[{\"movie\":{\"ids\":{\"tmdb\":12}}},{\"movie\":{\"ids\":{\"tmdb\":99}}}]"),"movie",charts.trending,0);
        Snapshot s=new Snapshot();s.movies.add(movie(12,"Match","2024-01-01"));s.movies.add(movie(13,"Same title","2024-01-02"));Entry show=new Entry(new Tvshow(12,"Match",null,1,1,0,"/show"),0,12,"Drama");show.onlineId=12;s.shows.add(show);
        assertEquals(1,charts.matches(s,true).size());assertEquals(12,charts.matches(s,true).get(0).onlineId);
    }
    @Test public void releaseSortUsesActualDateAndBothDirections(){Entry a=movie(1,"Zulu","2024-01-01"),b=movie(2,"Alpha","2024-12-31");assertSame(a,PreviewPages.sortEntries(Arrays.asList(a,b),2,true,new PreviewDiscovery()).get(0));assertSame(b,PreviewPages.sortEntries(Arrays.asList(a,b),2,false,new PreviewDiscovery()).get(0));}
    @Test public void unrankedTitlesStayAfterRankedInEitherOrder(){Entry a=movie(1,"A",""),b=movie(2,"B",""),c=movie(3,"C","");PreviewDiscovery charts=new PreviewDiscovery();charts.trending.put("movie:1",0);charts.trending.put("movie:2",1);assertSame(c,PreviewPages.sortEntries(Arrays.asList(c,b,a),3,true,charts).get(2));assertSame(c,PreviewPages.sortEntries(Arrays.asList(c,b,a),3,false,charts).get(2));}
    @Test public void emptyGenreDoesNotInventRecommendations(){Snapshot s=new Snapshot();s.movies.add(movie(1,"A",""));Entry blank=new Entry(s.movies.get(0).media,0,0,"");assertTrue(PreviewDiscovery.similar(blank,s).isEmpty());}
    @Test public void sourceLabelsNeverExposeEmbeddedCredentials(){String label=com.archos.mediacenter.video.leanback.details.PreviewMoviePage.source(android.net.Uri.parse("smb://user:password@server/film.mkv"));assertEquals("NAS / SMB",label);}
}
