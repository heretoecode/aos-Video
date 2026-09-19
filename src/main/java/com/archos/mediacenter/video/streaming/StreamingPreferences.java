// Copyright 2026. Licensed under the Apache License, Version 2.0.
package com.archos.mediacenter.video.streaming;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import androidx.preference.*;
import com.archos.mediacenter.video.R;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.*;
import java.util.concurrent.Future;

/** Native NOVA preference rows: no replacement settings screen. */
public final class StreamingPreferences {
    interface CatalogueLoader { List<StreamingRepository.Provider> load(Context context, String country) throws Exception; }
    private final CatalogueLoader loader;
    private final PreferenceFragmentCompat fragment;
    private final PreferenceCategory category;
    private final ListPreference country;
    private final MultiSelectListPreference providers;
    private final ListPreference preferred;
    private final Preference status;
    private List<StreamingRepository.Provider> catalogue = Collections.emptyList();
    private Future<?> task;
    private int generation;
    private String query = "";
    private boolean showAll;
    private final Handler main = new Handler(Looper.getMainLooper());

    public StreamingPreferences(PreferenceFragmentCompat fragment) {
        this(fragment, StreamingRepository::providers);
    }
    StreamingPreferences(PreferenceFragmentCompat fragment, CatalogueLoader loader) {
        this.loader = loader;
        this.fragment = fragment;
        Context context = fragment.requireContext();
        category = fragment.findPreference("streaming_category");
        SwitchPreferenceCompat enabled = new SwitchPreferenceCompat(context);
        enabled.setKey(StreamingRepository.ENABLED);
        enabled.setTitle(R.string.streaming_enabled_title);
        enabled.setSummary(R.string.streaming_enabled_summary);
        enabled.setDefaultValue(false);
        enabled.setEnabled(StreamingRepository.LINKS_AVAILABLE);
        enabled.setSummary(R.string.streaming_enabled_summary);
        add(enabled);
        country = new ListPreference(context);
        country.setKey(StreamingRepository.COUNTRY);
        country.setTitle(R.string.streaming_country);
        country.setDefaultValue("IE");
        country.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
        List<String> codes = new ArrayList<>(Arrays.asList(Locale.getISOCountries()));
        codes.sort(Comparator.comparing(code -> new Locale("", code).getDisplayCountry()));
        CharSequence[] names = new CharSequence[codes.size()];
        for (int i = 0; i < names.length; i++) names[i] = new Locale("", codes.get(i)).getDisplayCountry();
        country.setEntries(names); country.setEntryValues(codes.toArray(new CharSequence[0]));
        add(country);
        EditTextPreference search = new EditTextPreference(context);
        search.setKey("streaming_provider_search"); search.setTitle("Search providers"); search.setPersistent(false);
        search.setOnPreferenceChangeListener((p, value) -> {
            query = value.toString().trim().toLowerCase(Locale.ROOT);
            search.setSummary(query.isEmpty() ? "All popular providers" : value.toString());
            populate(context); return true;
        });
        add(search);
        SwitchPreferenceCompat all = new SwitchPreferenceCompat(context);
        all.setKey("streaming_show_all"); all.setTitle("Show all providers"); all.setPersistent(false); all.setChecked(false);
        all.setSummary("Selected providers always remain visible");
        all.setOnPreferenceChangeListener((p, value) -> { showAll = (Boolean)value; populate(context); return true; });
        add(all);
        providers = new MultiSelectListPreference(context);
        providers.setKey(StreamingRepository.PROVIDERS + StreamingRepository.country(context));
        providers.setTitle(R.string.streaming_providers);
        providers.setDialogTitle(R.string.streaming_providers);
        providers.setEntries(new CharSequence[0]); providers.setEntryValues(new CharSequence[0]);
        add(providers);
        preferred = new ListPreference(context);
        preferred.setKey(StreamingRepository.PREFERRED + StreamingRepository.country(context));
        preferred.setTitle(R.string.streaming_preferred);
        preferred.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
        add(preferred);
        status = new Preference(context);
        status.setKey("streaming_provider_status");
        status.setTitle(R.string.streaming_refresh);
        status.setOnPreferenceClickListener(p -> { StreamingRepository.invalidate(); load(); return true; });
        add(status);
        Preference attribution = new Preference(context);
        attribution.setTitle("JustWatch");
        attribution.setSummary(R.string.streaming_attribution);
        attribution.setOnPreferenceClickListener(p -> {
            StreamingActions.openWeb(fragment.requireActivity(), "https://www.justwatch.com/" + StreamingRepository.country(context).toLowerCase(Locale.ROOT));
            return true;
        });
        add(attribution);
        country.setOnPreferenceChangeListener((p, value) -> {
            StreamingRepository.prefs(context).edit().putString(StreamingRepository.COUNTRY, value.toString()).apply();
            load(); return true;
        });
        providers.setOnPreferenceChangeListener((p, value) -> {
            @SuppressWarnings("unchecked") Set<String> ids = new HashSet<>((Set<String>) value);
            StreamingRepository.prefs(context).edit().putStringSet(StreamingRepository.PROVIDERS + StreamingRepository.country(context), ids).apply();
            updatePreferred(ids); updateSummary(ids); return true;
        });
        load();
    }
    private void add(Preference preference) {
        preference.setIconSpaceReserved(false);
        category.addPreference(preference);
    }
    private void load() {
        final int request = ++generation;
        if (task != null) task.cancel(true);
        final Context context = fragment.requireContext().getApplicationContext();
        final String region = StreamingRepository.country(context);
        providers.setKey(StreamingRepository.PROVIDERS + region);
        preferred.setKey(StreamingRepository.PREFERRED + region);
        catalogue = cachedCatalogue(context, region);
        populate(context);
        status.setSummary(R.string.streaming_loading);
        task = StreamingRepository.IO.submit(() -> {
            try {
                List<StreamingRepository.Provider> loaded = loader.load(context, region);
                JSONArray json = new JSONArray();
                for (StreamingRepository.Provider p : loaded) json.put(new JSONObject().put("id", p.id).put("name", p.name));
                StreamingRepository.prefs(context).edit().putString("streaming_catalogue_" + region, json.toString()).apply();
                main.post(() -> {
                    if (request != generation || !fragment.isAdded()) return;
                    catalogue = loaded; populate(context);
                    status.setSummary(loaded.isEmpty() ? R.string.streaming_no_country_providers : R.string.streaming_ready);
                });
            } catch (Exception e) {
                main.post(() -> {
                    if (request == generation && fragment.isAdded()) status.setSummary(R.string.streaming_settings_error);
                });
            }
        });
    }
    private static List<StreamingRepository.Provider> cachedCatalogue(Context c, String region) {
        List<StreamingRepository.Provider> result = new ArrayList<>();
        try {
            JSONArray data = new JSONArray(StreamingRepository.prefs(c).getString("streaming_catalogue_" + region, "[]"));
            for (int i = 0; i < data.length(); i++) {
                JSONObject p = data.getJSONObject(i);
                result.add(new StreamingRepository.Provider(p.getInt("id"), p.getString("name")));
            }
        } catch (Exception ignored) { }
        return result;
    }
    private void populate(Context context) {
        Set<String> selected = StreamingRepository.selected(context);
        List<StreamingRepository.Provider> visible = new ArrayList<>();
        for (int i = 0; i < catalogue.size(); i++) {
            StreamingRepository.Provider p = catalogue.get(i);
            if (selected.contains(Integer.toString(p.id)) ||
                ((!query.isEmpty() || showAll || i < 15) && p.name.toLowerCase(Locale.ROOT).contains(query))) visible.add(p);
        }
        Set<String> known = new HashSet<>();
        for (StreamingRepository.Provider p : catalogue) known.add(Integer.toString(p.id));
        for (String id : selected) if (!known.contains(id)) {
            try { visible.add(new StreamingRepository.Provider(Integer.parseInt(id), "Saved provider " + id)); }
            catch (NumberFormatException ignored) { }
        }
        CharSequence[] names = new CharSequence[visible.size()], ids = new CharSequence[visible.size()];
        for (int i = 0; i < visible.size(); i++) {
            names[i] = visible.get(i).name; ids[i] = Integer.toString(visible.get(i).id);
        }
        providers.setEntries(names); providers.setEntryValues(ids);
        providers.setEnabled(!visible.isEmpty());
        Set<String> chosen = StreamingRepository.selected(context);
        providers.setValues(chosen);
        updatePreferred(chosen); updateSummary(chosen);
    }
    private void updateSummary(Set<String> chosen) {
        List<String> names = new ArrayList<>();
        for (StreamingRepository.Provider p : catalogue) if (chosen.contains(Integer.toString(p.id))) names.add(p.name);
        providers.setSummary(names.isEmpty() ? fragment.getString(R.string.streaming_select_providers) : android.text.TextUtils.join(", ", names));
    }
    private void updatePreferred(Set<String> chosen) {
        List<CharSequence> names = new ArrayList<>(), ids = new ArrayList<>();
        names.add(fragment.getString(R.string.streaming_automatic)); ids.add("");
        for (StreamingRepository.Provider p : catalogue) if (chosen.contains(Integer.toString(p.id))) {
            names.add(p.name); ids.add(Integer.toString(p.id));
        }
        preferred.setEntries(names.toArray(new CharSequence[0])); preferred.setEntryValues(ids.toArray(new CharSequence[0]));
        String current = StreamingRepository.preferred(fragment.requireContext());
        preferred.setValue(chosen.contains(current) ? current : "");
        preferred.setEnabled(!chosen.isEmpty());
    }
    public void close() {
        generation++;
        if (task != null) task.cancel(true);
        main.removeCallbacksAndMessages(null);
    }
}
