package com.archos.mediacenter.video.streaming;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import androidx.leanback.widget.*;
import com.squareup.picasso.Picasso;

/** Real provider artwork with an accessible provider name and an offline text fallback. */
public final class StreamingActionPresenter extends Presenter {
    static final class LogoAction extends StreamingActions.StreamingAction {
        final StreamingRepository.Provider provider;
        LogoAction(StreamingRepository.Provider provider, Runnable click) {
            super(50000, provider.name, "", click); this.provider = provider;
        }
    }
    public static final class Selector extends PresenterSelector {
        private final PresenterSelector normal;
        public Selector(PresenterSelector normal) { this.normal = normal; }
        private final Presenter logo = new StreamingActionPresenter();
        @Override public Presenter getPresenter(Object item) { return item instanceof LogoAction ? logo : normal.getPresenter(item); }
        @Override public Presenter[] getPresenters() {
            Presenter[] original = normal.getPresenters();
            Presenter[] all = java.util.Arrays.copyOf(original, original.length + 1);
            all[original.length] = logo; return all;
        }
    }
    static final class Holder extends ViewHolder {
        final ImageView image; final TextView fallback;
        Holder(FrameLayout frame, ImageView image, TextView fallback) { super(frame); this.image = image; this.fallback = fallback; }
    }
    @Override public ViewHolder onCreateViewHolder(ViewGroup parent) {
        android.content.Context c = parent.getContext();
        float d = c.getResources().getDisplayMetrics().density;
        FrameLayout frame = new FrameLayout(c);
        boolean preview=androidx.preference.PreferenceManager.getDefaultSharedPreferences(c).getBoolean("try_new_ui",false);
        frame.setLayoutParams(new ViewGroup.LayoutParams((int)((preview?64:112)*d), (int)((preview?38:56)*d)));
        frame.setFocusable(true); frame.setClickable(true);
        android.graphics.drawable.StateListDrawable bg = new android.graphics.drawable.StateListDrawable();
        android.graphics.drawable.GradientDrawable focused = new android.graphics.drawable.GradientDrawable();
        focused.setColor(0xff345571); focused.setCornerRadius(6*d); focused.setStroke((int)(2*d), 0xff8fceff);
        bg.addState(new int[]{android.R.attr.state_focused}, focused);
        bg.addState(new int[]{}, new android.graphics.drawable.ColorDrawable(0xff223b50));
        frame.setBackground(bg);
        ImageView image = new ImageView(c); image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        FrameLayout.LayoutParams ip = new FrameLayout.LayoutParams(-1, -1); ip.setMargins((int)(12*d),(int)(8*d),(int)(12*d),(int)(8*d));
        frame.addView(image, ip);
        TextView fallback = new TextView(c); fallback.setGravity(Gravity.CENTER); fallback.setTextColor(Color.WHITE); fallback.setTextSize(13);
        frame.addView(fallback, new FrameLayout.LayoutParams(-1,-1));
        return new Holder(frame,image,fallback);
    }
    @Override public void onBindViewHolder(ViewHolder viewHolder, Object item) {
        Holder h = (Holder)viewHolder; LogoAction a = (LogoAction)item;
        h.view.setContentDescription("Open " + a.provider.name);
        h.fallback.setText(a.provider.name); h.fallback.setVisibility(View.VISIBLE);
        h.image.setImageDrawable(null);
        h.view.setOnClickListener(v -> a.click.run());
        String path = a.provider.logo;
        if (path != null && path.matches("/[A-Za-z0-9._-]+")) {
            Picasso.get().load("https://image.tmdb.org/t/p/w154" + path).fit().centerInside()
                .into(h.image, new com.squareup.picasso.Callback() {
                    @Override public void onSuccess() { h.fallback.setVisibility(View.GONE); }
                    @Override public void onError(Exception error) { h.fallback.setVisibility(View.VISIBLE); }
                });
        }
    }
    @Override public void onUnbindViewHolder(ViewHolder viewHolder) {
        Holder h = (Holder)viewHolder; Picasso.get().cancelRequest(h.image);
        h.image.setImageDrawable(null); h.view.setOnClickListener(null);
    }
}
