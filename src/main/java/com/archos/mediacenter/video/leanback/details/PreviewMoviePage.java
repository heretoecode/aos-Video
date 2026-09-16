package com.archos.mediacenter.video.leanback.details;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.view.*;
import android.widget.*;
import androidx.leanback.widget.*;
import com.archos.mediacenter.video.browser.adapters.object.*;
import com.archos.mediacenter.video.leanback.*;
import com.archos.mediacenter.video.leanback.PreviewLibraryLoader.*;
import com.archos.mediacenter.video.leanback.presenter.PreviewCardPresenter;
import com.archos.mediascraper.*;
import java.util.*;
import java.util.function.*;

/** Presentation over the existing details fragment: playback, edits and deletion keep native handlers. */
public final class PreviewMoviePage extends ScrollView {
    private final LinearLayout body,cast,details,related,trailers;
    private final TextView title,meta,plot,play,trailer;
    private final Supplier<ObjectAdapter> actions;
    private final Consumer<Action> action;
    private final Runnable nativeDetails;
    private final Consumer<Uri> artwork;
    private Movie movie;
    private BaseTags tags;
    private List<ScraperTrailer> trailerList=Collections.emptyList();
    private Snapshot snapshot;
    private final List<Presenter.ViewHolder> cards=new ArrayList<>();
    private final PreviewCardPresenter presenter=new PreviewCardPresenter(PreviewCardPresenter.Style.POSTER);
    public PreviewMoviePage(Context c,Supplier<ObjectAdapter> actions,Consumer<Action> action,Runnable nativeDetails,Consumer<Uri> artwork){
        super(c);this.actions=actions;this.action=action;this.nativeDetails=nativeDetails;this.artwork=artwork;
        setFillViewport(true);setClipToPadding(false);body=new LinearLayout(c);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(28),dp(20),dp(28),dp(24));addView(body);
        LinearLayout hero=new LinearLayout(c);hero.setOrientation(LinearLayout.VERTICAL);hero.setGravity(Gravity.CENTER_VERTICAL);
        body.addView(hero,new LinearLayout.LayoutParams(-1,dp(255)));
        title=text("",34);title.setTypeface(null,android.graphics.Typeface.BOLD);title.setMaxLines(2);hero.addView(title,new LinearLayout.LayoutParams(dp(530),-2));
        meta=text("",13);meta.setPadding(0,dp(10),0,dp(10));hero.addView(meta);
        plot=text("",14);plot.setMaxLines(4);plot.setEllipsize(android.text.TextUtils.TruncateAt.END);hero.addView(plot,new LinearLayout.LayoutParams(dp(480),-2));
        LinearLayout buttons=new LinearLayout(c);buttons.setPadding(0,dp(16),0,0);hero.addView(buttons);
        play=button("▶  Play",this::play);buttons.addView(play);
        trailer=button("Trailer",this::chooseTrailer);trailer.setVisibility(View.GONE);margin(buttons,trailer);
        margin(buttons,button("More  ▾",this::more));
        cast=section("Cast & Crew");details=section("Details");related=section("More Like This — In Your Library");trailers=section("Trailers & Extras");
    }
    public boolean atTop(){return getScrollY()==0;}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private TextView text(String s,int size){TextView t=new TextView(getContext());t.setText(s);t.setTextColor(Color.WHITE);t.setTextSize(size);return t;}
    private GradientDrawable bg(boolean focus){GradientDrawable d=new GradientDrawable();d.setColor(focus?0xee254964:0xcc10283b);d.setCornerRadius(dp(5));d.setStroke(dp(focus?2:1),focus?0xff62bbf3:0xff284b62);return d;}
    private TextView button(String s,Runnable run){TextView t=text(s,14);t.setGravity(Gravity.CENTER);t.setPadding(dp(18),dp(10),dp(18),dp(10));t.setFocusable(true);t.setFocusableInTouchMode(true);t.setBackground(bg(false));t.setOnFocusChangeListener((v,f)->v.setBackground(bg(f)));t.setOnClickListener(v->run.run());return t;}
    private void margin(LinearLayout row,View view){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,-2);lp.leftMargin=dp(12);row.addView(view,lp);}
    private LinearLayout section(String name){LinearLayout section=new LinearLayout(getContext());section.setOrientation(LinearLayout.VERTICAL);section.setPadding(0,dp(14),0,dp(8));TextView label=text(name,19);label.setTextColor(0xff9ed4f7);section.addView(label);LinearLayout content=new LinearLayout(getContext());content.setOrientation(LinearLayout.VERTICAL);content.setPadding(0,dp(10),0,0);section.addView(content);body.addView(section);return content;}
    public void bind(Movie value){movie=value;title.setText(movie.getName());plot.setText(movie.getDescriptionBody());
        String m=(movie.getYear()>0?movie.getYear()+"   ":"")+(movie.getDurationMs()>0?movie.getDurationMs()/60000+" min   ":"")+safe(movie.getContentRating());
        if(movie.hasMeasured4K())m+="   4K";
        if(movie.getCalculatedVideoFormat()!=null)m+="   "+movie.getCalculatedVideoFormat();
        if(movie.getCalculatedBestAudioFormat()!=null)m+="   "+movie.getCalculatedBestAudioFormat();meta.setText(m);
        play.setText(movie.getResumeMs()>0?"▶  Resume":"▶  Play");
        if(movie.getPreviewBackdrop()!=null)artwork.accept(movie.getPreviewBackdrop());renderDetails();
    }
    public void setTags(BaseTags value,List<ScraperTrailer> videos,List<ScraperImage> backdrops){tags=value;trailerList=videos==null?Collections.emptyList():videos;
        if(backdrops!=null&&!backdrops.isEmpty()){ScraperImage image=backdrops.get(0);java.io.File file=image.getLargeFileF();if(file!=null&&file.exists()){artwork.accept(Uri.fromFile(file));if(movie!=null)movie.setPreviewBackdrop(Uri.fromFile(file).toString());}else if(image.getLargeUrl()!=null)artwork.accept(Uri.parse(image.getLargeUrl()));}
        cast.removeAllViews();HorizontalScrollView scroll=new HorizontalScrollView(getContext());LinearLayout people=new LinearLayout(getContext());scroll.addView(people);cast.addView(scroll);
        if(tags!=null){for(Map.Entry<String,String> person:tags.getActors().entrySet()){LinearLayout card=new LinearLayout(getContext());card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(10),dp(15),dp(10),dp(15));card.setBackground(bg(false));card.addView(text(person.getKey(),14));TextView role=text(safe(person.getValue()),12);role.setTextColor(0xffa8cce7);card.addView(role);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(150),dp(85));lp.rightMargin=dp(10);people.addView(card,lp);}
            if(tags.getDirectorsFormatted()!=null)cast.addView(text("Director: "+tags.getDirectorsFormatted(),13));}
        if(people.getChildCount()==0)cast.addView(text("Cast information unavailable",13));
        trailer.setVisibility(trailerList.isEmpty()?View.GONE:View.VISIBLE);
        trailers.removeAllViews();if(trailerList.isEmpty())trailers.addView(text("No trailers available for this title",13));
        else for(ScraperTrailer video:trailerList)trailers.addView(button(video.mName,()->PreviewTrailer.show((Activity)getContext(),video)));
        renderDetails();renderRelated();
    }
    public void setSnapshot(Snapshot value){snapshot=value;renderRelated();}
    private void renderDetails(){if(movie==null)return;details.removeAllViews();
        LinearLayout row=new LinearLayout(getContext());details.addView(row);
        String genre=tags instanceof VideoTags?((VideoTags)tags).getGenresFormatted():"";
        String release=tags instanceof MovieTags?((MovieTags)tags).getReleaseDate():"";
        String studio=tags instanceof VideoTags?((VideoTags)tags).getStudiosFormatted():"";
        info(row,"Genre",safe(genre)+"\n\nRelease date\n"+safe(release));
        info(row,"Studio",safe(studio)+"\n\nRating\n"+(movie.getRating()>0?String.format(Locale.UK,"TMDb  %.1f / 10",movie.getRating()):"Not available"));
        info(row,"Source",source(movie.getFileUri())+"\n\nVideo\n"+safe(movie.getCalculatedVideoFormat())+"\nAudio\n"+safe(movie.getCalculatedBestAudioFormat()));
        details.addView(button("File, subtitles and artwork details",nativeDetails));
    }
    public static String source(Uri uri){if(uri==null)return "Unknown";String scheme=uri.getScheme();
        if(scheme==null||scheme.equals("file")||scheme.equals("content"))return "Local storage";
        if(scheme.equals("smb"))return "NAS / SMB";if(scheme.equals("webdav")||scheme.equals("webdavs")||scheme.equals("dav")||scheme.equals("davs"))return "WebDAV";
        if(scheme.equals("http")||scheme.equals("https"))return "Network / HTTP(S)";
        return "Network / "+scheme.toUpperCase(Locale.ROOT);
    }
    private static String safe(String s){return s==null||s.isEmpty()?"—":s;}
    private void info(LinearLayout row,String name,String value){TextView t=text(name+"\n\n"+value,13);t.setPadding(dp(14),dp(14),dp(14),dp(14));t.setBackground(bg(false));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.rightMargin=dp(10);row.addView(t,lp);}
    private void renderRelated(){for(Presenter.ViewHolder h:cards)presenter.onUnbindViewHolder(h);cards.clear();related.removeAllViews();if(movie==null||snapshot==null||tags==null)return;
        Entry seed=new Entry(movie,0,0,tags instanceof VideoTags?((VideoTags)tags).getGenresFormatted():"");
        HorizontalScrollView scroll=new HorizontalScrollView(getContext());LinearLayout row=new LinearLayout(getContext());scroll.addView(row);related.addView(scroll);
        int count=0;for(Entry e:PreviewDiscovery.similar(seed,snapshot)){if(!(e.media instanceof Movie))continue;if(count++==12)break;
            Presenter.ViewHolder h=presenter.onCreateViewHolder(row);presenter.onBindViewHolder(h,e.media);cards.add(h);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(135),-2);lp.rightMargin=dp(12);row.addView(h.view,lp);h.view.setOnClickListener(v->new VideoViewClickedListener((Activity)getContext()).onItemClicked(h,e.media,null,null));}
        if(count==0)related.addView(text("No similar titles in your library",13));
    }
    public void play(){ObjectAdapter adapter=actions.get();if(adapter==null)return;
        for(int id:new int[]{VideoActionAdapter.ACTION_RESUME,VideoActionAdapter.ACTION_LOCAL_RESUME,VideoActionAdapter.ACTION_PLAY,VideoActionAdapter.ACTION_PLAY_FROM_BEGIN,VideoActionAdapter.ACTION_REMOTE_RESUME})
            for(int i=0;i<adapter.size();i++){Object item=adapter.get(i);if(item instanceof Action&&((Action)item).getId()==id){action.accept((Action)item);return;}}
    }
    private void more(){ObjectAdapter adapter=actions.get();if(adapter==null)return;List<Action> items=new ArrayList<>();List<String> labels=new ArrayList<>();
        for(int i=0;i<adapter.size();i++){Object value=adapter.get(i);if(value instanceof Action){Action a=(Action)value;items.add(a);labels.add(String.valueOf(a.getLabel1())+(a.getLabel2()==null?"":" — "+a.getLabel2()));}}
        labels.add("File, subtitles and artwork details");new AlertDialog.Builder(getContext()).setTitle("More").setItems(labels.toArray(new String[0]),(d,n)->{if(n==items.size())nativeDetails.run();else action.accept(items.get(n));}).setNegativeButton("Cancel",null).show();
    }
    private void chooseTrailer(){if(trailerList.isEmpty())return;String[] names=new String[trailerList.size()];for(int i=0;i<names.length;i++)names[i]=trailerList.get(i).mName;
        if(names.length==1)PreviewTrailer.show((Activity)getContext(),trailerList.get(0));else new AlertDialog.Builder(getContext()).setTitle("Trailers").setItems(names,(d,n)->PreviewTrailer.show((Activity)getContext(),trailerList.get(n))).show();}
    @Override protected void onDetachedFromWindow(){for(Presenter.ViewHolder h:cards)presenter.onUnbindViewHolder(h);cards.clear();super.onDetachedFromWindow();}
}
