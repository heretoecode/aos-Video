package com.archos.mediacenter.video.leanback.search;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.os.*;
import android.text.*;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import androidx.recyclerview.widget.*;
import androidx.leanback.widget.Presenter;
import com.archos.mediacenter.video.browser.adapters.object.*;
import com.archos.mediacenter.video.browser.adapters.mappers.VideoCursorMapper;
import com.archos.mediacenter.video.browser.loader.*;
import com.archos.mediacenter.video.leanback.*;
import com.archos.mediaprovider.video.VideoStore;
import com.squareup.picasso.Picasso;
import java.util.*;
import java.util.concurrent.*;

/** Local Nova search, with recycled compact rows and no remote discovery result universe. */
public final class PreviewSearch extends LinearLayout {
 private final Activity activity;
 private final EditText query;
 private final TextView status;
 private final PreviewFocusRecycler results;
 private final LinearLayoutManager layout;
 private final ResultAdapter adapter=new ResultAdapter();
 private final List<Result> items=new ArrayList<>();
 private final Handler main=new Handler(Looper.getMainLooper());
 private final ExecutorService worker=Executors.newSingleThreadExecutor();
 private CancellationSignal cancellation;
 private int generation;
 private long selectedId=-1;
 private boolean restoring;
 private Parcelable scrollState;
 private Runnable pending;
 private final int mode;
 private static class Result {Video video;String genres;Result(Video video,String genres){this.video=video;this.genres=genres;}}
 public PreviewSearch(Activity activity,int mode,Bundle state){
  super(activity);this.activity=activity;this.mode=mode;setOrientation(VERTICAL);setPadding(dp(28),dp(12),dp(28),dp(12));setBackground(com.archos.mediacenter.video.leanback.PreviewAccent.utility(getContext()));
  status=text("Search your library",12);status.setGravity(Gravity.END);addView(status,new LayoutParams(-1,dp(22)));
  LinearLayout search=new LinearLayout(activity);search.setGravity(Gravity.CENTER_VERTICAL);search.setPadding(dp(12),0,dp(8),0);search.setBackground(PreviewDialog.surface(activity,false));ImageView icon=new ImageView(activity);icon.setImageResource(com.archos.mediacenter.video.R.drawable.preview_search);search.addView(icon,new LayoutParams(dp(20),dp(20)));
  query=new EditText(activity);query.setHint("Search Movies and TV Shows");query.setTextColor(-1);query.setHintTextColor(0xff8da7b9);query.setTextSize(16);query.setSingleLine(true);query.setBackgroundColor(android.graphics.Color.TRANSPARENT);query.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH | android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI);query.setPadding(dp(12),0,dp(12),0);search.addView(query,new LayoutParams(0,dp(46),1));TextView clear=text("×",24);clear.setContentDescription("Clear search");clear.setGravity(Gravity.CENTER);clear.setFocusable(true);clear.setBackground(PreviewDialog.focus(activity));clear.setOnClickListener(v->{query.setText("");query.requestFocus();});search.addView(clear,new LayoutParams(dp(34),dp(34)));addView(search);
  results=new PreviewFocusRecycler(activity);layout=new LinearLayoutManager(activity);results.setLayoutManager(layout);results.setAdapter(adapter);results.setPadding(0,dp(12),0,0);results.setClipToPadding(false);addView(results,new LayoutParams(-1,0,1));
  query.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){schedule(s.toString());}public void afterTextChanged(Editable e){}});
  query.setOnEditorActionListener((v,id,event)->{hideKeyboard();if(!items.isEmpty())results.requestFocus();return true;});
  if(state!=null){selectedId=state.getLong("preview_search_selected",-1);scrollState=state.getParcelable("preview_search_scroll");restoring=selectedId>=0;query.setText(state.getString("preview_search_query",""));}else query.requestFocus();
 }
 public boolean atTop(){return query.hasFocus();}
 public void focusQuery(){query.requestFocus();}
 public void acceptVoice(String text){query.setText(text);query.setSelection(query.length());}
 public void save(Bundle state){state.putString("preview_search_query",query.getText().toString());state.putLong("preview_search_selected",selectedId);state.putParcelable("preview_search_scroll",layout.onSaveInstanceState());}
 private void hideKeyboard(){InputMethodManager ime=(InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE);if(ime!=null)ime.hideSoftInputFromWindow(query.getWindowToken(),0);}
 private void schedule(String value){if(pending!=null)main.removeCallbacks(pending);if(cancellation!=null)cancellation.cancel();int request=++generation;String search=value.trim();if(search.isEmpty()){items.clear();adapter.notifyDataSetChanged();status.setText("Search your library");return;}pending=()->load(search,request);main.postDelayed(pending,250);}
 private void load(String value,int request){
  VideoLoader loader;switch(mode){case VideoSearchActivity.SEARCH_MODE_MOVIE:SearchMovieLoader movie=new SearchMovieLoader(activity);movie.setQuery("");loader=movie;break;case VideoSearchActivity.SEARCH_MODE_EPISODE:SearchEpisodeLoader ep=new SearchEpisodeLoader(activity);ep.setQuery("");loader=ep;break;case VideoSearchActivity.SEARCH_MODE_NON_SCRAPED:SearchNonScrapedVideoLoader file=new SearchNonScrapedVideoLoader(activity);file.setQuery("");loader=file;break;default:SearchVideoLoader all=new SearchVideoLoader(activity);all.setQuery("");loader=all;}
  android.net.Uri uri=loader.getUri();String[] projection=Arrays.copyOf(loader.getProjection(),loader.getProjection().length+2);projection[projection.length-2]=VideoStore.Video.VideoColumns.SCRAPER_M_GENRES;projection[projection.length-1]=VideoStore.Video.VideoColumns.SCRAPER_S_GENRES;String selection=loader.getSelection(),sort=loader.getSortOrder();String[] args=loader.getSelectionArgs();CancellationSignal signal=new CancellationSignal();cancellation=signal;status.setText("Searching library…");Context app=activity.getApplicationContext();
  worker.execute(()->{List<Result> found=new ArrayList<>();String error=null;try(Cursor cursor=app.getContentResolver().query(uri,projection,selection,args,sort,signal)){if(cursor!=null){VideoCursorMapper mapper=new VideoCursorMapper();mapper.bindColumns(cursor);while(cursor.moveToNext()){signal.throwIfCanceled();String name=cursor.getString(cursor.getColumnIndexOrThrow(VideoLoader.COLUMN_NAME));String episode=cursor.getString(cursor.getColumnIndexOrThrow(VideoStore.Video.VideoColumns.SCRAPER_E_NAME));String path=cursor.getString(cursor.getColumnIndexOrThrow(VideoStore.Video.VideoColumns.DATA));if(!PreviewSearchText.matches(value,name,episode,path))continue;Video video=(Video)mapper.bind(cursor);String genres=cursor.getString(cursor.getColumnIndexOrThrow(video instanceof Episode?VideoStore.Video.VideoColumns.SCRAPER_S_GENRES:VideoStore.Video.VideoColumns.SCRAPER_M_GENRES));found.add(new Result(video,genres));}}}catch(OperationCanceledException cancelled){return;}catch(RuntimeException failure){android.util.Log.w("NovaPreview","Local search unavailable",failure);error="Library search is temporarily unavailable";}String message=error;main.post(()->{if(request!=generation||!isAttachedToWindow())return;boolean focusResults=results.hasFocus();int position=0;if(selectedId>=0)for(int i=0;i<found.size();i++)if(found.get(i).video.getId()==selectedId){position=i;break;}items.clear();items.addAll(found);adapter.notifyDataSetChanged();status.setText(message!=null?message:found.isEmpty()?"No matching library titles":found.size()+" results · In your library");if(scrollState!=null){layout.onRestoreInstanceState(scrollState);scrollState=null;}if((restoring||focusResults)&&!items.isEmpty()){final int target=position;results.scrollToPosition(target);results.post(()->{RecyclerView.ViewHolder h=results.findViewHolderForAdapterPosition(target);if(h!=null)h.itemView.requestFocus();});}restoring=false;});});
 }
 @Override public boolean dispatchKeyEvent(KeyEvent e){if(e.getAction()==KeyEvent.ACTION_DOWN&&e.getKeyCode()==KeyEvent.KEYCODE_DPAD_UP&&results.hasFocus()){View focus=results.findFocus(),row=focus==null?null:results.findContainingItemView(focus);if(row!=null&&results.getChildAdapterPosition(row)==0){query.requestFocus();return true;}}return super.dispatchKeyEvent(e);}
 @Override protected void onDetachedFromWindow(){main.removeCallbacksAndMessages(null);if(cancellation!=null)cancellation.cancel();worker.shutdownNow();super.onDetachedFromWindow();}
 private int dp(int n){return PreviewDialog.dp(activity,n);}
 private TextView text(String s,int size){TextView t=new TextView(activity);t.setText(s);t.setTextSize(size);t.setTextColor(0xffc0d3df);t.setSingleLine(true);t.setEllipsize(TextUtils.TruncateAt.END);return t;}
 private class Holder extends RecyclerView.ViewHolder {ImageView image;TextView title,metadata,genres;Holder(LinearLayout row){super(row);image=new ImageView(activity);image.setScaleType(ImageView.ScaleType.FIT_CENTER);row.addView(image,new LayoutParams(dp(92),dp(65)));LinearLayout labels=new LinearLayout(activity);labels.setOrientation(VERTICAL);labels.setPadding(dp(12),0,dp(12),0);title=text("",16);title.setTextColor(-1);metadata=text("",12);genres=text("",12);labels.addView(title);labels.addView(metadata);labels.addView(genres);row.addView(labels,new LayoutParams(0,-2,1));TextView arrow=text("›",26);row.addView(arrow,new LayoutParams(dp(25),-2));}}
 private class ResultAdapter extends RecyclerView.Adapter<Holder>{
  public int getItemCount(){return items.size();}
  public Holder onCreateViewHolder(ViewGroup parent,int type){LinearLayout row=new LinearLayout(activity);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(6),dp(5),dp(6),dp(5));row.setFocusable(true);row.setFocusableInTouchMode(true);row.setDescendantFocusability(FOCUS_BLOCK_DESCENDANTS);row.setBackground(PreviewDialog.surface(activity,false));row.setForeground(PreviewDialog.focus(activity));RecyclerView.LayoutParams lp=new RecyclerView.LayoutParams(-1,dp(78));lp.bottomMargin=dp(8);row.setLayoutParams(lp);return new Holder(row);}
  public void onBindViewHolder(Holder h,int position){Result r=items.get(position);Video video=r.video;h.title.setText(video instanceof Episode?((Episode)video).getShowName()+" · "+((Episode)video).getEpisodeName():video.getName());List<String> meta=new ArrayList<>();if(video instanceof Movie){Movie m=(Movie)video;if(m.getYear()>0)meta.add(String.valueOf(m.getYear()));if(m.getContentRating()!=null&&!m.getContentRating().isEmpty())meta.add(m.getContentRating());}else if(video instanceof Episode){Episode e=(Episode)video;meta.add("S"+e.getSeasonNumber()+" E"+e.getEpisodeNumber());}if(video.getDurationMs()>0)meta.add(video.getDurationMs()/60000+" min");h.metadata.setText(TextUtils.join("  ·  ",meta));h.genres.setText(r.genres==null?"":r.genres);h.genres.setVisibility(TextUtils.isEmpty(r.genres)?GONE:VISIBLE);Picasso.get().cancelRequest(h.image);h.image.setImageDrawable(null);android.net.Uri art=video instanceof Episode?((Episode)video).getPictureUri():video.getPosterUri();if(art!=null)Picasso.get().load(art).resize(dp(92),dp(65)).centerInside().noFade().into(h.image);h.itemView.setOnFocusChangeListener((v,focused)->{if(focused)selectedId=video.getId();});h.itemView.setOnClickListener(v->{selectedId=video.getId();hideKeyboard();VideoViewClickedListener.showVideoDetails(activity,video,new Presenter.ViewHolder(v),false,-1);});}
  public void onViewRecycled(Holder h){Picasso.get().cancelRequest(h.image);h.image.setImageDrawable(null);}
 }
}
