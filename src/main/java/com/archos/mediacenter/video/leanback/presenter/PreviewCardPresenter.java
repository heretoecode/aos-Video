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
            height = dp(style == Style.LIST ? 92 : style == Style.CONTINUE ? 105 : style == Style.CATEGORY ? 86 : 144);
            setFocusable(true); setFocusableInTouchMode(true);
            setCardType(CARD_TYPE_MAIN_ONLY);
            FrameLayout body = new FrameLayout(c);
            GradientDrawable outline = new GradientDrawable();
            outline.setColor(0xff182c3e); outline.setCornerRadius(dp(4));
            body.setBackground(outline); body.setClipToOutline(true);
            BaseCardView.LayoutParams bp = new BaseCardView.LayoutParams(width, height);
            bp.viewType = BaseCardView.LayoutParams.VIEW_TYPE_MAIN;
            if (style == Style.POSTER) bp.height += dp(46);
            bp.width = width;
            addView(body, bp);
            image = new ImageView(c); image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            body.addView(image, new FrameLayout.LayoutParams(style == Style.LIST ? dp(160) : -1, style == Style.POSTER ? height : -1));
            caption = new LinearLayout(c); caption.setOrientation(LinearLayout.VERTICAL);
            caption.setPadding(dp(8), dp(style == Style.POSTER ? 2 : 16), dp(8), dp(style == Style.POSTER ? 3 : style == Style.CONTINUE ? 10 : 7));
            caption.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x00101e2c, 0xee101e2c}));
            title = new TextView(c); title.setTextColor(Color.WHITE); title.setTextSize(13);
            title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            title.setMaxLines(style == Style.CONTINUE ? 1 : 2);
            if(style == Style.POSTER){title.setLines(2);title.setTextSize(12);title.setIncludeFontPadding(false);caption.setPadding(dp(6),dp(4),dp(6),dp(3));caption.setBackgroundColor(0xff102333);} title.setEllipsize(TextUtils.TruncateAt.END);
            caption.addView(title, new LinearLayout.LayoutParams(-1, -2));
            subtitle = new TextView(c); subtitle.setTextSize(style == Style.POSTER ? 10 : 11); subtitle.setTextColor(0xffa4b6c7);subtitle.setIncludeFontPadding(false);
            subtitle.setSingleLine(true); subtitle.setEllipsize(TextUtils.TruncateAt.END);
            caption.addView(subtitle, new LinearLayout.LayoutParams(-1, -2));
            FrameLayout.LayoutParams captionParams=new FrameLayout.LayoutParams(-1,-2,style==Style.LIST?Gravity.CENTER_VERTICAL:Gravity.BOTTOM);
            if(style==Style.POSTER)captionParams.height=dp(46);
            if(style==Style.LIST){captionParams.leftMargin=dp(172);caption.setBackground(null);}
            body.addView(caption,captionParams);
            progress = new ProgressBar(c, null, android.R.attr.progressBarStyleHorizontal);
            progress.setMax(100); progress.setProgressTintList(ColorStateList.valueOf(0xff62bbf3));
            progress.setProgressBackgroundTintList(ColorStateList.valueOf(0xff627386));
            FrameLayout.LayoutParams pp = new FrameLayout.LayoutParams(-1, dp(3), Gravity.BOTTOM);
            pp.setMargins(dp(8), 0, dp(8), dp(5)); body.addView(progress, pp);
            progress.setVisibility(View.GONE);
            setOnFocusChangeListener((v, focus) -> updateFocus());
            updateFocus();
        }
        @Override protected void onMeasure(int widthSpec, int heightSpec) {
            if (View.MeasureSpec.getMode(widthSpec) == View.MeasureSpec.EXACTLY && View.MeasureSpec.getSize(widthSpec)>0)
                getChildAt(0).getLayoutParams().width = View.MeasureSpec.getSize(widthSpec);
            super.onMeasure(widthSpec,heightSpec);
        }
        private int dp(int n) { return Math.round(n * getResources().getDisplayMetrics().density); }
        void updateFocus() {
            GradientDrawable border = new GradientDrawable(); border.setColor(Color.TRANSPARENT);
            border.setCornerRadius(dp(4)); border.setStroke(dp(isFocused() ? 2 : 1), isFocused() ? 0xff62bbf3 : 0x303d5870);
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
        Uri uri = null;
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
                if ((style == Style.CONTINUE || style == Style.LIST) && v.getPreviewBackdrop() != null) uri = v.getPreviewBackdrop();
                if (TextUtils.isEmpty(b.getName())) c.title.setText(v.getFilenameNonCryptic());
                if (v instanceof Episode) {
                    Episode e = (Episode)v; c.title.setText(e.getShowName());
                    c.subtitle.setText(c.getResources().getString(R.string.preview_episode, e.getSeasonNumber(), e.getEpisodeNumber()));
                    c.subtitle.setVisibility(View.VISIBLE);
                    if ((style == Style.CONTINUE || style == Style.LIST) && e.getPictureUri() != null) uri = e.getPictureUri();
                }
                if (style == Style.CONTINUE || style == Style.LIST) {
                    c.progress.setVisibility(View.VISIBLE);
                    c.progress.setProgress(progress(v.getResumeMs(), v.getDurationMs()));
                }
            }
        }
        c.setContentDescription(c.title.getText() + (c.subtitle.length() == 0 ? "" : ", " + c.subtitle.getText()));
        if ((style == Style.POSTER || style == Style.LIST) && item instanceof Base) {
            int year = item instanceof Movie ? ((Movie)item).getYear() : item instanceof Tvshow ? ((Tvshow)item).getYear() : 0;
            String detail = year >= 1800 && year <= java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)+5 ? String.valueOf(year) : "";
            // An end year / ongoing marker needs reliable series-status metadata.
            c.subtitle.setText(detail); c.subtitle.setVisibility(detail.isEmpty() ? View.INVISIBLE : View.VISIBLE);
        }
        c.setContentDescription(c.title.getText() + (c.subtitle.length() == 0 ? "" : ", " + c.subtitle.getText()));
        c.hasArtwork |= uri != null;
        c.updateFocus();
        if (uri != null) Picasso.get().load(uri).resize(c.width, c.height).centerCrop().noFade()
            .into(c.image, new com.squareup.picasso.Callback() {
                @Override public void onSuccess() { }
                @Override public void onError(Exception error) { c.hasArtwork = false; c.updateFocus(); }
            });
    }
    @Override public void onUnbindViewHolder(ViewHolder holder) {
        Card c = (Card)holder.view; Picasso.get().cancelRequest(c.image);
        c.image.setImageDrawable(null); c.setContentDescription(null);
        c.title.setText(""); c.subtitle.setText(""); c.progress.setProgress(0);
    }
}
