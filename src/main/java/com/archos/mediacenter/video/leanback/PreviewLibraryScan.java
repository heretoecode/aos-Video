package com.archos.mediacenter.video.leanback;
import android.content.Context;
import android.media.MediaScannerConnection;
import android.os.Environment;
import android.os.SystemClock;
import android.widget.Toast;
import com.archos.filecorelibrary.ExtStorageManager;
import com.archos.mediaprovider.video.NetworkAutoRefresh;
import com.archos.mediaprovider.video.NetworkScannerReceiver;
import java.util.LinkedHashSet;
/** Uses the existing local scanner and indexed-source network scheduler. No new importer. */
public final class PreviewLibraryScan {
 private static long requestedAt;
 public static synchronized void request(Context context){
  if(NetworkScannerReceiver.isScannerWorking()||requestedAt>0&&SystemClock.elapsedRealtime()-requestedAt<15000){PreviewNotice.show(context,"A library scan is already running or queued",false);return;}
  requestedAt=SystemClock.elapsedRealtime();Context app=context.getApplicationContext();
  LinkedHashSet<String> roots=new LinkedHashSet<>();roots.add(Environment.getExternalStorageDirectory().getAbsolutePath());ExtStorageManager storage=ExtStorageManager.getExtStorageManager();if(storage.hasExtStorage()){roots.addAll(storage.getExtSdcards());roots.addAll(storage.getExtUsbStorages());roots.addAll(storage.getExtOtherStorages());}
  MediaScannerConnection.scanFile(app,roots.toArray(new String[0]),null,null);NetworkAutoRefresh.forceRescan(app);PreviewNotice.show(context,"Local and indexed network library scan requested",false);
 }
}
