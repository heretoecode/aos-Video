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
            PreviewPagesTest.addTestArtwork(nav);
            android.graphics.Bitmap bitmap=android.graphics.Bitmap.createBitmap(960,540,android.graphics.Bitmap.Config.ARGB_8888);nav.draw(new android.graphics.Canvas(bitmap));java.io.File out=new java.io.File("build/reports/preview-ui/movie-details.png");out.getParentFile().mkdirs();try(java.io.FileOutputStream stream=new java.io.FileOutputStream(out)){bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,stream);}
        }finally{host.pause().stop().destroy();}
    }
}
