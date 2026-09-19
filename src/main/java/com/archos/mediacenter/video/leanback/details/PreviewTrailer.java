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
        web.getSettings().setMediaPlaybackRequiresUserGesture(false);web.getSettings().setJavaScriptEnabled(true);web.getSettings().setDomStorageEnabled(true);web.getSettings().setAllowFileAccess(false);web.getSettings().setAllowContentAccess(false);
        web.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        web.setWebViewClient(new WebViewClient(){@Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){String host=r.getUrl().getHost();return host==null||!(host.equals("www.youtube.com")||host.equals("www.youtube-nocookie.com"));}});
        final View previous=activity.getCurrentFocus();
        Dialog dialog=new Dialog(activity);dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout layout=new LinearLayout(activity);layout.setOrientation(LinearLayout.VERTICAL);layout.setBackgroundColor(0xff0b1b2a);
        LinearLayout bar=new LinearLayout(activity);bar.setGravity(Gravity.CENTER_VERTICAL);bar.setPadding(dp(activity,16),dp(activity,6),dp(activity,16),dp(activity,6));layout.addView(bar,new LinearLayout.LayoutParams(-1,dp(activity,46)));
        TextView title=new TextView(activity);title.setText("Official trailer · "+trailer.mName);title.setTextSize(14);title.setTextColor(0xffffffff);title.setSingleLine(true);title.setEllipsize(android.text.TextUtils.TruncateAt.END);bar.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        for(String label:new String[]{"Open YouTube","Close"}){TextView button=new TextView(activity);button.setText(label);button.setTextColor(0xffffffff);button.setTextSize(12);button.setPadding(dp(activity,12),dp(activity,8),dp(activity,12),dp(activity,8));com.archos.mediacenter.video.leanback.PreviewIcon.apply(button,label,16);button.setFocusable(true);button.setBackground(com.archos.mediacenter.video.leanback.PreviewDialog.focus(activity));button.setOnClickListener(v->{dialog.dismiss();if(label.equals("Open YouTube"))external(activity,fallback);});bar.addView(button);}
        web.setBackgroundColor(0xff000000);web.setLayerType(View.LAYER_TYPE_HARDWARE,null);web.setWebChromeClient(new WebChromeClient());
        layout.addView(web,new LinearLayout.LayoutParams(-1,0,1));dialog.setContentView(layout);
        dialog.setOnDismissListener(d->{web.stopLoading();web.loadUrl("about:blank");web.onPause();layout.removeView(web);web.destroy();if(previous!=null)previous.requestFocus();});
        dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);dialog.show();dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);dialog.getWindow().setDimAmount(.45f);dialog.getWindow().setLayout((int)(activity.getResources().getDisplayMetrics().widthPixels*.94),(int)(activity.getResources().getDisplayMetrics().heightPixels*.92));
        web.loadDataWithBaseURL("https://"+activity.getPackageName()+"/", "<html style='height:100%'><body style='margin:0;background:#000;height:100%;overflow:hidden'><iframe width='100%' height='100%' src='https://www.youtube.com/embed/"+trailer.mVideoKey+"?playsinline=1&autoplay=1' frameborder='0' allow='autoplay; encrypted-media; fullscreen' allowfullscreen></iframe></body></html>","text/html","UTF-8",null);
    }
    private static int dp(Activity a,int n){return Math.round(n*a.getResources().getDisplayMetrics().density);}
    private static void external(Activity a,Uri uri){try{a.startActivity(new Intent(Intent.ACTION_VIEW,uri));}catch(ActivityNotFoundException e){Toast.makeText(a,"No app available to open this trailer",Toast.LENGTH_LONG).show();}}
}
