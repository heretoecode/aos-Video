package com.archos.mediacenter.video.leanback;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import androidx.recyclerview.widget.*;
import androidx.leanback.widget.Presenter;
import com.archos.mediacenter.video.leanback.PreviewLibraryLoader.*;
import com.archos.mediacenter.video.leanback.adapter.object.Box;
import com.archos.mediacenter.video.leanback.presenter.PreviewCardPresenter;
import java.util.*;
import com.archos.mediacenter.video.browser.adapters.object.*;

/** Recycled native TV grids. Only visible artwork is decoded. Classic Browse remains untouched. */
public final class PreviewPages extends FrameLayout {
    public interface Click { void open(Presenter.ViewHolder holder,Object item); }
    private final RecyclerView list;
    private final GridLayoutManager layout;
    private final PageAdapter adapter=new PageAdapter();
    private final Click click;
    private Snapshot snapshot=new Snapshot();
    private List<Box> files=new ArrayList<>();
    private final List<Cell> cells=new ArrayList<>();
    private int tab;
    private final FocusAnchor[] anchors=new FocusAnchor[4];
    private final android.os.Parcelable[] scrollStates=new android.os.Parcelable[4];
    private boolean restoringFocus;
    private int focusGeneration;
    private static final class FocusAnchor {
        String cell, child; int position, inner;
    }
    private String cellKey(Cell c){return c.type+":"+(c.type==POSTER?((Entry)c.value).key():c.type==STORAGE?((Box)c.value).getBoxId()+":"+((Box)c.value).getPath():c.title);}
    private boolean inside(View view,View ancestor){while(view!=null){if(view==ancestor)return true;android.view.ViewParent p=view.getParent();view=p instanceof View?(View)p:null;}return false;}
    private final class FocusRecycler extends RecyclerView {
        final boolean horizontal;
        FocusRecycler(Context c,boolean horizontal){super(c);this.horizontal=horizontal;setPreserveFocusAfterLayout(false);}
        @Override public View focusSearch(View focused,int direction){
            View next=super.focusSearch(focused,direction);
            // At loaded-content boundaries retain the last valid card; do not let
            // RecyclerView's ancestor search fall through to global navigation.
            if(horizontal&&(direction==FOCUS_LEFT||direction==FOCUS_RIGHT)&&!inside(next,this))return focused;
            if(!horizontal&&!inside(next,PreviewPages.this)&&!(direction==FOCUS_UP&&atTop()))return focused;
            return next;
        }
    }
    private void rememberFocus(){
        if(restoringFocus)return;
        View focused=list.findFocus();if(focused==null)return;
        View item=list.findContainingItemView(focused);if(item==null)return;
        int pos=list.getChildAdapterPosition(item);if(pos<0||pos>=cells.size())return;
        FocusAnchor a=new FocusAnchor();a.cell=cellKey(cells.get(pos));a.position=pos;
        a.child=focused.getTag() instanceof String?(String)focused.getTag():null;
        if(cells.get(pos).type==RAIL){List<Entry> entries=(List<Entry>)cells.get(pos).value;for(int i=0;i<entries.size();i++)if(entries.get(i).key().equals(a.child)){a.inner=i;break;}}
        anchors[tab]=a;
    }
    @Override public void requestChildFocus(View child,View focused){super.requestChildFocus(child,focused);if(list!=null)rememberFocus();}
    @Override protected boolean onRequestFocusInDescendants(int direction,android.graphics.Rect rect){
        if(restoringFocus)return false;
        if(anchors[tab]!=null){holdFocus();restoreFocus();return true;}
        return super.onRequestFocusInDescendants(direction,rect);
    }
    private void holdFocus(){restoringFocus=true;setDescendantFocusability(FOCUS_BLOCK_DESCENDANTS);requestFocus();setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);}
    private View tagged(View root,String tag){if(tag!=null&&tag.equals(root.getTag())&&root.isFocusable())return root;if(root instanceof ViewGroup){ViewGroup g=(ViewGroup)root;for(int i=0;i<g.getChildCount();i++){View found=tagged(g.getChildAt(i),tag);if(found!=null)return found;}}return null;}
    private void restoreFocus(){
        final int generation=++focusGeneration;final FocusAnchor anchor=anchors[tab];
        int position=anchor==null?0:Math.min(anchor.position,Math.max(0,cells.size()-1));
        if(anchor!=null)for(int i=0;i<cells.size();i++)if(cellKey(cells.get(i)).equals(anchor.cell)){position=i;break;}
        final int target=position;
        if(layout.findViewByPosition(target)==null)list.scrollToPosition(target);
        list.postOnAnimation(new Runnable(){int attempts;
            public void run(){
                if(generation!=focusGeneration)return;
                if(!hasFocus()){restoringFocus=false;return;}
                View item=layout.findViewByPosition(target);
                if(item==null||list.hasPendingAdapterUpdates()){if(attempts++<8){list.postOnAnimation(this);return;}}
                if(item!=null&&anchor!=null&&target<cells.size()&&cells.get(target).type==RAIL){
                    RecyclerView rail=(RecyclerView)((ViewGroup)item).getChildAt(0);
                    List<Entry> entries=(List<Entry>)cells.get(target).value;
                    int inner=Math.min(anchor.inner,Math.max(0,entries.size()-1));
                    for(int i=0;i<entries.size();i++)if(entries.get(i).key().equals(anchor.child)){inner=i;break;}
                    if(rail.findViewHolderForAdapterPosition(inner)==null&&attempts++<8){rail.scrollToPosition(inner);list.postOnAnimation(this);return;}
                    RecyclerView.ViewHolder holder=rail.findViewHolderForAdapterPosition(inner);if(holder!=null)item=holder.itemView;
                }
                View exact=item==null||anchor==null?null:tagged(item,anchor.child);
                restoringFocus=false;
                if(exact!=null)exact.requestFocus();else if(item!=null&&item.requestFocus()){}else list.requestFocus();
                rememberFocus();
            }
        });
    }
    private final int[] sorts={0,0,0};
    private final String[] genres={"","",""};
    private final int[] years={0,0,0};
    private boolean loaded;
    private PreviewDiscovery discovery=new PreviewDiscovery();
    private java.util.concurrent.ExecutorService worker;
    private java.util.function.Consumer<android.net.Uri> artwork=uri->{};
    private final boolean[] ascending={false,false,false};
    private final boolean[] listMode={false,false,false};
    private int featuredIndex;
    public void setArtworkListener(java.util.function.Consumer<android.net.Uri> listener){artwork=listener;updateArtwork();}
    public void setDiscovery(PreviewDiscovery value){requestedDiscovery=true;discovery=value;render();}
    private boolean requestedDiscovery;
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();requestDiscovery();}
    private void requestDiscovery(){if(requestedDiscovery||!isAttachedToWindow()||snapshot.movies.isEmpty()&&snapshot.shows.isEmpty())return;requestedDiscovery=true;
        worker=java.util.concurrent.Executors.newSingleThreadExecutor();worker.execute(()->{try{PreviewDiscovery result=PreviewDiscovery.load(getContext().getApplicationContext());post(()->{if(isAttachedToWindow())setDiscovery(result);});}finally{worker.shutdown();}});}
    @Override protected void onDetachedFromWindow(){if(worker!=null)worker.shutdownNow();super.onDetachedFromWindow();}
    private Entry featured(){List<Entry> entries=tab==1?snapshot.movies:tab==2?snapshot.shows:snapshot.recent;return entries.isEmpty()?null:entries.get(featuredIndex%Math.min(entries.size(),5));}
    private void updateArtwork(){Entry entry=featured();artwork.accept(tab==3||entry==null?null:entry.backdrop);}
    public static String displayName(Entry e){return e.media instanceof Episode?((Episode)e.media).getShowName():e.media.getName();}
    private void open(Entry e,View v){click.open(new Presenter.ViewHolder(v),e.media);}
    private void play(Entry e,View v){
        if(e.media instanceof Video && getContext() instanceof android.app.Activity){
            android.content.Intent intent=new android.content.Intent(getContext(),com.archos.mediacenter.video.leanback.details.VideoDetailsActivity.class);
            intent.putExtra(com.archos.mediacenter.video.leanback.details.VideoDetailsFragment.EXTRA_VIDEO,(Video)e.media);
            intent.putExtra("preview_play",true);getContext().startActivity(intent);
        }else open(e,v);
    }
    private static final int HEADER=0, POSTER=1, RAIL=2, STORAGE=3, HERO=4, NOTICE=5, LIST=6;
    static class Cell {
        int type; String title; Object value;
        Cell(int type,String title,Object value) {this.type=type;this.title=title;this.value=value;}
    }
    public PreviewPages(Context c,Click click) {
        super(c); this.click=click; setBackgroundColor(Color.TRANSPARENT);setFocusable(true);setFocusableInTouchMode(true);setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
        list=new FocusRecycler(c,false); list.setClipToPadding(false); list.setPadding(dp(28),dp(10),dp(28),dp(12));
        layout=new GridLayoutManager(c,24); layout.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup(){@Override public int getSpanSize(int p){return cells.get(p).type==POSTER?(tab<3&&listMode[tab]?24:3):cells.get(p).type==STORAGE?8:24;}});
        adapter.setHasStableIds(true);
        list.setLayoutManager(layout); list.setAdapter(adapter); list.setItemAnimator(null);
        addView(list,new FrameLayout.LayoutParams(-1,-1)); render();
    }
    public boolean atTop() {
        View focused=list.findFocus(); if(focused==null)return isFocused()&&!restoringFocus;
        View item=list.findContainingItemView(focused);if(item==null)return false;
        int p=list.getChildAdapterPosition(item);if(p<0||p>=cells.size())return false;
        Cell cell=cells.get(p);
        if(tab==0)return cell.type==HERO;
        if(tab==1||tab==2)return cell.type==HEADER&&Boolean.TRUE.equals(cell.value);
        return cell.type==STORAGE&&p<5;
    }
    public void setTab(int tab) {
        if(this.tab==tab)return;
        rememberFocus();scrollStates[this.tab]=layout.onSaveInstanceState();
        this.tab=tab;featuredIndex=0;render();
        if(scrollStates[tab]!=null)layout.onRestoreInstanceState(scrollStates[tab]);else list.scrollToPosition(0);
    }
    public void setSnapshot(Snapshot s) { if(s==null)return; snapshot=s; loaded=true; render();postDelayed(this::requestDiscovery,750); }
    public void setFiles(List<Box> f) {
        boolean same=f.size()==files.size();
        for(int i=0;same && i<f.size();i++)same=f.get(i).getBoxId()==files.get(i).getBoxId() && Objects.equals(f.get(i).getName(),files.get(i).getName()) && Objects.equals(f.get(i).getPath(),files.get(i).getPath());
        if(same)return;files=f;if(tab==3)render();
    }
    private void header(String title,boolean controls) {cells.add(new Cell(HEADER,title,controls));}
    private void rail(String title,List<Entry> items) {if(items.isEmpty())return; header(title,false);cells.add(new Cell(RAIL,title,new ArrayList<>(items.subList(0,Math.min(30,items.size())))));}
    private void render() {
        rememberFocus();boolean preserve=hasFocus();
        if(preserve)holdFocus();
        Object state=layout.onSaveInstanceState(); cells.clear();
        if(tab==0) {
            if(featured()!=null)cells.add(new Cell(HERO,"Featured",featured()));
            List<Entry> continuing=new ArrayList<>(snapshot.continuingMovies);continuing.addAll(snapshot.continuingShows);
            rail("Continue Watching",continuing);rail("Recently Added",snapshot.recent);
            chart("Trending on Trakt — In Your Library",true);chart("Popular on Trakt — In Your Library",false);
            rail("Recently Watched",snapshot.watched);
            Entry recent=snapshot.played.isEmpty()?null:snapshot.played.get(0);
            if(recent!=null)rail("Because You Watched "+displayName(recent),PreviewDiscovery.similar(recent,snapshot));
            if(loaded&&snapshot.movies.isEmpty()&&snapshot.shows.isEmpty()&&snapshot.recent.isEmpty()) header("Your library is empty — add media through Network & files",false);
        } else if(tab==1||tab==2) {
            if(featured()!=null)cells.add(new Cell(HERO,tab==1?"Movies":"TV Shows",featured()));

            header(tab==1?"Movie library":"TV show library",true);
            List<Entry> entries=filtered(); for(Entry e:entries)cells.add(new Cell(POSTER,"",e));
            if(loaded&&entries.isEmpty())header("No matching titles",false);
        } else {
            header("Network & files",false);
            for(String section:new String[]{"Local storage","Network","Playlists"}) {
                List<Box> group=new ArrayList<>();for(Box box:files){String s=box.getBoxId()==Box.ID.NETWORK?"Network":box.getBoxId()==Box.ID.VIDEOS_BY_LISTS?"Playlists":"Local storage";if(section.equals(s))group.add(box);}
                if(!group.isEmpty()){header(section,false);for(Box box:group)cells.add(new Cell(STORAGE,"",box));}
            }
        }
        if(!loaded&&tab!=3)header("Loading library…",false);
        updateArtwork();adapter.notifyDataSetChanged(); if(state!=null)layout.onRestoreInstanceState((android.os.Parcelable)state);if(preserve)restoreFocus();
    }
    private List<Entry> source(){return tab==1?snapshot.movies:snapshot.shows;}
    private void chart(String name,boolean trend){
        List<Entry> matches=discovery.matches(snapshot,trend);
        if(!matches.isEmpty())rail(name,matches);
        else {cells.add(new Cell(NOTICE,name,discovery.available?"No matching titles in the current Trakt chart":"Unavailable — Trakt charts will appear here when connected"));}
    }
    public static List<Entry> sortEntries(List<Entry> input,int sort,boolean ascending,PreviewDiscovery discovery){
        List<Entry> entries=new ArrayList<>(input);
        Comparator<Entry> cmp;
        if(sort>=3){Map<String,Integer> ranks=sort==3?discovery.trending:discovery.popular;
            cmp=(a,b)->{Integer x=ranks.get(PreviewDiscovery.key(a)),y=ranks.get(PreviewDiscovery.key(b));
                if(x==null||y==null)return x==null?(y==null?0:1):-1;
                return ascending?Integer.compare(y,x):Integer.compare(x,y);};
        }else{
            cmp=sort==1?Comparator.comparing(e->displayName(e),String.CASE_INSENSITIVE_ORDER):sort==2?Comparator.comparing(e->e.releaseDate==null||e.releaseDate.isEmpty()?String.valueOf(e.year()):e.releaseDate):Comparator.comparingLong(e->e.added);
            if(!ascending)cmp=cmp.reversed();
        }
        entries.sort(cmp.thenComparing(e->displayName(e),String.CASE_INSENSITIVE_ORDER));return entries;
    }
    private List<Entry> filtered(){
        List<Entry> entries=new ArrayList<>();for(Entry e:source())if((genres[tab].isEmpty()||Arrays.asList(e.genres.split("[|,;/]")).stream().anyMatch(g->g.trim().equals(genres[tab])))&&(years[tab]==0||e.year()==years[tab]))entries.add(e);
        return sortEntries(entries,sorts[tab],ascending[tab],discovery);
    }
    private String[] sortLabels(){return new String[]{"Date Added","Title",tab==2?"Air Date":"Release Date","Trakt Trending","Trakt Popular"};}
    private void sort(){
        String[] names=sortLabels();if(!discovery.available){names[3]+=" — unavailable";names[4]+=" — unavailable";}
        new AlertDialog.Builder(getContext()).setTitle("Sort").setSingleChoiceItems(names,sorts[tab],(d,n)->{
            if(n>=3&&!discovery.available)return;sorts[tab]=n;ascending[tab]=n==1;d.dismiss();render();
        }).setNegativeButton("Cancel",null).show();
    }
    private void filter(){new AlertDialog.Builder(getContext()).setTitle("Filter").setItems(new String[]{"Genre"+(genres[tab].isEmpty()?"":": "+genres[tab]),"Year"+(years[tab]==0?"":": "+years[tab]),"Clear filters"},(d,n)->{
        if(n==2){genres[tab]="";years[tab]=0;render();return;}
        TreeSet<String> values=new TreeSet<>(); for(Entry e:source())if(n==0){for(String g:e.genres.split("[|,;/]"))if(!g.trim().isEmpty())values.add(g.trim());}else if(e.year()>0)values.add(String.valueOf(e.year()));
        List<String> options=new ArrayList<>();options.add("All");options.addAll(values);
        new AlertDialog.Builder(getContext()).setTitle(n==0?"Genre":"Year").setItems(options.toArray(new String[0]),(dialog,i)->{if(n==0)genres[tab]=i==0?"":options.get(i);else years[tab]=i==0?0:Integer.parseInt(options.get(i));render();}).setNegativeButton("Cancel",null).show();
    }).setNegativeButton("Cancel",null).show();}
    private TextView text(String value,int size){TextView t=new TextView(getContext());t.setText(value);t.setTextColor(Color.WHITE);t.setTextSize(size);return t;}
    private GradientDrawable background(boolean focus){GradientDrawable d=new GradientDrawable();d.setColor(focus?0xff25445c:0xff192f45);d.setCornerRadius(dp(5));d.setStroke(dp(focus?2:1),focus?0xff62bbf3:0xff304b60);return d;}
    private TextView button(String name,Runnable action){TextView b=text(name,13);b.setGravity(Gravity.CENTER);b.setPadding(dp(14),dp(9),dp(14),dp(9));b.setFocusable(true);b.setFocusableInTouchMode(true);b.setClickable(true);b.setBackground(background(false));b.setOnFocusChangeListener((v,f)->v.setBackground(background(f)));b.setOnClickListener(v->action.run());return b;}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    class Holder extends RecyclerView.ViewHolder {
        Presenter presenter; Presenter.ViewHolder card;
        Holder(View v){super(v);}
    }
    class PageAdapter extends RecyclerView.Adapter<Holder> {
        @Override public long getItemId(int p){Cell c=cells.get(p);String key=c.type==POSTER?((Entry)c.value).key():c.type==STORAGE?((Box)c.value).getBoxId()+":"+((Box)c.value).getPath():c.title;return ((long)c.type<<32) | (key.hashCode() & 0xffffffffL);}
        @Override public int getItemCount(){return cells.size();}
        @Override public int getItemViewType(int p){return cells.get(p).type==POSTER&&tab<3&&listMode[tab]?LIST:cells.get(p).type;}
        @Override public Holder onCreateViewHolder(ViewGroup parent,int type){
            if(type==POSTER||type==LIST){PreviewCardPresenter pr=new PreviewCardPresenter(type==LIST?PreviewCardPresenter.Style.LIST:PreviewCardPresenter.Style.POSTER); Presenter.ViewHolder card=pr.onCreateViewHolder(parent);Holder h=new Holder(card.view);h.presenter=pr;h.card=card;RecyclerView.LayoutParams lp=new RecyclerView.LayoutParams(-1,-2);lp.setMargins(0,0,dp(10),dp(15));h.itemView.setLayoutParams(lp);return h;}
            LinearLayout v=new LinearLayout(getContext());v.setGravity(Gravity.CENTER_VERTICAL);v.setPadding(0,dp(6),0,dp(6));v.setLayoutParams(new RecyclerView.LayoutParams(-1,-2));return new Holder(v);
        }
        @Override public void onBindViewHolder(Holder h,int p){Cell c=cells.get(p);
            if(c.type==POSTER){Entry e=(Entry)c.value;h.presenter.onBindViewHolder(h.card,e.media);h.itemView.setTag(e.key());h.itemView.setOnClickListener(v->click.open(h.card,e.media));return;}
            LinearLayout v=(LinearLayout)h.itemView;v.removeAllViews();v.setFocusable(false);v.setOnClickListener(null);v.setBackground(null);v.setOrientation(LinearLayout.HORIZONTAL);v.setPadding(0,dp(6),0,dp(6));v.setLayoutParams(new RecyclerView.LayoutParams(-1,-2));
            if(c.type==HERO){Entry e=(Entry)c.value;v.setOrientation(LinearLayout.VERTICAL);v.setGravity(Gravity.CENTER_VERTICAL);v.setPadding(0,dp(6),0,dp(12));
                v.setMinimumHeight(dp(tab==0?235:76));
                v.setLayoutParams(new RecyclerView.LayoutParams(-1,tab==0?dp(235):android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
                if(tab==0){TextView featured=text("F E A T U R E D",11);featured.setTextColor(0xff9ed4f7);v.addView(featured);
                    TextView title=text(displayName(e),32);title.setLines(2);title.setEllipsize(android.text.TextUtils.TruncateAt.END);title.setTypeface(null,android.graphics.Typeface.BOLD);v.addView(title,new LinearLayout.LayoutParams(dp(470),-2));
                    String meta=e.year()>0?String.valueOf(e.year()):"";
                    if(e.media instanceof Video){Video video=(Video)e.media;if(video.getDurationMs()>0)meta+="   "+video.getDurationMs()/60000+" min";if(video.hasMeasured4K())meta+="   4K";}
                    v.addView(text(meta,13));String plot=e.media instanceof Tvshow?((Tvshow)e.media).getPlot():e.media instanceof Video?((Video)e.media).getDescriptionBody():"";
                    TextView description=text(plot==null?"":plot,13);description.setLines(3);description.setEllipsize(android.text.TextUtils.TruncateAt.END);v.addView(description,new LinearLayout.LayoutParams(dp(450),-2));
                    LinearLayout actions=new LinearLayout(getContext());actions.setPadding(0,dp(12),0,0);
                    TextView play=button("▶  "+(e.media instanceof Tvshow?"View Show":"Play"),()->play(e,v));play.setTag("hero:play");actions.addView(play);
                    TextView info=button("ⓘ  More Info",()->open(e,v));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,-2);lp.leftMargin=dp(10);info.setTag("hero:info");actions.addView(info,lp);
                    TextView next=button("Next featured  ›",()->{featuredIndex++;render();});lp=new LinearLayout.LayoutParams(-2,-2);lp.leftMargin=dp(10);next.setTag("hero:next");actions.addView(next,lp);v.addView(actions);
                }else{TextView title=text(c.title,30);title.setTypeface(null,android.graphics.Typeface.BOLD);v.addView(title);v.addView(text(source().size()+ (tab==1?" movies":" shows"),13));}
            }
            else if(c.type==NOTICE){v.setOrientation(LinearLayout.VERTICAL);TextView title=text(c.title,18);title.setTextColor(0xff8298aa);v.addView(title);TextView status=text((String)c.value,12);status.setTextColor(0xff8298aa);v.addView(status);v.setPadding(0,dp(12),0,dp(12));}
            else if(c.type==HEADER){
                if(Boolean.TRUE.equals(c.value)){
                    v.setOrientation(LinearLayout.VERTICAL);LinearLayout controls=new LinearLayout(getContext());
                    controls.addView(button("Genre: "+(genres[tab].isEmpty()?"All Genres":genres[tab])+"  ▾",()->filter()));
                    String order=sorts[tab]==1?(ascending[tab]?"A → Z":"Z → A"):sorts[tab]>=3?(ascending[tab]?"Lowest ranked first":"Highest ranked first"):(ascending[tab]?"Oldest first":"Newest first");
                    for(TextView control:new TextView[]{button("Sort: "+sortLabels()[sorts[tab]]+"  ▾",()->sort()),button(order+"  ▾",()->{ascending[tab]=!ascending[tab];render();}),button(listMode[tab]?"Grid view":"List view",()->{listMode[tab]=!listMode[tab];render();})}){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,-2);lp.leftMargin=dp(8);controls.addView(control,lp);}
                    for(int i=0;i<controls.getChildCount();i++)controls.getChildAt(i).setTag("control:"+i);v.addView(controls);TextView title=text(c.title,18);title.setPadding(0,dp(12),0,0);v.addView(title);
                }else{TextView title=text(c.title,19);title.setTextColor(0xff9ed4f7);v.addView(title);}
            }
            else if(c.type==RAIL){v.setPadding(0,0,0,dp(10));RecyclerView rail=new FocusRecycler(getContext(),true);rail.setLayoutManager(new LinearLayoutManager(getContext(),RecyclerView.HORIZONTAL,false));rail.setItemAnimator(null);rail.setAdapter(new RailAdapter((List<Entry>)c.value));v.addView(rail,new LinearLayout.LayoutParams(-1,dp(105)));}
            else if(c.type==STORAGE){Box b=(Box)c.value;v.setPadding(dp(12),dp(12),dp(12),dp(12));RecyclerView.LayoutParams lp=new RecyclerView.LayoutParams(-1,dp(88));lp.setMargins(0,0,dp(12),dp(12));v.setLayoutParams(lp);v.setBackground(background(false));v.setFocusable(true);v.setClickable(true);v.setOnFocusChangeListener((view,f)->view.setBackground(background(f)));
                ImageView icon=new ImageView(getContext());icon.setImageDrawable(new StorageIcon(b.getBoxId()));v.addView(icon,new LinearLayout.LayoutParams(dp(35),dp(35)));
                LinearLayout labels=new LinearLayout(getContext());labels.setOrientation(LinearLayout.VERTICAL);labels.setPadding(dp(12),0,0,0);
                String name=b.getName(),sub="";if(b.getBoxId()==Box.ID.NETWORK){name="Browse network";sub="Find shared folders";}else if(b.getBoxId()==Box.ID.FOLDERS){name="Internal storage";}else if(b.getBoxId()==Box.ID.VIDEOS_BY_LISTS){name="Playlists";sub="Browse saved playlists";}else{int at=name.indexOf('(');if(at>0){sub=name.substring(at+1).replace(")","").trim();name=name.substring(0,at).trim();}name=name.replaceFirst("^[^:]+:\\s*","");}
                TextView title=text(name,15);title.setMaxLines(2);labels.addView(title);if(!sub.isEmpty()){TextView detail=text(sub,12);detail.setTextColor(0xffb4cbe0);detail.setMaxLines(2);labels.addView(detail);}v.addView(labels,new LinearLayout.LayoutParams(0,-2,1));v.setContentDescription(name+" "+sub);v.setOnClickListener(view->click.open(new Presenter.ViewHolder(view),b));
            }
        }
        @Override public void onViewRecycled(Holder h){if(h.presenter!=null)h.presenter.onUnbindViewHolder(h.card);else if(h.itemView instanceof ViewGroup){ViewGroup group=(ViewGroup)h.itemView;for(int i=0;i<group.getChildCount();i++)if(group.getChildAt(i) instanceof RecyclerView)((RecyclerView)group.getChildAt(i)).setAdapter(null);}}
    }
    class RailAdapter extends RecyclerView.Adapter<Holder>{
        final List<Entry> entries;final PreviewCardPresenter pr=new PreviewCardPresenter(PreviewCardPresenter.Style.CONTINUE);
        RailAdapter(List<Entry> e){entries=e;setHasStableIds(true);}
        public long getItemId(int p){return entries.get(p).key().hashCode();}public int getItemCount(){return entries.size();}
        public Holder onCreateViewHolder(ViewGroup p,int type){Presenter.ViewHolder card=pr.onCreateViewHolder(p);Holder h=new Holder(card.view);h.card=card;h.presenter=pr;RecyclerView.LayoutParams lp=new RecyclerView.LayoutParams(dp(172),dp(105));lp.rightMargin=dp(12);h.itemView.setLayoutParams(lp);return h;}
        public void onBindViewHolder(Holder h,int p){Entry e=entries.get(p);pr.onBindViewHolder(h.card,e.media);h.itemView.setTag(e.key());h.itemView.setOnClickListener(v->click.open(h.card,e.media));}
        public void onViewRecycled(Holder h){pr.onUnbindViewHolder(h.card);}
    }
    static final class StorageIcon extends android.graphics.drawable.Drawable {
        final Box.ID id;final android.graphics.Paint paint=new android.graphics.Paint(3);
        StorageIcon(Box.ID id){this.id=id;paint.setColor(0xffb7d7f5);paint.setStyle(android.graphics.Paint.Style.STROKE);paint.setStrokeWidth(1.7f);paint.setStrokeCap(android.graphics.Paint.Cap.ROUND);}
        public void draw(android.graphics.Canvas c){c.save();c.translate(getBounds().left,getBounds().top);c.scale(getBounds().width()/32f,getBounds().height()/32f);
            if(id==Box.ID.VIDEOS_BY_LISTS){for(int y=7;y<29;y+=8){c.drawCircle(4,y,1,paint);c.drawLine(10,y,29,y,paint);}}
            else if(id==Box.ID.NETWORK){for(int y=3;y<25;y+=14){c.drawRoundRect(2,y,30,y+10,2,2,paint);c.drawCircle(7,y+5,1,paint);}}
            else {c.drawRoundRect(4,5,28,27,3,3,paint);c.drawLine(4,20,28,20,paint);c.drawCircle(23,24,1,paint);}
            c.restore();}
        public void setAlpha(int a){paint.setAlpha(a);}public void setColorFilter(android.graphics.ColorFilter f){paint.setColorFilter(f);}public int getOpacity(){return android.graphics.PixelFormat.TRANSLUCENT;}
    }
}
