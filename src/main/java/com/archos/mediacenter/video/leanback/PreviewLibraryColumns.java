package com.archos.mediacenter.video.leanback;

import android.app.Dialog;
import android.content.*;
import android.graphics.Color;
import android.net.Uri;
import android.text.TextUtils;
import android.view.*;
import android.widget.*;
import androidx.preference.PreferenceManager;
import com.archos.mediacenter.video.leanback.PreviewLibraryLoader.Entry;
import com.archos.mediacenter.video.browser.adapters.object.*;
import com.squareup.picasso.Picasso;
import java.util.*;

/** Shared library table. All values come from the background-built database snapshot. */
public final class PreviewLibraryColumns {
 public enum Column {
  TITLE("Title",3.5f), YEAR("Year",.8f), SEASONS("Seasons",.85f), EPISODES("Episodes",.9f),
  DURATION("Runtime",1.1f), RESOLUTION("Resolution",1.05f), HDR("HDR",.65f), AUDIO("Audio",1.1f),
  SIZE("File size",1.15f), AVERAGE("Avg. ep. size",1.15f), ADDED("Date added",1.25f),
  MODIFIED("Modified",1.25f), CODEC("Codec",1.05f), BITRATE("Bitrate",1f), CONTAINER("Container",.9f), PATH("Path",2f);
  final String label;final float width;
  Column(String label,float width){this.label=label;this.width=width;}
 }
 private final Context context;
 private final SharedPreferences prefs;
 private final String prefix;
 private final boolean tv;
 private final List<Column> order=new ArrayList<>();
 private final Set<Column> shown=EnumSet.noneOf(Column.class);
 public Column sortColumn;
 public boolean ascending=true;
 public PreviewLibraryColumns(Context c,boolean tv){context=c;this.tv=tv;prefs=PreferenceManager.getDefaultSharedPreferences(c);prefix="preview_columns_"+(tv?"tv_":"movies_");reset(false);
  if(prefs.getBoolean("remember_library_views",true)){
   List<Column> saved=parse(prefs.getString(prefix+"order",""));if(!saved.isEmpty()){order.clear();order.addAll(saved);for(Column col:available())if(!order.contains(col))order.add(col);}
   String visible=prefs.getString(prefix+"shown",null);if(visible!=null){shown.clear();shown.addAll(parse(visible));shown.add(Column.TITLE);}
   try{sortColumn=Column.valueOf(prefs.getString(prefix+"sort",""));}catch(IllegalArgumentException ignored){}
   ascending=prefs.getBoolean(prefix+"ascending",true);
  }
 }
 private List<Column> available(){List<Column> values=new ArrayList<>(Arrays.asList(Column.values()));if(!tv)values.removeAll(Arrays.asList(Column.SEASONS,Column.EPISODES,Column.AVERAGE));return values;}
 private List<Column> parse(String value){List<Column> result=new ArrayList<>();for(String key:value.split(","))try{Column c=Column.valueOf(key);if(available().contains(c)&&!result.contains(c))result.add(c);}catch(IllegalArgumentException ignored){}return result;}
 private void reset(boolean save){order.clear();order.addAll(available());shown.clear();shown.addAll(tv?Arrays.asList(Column.TITLE,Column.SEASONS,Column.EPISODES,Column.DURATION,Column.RESOLUTION,Column.SIZE,Column.AVERAGE,Column.ADDED):Arrays.asList(Column.TITLE,Column.YEAR,Column.DURATION,Column.RESOLUTION,Column.AUDIO,Column.SIZE,Column.ADDED));if(save)save();}
 private void save(){if(!prefs.getBoolean("remember_library_views",true))return;prefs.edit().putString(prefix+"order",TextUtils.join(",",order)).putString(prefix+"shown",TextUtils.join(",",shown)).putString(prefix+"sort",sortColumn==null?"":sortColumn.name()).putBoolean(prefix+"ascending",ascending).apply();}
 public List<Column> visible(){List<Column> result=new ArrayList<>();for(Column c:order)if(shown.contains(c))result.add(c);return result;}
 public void setAscending(boolean value){ascending=value;save();}
 public void clearSort(){sortColumn=null;save();}
 public List<Entry> sort(List<Entry> source){if(sortColumn==null)return source;List<Entry> copy=new ArrayList<>(source);Column column=sortColumn;
  copy.sort((a,b)->{boolean ak=known(a,column),bk=known(b,column);if(ak!=bk)return ak?-1:1;int compare=isNumeric(column)?Long.compare(number(a,column),number(b,column)):value(a,column).compareToIgnoreCase(value(b,column));if(!ascending)compare=-compare;return compare!=0?compare:PreviewPages.displayName(a).compareToIgnoreCase(PreviewPages.displayName(b));});return copy;
 }
 private static boolean isNumeric(Column c){return Arrays.asList(Column.YEAR,Column.SEASONS,Column.EPISODES,Column.DURATION,Column.SIZE,Column.AVERAGE,Column.ADDED,Column.MODIFIED,Column.BITRATE,Column.RESOLUTION).contains(c);}
 private static long number(Entry e,Column c){switch(c){case RESOLUTION:return resolutionRank(e.resolution);case YEAR:return e.year();case SEASONS:return e.seasons;case EPISODES:return e.episodes;case DURATION:return e.runtime;case SIZE:return e.bytes;case AVERAGE:return e.episodes>0?e.bytes/e.episodes:0;case ADDED:return e.added;case MODIFIED:return e.modified;case BITRATE:return e.bitrate;default:return 0;}}
 private static long resolutionRank(String value){if(value==null)return 0;if(value.equals("SD"))return 480;if(value.equals("8K"))return 4320;if(value.equals("4K"))return 2160;try{return Long.parseLong(value.replace("p","").replace("i",""));}catch(NumberFormatException ignored){return 0;}}
 private boolean known(Entry e,Column c){if(c==Column.AVERAGE&&e.knownSizes!=e.files)return false;return isNumeric(c)?number(e,c)>0:!value(e,c).isEmpty();}
 public String value(Entry e,Column c){switch(c){
  case TITLE:return PreviewPages.displayName(e);case YEAR:return e.year()>0?String.valueOf(e.year()):"";
  case SEASONS:return e.seasons>0?String.valueOf(e.seasons):"";case EPISODES:return e.episodes>0?String.valueOf(e.episodes):"";
  case DURATION:return duration(e.runtime);case RESOLUTION:return e.resolution;case HDR:return e.hdr;case AUDIO:return e.audio;case CODEC:return e.codec;
  case SIZE:return e.knownSizes==0?"":(e.knownSizes<e.files?"≥ ":"")+android.text.format.Formatter.formatShortFileSize(context,e.bytes);
  case AVERAGE:return e.episodes>0&&e.knownSizes==e.files?android.text.format.Formatter.formatShortFileSize(context,e.bytes/e.episodes):"";
  case ADDED:return date(e.added);case MODIFIED:return date(e.modified);case PATH:return e.path;case CONTAINER:return e.container;
  case BITRATE:return e.bitrate>0?String.format(Locale.UK,"%.1f Mb/s",e.bitrate/1000000d):"";default:return "";
 }}
 private static String duration(long ms){if(ms<=0)return "";long minutes=ms/60000;return minutes>=60?minutes/60+"h "+minutes%60+"m":minutes+" min";}
 private static String date(long date){if(date<=0)return "";return new java.text.SimpleDateFormat("dd MMM yy",Locale.UK).format(new Date(date<100000000000L?date*1000:date));}
 private TextView text(String value,int size){TextView t=new TextView(context);t.setText(value);t.setTextSize(size);t.setTextColor(0xffd5e4ee);t.setGravity(Gravity.CENTER_VERTICAL);t.setPadding(dp(6),0,dp(6),0);t.setSingleLine(true);t.setEllipsize(TextUtils.TruncateAt.END);t.setIncludeFontPadding(false);return t;}
 public View header(Runnable changed){LinearLayout row=new LinearLayout(context);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(0,dp(5),0,0);
  for(Column c:visible()){TextView label=text(c.label+(sortColumn==c?(ascending?" ↑":" ↓"):""),11);label.setFocusable(true);label.setTag("column:"+c);label.setContentDescription(c.label+", sort"+(sortColumn==c?(ascending?", ascending":", descending"):""));label.setBackground(PreviewDialog.focus(context));label.setOnClickListener(v->{ascending=sortColumn==c?!ascending:c==Column.TITLE||c==Column.YEAR;sortColumn=c;save();changed.run();});row.addView(label,new LinearLayout.LayoutParams(0,dp(30),c.width));}
  return row;
 }
 public LinearLayout newRow(){LinearLayout row=new LinearLayout(context);row.setGravity(Gravity.CENTER_VERTICAL);row.setFocusable(true);row.setFocusableInTouchMode(true);row.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);android.graphics.drawable.StateListDrawable background=new android.graphics.drawable.StateListDrawable();android.graphics.drawable.GradientDrawable selected=new android.graphics.drawable.GradientDrawable();selected.setColor(PreviewAccent.alpha(context,45));selected.setStroke(dp(1),PreviewAccent.color(context));background.addState(new int[]{android.R.attr.state_focused},selected);android.graphics.drawable.GradientDrawable separator=new android.graphics.drawable.GradientDrawable();separator.setColor(0x180b1b29);android.graphics.drawable.LayerDrawable base=new android.graphics.drawable.LayerDrawable(new android.graphics.drawable.Drawable[]{separator,new android.graphics.drawable.ColorDrawable(0x50708695)});base.setLayerHeight(1,dp(1));base.setLayerGravity(1,android.view.Gravity.BOTTOM);background.addState(new int[]{},base);row.setBackground(background);return row;}
 public void bind(LinearLayout row,Entry entry){clear(row);row.removeAllViews();StringBuilder description=new StringBuilder();
  for(Column c:visible()){
   if(c==Column.TITLE){LinearLayout title=new LinearLayout(context);title.setGravity(Gravity.CENTER_VERTICAL);title.setPadding(dp(5),dp(3),0,dp(3));ImageView image=new ImageView(context);image.setScaleType(ImageView.ScaleType.FIT_CENTER);title.addView(image,new LinearLayout.LayoutParams(dp(58),dp(34)));Uri uri=entry.backdrop!=null?entry.backdrop:entry.media.getPosterUri();if(uri!=null)Picasso.get().load(uri).resize(dp(58),dp(34)).centerInside().noFade().into(image);TextView name=text(value(entry,c),12);name.setTextColor(Color.WHITE);title.addView(name,new LinearLayout.LayoutParams(0,-1,1));row.addView(title,new LinearLayout.LayoutParams(0,dp(42),c.width));
   }else{TextView cell=text(value(entry,c),11);row.addView(cell,new LinearLayout.LayoutParams(0,dp(42),c.width));}
   String value=value(entry,c);if(!value.isEmpty())description.append(c.label).append(": ").append(value).append(". ");
  }
  row.setContentDescription(description.toString());
 }
 public void clear(View view){if(view instanceof ImageView){Picasso.get().cancelRequest((ImageView)view);((ImageView)view).setImageDrawable(null);}if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)clear(((ViewGroup)view).getChildAt(i));}
 public void choose(Runnable changed){choose(changed,0);}
 private void choose(Runnable changed,int focus){List<String> labels=new ArrayList<>();Set<Integer> checked=new HashSet<>();for(int i=0;i<order.size();i++){Column c=order.get(i);labels.add(c.label+(c==Column.TITLE?" (always shown)":"")+"   ≡");if(shown.contains(c))checked.add(i);}labels.add("Reset to default");labels.add("Done");final Dialog[] menu={null};
  menu[0]=PreviewDialog.choose(context,"Columns · ◀ ▶ to reorder",labels.toArray(new String[0]),focus,checked,false,n->{
   menu[0].dismiss();if(n==order.size()+1){changed.run();return;}if(n==order.size())reset(true);else {Column column=order.get(n);if(column!=Column.TITLE){if(shown.contains(column))shown.remove(column);else shown.add(column);}save();}choose(changed,Math.min(n,order.size()));
  });menu[0].setOnCancelListener(d->changed.run());menu[0].setOnKeyListener((d,key,event)->{if(event.getAction()!=KeyEvent.ACTION_DOWN||key!=KeyEvent.KEYCODE_DPAD_LEFT&&key!=KeyEvent.KEYCODE_DPAD_RIGHT)return false;View selected=menu[0].getCurrentFocus();if(selected==null||!(selected.getTag() instanceof Integer))return false;int from=(Integer)selected.getTag(),to=from+(key==KeyEvent.KEYCODE_DPAD_LEFT?-1:1);if(from>=order.size()||to<0||to>=order.size())return true;Collections.swap(order,from,to);save();menu[0].dismiss();choose(changed,to);return true;});
  Window w=menu[0].getWindow();w.setGravity(Gravity.END|Gravity.TOP);WindowManager.LayoutParams attributes=w.getAttributes();attributes.x=dp(20);attributes.y=dp(64);w.setAttributes(attributes);w.setLayout(dp(320),Math.min(dp(60+37*labels.size()),context.getResources().getDisplayMetrics().heightPixels-dp(88)));
 }
 private int dp(int value){return PreviewDialog.dp(context,value);}
}
