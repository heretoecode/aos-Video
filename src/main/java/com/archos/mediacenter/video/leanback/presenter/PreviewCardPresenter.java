package com.archos.mediacenter.video.leanback.presenter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.leanback.widget.BaseCardView;
import androidx.leanback.widget.Presenter;
import com.archos.mediacenter.video.R;
import com.archos.mediacenter.video.browser.adapters.object.*;
import com.archos.mediacenter.video.leanback.adapter.object.Box;
import com.squareup.picasso.Picasso;

/** Lightweight experimental cards: existing library objects and click actions are preserved. */
public final class PreviewCardPresenter extends Presenter {
    public enum Style { POSTER, CONTINUE, CATEGORY, LIST }
    private final Style style;
    public PreviewCardPresenter(Style style) { this.style = style; }
    public static int progress(long resume, long duration) {
        return resume <= 0 || duration <= 0 ? 0 : (int)Math.min(100, 100d * resume / duration);
    }
    public static final class Card extends BaseCardView {
        public final ImageView image;
        final TextView title, subtitle;
        final LinearLayout caption;
        final ProgressBar progress;
        final Style style;
        boolean hasArtwork;
        final int width, height;
        public Card(Context c, Style style) {
            super(c); this.style = style;
            // Android TV's logical viewport is normally 960 x 540 dp.
            width = dp(style == Style.LIST ? 860 : style == Style.CONTINUE ? 172 : style == Style.CATEGORY ? 174 : 140);
            height = dp(style == Style.LIST ? 62 : style == Style.CONTINUE ? 105 : style == Style.CATEGORY ? 86 : 210);
            setFocusable(true); setFocusableInTouchMode(true);
            setCardType(CARD_TYPE_MAIN_ONLY);setBackgroundColor(Color.TRANSPARENT);
            FrameLayout body = new FrameLayout(c);
            GradientDrawable outline = new GradientDrawable();
            outline.setColor(0xc00b1b29); outline.setCornerRadius(dp(4));
            body.setBackground(outline); body.setClipToOutline(true);
            BaseCardView.LayoutParams bp = new BaseCardView.LayoutParams(width, height);
            bp.viewType = BaseCardView.LayoutParams.VIEW_TYPE_MAIN;
            if (style == Style.POSTER) bp.height += dp(30);
            bp.width = width;
            addView(body, bp);
            image = new ImageView(c); image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            body.addView(image, new FrameLayout.LayoutParams(style == Style.LIST ? dp(112) : -1, style == Style.POSTER ? height : -1));
            caption = new LinearLayout(c); caption.setOrientation(LinearLayout.VERTICAL);
            caption.setPadding(dp(8), dp(style == Style.POSTER ? 2 : 16), dp(8), dp(style == Style.POSTER ? 3 : style == Style.CONTINUE ? 10 : 7));
            caption.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x00101e2c, 0xee101e2c}));
            title = new TextView(c); title.setTextColor(Color.WHITE); title.setTextSize(13);
            title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            title.setMaxLines(style == Style.CONTINUE ? 1 : 2);
            if(style == Style.POSTER){title.setLines(1);title.setTextSize(11);title.setIncludeFontPadding(false);caption.setPadding(dp(5),dp(3),dp(5),dp(2));caption.setBackgroundColor(0xc00b1b29);} title.setEllipsize(TextUtils.TruncateAt.END);
            caption.addView(title, new LinearLayout.LayoutParams(-1, -2));
            subtitle = new TextView(c); subtitle.setTextSize(style == Style.POSTER ? 9 : 11); subtitle.setTextColor(0xffa4b6c7);subtitle.setIncludeFontPadding(false);
            subtitle.setSingleLine(true); subtitle.setEllipsize(TextUtils.TruncateAt.END);
            caption.addView(subtitle, new LinearLayout.LayoutParams(-1, -2));
            FrameLayout.LayoutParams captionParams=new FrameLayout.LayoutParams(-1,-2,style==Style.LIST?Gravity.CENTER_VERTICAL:Gravity.BOTTOM);
            if(style==Style.POSTER)captionParams.height=dp(30);
            if(style==Style.LIST){caption.setPadding(0,0,0,0);title.setSingleLine(true);title.setTextSize(15);captionParams.leftMargin=dp(120);captionParams.rightMargin=dp(30);caption.setBackground(null);TextView arrow=new TextView(c);arrow.setText("›");arrow.setTextColor(0xffa4b6c7);arrow.setTextSize(24);arrow.setGravity(Gravity.CENTER);body.addView(arrow,new FrameLayout.LayoutParams(dp(28),-1,Gravity.RIGHT));}
            body.addView(caption,captionParams);
            progress = new ProgressBar(c, null, android.R.attr.progressBarStyleHorizontal);
            progress.setMax(100); progress.setProgressTintList(ColorStateList.valueOf(com.archos.mediacenter.video.leanback.PreviewAccent.color(c)));
            progress.setProgressBackgroundTintList(ColorStateList.valueOf(0xff627386));
            FrameLayout.LayoutParams pp = new FrameLayout.LayoutParams(-1, dp(3), Gravity.BOTTOM);
            pp.setMargins(dp(8), 0, dp(8), dp(5)); body.addView(progress, pp);
            progress.setVisibility(View.GONE);
            setDescendantFocusability(FOCUS_BLOCK_DESCENDANTS);
            updateFocus();
        }
        @Override protected void drawableStateChanged(){super.drawableStateChanged();if(caption!=null)updateFocus();}
        @Override protected void onMeasure(int widthSpec, int heightSpec) {
            if (View.MeasureSpec.getMode(widthSpec) == View.MeasureSpec.EXACTLY && View.MeasureSpec.getSize(widthSpec)>0)
                getChildAt(0).getLayoutParams().width = View.MeasureSpec.getSize(widthSpec);
            if(style==Style.POSTER){int w=getChildAt(0).getLayoutParams().width;int poster=Math.round(w*1.5f);getChildAt(0).getLayoutParams().height=poster+dp(30);image.getLayoutParams().height=poster;}
            super.onMeasure(widthSpec,heightSpec);
        }
        private int dp(int n) { return Math.round(n * getResources().getDisplayMetrics().density); }
        void updateFocus() {
            GradientDrawable border = new GradientDrawable(); border.setColor(Color.TRANSPARENT);
            border.setCornerRadius(dp(4)); border.setStroke(dp(isFocused() ? 2 : 1), isFocused() ? com.archos.mediacenter.video.leanback.PreviewAccent.color(getContext()) : 0x303d5870);
            setForeground(border);
            // Poster names remain accessible without permanently covering the artwork.
            caption.setVisibility(View.VISIBLE);
        }
    }
    @Override public ViewHolder onCreateViewHolder(ViewGroup parent) { return new ViewHolder(new Card(parent.getContext(), style)); }
    @Override public void onBindViewHolder(ViewHolder holder, Object item) {
        Card c = (Card)holder.view;
        Picasso.get().cancelRequest(c.image); c.image.setImageDrawable(null);
        c.hasArtwork = false; c.subtitle.setText(""); c.subtitle.setVisibility(View.GONE);
        c.progress.setProgress(0); c.progress.setVisibility(View.GONE);
        Uri uri = null;boolean landscape=false;
        c.subtitle.setTextColor(0xffa4b6c7);
        if (item instanceof Box) {
            Box box = (Box)item; c.title.setText(box.getName());
            if (box.getBoxId() == Box.ID.DOCUMENTARIES) c.image.setImageResource(R.drawable.preview_documentaries);
            else if (box.getBitmap() != null) c.image.setImageBitmap(box.getBitmap());
            else if (box.getIconResId() > 0) c.image.setImageResource(box.getIconResId());
            c.hasArtwork = true;
        } else if (item instanceof Base) {
            Base b = (Base)item; c.title.setText(b.getName()); uri = b.getPosterUri();
            if (item instanceof Video) {
                Video v = (Video)item;
                if ((style == Style.CONTINUE || style == Style.LIST) && v.getPreviewBackdrop() != null) {uri = v.getPreviewBackdrop();landscape=true;}
                if (TextUtils.isEmpty(b.getName())) c.title.setText(v.getFilenameNonCryptic());
                if (v instanceof Episode) {
                    Episode e = (Episode)v; c.title.setText(e.getShowName());
                    c.subtitle.setText((v.getResumeMs()>0?"Resume · ":"Up Next · ")+"S"+e.getSeasonNumber()+" E"+e.getEpisodeNumber());
                    c.subtitle.setVisibility(View.VISIBLE);
                    if ((style == Style.CONTINUE || style == Style.LIST) && e.getPictureUri() != null) {uri = e.getPictureUri();landscape=true;}
                }
                if (style == Style.CONTINUE || style == Style.LIST) {
                    c.progress.setVisibility(v.getResumeMs()>0?View.VISIBLE:View.GONE);
                    c.progress.setProgress(progress(v.getResumeMs(), v.getDurationMs()));
                }
            }
        }
        c.setContentDescription(c.title.getText() + (c.subtitle.length() == 0 ? "" : ", " + c.subtitle.getText()));
        if ((style == Style.POSTER || style == Style.LIST) && item instanceof Base) {
            int year = item instanceof Movie ? ((Movie)item).getYear() : item instanceof Tvshow ? ((Tvshow)item).getYear() : 0;
            String detail = year >= 1800 && year <= java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)+5 ? String.valueOf(year) : "";
            // An end year / ongoing marker needs reliable series-status metadata.
            if(item instanceof Tvshow){Tvshow t=(Tvshow)item;if(t.getSeasonCount()>0)detail+=(detail.isEmpty()?"":" · ")+t.getSeasonCount()+(t.getSeasonCount()==1?" season":" seasons");if(t.getEpisodeCount()>0)detail+=(detail.isEmpty()?"":" · ")+t.getEpisodeCount()+(t.getEpisodeCount()==1?" episode":" episodes");}
            c.subtitle.setText(detail); c.subtitle.setVisibility(detail.isEmpty() ? View.INVISIBLE : View.VISIBLE);
        }
        c.setContentDescription(c.title.getText() + (c.subtitle.length() == 0 ? "" : ", " + c.subtitle.getText()));
        c.hasArtwork |= uri != null;
        c.updateFocus();
        boolean letterbox=style==Style.POSTER||(style==Style.CONTINUE||style==Style.LIST)&&!landscape;
        c.image.setScaleType(letterbox?ImageView.ScaleType.FIT_CENTER:ImageView.ScaleType.CENTER_CROP);
        if(uri!=null){com.squareup.picasso.RequestCreator request=Picasso.get().load(uri).resize(style==Style.LIST?Math.round(112*c.getResources().getDisplayMetrics().density):c.width,c.height);
        if(letterbox)request.centerInside();else request.centerCrop();request.noFade().into(c.image, new com.squareup.picasso.Callback() {
                @Override public void onSuccess() { }
                @Override public void onError(Exception error) { c.hasArtwork = false; c.updateFocus(); }
            });}
    }
    public static void bindSecondary(ViewHolder holder,com.archos.mediacenter.video.leanback.PreviewLibraryLoader.Entry entry){
        Card c=(Card)holder.view;
        if((c.style==Style.CONTINUE||c.style==Style.LIST)&&entry.backdrop!=null){c.image.setScaleType(ImageView.ScaleType.CENTER_CROP);Picasso.get().load(entry.backdrop).resize(c.width,c.height).centerCrop().noFade().into(c.image);}
        if(entry.secondary!=null&&!entry.secondary.isEmpty()){
            c.subtitle.setText(entry.secondary);c.subtitle.setVisibility(View.VISIBLE);c.subtitle.setTextColor(entry.active?0xff59d8ff:0xffa4b6c7);
            c.setContentDescription(c.title.getText()+", "+entry.secondary);
        }
    }
    @Override public void onUnbindViewHolder(ViewHolder holder) {
        Card c = (Card)holder.view; Picasso.get().cancelRequest(c.image);
        c.image.setImageDrawable(null); c.setContentDescription(null);
        c.title.setText(""); c.subtitle.setText(""); c.progress.setProgress(0);
    }
}
