package com.archos.mediacenter.video.leanback;

import android.content.Context;
import android.content.SharedPreferences;
import com.archos.mediacenter.video.browser.adapters.object.*;
import com.archos.mediacenter.utils.videodb.VideoDbInfo;
import com.archos.mediacenter.video.player.PrivateMode;
import com.archos.mediacenter.video.player.PlayerActivity;
import com.archos.mediacenter.video.leanback.PreviewLibraryLoader.Entry;
import java.util.*;

/** A committed series cursor, distinct from each file's always-saved bookmark. */
public final class PreviewSeriesJourney {
    private static SharedPreferences prefs(Context c) { return androidx.preference.PreferenceManager.getDefaultSharedPreferences(c); }
    public static long meaningfulViewMs(long runtimeMs) { return Math.max(60000,Math.min(180000,runtimeMs>0?runtimeMs/10:180000)); }
    public static boolean resumable(Video v) { return v.getResumeMs()>0; }
    public static boolean completed(Video v) { return v.getResumeMs()==PlayerActivity.LAST_POSITION_END || (v.isWatched()&&!resumable(v)); }
    private static String key(List<Entry> entries) {
        Entry first=entries.get(0);long online=first.onlineId;
        if(online<=0&&PreviewLibraryLoader.memoryCache()!=null)for(Entry show:PreviewLibraryLoader.memoryCache().shows)if(show.show==first.show){online=show.onlineId;break;}
        return online>0?"preview_journey41:show:"+online+":":"preview_journey41:local:"+first.show+":";
    }
    private static void put(SharedPreferences.Editor e,String key,int season,int episode,boolean complete,long time){
        e.putInt(key+"season",season).putInt(key+"episode",episode).putBoolean(key+"complete",complete).putLong(key+"time",time);
    }
    public static void record(Context c,VideoDbInfo info,boolean completed,long viewedMs) {
        if(PrivateMode.isActive()||!info.isShow||info.scraperShowId==null||info.scraperShowId.isEmpty()||info.scraperEpisodeNr<1)return;
        SharedPreferences p=prefs(c);String key="preview_journey41:show:"+info.scraperShowId+":";SharedPreferences.Editor edit=p.edit();
        edit.putBoolean("preview_journey41:attempt:"+info.id,true);
        boolean same=p.getInt(key+"season",-1)==info.scraperSeasonNr&&p.getInt(key+"episode",-1)==info.scraperEpisodeNr;
        if(completed||viewedMs>=meaningfulViewMs(info.duration)||same)put(edit,key,info.scraperSeasonNr,info.scraperEpisodeNr,completed,info.lastTimePlayed);
        edit.apply();
    }
    public static void explicitlyWatched(Context c,Video video){if(video instanceof Episode&&!PrivateMode.isActive())prefs(c).edit().putLong("preview_journey41:mark:"+video.getId(),System.currentTimeMillis()/1000L).apply();}
    public static final class Selection {
        public final Entry episode; public final boolean started;public final long activity;
        Selection(Entry episode,boolean started,long activity){this.episode=episode;this.started=started;this.activity=activity;}
    }
    private static final Comparator<Entry> ORDER=Comparator.comparingInt((Entry e)->((Episode)e.media).getSeasonNumber()).thenComparingInt(e->((Episode)e.media).getEpisodeNumber());
    public static Selection select(Context c,List<Entry> source){
        if(source.isEmpty())return new Selection(null,false,0);
        List<Entry> episodes=new ArrayList<>(source);episodes.sort(ORDER);SharedPreferences p=prefs(c);String key=key(episodes);
        int season=p.getInt(key+"season",-1),number=p.getInt(key+"episode",-1);boolean complete=p.getBoolean(key+"complete",false);long time=p.getLong(key+"time",0);
        // Explicit Mark Watched is a strong user signal even without a playback session.
        for(Entry e:episodes){long marked=p.getLong("preview_journey41:mark:"+((Video)e.media).getId(),0);if(marked>0&&marked>=time){Episode ep=(Episode)e.media;season=ep.getSeasonNumber();number=ep.getEpisodeNumber();complete=true;time=marked;}}
        if(season<0){
            // One-time migration of 4.0 progress, excluding files merely sampled in 4.1.
            Entry seed=episodes.stream().filter(e->resumable((Video)e.media)&&!p.getBoolean("preview_journey41:attempt:"+((Video)e.media).getId(),false)).max(Comparator.comparingLong(e->((Video)e.media).getLastPlayed())).orElse(null);
            if(seed==null)seed=episodes.stream().filter(e->completed((Video)e.media)).max(Comparator.comparingLong((Entry e)->((Video)e.media).getLastPlayed()).thenComparing(ORDER)).orElse(null);
            if(seed!=null){Episode ep=(Episode)seed.media;season=ep.getSeasonNumber();number=ep.getEpisodeNumber();complete=completed(ep);time=ep.getLastPlayed();}
        }
        boolean started=season>=0;Entry selected=null;
        if(started){
            for(Entry e:episodes){Episode ep=(Episode)e.media;if(ep.getSeasonNumber()==season&&ep.getEpisodeNumber()==number){if(completed(ep))complete=true;if(!complete)selected=e;break;}}
            if(complete)for(Entry e:episodes){Episode ep=(Episode)e.media;if((ep.getSeasonNumber()>season||ep.getSeasonNumber()==season&&ep.getEpisodeNumber()>number)&&!completed(ep)){selected=e;break;}}
            if(!PrivateMode.isActive()){SharedPreferences.Editor edit=p.edit();put(edit,key,season,number,complete,time);for(Entry e:episodes)edit.remove("preview_journey41:mark:"+((Video)e.media).getId());edit.apply();}
        }else{
            selected=episodes.stream().filter(e->!completed((Video)e.media)&&((Episode)e.media).getSeasonNumber()>0).findFirst().orElse(null);
            if(selected==null)selected=episodes.stream().filter(e->!completed((Video)e.media)).findFirst().orElse(null);
        }
        return new Selection(selected,started,time);
    }
    private PreviewSeriesJourney(){}
}
