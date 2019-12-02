/*
 * SPDX-FileCopyrightText: crDroid Android Project
 * SPDX-License-Identifier: GPL-3.0
 */

package com.android.settings.deviceinfo.firmwareversion

import android.content.Context
import android.os.Build
import android.os.SystemProperties
import androidx.preference.Preference
import com.android.settings.R
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.preference.PreferenceBinding

class AboutDeviceNamePreference :
    PreferenceMetadata,
    PreferenceSummaryProvider,
    PreferenceBinding {

    override val key: String
        get() = "about_device_name"

    override val purpose: Int
        get() = R.string.os_firmware_version_purpose

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
