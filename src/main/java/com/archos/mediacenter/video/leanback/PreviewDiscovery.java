package com.archos.mediacenter.video.leanback;

import android.content.Context;
import org.json.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import okhttp3.*;
import com.archos.mediacenter.video.leanback.PreviewLibraryLoader.*;
import com.archos.mediacenter.video.browser.adapters.object.*;

/** Public Trakt charts only: never uploads library contents or touches account tokens. */
public final class PreviewDiscovery {
    private static final long TTL=6*60*60*1000L;
    public final Map<String,Integer> trending=new HashMap<>(),popular=new HashMap<>();
    public boolean available;
    public static String key(Entry e){return (e.media instanceof Movie?"movie:":"show:")+e.onlineId;}
    public static void parse(JSONArray array,String kind,Map<String,Integer> ranks,int offset) throws JSONException {
        for(int i=0;i<array.length();i++){
            JSONObject item=array.getJSONObject(i);JSONObject title=item.optJSONObject(kind);if(title==null)title=item;
            JSONObject ids=title.optJSONObject("ids");long id=ids==null?0:ids.optLong("tmdb",0);
            if(id>0)ranks.putIfAbsent(kind+":"+id,offset+i);
        }
    }
    public static PreviewDiscovery load(Context context) {
        PreviewDiscovery out=new PreviewDiscovery();
        android.content.SharedPreferences cache=context.getSharedPreferences("preview_discovery",Context.MODE_PRIVATE);
        String raw=cache.getString("charts",null);
        if(raw==null||System.currentTimeMillis()-cache.getLong("time",0)>TTL){
            try{
                int resource=context.getResources().getIdentifier("trakt_api_key","string",context.getPackageName());
                if(resource==0)return out;
                String key=context.getString(resource);if(key.trim().isEmpty())return out;
                OkHttpClient client=new OkHttpClient.Builder().connectTimeout(5,TimeUnit.SECONDS).readTimeout(8,TimeUnit.SECONDS).callTimeout(12,TimeUnit.SECONDS).build();
                JSONObject charts=new JSONObject();
                for(String kind:new String[]{"movie","show"})for(String chart:new String[]{"trending","popular"}){
                    JSONArray all=new JSONArray();
                    for(int page=1;page<=2;page++){
                        if(Thread.currentThread().isInterrupted())return out;
                        Request request=new Request.Builder().url("https://api.trakt.tv/"+kind+"s/"+chart+"?page="+page+"&limit=100").header("trakt-api-version","2").header("trakt-api-key",key).build();
                        try(Response response=client.newCall(request).execute()){
                            if(!response.isSuccessful()||response.body()==null)throw new java.io.IOException("Chart unavailable");
                            String body=response.body().string();if(body.length()>2000000)throw new java.io.IOException("Chart too large");
                            JSONArray a=new JSONArray(body);for(int i=0;i<a.length();i++)all.put(a.get(i));if(a.length()<100)break;
                        }
                    }
                    charts.put(kind+chart,all);
                }
                raw=charts.toString();cache.edit().putString("charts",raw).putLong("time",System.currentTimeMillis()).apply();
            }catch(Exception ignored){ /* Last successful charts remain usable offline. */ }
        }
        if(raw!=null)try{
            JSONObject charts=new JSONObject(raw);
            for(String kind:new String[]{"movie","show"}){
                parse(charts.getJSONArray(kind+"trending"),kind,out.trending,0);
                parse(charts.getJSONArray(kind+"popular"),kind,out.popular,0);
            }
            out.available=true;
        }catch(JSONException ignored){}
        return out;
    }
    public List<Entry> matches(Snapshot snapshot,boolean trend){
        Map<String,Integer> ranks=trend?trending:popular;List<Entry> result=new ArrayList<>();
        Set<String> seen=new HashSet<>();
        for(List<Entry> entries:Arrays.asList(snapshot.movies,snapshot.shows))for(Entry e:entries)
            if(e.onlineId>0&&ranks.containsKey(key(e))&&seen.add(key(e)))result.add(e);
        result.sort(Comparator.comparingInt(e->ranks.get(key(e))));return result;
    }
    public static List<Entry> similar(Entry selected,Snapshot snapshot){
        List<Entry> result=new ArrayList<>();if(selected==null||selected.genres.isEmpty())return result;
        Set<String> genres=new HashSet<>();
        for(String genre:selected.genres.split("[|,;/]")){String normal=genre.trim().toLowerCase(Locale.ROOT);if(!normal.isEmpty())genres.add(normal);}
        for(List<Entry> entries:Arrays.asList(snapshot.movies,snapshot.shows))for(Entry e:entries){
            if(e.key().equals(selected.key()))continue;
            if(Arrays.stream(e.genres.split("[|,;/]")).map(g->g.trim().toLowerCase(Locale.ROOT)).anyMatch(genres::contains))result.add(e);
        }
        result.sort(Comparator.comparingLong((Entry e)->e.added).reversed());return result;
    }
}
