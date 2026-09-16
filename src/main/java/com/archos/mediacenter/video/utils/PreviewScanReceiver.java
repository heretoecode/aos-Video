package com.archos.mediacenter.video.utils;

import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import com.archos.mediaprovider.ArchosMediaIntent;
import com.archos.mediaprovider.video.NetworkScannerReceiver;
import com.archos.mediacenter.utils.ShortcutDbAdapter;
import java.util.concurrent.ConcurrentHashMap;

/** Adds presentation-only source labels to the existing scanner broadcast receiver. */
public final class PreviewScanReceiver extends NetworkScannerReceiver {
    private static final ConcurrentHashMap<String,String> sources=new ConcurrentHashMap<>();
    public static String currentSource(){return sources.size()==1?sources.values().stream().findFirst().orElse("network"):"network";}
    @Override public void onReceive(Context context,Intent intent){
        super.onReceive(context,intent); // retain all scanner state/accounting
        Uri uri=intent.getData();if(uri==null)return;
        String key=uri.toString();
        if(ArchosMediaIntent.ACTION_VIDEO_SCANNER_SCAN_FINISHED.equals(intent.getAction())){sources.remove(key);return;}
        if(!ArchosMediaIntent.ACTION_VIDEO_SCANNER_SCAN_STARTED.equals(intent.getAction()))return;
        String scheme=uri.getScheme();String fallback="smb".equals(scheme)?"SMB":("webdav".equals(scheme)||"webdavs".equals(scheme)||"dav".equals(scheme)||"davs".equals(scheme))?"WebDAV":("file".equals(scheme)||"content".equals(scheme))?"local library":"network";
        sources.put(key,fallback);
        final PendingResult pending=goAsync();final Context app=context.getApplicationContext();
        new Thread(()->{
            try(Cursor c=ShortcutDbAdapter.VIDEO.getAllShortcuts(app,null,null)){
                String name=null;int length=-1;
                if(c!=null)while(c.moveToNext()){
                    String path=c.getString(c.getColumnIndexOrThrow(ShortcutDbAdapter.KEY_PATH));
                    String candidate=c.getString(c.getColumnIndexOrThrow(ShortcutDbAdapter.KEY_NAME));
                    if(path!=null&&candidate!=null&&!candidate.trim().isEmpty()&&(key.equals(path)||key.startsWith(path.endsWith("/")?path:path+"/"))&&path.length()>length){name=candidate;length=path.length();}
                }
                if(name!=null){final String friendly=name;sources.computeIfPresent(key,(k,v)->friendly);}
            }catch(RuntimeException ignored){/* Protocol fallback remains available. */}finally{pending.finish();}
        },"preview-scan-label").start();
    }
}
