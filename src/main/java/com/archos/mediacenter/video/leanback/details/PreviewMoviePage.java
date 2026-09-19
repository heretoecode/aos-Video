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
    private final TextView title,meta,plot,play,trailer,context;
    private final LinearLayout pills;
    private final ImageView poster;
    private final LinearLayout providerActions;
    private ObjectAdapter observedActions;
    private final com.archos.mediacenter.video.streaming.StreamingActionPresenter providerPresenter=new com.archos.mediacenter.video.streaming.StreamingActionPresenter();
    private final List<Presenter.ViewHolder> providerHolders=new ArrayList<>();
    private final ObjectAdapter.DataObserver actionObserver=new ObjectAdapter.DataObserver(){public void onChanged(){renderProviders();}public void onItemRangeChanged(int start,int count){renderProviders();}public void onItemRangeInserted(int start,int count){renderProviders();}public void onItemRangeRemoved(int start,int count){renderProviders();}};
    private final Supplier<ObjectAdapter> actions;
    private final Consumer<Action> action;
    private final Runnable nativeDetails;
    private final Consumer<Uri> artwork;
    private Video movie;
    private Tvshow show;
    private Runnable showPlay;
    private final java.util.SortedMap<Integer,java.util.List<Episode>> seasons=new java.util.TreeMap<>();
    private LinearLayout episodes;
    private BaseTags tags;
    private final Map<String,ImageView> portraits=new HashMap<>();
    private List<ScraperTrailer> trailerList=Collections.emptyList();
    private Snapshot snapshot;
    private final List<Presenter.ViewHolder> cards=new ArrayList<>();
    private final PreviewCardPresenter presenter=new PreviewCardPresenter(PreviewCardPresenter.Style.CONTINUE);
    public PreviewMoviePage(Context c,Supplier<ObjectAdapter> actions,Consumer<Action> action,Runnable nativeDetails,Consumer<Uri> artwork){
        super(c);this.actions=actions;this.action=action;this.nativeDetails=nativeDetails;this.artwork=artwork;
        setFillViewport(true);setSmoothScrollingEnabled(false);setClipToPadding(false);body=new LinearLayout(c);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(28),dp(20),dp(28),dp(24));addView(body);
        LinearLayout composition=new LinearLayout(c);composition.setGravity(Gravity.TOP);body.addView(composition,new LinearLayout.LayoutParams(-1,-2));
        poster=new ImageView(c);poster.setScaleType(ImageView.ScaleType.FIT_CENTER);poster.setBackgroundColor(0x40071622);
        LinearLayout.LayoutParams posterParams=new LinearLayout.LayoutParams(dp(142),dp(213));posterParams.rightMargin=dp(20);composition.addView(poster,posterParams);
        LinearLayout hero=new LinearLayout(c);hero.setOrientation(LinearLayout.VERTICAL);hero.setGravity(Gravity.TOP);composition.addView(hero,new LinearLayout.LayoutParams(0,-2,1));
        title=text("",27);title.setTypeface(null,android.graphics.Typeface.BOLD);title.setMaxLines(2);title.setEllipsize(android.text.TextUtils.TruncateAt.END);hero.addView(title,new LinearLayout.LayoutParams(-1,-2));
        meta=text("",12);meta.setPadding(0,dp(5),0,dp(4));hero.addView(meta);
        context=text("",12);context.setTextColor(0xffb3c8d7);context.setMaxLines(2);context.setEllipsize(android.text.TextUtils.TruncateAt.END);hero.addView(context,new LinearLayout.LayoutParams(-1,-2));
        pills=new LinearLayout(c);pills.setPadding(0,dp(5),0,dp(5));hero.addView(pills);
        plot=text("",13);plot.setMaxLines(3);plot.setEllipsize(android.text.TextUtils.TruncateAt.END);hero.addView(plot,new LinearLayout.LayoutParams(-1,dp(48)));
        LinearLayout buttons=new LinearLayout(c);buttons.setPadding(0,dp(12),0,dp(10));hero.addView(buttons);
        play=button("Play",this::play);buttons.addView(play);providerActions=new LinearLayout(c);buttons.addView(providerActions);
        trailer=button("Trailer",this::chooseTrailer);trailer.setVisibility(View.GONE);margin(buttons,trailer);
        margin(buttons,button("Information",this::moreInfo));margin(buttons,button("Actions",this::more));
        episodes=section("Seasons & Episodes");episodes.getParent();((View)episodes.getParent()).setVisibility(GONE);cast=section("Cast & Crew");details=section("Details");related=section("More Like This — In Your Library");trailers=section("Trailers & Extras");((View)trailers.getParent()).setVisibility(GONE);
        ((View)cast.getParent()).setVisibility(GONE);((View)related.getParent()).setVisibility(GONE);
    }
    public boolean atTop(){return getScrollY()==0 && (play.hasFocus()||trailer.hasFocus()||findFocus()!=null&&String.valueOf(findFocus().getTag()).startsWith("action:"));}
    public void focusPrimary(){play.requestFocus();}
    @Override public boolean dispatchKeyEvent(KeyEvent e){if(e.getAction()==KeyEvent.ACTION_DOWN&&e.getKeyCode()==KeyEvent.KEYCODE_DPAD_UP&&(play.hasFocus()||trailer.hasFocus())&&getScrollY()>0){smoothScrollTo(0,0);return true;}return super.dispatchKeyEvent(e);}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private TextView text(String s,int size){TextView t=new TextView(getContext());t.setText(s);t.setTextColor(Color.WHITE);t.setTextSize(size);return t;}
    private GradientDrawable bg(boolean focus){GradientDrawable d=new GradientDrawable();d.setColor(focus?0xee254964:0xcc10283b);d.setCornerRadius(dp(5));d.setStroke(dp(focus?2:1),focus?PreviewAccent.color(getContext()):0xff284b62);return d;}
    private TextView button(String s,Runnable run){TextView t=text(s,14);t.setTag("action:"+s);PreviewIcon.apply(t,s,16);t.setGravity(Gravity.CENTER);t.setPadding(dp(14),dp(9),dp(14),dp(9));t.setFocusable(true);t.setFocusableInTouchMode(true);t.setBackground(bg(false));t.setOnFocusChangeListener((v,f)->v.setBackground(bg(f)));t.setOnClickListener(v->run.run());return t;}
    private void margin(LinearLayout row,View view){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,-2);lp.leftMargin=dp(12);row.addView(view,lp);}
    private LinearLayout section(String name){LinearLayout section=new LinearLayout(getContext());section.setOrientation(LinearLayout.VERTICAL);section.setPadding(0,dp(14),0,dp(8));TextView label=text(name,19);label.setTextColor(0xff9ed4f7);section.addView(label);LinearLayout content=new LinearLayout(getContext());content.setOrientation(LinearLayout.VERTICAL);content.setPadding(0,dp(10),0,0);section.addView(content);body.addView(section);return content;}
    public void bind(Video value){movie=value;title.setText(movie.getName());OfficialTitleArtwork.bind(title,value,false);plot.setText(movie.getDescriptionBody());
        bindPoster(movie);
        play.setText(movie.getResumeMs()>0?"Resume":"Play");
        if(movie.getPreviewBackdrop()!=null)artwork.accept(movie.getPreviewBackdrop());renderDetails();
    }
    private void renderHumanMetadata(){if(movie==null)return;
        List<String> metadata=new ArrayList<>();
        if(movie instanceof Episode){Episode episode=(Episode)movie;title.setText(safe(episode.getShowName())+" — "+safe(episode.getEpisodeName()));
            if(episode.getSeasonNumber()>=0&&episode.getEpisodeNumber()>0)metadata.add("S"+episode.getSeasonNumber()+" E"+episode.getEpisodeNumber());
            if(episode.getEpisodeDate()>0)metadata.add(episode.getEpisodeDateFormatted());
            else if(tags instanceof EpisodeTags){Date aired=((EpisodeTags)tags).getAired();if(aired!=null&&aired.getTime()>0)metadata.add(java.text.DateFormat.getDateInstance().format(aired));}
        }else if(movie instanceof Movie&&((Movie)movie).getYear()>0)metadata.add(String.valueOf(((Movie)movie).getYear()));
        long minutes=movie.getDurationMs()/60000;
        if(minutes<=0&&movie instanceof Episode&&tags instanceof EpisodeTags)minutes=tags.getRuntime(java.util.concurrent.TimeUnit.MINUTES);
        if(minutes>0)metadata.add(minutes+" min");
        String certificate=movie instanceof Movie?((Movie)movie).getContentRating():movie instanceof Episode?((Episode)movie).getContentRating():null;
        if(certificate!=null&&!certificate.isEmpty())metadata.add(certificate);
        meta.setText((movie instanceof Episode?safe(((Episode)movie).getEpisodeName())+"\n":"")+android.text.TextUtils.join("  ·  ",metadata));meta.setVisibility(metadata.isEmpty()?GONE:VISIBLE);
    }
    public void setTags(BaseTags value,List<ScraperTrailer> videos,List<ScraperImage> backdrops){View oldCast=cast.findFocus();Object castKey=oldCast==null?null:oldCast.getTag();int oldY=getScrollY();tags=value;trailerList=videos==null?Collections.emptyList():videos;
        if(backdrops!=null&&!backdrops.isEmpty()){ScraperImage image=backdrops.get(0);java.io.File file=image.getLargeFileF();if(file!=null&&file.exists()){artwork.accept(Uri.fromFile(file));if(movie!=null)movie.setPreviewBackdrop(Uri.fromFile(file).toString());}else if(image.getLargeUrl()!=null)artwork.accept(Uri.parse(image.getLargeUrl()));}
        portraits.clear();cast.removeAllViews();HorizontalScrollView scroll=new HorizontalScrollView(getContext());scroll.setSmoothScrollingEnabled(false);scroll.setHorizontalScrollBarEnabled(false);LinearLayout people=new LinearLayout(getContext());scroll.addView(people);cast.addView(scroll);
        if(tags!=null){for(Map.Entry<String,String> person:tags.getActors().entrySet())person(people,person.getKey(),person.getValue());
            if(tags.getDirectorsFormatted()!=null&&!tags.getDirectorsFormatted().isEmpty())person(people,tags.getDirectorsFormatted(),"Director");}
        ((View)cast.getParent()).setVisibility(people.getChildCount()==0?GONE:VISIBLE);
        trailer.setVisibility(primaryTrailer()==null?View.GONE:View.VISIBLE);
        PreviewPeople.load(getContext().getApplicationContext(),tags,new HashMap<>(portraits));renderDetails();renderRelated();restoreRowFocus(cast,castKey,oldY);
    }
    public void setSnapshot(Snapshot value){snapshot=value;renderRelated();}
    private void renderDetails(){observeActions();if(movie==null&&show==null)return;renderHumanMetadata();details.removeAllViews();
        LinearLayout row=new LinearLayout(getContext());row.setOrientation(LinearLayout.VERTICAL);details.addView(row);
        String genre=tags instanceof VideoTags?((VideoTags)tags).getGenresFormatted():"";
        if(safe(genre).isEmpty()&&tags instanceof EpisodeTags){ShowTags parent=((EpisodeTags)tags).getShowTags();if(parent!=null)genre=parent.getGenresFormatted();}
        String studio=tags instanceof VideoTags?((VideoTags)tags).getStudiosFormatted():"";
        float rating=movie instanceof Episode?((Episode)movie).getEpisodeRating():movie instanceof Movie?((Movie)movie).getRating():show==null?0:show.getRating();
        if(movie instanceof Episode&&rating<=0&&tags instanceof EpisodeTags)rating=tags.getRating();
        List<String> contextParts=new ArrayList<>();if(!safe(genre).isEmpty())contextParts.add(genre);if(rating>0)contextParts.add(String.format(Locale.UK,"TMDb %.1f / 10",rating));context.setText(android.text.TextUtils.join("  ·  ",contextParts));context.setVisibility(contextParts.isEmpty()?GONE:VISIBLE);
        pills.removeAllViews();if(movie!=null){if(movie.hasMeasured4K())pill("4K");else if(movie.getMeasuredHeight()>=1040)pill("1080p");else if(movie.getMeasuredHeight()>=720)pill("720p");
        }
        ((View)details.getParent()).setVisibility(row.getChildCount()==0?GONE:VISIBLE);
    }
    private void observeActions(){ObjectAdapter next=actions.get();if(next!=observedActions){if(observedActions!=null)observedActions.unregisterObserver(actionObserver);observedActions=next;if(next!=null)next.registerObserver(actionObserver);}renderProviders();}
    private void renderProviders(){
        View focused=providerActions.findFocus();Object focusKey=focused==null?null:focused.getTag();
        for(Presenter.ViewHolder holder:providerHolders)providerPresenter.onUnbindViewHolder(holder);providerHolders.clear();providerActions.removeAllViews();if(observedActions==null){if(focused!=null)play.requestFocus();return;}
        for(int i=0;i<observedActions.size();i++){Object item=observedActions.get(i);if(!(item instanceof Action)||!com.archos.mediacenter.video.streaming.StreamingActions.isAvailableOffer((Action)item))continue;Action offer=(Action)item;Presenter.ViewHolder holder=providerPresenter.onCreateViewHolder(providerActions);providerPresenter.onBindViewHolder(holder,offer);holder.view.setTag(offer.getId());holder.view.setOnClickListener(v->action.accept(offer));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(64),dp(38));lp.leftMargin=dp(10);providerActions.addView(holder.view,lp);providerHolders.add(holder);}
        if(focused!=null){View replacement=focusKey==null?null:providerActions.findViewWithTag(focusKey);if(replacement!=null)replacement.requestFocus();else if(providerActions.getChildCount()>0)providerActions.getChildAt(0).requestFocus();else play.requestFocus();}
    }
    private void pill(String value){if(value==null||value.isEmpty())return;TextView label=text(value,10);label.setSingleLine(true);label.setPadding(dp(6),dp(3),dp(6),dp(3));GradientDrawable badge=new GradientDrawable();badge.setColor(0x50142634);badge.setCornerRadius(dp(3));badge.setStroke(dp(1),0xff648196);label.setBackground(badge);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,-2);lp.rightMargin=dp(6);pills.addView(label,lp);}

    private void person(LinearLayout people,String name,String role){
        LinearLayout card=new LinearLayout(getContext());card.setOrientation(android.widget.LinearLayout.HORIZONTAL);card.setGravity(Gravity.CENTER_VERTICAL);card.setBackgroundResource(com.archos.mediacenter.video.R.drawable.preview_surface_focus);card.setFocusable(true);card.setFocusableInTouchMode(true);card.setPadding(dp(6),dp(6),dp(6),dp(8));
        // The existing scraper stores names/roles but no portrait URLs. Use the real Nova asset.
        ImageView portrait=new ImageView(getContext());portrait.setImageResource(com.archos.mediacenter.video.R.drawable.preview_person);portraits.put(name,portrait);portrait.setScaleType(ImageView.ScaleType.CENTER_CROP);portrait.setClipToOutline(true);portrait.setOutlineProvider(new ViewOutlineProvider(){@Override public void getOutline(View view,android.graphics.Outline outline){outline.setOval(0,0,view.getWidth(),view.getHeight());}});card.addView(portrait,new LinearLayout.LayoutParams(dp(42),dp(42)));
        LinearLayout words=new LinearLayout(getContext());words.setOrientation(android.widget.LinearLayout.VERTICAL);words.setPadding(dp(8),0,0,0);card.addView(words,new LinearLayout.LayoutParams(0,-2,1));
        TextView label=text(name,12);label.setMaxLines(2);label.setEllipsize(android.text.TextUtils.TruncateAt.END);words.addView(label);TextView detail=text(role==null?"":role,10);detail.setMaxLines(1);detail.setTextColor(0xffa4b6c7);words.addView(detail);card.setContentDescription(name+" "+safe(role));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(165),dp(62));lp.rightMargin=dp(10);people.addView(card,lp);card.setTag("person:"+name+":"+safe(role));rowKeys(card,people);
    }
    private void bindPoster(Base value){
        com.squareup.picasso.Picasso.get().cancelRequest(poster);poster.setImageDrawable(null);
        if(value.getPosterUri()!=null)com.squareup.picasso.Picasso.get().load(value.getPosterUri()).resize(dp(142),dp(213)).centerInside().noFade().into(poster);
        poster.setContentDescription(value.getName());
    }
    public void bindShow(Tvshow value,Runnable playAction){show=value;showPlay=playAction;OfficialTitleArtwork.bind(title,value,false);bindPoster(value);title.setText(value.getName());plot.setText(value.getPlot());meta.setText((value.getYear()>0?value.getYear()+" · ":"")+value.getSeasonCount()+(value.getSeasonCount()==1?" season":" seasons")+" · "+value.getEpisodeCount()+(value.getEpisodeCount()==1?" episode":" episodes"));renderDetails();}
    public void setSeason(int number,List<Episode> values){
        View focused=findFocus();Object old=focused==null?null:focused.getTag();int y=getScrollY();seasons.put(number,values);episodes.removeAllViews();((View)episodes.getParent()).setVisibility(VISIBLE);
        for(Map.Entry<Integer,List<Episode>> season:seasons.entrySet()){
            episodes.addView(text(season.getKey()==0?"Specials":"Season "+season.getKey(),16));HorizontalScrollView scroll=new HorizontalScrollView(getContext());scroll.setSmoothScrollingEnabled(false);LinearLayout row=new LinearLayout(getContext());scroll.addView(row);episodes.addView(scroll);
            for(Episode e:season.getValue()){Presenter.ViewHolder h=presenter.onCreateViewHolder(row);presenter.onBindViewHolder(h,e);h.view.setTag("episode:"+e.getId());h.view.setContentDescription("S"+e.getSeasonNumber()+" E"+e.getEpisodeNumber()+" · "+e.getEpisodeName());LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(172),dp(105));lp.setMargins(0,dp(6),dp(12),dp(12));row.addView(h.view,lp);h.view.setOnKeyListener((v,key,event)->{if(event.getAction()!=KeyEvent.ACTION_DOWN||key!=KeyEvent.KEYCODE_DPAD_LEFT&&key!=KeyEvent.KEYCODE_DPAD_RIGHT)return false;int index=row.indexOfChild(v),next=index+(key==KeyEvent.KEYCODE_DPAD_LEFT?-1:1);if(next>=0&&next<row.getChildCount()){View target=row.getChildAt(next);target.requestFocus();android.graphics.Rect rect=new android.graphics.Rect();target.getDrawingRect(rect);target.requestRectangleOnScreen(rect,true);}return true;});h.view.setOnClickListener(v->new VideoViewClickedListener((Activity)getContext()).onItemClicked(h,e,null,null));if(old!=null&&old.equals(h.view.getTag()))h.view.requestFocus();}
        }
        if(show!=null){List<Entry> all=new ArrayList<>();Snapshot cached=PreviewLibraryLoader.memoryCache();if(cached!=null)for(Entry ep:cached.episodes)if(ep.show==show.getTvshowId())all.add(ep);if(all.isEmpty())for(List<Episode> group:seasons.values())for(Episode ep:group){Entry entry=new Entry(ep,0,show.getTvshowId(),"");if(show.getShowTags()!=null)entry.onlineId=show.getShowTags().getOnlineId();all.add(entry);}PreviewSeriesJourney.Selection selected=PreviewSeriesJourney.select(getContext(),all);play.setText(selected.episode!=null&&PreviewSeriesJourney.resumable((Video)selected.episode.media)?"Resume":"Play");}
        scrollTo(0,y);
    }

    public static String source(Uri uri){if(uri==null)return "Unknown";String scheme=uri.getScheme();
        if(scheme==null||scheme.equals("file")||scheme.equals("content"))return "Local storage";
        if(scheme.equals("smb"))return "NAS / SMB";if(scheme.equals("webdav")||scheme.equals("webdavs")||scheme.equals("dav")||scheme.equals("davs"))return "WebDAV";
        if(scheme.equals("http")||scheme.equals("https"))return "Network / HTTP(S)";
        return "Network / "+scheme.toUpperCase(Locale.ROOT);
    }
    private static String safe(String s){return s==null?"":s;}
    private static String shortFile(String s){if(s==null)return "";return s.length()>90?s.substring(0, sixty(s))+"…"+s.substring(s.length()-20):s;}
    private static int sixty(String s){return Math.min(60,s.length());}
    private void info(LinearLayout row,String name,String value){TextView t=text(value.replace("\n\n","    ·    ").replace("\n",": "),13);t.setMaxLines(3);t.setEllipsize(android.text.TextUtils.TruncateAt.END);t.setTextColor(0xffa8becf);t.setPadding(0,dp(3),0,dp(3));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(7);row.addView(t,lp);}
    private void renderRelated(){View oldFocus=related.findFocus();Object focusKey=oldFocus==null?null:oldFocus.getTag();int oldY=getScrollY();for(Presenter.ViewHolder h:cards)presenter.onUnbindViewHolder(h);cards.clear();related.removeAllViews();if(movie==null||snapshot==null||tags==null){restoreRowFocus(related,focusKey,oldY);return;}
        Entry seed=new Entry(movie,0,0,tags instanceof VideoTags?((VideoTags)tags).getGenresFormatted():"");
        HorizontalScrollView scroll=new HorizontalScrollView(getContext());scroll.setSmoothScrollingEnabled(false);LinearLayout row=new LinearLayout(getContext());scroll.addView(row);related.addView(scroll);
        int count=0;for(Entry e:PreviewDiscovery.similar(seed,snapshot)){if(!(e.media instanceof Movie))continue;if(count++==12)break;
            Presenter.ViewHolder h=presenter.onCreateViewHolder(row);presenter.onBindViewHolder(h,e.media);cards.add(h);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(172),-2);lp.rightMargin=dp(12);row.addView(h.view,lp);h.view.setTag("related:"+e.key());rowKeys(h.view,row);h.view.setOnClickListener(v->new VideoViewClickedListener((Activity)getContext()).onItemClicked(h,e.media,null,null));}
        ((View)related.getParent()).setVisibility(count==0?GONE:VISIBLE);restoreRowFocus(related,focusKey,oldY);
    }
    private void rowKeys(View card,LinearLayout row){card.setOnKeyListener((v,key,event)->{if(event.getAction()!=KeyEvent.ACTION_DOWN||key!=KeyEvent.KEYCODE_DPAD_LEFT&&key!=KeyEvent.KEYCODE_DPAD_RIGHT)return false;int next=row.indexOfChild(v)+(key==KeyEvent.KEYCODE_DPAD_LEFT?-1:1);if(next>=0&&next<row.getChildCount())row.getChildAt(next).requestFocus();return true;});}
    private void restoreRowFocus(ViewGroup area,Object key,int y){if(key==null)return;View target=area.findViewWithTag(key);if(target==null)for(View candidate:area.getFocusables(View.FOCUS_FORWARD))if(candidate.getTag()!=null){target=candidate;break;}if(target==null)target=play;target.requestFocus();scrollTo(0,y);final View restored=target;post(()->{if(restored.hasFocus())scrollTo(0,y);});}
    public void play(){if(showPlay!=null){showPlay.run();return;}ObjectAdapter adapter=actions.get();if(adapter==null)return;
        for(int id:new int[]{VideoActionAdapter.ACTION_RESUME,VideoActionAdapter.ACTION_LOCAL_RESUME,VideoActionAdapter.ACTION_PLAY,VideoActionAdapter.ACTION_PLAY_FROM_BEGIN,VideoActionAdapter.ACTION_REMOTE_RESUME})
            for(int i=0;i<adapter.size();i++){Object item=adapter.get(i);if(item instanceof Action&&((Action)item).getId()==id){action.accept((Action)item);return;}}
    }
    private Entry rowEntry(){if(show!=null)return new Entry(show,0,show.getTvshowId(),"");if(movie==null)return null;long showId=0;Snapshot cached=PreviewLibraryLoader.memoryCache();if(movie instanceof Episode&&cached!=null)for(Entry e:cached.episodes)if(((Video)e.media).getId()==movie.getId()){showId=e.show;break;}return new Entry(movie,0,showId,"");}
    private void moreInfo(){
        View previous=findFocus();Dialog dialog=new Dialog(getContext(),android.R.style.Theme_Material_NoActionBar_Fullscreen);
        ScrollView scroll=new ScrollView(getContext());scroll.setFillViewport(true);scroll.setFocusable(true);scroll.setBackground(PreviewAccent.utility(getContext()));LinearLayout page=new LinearLayout(getContext());page.setOrientation(LinearLayout.VERTICAL);page.setPadding(dp(42),dp(28),dp(42),dp(30));scroll.addView(page);
        TextView heading=text(title.getText().toString(),28);page.addView(heading);TextView metadata=text(meta.getText()+"  "+context.getText(),13);metadata.setTextColor(0xffadc3d3);page.addView(metadata);
        LinearLayout columns=new LinearLayout(getContext());columns.setPadding(0,dp(10),0,0);page.addView(columns,new LinearLayout.LayoutParams(-1,-2));LinearLayout overview=new LinearLayout(getContext());overview.setOrientation(LinearLayout.VERTICAL);columns.addView(overview,new LinearLayout.LayoutParams(0,-2,1.65f));LinearLayout facts=new LinearLayout(getContext());facts.setOrientation(LinearLayout.VERTICAL);facts.setPadding(dp(30),0,0,0);columns.addView(facts,new LinearLayout.LayoutParams(0,-2,1));
        infoSection(overview,"Overview",show!=null?safe(show.getPlot()):movie==null?"":safe(movie.getDescriptionBody()));
        if(tags!=null){StringBuilder people=new StringBuilder();for(Map.Entry<String,String> p:tags.getActors().entrySet())people.append(p.getKey()).append(safe(p.getValue()).isEmpty()?"":" · "+p.getValue()).append("\n");if(!safe(tags.getDirectorsFormatted()).isEmpty())people.append("Director · ").append(tags.getDirectorsFormatted());infoSection(overview,"Cast & Crew",people.toString());}
        if(movie!=null){List<String> technical=new ArrayList<>();if(movie.getMeasuredWidth()>0&&movie.getMeasuredHeight()>0)technical.add(movie.getMeasuredWidth()+" × "+movie.getMeasuredHeight());String video=PreviewMediaInfo.format(movie.getCalculatedVideoFormat()),audio=PreviewMediaInfo.format(movie.getCalculatedBestAudioFormat());if(!safe(video).isEmpty())technical.add(video);if(!safe(audio).isEmpty())technical.add(audio);infoSection(facts,"Technical information",android.text.TextUtils.join(" · ",technical));String file=safe(movie.getFilenameNonCryptic())+"\n"+source(movie.getUri());if(movie.getSize()>0)file+=" · "+android.text.format.Formatter.formatFileSize(getContext(),movie.getSize());infoSection(facts,"Local library file",file);}
        if(show!=null)infoSection(facts,"Series",meta.getText()+"\n"+context.getText());TextView back=text("Press Back to return to Details",11);back.setPadding(0,dp(26),0,0);back.setTextColor(0xff8da9bb);page.addView(back);
        dialog.setContentView(scroll);dialog.setOnDismissListener(d->{if(previous!=null)previous.requestFocus();});dialog.show();dialog.getWindow().setLayout(-1,-1);scroll.requestFocus();
    }
    private void infoSection(LinearLayout page,String label,String value){if(value==null||value.trim().isEmpty())return;TextView heading=text(label,18);heading.setTextColor(PreviewAccent.color(getContext()));heading.setPadding(0,dp(22),0,dp(8));page.addView(heading);TextView content=text(value.trim(),14);content.setLineSpacing(dp(3),1f);page.addView(content);}
    private void more(){ObjectAdapter adapter=actions.get();if(adapter==null)return;List<Action> items=new ArrayList<>();List<String> labels=new ArrayList<>();
        List<Action> source=new ArrayList<>();for(int i=0;i<adapter.size();i++){Object value=adapter.get(i);if(value instanceof Action){Action a=(Action)value;if(show!=null&&a.getId()==com.archos.mediacenter.video.leanback.tvshow.TvshowActionAdapter.ACTION_MORE_DETAILS)continue;source.add(a);}}
        for(int group=0;group<3;group++){boolean heading=false;for(Action a:source){String label=String.valueOf(a.getLabel1())+(a.getLabel2()==null?"":" — "+a.getLabel2());String lower=label.toLowerCase(Locale.ROOT);int category=lower.contains("delete")||lower.contains("remove")?2:lower.contains("play")||lower.contains("resume")||lower.contains("episode")?0:1;if(category!=group)continue;if(!heading){items.add(null);labels.add(group==0?"— Playback & navigation":group==1?"— Library":"— Remove / delete");heading=true;}items.add(a);labels.add(label);}}
        final int rowIndex=items.size();items.add(null);labels.add("Add to Row");
        final int synopsisIndex=items.size();items.add(null);labels.add("Full synopsis");
        if(show==null){items.add(null);labels.add("— File & media");labels.add("File, subtitles and artwork");}
        PreviewDialog.choose(getContext(),"Actions",labels.toArray(new String[0]),-1,n->{if(n==rowIndex)PreviewHomeRows.add(getContext(),rowEntry(),()->{});else if(n==synopsisIndex)PreviewDialog.read(getContext(),title.getText().toString(),show!=null?safe(show.getPlot()):movie==null?"":safe(movie.getDescriptionBody()));else if(n==items.size())nativeDetails.run();else if(items.get(n)!=null)action.accept(items.get(n));});
    }
    private ScraperTrailer primaryTrailer(){
        ScraperTrailer best=null;int score=0;for(ScraperTrailer t:trailerList){if(!"YouTube".equals(t.mSite)||t.mVideoKey==null||!t.mVideoKey.matches("[A-Za-z0-9_-]{11}"))continue;String name=t.mName==null?"":t.mName.toLowerCase(Locale.ROOT);if(!name.contains("trailer")||name.contains("fan")||name.contains("reaction"))continue;int rank=(name.contains("official")?4:1)+(name.contains("main")?2:0)+(name.contains("teaser")?-1:0);if(rank>score){score=rank;best=t;}}return best;
    }
    private void chooseTrailer(){ScraperTrailer t=primaryTrailer();if(t!=null)PreviewTrailer.show((Activity)getContext(),t);}
    @Override protected void onDetachedFromWindow(){if(observedActions!=null){observedActions.unregisterObserver(actionObserver);observedActions=null;}for(Presenter.ViewHolder h:providerHolders)providerPresenter.onUnbindViewHolder(h);providerHolders.clear();for(Presenter.ViewHolder h:cards)presenter.onUnbindViewHolder(h);cards.clear();super.onDetachedFromWindow();}
}
