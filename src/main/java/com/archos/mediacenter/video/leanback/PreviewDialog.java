package com.archos.mediacenter.video.leanback;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.*;
import android.widget.*;
import java.util.function.IntConsumer;
/** Content-sized Nova menus. The caller still owns every real action and selection. */
public final class PreviewDialog {
 /** Presentation only for retained credential/artwork dialogs; original listeners and inputs remain. */
 public static void styleNative(Dialog dialog){
  Context c=dialog.getContext();if(!androidx.preference.PreferenceManager.getDefaultSharedPreferences(c).getBoolean("try_new_ui",false))return;
  Window window=dialog.getWindow();if(window==null)return;window.setBackgroundDrawable(surface(c,false));window.setDimAmount(.35f);
  window.setLayout(Math.min(dp(c,560),c.getResources().getDisplayMetrics().widthPixels-dp(c,64)),WindowManager.LayoutParams.WRAP_CONTENT);
  styleNativeChildren(window.getDecorView(),c);
 }
 private static void styleNativeChildren(View view,Context c){
  if(view instanceof TextView){TextView text=(TextView)view;if(!(view instanceof EditText)){text.setTextColor(0xffd6e5ef);text.setTextSize(Math.min(15,text.getTextSize()/c.getResources().getDisplayMetrics().scaledDensity));}if(view instanceof Button){view.setBackground(focus(c));view.setMinimumHeight(dp(c,36));}}
  if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)styleNativeChildren(((ViewGroup)view).getChildAt(i),c);
 }
 public static Dialog read(Context c,String title,String body){
  Dialog dialog=new Dialog(c);dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);LinearLayout panel=new LinearLayout(c);panel.setOrientation(android.widget.LinearLayout.VERTICAL);panel.setPadding(dp(c,20),dp(c,16),dp(c,20),dp(c,16));panel.setBackground(surface(c,false));
  TextView heading=new TextView(c);heading.setText(title);heading.setTextSize(20);heading.setTextColor(Color.WHITE);panel.addView(heading);
  ScrollView scroll=new ScrollView(c);TextView text=new TextView(c);text.setText(body);text.setTextColor(0xffd2e1ed);text.setTextSize(13);text.setPadding(0,dp(c,14),0,dp(c,14));scroll.addView(text);scroll.setFocusable(true);panel.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
  LinearLayout actions=new LinearLayout(c);for(String label:new String[]{"Close","Copy"}){TextView button=new TextView(c);button.setText(label);button.setTextSize(13);button.setTextColor(Color.WHITE);button.setGravity(Gravity.CENTER);button.setFocusable(true);button.setBackground(focus(c));button.setOnClickListener(v->{if(label.equals("Close"))dialog.dismiss();else{android.content.ClipboardManager clipboard=(android.content.ClipboardManager)c.getSystemService(Context.CLIPBOARD_SERVICE);if(clipboard!=null)clipboard.setPrimaryClip(android.content.ClipData.newPlainText(title,body));Toast.makeText(c,"Copied",Toast.LENGTH_SHORT).show();}});actions.addView(button,new LinearLayout.LayoutParams(dp(c,90),dp(c,38)));}panel.addView(actions);
  dialog.setContentView(panel);dialog.show();Window w=dialog.getWindow();w.setBackgroundDrawableResource(android.R.color.transparent);w.setDimAmount(.35f);w.setLayout(Math.min(dp(c,720),c.getResources().getDisplayMetrics().widthPixels-dp(c,56)),Math.min(dp(c,420),c.getResources().getDisplayMetrics().heightPixels-dp(c,56)));scroll.requestFocus();return dialog;
 }
 public static Dialog choose(Context c,String title,String[] labels,int selected,IntConsumer action){
  return choose(c,title,labels,selected,selected<0?java.util.Collections.emptySet():java.util.Collections.singleton(selected),action);
 }
 public static Dialog choose(Context c,String title,String[] labels,int selected,java.util.Set<Integer> checked,IntConsumer action){
  return choose(c,title,labels,selected,checked,true,action);
 }
 public static Dialog choose(Context c,String title,String[] labels,int selected,java.util.Set<Integer> checked,boolean dismissOnSelect,IntConsumer action){
  Dialog d=new Dialog(c);d.requestWindowFeature(Window.FEATURE_NO_TITLE);
  LinearLayout panel=new LinearLayout(c);panel.setOrientation(android.widget.LinearLayout.VERTICAL);int pad=dp(c,12);panel.setPadding(pad,pad,pad,pad);panel.setBackground(surface(c,false));
  TextView heading=new TextView(c);heading.setText(title);heading.setTextSize(17);heading.setTextColor(Color.WHITE);heading.setPadding(dp(c,6),dp(c,2),0,dp(c,12));panel.addView(heading);
  ScrollView scroll=new ScrollView(c);scroll.setVerticalScrollBarEnabled(false);LinearLayout rows=new LinearLayout(c);rows.setOrientation(android.widget.LinearLayout.VERTICAL);scroll.addView(rows);panel.addView(scroll,new LinearLayout.LayoutParams(-1,-2));
  View initial=null;int height=60;
  for(int i=0;i<labels.length;i++){final int index=i;boolean group=labels[i].startsWith("— ");boolean enabled=!group&&!labels[i].contains("unavailable")&&!labels[i].contains("Coming soon");
   LinearLayout row=new LinearLayout(c);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(c,10),0,dp(c,10),0);row.setBackground(focus(c));row.setFocusable(enabled);row.setFocusableInTouchMode(enabled);row.setEnabled(enabled);row.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);row.setAlpha(enabled?1f:group?1f:.4f);
   if(!group){ImageView icon=new ImageView(c);String iconLabel=title.equals(c.getString(com.archos.mediacenter.video.R.string.menu_audio))?"Audio":title.equals(c.getString(com.archos.mediacenter.video.R.string.menu_subtitles))?"Subtitles":labels[i];icon.setImageDrawable(new PreviewIcon(iconLabel));row.addView(icon,new LinearLayout.LayoutParams(dp(c,18),dp(c,18)));}
   TextView label=new TextView(c);label.setText(group?labels[i].substring(2):labels[i]);label.setTextSize(group?11:14);label.setTextColor(group?0xff8aaec5:Color.WHITE);label.setSingleLine(true);label.setEllipsize(android.text.TextUtils.TruncateAt.END);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.leftMargin=group?0:dp(c,10);row.addView(label,lp);
   if(!group){ImageView check=new ImageView(c);check.setTag("preview-check:"+i);check.setImageDrawable(new PreviewIcon("check"));check.setVisibility(checked.contains(i)?View.VISIBLE:View.INVISIBLE);row.addView(check,new LinearLayout.LayoutParams(dp(c,18),dp(c,18)));}
   row.setTag(i);row.setContentDescription(labels[i]+(checked.contains(i)?", selected":""));row.setOnClickListener(v->{if(dismissOnSelect)d.dismiss();action.accept(index);});int rh=group?25:37;height+=rh;rows.addView(row,new LinearLayout.LayoutParams(-1,dp(c,rh)));if(enabled&&(initial==null||i==selected))initial=row;
  }
  d.setContentView(panel);d.show();Window w=d.getWindow();w.setBackgroundDrawableResource(android.R.color.transparent);w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);w.setDimAmount(.32f);w.setLayout(Math.min(dp(c,title.equals("More")?330:280),c.getResources().getDisplayMetrics().widthPixels-dp(c,64)),Math.min(dp(c,height),c.getResources().getDisplayMetrics().heightPixels-dp(c,64)));if(initial!=null)initial.requestFocus();return d;
 }
 public static void updateChecks(Dialog dialog,java.util.Set<Integer> checked){if(dialog==null||dialog.getWindow()==null)return;updateChecks(dialog.getWindow().getDecorView(),checked);}
 private static void updateChecks(View view,java.util.Set<Integer> checked){Object tag=view.getTag();if(tag instanceof String&&((String)tag).startsWith("preview-check:")){int index=Integer.parseInt(((String)tag).substring(14));view.setVisibility(checked.contains(index)?View.VISIBLE:View.INVISIBLE);View parent=(View)view.getParent();CharSequence description=parent.getContentDescription();if(description!=null)parent.setContentDescription(description.toString().replace(", selected","")+(checked.contains(index)?", selected":""));}if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)updateChecks(((ViewGroup)view).getChildAt(i),checked);}
 public static Dialog confirmDelete(Context c,String title,String message,Runnable action){android.app.AlertDialog dialog=new android.app.AlertDialog.Builder(c).setTitle(title).setMessage(message).setIcon(android.R.drawable.ic_dialog_alert).setNegativeButton("Cancel",null).setPositiveButton("Delete",(d,w)->action.run()).create();dialog.setOnShowListener(d->{styleNative(dialog);dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setTextColor(0xffffa5a5);dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE).requestFocus();});dialog.show();return dialog;}
 public static int dp(Context c,int v){return Math.round(v*c.getResources().getDisplayMetrics().density);}
 public static StateListDrawable focus(Context c){StateListDrawable s=new StateListDrawable();s.addState(new int[]{android.R.attr.state_focused},surface(c,true));s.addState(new int[]{},new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));return s;}
 public static GradientDrawable surface(Context c,boolean f){GradientDrawable g=new GradientDrawable();g.setColor(f?PreviewAccent.alpha(c,70):0xef0b1b29);g.setCornerRadius(dp(c,6));g.setStroke(dp(c,1),f?PreviewAccent.color(c):0x50426a80);return g;}
}
