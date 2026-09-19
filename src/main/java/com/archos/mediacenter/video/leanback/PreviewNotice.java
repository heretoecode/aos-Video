package com.archos.mediacenter.video.leanback;
import android.app.Activity;
import android.content.Context;
import android.view.*;
import android.widget.*;
/** Non-modal feedback; never takes remote focus. */
public final class PreviewNotice {
 public static void show(Context context,String message,boolean error){if(!(context instanceof Activity)){Toast.makeText(context,message,Toast.LENGTH_LONG).show();return;}Activity a=(Activity)context;a.runOnUiThread(()->{FrameLayout root=a.findViewById(android.R.id.content);if(root==null)return;View old=root.findViewWithTag("preview-notice");if(old!=null)root.removeView(old);TextView note=new TextView(a);note.setTag("preview-notice");note.setText(message);note.setTextSize(13);note.setTextColor(error?0xffffb4b4:0xffdceaf5);note.setMaxWidth(PreviewDialog.dp(a,450));note.setPadding(PreviewDialog.dp(a,16),PreviewDialog.dp(a,12),PreviewDialog.dp(a,16),PreviewDialog.dp(a,12));note.setBackground(PreviewDialog.surface(a,false));note.setFocusable(false);FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(-2,-2,Gravity.BOTTOM|Gravity.END);p.setMargins(20,20,PreviewDialog.dp(a,26),PreviewDialog.dp(a,22));root.addView(note,p);note.postDelayed(()->note.animate().alpha(0).setDuration(180).withEndAction(()->root.removeView(note)).start(),error?5500:3000);});}
 private PreviewNotice(){}
}
