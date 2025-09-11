/*
 * Copyright (C) 2025 AxionOS Project
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
package com.android.settings.preferences

import android.os.Bundle
import androidx.annotation.XmlRes
import com.android.internal.logging.nano.MetricsProto
import com.android.settings.SettingsPreferenceFragment
import com.android.settings.search.BaseSearchIndexProvider
import com.android.settings.search.SearchIndexProviderHolder

abstract class BasePreferenceFragment(
    @XmlRes private val prefResId: Int
) : SettingsPreferenceFragment(), SearchIndexProviderHolder {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(prefResId)
    }

    override fun getMetricsCategory(): Int = MetricsProto.MetricsEvent.VIEW_UNKNOWN

    override fun getSearchIndexProvider(): BaseSearchIndexProvider {
        return object : BaseSearchIndexProvider(prefResId) {}
    }
}
