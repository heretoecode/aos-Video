// Copyright 2025 Courville Software
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.archos.mediacenter.video.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;

import com.archos.mediacenter.video.R;
import com.archos.mediacenter.utils.ShortcutDbAdapter;
import com.archos.mediaprovider.DbHolder;
import com.archos.mediaprovider.VideoDb;
import com.archos.mediaprovider.video.VideoOpenHelper;
import com.archos.mediascraper.MediaScraper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class MediaLibraryBackupService extends Service {
    private static final Logger log = LoggerFactory.getLogger(MediaLibraryBackupService.class);

    public static final String ACTION_EXPORT = "export_library";
    public static final String ACTION_IMPORT = "import_library";
    public static final String EXTRA_IMPORT_FILE = "import_file";
    public static final String EXTRA_EXPORT_URI = "export_uri";
    private String exportUri;

    private static final int NOTIFICATION_ID = 7;
    private static final String NOTIF_CHANNEL_ID = "MediaLibraryBackup_id";
    private static final String NOTIF_CHANNEL_NAME = "Media Library Backup";
    private static final String NOTIF_CHANNEL_DESCR = "Media Library Backup Service";

    private static final String DATABASE_NAME = "media.db";
    private static final String CREDENTIALS_DB_NAME = "credentials_db";
    private static final String SHORTCUTS_DB_NAME = "shortcuts_db";
    private static final String SHORTCUTS2_DB_NAME = "shortcuts2_db";
    private static final String VERSION_FILE = "db_version.txt";
    private static final String BACKUP_FILENAME = "backup.zip";
    private static final int BUFFER_SIZE = 8192;

    private NotificationManager nm;
    private NotificationCompat.Builder nb;
    private Thread mThread;

    @Override
    public void onCreate() {
        super.onCreate();
        if (log.isDebugEnabled()) log.debug("onCreate");

        nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel nc = new NotificationChannel(NOTIF_CHANNEL_ID, NOTIF_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW);
            nc.setDescription(NOTIF_CHANNEL_DESCR);
            nc.setSound(null, null);
            nc.enableLights(false);
            nc.enableVibration(false);
            nm.createNotificationChannel(nc);
        }

        nb = new NotificationCompat.Builder(this, NOTIF_CHANNEL_ID)
                .setSmallIcon(R.drawable.nova_notification)
                .setContentTitle(getString(R.string.media_library_export_in_progress))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (log.isDebugEnabled()) log.debug("onStartCommand: intent={}", intent);

        androidx.core.app.ServiceCompat.startForeground(this, NOTIFICATION_ID, nb.build(),
                android.os.Build.VERSION.SDK_INT >= 29 ? android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC : 0);
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_EXPORT.equals(action)) {
                startExport(intent.getStringExtra(EXTRA_EXPORT_URI));
            } else if (ACTION_IMPORT.equals(action)) {
                String importFile = intent.getStringExtra(EXTRA_IMPORT_FILE);
                startImport(importFile);
            }
        }

        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startExport(final String destination) {
        if (mThread != null && mThread.isAlive()) {
            log.warn("startExport: export already in progress");
            return;
        }

        mThread = new Thread(() -> {
            android.net.Uri[] published={destination==null?null:android.net.Uri.parse(destination)};
            try {
                nb.setContentTitle(getString(R.string.media_library_export_in_progress));
                nm.notify(NOTIFICATION_ID, nb.build());

                String exportPath = exportMediaLibrary();
                if (destination != null) {
                    try (java.io.InputStream in = new FileInputStream(exportPath);
                         java.io.OutputStream out = getContentResolver().openOutputStream(published[0], "w")) {
                        if (out == null) throw new IOException("Destination unavailable");
                        byte[] data = new byte[8192]; int n; long written=0;while ((n=in.read(data))!=-1){out.write(data,0,n);written+=n;}out.flush();if(written==0||written!=new File(exportPath).length())throw new IOException("Backup destination copy was incomplete");
                    }
                    java.security.MessageDigest expected=java.security.MessageDigest.getInstance("SHA-256"),actual=java.security.MessageDigest.getInstance("SHA-256");
                    byte[] verification=new byte[8192];try(java.io.InputStream local=new FileInputStream(exportPath)){int n;while((n=local.read(verification))!=-1)expected.update(verification,0,n);}
                    try(java.io.InputStream saved=getContentResolver().openInputStream(published[0])){if(saved==null)throw new IOException("Cannot verify the selected backup location");int n;while((n=saved.read(verification))!=-1)actual.update(verification,0,n);}
                    if(!java.util.Arrays.equals(expected.digest(),actual.digest()))throw new IOException("The selected backup location did not retain a complete copy");
                    String name="nova-backup-"+System.currentTimeMillis()+".zip";
                    try(android.database.Cursor document=getContentResolver().query(published[0],new String[]{android.provider.OpenableColumns.DISPLAY_NAME},null,null,null)){if(document!=null&&document.moveToFirst()){String chosen=document.getString(0);if(chosen!=null)name=chosen.replaceFirst("\\.in-progress(?:\\.zip)?$","");}}
                    if(!name.endsWith(".zip"))name+=".zip";
                    boolean renamed=false;
                    try{android.net.Uri complete=android.provider.DocumentsContract.renameDocument(getContentResolver(),published[0],name);if(complete!=null){published[0]=complete;renamed=true;}}catch(Exception unsupported){log.info("Backup verified; destination does not support rename");}
                    exportPath = renamed?name:"selected location (verified archive; this provider cannot rename the .in-progress file — rename it to .zip)";
                }

                showToast(getString(R.string.media_library_export_success, exportPath));
                nm.cancel(NOTIFICATION_ID);
            } catch (Exception e) {
                log.error("startExport: error exporting media library", e);
                // The document picker creates the destination before generation. Remove that
                // newly-created incomplete document instead of leaving a misleading 0 KB ZIP.
                if(destination!=null)try{android.provider.DocumentsContract.deleteDocument(getContentResolver(),published[0]);}catch(Exception cleanup){log.warn("Could not remove incomplete backup document",cleanup);}
                showToast(getString(R.string.media_library_export_error)+" · "+(e.getMessage()==null?e.getClass().getSimpleName():e.getMessage()));
            } finally {
                ServiceCompat.stopForeground(MediaLibraryBackupService.this, ServiceCompat.STOP_FOREGROUND_REMOVE);
                stopSelf();
            }
        });
        mThread.start();
    }

    private void startImport(String importFilePath) {
        if (mThread != null && mThread.isAlive()) {
            log.warn("startImport: import already in progress");
            return;
        }

        mThread = new Thread(() -> {
            try {
                nb.setContentTitle(getString(R.string.media_library_import_in_progress));
                nm.notify(NOTIFICATION_ID, nb.build());

                importMediaLibrary(importFilePath);

                nm.cancel(NOTIFICATION_ID);
                showToast(getString(R.string.media_library_import_success));

                // Wait 2 seconds for user to see success message, then restart app
                if (log.isDebugEnabled()) log.debug("startImport: waiting 2 seconds before restarting app");
                Thread.sleep(2000);

                restartApplication();
            } catch (Exception e) {
                log.error("startImport: error importing media library", e);
                showToast(getString(R.string.media_library_import_error));
            } finally {
                ServiceCompat.stopForeground(MediaLibraryBackupService.this, ServiceCompat.STOP_FOREGROUND_REMOVE);
                stopSelf();
            }
        });
        mThread.start();
    }

    private String exportMediaLibrary() throws IOException {
        java.util.Set<String> reproducible=MigrationBackup.reproducibleFiles(this);
        DbHolder holder = VideoDb.getHolder(this);
        holder.get(); // Ensure even an empty library has its schema before snapshotting.
        holder.lockExclusive();
        try {
            holder.close();
            return exportLockedLibrary(reproducible);
        } finally { holder.unlockExclusive(); }
    }

    private String exportLockedLibrary(java.util.Set<String> reproducible) throws IOException {
        if (log.isDebugEnabled()) log.debug("exportMediaLibrary: starting full library export");

        // Get export directory
        File exportDir = getExternalFilesDir(null);
        if (exportDir == null) {
            exportDir = new File(getFilesDir(),"backups");
        }

        // Flush database WAL to ensure consistency
        if (log.isDebugEnabled()) log.debug("exportMediaLibrary: flushing database WAL");
        flushDatabaseWAL();

        // Create export zip file (fixed name, will overwrite previous backup)
        File zipFile = new File(exportDir, "nova-backup-" + System.currentTimeMillis() + ".zip");

        // Delete old backup if it exists
        if (zipFile.exists()) {
            if (log.isDebugEnabled()) log.debug("exportMediaLibrary: deleting existing backup file: {}", zipFile.getAbsolutePath());
            if (!zipFile.delete()) {
                log.warn("exportMediaLibrary: failed to delete existing backup file");
            }
        }

        if (log.isDebugEnabled()) log.debug("exportMediaLibrary: creating new backup file: {}", zipFile.getAbsolutePath());

        VerifiedBackup.write(zipFile, zos -> {
            try {
                byte[] settings = SettingsBackup.encode(androidx.preference.PreferenceManager.getDefaultSharedPreferences(this))
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8);
                zos.putNextEntry(new ZipEntry("settings.json")); zos.write(settings); zos.closeEntry();
                zos.putNextEntry(new ZipEntry("named_preferences.json"));zos.write(MigrationBackup.settings(this).getBytes(java.nio.charset.StandardCharsets.UTF_8));zos.closeEntry();
            } catch (org.json.JSONException e) { throw new IOException("Could not export settings", e); }
            // Export database version first
            int dbVersion = VideoOpenHelper.getDatabaseVersion();
            if (log.isDebugEnabled()) log.debug("exportMediaLibrary: adding database version={}", dbVersion);
            addVersionToZip(zos, dbVersion);

            // Export media database
            File dbFile = getDatabasePath(DATABASE_NAME);
            if (dbFile.exists()) {
                if (log.isDebugEnabled()) log.debug("exportMediaLibrary: exporting media database");
                addFileToZip(zos, dbFile, DATABASE_NAME);
            } else {
                throw new IOException("Media database is unavailable; backup was not created");
            }

            // Export credentials database (SMB/FTP/SFTP/WebDAV credentials)
            File credentialsDbFile = getDatabasePath(CREDENTIALS_DB_NAME);
            if (credentialsDbFile.exists()) {
                if (log.isDebugEnabled()) log.debug("exportMediaLibrary: exporting credentials database");
                addFileToZip(zos, credentialsDbFile, CREDENTIALS_DB_NAME);
            } else {
                if (log.isDebugEnabled()) log.debug("exportMediaLibrary: credentials database not found (no saved credentials)");
            }

            // Export shortcuts database (network shortcuts/bookmarks)
            File shortcutsDbFile = getDatabasePath(SHORTCUTS_DB_NAME);
            if (shortcutsDbFile.exists()) {
                if (log.isDebugEnabled()) log.debug("exportMediaLibrary: exporting shortcuts database");
                addFileToZip(zos, shortcutsDbFile, SHORTCUTS_DB_NAME);
            } else {
                if (log.isDebugEnabled()) log.debug("exportMediaLibrary: shortcuts database not found (no saved shortcuts)");
            }

            // Export shortcuts2 database (newer network shortcuts/bookmarks)
            File shortcuts2DbFile = getDatabasePath(SHORTCUTS2_DB_NAME);
            if (shortcuts2DbFile.exists()) {
                if (log.isDebugEnabled()) log.debug("exportMediaLibrary: exporting shortcuts2 database");
                addFileToZip(zos, shortcuts2DbFile, SHORTCUTS2_DB_NAME);
            } else {
                if (log.isDebugEnabled()) log.debug("exportMediaLibrary: shortcuts2 database not found (no saved shortcuts)");
            }

            // Export poster directory
            File posterDir = MediaScraper.getPosterDirectory(this);
            if (posterDir.exists()) {
                addPersonalArtworkToZip(zos, posterDir, "scraper_posters",reproducible);
            }

            // Export backdrop directory
            File backdropDir = MediaScraper.getBackdropDirectory(this);
            if (backdropDir.exists()) {
                addPersonalArtworkToZip(zos, backdropDir, "scraper_backdrops",reproducible);
            }

            // Episode stills are reproducible scraper output, not user-created artwork.
        });

        if (log.isDebugEnabled()) log.debug("exportMediaLibrary: export completed to {}", zipFile.getAbsolutePath());
        return zipFile.getAbsolutePath();
    }

    private void importMediaLibrary(String path) throws Exception {
        if(path==null||path.isEmpty()) throw new IOException("Select a backup file");
        java.io.InputStream input = path.startsWith("content:")
            ? getContentResolver().openInputStream(android.net.Uri.parse(path)) : new FileInputStream(path);
        if(input==null) throw new IOException("Backup unavailable");
        File stage;
        try(java.io.InputStream in=input) { stage=SafeBackup.stage(this,in); }
        try {
            // Keep a complete, dated recovery archive before changing any live data.
            exportMediaLibrary();
            SafeBackup.restore(this,stage);
            androidx.preference.PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("preview_restore_artwork_pending",true).commit();
        } finally { SafeBackup.remove(stage); }
    }

    private void flushDatabaseWAL() {
        try {
            for(String database:new String[]{DATABASE_NAME,CREDENTIALS_DB_NAME,SHORTCUTS_DB_NAME,SHORTCUTS2_DB_NAME}){File dbFile = getDatabasePath(database);
            if (dbFile.exists()) {
                if (log.isDebugEnabled()) log.debug("flushDatabaseWAL: opening database for WAL checkpoint");
                try(SQLiteDatabase db = SQLiteDatabase.openDatabase(dbFile.getAbsolutePath(), null,
                        SQLiteDatabase.OPEN_READWRITE)){
                // Checkpoint WAL to main database file
                if (log.isDebugEnabled()) log.debug("flushDatabaseWAL: executing PRAGMA wal_checkpoint(FULL)");
                try (android.database.Cursor checkpoint = db.rawQuery("PRAGMA wal_checkpoint(FULL)", null)) {
                    if (!checkpoint.moveToFirst() || checkpoint.getInt(0) != 0) throw new IllegalStateException("Database is busy");
                }
                }
                if (log.isDebugEnabled()) log.debug("flushDatabaseWAL: database WAL flushed successfully");
            }}
        } catch (Exception e) {
            throw new IllegalStateException("Cannot make a consistent database backup", e);
        }
    }

    private void addVersionToZip(ZipOutputStream zos, int version) throws IOException {
        if (log.isDebugEnabled()) log.debug("addVersionToZip: adding version={}", version);

        ZipEntry zipEntry = new ZipEntry(VERSION_FILE);
        zos.putNextEntry(zipEntry);

        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(zos));
        writer.write(String.valueOf(version));
        writer.newLine();
        writer.flush();

        zos.closeEntry();
    }

    private void cleanupExistingData() {
        if (log.isDebugEnabled()) log.debug("cleanupExistingData: DELETING all existing media library data");

        // Close database helper and connections before deleting files to release locks and files
        if (log.isDebugEnabled()) log.debug("cleanupExistingData: closing database connections");
        VideoDb.getHolder(this).close();
        ShortcutDbAdapter.VIDEO.close();

        // Delete media database files
        File dbFile = getDatabasePath(DATABASE_NAME);
        File dbWalFile = new File(dbFile.getAbsolutePath() + "-wal");
        File dbShmFile = new File(dbFile.getAbsolutePath() + "-shm");

        if (dbFile.exists()) {
            if (log.isDebugEnabled()) log.debug("cleanupExistingData: deleting {}", dbFile.getAbsolutePath());
            dbFile.delete();
        }
        if (dbWalFile.exists()) {
            if (log.isDebugEnabled()) log.debug("cleanupExistingData: deleting {}", dbWalFile.getAbsolutePath());
            dbWalFile.delete();
        }
        if (dbShmFile.exists()) {
            if (log.isDebugEnabled()) log.debug("cleanupExistingData: deleting {}", dbShmFile.getAbsolutePath());
            dbShmFile.delete();
        }

        // Delete credentials database files
        File credentialsDbFile = getDatabasePath(CREDENTIALS_DB_NAME);
        File credentialsDbWalFile = new File(credentialsDbFile.getAbsolutePath() + "-wal");
        File credentialsDbShmFile = new File(credentialsDbFile.getAbsolutePath() + "-shm");

        if (credentialsDbFile.exists()) {
            if (log.isDebugEnabled()) log.debug("cleanupExistingData: deleting {}", credentialsDbFile.getAbsolutePath());
            credentialsDbFile.delete();
        }
        if (credentialsDbWalFile.exists()) {
            if (log.isDebugEnabled()) log.debug("cleanupExistingData: deleting {}", credentialsDbWalFile.getAbsolutePath());
            credentialsDbWalFile.delete();
        }
        if (credentialsDbShmFile.exists()) {
            if (log.isDebugEnabled()) log.debug("cleanupExistingData: deleting {}", credentialsDbShmFile.getAbsolutePath());
            credentialsDbShmFile.delete();
        }

        // Delete shortcuts database files
        File shortcutsDbFile = getDatabasePath(SHORTCUTS_DB_NAME);
        File shortcutsDbWalFile = new File(shortcutsDbFile.getAbsolutePath() + "-wal");
        File shortcutsDbShmFile = new File(shortcutsDbFile.getAbsolutePath() + "-shm");

        if (shortcutsDbFile.exists()) {
            if (log.isDebugEnabled()) log.debug("cleanupExistingData: deleting {}", shortcutsDbFile.getAbsolutePath());
            shortcutsDbFile.delete();
        }
        if (shortcutsDbWalFile.exists()) {
            if (log.isDebugEnabled()) log.debug("cleanupExistingData: deleting {}", shortcutsDbWalFile.getAbsolutePath());
            shortcutsDbWalFile.delete();
        }
        if (shortcutsDbShmFile.exists()) {
            if (log.isDebugEnabled()) log.debug("cleanupExistingData: deleting {}", shortcutsDbShmFile.getAbsolutePath());
            shortcutsDbShmFile.delete();
        }

        // Delete shortcuts2 database files
        File shortcuts2DbFile = getDatabasePath(SHORTCUTS2_DB_NAME);
        File shortcuts2DbWalFile = new File(shortcuts2DbFile.getAbsolutePath() + "-wal");
        File shortcuts2DbShmFile = new File(shortcuts2DbFile.getAbsolutePath() + "-shm");

        if (shortcuts2DbFile.exists()) {
            if (log.isDebugEnabled()) log.debug("cleanupExistingData: deleting {}", shortcuts2DbFile.getAbsolutePath());
            shortcuts2DbFile.delete();
        }
        if (shortcuts2DbWalFile.exists()) {
            if (log.isDebugEnabled()) log.debug("cleanupExistingData: deleting {}", shortcuts2DbWalFile.getAbsolutePath());
            shortcuts2DbWalFile.delete();
        }
        if (shortcuts2DbShmFile.exists()) {
            if (log.isDebugEnabled()) log.debug("cleanupExistingData: deleting {}", shortcuts2DbShmFile.getAbsolutePath());
            shortcuts2DbShmFile.delete();
        }

        // Delete all poster files
        File posterDir = MediaScraper.getPosterDirectory(this);
        if (posterDir.exists()) {
            if (log.isDebugEnabled()) log.debug("cleanupExistingData: deleting all files in {}", posterDir.getAbsolutePath());
            deleteDirectoryContents(posterDir);
        }

        // Delete all backdrop files
        File backdropDir = MediaScraper.getBackdropDirectory(this);
        if (backdropDir.exists()) {
            if (log.isDebugEnabled()) log.debug("cleanupExistingData: deleting all files in {}", backdropDir.getAbsolutePath());
            deleteDirectoryContents(backdropDir);
        }

        // Delete all picture files
        File pictureDir = MediaScraper.getPictureDirectory(this);
        if (pictureDir.exists()) {
            if (log.isDebugEnabled()) log.debug("cleanupExistingData: deleting all files in {}", pictureDir.getAbsolutePath());
            deleteDirectoryContents(pictureDir);
        }

        if (log.isDebugEnabled()) log.debug("cleanupExistingData: cleanup completed");
    }

    private void deleteDirectoryContents(File dir) {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                deleteDirectoryContents(file);
                file.delete();
            } else {
                file.delete();
            }
        }
    }

    private void addFileToZip(ZipOutputStream zos, File file, String zipEntryName) throws IOException {
        if (log.isDebugEnabled()) log.debug("addFileToZip: {}", zipEntryName);

        ZipEntry zipEntry = new ZipEntry(zipEntryName);
        zos.putNextEntry(zipEntry);

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int len;
            while ((len = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, len);
            }
        }

        zos.closeEntry();
    }

    private void addPersonalArtworkToZip(ZipOutputStream zip,File dir,String prefix,java.util.Set<String> reproducible)throws IOException{File[] files=dir.listFiles();if(files==null)throw new IOException("Cannot list personal artwork");for(File file:files)if(file.isDirectory())addPersonalArtworkToZip(zip,file,prefix+"/"+file.getName(),reproducible);else if(!reproducible.contains(file.getCanonicalPath()))addFileToZip(zip,file,prefix+"/"+file.getName());}

    private void addDirectoryToZip(ZipOutputStream zos, File dir, String zipDirName) throws IOException {
        if (log.isDebugEnabled()) log.debug("addDirectoryToZip: {}", zipDirName);

        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            String zipEntryName = zipDirName + "/" + file.getName();

            if (file.isDirectory()) {
                addDirectoryToZip(zos, file, zipEntryName);
            } else {
                addFileToZip(zos, file, zipEntryName);
            }
        }
    }

    private void showToast(final String message) {
        new android.os.Handler(android.os.Looper.getMainLooper()).post(() ->
                Toast.makeText(MediaLibraryBackupService.this, message, Toast.LENGTH_LONG).show()
        );
    }

    private void restartApplication() {
        if (log.isDebugEnabled()) log.debug("restartApplication: restarting app after import");

        try {
            // Get the main activity intent
            Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                // Schedule app restart after a short delay
                int pendingIntentFlags = android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE;

                android.app.PendingIntent pendingIntent = android.app.PendingIntent.getActivity(
                        this,
                        0,
                        intent,
                        pendingIntentFlags
                );

                android.app.AlarmManager alarmManager = (android.app.AlarmManager) getSystemService(Context.ALARM_SERVICE);
                if (alarmManager != null) {
                    alarmManager.set(
                            android.app.AlarmManager.RTC,
                            System.currentTimeMillis() + 500, // 500ms delay
                            pendingIntent
                    );
                }

                if (log.isDebugEnabled()) log.debug("restartApplication: app restart scheduled, exiting process");

                // Exit the current process
                System.exit(0);
            }
        } catch (Exception e) {
            log.error("restartApplication: failed to restart app", e);
        }
    }

    @Override
    public void onDestroy() {
        if (log.isDebugEnabled()) log.debug("onDestroy");
        super.onDestroy();
    }
}
