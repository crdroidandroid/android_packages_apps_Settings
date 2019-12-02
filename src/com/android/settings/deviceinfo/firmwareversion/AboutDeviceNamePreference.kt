/*
 * Copyright (C) 2025 crDroid Android Project
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

package com.android.settings.deviceinfo.firmwareversion

import android.content.Context
import android.os.Build
import android.os.SystemProperties
import androidx.preference.Preference
import com.android.settings.R
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.preference.PreferenceBinding

class AboutDeviceNamePreference :
    PreferenceMetadata,
    PreferenceSummaryProvider,
    PreferenceAvailabilityProvider,
    PreferenceBinding {

    override val key: String
        get() = "about_device_name"

    override val title: Int
        get() = R.string.about_device_name

    override fun getSummary(context: Context): CharSequence {
        val deviceBrand = SystemProperties.get(
            KEY_BRAND_NAME_PROP,
            context.getString(R.string.device_info_default)
        )
        val deviceCodename = SystemProperties.get(
            KEY_DEVICE_NAME_PROP,
            context.getString(R.string.device_info_default)
        )
        val deviceModel = Build.MODEL
        val deviceMarketName = SystemProperties.get(
            KEY_MARKET_NAME_PROP,
            "$deviceBrand $deviceModel"
        )
        return "$deviceMarketName | $deviceCodename"
    }

    override fun isAvailable(context: Context) = true

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        // Match old XML: enableCopying="true" and default selectable
        preference.isCopyingEnabled = true
    }

    companion object {
        const val KEY_MARKET_NAME_PROP = "ro.product.marketname"
        const val KEY_BRAND_NAME_PROP = "ro.product.manufacturer"
        const val KEY_DEVICE_NAME_PROP = "ro.product.device"
    }
}
