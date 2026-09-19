package com.archos.mediacenter.video.utils;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.preference.PreferenceManager;
import com.archos.mediaprovider.video.VideoOpenHelper;
import com.archos.mediascraper.MediaScraper;
import com.archos.mediaprovider.DbHolder;
import com.archos.mediaprovider.VideoDb;
import com.archos.mediacenter.utils.ShortcutDbAdapter;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;

/** Stage and validate the whole archive before replacing any live library files. */
public final class SafeBackup {
    static final long MAX_BYTES = 4L * 1024 * 1024 * 1024;
    static final int MAX_ENTRIES = 100000;
    public static File child(File root, String name) throws IOException {
        File file = new File(root, name);
        if (name.contains("\\") || !file.getCanonicalPath().startsWith(root.getCanonicalPath() + File.separator))
            throw new IOException("Unsafe backup path");
        return file;
    }
    static boolean allowed(String name) {
        return name.equals("media.db") || name.equals("credentials_db") || name.equals("shortcuts_db")
            || name.equals("shortcuts2_db") || name.equals("db_version.txt") || name.equals("settings.json") || name.equals("named_preferences.json")
            || name.startsWith("scraper_posters/") || name.startsWith("scraper_backdrops/") || name.startsWith("scraper_pictures/");
    }
    public static File stage(Context c, InputStream input) throws Exception {
        File stage = new File(c.getCacheDir(), "restore-" + UUID.randomUUID());
        if (!stage.mkdirs()) throw new IOException("Cannot create restore staging folder");
        try {
            long total = 0; int count = 0;
            Set<String> names = new HashSet<>();
            try (ZipInputStream zip = new ZipInputStream(input)) {
                ZipEntry entry; byte[] buffer = new byte[8192];
                while ((entry = zip.getNextEntry()) != null) {
                    String name = entry.getName(); File file = child(stage, name);
                    if (++count > MAX_ENTRIES || !names.add(file.getCanonicalPath()) || !allowed(name))
                        throw new IOException("Unsupported or duplicate backup entry");
                    if (entry.isDirectory()) { file.mkdirs(); continue; }
                    if (!file.getParentFile().isDirectory() && !file.getParentFile().mkdirs()) throw new IOException("Cannot create restore folder");
                    try (FileOutputStream out = new FileOutputStream(file)) {
                        int n; long size = 0;
                        while ((n = zip.read(buffer)) != -1) {
                            total += n; size += n;
                            if (total > MAX_BYTES || ((name.equals("settings.json")||name.equals("named_preferences.json")) && size > 4194304)
                                    || (name.equals("db_version.txt") && size > 32)) throw new IOException("Backup exceeds size limit");
                            out.write(buffer,0,n);
                        }
                    }
                }
            }
            int version = Integer.parseInt(read(new File(stage,"db_version.txt")).trim());
            if (version != VideoOpenHelper.getDatabaseVersion()) throw new IOException("Backup database version is incompatible");
            File media = new File(stage,"media.db");
            if (!media.isFile()) throw new IOException("Backup has no media database");
            try (SQLiteDatabase db = SQLiteDatabase.openDatabase(media.getPath(), null, SQLiteDatabase.OPEN_READONLY);
                 Cursor check = db.rawQuery("PRAGMA quick_check", null)) {
                if (db.getVersion() != version || !check.moveToFirst() || !"ok".equals(check.getString(0))) throw new IOException("Backup database is damaged");
            }
            File settings = new File(stage,"settings.json");
            if(settings.isFile()) SettingsBackup.decode(PreferenceManager.getDefaultSharedPreferences(c), read(settings));
            File named=new File(stage,"named_preferences.json");if(named.isFile())MigrationBackup.validateSettings(c,read(named));
            return stage;
        } catch (Exception error) { remove(stage); throw error; }
    }
    static String read(File file) throws IOException {
        try (InputStream in = new FileInputStream(file); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] b = new byte[8192]; int n;
            while((n=in.read(b))!=-1) { if(out.size()+n>4194304) throw new IOException("Backup metadata too large"); out.write(b,0,n); }
            return out.toString(StandardCharsets.UTF_8.name());
        }
    }
    static void copy(File from, File to) throws IOException {
        if (from.isDirectory()) {
            if (!to.mkdirs() && !to.isDirectory()) throw new IOException("Cannot prepare restore");
            File[] files = from.listFiles(); if(files==null) throw new IOException("Cannot list restore files");
            for (File file : files) copy(file,new File(to,file.getName()));
        } else {
            try (InputStream in = new FileInputStream(from); OutputStream out = new FileOutputStream(to)) {
                byte[] b = new byte[8192]; int n; while((n=in.read(b))!=-1) out.write(b,0,n);
            }
        }
    }
    private static final class Swap {
        final File live, fresh, old;
        boolean movedOld, installed;
        Swap(File live, File staged, String token) throws IOException {
            this.live=live;
            File parent=live.getParentFile(); if(!parent.isDirectory()&&!parent.mkdirs()) throw new IOException("Restore folder unavailable");
            fresh=new File(parent, "."+live.getName()+".new-"+token);
            old=new File(parent, "."+live.getName()+".old-"+token);
            if(staged!=null&&staged.exists()) copy(staged,fresh);
        }
        void apply() throws IOException {
            if(live.exists()) { if(!live.renameTo(old)) throw new IOException("Cannot retain recovery copy"); movedOld=true; }
            if(fresh.exists()) { if(!fresh.renameTo(live)) throw new IOException("Cannot install restored data"); installed=true; }
        }
        void rollback() throws IOException {
            if(installed) remove(live);
            if(movedOld&&!old.renameTo(live)) throw new IOException("Recovery copy retained at "+old.getPath());
        }
    }
    public static void restore(Context c, File stage) throws Exception {
        String token=UUID.randomUUID().toString();
        List<Swap> swaps=new ArrayList<>();
        try {
            for(String name:new String[]{"media.db","credentials_db","shortcuts_db","shortcuts2_db"}) {
                File dest=c.getDatabasePath(name);
                swaps.add(new Swap(dest,new File(stage,name),token));
                swaps.add(new Swap(new File(dest+"-wal"),null,token));
                swaps.add(new Swap(new File(dest+"-shm"),null,token));
            }
            swaps.add(new Swap(MediaScraper.getPosterDirectory(c),new File(stage,"scraper_posters"),token));
            swaps.add(new Swap(MediaScraper.getBackdropDirectory(c),new File(stage,"scraper_backdrops"),token));
            swaps.add(new Swap(MediaScraper.getPictureDirectory(c),new File(stage,"scraper_pictures"),token));
            File named=new File(stage,"named_preferences.json");Map<String,String> restoredNamed=named.isFile()?MigrationBackup.validateSettings(c,read(named)):Collections.emptyMap();Map<String,String> originalNamed=new LinkedHashMap<>();for(String name:restoredNamed.keySet())originalNamed.put(name,SettingsBackup.encode(c.getSharedPreferences(name,0)));
            String originalSettings=SettingsBackup.encode(PreferenceManager.getDefaultSharedPreferences(c));
            DbHolder holder=VideoDb.getHolder(c);
            holder.lockExclusive();
            try {
                holder.close(); ShortcutDbAdapter.VIDEO.close();
                try {
                    for(Swap swap:swaps) swap.apply();
                    for(Map.Entry<String,String> entry:restoredNamed.entrySet())if(!SettingsBackup.decode(c.getSharedPreferences(entry.getKey(),0),entry.getValue()).commit())throw new IOException("Cannot restore account configuration");
                    File settings=new File(stage,"settings.json");
                    if(settings.isFile()&&!SettingsBackup.decode(PreferenceManager.getDefaultSharedPreferences(c),read(settings)).commit())
                        throw new IOException("Cannot save restored settings");
                } catch(Exception error) {
                    SettingsBackup.decode(PreferenceManager.getDefaultSharedPreferences(c),originalSettings).clear().commit();
                    for(Map.Entry<String,String> entry:originalNamed.entrySet())SettingsBackup.decode(c.getSharedPreferences(entry.getKey(),0),entry.getValue()).clear().commit();
                    for(int i=swaps.size()-1;i>=0;i--) {
                        try { swaps.get(i).rollback(); } catch(IOException recovery) { error.addSuppressed(recovery); }
                    }
                    throw error;
                }
            } finally { holder.unlockExclusive(); }
            for(Swap swap:swaps) remove(swap.old);
        } finally {
            for(Swap swap:swaps) remove(swap.fresh);
            remove(stage);
        }
    }
    static void remove(File file) {
        if(file.isDirectory()) { File[] files=file.listFiles(); if(files!=null) for(File child:files) remove(child); }
        file.delete();
    }
    private SafeBackup() {}
}
