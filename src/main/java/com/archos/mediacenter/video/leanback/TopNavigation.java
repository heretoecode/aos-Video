package com.archos.mediacenter.video.leanback;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.archos.mediacenter.video.utils.ThemeManager;
import java.util.function.IntConsumer;
import java.util.function.BooleanSupplier;

/** Optional navigation shell; the same native BrowseSupportFragment owns all library rows. */
public final class TopNavigation extends LinearLayout {
    private final LinearLayout bar;
    private final PreviewBackdrop artwork;
    private final View content;
    private final android.widget.FrameLayout scanStatus;
    private final android.widget.FrameLayout status;
    private final BooleanSupplier firstRow;
    private TextView selected;
    private final TextView[] tabs = new TextView[6];

    public TopNavigation(Context c, View content, IntConsumer navigate, BooleanSupplier firstRow) {
        super(c);
        this.content = content; this.firstRow = firstRow;
        scanStatus = new android.widget.FrameLayout(c);scanStatus.setFocusable(false);scanStatus.setDescendantFocusability(FOCUS_BLOCK_DESCENDANTS);
        setOrientation(VERTICAL);
        artwork = new PreviewBackdrop(c); setBackground(artwork);
        bar = new LinearLayout(c); bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(26), 0, dp(26), 0);
        bar.setBackgroundColor(Color.TRANSPARENT);
        TextView brand = new TextView(c);
        brand.setText("SUPERNOVA"); brand.setTypeface(android.graphics.Typeface.create("sans-serif-light", android.graphics.Typeface.NORMAL)); brand.setTextSize(19); brand.setTextColor(0xffb7d7f5);
        brand.setGravity(Gravity.CENTER_VERTICAL); brand.setPadding(0, 0, 0, 0); bar.addView(brand, new LayoutParams(dp(130), -1));
        LinearLayout group = new LinearLayout(c); group.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);
        bar.addView(group, new LayoutParams(0, -1, 1));
        String[] labels = {"Home", "Movies", "TV shows", "Network & files", "Settings", "Search"};
        for (int i = 0; i < labels.length; i++) {
            final int index = i;
            TextView tab = new TextView(c); tabs[i] = tab;
            tab.setText(labels[i]); tab.setContentDescription(labels[i]);
            tab.setTextColor(Color.WHITE); tab.setTextSize(15); tab.setGravity(Gravity.CENTER);
            tab.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
            tab.setSingleLine(true); tab.setFocusable(true); tab.setFocusableInTouchMode(true); tab.setClickable(true);
            tab.setPadding(dp(12), dp(6), dp(12), dp(6));
            tab.setTextColor(new android.content.res.ColorStateList(new int[][]{new int[]{android.R.attr.state_selected}, new int[]{android.R.attr.state_focused}, new int[]{}}, new int[]{Color.WHITE, 0xff59d8ff, 0xffb4cbe0}));
            styleTab(tab);
            tab.setOnFocusChangeListener((v, focused)->{
                for(android.graphics.drawable.Drawable icon:tab.getCompoundDrawables())if(icon!=null)icon.setTint(focused?PreviewAccent.color(c):0xffb4cbe0);
                v.animate().scaleX(focused?1.025f:1f).scaleY(focused?1.025f:1f).setDuration(160).start();});
            tab.setOnClickListener(v -> {
                if (index < 4) {
                    for (TextView t : tabs) t.setSelected(false);
                    selected = tab; tab.setSelected(true);
                }
                navigate.accept(index);

            });
            tab.setOnKeyListener((v, key, event) -> {
                if (key == KeyEvent.KEYCODE_DPAD_DOWN && event.getAction() == KeyEvent.ACTION_DOWN) {
                    content.requestFocus(); return true;
                }
                return false;
            });
            if (i == 5) {
                tab.setText("");
                android.graphics.drawable.Drawable search = c.getDrawable(com.archos.mediacenter.video.R.drawable.preview_search);
                search.setBounds(0, 0, dp(22), dp(22)); tab.setCompoundDrawables(search, null, null, null);
            }
            if(i==4){View gap=new View(c);group.addView(gap,new LayoutParams(0,1,1));}
            group.addView(tab, new LayoutParams(-2, dp(36)));
        }
        group.removeView(tabs[5]);group.addView(tabs[5],group.indexOfChild(tabs[4]),new LayoutParams(-2,dp(36)));
        status = new android.widget.FrameLayout(c);
        bar.addView(status, new LayoutParams(dp(85), dp(46)));
        android.widget.TextClock clock=new android.widget.TextClock(c);clock.setTag("preview-default-clock");clock.setFormat12Hour("h:mm");clock.setFormat24Hour("HH:mm");clock.setTextSize(19);clock.setTextColor(0xffd6e5f3);clock.setGravity(Gravity.CENTER);status.addView(clock,new android.widget.FrameLayout.LayoutParams(-1,-1));
        selected = tabs[0]; selected.setSelected(true);
        addView(bar, new LayoutParams(-1, dp(52)));
        android.widget.FrameLayout stage=new android.widget.FrameLayout(c);
        stage.addView(content,new android.widget.FrameLayout.LayoutParams(-1,-1));
        android.widget.FrameLayout.LayoutParams scanParams=new android.widget.FrameLayout.LayoutParams(-2,-2,Gravity.BOTTOM|Gravity.END);
        scanParams.setMargins(dp(20),dp(12),dp(20),dp(16));stage.addView(scanStatus,scanParams);
        addView(stage,new LayoutParams(-1,0,1));
    }
    private final android.content.SharedPreferences.OnSharedPreferenceChangeListener accentListener=(prefs,key)->{if("preview_accent41".equals(key)){for(TextView tab:tabs)styleTab(tab);invalidate();}};
    private void styleTab(TextView tab){Context c=getContext();tab.setTextColor(new android.content.res.ColorStateList(new int[][]{new int[]{android.R.attr.state_selected},new int[]{android.R.attr.state_focused},new int[]{}},new int[]{Color.WHITE,PreviewAccent.color(c),0xffb4cbe0}));StateListDrawable bg=new StateListDrawable();GradientDrawable focused=new GradientDrawable();focused.setColor(PreviewAccent.alpha(c,50));focused.setCornerRadius(dp(7));focused.setStroke(dp(1),PreviewAccent.alpha(c,153));bg.addState(new int[]{android.R.attr.state_focused},focused);GradientDrawable active=new GradientDrawable();active.setColor(PreviewAccent.color(c));active.setCornerRadius(dp(2));android.graphics.drawable.LayerDrawable underline=new android.graphics.drawable.LayerDrawable(new android.graphics.drawable.Drawable[]{active});underline.setLayerHeight(0,dp(3));underline.setLayerGravity(0,Gravity.BOTTOM);underline.setLayerInset(0,dp(10),0,dp(10),dp(3));bg.addState(new int[]{android.R.attr.state_selected},underline);bg.addState(new int[]{},new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));tab.setBackground(bg);}
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();androidx.preference.PreferenceManager.getDefaultSharedPreferences(getContext()).registerOnSharedPreferenceChangeListener(accentListener);for(TextView tab:tabs)styleTab(tab);}
    private boolean scrolled;private android.animation.ValueAnimator scrimAnimation;private int scrimAlpha;
    public void setScrolled(boolean value){value=value&&selected!=tabs[3]&&selected!=tabs[5];if(value==scrolled)return;scrolled=value;if(scrimAnimation!=null)scrimAnimation.cancel();scrimAnimation=android.animation.ValueAnimator.ofInt(scrimAlpha,value?247:0);scrimAnimation.setDuration(180);scrimAnimation.addUpdateListener(a->{scrimAlpha=(Integer)a.getAnimatedValue();bar.setBackgroundColor(android.graphics.Color.argb(scrimAlpha,9,23,35));});scrimAnimation.start();}
    public void setArtwork(android.net.Uri uri) { artwork.load(uri); }
    public void selectTab(int index) { if(index<0||index>=6)return; for(TextView t:tabs)t.setSelected(false); selected=tabs[index];selected.setSelected(true);if(index==3||index==5)setScrolled(false); }
    @Override protected void onDetachedFromWindow() { androidx.preference.PreferenceManager.getDefaultSharedPreferences(getContext()).unregisterOnSharedPreferenceChangeListener(accentListener);artwork.release();super.onDetachedFromWindow(); }
    public android.widget.FrameLayout getScanContainer() { return scanStatus; }
    public android.widget.FrameLayout getStatusContainer() { return status; }
    private int dp(int n) { return (int)(n * getResources().getDisplayMetrics().density + .5f); }
    public boolean focusNavigation() {
        if (bar.hasFocus()) return false;
        selected.requestFocus(); return true;
    }
    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_DPAD_UP
                && !bar.hasFocus() && firstRow.getAsBoolean()) {
            selected.requestFocus(); return true;
        }
        return super.dispatchKeyEvent(event);
    }
}
