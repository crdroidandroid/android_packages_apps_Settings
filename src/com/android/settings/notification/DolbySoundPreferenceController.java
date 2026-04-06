/*
 * Copyright (C) 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.notification;

import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.dashboard.DashboardFeatureProvider;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.drawer.Tile;

/**
 * Shows a Dolby dashboard tile above Spatial audio when a Dolby package is present.
 */
public class DolbySoundPreferenceController extends BasePreferenceController {

    public DolbySoundPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return DolbyTileUtils.findDolbyTile(mContext) == null
                ? CONDITIONALLY_UNAVAILABLE : AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        updateState(screen.findPreference(getPreferenceKey()));
    }

    @Override
    public void updateState(@Nullable Preference preference) {
        if (preference == null) {
            return;
        }

        final Tile tile = DolbyTileUtils.findDolbyTile(mContext);
        if (tile == null) {
            preference.setVisible(false);
            return;
        }

        preference.setVisible(true);
        preference.setTitle(tile.getTitle(mContext));
        preference.setSummary(tile.getSummary(mContext));

        final Icon icon = tile.getIcon(mContext);
        final Drawable drawable = icon == null ? null : icon.loadDrawable(mContext);
        if (drawable != null) {
            preference.setIcon(drawable);
        }

        preference.setOnPreferenceClickListener(clickedPreference -> {
            final FragmentActivity activity = getActivity(clickedPreference.getContext());
            if (activity == null) {
                return false;
            }
            final DashboardFeatureProvider dashboardFeatureProvider =
                    FeatureFactory.getFeatureFactory().getDashboardFeatureProvider();
            dashboardFeatureProvider.openTileIntent(activity, tile);
            return true;
        });
    }

    @Nullable
    private FragmentActivity getActivity(@NonNull Context context) {
        Context current = context;
        while (current instanceof ContextWrapper) {
            if (current instanceof FragmentActivity) {
                return (FragmentActivity) current;
            }
            current = ((ContextWrapper) current).getBaseContext();
        }
        return null;
    }
}
