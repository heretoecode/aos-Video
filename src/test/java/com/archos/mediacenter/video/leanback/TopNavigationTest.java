package com.archos.mediacenter.video.leanback;
import android.app.Application;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.fragment.app.FragmentActivity;
import androidx.leanback.app.BrowseSupportFragment;
import androidx.leanback.widget.*;
import com.archos.mediacenter.video.R;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class)
@Config(application=Application.class,sdk=28)
public class TopNavigationTest {
    @Before public void initialiseImageLoader() {
        try { com.squareup.picasso.Picasso.get(); }
        catch (IllegalStateException missingTestProvider) {
            com.squareup.picasso.Picasso.setSingletonInstance(new com.squareup.picasso.Picasso.Builder(RuntimeEnvironment.getApplication()).build());
        }
    }
    public static class Host extends FragmentActivity {
        @Override public void onCreate(Bundle state) {
            setTheme(R.style.MyLeanbackTheme); super.onCreate(state);
            FrameLayout frame=new FrameLayout(this);frame.setId(android.R.id.content);setContentView(frame);
        }
    }
    public static class Browse extends ExperimentalBrowseFragment {
        @Override public View onCreateView(LayoutInflater i,ViewGroup p,Bundle b) {
            return new TopNavigation(requireContext(),super.onCreateView(i,p,b),tab->{},()->true);
        }
        @Override public void onViewCreated(View v,Bundle state) {
            super.onViewCreated(v,state);setHeadersState(HEADERS_DISABLED);showTitle(false);
            setAdapter(new ArrayObjectAdapter(new ListRowPresenter()));
        }
    }
    @Test public void topShellWorksWithNativeBrowseAndCanFocusNavigation() {
        androidx.preference.PreferenceManager.getDefaultSharedPreferences(RuntimeEnvironment.getApplication()).edit().putBoolean("try_new_ui", true).commit();
        org.robolectric.android.controller.ActivityController<Host> host=Robolectric.buildActivity(Host.class).setup();
        try {
            Browse browse=new Browse();
            host.get().getSupportFragmentManager().beginTransaction().add(android.R.id.content,browse).commitNow();
            assertTrue(browse.getView() instanceof TopNavigation);
            browse.showTitle(true);
            assertNull("Legacy branding and search must not be installed", browse.getTitleView());
            android.util.TypedValue spacing = new android.util.TypedValue();
            assertTrue(host.get().getTheme().resolveAttribute(androidx.leanback.R.attr.browseRowsMarginTop, spacing, true));
            assertEquals(40f, spacing.getDimension(host.get().getResources().getDisplayMetrics()) / host.get().getResources().getDisplayMetrics().density, .1f);
            assertEquals(BrowseSupportFragment.HEADERS_DISABLED,browse.getHeadersState());
            ((TopNavigation)browse.getView()).focusNavigation();
            assertTrue(browse.getView().hasFocus());
        } finally { host.pause().stop().destroy(); }
    }
    @Test public void experimentalPlaybackLayoutKeepsNativeControlTypes() {
        org.robolectric.android.controller.ActivityController<Host> host=Robolectric.buildActivity(Host.class).setup();
        try {
            View hud=LayoutInflater.from(host.get()).inflate(R.layout.player_controller_experimental,null);
            assertTrue(hud.findViewById(R.id.seek_progress) instanceof android.widget.SeekBar);
            assertTrue(hud.findViewById(R.id.pause) instanceof ImageButton);
            assertNotNull(hud.findViewById(R.id.my_recycler_view));
            assertNotNull(hud.findViewById(R.id.time_current));
        } finally {host.pause().stop().destroy();}
    }

    @Test public void sectionsDoNotLeakIntoEachOther() {
        assertTrue(MainFragment.belongsToTab(MainFragment.ROW_ID_ALL_MOVIES, 1));
        assertFalse(MainFragment.belongsToTab(MainFragment.ROW_ID_ALL_MOVIES, 2));
        assertTrue(MainFragment.belongsToTab(MainFragment.ROW_ID_TVSHOW, 2));
        assertFalse(MainFragment.belongsToTab(MainFragment.ROW_ID_FILES, 0));
        assertTrue(MainFragment.belongsToTab(MainFragment.ROW_ID_FILES, 3));
        assertFalse(MainFragment.belongsToTab(MainFragment.ROW_ID_PREFERENCES, 0));
    }
    @Test public void classicBrowseRetainsItsTitle() {
        androidx.preference.PreferenceManager.getDefaultSharedPreferences(RuntimeEnvironment.getApplication()).edit().putBoolean("try_new_ui", false).commit();
        org.robolectric.android.controller.ActivityController<Host> host=Robolectric.buildActivity(Host.class).setup();
        try {
            ExperimentalBrowseFragment browse=new ExperimentalBrowseFragment();
            host.get().getSupportFragmentManager().beginTransaction().add(android.R.id.content,browse).commitNow();
            assertNotNull(browse.getTitleView());
        } finally {host.pause().stop().destroy();}
    }

    @Test public void previewCardsResetRecycledStateAndClampProgress() {
        org.robolectric.android.controller.ActivityController<Host> host=Robolectric.buildActivity(Host.class).setup();
        try {
            com.archos.mediacenter.video.leanback.presenter.PreviewCardPresenter p = new com.archos.mediacenter.video.leanback.presenter.PreviewCardPresenter(com.archos.mediacenter.video.leanback.presenter.PreviewCardPresenter.Style.CONTINUE);
            Presenter.ViewHolder vh = p.onCreateViewHolder(new FrameLayout(host.get()));
            p.onBindViewHolder(vh, new com.archos.mediacenter.video.browser.adapters.object.Base("First programme", null));
            assertEquals("First programme", vh.view.getContentDescription());
            p.onUnbindViewHolder(vh);
            assertNull(vh.view.getContentDescription());
            p.onBindViewHolder(vh, new com.archos.mediacenter.video.browser.adapters.object.Base("Second programme", null));
            assertEquals("Second programme", vh.view.getContentDescription());
            assertEquals(0, com.archos.mediacenter.video.leanback.presenter.PreviewCardPresenter.progress(-1, 100));
            assertEquals(0, com.archos.mediacenter.video.leanback.presenter.PreviewCardPresenter.progress(50, 0));
            assertEquals(50, com.archos.mediacenter.video.leanback.presenter.PreviewCardPresenter.progress(50, 100));
            assertEquals(100, com.archos.mediacenter.video.leanback.presenter.PreviewCardPresenter.progress(200, 100));
        } finally {host.pause().stop().destroy();}
    }

    @Test @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
    @Config(qualifiers="w960dp-h540dp-land-mdpi")
    public void renderPreviewHomeAndCheckRemoteTabSelection() throws Exception {
        androidx.preference.PreferenceManager.getDefaultSharedPreferences(RuntimeEnvironment.getApplication()).edit().putBoolean("try_new_ui", true).commit();
        org.robolectric.android.controller.ActivityController<Host> host=Robolectric.buildActivity(Host.class).setup();
        try {
            Browse browse = new Browse();
            host.get().getSupportFragmentManager().beginTransaction().add(android.R.id.content, browse).commitNow();
            ArrayObjectAdapter rows = (ArrayObjectAdapter)browse.getAdapter();
            for (int row=0; row<3; row++) {
                com.archos.mediacenter.video.leanback.presenter.PreviewCardPresenter.Style style = row == 0 ? com.archos.mediacenter.video.leanback.presenter.PreviewCardPresenter.Style.CONTINUE : row == 1 ? com.archos.mediacenter.video.leanback.presenter.PreviewCardPresenter.Style.CATEGORY : com.archos.mediacenter.video.leanback.presenter.PreviewCardPresenter.Style.POSTER;
                ArrayObjectAdapter cards = new ArrayObjectAdapter(new com.archos.mediacenter.video.leanback.presenter.PreviewCardPresenter(style));
                for (int i=0; i<6; i++) cards.add(new com.archos.mediacenter.video.leanback.adapter.object.Box(com.archos.mediacenter.video.leanback.adapter.object.Box.ID.DOCUMENTARIES, row == 1 ? (i == 0 ? "All TV shows" : i == 1 ? "Documentaries" : "Genres") : "Preview title " + (i+1), R.drawable.preview_documentaries));
                rows.add(new ListRow(new HeaderItem(new String[]{"Continue watching", "TV shows", "Recently added"}[row]), cards));
            }
            browse.setSelectedPosition(0, false); // MainFragment selects the first row after asynchronous loading.
            View root = browse.getView();
            for (int frame=0; frame<8; frame++) {
                browse.getChildFragmentManager().executePendingTransactions();
                host.get().getSupportFragmentManager().executePendingTransactions();
                root.measure(View.MeasureSpec.makeMeasureSpec(960, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(540, View.MeasureSpec.EXACTLY));
                root.layout(0, 0, 960, 540);
                root.getViewTreeObserver().dispatchOnPreDraw();
                org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(100));
            }
            assertNotNull(browse.getRowsSupportFragment());
            assertTrue("Render must include populated library rows", browse.getRowsSupportFragment().getVerticalGridView().getChildCount() > 0);
            android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(960, 540, android.graphics.Bitmap.Config.ARGB_8888);
            root.draw(new android.graphics.Canvas(bitmap));
            java.io.File out = new java.io.File("build/reports/preview-ui/home.png"); out.getParentFile().mkdirs();
            try (java.io.FileOutputStream stream = new java.io.FileOutputStream(out)) { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream); }
            LinearLayout bar = (LinearLayout)((TopNavigation)root).getChildAt(0);
            LinearLayout group = (LinearLayout)bar.getChildAt(1);
            TextView movies = (TextView)group.getChildAt(1); movies.performClick();
            assertTrue(movies.isSelected());
            assertFalse(group.getChildAt(0).isSelected());
            assertEquals("Search", group.getChildAt(5).getContentDescription());
        } finally {host.pause().stop().destroy();}
    }

}
