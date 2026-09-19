// Copyright 2026
// Licensed under the Apache License, Version 2.0
package com.archos.mediacenter.video.leanback.tvshow;

import android.database.Cursor;
import android.os.Bundle;

import androidx.loader.content.Loader;

import com.archos.mediacenter.video.R;
import com.archos.mediacenter.video.browser.loader.DocumentaryTvshowsLoader;
import com.archos.mediacenter.video.browser.loader.VideoLoader;
import com.archos.mediacenter.video.tvshow.TvshowSortOrderEntries;
import com.archos.mediaprovider.video.VideoStore;

/** Leanback grid for TV shows identified as documentaries by NOVA's existing metadata. */
public class DocumentaryTvshowsGridFragment extends AllTvshowsGridFragment {

    @Override
    protected String getScreenTitle() {
        return getString(R.string.documentaries);
    }

    @Override
    protected String getScreenTitle(int count, boolean showWatched) {
        return getString(R.string.documentaries_format, count);
    }

    @Override
    protected AllTvshowsGridFragment createReplacementFragment() {
        return new DocumentaryTvshowsGridFragment();
    }

    @Override
    protected Loader<Cursor> createTvshowsLoader(Bundle args) {
        String sortOrder = args == null ? TvshowSortOrderEntries.DEFAULT_SORT : args.getString("sort");
        boolean showWatched = args == null || args.getBoolean("showWatched");
        return new DocumentaryTvshowsLoader(getActivity(),
                VideoStore.Video.VideoColumns.NOVA_PINNED + " DESC, " + sortOrder,
                showWatched, VideoLoader.GRIDVIDEO_THROTTLE, VideoLoader.GRIDVIDEO_THROTTLE_DELAY);
    }
}
