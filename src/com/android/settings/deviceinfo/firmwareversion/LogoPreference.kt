/*
 * SPDX-FileCopyrightText: crDroid Android Project
 * SPDX-License-Identifier: GPL-3.0
 */

package com.android.settings.deviceinfo.firmwareversion

import android.content.Context
import androidx.preference.Preference
import com.android.settings.R
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.preference.PreferenceBinding

class LogoPreference :
    PreferenceMetadata,
    PreferenceBinding {

    override val key: String
        get() = "crdroid_logo"

    override val purpose: Int
        get() = R.string.os_firmware_version_purpose

    // No title for this item
    override val title: Int
        get() = 0

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        preference.layoutResource = R.layout.crdroid_logo
        preference.isSelectable = false
    }
}
