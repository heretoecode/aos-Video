package com.archos.mediacenter.video.leanback;

import android.app.Application;
import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import android.net.Uri;
import android.os.Looper;
import android.view.View;
import androidx.preference.PreferenceManager;
import com.archos.filecorelibrary.ExtStorageManager;
import com.archos.mediacenter.video.CustomApplication;
import com.archos.mediaprovider.video.VideoOpenHelper;
import com.archos.mediaprovider.video.VideoStore;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.shadows.ShadowContentResolver;
import java.time.Duration;
import java.util.Locale;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/** Real MainFragment lifecycle, overlay and SQL schema; no device scanner or native playback. */
@RunWith(RobolectricTestRunner.class)
@Config(application=Application.class,sdk=28,qualifiers="w960dp-h540dp-land-mdpi")
public class PreviewStartupTest {
    private VideoOpenHelper database;
    private MockedStatic<ExtStorageManager> storage;
    @Before public void setUp() throws Exception {
        Context context=RuntimeEnvironment.getApplication();
        Shadows.shadowOf((Application)context).grantPermissions(context.getPackageName()+".DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION");
        org.robolectric.util.ReflectionHelpers.setStaticField(CustomApplication.class,"log",org.slf4j.LoggerFactory.getLogger(CustomApplication.class));
        org.robolectric.util.ReflectionHelpers.setStaticField(CustomApplication.class,"mContext",context);
        org.robolectric.util.ReflectionHelpers.setStaticField(CustomApplication.class,"systemLocale",Locale.UK);
        storage=mockStatic(ExtStorageManager.class);
        storage.when(ExtStorageManager::getExtStorageManager).thenReturn(mock(ExtStorageManager.class));
        try { com.squareup.picasso.Picasso.get(); } catch (IllegalStateException e) {
            com.squareup.picasso.Picasso.setSingletonInstance(new com.squareup.picasso.Picasso.Builder(context).build());
        }
        database=new VideoOpenHelper(context);
        SQLiteDatabase db=database.getWritableDatabase();
        ContentProvider provider=new ContentProvider(){
            public boolean onCreate(){return true;}
            public Cursor query(Uri uri,String[] projection,String selection,String[] args,String order){
                SQLiteQueryBuilder builder=new SQLiteQueryBuilder();builder.setTables(VideoOpenHelper.VIDEO_VIEW_NAME);
                return builder.query(db,projection,selection,args,null,null,order);
            }
            public String getType(Uri uri){return null;}
            public Uri insert(Uri uri,ContentValues values){throw new UnsupportedOperationException();}
            public int update(Uri uri,ContentValues values,String s,String[] a){throw new UnsupportedOperationException();}
            public int delete(Uri uri,String s,String[] a){throw new UnsupportedOperationException();}
        };
        ShadowContentResolver.registerProviderInternal(VideoStore.Video.Media.EXTERNAL_CONTENT_URI.getAuthority(),provider);
    }
    @After public void tearDown(){if(storage!=null)storage.close();if(database!=null)database.close();}
    @Test public void previewQueriesRunAgainstRealEmptyDatabase(){
        PreviewLibraryLoader loader=new PreviewLibraryLoader(RuntimeEnvironment.getApplication());
        try(Cursor c=loader.loadInBackground()) {assertNotNull(c);assertNotNull(loader.snapshot);assertTrue(loader.snapshot.recent.isEmpty());}
    }
    @Test public void previewQueriesRunOnWorkerWithoutLooper() throws Exception {
        PreviewLibraryLoader loader=new PreviewLibraryLoader(RuntimeEnvironment.getApplication());
        java.util.concurrent.ExecutorService worker=java.util.concurrent.Executors.newSingleThreadExecutor();
        try {
            worker.submit(() -> {
                assertNull(Looper.myLooper());
                try(Cursor c=loader.loadInBackground()) {
                    assertNotNull(c);assertNotNull(loader.snapshot);
                }
            }).get(15,java.util.concurrent.TimeUnit.SECONDS);
        } finally { worker.shutdownNow(); }
    }
    @Test public void savedPreviewPreferenceSurvivesStartupAndRecreation(){
        PreferenceManager.getDefaultSharedPreferences(RuntimeEnvironment.getApplication()).edit().putBoolean("try_new_ui",true).commit();
        org.robolectric.android.controller.ActivityController<TopNavigationTest.Host> host=Robolectric.buildActivity(TopNavigationTest.Host.class).setup();
        try {
            MainFragment fragment=new MainFragment();
            host.get().getSupportFragmentManager().beginTransaction().add(android.R.id.content,fragment).commitNow();
            draw(fragment);
            assertTrue(fragment.getView() instanceof TopNavigation);
            TopNavigation nav=(TopNavigation)fragment.getView();
            assertNotNull(nav.getStatusContainer().findViewById(com.archos.mediacenter.video.R.id.clock));
            assertNotNull(nav.getScanContainer().findViewById(com.archos.mediacenter.video.R.id.progress_group));
            host.recreate();
            MainFragment restored=(MainFragment)host.get().getSupportFragmentManager().findFragmentById(android.R.id.content);
            assertNotNull(restored);draw(restored);assertTrue(restored.getView() instanceof TopNavigation);
        } catch (Throwable failure) {
            try {host.pause().stop().destroy();} catch (Throwable cleanup) {failure.addSuppressed(cleanup);}
            throw failure;
        }
        host.pause().stop().destroy();
    }
    @Test public void clockCleanupHandlesUiSwitchBeforeResume() {
        Context context=RuntimeEnvironment.getApplication();
        android.widget.FrameLayout root=new android.widget.FrameLayout(context);
        android.widget.TextView text=new android.widget.TextView(context);
        text.setId(com.archos.mediacenter.video.R.id.clock);root.addView(text);
        com.archos.mediacenter.video.leanback.overlay.Clock clock=new com.archos.mediacenter.video.leanback.overlay.Clock(context,root);
        // An early activity recreation can skip resume, yet still invoke pause.
        clock.pause();clock.resume();clock.pause();clock.pause();clock.destroy();
    }
    @Test public void switchingFromClassicToPreviewCanPauseForRecreation() {
        android.content.SharedPreferences prefs=PreferenceManager.getDefaultSharedPreferences(RuntimeEnvironment.getApplication());
        prefs.edit().putBoolean("try_new_ui",false).commit();
        org.robolectric.android.controller.ActivityController<TopNavigationTest.Host> host=Robolectric.buildActivity(TopNavigationTest.Host.class).setup();
        try {
            MainFragment fragment=new MainFragment();
            host.get().getSupportFragmentManager().beginTransaction().add(android.R.id.content,fragment).commitNow();
            host.pause();
            prefs.edit().putBoolean("try_new_ui",true).commit();
            host.resume();
        } catch (Throwable failure) {
            try {host.pause().stop().destroy();} catch (Throwable cleanup) {failure.addSuppressed(cleanup);}
            throw failure;
        }
        host.pause().stop().destroy();
    }
    private void draw(MainFragment fragment){
        for(int i=0;i<4;i++){
            fragment.getChildFragmentManager().executePendingTransactions();
            View root=fragment.getView();
            root.measure(View.MeasureSpec.makeMeasureSpec(960,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(540,View.MeasureSpec.EXACTLY));
            root.layout(0,0,960,540);root.getViewTreeObserver().dispatchOnPreDraw();
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100));
        }
    }
}
