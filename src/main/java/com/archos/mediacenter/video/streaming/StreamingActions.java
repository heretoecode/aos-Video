// Copyright 2026. Licensed under the Apache License, Version 2.0.
package com.archos.mediacenter.video.streaming;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.leanback.widget.Action;
import androidx.leanback.widget.ObjectAdapter;
import androidx.leanback.widget.SparseArrayObjectAdapter;
import com.archos.mediacenter.video.R;
import com.archos.mediacenter.video.browser.adapters.object.*;
import com.archos.mediacenter.video.leanback.settings.VideoSettingsActivity;
import com.archos.mediacenter.video.player.PrivateMode;
import com.archos.mediascraper.BaseTags;
import com.archos.mediascraper.EpisodeTags;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.Future;

/** Adds at most a preferred provider and More to the existing Leanback action strip. */
public final class StreamingActions {
    private static final Map<ObjectAdapter, StreamingActions> BOUND = new WeakHashMap<>();
    private final WeakReference<SparseArrayObjectAdapter> adapter;
    private final WeakReference<Activity> activity;
    private final Context app;
    private final int key;
    private final Handler main = new Handler(Looper.getMainLooper());
    private Future<?> task;
    private int generation;
    private Base item;
    private String signature = "";
    private boolean inPlayer;
    private boolean opening;
    private String lookupKind;
    private long lookupId;
    private String lookupCountry;
    private StreamingRepository.Availability lookupAvailability;
    static class StreamingAction extends Action {
        final Runnable click;
        StreamingAction(long id, String label, String detail, Runnable click) {
            super(id, label, detail); this.click = click;
        }
    }
    private StreamingActions(SparseArrayObjectAdapter adapter, Activity activity, int key) {
        this.adapter = new WeakReference<>(adapter); this.activity = new WeakReference<>(activity);
        this.app = activity.getApplicationContext(); this.key = key;
    }
    public static void bind(SparseArrayObjectAdapter adapter, Context context, Base item, boolean inPlayer, int key) {
        if (!StreamingRepository.LINKS_AVAILABLE || !(context instanceof Activity)) return;
        StreamingActions controller = BOUND.get(adapter);
        if (controller == null) {
            controller = new StreamingActions(adapter, (Activity) context, key);
            BOUND.put(adapter, controller);
        }
        if (!(adapter.getPresenterSelector() instanceof StreamingActionPresenter.Selector))
            adapter.setPresenterSelector(new StreamingActionPresenter.Selector(adapter.getPresenterSelector()));
        String signature = StreamingRepository.country(context) + StreamingRepository.selected(context).toString()
                + StreamingRepository.preferred(context) + StreamingRepository.prefs(context).getBoolean(StreamingRepository.ENABLED, false)
                + inPlayer + PrivateMode.isActive();
        if (controller.item == item && signature.equals(controller.signature)) return;
        controller.item = item; controller.inPlayer = inPlayer; controller.signature = signature;
        controller.load();
    }
    public static void refresh(ObjectAdapter adapter) {
        StreamingActions controller = BOUND.get(adapter);
        if (controller != null) controller.load();
    }
    public static void cancel(ObjectAdapter adapter) {
        StreamingActions controller = BOUND.remove(adapter);
        if (controller != null) {
            controller.generation++;
            if (controller.task != null) controller.task.cancel(true);
            controller.main.removeCallbacksAndMessages(null);
        }
    }
    public static boolean isAvailableOffer(Action action) { return action instanceof StreamingActionPresenter.LogoAction; }
    public static boolean onClick(Action action) {
        if (!(action instanceof StreamingAction)) return false;
        ((StreamingAction) action).click.run(); return true;
    }
    private Activity active() {
        Activity a = activity.get();
        return a == null || a.isFinishing() || a.isDestroyed() ? null : a;
    }
    private void clear() {
        SparseArrayObjectAdapter a = adapter.get();
        if (a != null) { a.clear(key); a.clear(key + 1); }
    }
    private void action(int offset, String label, String detail, Runnable click) {
        SparseArrayObjectAdapter a = adapter.get();
        if (a != null && active() != null) a.set(key + offset, new StreamingAction(50000 + offset, label, detail, click));
    }
    private void setup() {
        Activity a = active();
        if (a != null) a.startActivity(new Intent(a, VideoSettingsActivity.class).putExtra("show_streaming_settings", true));
    }
    private void load() {
        final int request = ++generation;
        opening = false;
        if (task != null) task.cancel(true);
        clear();
        if (item == null || inPlayer || PrivateMode.isActive()
                || !StreamingRepository.prefs(app).getBoolean(StreamingRepository.ENABLED, false)) return;
        if (StreamingRepository.selected(app).isEmpty()) {
            action(0, app.getString(R.string.streaming_title), app.getString(R.string.streaming_select_providers), this::setup);
            return;
        }
        final String region = StreamingRepository.country(app);
        final Base target = item;
        // Conditional availability stays absent until a real offer or retry action exists.
        task = StreamingRepository.IO.submit(() -> {
            try {
                String kind = target instanceof Movie ? "movie" : "tv";
                long id;
                if (target instanceof Movie) id = ((Movie) target).getOnlineId();
                else {
                    BaseTags tags = target.getFullScraperTags(app);
                    if (tags instanceof EpisodeTags) tags = ((EpisodeTags) tags).getShowTags();
                    id = tags == null ? -1 : tags.getOnlineId();
                }
                if (id <= 0) {
                    main.post(() -> {
                        if (request == generation) { clear(); action(0, app.getString(R.string.streaming_title), app.getString(R.string.streaming_needs_metadata), this::setup); }
                    });
                    return;
                }
                StreamingRepository.Availability result = StreamingRepository.load(app, kind, id, region, target instanceof Episode ? ((Episode) target).getSeasonNumber() : -1);
                main.post(() -> {
                    if (request != generation || active() == null || !region.equals(StreamingRepository.country(app))) return;
                    clear();
                    lookupKind = kind; lookupId = id; lookupCountry = region; lookupAvailability = result;
                    List<StreamingRepository.Offer> offers = StreamingRepository.filter(result, StreamingRepository.selected(app), StreamingRepository.preferred(app));
                    if (offers.isEmpty()) {
                        action(0, app.getString(R.string.streaming_title), app.getString(R.string.streaming_no_offers, region), this::setup);
                        return;
                    }
                    StreamingRepository.Offer first = offers.get(0);
                    SparseArrayObjectAdapter current = adapter.get();
                    if (current != null) current.set(key, new StreamingActionPresenter.LogoAction(first.provider,
                            () -> openOffer(first, result.watchUrl, target.getName())));
                    if (offers.size() > 1) action(1, "•••", "", () -> more(offers.subList(1, offers.size()), result.watchUrl, target.getName(), region));

                });
            } catch (Exception e) {
                main.post(() -> {
                    if (request == generation && active() != null) {
                        clear(); action(0, app.getString(R.string.streaming_retry), app.getString(R.string.streaming_lookup_error), this::load);
                    }
                });
            }
        });
    }
    private void more(List<StreamingRepository.Offer> offers, String watchUrl, String title, String region) {
        Activity a = active(); if (a == null) return;
        CharSequence[] labels = new CharSequence[offers.size()];
        for (int i = 0; i < offers.size(); i++) labels[i] = offers.get(i).provider.name;
        if (androidx.preference.PreferenceManager.getDefaultSharedPreferences(a).getBoolean("try_new_ui", false)) {
            String[] names=new String[labels.length];for(int i=0;i<labels.length;i++)names[i]=labels[i].toString();
            com.archos.mediacenter.video.leanback.PreviewDialog.choose(a, app.getString(R.string.streaming_more)+" · "+region+" · JustWatch",names,-1,i->openOffer(offers.get(i),watchUrl,title));return;
        }
        new AlertDialog.Builder(a, com.archos.mediacenter.video.utils.ThemeManager.getInstance(a).isSlateTheme() ? R.style.Theme_AlertDialog_Slate : 0).setTitle(app.getString(R.string.streaming_more) + " · " + region + " · JustWatch")
                .setItems(labels, (dialog, which) -> openOffer(offers.get(which), watchUrl, title))
                .setNegativeButton(android.R.string.cancel, null).show();
    }
    private void openOffer(StreamingRepository.Offer offer, String watchUrl, String title) {
        Activity a = active(); if (a == null || opening) return;
        opening = true;
        final int request = generation;
        final String kind = lookupKind, country = lookupCountry;
        final long id = lookupId;
        final StreamingRepository.Availability availability = lookupAvailability;
        Toast.makeText(a, app.getString(R.string.streaming_open_provider, offer.provider.name), Toast.LENGTH_SHORT).show();
        task = StreamingRepository.IO.submit(() -> {
            String link = availability == null ? "" : StreamingRepository.titleLink(app, kind, id, country, availability, offer.provider.id);
            String resolved = StreamingRepository.resolveTitleUrl(link);
            main.post(() -> {
                if (request != generation || active() == null) return;
                opening = false;
                launchOffer(offer, resolved, watchUrl, title);
            });
        });
    }
    private void launchOffer(StreamingRepository.Offer offer, String url, String watchUrl, String title) {
        Activity a = active(); if (a == null) return;
        if (StreamingRepository.safeWebUrl(url)) {
            for (String pkg : packages(offer.provider.name)) {
                if (start(a, new Intent(Intent.ACTION_VIEW, Uri.parse(url)).setPackage(pkg))) return;
            }
            if (start(a, new Intent(Intent.ACTION_VIEW, Uri.parse(url)))) return;
        }
        if (androidx.preference.PreferenceManager.getDefaultSharedPreferences(a).getBoolean("try_new_ui", false)) {
            com.archos.mediacenter.video.leanback.PreviewDialog.choose(a,offer.provider.name,new String[]{app.getString(R.string.streaming_open_app),app.getString(R.string.streaming_watch_page),app.getString(android.R.string.cancel)},2,choice->{
                if(choice==1){openWeb(a,watchUrl);return;}if(choice!=0)return;
                for(String pkg:packages(offer.provider.name)){Intent launch=a.getPackageManager().getLeanbackLaunchIntentForPackage(pkg);if(launch==null)launch=a.getPackageManager().getLaunchIntentForPackage(pkg);if(launch!=null&&start(a,launch))return;}
                Toast.makeText(a,R.string.streaming_app_missing,Toast.LENGTH_LONG).show();
            });return;
        }
        new AlertDialog.Builder(a, com.archos.mediacenter.video.utils.ThemeManager.getInstance(a).isSlateTheme() ? R.style.Theme_AlertDialog_Slate : 0).setTitle(offer.provider.name)
                .setMessage(app.getString(R.string.streaming_open_fallback, title))
                .setPositiveButton(R.string.streaming_open_app, (d, w) -> {
                    for (String pkg : packages(offer.provider.name)) {
                        Intent launch = a.getPackageManager().getLeanbackLaunchIntentForPackage(pkg);
                        if (launch == null) launch = a.getPackageManager().getLaunchIntentForPackage(pkg);
                        if (launch != null && start(a, launch)) return;
                    }
                    Toast.makeText(a, R.string.streaming_app_missing, Toast.LENGTH_LONG).show();
                })
                .setNeutralButton(R.string.streaming_watch_page, (d, w) -> openWeb(a, watchUrl))
                .setNegativeButton(android.R.string.cancel, null).show();
    }
    private static String[] packages(String name) {
        String n = name.toLowerCase(Locale.ROOT);
        if (n.contains("netflix")) return new String[]{"com.netflix.ninja", "com.netflix.mediaclient"};
        if (n.contains("amazon") || n.contains("prime video")) return new String[]{"com.amazon.amazonvideo.livingroom", "com.amazon.avod.thirdpartyclient"};
        if (n.contains("disney")) return new String[]{"com.disney.disneyplus"};
        if (n.contains("apple tv")) return new String[]{"com.apple.atve.androidtv.appletv"};
        if (n.contains("paramount")) return new String[]{"com.cbs.ca", "com.cbs.ott"};
        if (n.contains("now")) return new String[]{"com.bskyb.nowtv.beta"};
        return new String[0];
    }
    private static boolean start(Activity activity, Intent intent) {
        try { activity.startActivity(intent); return true; }
        catch (android.content.ActivityNotFoundException | SecurityException e) { return false; }
    }
    public static void openWeb(Activity a, String url) {
        if (!StreamingRepository.safeWebUrl(url) || !start(a, new Intent(Intent.ACTION_VIEW, Uri.parse(url))))
            Toast.makeText(a, R.string.streaming_browser_missing, Toast.LENGTH_LONG).show();
    }
}
