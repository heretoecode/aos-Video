// Copyright 2017 Archos SA
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

package com.archos.mediacenter.video.leanback.search;

import androidx.fragment.app.Fragment;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import androidx.leanback.app.SearchSupportFragment;
import androidx.fragment.app.FragmentActivity;

import android.view.KeyEvent;

import com.archos.filecorelibrary.FileUtils;
import com.archos.mediacenter.video.R;
import com.archos.mediacenter.video.browser.adapters.mappers.VideoCursorMapper;
import com.archos.mediacenter.video.browser.adapters.object.Video;
import com.archos.mediacenter.video.leanback.details.VideoDetailsActivity;
import com.archos.mediacenter.video.leanback.details.VideoDetailsFragment;
import com.archos.mediacenter.video.info.SingleVideoLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class VideoSearchActivity extends FragmentActivity {

    private PreviewSearch previewSearch;
    private final androidx.activity.result.ActivityResultLauncher<Intent> voice = registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(), result -> {
        if(result.getResultCode()==RESULT_OK&&result.getData()!=null&&previewSearch!=null){java.util.ArrayList<String> words=result.getData().getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS);if(words!=null&&!words.isEmpty())previewSearch.acceptVoice(words.get(0));}
    });

    private static final Logger log = LoggerFactory.getLogger(VideoSearchActivity.class);

    public static final String EXTRA_SEARCH_MODE = "searchMode";
    public static final int SEARCH_MODE_ALL = 0;
    public static final int SEARCH_MODE_MOVIE = 1;
    public static final int SEARCH_MODE_EPISODE = 3;
    public static final int SEARCH_MODE_NON_SCRAPED = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        String action = intent != null ? intent.getAction() : null;
        if (Intent.ACTION_VIEW.equals(action)) {
            // this is something we got from the global search bar
            Cursor cursor = null;
            try {
                int videoId = Integer.parseInt(FileUtils.getName(intent.getData()));
                SingleVideoLoader loader = new SingleVideoLoader(this, videoId);
                cursor = getContentResolver().query(loader.getUri(), loader.getProjection(), loader.getSelection(), loader.getSelectionArgs(), loader.getSortOrder());
                if (cursor.getCount() > 0) {
                    VideoCursorMapper cursorMapper = new VideoCursorMapper();
                    cursorMapper.publicBindColumns(cursor);
                    cursor.moveToFirst();
                    Video video = (Video)cursorMapper.publicBind(cursor);

                    Intent activityIntent = new Intent(this, VideoDetailsActivity.class);
                    activityIntent.putExtra(VideoDetailsFragment.EXTRA_VIDEO, video);
                    startActivity(activityIntent);
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
            finish();
            return;
        }

        if(androidx.preference.PreferenceManager.getDefaultSharedPreferences(this).getBoolean("try_new_ui",false)){
            previewSearch=new PreviewSearch(this,getIntent().getIntExtra(EXTRA_SEARCH_MODE,SEARCH_MODE_ALL),savedInstanceState);
            com.archos.mediacenter.video.leanback.TopNavigation nav=new com.archos.mediacenter.video.leanback.TopNavigation(this,previewSearch,index->{
                if(index==5){previewSearch.focusQuery();return;}
                if(index==4){startActivity(new Intent(this,com.archos.mediacenter.video.leanback.settings.VideoSettingsActivity.class));return;}
                Intent home=new Intent(this,com.archos.mediacenter.video.leanback.MainActivityLeanback.class).putExtra("preview_tab",index).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);startActivity(home);finish();
            },previewSearch::atTop);nav.selectTab(5);nav.setScrolled(true);setContentView(nav);getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN);return;
        }

        setContentView(R.layout.androidtv_search_activity);

        Bundle args = new Bundle();
        args.putInt(EXTRA_SEARCH_MODE, getIntent().getIntExtra(EXTRA_SEARCH_MODE, SEARCH_MODE_ALL));

        VideoSearchFragment frag = new VideoSearchFragment();
        frag.setArguments(args);
        getSupportFragmentManager().beginTransaction().add(R.id.video_search_fragment, frag).commit();
    }

    /**
     * Catch the SEARCH key so that the general Android TV search is not launched.
     * Try to relaunch the AVP search instead
     * @param keyCode
     * @param event
     * @return
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_SEARCH && previewSearch != null) {
            Intent speak=new Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL,android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            try{voice.launch(speak);}catch(android.content.ActivityNotFoundException unavailable){previewSearch.focusQuery();}return true;
        }
        if (event.getKeyCode() == KeyEvent.KEYCODE_SEARCH) {
            Fragment f = getSupportFragmentManager().findFragmentById(R.id.video_search_fragment);
            if (f instanceof SearchSupportFragment) {
                ((SearchSupportFragment)f).startRecognition();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event); // default
    }
    @Override protected void onSaveInstanceState(Bundle state){super.onSaveInstanceState(state);if(previewSearch!=null)previewSearch.save(state);}
    @Override
    protected void onPause() {
        // to avoid ACRA report on AFM (amazon)
        // RuntimeException: Unable to pause activity {VideoSearchActivity}:
        // IllegalArgumentException: Service not registered: android.speech.SpeechRecognizer
        try {
            super.onPause();
        } catch (IllegalArgumentException e) {
            log.error("onPause caught IllegalArgumentException", e);
        }
    }
}
