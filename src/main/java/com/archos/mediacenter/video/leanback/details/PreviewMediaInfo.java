package com.archos.mediacenter.video.leanback.details;
import org.json.*;
import java.util.*;
public final class PreviewMediaInfo {
 public static String format(String value){
  if(value==null||value.trim().isEmpty())return "";String raw=value.trim();
  if(raw.startsWith("{")){try{JSONObject o=new JSONObject(raw);JSONArray tracks=o.optJSONArray("audiotracks");LinkedHashSet<String> labels=new LinkedHashSet<>();if(tracks!=null){for(int i=0;i<tracks.length();i++){JSONObject t=tracks.optJSONObject(i);if(t==null)continue;String codec=plain(t.optString("format","")),channels=plain(t.optString("channels",""));if(!codec.isEmpty())labels.add(codec+(channels.isEmpty()?"":" · "+channels));}}else {String codec=plain(o.optString("format",o.optString("codec","")));if(!codec.isEmpty())labels.add(codec);}return android.text.TextUtils.join(" · ",labels);}catch(JSONException ignored){return "";}}
  return plain(raw);
 }
 private static String plain(String s){if(s==null||s.length()>70||s.matches(".*[{}\\[\\]\"=:].*")||s.equalsIgnoreCase("unknown")||s.equalsIgnoreCase("null"))return "";return s;}
}
