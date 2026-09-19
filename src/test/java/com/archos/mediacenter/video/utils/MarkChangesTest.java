package com.archos.mediacenter.video.utils;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import java.io.*;
import java.util.*;
import java.util.zip.*;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class)
@Config(application=Application.class,sdk=28)
public class MarkChangesTest {
    @Test public void directIdsAndLinksAreUnambiguous() {
        assertEquals("66732",DirectShowLookup.identifier("66732"));
        assertEquals("66732",DirectShowLookup.identifier("https://www.themoviedb.org/tv/66732-stranger-things"));
        assertEquals("tt4574334",DirectShowLookup.identifier("https://www.imdb.com/title/tt4574334/"));
        assertNull(DirectShowLookup.identifier("https://www.themoviedb.org.evil.test/tv/66732"));
        assertNull(DirectShowLookup.identifier("https://www.themoviedb.org/movie/550"));
        assertNull(DirectShowLookup.identifier("The Last Horizon"));
    }
    @Test public void backupTraversalIsRejected() throws Exception {
        File root=RuntimeEnvironment.getApplication().getCacheDir();
        try { SafeBackup.child(root,"scraper_posters/../../outside"); fail(); } catch(IOException expected) {}
        assertEquals(new File(root,"scraper_posters/poster.jpg").getCanonicalFile(),SafeBackup.child(root,"scraper_posters/poster.jpg").getCanonicalFile());
    }
    @Test public void settingsTypesSurviveRoundTrip() throws Exception {
        Context c=RuntimeEnvironment.getApplication();
        SharedPreferences from=c.getSharedPreferences("source",0), to=c.getSharedPreferences("target",0);
        from.edit().clear().putBoolean("try_new_ui",true).putInt("count",4).putLong("time",12345678901L)
            .putFloat("speed",1.25f).putString("app_theme","slate").putStringSet("providers",new HashSet<>(Arrays.asList("8","337"))).commit();
        assertTrue(SettingsBackup.decode(to,SettingsBackup.encode(from)).commit());
        assertEquals(from.getAll(),to.getAll());
    }
    @Test public void malformedArchiveDoesNotChangeLiveDatabase() throws Exception {
        Context c=RuntimeEnvironment.getApplication(); File live=c.getDatabasePath("media.db");
        live.getParentFile().mkdirs();
        try(FileOutputStream out=new FileOutputStream(live)) { out.write(42); }
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try(ZipOutputStream zip=new ZipOutputStream(bytes)) {
            zip.putNextEntry(new ZipEntry("scraper_posters/../../outside")); zip.write(1); zip.closeEntry();
        }
        try { SafeBackup.stage(c,new ByteArrayInputStream(bytes.toByteArray())); fail(); } catch(Exception expected) {}
        try(FileInputStream in=new FileInputStream(live)) { assertEquals(42,in.read()); }
        live.delete();
    }
}
