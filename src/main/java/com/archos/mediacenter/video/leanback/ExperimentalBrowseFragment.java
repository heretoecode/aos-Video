package com.archos.mediacenter.video.leanback;

import android.os.Bundle;
import android.view.*;
import androidx.leanback.app.BrowseSupportFragment;
import androidx.preference.PreferenceManager;
import com.archos.mediacenter.video.R;

/** Removes the legacy title structurally, so Leanback cannot restore it on selection. */
public class ExperimentalBrowseFragment extends BrowseSupportFragment {
    protected boolean usesTopNavigation() {
        return PreferenceManager.getDefaultSharedPreferences(requireContext()).getBoolean("try_new_ui", false);
    }
    @Override public void onCreate(Bundle state) {
        if (usesTopNavigation()) requireContext().getTheme().applyStyle(R.style.NovaPreviewHomeOverlay, true);
        super.onCreate(state);
    }
    @Override public View onInflateTitleView(LayoutInflater inflater, ViewGroup parent, Bundle state) {
        if (!usesTopNavigation()) return super.onInflateTitleView(inflater, parent, state);
        View empty = new View(requireContext());
        empty.setLayoutParams(new ViewGroup.LayoutParams(0, 0));
        empty.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        return empty;
    }
}
