package com.archos.mediacenter.video.leanback;
import android.app.Application;
import android.view.*;
import androidx.leanback.widget.*;
import com.archos.mediacenter.video.browser.adapters.object.Movie;
import com.archos.mediacenter.video.leanback.details.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(application=Application.class,sdk=28,qualifiers="w960dp-h540dp-land-mdpi")
public class PreviewMoviePageTest {
    @Test @GraphicsMode(GraphicsMode.Mode.NATIVE) public void episodeMetadataDoesNotBorrowSeriesRating() throws Exception {
        try{com.squareup.picasso.Picasso.get();}catch(IllegalStateException e){com.squareup.picasso.Picasso.setSingletonInstance(new com.squareup.picasso.Picasso.Builder(RuntimeEnvironment.getApplication()).build());}
        org.robolectric.android.controller.ActivityController<TopNavigationTest.Host> host=Robolectric.buildActivity(TopNavigationTest.Host.class).setup();
        try {
            ArrayObjectAdapter actions=new ArrayObjectAdapter();PreviewMoviePage page=new PreviewMoviePage(host.get(),()->actions,a->{},()->{},uri->{});host.get().setContentView(page);
            com.archos.mediacenter.video.browser.adapters.object.Episode episode=new com.archos.mediacenter.video.browser.adapters.object.Episode(1,1,1,1,"Pilot",1642377600000L,0,"","An episode synopsis","Example show","/episode",null,null,0,0,0,0,false,false,false,false,1,0,1920,1080,null,null,null,null,0,1,2000);
            com.archos.mediascraper.ShowTags series=new com.archos.mediascraper.ShowTags();series.setRating(9.8f);series.addGenreIfAbsent("Drama");
            com.archos.mediascraper.EpisodeTags tags=new com.archos.mediascraper.EpisodeTags(series,1,1);tags.setRuntime(45,java.util.concurrent.TimeUnit.MINUTES);page.bind(episode);page.setTags(tags,java.util.Collections.emptyList(),java.util.Collections.emptyList());
            assertNotNull(PreviewPagesTest.findText(page,"S1 E1"));assertNotNull(PreviewPagesTest.findText(page,"45 min"));assertNotNull(PreviewPagesTest.findText(page,"Drama"));assertNull(PreviewPagesTest.findText(page,"9.8"));
            tags.setRating(7.4f);page.setTags(tags,java.util.Collections.emptyList(),java.util.Collections.emptyList());assertNotNull(PreviewPagesTest.findText(page,"7.4"));assertNull(PreviewPagesTest.findText(page,"9.8"));PreviewPagesTest.layout(page);PreviewPagesTest.capture(page,"episode-details");
        }finally{host.pause().stop().destroy();}
    }
    @Test @GraphicsMode(GraphicsMode.Mode.NATIVE) public void moviePageDelegatesPlaybackAndRenders() throws Exception {
        try{com.squareup.picasso.Picasso.get();}catch(IllegalStateException e){com.squareup.picasso.Picasso.setSingletonInstance(new com.squareup.picasso.Picasso.Builder(RuntimeEnvironment.getApplication()).build());}
        org.robolectric.android.controller.ActivityController<TopNavigationTest.Host> host=Robolectric.buildActivity(TopNavigationTest.Host.class).setup();
        try{
            ArrayObjectAdapter actions=new ArrayObjectAdapter();actions.add(new Action(VideoActionAdapter.ACTION_RESUME,"Resume"));final long[] selected={-1};
            PreviewMoviePage page=new PreviewMoviePage(host.get(),()->actions,a->selected[0]=a.getId(),()->{},uri->{});
            TopNavigation nav=new TopNavigation(host.get(),page,i->{},page::atTop);nav.selectTab(1);host.get().setContentView(nav);
            Movie m=new Movie(1,"smb://server/movies/film.mkv","The Last Horizon",1,"A journey through the mountains brings a family together.",2024,7.5f,"12",null,7200000,1000,0,0,false,false,false,false,1,0,3840,2160,"Atmos","HEVC",null,null,0,1,1000,0);
            page.bind(m);page.play();assertEquals(VideoActionAdapter.ACTION_RESUME,selected[0]);
            for(int frame=0;frame<4;frame++){nav.measure(View.MeasureSpec.makeMeasureSpec(960,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(540,View.MeasureSpec.EXACTLY));nav.layout(0,0,960,540);Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(50));}
            PreviewPagesTest.addTestArtwork(nav);Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(210));
            android.graphics.Bitmap bitmap=android.graphics.Bitmap.createBitmap(960,540,android.graphics.Bitmap.Config.ARGB_8888);nav.draw(new android.graphics.Canvas(bitmap));java.io.File out=new java.io.File("build/reports/preview-ui/movie-details.png");out.getParentFile().mkdirs();try(java.io.FileOutputStream stream=new java.io.FileOutputStream(out)){bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,stream);}
        }finally{host.pause().stop().destroy();}
    }
}
