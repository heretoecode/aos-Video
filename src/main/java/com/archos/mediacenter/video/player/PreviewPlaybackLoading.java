package com.archos.mediacenter.video.player;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.view.*;
import android.widget.*;
import com.archos.mediacenter.video.R;
import com.archos.mediacenter.video.browser.adapters.object.*;
import com.archos.mediacenter.utils.videodb.VideoDbInfo;
import com.squareup.picasso.Picasso;
import java.util.Locale;

/** Cached artwork only. Visibility follows the existing player preparation timer. */
final class PreviewPlaybackLoading extends FrameLayout {
    private final ImageView artwork;
    private final TextView title, episode;
    private Uri cachedArtwork, source, fileSource;
    private boolean requestedArtwork;
    PreviewPlaybackLoading(Context context, Intent intent) {
        super(context);setId(R.id.progress_indicator);setVisibility(GONE);
        setBackgroundColor(0xff091725);setFocusable(false);setDescendantFocusability(FOCUS_BLOCK_DESCENDANTS);
        artwork=new ImageView(context);artwork.setScaleType(ImageView.ScaleType.CENTER_CROP);addView(artwork,new LayoutParams(-1,-1));
        View shade=new View(context);shade.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,new int[]{0xee071522,0x99071522,0x66071522}));addView(shade,new LayoutParams(-1,-1));
        LinearLayout labels=new LinearLayout(context);labels.setOrientation(LinearLayout.VERTICAL);
        LayoutParams labelsParams=new LayoutParams(dp(570),-2,Gravity.START|Gravity.CENTER_VERTICAL);labelsParams.leftMargin=dp(48);addView(labels,labelsParams);
        title=text("",32);title.setTypeface(null,android.graphics.Typeface.BOLD);title.setMaxLines(2);title.setEllipsize(android.text.TextUtils.TruncateAt.END);labels.addView(title);
        episode=text("",16);episode.setPadding(0,dp(10),0,dp(20));labels.addView(episode);
        LinearLayout status=new LinearLayout(context);status.setGravity(Gravity.CENTER_VERTICAL);status.setPadding(0,dp(18),0,0);labels.addView(status);
        ProgressBar spinner=new ProgressBar(context,null,android.R.attr.progressBarStyleSmall);spinner.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(0xff62bbf3));status.addView(spinner,new LinearLayout.LayoutParams(dp(28),dp(28)));
        TextView starting=text("Starting playback…",16);starting.setPadding(dp(14),0,dp(8),0);status.addView(starting);
        TextView buffer=text("",13);buffer.setId(R.id.buffer_percentage);status.addView(buffer);
        TextView torrent=text("",13);torrent.setId(R.id.torrent_status);torrent.setVisibility(GONE);labels.addView(torrent);
        Object value=intent.getSerializableExtra(PlayerService.VIDEO);
        if(value instanceof Video){Video video=(Video)value;source=video.getUri();fileSource=video.getFileUri();cachedArtwork=video.getPreviewBackdrop();
            title.setText(video instanceof Episode?((Episode)video).getShowName():video.getName());
            if(video instanceof Episode){Episode e=(Episode)video;episode.setText(String.format(Locale.getDefault(),"S%02d E%02d",e.getSeasonNumber(),e.getEpisodeNumber())+(video.getName()==null?"":" · "+video.getName()));}
        }
        episode.setVisibility(episode.length()==0?GONE:VISIBLE);
    }
    void bind(VideoDbInfo info,String fallback){
        if(info==null){if(fallback!=null)title.setText(fallback);return;}
        title.setText(info.isScraped&&info.scraperTitle!=null?info.scraperTitle:fallback);
        boolean show=info.isShow&&info.scraperSeasonNr>=0&&info.scraperEpisodeNr>=0;
        episode.setText(show?String.format(Locale.getDefault(),"S%02d E%02d",info.scraperSeasonNr,info.scraperEpisodeNr)+(info.scraperEpisodeName==null?"":" · "+info.scraperEpisodeName):"");episode.setVisibility(show?VISIBLE:GONE);
        if(info.uri!=null&&!info.uri.equals(source)&&!info.uri.equals(fileSource)){cachedArtwork=null;Picasso.get().cancelRequest(artwork);artwork.setImageDrawable(null);}
    }
    @Override protected void onVisibilityChanged(View changed,int visibility){
        super.onVisibilityChanged(changed,visibility);
        if(artwork==null)return;
        if(visibility==VISIBLE&&!requestedArtwork&&cachedArtwork!=null&&"file".equals(cachedArtwork.getScheme())){
            requestedArtwork=true;Picasso.get().load(cachedArtwork).resize(1280,720).centerCrop().noFade().into(artwork);
        }else if(visibility!=VISIBLE){Picasso.get().cancelRequest(artwork);requestedArtwork=false;}
    }
    @Override protected void onDetachedFromWindow(){Picasso.get().cancelRequest(artwork);super.onDetachedFromWindow();}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private TextView text(String value,int size){TextView t=new TextView(getContext());t.setText(value);t.setTextSize(size);t.setTextColor(Color.WHITE);return t;}
}
