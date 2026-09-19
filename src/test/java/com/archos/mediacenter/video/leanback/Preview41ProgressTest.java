package com.archos.mediacenter.video.leanback;
import android.app.Application;
import com.archos.mediacenter.video.leanback.PreviewLibraryLoader.Entry;
import com.archos.mediacenter.video.browser.adapters.object.Video;
import com.archos.mediacenter.utils.videodb.VideoDbInfo;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(application=Application.class,sdk=28)
public class Preview41ProgressTest {
    Application context;PreviewPagesTest fixture=new PreviewPagesTest();
    @Before public void setup(){context=RuntimeEnvironment.getApplication();androidx.preference.PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit();}
    Entry episode(int n,int resume,boolean watched,long time){Entry e=fixture.episode(n,resume,watched,time,n);e.onlineId=7;return e;}
    @Test public void finalEpisodePositiveBookmarkWinsOverSeenThreshold(){Entry finalEpisode=episode(9,986000,true,20);assertFalse(PreviewSeriesJourney.completed((Video)finalEpisode.media));assertSame(finalEpisode,PreviewSeriesJourney.select(context,Arrays.asList(finalEpisode)).episode);}
    @Test public void briefLaterVisitPreservesCommittedEpisode(){Entry first=episode(1,5000,false,10),later=episode(4,2000,false,30);PreviewSeriesJourney.select(context,Arrays.asList(first));VideoDbInfo info=new VideoDbInfo();info.id=4;info.isShow=true;info.scraperShowId="7";info.scraperSeasonNr=1;info.scraperEpisodeNr=4;info.duration=1800000;info.lastTimePlayed=30;PreviewSeriesJourney.record(context,info,false,2000);assertSame(first,PreviewSeriesJourney.select(context,Arrays.asList(first,later)).episode);PreviewSeriesJourney.record(context,info,false,180000);assertSame(later,PreviewSeriesJourney.select(context,Arrays.asList(first,later)).episode);}
    @Test public void thresholdHasCentralBounds(){assertEquals(60000,PreviewSeriesJourney.meaningfulViewMs(100000));assertEquals(120000,PreviewSeriesJourney.meaningfulViewMs(1200000));assertEquals(180000,PreviewSeriesJourney.meaningfulViewMs(3600000));}
    @Test public void completionAdvancesToNextAvailableEpisode(){Entry first=episode(1,-2,true,10),next=episode(2,0,false,0);assertSame(next,PreviewSeriesJourney.select(context,Arrays.asList(first,next)).episode);}
}
