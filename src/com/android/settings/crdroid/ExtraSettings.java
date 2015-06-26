/*
 * Copyright (C) 2015 crDroid Android
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.settings.crdroid;

import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceScreen;
import android.provider.SearchIndexableResource;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.Utils;

import java.util.ArrayList;
import java.util.List;

public class ExtraSettings extends SettingsPreferenceFragment
        implements Indexable {

    private static final String TAG = ExtraSettings.class.getSimpleName();

    private static final String KEY_GESTURE_SETTINGS = "gesture_settings";
    private static final String KEY_OCLICK = "oclick";

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.extra_settings);
        final PreferenceScreen prefScreen = getPreferenceScreen();

        // Show gestures only if supported
        Utils.updatePreferenceToSpecificActivityFromMetaDataOrRemove(getActivity(),
                    getPreferenceScreen(), KEY_GESTURE_SETTINGS);
        // Show bluetooth only if supported
        Utils.updatePreferenceToSpecificActivityFromMetaDataOrRemove(getActivity(),
                    getPreferenceScreen(), KEY_OCLICK);
    }

    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                                                                            boolean enabled) {
                    ArrayList<SearchIndexableResource> result =
                            new ArrayList<SearchIndexableResource>();

                    SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.extra_settings;
                    result.add(sir);

                    return result;
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    ArrayList<String> result = new ArrayList<String>();
                    return result;
                }
            };
}
