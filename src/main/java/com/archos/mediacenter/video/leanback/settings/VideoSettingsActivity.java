// Copyright 2026 Courville Software
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.archos.mediacenter.video.leanback.settings;

import android.os.Build;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.fragment.app.Fragment;
import androidx.leanback.preference.LeanbackSettingsFragmentCompat;

import com.archos.mediacenter.video.R;
import com.archos.mediacenter.video.leanback.LeanbackActivity;
import com.archos.mediacenter.video.utils.ThemeManager;

public class VideoSettingsActivity extends LeanbackActivity {
    private com.archos.mediacenter.video.leanback.TopNavigation previewNavigation;

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        // Apply black theme variant for leanback preferences when in black theme
        if (ThemeManager.getInstance(this).isSlateTheme()) {
            setTheme(R.style.MyLeanbackTheme_Preferences_Slate);
        } else if (ThemeManager.getInstance(this).isBlackTheme()) {
            setTheme(R.style.MyLeanbackTheme_Preferences_Black);
        }
        super.onCreate(savedInstanceState);
        if(androidx.preference.PreferenceManager.getDefaultSharedPreferences(this).getBoolean("try_new_ui",false)){
            android.widget.FrameLayout full=new android.widget.FrameLayout(this);full.setId(R.id.settingsFragment);full.setBackgroundColor(0xff0b1b29);com.archos.mediacenter.video.leanback.TopNavigation nav=new com.archos.mediacenter.video.leanback.TopNavigation(this,full,index->{if(index==4)return;if(index==5){startActivity(new android.content.Intent(this,com.archos.mediacenter.video.leanback.search.VideoSearchActivity.class));return;}android.content.Intent intent=new android.content.Intent(this,com.archos.mediacenter.video.leanback.MainActivityLeanback.class);intent.putExtra("preview_tab",index);intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP);startActivity(intent);finish();},()->false);previewNavigation=nav;nav.selectTab(4);nav.setScrolled(true);setContentView(nav);new androidx.core.view.WindowInsetsControllerCompat(getWindow(),nav).hide(androidx.core.view.WindowInsetsCompat.Type.systemBars());getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN);
            if(savedInstanceState==null)getSupportFragmentManager().beginTransaction().replace(R.id.settingsFragment,new VideoSettingsFragment.PrefsFragment()).commit();
        }else setContentView(R.layout.activity_video_settings);
        if (Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, R.anim.slide_in_from_right, 0);
        } else {
            overridePendingTransition(R.anim.slide_in_from_right, 0);
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Fragment fragment = getSupportFragmentManager()
                        .findFragmentById(R.id.settingsFragment);
                if (fragment instanceof LeanbackSettingsFragmentCompat
                        && fragment.getChildFragmentManager().popBackStackImmediate()) {
                    return;
                }
                if(fragment instanceof androidx.preference.PreferenceFragmentCompat){androidx.recyclerview.widget.RecyclerView list=((androidx.preference.PreferenceFragmentCompat)fragment).getListView();if(list.hasFocus()&&list.getTag() instanceof android.view.View){((android.view.View)list.getTag()).requestFocus();return;}}
                if(previewNavigation!=null&&previewNavigation.focusNavigation())return;
                finish();
                if (Build.VERSION.SDK_INT >= 34) {
                    overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, R.anim.slide_out_to_right);
                } else {
                    overridePendingTransition(0, R.anim.slide_out_to_right);
                }
            }
        });
    }

    private int getResultCode() {
        // This is a workaround to get the current result code
        // since there's no public API for it
        return 0; // We can't easily get this, so we'll log differently
    }

    @Override
    public void finish() {
        super.finish();
    }

}
