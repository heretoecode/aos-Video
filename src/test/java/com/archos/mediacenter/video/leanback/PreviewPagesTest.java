package com.archos.mediacenter.video.leanback;
import android.app.Application;
import android.view.*;
import android.widget.*;
import com.archos.mediacenter.video.browser.adapters.object.*;
import com.archos.mediacenter.video.leanback.PreviewLibraryLoader.*;
import com.archos.mediacenter.video.leanback.adapter.object.Box;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import java.util.*;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class)
@Config(application=Application.class,sdk=28,qualifiers="w960dp-h540dp-land-mdpi")
public class PreviewPagesTest {
    Entry episode(int number,int resume,boolean watched,long played,long added) {
        return new Entry(new Episode(number,number,1,number,"Episode "+number,0,0,"","","Example show","/test/"+number,null,null,100000,resume,0,0,watched,false,false,false,1,played,1920,1080,null,null,null,null,0,1,1000),added,7,"Drama");
    }
    @Test public void tvPremiereUses64BitDateInsteadOfTruncatedTimestamp() {
        java.util.Calendar c=java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"));c.clear();c.set(2022,0,17);
        assertEquals(2022,com.archos.mediacenter.video.browser.adapters.mappers.TvshowCursorMapper.premiereYear(c.getTimeInMillis()));
        c.clear();c.set(1959,8,12);
        assertEquals(1959,com.archos.mediacenter.video.browser.adapters.mappers.TvshowCursorMapper.premiereYear(c.getTimeInMillis()));
        assertEquals(0,com.archos.mediacenter.video.browser.adapters.mappers.TvshowCursorMapper.premiereYear(0));
    }
    @Test public void batchAdditionPicksFirstUnwatchedNotNewestFile() {
        Snapshot s=PreviewLibraryLoader.build(Arrays.asList(episode(4,0,false,0,40),episode(1,0,false,0,10),episode(2,0,false,0,20)),Collections.emptyList());
        assertEquals(1,s.recent.size()); assertEquals(1,((Episode)s.recent.get(0).media).getEpisodeNumber()); assertEquals(40,s.recent.get(0).added);
        assertTrue(s.continuingShows.isEmpty());
    }
    @Test public void resumeWinsAndCompletionAdvancesRetrospectively() {
        Entry first=episode(1,com.archos.mediacenter.video.player.PlayerActivity.LAST_POSITION_END,false,100,10);
        Entry second=episode(2,5000,false,200,20),third=episode(3,0,false,0,30);
        Snapshot s=PreviewLibraryLoader.build(Arrays.asList(third,first,second),Collections.emptyList());
        assertSame(second.media,s.recent.get(0).media);assertSame(second.media,s.continuingShows.get(0).media);assertEquals(1,s.played.size());
        s=PreviewLibraryLoader.build(Arrays.asList(first,episode(2,0,true,200,20),third),Collections.emptyList());
        assertSame(third.media,s.recent.get(0).media);
    }
    @Test public void completedShowsDoNotSuggestWatchedEpisodes() {
        Snapshot s=PreviewLibraryLoader.build(Arrays.asList(episode(1,0,true,100,10),episode(2,0,true,200,20)),Collections.emptyList());
        assertTrue(s.recent.isEmpty());assertTrue(s.continuingShows.isEmpty());
    }
    @Test @GraphicsMode(GraphicsMode.Mode.NATIVE) public void renderActualPagesAndCheckTabFocus() throws Exception {
        try{com.squareup.picasso.Picasso.get();}catch(IllegalStateException e){com.squareup.picasso.Picasso.setSingletonInstance(new com.squareup.picasso.Picasso.Builder(RuntimeEnvironment.getApplication()).build());}
        org.robolectric.android.controller.ActivityController<TopNavigationTest.Host> host=Robolectric.buildActivity(TopNavigationTest.Host.class).setup();
        try {
            PreviewPages pages=new PreviewPages(host.get(),(holder,item)->{});
            TopNavigation nav=new TopNavigation(host.get(),pages,pages::setTab,pages::atTop);host.get().setContentView(nav);pages.setArtworkListener(nav::setArtwork);pages.setDiscovery(new PreviewDiscovery());
            Snapshot s=new Snapshot();for(int i=1;i<=18;i++){
                Movie movie=new Movie(i,"/movie"+i,"Film "+i,i,"",2024,7,"",null,100000,i==1?10000:0,0,0,false,false,false,false,i,i,3840,2160,"Atmos",null,null,null,0,1,1000,0);
                Entry entry=new Entry(movie,i,0,"Drama");s.movies.add(entry);s.recent.add(entry);if(i==1){s.continuingMovies.add(entry);s.played.add(entry);}
            }
            for(int i=1;i<=12;i++)s.shows.add(new Entry(new Tvshow(i,"Show "+i,null,2,10,2,"/show"+i),i,i,"Drama"));
            s.continuingShows.add(episode(2,5000,false,200,20));
            pages.setSnapshot(s);pages.setFiles(Arrays.asList(new Box(Box.ID.FOLDERS,"Internal storage",0),new Box(Box.ID.USB,"External drive: Backup #1 (SanDisk USB drive)",0,"/test"),new Box(Box.ID.NETWORK,"Network",0),new Box(Box.ID.VIDEOS_BY_LISTS,"Playlists",0)));
            for(int tab=0;tab<4;tab++){
                ((LinearLayout)((LinearLayout)nav.getChildAt(0)).getChildAt(1)).getChildAt(tab).performClick();
                for(int frame=0;frame<4;frame++){nav.measure(View.MeasureSpec.makeMeasureSpec(960,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(540,View.MeasureSpec.EXACTLY));nav.layout(0,0,960,540);Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(50));}
                addTestArtwork(nav);
                android.graphics.Bitmap bitmap=android.graphics.Bitmap.createBitmap(960,540,android.graphics.Bitmap.Config.ARGB_8888);nav.draw(new android.graphics.Canvas(bitmap));
                java.io.File file=new java.io.File("build/reports/preview-ui/page-"+tab+".png");file.getParentFile().mkdirs();try(java.io.FileOutputStream out=new java.io.FileOutputStream(file)){bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}
            }
            nav.focusNavigation();assertTrue(nav.hasFocus());
        }finally{host.pause().stop().destroy();}
    }
    static void addTestArtwork(TopNavigation nav){
        android.graphics.Bitmap art=android.graphics.Bitmap.createBitmap(1600,900,android.graphics.Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas=new android.graphics.Canvas(art);android.graphics.Paint paint=new android.graphics.Paint();
        paint.setShader(new android.graphics.LinearGradient(0,0,1600,900,new int[]{0xff39586e,0xffaf8b66,0xff203c4e},null,android.graphics.Shader.TileMode.CLAMP));canvas.drawPaint(paint);paint.setShader(null);
        paint.setColor(0xff203d4c);android.graphics.Path mountain=new android.graphics.Path();mountain.moveTo(650,900);mountain.lineTo(1180,180);mountain.lineTo(1600,770);mountain.lineTo(1600,900);mountain.close();canvas.drawPath(mountain,paint);
        ((PreviewBackdrop)nav.getBackground()).onBitmapLoaded(art,com.squareup.picasso.Picasso.LoadedFrom.MEMORY);
    }

}
