package com.archos.mediacenter.video.leanback.details;
import android.content.Context;
import android.widget.ImageView;
import com.archos.mediascraper.*;
import com.archos.mediascraper.xml.MovieScraper3;
import com.uwetrottmann.tmdb2.entities.*;
import java.util.*;
import java.util.concurrent.*;
import com.squareup.picasso.Picasso;
final class PreviewPeople {
 private static final ExecutorService worker=Executors.newSingleThreadExecutor();
 static void load(Context context,BaseTags tags,Map<String,ImageView> targets){
  if(tags==null||targets.isEmpty())return;BaseTags source=tags instanceof EpisodeTags?((EpisodeTags)tags).getShowTags():tags;if(source==null||source.getOnlineId()<=0)return;
  String key=(source instanceof MovieTags?"movie:":"tv:")+source.getOnlineId();android.content.SharedPreferences cache=context.getSharedPreferences("preview_people",0);
  for(Map.Entry<String,ImageView> e:targets.entrySet()){String path=cache.getString(key+":"+e.getKey(),null);if(path!=null)image(e.getValue(),path);}
  if(System.currentTimeMillis()-cache.getLong(key+":updated",0)<7L*86400000)return;
  worker.execute(()->{try{
   Credits credits=source instanceof MovieTags?MovieScraper3.getTmdb().moviesService().credits((int)source.getOnlineId()).execute().body():MovieScraper3.getTmdb().tvService().credits((int)source.getOnlineId(),null).execute().body();
   if(credits==null)return;Map<String,String> paths=new HashMap<>();if(credits.cast!=null)for(CastMember c:credits.cast)if(c.profile_path!=null)paths.put(c.name,c.profile_path);if(credits.crew!=null)for(CrewMember c:credits.crew)if(c.profile_path!=null)paths.put(c.name,c.profile_path);
   android.content.SharedPreferences.Editor save=cache.edit();for(Map.Entry<String,String> e:paths.entrySet())save.putString(key+":"+e.getKey(),e.getValue());save.putLong(key+":updated",System.currentTimeMillis()).apply();
   for(Map.Entry<String,ImageView> e:targets.entrySet()){String path=paths.get(e.getKey());if(path!=null)e.getValue().post(()->{if(e.getValue().isAttachedToWindow())image(e.getValue(),path);});}
  }catch(Exception unavailable){/* Keep cached portrait or neutral silhouette. */}});
 }
 private static void image(ImageView view,String path){if(!path.matches("/[A-Za-z0-9._-]+"))return;Picasso.get().load("https://image.tmdb.org/t/p/w185"+path).fit().centerCrop().placeholder(com.archos.mediacenter.video.R.drawable.preview_person).into(view);}
}
