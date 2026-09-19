package com.archos.mediacenter.video.leanback;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.widget.TextView;
import com.archos.mediacenter.video.browser.adapters.object.*;
import com.archos.mediacenter.video.browser.adapters.object.Movie;
import com.archos.mediacenter.utils.videodb.VideoDbInfo;
import com.archos.mediascraper.*;
import java.io.*;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.*;
import okhttp3.*;
import org.json.*;

/** Genuine TMDb title logos. All lookup/decode is off the UI thread; playback only reads cache.
 * Text remains the accessibility/fallback title and fixes layout geometry even when artwork loads. */
public final class OfficialTitleArtwork {
 private static final ExecutorService IO=Executors.newFixedThreadPool(2);
 private static final OkHttpClient HTTP=new OkHttpClient.Builder().connectTimeout(5,TimeUnit.SECONDS).readTimeout(7,TimeUnit.SECONDS).callTimeout(10,TimeUnit.SECONDS).build();
 private static final Map<TextView,String> BOUND=new WeakHashMap<>();
 private static final android.util.LruCache<String,Bitmap> MEMORY=new android.util.LruCache<>(12);
 public static void bind(TextView view,Base media,boolean cachedOnly){
  if(media==null){clear(view);return;}String identity=media instanceof Tvshow?"show-local:"+((Tvshow)media).getTvshowId():media instanceof Video?"video-local:"+((Video)media).getId():media.getName();
  request(view,identity,cachedOnly,()->{BaseTags tags=media.getFullScraperTags(view.getContext().getApplicationContext());if(tags instanceof EpisodeTags)tags=((EpisodeTags)tags).getShowTags();return tags==null||tags.getOnlineId()<=0?null:(media instanceof Movie?"movie/":"tv/")+tags.getOnlineId();});
 }
 public static void bind(TextView view,VideoDbInfo info){
  String id=info==null?null:info.isShow?info.scraperShowId:info.scraperMovieId;
  if(id==null||!id.matches("[1-9][0-9]*")){clear(view);return;}String key=(info.isShow?"tv/":"movie/")+id;request(view,key,true,()->key);
 }
 private static void clear(TextView view){BOUND.remove(view);view.setForeground(null);view.setTextColor(0xffffffff);}
 private static void request(TextView view,String identity,boolean cachedOnly,Callable<String> resolve){
  if(identity.equals(BOUND.get(view)))return;clear(view);BOUND.put(view,identity);
  Context app=view.getContext().getApplicationContext();WeakReference<TextView> weak=new WeakReference<>(view);
  IO.execute(()->{try{
   String key=resolve.call();if(key==null)return;String language=Locale.getDefault().getLanguage();String diskKey=key.replace('/','_')+"_"+language;
   Bitmap bitmap=MEMORY.get(diskKey);File directory=new File(app.getCacheDir(),"official-title-artwork"),file=new File(directory,diskKey+".png");
   if(bitmap==null&&file.isFile())bitmap=BitmapFactory.decodeFile(file.getPath());
   if(bitmap==null&&!cachedOnly){android.content.SharedPreferences cache=app.getSharedPreferences("preview_title_logos",0);long last=cache.getLong(diskKey,0);
    if(System.currentTimeMillis()-last<86400000L)return;
    android.net.Uri uri=android.net.Uri.parse("https://api.themoviedb.org/3/"+key+"/images").buildUpon().appendQueryParameter("api_key",app.getString(com.archos.medialib.R.string.tmdb_api_key)).appendQueryParameter("include_image_language",language+",en,null").build();
    JSONObject result=new JSONObject(new String(download(uri.toString(),2*1024*1024),java.nio.charset.StandardCharsets.UTF_8));String path=select(result.optJSONArray("logos"),language);cache.edit().putLong(diskKey,System.currentTimeMillis()).apply();
    if(!path.isEmpty()){byte[] bytes=download("https://image.tmdb.org/t/p/w500"+path,4*1024*1024);BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);if(options.outWidth<=0||options.outHeight<=0||options.outWidth>4096||options.outHeight>4096)return;bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.length);if(bitmap!=null){directory.mkdirs();android.util.AtomicFile atomic=new android.util.AtomicFile(file);FileOutputStream out=null;try{out=atomic.startWrite();out.write(bytes);atomic.finishWrite(out);}catch(IOException e){atomic.failWrite(out);}}}
   }
   if(bitmap==null)return;MEMORY.put(diskKey,bitmap);final Bitmap ready=bitmap;TextView target=weak.get();if(target!=null)target.post(()->{TextView current=weak.get();if(current==null||!identity.equals(BOUND.get(current)))return;current.setForeground(new Logo(ready));current.setTextColor(Color.TRANSPARENT);});
  }catch(Exception unavailable){/* A missing logo never blocks content or playback. */}});
 }
 static String select(JSONArray logos,String language)throws JSONException{String chosen="";double best=-1;if(logos==null)return chosen;for(int i=0;i<logos.length();i++){JSONObject logo=logos.getJSONObject(i);String path=logo.optString("file_path"),lang=logo.optString("iso_639_1");if(!path.matches("/[A-Za-z0-9._-]+\\.png"))continue;double score=(language.equals(lang)?30:"en".equals(lang)?20:lang.isEmpty()||"null".equals(lang)?10:0)+Math.min(9,logo.optDouble("vote_average",0));if(score>best){best=score;chosen=path;}}return chosen;}
 private static byte[] download(String url,int limit)throws IOException{try(Response response=HTTP.newCall(new Request.Builder().url(url).build()).execute()){if(!response.isSuccessful()||response.body()==null)throw new IOException("Artwork unavailable");try(InputStream in=response.body().byteStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(out.size()+n>limit)throw new IOException("Artwork too large");out.write(b,0,n);}return out.toByteArray();}}}
 private static final class Logo extends Drawable{private final Bitmap bitmap;private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);Logo(Bitmap bitmap){this.bitmap=bitmap;}public void draw(Canvas canvas){Rect b=getBounds();float scale=Math.min(b.width()/(float)bitmap.getWidth(),b.height()/(float)bitmap.getHeight());float width=bitmap.getWidth()*scale,height=bitmap.getHeight()*scale;canvas.drawBitmap(bitmap,null,new RectF(b.left,b.top+(b.height()-height)/2f,b.left+width,b.top+(b.height()+height)/2f),paint);}public void setAlpha(int alpha){paint.setAlpha(alpha);}public void setColorFilter(ColorFilter filter){paint.setColorFilter(filter);}public int getOpacity(){return PixelFormat.TRANSLUCENT;}}
 private OfficialTitleArtwork(){}
}
