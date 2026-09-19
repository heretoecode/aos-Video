package com.archos.mediacenter.video.leanback;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;
import com.archos.mediaprovider.video.NetworkAutoRefresh;
import com.archos.mediaprovider.video.NetworkScannerUtil;

/** Initialise only previously unconfigured automation. Never replace an explicit Off/schedule. */
public final class PreviewAutoScanPolicy {
 public static void initialise(Context c){
  SharedPreferences p=PreferenceManager.getDefaultSharedPreferences(c);
  if(!p.getBoolean("try_new_ui",false))return;
  SharedPreferences.Editor e=p.edit();
  if(!p.contains("auto_rescan_on_app_restart"))e.putBoolean("auto_rescan_on_app_restart",true);
  // An absent period previously meant no foreground polling and no scheduled job.
  if(!p.contains(NetworkAutoRefresh.AUTO_RESCAN_PERIOD))e.putInt(NetworkAutoRefresh.AUTO_RESCAN_PERIOD,15*60*1000);
  e.apply();
  int period=p.getInt(NetworkAutoRefresh.AUTO_RESCAN_PERIOD,0);
  if(period>0){int start=p.getInt(NetworkAutoRefresh.AUTO_RESCAN_STARTING_TIME_PREF,0);NetworkScannerUtil.scheduleNewRescan(c,start<0?0:start,period,true);}
 }
 private PreviewAutoScanPolicy(){}
}
