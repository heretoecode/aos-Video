package com.archos.mediacenter.video.leanback;

import android.content.Context;
import android.database.Cursor;
import com.archos.mediacenter.video.browser.loader.*;
import com.archos.mediacenter.video.browser.adapters.mappers.*;
import com.archos.mediacenter.video.browser.adapters.object.*;
import com.archos.mediaprovider.video.VideoStore;
import java.util.*;

/** Builds an immutable UI snapshot on the loader worker, never querying per card. */
public final class PreviewLibraryLoader extends AllVideosLoader {
    public static final int ID = 12001;
    public volatile Snapshot snapshot;
    private final AllTvshowsLoader showsQuery;
    public PreviewLibraryLoader(Context context) {
        super(context);
        // CursorLoader creates a Handler: construct on the same main thread as this loader.
        showsQuery = new AllTvshowsLoader(context) {
            @Override public String getSelection() {
                return super.getSelection().replace(com.archos.mediaprovider.video.LoaderUtils.HIDE_WATCHED_FILTER, "1");
            }
        };
    }
    @Override public String[] getProjection() {
        return concatTwoStringArrays(mProjection, new String[]{
            android.provider.MediaStore.Video.VideoColumns.WIDTH, android.provider.MediaStore.Video.VideoColumns.HEIGHT,
            VideoStore.Video.VideoColumns.ARCHOS_CALCULATED_BEST_AUDIOTRACK_FORMAT, VideoStore.Video.VideoColumns.ARCHOS_CALCULATED_VIDEO_FORMAT,
            VideoStore.Video.VideoColumns.SCRAPER_BACKDROP_LARGE_FILE, VideoStore.Video.VideoColumns.SCRAPER_BACKDROP_LARGE_URL,
            VideoStore.Video.VideoColumns.DATE_ADDED, VideoStore.Video.VideoColumns.SCRAPER_SHOW_ID,
            VideoStore.Video.VideoColumns.DATE_MODIFIED, VideoStore.Video.VideoColumns.ARCHOS_VIDEO_BITRATE,
            VideoStore.Video.VideoColumns.SCRAPER_M_RELEASE_DATE, VideoStore.Video.VideoColumns.SCRAPER_S_PREMIERED, VideoStore.Video.VideoColumns.SCRAPER_S_ONLINE_ID,
            VideoStore.Video.VideoColumns.SCRAPER_M_GENRES, VideoStore.Video.VideoColumns.SCRAPER_S_GENRES});
    }
    public static final class Entry implements java.io.Serializable {
        public final Base media;
        public final long added, show;
        public final String genres;
        public String secondary="";public boolean active;public long playedAt;
        public String releaseDate=""; public long onlineId; public transient android.net.Uri backdrop;
        public long bytes, runtime, modified, bitrate;
        public int episodes, seasons, knownSizes, files;
        public String resolution="", hdr="", audio="", codec="", path="", container="";
        public Entry(Base media, long added, long show, String genres) {
            this.media=media;this.playedAt=media instanceof Video?((Video)media).getLastPlayed():0; this.added=added; this.show=show; this.genres=genres == null ? "" : genres;
            if(media instanceof Movie) onlineId=((Movie)media).getOnlineId();
            if(media instanceof Video) backdrop=((Video)media).getPreviewBackdrop();
            if(media instanceof Video){Video v=(Video)media;files=1;bytes=Math.max(0,v.getSize());knownSizes=v.getSize()>0?1:0;runtime=Math.max(0,v.getDurationMs());path=v.getFilePath();
                int w=v.getMeasuredWidth(),h=v.getMeasuredHeight();resolution=w>=3840||h>=2160?"4K":w>=1728||h>=1040?"1080p":w>=1200||h>=720?"720p":w>0&&h>0?"SD":"";
                codec=com.archos.mediacenter.video.leanback.details.PreviewMediaInfo.format(v.getCalculatedVideoFormat());audio=com.archos.mediacenter.video.leanback.details.PreviewMediaInfo.format(v.getCalculatedBestAudioFormat());
                String name=v.getFilenameNonCryptic();int dot=name==null?-1:name.lastIndexOf('.');if(dot>=0&&name.length()-dot<=6)container=name.substring(dot+1).toUpperCase(Locale.ROOT);
            }
        }
        public String key() { return media instanceof Episode ? "s"+show : media instanceof Tvshow ? "s"+((Tvshow)media).getTvshowId() : "v"+((Video)media).getId(); }
        private void writeObject(java.io.ObjectOutputStream out)throws java.io.IOException{out.defaultWriteObject();out.writeObject(backdrop==null?null:backdrop.toString());}
        private void readObject(java.io.ObjectInputStream in)throws java.io.IOException,ClassNotFoundException{in.defaultReadObject();String uri=(String)in.readObject();backdrop=uri==null?null:android.net.Uri.parse(uri);}
        public int year() { return media instanceof Movie ? ((Movie)media).getYear() : media instanceof Tvshow ? ((Tvshow)media).getYear() : 0; }
    }
    public static final class Snapshot implements java.io.Serializable {
        public final List<Entry> episodes=new ArrayList<>();
        public final List<Entry> watched = new ArrayList<>();
        public final List<Entry> movies = new ArrayList<>(), shows = new ArrayList<>(), recent = new ArrayList<>(), played = new ArrayList<>(), continuingMovies = new ArrayList<>(), continuingShows = new ArrayList<>();
    }
    public static boolean watched(Video v) { return PreviewSeriesJourney.completed(v); }
    @Override public String getSelection() { return com.archos.mediaprovider.video.LoaderUtils.mustHideUserHiddenObjects() ? com.archos.mediaprovider.video.LoaderUtils.HIDE_USER_HIDDEN_FILTER : ""; }
    public static Entry next(List<Entry> episodes) {
        Comparator<Entry> order=Comparator.comparingInt((Entry e)->((Episode)e.media).getSeasonNumber()).thenComparingInt(e->((Episode)e.media).getEpisodeNumber()).thenComparingLong(e->((Video)e.media).getId());
        // Resume an unfinished episode before starting another. Completed entries never win.
        return episodes.stream().filter(e->!watched((Video)e.media) && ((Video)e.media).getResumeMs()>0)
            .min(order).orElseGet(()->episodes.stream().filter(e->!watched((Video)e.media) && ((Episode)e.media).getSeasonNumber()>0).min(order)
                .orElseGet(()->episodes.stream().filter(e->!watched((Video)e.media)).min(order).orElse(null)));
    }
    public static Snapshot build(List<Entry> videos, List<Entry> shows) {
        Snapshot s=new Snapshot(); s.shows.addAll(shows);
        Map<Long,List<Entry>> groups=new LinkedHashMap<>();
        for(Entry e:videos) {
            Video v=(Video)e.media;
            if(v instanceof Episode && e.show>0) {groups.computeIfAbsent(e.show,k->new ArrayList<>()).add(e);s.episodes.add(e);}
            else { s.recent.add(e); if(v instanceof Movie) { s.movies.add(e); if(!watched(v) && v.getResumeMs()>0) s.continuingMovies.add(e); } }
            if(v.getLastPlayed()>0) { s.played.add(e); if(watched(v))s.watched.add(e); }
        }
        for(List<Entry> group:groups.values()) {
            Entry n=next(group); long newest=group.stream().mapToLong(e->e.added).max().orElse(0);
            Entry recent=n!=null?n:group.get(0);
            Entry grouped=new Entry(recent.media,newest,recent.show,recent.genres);grouped.onlineId=recent.onlineId;grouped.releaseDate=recent.releaseDate;s.recent.add(grouped);
            if(n!=null) {
                if(group.stream().anyMatch(e->((Video)e.media).getLastPlayed()>0 || watched((Video)e.media))){n.playedAt=group.stream().mapToLong(e->e.playedAt).max().orElse(0);s.continuingShows.add(n);}
            }
        }
        for(Entry show:s.shows){show.files=0;show.bytes=0;show.runtime=0;show.knownSizes=0;show.modified=0;List<Entry> group=groups.get(show.show);if(group==null)continue;
            Set<String> episodeKeys=new HashSet<>();Set<Integer> seasonKeys=new HashSet<>();Set<String> resolutions=new TreeSet<>(),audios=new TreeSet<>(),codecs=new TreeSet<>();
            for(Entry e:group){Episode ep=(Episode)e.media;episodeKeys.add(ep.getSeasonNumber()+":"+ep.getEpisodeNumber());seasonKeys.add(ep.getSeasonNumber());show.files++;show.bytes+=e.bytes;show.knownSizes+=e.knownSizes;show.runtime+=e.runtime;show.modified=Math.max(show.modified,e.modified);
                if(!e.resolution.isEmpty())resolutions.add(e.resolution);if(!e.audio.isEmpty())audios.add(e.audio);if(!e.codec.isEmpty())codecs.add(e.codec);
            }
            show.episodes=group.size();show.seasons=seasonKeys.size();show.resolution=resolutions.size()==1?resolutions.iterator().next():resolutions.isEmpty()?"":"Mixed";show.audio=audios.size()==1?audios.iterator().next():audios.isEmpty()?"":"Mixed";show.codec=codecs.size()==1?codecs.iterator().next():codecs.isEmpty()?"":"Mixed";
        }
        s.recent.sort(Comparator.comparingLong((Entry e)->e.added).reversed());
        s.movies.sort(Comparator.comparingLong((Entry e)->e.added).reversed());s.shows.sort(Comparator.comparingLong((Entry e)->e.added).reversed());
        s.played.sort(Comparator.comparingLong((Entry e)->((Video)e.media).getLastPlayed()).reversed());
        s.watched.sort(Comparator.comparingLong((Entry e)->((Video)e.media).getLastPlayed()).reversed());
        Set<String> watchedSeen=new HashSet<>();s.watched.removeIf(e->!watchedSeen.add(e.key()));
        Set<String> seen=new HashSet<>(); s.played.removeIf(e->!seen.add(e.key()));
        s.continuingMovies.sort(Comparator.comparingLong((Entry e)->((Video)e.media).getLastPlayed()).reversed());
        s.continuingShows.sort(Comparator.comparingLong((Entry e)->groups.get(e.show).stream().mapToLong(x->((Video)x.media).getLastPlayed()).max().orElse(0)).reversed());
        return s;
    }
    private static final Object CACHE_LOCK=new Object();
    private static final java.util.concurrent.ExecutorService cacheWriter=java.util.concurrent.Executors.newSingleThreadExecutor();
    public static void warmCache(Context context){cacheWriter.execute(()->{Snapshot value=readCache(context);if(value!=null&&cached==null&&!com.archos.mediacenter.video.player.PrivateMode.isActive()){cachePrivate=false;cached=value;}});}
    public static volatile Snapshot cached;
    private static boolean cachePrivate;
    public static Snapshot memoryCache(){return cachePrivate==com.archos.mediacenter.video.player.PrivateMode.isActive()?cached:null;}
    public static Snapshot readCache(Context c){if(memoryCache()!=null)return memoryCache();if(com.archos.mediacenter.video.player.PrivateMode.isActive())return null;try(java.io.ObjectInputStream in=new java.io.ObjectInputStream(new java.io.BufferedInputStream(new java.io.FileInputStream(new java.io.File(c.getCacheDir(),"preview-library-v41"))))){Snapshot s=(Snapshot)in.readObject();for(List<Entry> list:java.util.Arrays.asList(s.movies,s.shows,s.recent,s.played,s.watched,s.continuingMovies,s.continuingShows,s.episodes))for(Entry e:list)if(e.media instanceof Video)e.backdrop=((Video)e.media).getPreviewBackdrop();return s;}catch(Exception unavailable){return null;}}
    private void writeCache(Snapshot value){if(com.archos.mediacenter.video.player.PrivateMode.isActive())return;synchronized(CACHE_LOCK){writeCacheLocked(value);}}
    private void writeCacheLocked(Snapshot value){android.util.AtomicFile file=new android.util.AtomicFile(new java.io.File(getContext().getCacheDir(),"preview-library-v41"));java.io.FileOutputStream stream=null;try{stream=file.startWrite();java.io.ObjectOutputStream out=new java.io.ObjectOutputStream(stream);out.writeObject(value);out.flush();file.finishWrite(stream);}catch(Exception failure){if(stream!=null)file.failWrite(stream);android.util.Log.d("NovaPreview","Snapshot cache unavailable",failure);}}

    private void applyJourneys(Snapshot s,List<Entry> videos){
        Map<Long,List<Entry>> groups=new LinkedHashMap<>();for(Entry e:videos)if(e.media instanceof Episode&&e.show>0)groups.computeIfAbsent(e.show,k->new ArrayList<>()).add(e);
        s.continuingShows.clear();
        for(Map.Entry<Long,List<Entry>> group:groups.entrySet()){
            PreviewSeriesJourney.Selection journey=PreviewSeriesJourney.select(getContext(),group.getValue());Entry candidate=journey.episode;
            if(candidate!=null&&journey.started){Episode ep=(Episode)candidate.media;candidate.playedAt=journey.activity;candidate.active=true;candidate.secondary=(PreviewSeriesJourney.resumable(ep)?"Resume · ":"Up Next · ")+"S"+ep.getSeasonNumber()+" E"+ep.getEpisodeNumber();s.continuingShows.add(candidate);
                for(Entry show:s.shows)if(show.show==group.getKey()){show.secondary=candidate.secondary;show.active=true;break;}
            }
        }
        s.continuingShows.sort(Comparator.comparingLong((Entry e)->e.playedAt).reversed());
    }
    @Override public Cursor loadInBackground() {
        long started=android.os.SystemClock.elapsedRealtime();
        Cursor c=super.loadInBackground();
        if(c==null) return null;
        try {
            List<Entry> videos=new ArrayList<>(), shows=new ArrayList<>();
            VideoCursorMapper mapper=new VideoCursorMapper(); mapper.bindColumns(c);
            int added=c.getColumnIndexOrThrow(VideoStore.Video.VideoColumns.DATE_ADDED);
            int show=c.getColumnIndexOrThrow(VideoStore.Video.VideoColumns.SCRAPER_SHOW_ID);
            int mg=c.getColumnIndexOrThrow(VideoStore.Video.VideoColumns.SCRAPER_M_GENRES), sg=c.getColumnIndexOrThrow(VideoStore.Video.VideoColumns.SCRAPER_S_GENRES);
            while(c.moveToNext()) { Video v=(Video)mapper.bind(c);
                String backdrop=c.getString(c.getColumnIndexOrThrow(VideoStore.Video.VideoColumns.SCRAPER_BACKDROP_LARGE_FILE));
                if(backdrop!=null && !backdrop.isEmpty()) v.setPreviewBackdrop(android.net.Uri.fromFile(new java.io.File(backdrop)).toString());
                else { String remote=c.getString(c.getColumnIndexOrThrow(VideoStore.Video.VideoColumns.SCRAPER_BACKDROP_LARGE_URL)); if(remote!=null && (remote.startsWith("https://") || remote.startsWith("http://")))v.setPreviewBackdrop(remote); }
                Entry entry=new Entry(v,c.getLong(added),c.getLong(show),c.getString(v instanceof Episode?sg:mg));
                entry.modified=c.getLong(c.getColumnIndexOrThrow(VideoStore.Video.VideoColumns.DATE_MODIFIED));entry.bitrate=c.getLong(c.getColumnIndexOrThrow(VideoStore.Video.VideoColumns.ARCHOS_VIDEO_BITRATE));
                entry.releaseDate=c.getString(c.getColumnIndexOrThrow(v instanceof Episode?VideoStore.Video.VideoColumns.SCRAPER_S_PREMIERED:VideoStore.Video.VideoColumns.SCRAPER_M_RELEASE_DATE));
                if(v instanceof Episode)entry.onlineId=c.getLong(c.getColumnIndexOrThrow(VideoStore.Video.VideoColumns.SCRAPER_S_ONLINE_ID));
                videos.add(entry); }
            AllTvshowsLoader loader=showsQuery;
            try(Cursor sc=getContext().getContentResolver().query(loader.getUri(),loader.getProjection(),loader.getSelection(),loader.getSelectionArgs(),loader.getSortOrder())) {
                if(sc!=null) { TvshowCursorMapper sm=new TvshowCursorMapper(); sm.bindColumns(sc);
                    Map<Long,Entry> byShow=new HashMap<>(); for(Entry e:videos) if(e.show>0) { Entry old=byShow.get(e.show); if(old==null||old.added<e.added) byShow.put(e.show,e); }
                    while(sc.moveToNext()) { Tvshow tv=(Tvshow)sm.bind(sc); Entry e=byShow.get(tv.getTvshowId()); Entry se=new Entry(tv,e==null?0:e.added,tv.getTvshowId(),e==null?"":e.genres);if(e!=null){se.backdrop=e.backdrop;se.onlineId=e.onlineId;se.releaseDate=e.releaseDate;}shows.add(se); }
                }
            }
            snapshot=build(videos,shows);applyJourneys(snapshot,videos);cachePrivate=com.archos.mediacenter.video.player.PrivateMode.isActive();cached=snapshot;final Snapshot diskSnapshot=snapshot;if(!cachePrivate)cacheWriter.execute(()->writeCache(diskSnapshot)); android.util.Log.d("NovaPreview","Library snapshot: "+(android.os.SystemClock.elapsedRealtime()-started)+" ms, "+videos.size()+" files (local database)");c.moveToPosition(-1); return c;
        } catch(RuntimeException e) { c.close(); throw e; }
    }
}
