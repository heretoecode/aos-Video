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
    private boolean restoringFocus, switchingTab;
    private android.content.SharedPreferences preferences;
    private int focusGeneration;
    private static final class FocusAnchor {
        String cell, child; int position, inner;
    }
    private String cellKey(Cell c){return c.type+":"+(c.type==POSTER?((Entry)c.value).key():c.type==STORAGE?((Box)c.value).getBoxId()+":"+((Box)c.value).getPath():c.title);}
    private boolean inside(View view,View ancestor){while(view!=null){if(view==ancestor)return true;android.view.ViewParent p=view.getParent();view=p instanceof View?(View)p:null;}return false;}
    private final class FocusRecycler extends PreviewFocusRecycler {
        final boolean horizontal;
        FocusRecycler(Context c,boolean horizontal){super(c);this.horizontal=horizontal;}
        @Override protected boolean focusablePosition(int p){if(horizontal)return true;if(p<0||p>=cells.size())return false;Cell c=cells.get(p);return c.type!=NOTICE&&(c.type!=HEADER||Boolean.TRUE.equals(c.value));}
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
        if(restoringFocus||switchingTab)return;
        View focused=list.findFocus();if(focused==null)return;
        View item=list.findContainingItemView(focused);if(item==null)return;
        int pos=list.getChildAdapterPosition(item);if(pos<0||pos>=cells.size())return;
        FocusAnchor a=new FocusAnchor();a.cell=cellKey(cells.get(pos));a.position=pos;
        a.child=focused.getTag() instanceof String?(String)focused.getTag():null;
        if(cells.get(pos).type==RAIL){List<Entry> entries=(List<Entry>)cells.get(pos).value;for(int i=0;i<entries.size();i++)if(entries.get(i).key().equals(a.child)){a.inner=i;break;}}
        anchors[tab]=a;
    }
    @Override public void requestChildFocus(View child,View focused){super.requestChildFocus(child,focused);if(list!=null){rememberFocus();if(tab==1||tab==2){View item=list.findContainingItemView(focused);int p=item==null?-1:list.getChildAdapterPosition(item);if(p>=0&&p<cells.size()&&cells.get(p).value instanceof Entry){lastArtwork[tab]=((Entry)cells.get(p).value).backdrop;artwork.accept(lastArtwork[tab]);}}}}
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
    private final Map<String,Integer> quietOrder=new HashMap<>();
    private PreviewDiscovery discovery=new PreviewDiscovery();
    private java.util.concurrent.ExecutorService worker;
    private java.util.function.Consumer<android.net.Uri> artwork=uri->{};
    private final boolean[] ascending={false,false,false};
    private final boolean[] listMode={false,false,false};
    private int featuredIndex;
    private final android.net.Uri[] lastArtwork=new android.net.Uri[4];
    private final Map<String,String> featuredReasons=new HashMap<>();
    private List<Entry> featuredCandidates(){List<List<Entry>> sources=new ArrayList<>();featuredReasons.clear();if(preferences==null||preferences.getBoolean("preview_featured_recent",true))sources.add(snapshot.recent);else sources.add(Collections.emptyList());sources.add(preferences!=null&&preferences.getBoolean("preview_featured_trending",true)?discovery.matches(snapshot,true):Collections.emptyList());sources.add(preferences!=null&&preferences.getBoolean("preview_featured_popular",true)?discovery.matches(snapshot,false):Collections.emptyList());List<Entry> result=new ArrayList<>();Set<String> seen=new HashSet<>();String[] reasons={"RECENTLY ADDED","TRENDING ON TRAKT · IN YOUR LIBRARY","POPULAR ON TRAKT · IN YOUR LIBRARY"};for(int i=0;i<12;i++)for(int n=0;n<sources.size();n++){List<Entry> source=sources.get(n);if(i<source.size()){Entry e=source.get(i);if(seen.add(e.key())){result.add(e);featuredReasons.put(e.key(),reasons[n]);}}}if(result.isEmpty()){List<Entry> local=new ArrayList<>(snapshot.movies);local.addAll(snapshot.shows);for(Entry e:local)if(seen.add(e.key())){result.add(e);featuredReasons.put(e.key(),"IN YOUR LIBRARY");if(result.size()==8)break;}}return result.subList(0,Math.min(8,result.size()));}
    private final PreviewLibraryColumns[] columns=new PreviewLibraryColumns[3];
    private long lastInteraction;
    private final Runnable rotateFeatured=new Runnable(){public void run(){
        if(isAttachedToWindow()){
            boolean heroFocus=findFocus()!=null&&findFocus().getTag() instanceof String&&((String)findFocus().getTag()).startsWith("hero:");
            if(tab==0&&hasWindowFocus()&&isShown()&&!list.canScrollVertically(-1)&&!heroFocus&&android.os.SystemClock.elapsedRealtime()-lastInteraction>=30000&&featuredCandidates().size()>1){featuredIndex++;render();}
            postDelayed(this,30000);
        }
    }};
    private Runnable ready=()->{};
    public void setReadyListener(Runnable listener){ready=listener;if(loaded)post(ready);}
    public boolean hasLoadedSnapshot(){return loaded;}
    public void setArtworkListener(java.util.function.Consumer<android.net.Uri> listener){artwork=listener;updateArtwork();}
    public void setDiscovery(PreviewDiscovery value){requestedDiscovery=true;discovery=value;render();}
    private boolean requestedDiscovery;
    private final android.content.SharedPreferences.OnSharedPreferenceChangeListener homeSettings=(prefs,key)->{if(key!=null&&(key.startsWith("preview_featured_")||key.equals("preview_home_rows41")||key.equals("preview_accent41")))post(()->{if(isAttachedToWindow())render();});};
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();preferences.registerOnSharedPreferenceChangeListener(homeSettings);requestDiscovery();lastInteraction=android.os.SystemClock.elapsedRealtime();postDelayed(rotateFeatured,30000);}
    private void requestDiscovery(){if(requestedDiscovery||!isAttachedToWindow()||snapshot.movies.isEmpty()&&snapshot.shows.isEmpty())return;requestedDiscovery=true;
        worker=java.util.concurrent.Executors.newSingleThreadExecutor();worker.execute(()->{try{PreviewDiscovery result=PreviewDiscovery.load(getContext().getApplicationContext());post(()->{if(isAttachedToWindow())setDiscovery(result);});}finally{worker.shutdown();}});}
    @Override protected void onDetachedFromWindow(){removeCallbacks(rotateFeatured);if(worker!=null)worker.shutdownNow();preferences.unregisterOnSharedPreferenceChangeListener(homeSettings);super.onDetachedFromWindow();}
    private Entry featured(){List<Entry> entries=tab==1?snapshot.movies:tab==2?snapshot.shows:featuredCandidates();return entries.isEmpty()?null:entries.get(Math.floorMod(featuredIndex,entries.size()));}
    private void updateArtwork(){if((tab==1||tab==2)&&loaded){if(lastArtwork[tab]!=null){artwork.accept(lastArtwork[tab]);return;}Entry first=featured();artwork.accept(first==null?null:first.backdrop);return;}Entry entry=featured();artwork.accept(tab==3||entry==null?null:entry.backdrop);}
    public static String displayName(Entry e){return e.media instanceof Episode?((Episode)e.media).getShowName():e.media.getName();}
    private void open(Entry e,View v){click.open(new Presenter.ViewHolder(v),e.media);}
    private void play(Entry e,View v){
        if(e.media instanceof Tvshow){List<Entry> episodes=new ArrayList<>();for(Entry ep:snapshot.episodes)if(ep.show==e.show)episodes.add(ep);Entry next=PreviewSeriesJourney.select(getContext(),episodes).episode;if(next!=null){play(next,v);return;}}
        if(e.media instanceof Video && getContext() instanceof android.app.Activity){
            com.archos.mediacenter.video.utils.PlayUtils.startVideo((android.app.Activity)getContext(),(Video)e.media,com.archos.mediacenter.video.player.PlayerActivity.RESUME_FROM_LAST_POS,false,-1,null,-1);
        }else open(e,v);
    }
    private static final int HEADER=0, POSTER=1, RAIL=2, STORAGE=3, HERO=4, NOTICE=5, LIST=6, SCAN=7, CUSTOMISE=8;
    static class Cell {
        int type; String title; Object value;
        Cell(int type,String title,Object value) {this.type=type;this.title=title;this.value=value;}
    }
    public PreviewPages(Context c,Click click) {
        super(c); this.click=click; setBackgroundColor(Color.TRANSPARENT);setFocusable(true);setFocusableInTouchMode(true);setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
        list=new FocusRecycler(c,false); list.setClipToPadding(false); list.setPadding(dp(28),dp(10),dp(28),dp(12));
        layout=new GridLayoutManager(c,24); layout.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup(){@Override public int getSpanSize(int p){return cells.get(p).type==POSTER?(tab<3&&listMode[tab]?24:4):cells.get(p).type==STORAGE?6:24;}});
        adapter.setHasStableIds(true);
        list.addOnScrollListener(new RecyclerView.OnScrollListener(){@Override public void onScrolled(RecyclerView rv,int dx,int dy){notifyScroll();}});list.setLayoutManager(layout); list.setAdapter(adapter); list.setItemAnimator(null);
        addView(list,new FrameLayout.LayoutParams(-1,-1));
        preferences=androidx.preference.PreferenceManager.getDefaultSharedPreferences(c);
        if(preferences.getBoolean("remember_library_views",true))for(int i=1;i<=2;i++){
            String k="preview_library_"+i+"_";sorts[i]=Math.max(0,Math.min(4,preferences.getInt(k+"sort",0)));genres[i]=preferences.getString(k+"genre","");years[i]=preferences.getInt(k+"year",0);ascending[i]=preferences.getBoolean(k+"ascending",false);listMode[i]=preferences.getBoolean(k+"list",false);
        }
        columns[1]=new PreviewLibraryColumns(c,false);columns[2]=new PreviewLibraryColumns(c,true);
        render();
        if(PreviewLibraryLoader.memoryCache()==null){java.util.concurrent.ExecutorService cacheWorker=java.util.concurrent.Executors.newSingleThreadExecutor();cacheWorker.execute(()->{try{Snapshot previous=PreviewLibraryLoader.readCache(c.getApplicationContext());post(()->{if(!loaded&&previous!=null)setSnapshot(previous);});}finally{cacheWorker.shutdown();}});}
    }
    private java.util.function.Consumer<Boolean> scrollListener=value->{};
    public void setScrollListener(java.util.function.Consumer<Boolean> listener){scrollListener=listener;notifyScroll();}
    private void notifyScroll(){scrollListener.accept(list.canScrollVertically(-1));}
    public boolean atTop() {
        View focused=list.findFocus(); if(focused==null)return isFocused()&&!restoringFocus;
        View item=list.findContainingItemView(focused);if(item==null)return false;
        int p=list.getChildAdapterPosition(item);if(p<0||p>=cells.size())return false;
        Cell cell=cells.get(p);
        if(tab==0)return cell.type==HERO && item.getTop()>=list.getPaddingTop() || cell.type==CUSTOMISE&&cells.stream().noneMatch(c->c.type==HERO)&&!list.canScrollVertically(-1);
        if(tab==1||tab==2)return cell.type==HEADER&&Boolean.TRUE.equals(cell.value)&&focused.getTag() instanceof String&&((String)focused.getTag()).startsWith("control:")&&!list.canScrollVertically(-1)
                ||cell.type==HERO&&item.getTop()>=list.getPaddingTop();
        if(cell.type==SCAN)return !list.canScrollVertically(-1);
        if(cell.type!=STORAGE)return false;
        int first=0;while(first<cells.size()&&cells.get(first).type!=STORAGE)first++;
        return cells.stream().noneMatch(c->c.type==SCAN)&&first<cells.size()&&layout.getSpanSizeLookup().getSpanGroupIndex(p,24)==layout.getSpanSizeLookup().getSpanGroupIndex(first,24)&&!list.canScrollVertically(-1);
    }
    public void setTab(int tab) {
        if(this.tab==tab)return;
        rememberFocus();scrollStates[this.tab]=layout.onSaveInstanceState();
        quietOrder.clear();switchingTab=true;this.tab=tab;featuredIndex=0;render();switchingTab=false;
        if(scrollStates[tab]!=null)layout.onRestoreInstanceState(scrollStates[tab]);else list.scrollToPosition(0);
    }
    public void setSnapshot(Snapshot s) { if(s==null)return;
        quietOrder.clear();if(loaded&&(tab==1||tab==2))for(Cell cell:cells)if(cell.type==POSTER)quietOrder.put(((Entry)cell.value).key(),quietOrder.size());
        if(loaded&&tab==0){String featuredKey=featured()==null?null:featured().key();if(featuredKey!=null)for(int i=0;i<Math.min(5,s.recent.size());i++)if(s.recent.get(i).key().equals(featuredKey)){featuredIndex=i;break;}}
        snapshot=s;loaded=true;render();post(ready);postDelayed(this::requestDiscovery,750);
    }
    private static void keepOrder(List<Entry> old,List<Entry> current){Map<String,Integer> rank=new HashMap<>();for(Entry e:old)rank.put(e.key(),rank.size());current.sort(Comparator.comparingInt(e->rank.getOrDefault(e.key(),Integer.MAX_VALUE)));}
    public void setFiles(List<Box> f) {
        boolean same=f.size()==files.size();
        for(int i=0;same && i<f.size();i++)same=f.get(i).getBoxId()==files.get(i).getBoxId() && Objects.equals(f.get(i).getName(),files.get(i).getName()) && Objects.equals(f.get(i).getPath(),files.get(i).getPath());
        if(same)return;files=f;if(tab==3)render();
    }
    private void header(String title,boolean controls) {cells.add(new Cell(HEADER,title,controls));}
    private void rail(String title,List<Entry> items) {if(items.isEmpty())return; header(title,false);cells.add(new Cell(RAIL,title,new ArrayList<>(items.subList(0,Math.min(30,items.size())))));}
    private void persistViews(){
        if(preferences==null||!preferences.getBoolean("remember_library_views",true)||tab<1||tab>2)return;
        String k="preview_library_"+tab+"_";preferences.edit().putInt(k+"sort",sorts[tab]).putString(k+"genre",genres[tab]).putInt(k+"year",years[tab]).putBoolean(k+"ascending",ascending[tab]).putBoolean(k+"list",listMode[tab]).apply();
    }
    @Override public boolean dispatchKeyEvent(KeyEvent event){
        if(tab==0&&event.getAction()==KeyEvent.ACTION_DOWN&&(event.getKeyCode()==KeyEvent.KEYCODE_DPAD_LEFT||event.getKeyCode()==KeyEvent.KEYCODE_DPAD_RIGHT)){
            View focused=findFocus();String tag=focused==null?"":String.valueOf(focused.getTag());if(event.getKeyCode()==KeyEvent.KEYCODE_DPAD_LEFT&&tag.equals("hero:play")||event.getKeyCode()==KeyEvent.KEYCODE_DPAD_RIGHT&&tag.equals("hero:info")){featuredIndex+=event.getKeyCode()==KeyEvent.KEYCODE_DPAD_LEFT?-1:1;lastInteraction=android.os.SystemClock.elapsedRealtime();render();return true;}}
        if(event.getAction()==KeyEvent.ACTION_DOWN)lastInteraction=android.os.SystemClock.elapsedRealtime();
        if((tab==1||tab==2)&&event.getAction()==KeyEvent.ACTION_DOWN&&event.getKeyCode()==KeyEvent.KEYCODE_DPAD_UP){
            View focused=list.findFocus(),item=focused==null?null:list.findContainingItemView(focused);
            int p=item==null?-1:list.getChildAdapterPosition(item);
            if(p>=0&&p<cells.size()){
                Cell c=cells.get(p);int header=-1,first=-1;
                for(int i=0;i<cells.size();i++){if(cells.get(i).type==HEADER&&Boolean.TRUE.equals(cells.get(i).value))header=i;if(cells.get(i).type==POSTER){first=i;break;}}
                boolean firstRow=first>=0&&c.type==POSTER&&layout.getSpanSizeLookup().getSpanGroupIndex(p,24)==layout.getSpanSizeLookup().getSpanGroupIndex(first,24);
                if(firstRow&&header>=0){FocusAnchor a=new FocusAnchor();a.cell=cellKey(cells.get(header));a.position=header;a.child="control:0";anchors[tab]=a;holdFocus();layout.scrollToPositionWithOffset(0,0);restoreFocus();return true;}
                if((c.type==HEADER||c.type==HERO)&&list.canScrollVertically(-1)){layout.scrollToPositionWithOffset(0,0);return true;}
            }
        }
        if(tab==0&&event.getAction()==KeyEvent.ACTION_DOWN&&event.getKeyCode()==KeyEvent.KEYCODE_DPAD_UP){
            View focused=list.findFocus(),item=focused==null?null:list.findContainingItemView(focused);
            if(item!=null){int p=list.getChildAdapterPosition(item);if(p>=0&&p<cells.size()){
                if(cells.get(p).type==HERO&&item.getTop()<list.getPaddingTop()){layout.scrollToPositionWithOffset(0,0);return true;}
                if(cells.get(p).type==RAIL){boolean first=true;for(int i=0;i<p;i++)if(cells.get(i).type==RAIL)first=false;
                    if(first){FocusAnchor a=new FocusAnchor();a.cell=HERO+":Featured";a.child="hero:play";anchors[tab]=a;holdFocus();layout.scrollToPositionWithOffset(0,0);restoreFocus();return true;}}
            }}
        }
        return super.dispatchKeyEvent(event);
    }
    private void render() {
        persistViews();
        rememberFocus();boolean preserve=hasFocus();
        if(preserve)holdFocus();
        Object state=layout.onSaveInstanceState(); cells.clear();
        if(tab==0) {
            if(featured()!=null)cells.add(new Cell(HERO,"Featured",featured()));
            List<Entry> continuing=new ArrayList<>(snapshot.continuingMovies);continuing.addAll(snapshot.continuingShows);continuing.sort(Comparator.comparingLong((Entry e)->e.playedAt).reversed());
            PreviewHomeRows home=new PreviewHomeRows(getContext());Set<String> continuingKeys=new HashSet<>();for(Entry e:continuing)continuingKeys.add(e.key());continuing.removeIf(home::dismissed);
            for(PreviewHomeRows.Row row:home.rows){if(!row.visible)continue;List<Entry> entries=new ArrayList<>();String name=row.name;
                switch(row.id){case "continue":entries=continuing;break;case "recent":for(Entry e:snapshot.recent)if(!continuingKeys.contains(e.key()))entries.add(e);break;case "trending":entries=discovery.matches(snapshot,true);break;case "popular":entries=discovery.matches(snapshot,false);break;case "watched":entries=snapshot.watched;break;case "similar":Entry seed=snapshot.played.isEmpty()?null:snapshot.played.get(0);if(seed!=null){entries=PreviewDiscovery.similar(seed,snapshot);name="Because You Watched "+displayName(seed);}break;default:entries=home.members(row,snapshot);}
                rail(name,entries);
            }
            cells.add(new Cell(CUSTOMISE,"Customise Home",null));
            if(loaded&&snapshot.movies.isEmpty()&&snapshot.shows.isEmpty()&&snapshot.recent.isEmpty()) header("Your library is empty — add media through Network & files",false);
        } else if(tab==1||tab==2) {
            if(featured()!=null)cells.add(new Cell(HERO,tab==1?"Movies":"TV Shows",featured()));else header(tab==1?"Movies":"TV Shows",false);

            header(tab==1?"Movie library":"TV show library",true);
            List<Entry> entries=filtered(); for(Entry e:entries)cells.add(new Cell(POSTER,"",e));
            if(loaded&&entries.isEmpty())header("No matching titles",false);
        } else {
            header("Network & files",false);cells.add(new Cell(SCAN,"Scan Library",null));
            for(int group=0;group<3;group++){boolean heading=false;for(Box box:files){int g=box.getBoxId()==Box.ID.NETWORK?1:box.getBoxId()==Box.ID.VIDEOS_BY_LISTS?2:0;if(g!=group)continue;if(!heading){header(group==0?"Local storage":group==1?"Network":"Playlists",false);heading=true;}cells.add(new Cell(STORAGE,"",box));}}

        }
        if(!loaded&&tab!=3)header(tab==0?"Home":tab==1?"Movies":"TV Shows",false);
        updateArtwork();adapter.notifyDataSetChanged(); if(state!=null)layout.onRestoreInstanceState((android.os.Parcelable)state);if(preserve)restoreFocus();list.post(this::notifyScroll);
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
        entries=sortEntries(entries,sorts[tab],ascending[tab],discovery);if(listMode[tab]&&columns[tab]!=null)entries=columns[tab].sort(entries);if(!quietOrder.isEmpty())entries.sort(Comparator.comparingInt(e->quietOrder.getOrDefault(e.key(),Integer.MAX_VALUE)));return entries;
    }
    private String[] sortLabels(){return new String[]{"Date Added","Title",tab==2?"Air Date":"Release Date","Trakt Trending","Trakt Popular"};}
    private void sort(){
        String[] names=sortLabels();if(!discovery.available){names[3]+=" — unavailable";names[4]+=" — unavailable";}
        PreviewDialog.choose(getContext(),"Sort",names,sorts[tab],n->{
            if(n>=3&&!discovery.available)return;columns[tab].clearSort();sorts[tab]=n;ascending[tab]=n==1;render();
        });
    }
    private void filter(){PreviewDialog.choose(getContext(),"Filters",new String[]{"Genre"+(genres[tab].isEmpty()?"":": "+genres[tab]),"Year"+(years[tab]==0?"":": "+years[tab]),"Clear filters"},-1,n->{
        if(n==2){genres[tab]="";years[tab]=0;render();return;}
        TreeSet<String> values=new TreeSet<>(); for(Entry e:source())if(n==0){for(String g:e.genres.split("[|,;/]"))if(!g.trim().isEmpty())values.add(g.trim());}else if(e.year()>0)values.add(String.valueOf(e.year()));
        List<String> options=new ArrayList<>();options.add("All");options.addAll(values);
        PreviewDialog.choose(getContext(),n==0?"Genre":"Year",options.toArray(new String[0]),n==0?(genres[tab].isEmpty()?0:options.indexOf(genres[tab])):(years[tab]==0?0:options.indexOf(String.valueOf(years[tab]))),i->{if(n==0)genres[tab]=i==0?"":options.get(i);else years[tab]=i==0?0:Integer.parseInt(options.get(i));render();});
    });}
    private TextView text(String value,int size){TextView t=new TextView(getContext());t.setText(value);t.setTextColor(Color.WHITE);t.setTextSize(size);return t;}
    private GradientDrawable background(boolean focus){GradientDrawable d=new GradientDrawable();d.setColor(focus?0x60416b84:0xc00b1b29);d.setCornerRadius(dp(5));d.setStroke(dp(focus?2:1),focus?PreviewAccent.color(getContext()):0xff304b60);return d;}
    private TextView button(String name,Runnable action){TextView b=text(name,13);com.archos.mediacenter.video.leanback.PreviewIcon.apply(b,name,16);b.setGravity(Gravity.CENTER);b.setPadding(dp(14),dp(9),dp(14),dp(9));b.setFocusable(true);b.setFocusableInTouchMode(true);b.setClickable(true);b.setBackground(background(false));b.setOnFocusChangeListener((v,f)->v.setBackground(background(f)));b.setOnClickListener(v->{quietOrder.clear();action.run();});return b;}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    class Holder extends RecyclerView.ViewHolder {
        Presenter presenter; Presenter.ViewHolder card;
        Holder(View v){super(v);}
    }
    class PageAdapter extends RecyclerView.Adapter<Holder> {
        private void refreshColumns(){quietOrder.clear();render();}
        @Override public long getItemId(int p){Cell c=cells.get(p);String key=c.type==POSTER?((Entry)c.value).key():c.type==STORAGE?((Box)c.value).getBoxId()+":"+((Box)c.value).getPath():c.title;return ((long)c.type<<32) | (key.hashCode() & 0xffffffffL);}
        @Override public int getItemCount(){return cells.size();}
        @Override public int getItemViewType(int p){return cells.get(p).type==POSTER&&tab<3&&listMode[tab]?LIST:cells.get(p).type;}
        @Override public Holder onCreateViewHolder(ViewGroup parent,int type){
            if(type==LIST){LinearLayout row=columns[tab].newRow();RecyclerView.LayoutParams lp=new RecyclerView.LayoutParams(-1,dp(44));lp.bottomMargin=dp(1);row.setLayoutParams(lp);return new Holder(row);}
            if(type==POSTER){PreviewCardPresenter pr=new PreviewCardPresenter(type==LIST?PreviewCardPresenter.Style.LIST:PreviewCardPresenter.Style.POSTER); Presenter.ViewHolder card=pr.onCreateViewHolder(parent);Holder h=new Holder(card.view);h.presenter=pr;h.card=card;RecyclerView.LayoutParams lp=new RecyclerView.LayoutParams(-1,-2);lp.setMargins(0,0,dp(10),dp(15));h.itemView.setLayoutParams(lp);return h;}
            LinearLayout v=new LinearLayout(getContext());v.setGravity(Gravity.CENTER_VERTICAL);v.setPadding(0,dp(6),0,dp(6));v.setLayoutParams(new RecyclerView.LayoutParams(-1,-2));return new Holder(v);
        }
        @Override public void onBindViewHolder(Holder h,int p){Cell c=cells.get(p);
            if(c.type==POSTER&&h.itemView instanceof LinearLayout){Entry e=(Entry)c.value;columns[tab].bind((LinearLayout)h.itemView,e);h.itemView.setTag(e.key());h.itemView.setOnClickListener(v->click.open(new Presenter.ViewHolder(v),e.media));h.itemView.setOnLongClickListener(v->{contextMenu(e,v,false);return true;});return;}
            if(c.type==POSTER){Entry e=(Entry)c.value;h.presenter.onBindViewHolder(h.card,e.media);PreviewCardPresenter.bindSecondary(h.card,e);h.itemView.setTag(e.key());h.itemView.setOnClickListener(v->click.open(h.card,e.media));h.itemView.setOnLongClickListener(v->{contextMenu(e,v,false);return true;});return;}
            LinearLayout v=(LinearLayout)h.itemView;v.removeAllViews();v.setFocusable(false);v.setOnClickListener(null);v.setBackground(null);v.setOrientation(LinearLayout.HORIZONTAL);v.setPadding(0,dp(6),0,dp(6));v.setLayoutParams(new RecyclerView.LayoutParams(-1,-2));
            if(c.type==HERO){Entry e=(Entry)c.value;v.setOrientation(LinearLayout.VERTICAL);v.setGravity(Gravity.TOP);v.setPadding(0,dp(10),0,dp(8));
                v.setMinimumHeight(dp(tab==0?244:76));
                v.setLayoutParams(new RecyclerView.LayoutParams(-1,tab==0?dp(244):android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
                if(tab==0){TextView featured=text(featuredReasons.getOrDefault(e.key(),"IN YOUR LIBRARY"),11);featured.setTextColor(PreviewAccent.color(getContext()));v.addView(featured);
                    TextView title=text(displayName(e),30);title.setMaxLines(2);title.setIncludeFontPadding(false);title.setLineSpacing(0,.92f);title.setEllipsize(android.text.TextUtils.TruncateAt.END);title.setTypeface(null,android.graphics.Typeface.BOLD);v.addView(title,new LinearLayout.LayoutParams(dp(420),dp(64)));OfficialTitleArtwork.bind(title,e.media,false);
                    String meta=e.year()>0?String.valueOf(e.year()):"";
                    if(e.media instanceof Video){Video video=(Video)e.media;if(video.getDurationMs()>0)meta+="   "+video.getDurationMs()/60000+" min";}
                    if(!e.genres.isEmpty())meta+=(meta.isEmpty()?"":"   ·   ")+e.genres.replace("|"," · ");if(e.media instanceof Episode){Episode ep=(Episode)e.media;meta+="   ·   S"+ep.getSeasonNumber()+" E"+ep.getEpisodeNumber();}TextView metadata=text(meta,12);metadata.setSingleLine(true);metadata.setEllipsize(android.text.TextUtils.TruncateAt.END);v.addView(metadata,new LinearLayout.LayoutParams(dp(470),dp(22)));String plot=e.media instanceof Tvshow?((Tvshow)e.media).getPlot():e.media instanceof Video?((Video)e.media).getDescriptionBody():"";
                    TextView description=text(plot==null?"":plot,13);description.setIncludeFontPadding(false);description.setMaxLines(3);description.setEllipsize(android.text.TextUtils.TruncateAt.END);v.addView(description,new LinearLayout.LayoutParams(dp(450),dp(47)));
                    LinearLayout actions=new LinearLayout(getContext());actions.setPadding(0,dp(6),0,0);
                    TextView play=button((e.media instanceof Video&&((Video)e.media).getResumeMs()>0?"Resume":"Play"),()->play(e,v));play.setTag("hero:play");actions.addView(play,new LinearLayout.LayoutParams(dp(116),-1));
                    TextView info=button("More Info",()->open(e,v));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,-2);lp.leftMargin=dp(10);info.setTag("hero:info");actions.addView(info,lp);
                    v.setAlpha(.7f);v.animate().alpha(1f).setDuration(220).start();View spacer=new View(getContext());v.addView(spacer,new LinearLayout.LayoutParams(1,0,1));v.addView(actions,new LinearLayout.LayoutParams(-2,dp(40)));
                    LinearLayout markers=new LinearLayout(getContext());markers.setGravity(Gravity.CENTER);markers.setPadding(0,dp(8),0,0);int count=featuredCandidates().size();for(int i=0;i<count;i++){boolean active=i==Math.floorMod(featuredIndex,count);View dot=new View(getContext());GradientDrawable shape=new GradientDrawable();shape.setCornerRadius(dp(3));shape.setColor(active?PreviewAccent.color(getContext()):0x7792aabd);dot.setBackground(shape);LinearLayout.LayoutParams marker=new LinearLayout.LayoutParams(dp(active?24:5),dp(5));marker.setMargins(dp(3),0,dp(3),0);markers.addView(dot,marker);}v.addView(markers,new LinearLayout.LayoutParams(-1,dp(19)));

                }else{TextView title=text(c.title,30);title.setTypeface(null,android.graphics.Typeface.BOLD);v.addView(title);v.addView(text(source().size()+ (tab==1?" movies":" shows"),13));}
            }
            else if(c.type==CUSTOMISE){v.setGravity(Gravity.CENTER);TextView custom=button("Customise Home",()->PreviewHomeRows.customise(getContext(),()->{render();}));custom.setTag("home:customise");v.addView(custom);}
            else if(c.type==SCAN){v.setOrientation(LinearLayout.VERTICAL);TextView scan=button("Scan Library",()->PreviewLibraryScan.request(getContext()));scan.setTag("scan:library");v.addView(scan,new LinearLayout.LayoutParams(dp(158),dp(40)));TextView description=text("Scan local storage and indexed network folders. Full Library Scan in Settings also retries unmatched descriptions.",12);description.setPadding(0,dp(8),0,dp(8));description.setTextColor(0xffaac1d1);v.addView(description);TextView progress=text("",13);progress.setTextColor(PreviewAccent.color(getContext()));v.addView(progress);progress.post(new Runnable(){public void run(){if(!progress.isAttachedToWindow())return;String status="";if(com.archos.mediaprovider.video.NetworkScannerReceiver.isScannerWorking())status=com.archos.mediaprovider.video.NetworkScannerServiceVideo.isDeleting()?"Updating network library · "+com.archos.mediaprovider.video.NetworkScannerServiceVideo.getRemainingDeletesCount()+" remaining":"Scanning indexed network folders · "+com.archos.mediaprovider.video.NetworkScannerServiceVideo.getFilesFoundCount()+" files found";else if(com.archos.mediaprovider.ImportState.VIDEO.isInitialImport()||com.archos.mediaprovider.ImportState.VIDEO.isRegularImport())status="Importing local library · "+com.archos.mediaprovider.ImportState.VIDEO.getNumberOfFilesRemainingToImport()+" remaining";else if(com.archos.mediaprovider.video.LoaderUtils.getScrapeInProgress())status="Identifying library titles · "+com.archos.mediascraper.AutoScrapeService.getNumberOfFilesRemainingToProcess()+" remaining";progress.setText(status);progress.setVisibility(status.isEmpty()?GONE:VISIBLE);progress.postDelayed(this,1000);}});}
            else if(c.type==NOTICE){v.setOrientation(LinearLayout.VERTICAL);TextView title=text(c.title,18);title.setTextColor(0xff8298aa);v.addView(title);TextView status=text((String)c.value,12);status.setTextColor(0xff8298aa);v.addView(status);v.setPadding(0,dp(12),0,dp(12));}
            else if(c.type==HEADER){
                if(Boolean.TRUE.equals(c.value)){
                    v.setOrientation(LinearLayout.VERTICAL);v.setPadding(0,dp(13),0,dp(18));LinearLayout controls=new LinearLayout(getContext());
                    controls.addView(button("Filters"+(genres[tab].isEmpty()?"":": "+genres[tab])+(years[tab]==0?"":" · "+years[tab])+"  ▾",()->filter()));
                    String order=listMode[tab]&&columns[tab].sortColumn!=null?(columns[tab].ascending?"Ascending":"Descending"):sorts[tab]==1?(ascending[tab]?"A → Z":"Z → A"):sorts[tab]>=3?(ascending[tab]?"Lowest ranked first":"Highest ranked first"):(ascending[tab]?"Oldest first":"Newest first");
                    for(TextView control:new TextView[]{button("Sort: "+(listMode[tab]&&columns[tab].sortColumn!=null?columns[tab].sortColumn.label:sortLabels()[sorts[tab]])+"  ▾",()->sort()),button(order+"  ▾",()->PreviewDialog.choose(getContext(),"Order",new String[]{"Ascending","Descending"},ascending[tab]?0:1,n->{ascending[tab]=n==0;if(listMode[tab])columns[tab].setAscending(n==0);render();})),button(listMode[tab]?"Grid view":"List view",()->{listMode[tab]=!listMode[tab];render();})}){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,-2);lp.leftMargin=dp(8);controls.addView(control,lp);}
                    if(listMode[tab]){TextView chooser=button("Columns",()->columns[tab].choose(this::refreshColumns));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,-2);lp.leftMargin=dp(8);controls.addView(chooser,lp);}
                    for(int i=0;i<controls.getChildCount();i++)controls.getChildAt(i).setTag("control:"+i);v.addView(controls);
                    if(listMode[tab])v.addView(columns[tab].header(this::refreshColumns));
                }else{TextView title=text(c.title,tab==3&&c.title.equals("Network & files")?30:19);title.setTextColor(0xff9ed4f7);v.addView(title);}
            }
            else if(c.type==RAIL){v.setPadding(0,0,0,dp(10));RecyclerView rail=new FocusRecycler(getContext(),true);rail.setLayoutManager(new LinearLayoutManager(getContext(),RecyclerView.HORIZONTAL,false));rail.setItemAnimator(null);rail.setAdapter(new RailAdapter((List<Entry>)c.value,"Continue Watching".equals(c.title)));v.addView(rail,new LinearLayout.LayoutParams(-1,dp(105)));}
            else if(c.type==STORAGE){Box b=(Box)c.value;v.setPadding(dp(12),dp(12),dp(12),dp(12));RecyclerView.LayoutParams lp=new RecyclerView.LayoutParams(-1,dp(76));lp.setMargins(0,0,dp(12),dp(12));v.setLayoutParams(lp);v.setBackground(PreviewDialog.surface(getContext(),false));v.setForeground(PreviewDialog.focus(getContext()));v.setDescendantFocusability(FOCUS_BLOCK_DESCENDANTS);v.setFocusable(true);v.setClickable(true);
                ImageView icon=new ImageView(getContext());icon.setImageDrawable(new StorageIcon(b.getBoxId()));v.addView(icon,new LinearLayout.LayoutParams(dp(35),dp(35)));
                LinearLayout labels=new LinearLayout(getContext());labels.setOrientation(LinearLayout.VERTICAL);labels.setPadding(dp(12),0,0,0);
                String name=b.getName(),sub="";if(b.getBoxId()==Box.ID.NETWORK){name="Browse network";sub="Find shared folders";}else if(b.getBoxId()==Box.ID.FOLDERS){name="Internal storage";}else if(b.getBoxId()==Box.ID.VIDEOS_BY_LISTS){name="Playlists";sub="Browse saved playlists";}else{int at=name.indexOf('(');if(at>0){sub=name.substring(at+1).replace(")","").trim();name=name.substring(0,at).trim();}name=name.replaceFirst("^[^:]+:\\s*","");}
                TextView title=text(name,13);title.setMaxLines(1);title.setEllipsize(android.text.TextUtils.TruncateAt.END);labels.addView(title);if(!sub.isEmpty()){TextView detail=text(sub,10);detail.setTextColor(0xffb4cbe0);detail.setMaxLines(2);labels.addView(detail);}v.addView(labels,new LinearLayout.LayoutParams(0,-2,1));v.setContentDescription(name+" "+sub);v.setOnClickListener(view->click.open(new Presenter.ViewHolder(view),b));
            }
        }
        @Override public void onViewRecycled(Holder h){if(h.presenter!=null)h.presenter.onUnbindViewHolder(h.card);else if(h.itemView instanceof LinearLayout&&h.itemView.isFocusable()&&tab>0&&tab<3)columns[tab].clear(h.itemView);else if(h.itemView instanceof ViewGroup){ViewGroup group=(ViewGroup)h.itemView;for(int i=0;i<group.getChildCount();i++)if(group.getChildAt(i) instanceof RecyclerView)((RecyclerView)group.getChildAt(i)).setAdapter(null);}}
    }
    class RailAdapter extends RecyclerView.Adapter<Holder>{
        final List<Entry> entries;final PreviewCardPresenter pr=new PreviewCardPresenter(PreviewCardPresenter.Style.CONTINUE);
        final boolean resume;RailAdapter(List<Entry> e,boolean resume){entries=e;this.resume=resume;setHasStableIds(true);}
        public long getItemId(int p){return entries.get(p).key().hashCode();}public int getItemCount(){return entries.size();}
        public Holder onCreateViewHolder(ViewGroup p,int type){Presenter.ViewHolder card=pr.onCreateViewHolder(p);Holder h=new Holder(card.view);h.card=card;h.presenter=pr;RecyclerView.LayoutParams lp=new RecyclerView.LayoutParams(dp(172),dp(105));lp.rightMargin=dp(12);h.itemView.setLayoutParams(lp);return h;}
        public void onBindViewHolder(Holder h,int p){Entry e=entries.get(p);pr.onBindViewHolder(h.card,e.media);PreviewCardPresenter.bindSecondary(h.card,e);h.itemView.setTag(e.key());h.itemView.setOnClickListener(v->{if(resume)play(e,v);else click.open(h.card,e.media);});h.itemView.setOnLongClickListener(v->{contextMenu(e,v,resume);return true;});}
        public void onViewRecycled(Holder h){pr.onUnbindViewHolder(h.card);}
    }
    private void contextMenu(Entry e,View view,boolean continuing){List<String> labels=new ArrayList<>(Arrays.asList("More Info","Add to Row"));if(continuing)labels.add("Dismiss from Continue Watching");PreviewDialog.choose(getContext(),displayName(e),labels.toArray(new String[0]),-1,n->{if(n==0)open(e,view);else if(n==1)PreviewHomeRows.add(getContext(),e,this::render);else{new PreviewHomeRows(getContext()).dismiss(e);render();}});}
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
