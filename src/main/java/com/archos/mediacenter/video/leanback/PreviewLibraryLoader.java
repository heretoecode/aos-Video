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
            VideoStore.Video.VideoColumns.SCRAPER_M_RELEASE_DATE, VideoStore.Video.VideoColumns.SCRAPER_S_PREMIERED, VideoStore.Video.VideoColumns.SCRAPER_S_ONLINE_ID,
            VideoStore.Video.VideoColumns.SCRAPER_M_GENRES, VideoStore.Video.VideoColumns.SCRAPER_S_GENRES});
    }
    public static final class Entry {
        public final Base media;
        public final long added, show;
        public final String genres;
        public String releaseDate=""; public long onlineId; public android.net.Uri backdrop;
        public Entry(Base media, long added, long show, String genres) {
            this.media=media; this.added=added; this.show=show; this.genres=genres == null ? "" : genres;
            if(media instanceof Movie) onlineId=((Movie)media).getOnlineId();
            if(media instanceof Video) backdrop=((Video)media).getPreviewBackdrop();
        }
        public String key() { return media instanceof Episode ? "s"+show : media instanceof Tvshow ? "s"+((Tvshow)media).getTvshowId() : "v"+((Video)media).getId(); }
        public int year() { return media instanceof Movie ? ((Movie)media).getYear() : media instanceof Tvshow ? ((Tvshow)media).getYear() : 0; }
    }
    public static final class Snapshot {
        public final List<Entry> watched = new ArrayList<>();
        public final List<Entry> movies = new ArrayList<>(), shows = new ArrayList<>(), recent = new ArrayList<>(), played = new ArrayList<>(), continuingMovies = new ArrayList<>(), continuingShows = new ArrayList<>();
    }
    public static boolean watched(Video v) { return v.isWatched() || v.getResumeMs() == com.archos.mediacenter.video.player.PlayerActivity.LAST_POSITION_END; }
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
            if(v instanceof Episode && e.show>0) groups.computeIfAbsent(e.show,k->new ArrayList<>()).add(e);
            else { s.recent.add(e); if(v instanceof Movie) { s.movies.add(e); if(!watched(v) && v.getResumeMs()>0) s.continuingMovies.add(e); } }
            if(v.getLastPlayed()>0) { s.played.add(e); if(watched(v))s.watched.add(e); }
        }
        for(List<Entry> group:groups.values()) {
            Entry n=next(group); long newest=group.stream().mapToLong(e->e.added).max().orElse(0);
            if(n!=null) {
                Entry grouped=new Entry(n.media,newest,n.show,n.genres);grouped.onlineId=n.onlineId;grouped.releaseDate=n.releaseDate;s.recent.add(grouped);
                if(group.stream().anyMatch(e->((Video)e.media).getLastPlayed()>0 || watched((Video)e.media))) s.continuingShows.add(n);
            }
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
    @Override public Cursor loadInBackground() {
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
            snapshot=build(videos,shows); c.moveToPosition(-1); return c;
        } catch(RuntimeException e) { c.close(); throw e; }
    }
}
