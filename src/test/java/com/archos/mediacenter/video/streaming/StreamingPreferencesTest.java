package com.archos.mediacenter.video.streaming;

import android.app.Application;
import android.os.Bundle;
import android.os.Looper;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import com.archos.mediacenter.video.R;
import java.util.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application=Application.class, sdk=28)
public class StreamingPreferencesTest {
    public static class Host extends FragmentActivity {
        @Override public void onCreate(Bundle state) {
            setTheme(androidx.appcompat.R.style.Theme_AppCompat);
            super.onCreate(state);
            setContentView(new android.widget.FrameLayout(this));
        }
    }
    public static class Prefs extends PreferenceFragmentCompat {
        @Override public void onCreatePreferences(Bundle state, String root) {
            PreferenceScreen screen=getPreferenceManager().createPreferenceScreen(requireContext());
            setPreferenceScreen(screen);
            PreferenceCategory category=new PreferenceCategory(requireContext());
            category.setKey("streaming_category");screen.addPreference(category);
        }
    }
    private ActivityController<Host> host;
    private Prefs fragment;
    private StreamingPreferences binding;
    @Before public void start() {
        host=Robolectric.buildActivity(Host.class).setup();
        StreamingRepository.prefs(host.get()).edit().clear().commit();
        fragment=new Prefs();
        host.get().getSupportFragmentManager().beginTransaction().add(android.R.id.content,fragment).commitNow();
    }
    @After public void stop() {
        if(binding!=null)binding.close();
        host.pause().stop().destroy();
    }
    private void awaitSummary(int text) throws Exception {
        long deadline=System.currentTimeMillis()+5000;
        while(System.currentTimeMillis()<deadline) {
            Shadows.shadowOf(Looper.getMainLooper()).idle();
            PreferenceCategory category=fragment.findPreference("streaming_category");
            if(host.get().getString(text).contentEquals(fragment.findPreference("streaming_provider_status").getSummary()))return;
            Thread.sleep(10);
        }
        fail("Provider state did not settle");
    }
    @Test public void countrySwitchPreservesSeparateSelectionsAndPreferredProvider() throws Exception {
        StreamingRepository.prefs(host.get()).edit().putString("streaming_country","IE")
                .putStringSet("streaming_providers_IE",Collections.singleton("8"))
                .putString("streaming_preferred_IE","8").commit();
        binding=new StreamingPreferences(fragment,(c,region)->"IE".equals(region)
                ? Arrays.asList(new StreamingRepository.Provider(8,"Netflix"),new StreamingRepository.Provider(337,"Disney Plus"))
                : Collections.singletonList(new StreamingRepository.Provider(9,"Amazon")));
        awaitSummary(R.string.streaming_ready);
        ListPreference country=fragment.findPreference("streaming_country");
        country.callChangeListener("GB");country.setValue("GB");
        awaitSummary(R.string.streaming_ready);
        MultiSelectListPreference providers=fragment.findPreference("streaming_providers_GB");
        assertNotNull(providers);
        providers.callChangeListener(Collections.singleton("9"));providers.setValues(Collections.singleton("9"));
        ListPreference preferred=fragment.findPreference("streaming_preferred_GB");preferred.setValue("9");
        country.callChangeListener("IE");country.setValue("IE");
        awaitSummary(R.string.streaming_ready);
        assertEquals(Collections.singleton("8"),StreamingRepository.selected(host.get()));
        assertEquals("8",StreamingRepository.preferred(host.get()));
        assertEquals(Collections.singleton("9"),StreamingRepository.prefs(host.get()).getStringSet("streaming_providers_GB",Collections.emptySet()));
        assertEquals("9",StreamingRepository.prefs(host.get()).getString("streaming_preferred_GB",""));
    }
    @Test public void failedRefreshKeepsSavedChoices() throws Exception {
        StreamingRepository.prefs(host.get()).edit().putString("streaming_country","IE")
                .putStringSet("streaming_providers_IE",Collections.singleton("8"))
                .putString("streaming_preferred_IE","8")
                .putString("streaming_catalogue_IE","[{\"id\":8,\"name\":\"Netflix\"}]").commit();
        binding=new StreamingPreferences(fragment,(c,r)->{throw new java.io.IOException("Offline");});
        awaitSummary(R.string.streaming_settings_error);
        assertEquals(Collections.singleton("8"),StreamingRepository.selected(host.get()));
        assertEquals("8",StreamingRepository.preferred(host.get()));
        MultiSelectListPreference providers=fragment.findPreference("streaming_providers_IE");
        assertTrue(providers.isEnabled());assertEquals("Netflix",providers.getEntries()[0]);
    }
    @Test public void filteringKeepsSelectionsVisibleAndDoesNotChangeThem() throws Exception {
        StreamingRepository.prefs(host.get()).edit().putStringSet("streaming_providers_IE", Collections.singleton("99")).commit();
        binding=new StreamingPreferences(fragment,(c,r)-> {
            List<StreamingRepository.Provider> list=new ArrayList<>();
            for(int i=1;i<=20;i++)list.add(new StreamingRepository.Provider(i,"Provider "+i));
            return list;
        });
        awaitSummary(R.string.streaming_ready);
        MultiSelectListPreference providers=fragment.findPreference("streaming_providers_IE");
        assertEquals(16,providers.getEntries().length);
        fragment.findPreference("streaming_provider_search").callChangeListener("Provider 20");
        assertEquals(2,providers.getEntries().length);
        assertTrue(Arrays.asList(providers.getEntryValues()).contains("99"));
        assertTrue(Arrays.asList(providers.getEntryValues()).contains("20"));
        assertEquals(Collections.singleton("99"),StreamingRepository.selected(host.get()));
    }

}
