package com.archos.mediacenter.video.leanback.details;
import android.content.Context;
import com.archos.mediascraper.*;
import com.archos.mediascraper.xml.MovieScraper3;
import com.uwetrottmann.tmdb2.entities.Videos;
import org.json.*;
import java.util.*;
/** Reuses Nova's TMDb metadata client. Never blocks the initial Details render. */
public final class PreviewTvTrailers {
 public static List<ScraperTrailer> load(Context context,ShowTags show){
  List<ScraperTrailer> result=new ArrayList<>();if(show==null||show.getOnlineId()<=0)return result;
  android.content.SharedPreferences cache=context.getSharedPreferences("preview_tv_trailers",0);String key=String.valueOf(show.getOnlineId());
  try{JSONArray rows=new JSONArray(cache.getString(key,"[]"));for(int i=0;i<rows.length();i++){JSONObject o=rows.getJSONObject(i);result.add(new ScraperTrailer(ScraperTrailer.Type.SHOW_TRAILER,o.getString("name"),o.getString("key"),"YouTube",o.optString("lang")));}}catch(JSONException ignored){}
  if(System.currentTimeMillis()-cache.getLong(key+":updated",0)<7L*86400000)return result;
  try{Videos videos=MovieScraper3.getTmdb().tvService().videos((int)show.getOnlineId(),null).execute().body();if(videos==null||videos.results==null)return result;JSONArray rows=new JSONArray();List<ScraperTrailer> fresh=new ArrayList<>();for(Videos.Video v:videos.results){if(!"YouTube".equals(v.site)||v.type!=com.uwetrottmann.tmdb2.enumerations.VideoType.TRAILER||v.key==null||!v.key.matches("[A-Za-z0-9_-]{11}"))continue;String name=v.name==null?"Trailer":v.name;fresh.add(new ScraperTrailer(ScraperTrailer.Type.SHOW_TRAILER,name.contains("Trailer")||name.contains("trailer")?name:name+" · Trailer",v.key,v.site,v.iso_639_1));rows.put(new JSONObject().put("name",fresh.get(fresh.size()-1).mName).put("key",v.key).put("lang",v.iso_639_1));}cache.edit().putString(key,rows.toString()).putLong(key+":updated",System.currentTimeMillis()).apply();return fresh;}catch(Exception unavailable){return result;}
 }
}
