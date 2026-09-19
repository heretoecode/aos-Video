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
    @Test public void completedShowsLeaveContinueWatchingButRetainRecentEligibility() {
        Snapshot s=PreviewLibraryLoader.build(Arrays.asList(episode(1,0,true,100,10),episode(2,0,true,200,20)),Collections.emptyList());
        assertEquals(1,s.recent.size());assertTrue(s.continuingShows.isEmpty());
    }
    @Test public void tvStorageAggregatesLibraryFilesAndSortsUnknownLast() {
        Entry one=episode(1,0,false,0,10),two=episode(2,0,false,0,20);one.bytes=2000;two.bytes=4000;
        Entry show=new Entry(new Tvshow(7,"Example show",null,1,2,0,"/show"),20,7,"Drama");
        Snapshot snapshot=PreviewLibraryLoader.build(Arrays.asList(one,two),Arrays.asList(show));
        assertEquals(2,show.episodes);assertEquals(6000,show.bytes);assertEquals(2,show.knownSizes);
        PreviewLibraryColumns columns=new PreviewLibraryColumns(RuntimeEnvironment.getApplication(),true);
        assertEquals(android.text.format.Formatter.formatShortFileSize(RuntimeEnvironment.getApplication(),3000),columns.value(show,PreviewLibraryColumns.Column.AVERAGE));
        Entry incomplete=new Entry(new Tvshow(8,"Unknown size",null,1,2,0,"/unknown"),21,8,"");incomplete.bytes=99999;incomplete.episodes=2;incomplete.files=2;incomplete.knownSizes=1;
        columns.sortColumn=PreviewLibraryColumns.Column.AVERAGE;columns.ascending=false;
        assertSame(show,columns.sort(Arrays.asList(incomplete,show)).get(0));assertEquals("",columns.value(incomplete,PreviewLibraryColumns.Column.AVERAGE));
        PreviewLibraryLoader.build(Arrays.asList(one,two),Arrays.asList(show));assertEquals(6000,show.bytes);
    }
    @Test @GraphicsMode(GraphicsMode.Mode.NATIVE) public void renderActualPagesAndCheckTabFocus() throws Exception {
        try{com.squareup.picasso.Picasso.get();}catch(IllegalStateException e){com.squareup.picasso.Picasso.setSingletonInstance(new com.squareup.picasso.Picasso.Builder(RuntimeEnvironment.getApplication()).build());}
        org.robolectric.android.controller.ActivityController<TopNavigationTest.Host> host=Robolectric.buildActivity(TopNavigationTest.Host.class).setup();
        try {
            PreviewPages pages=new PreviewPages(host.get(),(holder,item)->{});
            TopNavigation nav=new TopNavigation(host.get(),pages,pages::setTab,pages::atTop);host.get().setContentView(nav);pages.setArtworkListener(nav::setArtwork);pages.setDiscovery(new PreviewDiscovery());
            Snapshot s=new Snapshot();for(int i=1;i<=18;i++){
                Movie movie=new Movie(i,"/movie"+i,"Film "+i,i,"A family discovers an unexpected path through a changing world, while an old promise draws them home.",2024,7,"",null,100000,i==1?10000:0,0,0,false,false,false,false,i,i,3840,2160,"Atmos",null,null,null,0,1,1000,0);
                Entry entry=new Entry(movie,i,0,"Drama");s.movies.add(entry);s.recent.add(entry);if(i==1){s.continuingMovies.add(entry);s.played.add(entry);}
            }
            for(int i=1;i<=12;i++)s.shows.add(new Entry(new Tvshow(i,"Show "+i,null,2,10,2,"/show"+i),i,i,"Drama"));
            s.continuingShows.add(episode(2,5000,false,200,20));
            pages.setSnapshot(s);pages.setFiles(Arrays.asList(new Box(Box.ID.FOLDERS,"Internal storage",0),new Box(Box.ID.USB,"External drive: Backup #1 (SanDisk USB drive)",0,"/test"),new Box(Box.ID.NETWORK,"Network",0),new Box(Box.ID.VIDEOS_BY_LISTS,"Playlists",0)));
            for(int tab=0;tab<4;tab++){
                ((LinearLayout)((LinearLayout)nav.getChildAt(0)).getChildAt(1)).getChildAt(tab).performClick();
                for(int frame=0;frame<4;frame++){nav.measure(View.MeasureSpec.makeMeasureSpec(960,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(540,View.MeasureSpec.EXACTLY));nav.layout(0,0,960,540);Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(50));}
                decorateCards(nav);if(tab!=3)addTestArtwork(nav);Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(210));
                android.graphics.Bitmap bitmap=android.graphics.Bitmap.createBitmap(960,540,android.graphics.Bitmap.Config.ARGB_8888);nav.draw(new android.graphics.Canvas(bitmap));
                java.io.File file=new java.io.File("build/reports/preview-ui/page-"+tab+".png");file.getParentFile().mkdirs();try(java.io.FileOutputStream out=new java.io.FileOutputStream(file)){bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}
            }
            pages.setTab(1);nav.selectTab(1);layout(nav);android.view.View listButton=findText(nav,"List view");if(listButton!=null)listButton.performClick();layout(nav);decorateCards(nav);capture(nav,"library-list");
            android.app.Dialog dialog=PreviewDialog.choose(host.get(),"Sort",new String[]{"Date Added","Title","Release Date","Trakt Trending — unavailable"},1,n->{});View menu=dialog.getWindow().getDecorView();int mw=dialog.getWindow().getAttributes().width,mh=dialog.getWindow().getAttributes().height;menu.measure(View.MeasureSpec.makeMeasureSpec(mw,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(mh,View.MeasureSpec.EXACTLY));menu.layout(0,0,mw,mh);capture(dialog.getWindow().getDecorView(),"sort-panel");dialog.dismiss();
            nav.focusNavigation();assertTrue(nav.hasFocus());
            android.widget.FrameLayout hudHost=new android.widget.FrameLayout(host.get());hudHost.setBackground(new android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.TL_BR,new int[]{0xff294a55,0xff6d8a85,0xff223d46}));View hud=android.view.LayoutInflater.from(host.get()).inflate(com.archos.mediacenter.video.R.layout.player_controller_experimental,hudHost,false);hudHost.addView(hud);host.get().setContentView(hudHost);hud.findViewById(com.archos.mediacenter.video.R.id.control_bar).setVisibility(View.VISIBLE);android.widget.TextView hudTitle=hud.findViewById(com.archos.mediacenter.video.R.id.preview_playback_title);hudTitle.setText("Example show");hudTitle.setVisibility(View.VISIBLE);android.widget.TextView ep=hud.findViewById(com.archos.mediacenter.video.R.id.preview_playback_episode);ep.setText("S01 E03 · A new beginning");ep.setVisibility(View.VISIBLE);((android.widget.TextView)hud.findViewById(com.archos.mediacenter.video.R.id.clock)).setText("18:06 · Ends 19:02");((android.widget.TextView)hud.findViewById(com.archos.mediacenter.video.R.id.time_current)).setText("22:14");((android.widget.TextView)hud.findViewById(com.archos.mediacenter.video.R.id.time)).setText("53:27");android.widget.ProgressBar progress=hud.findViewById(com.archos.mediacenter.video.R.id.seek_progress);progress.setMax(100);progress.setProgress(42);android.view.View pause=hud.findViewById(com.archos.mediacenter.video.R.id.pause);pause.setFocusableInTouchMode(true);pause.requestFocus();layout(hudHost);capture(hudHost,"playback-hud");
            android.app.Dialog audio=PreviewDialog.choose(host.get(),"Audio",new String[]{"English (Original)","English (DD+ 5.1)","French"},0,n->{});captureMenu(audio,"playback-audio");
            android.app.Dialog more=PreviewDialog.choose(host.get(),"More",new String[]{"Audio","Subtitles","Playback Speed","More Info"},0,n->{});captureMenu(more,"playback-more");
            Class<?> info=Class.forName("com.archos.mediacenter.video.player.PreviewPlaybackInfo");java.lang.reflect.Method showInfo=info.getDeclaredMethod("show",android.app.Activity.class,String.class,String.class,Object.class,Runnable.class,Runnable.class);showInfo.setAccessible(true);showInfo.invoke(null,host.get(),"Example show","S01 E03 · A new beginning",null,(Runnable)()->{},(Runnable)()->{});android.app.Dialog infoDialog=org.robolectric.shadows.ShadowDialog.getLatestDialog();View infoView=infoDialog.getWindow().getDecorView();infoView.measure(View.MeasureSpec.makeMeasureSpec(860,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(540,View.MeasureSpec.AT_MOST));infoView.layout(0,0,860,infoView.getMeasuredHeight());capture(infoView,"playback-info");infoDialog.dismiss();
        }finally{host.pause().stop().destroy();}
    }
    static void captureMenu(android.app.Dialog d,String name)throws Exception{View v=d.getWindow().getDecorView();int w=d.getWindow().getAttributes().width,h=d.getWindow().getAttributes().height;v.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));v.layout(0,0,w,h);capture(v,name);d.dismiss();}
    static void layout(View v){for(int i=0;i<3;i++){v.measure(View.MeasureSpec.makeMeasureSpec(960,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(540,View.MeasureSpec.EXACTLY));v.layout(0,0,960,540);Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(60));}}
    static View findText(View v,String text){if(v instanceof TextView&&((TextView)v).getText().toString().contains(text))return v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){View match=findText(((ViewGroup)v).getChildAt(i),text);if(match!=null)return match;}return null;}
    static void capture(View v,String name)throws Exception{android.graphics.Bitmap b=android.graphics.Bitmap.createBitmap(v.getWidth(),v.getHeight(),android.graphics.Bitmap.Config.ARGB_8888);v.draw(new android.graphics.Canvas(b));java.io.File f=new java.io.File("build/reports/preview-ui/"+name+".png");f.getParentFile().mkdirs();try(java.io.FileOutputStream o=new java.io.FileOutputStream(f)){b.compress(android.graphics.Bitmap.CompressFormat.PNG,100,o);}}
    static void decorateCards(View v){if(v instanceof com.archos.mediacenter.video.leanback.presenter.PreviewCardPresenter.Card){android.graphics.Bitmap b=android.graphics.Bitmap.createBitmap(200,300,android.graphics.Bitmap.Config.ARGB_8888);android.graphics.Canvas c=new android.graphics.Canvas(b);c.drawColor(0xff233b52);android.graphics.Paint p=new android.graphics.Paint();p.setColor(0xff62cfea);p.setStyle(android.graphics.Paint.Style.STROKE);p.setStrokeWidth(5);c.drawRect(3,3,197,297,p);p.setStyle(android.graphics.Paint.Style.FILL);p.setTextSize(20);c.drawText("POSTER TOP",20,25,p);c.drawText("FULL FRAME",20,280,p);((com.archos.mediacenter.video.leanback.presenter.PreviewCardPresenter.Card)v).image.setImageBitmap(b);}else if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)decorateCards(((ViewGroup)v).getChildAt(i));}
    static void addTestArtwork(TopNavigation nav){
        android.graphics.Bitmap art=android.graphics.Bitmap.createBitmap(1600,900,android.graphics.Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas=new android.graphics.Canvas(art);android.graphics.Paint paint=new android.graphics.Paint();
        paint.setShader(new android.graphics.LinearGradient(0,0,1600,900,new int[]{0xff39586e,0xffaf8b66,0xff203c4e},null,android.graphics.Shader.TileMode.CLAMP));canvas.drawPaint(paint);paint.setShader(null);
        paint.setColor(0xff203d4c);android.graphics.Path mountain=new android.graphics.Path();mountain.moveTo(650,900);mountain.lineTo(1180,180);mountain.lineTo(1600,770);mountain.lineTo(1600,900);mountain.close();canvas.drawPath(mountain,paint);
        ((PreviewBackdrop)nav.getBackground()).onBitmapLoaded(art,com.squareup.picasso.Picasso.LoadedFrom.MEMORY);
    }

}
