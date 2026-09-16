package com.archos.mediacenter.video.leanback.details;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.webkit.*;
import android.view.*;
import android.widget.*;
import com.archos.mediascraper.ScraperTrailer;

/** Official embedded player, with an explicit fallback on devices without a WebView provider. */
public final class PreviewTrailer {
    public static void show(Activity activity,ScraperTrailer trailer){
        if(!"YouTube".equals(trailer.mSite)||trailer.mVideoKey==null||!trailer.mVideoKey.matches("[A-Za-z0-9_-]{11}")){
            Toast.makeText(activity,"This trailer format is not supported",Toast.LENGTH_LONG).show();return;
        }
        Uri fallback=Uri.parse("https://www.youtube.com/watch?v="+trailer.mVideoKey);
        WebView web;
        try{web=new WebView(activity);}catch(RuntimeException unavailable){
            new AlertDialog.Builder(activity).setTitle("Trailer").setMessage("In-app trailers are unavailable on this device.").setPositiveButton("Open YouTube",(d,w)->external(activity,fallback)).setNegativeButton("Cancel",null).show();return;
        }
        web.getSettings().setJavaScriptEnabled(true);web.getSettings().setDomStorageEnabled(true);web.getSettings().setAllowFileAccess(false);web.getSettings().setAllowContentAccess(false);
        web.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        web.setWebViewClient(new WebViewClient(){@Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){String host=r.getUrl().getHost();return host==null||!(host.equals("www.youtube.com")||host.equals("www.youtube-nocookie.com"));}});
        LinearLayout layout=new LinearLayout(activity);layout.setOrientation(LinearLayout.VERTICAL);layout.addView(web,new LinearLayout.LayoutParams(-1,-1));
        AlertDialog dialog=new AlertDialog.Builder(activity).setTitle(trailer.mName).setView(layout).setPositiveButton("Close",null).setNeutralButton("Open YouTube",(d,w)->external(activity,fallback)).create();
        dialog.setOnDismissListener(d->{web.stopLoading();web.loadUrl("about:blank");web.onPause();layout.removeView(web);web.destroy();});
        dialog.show();dialog.getWindow().setLayout((int)(activity.getResources().getDisplayMetrics().widthPixels*.9),(int)(activity.getResources().getDisplayMetrics().heightPixels*.85));
        web.loadDataWithBaseURL("https://"+activity.getPackageName()+"/", "<html><body style='margin:0;background:#000'><iframe width='100%' height='100%' src='https://www.youtube.com/embed/"+trailer.mVideoKey+"?playsinline=1' frameborder='0' allow='encrypted-media; fullscreen' allowfullscreen></iframe></body></html>","text/html","UTF-8",null);
    }
    private static void external(Activity a,Uri uri){try{a.startActivity(new Intent(Intent.ACTION_VIEW,uri));}catch(ActivityNotFoundException e){Toast.makeText(a,"No app available to open this trailer",Toast.LENGTH_LONG).show();}}
}
