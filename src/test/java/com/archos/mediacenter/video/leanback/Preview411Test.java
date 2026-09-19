package com.archos.mediacenter.video.leanback;
import android.app.Application;
import androidx.preference.PreferenceManager;
import com.archos.mediacenter.video.leanback.search.PreviewSearchText;
import com.archos.mediacenter.video.player.PreviewTrackLabel;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(application=Application.class,sdk=28)
public class Preview411Test {
 @Test public void localSearchMatchesDiacriticsAndEpisodeNames(){assertTrue(PreviewSearchText.matches("shogun","Shōgun"));assertTrue(PreviewSearchText.matches("SHŌGUN","shogun"));assertTrue(PreviewSearchText.matches("pilot","Series","Pilot"));assertFalse(PreviewSearchText.matches("mobland","Shōgun"));}
 @Test public void logoSelectionUsesGenuinePngAndLanguage()throws Exception{JSONArray logos=new JSONArray("[{\"file_path\":\"/english.png\",\"iso_639_1\":\"en\"},{\"file_path\":\"/local.png\",\"iso_639_1\":\"fr\"},{\"file_path\":\"/not-supported.svg\",\"iso_639_1\":\"fr\",\"vote_average\":10}]");assertEquals("/local.png",OfficialTitleArtwork.select(logos,"fr"));assertEquals("/english.png",OfficialTitleArtwork.select(logos,"de"));}
 @Test public void audioSummaryDoesNotExposeTrackFilename(){assertEquals("English · DD+ 5.1",PreviewTrackLabel.concise("en","E-AC-3","5.1 channels","Audio"));}
 @Test public void explicitScanOffIsPreserved(){Application c=RuntimeEnvironment.getApplication();android.content.SharedPreferences p=PreferenceManager.getDefaultSharedPreferences(c);p.edit().clear().putBoolean("try_new_ui",true).putBoolean("auto_rescan_on_app_restart",false).putInt("auto_rescan_period",0).commit();PreviewAutoScanPolicy.initialise(c);assertFalse(p.getBoolean("auto_rescan_on_app_restart",true));assertEquals(0,p.getInt("auto_rescan_period",-1));}
}
