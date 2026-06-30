/*
 * SPDX-FileCopyrightText: crDroid Android Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.display.darkmode

import android.content.Context
import android.view.accessibility.Flags
import androidx.preference.Preference
import com.android.settings.R
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.preference.PreferenceBinding

class DarkThemePerAppPreference(
    private val modeStorage: DarkThemeModeStorage,
) : PreferenceMetadata, PreferenceBinding, PreferenceAvailabilityProvider {

    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.dark_theme_per_app_title

    override val summary: Int
        get() = R.string.dark_theme_per_app_summary

    override fun isAvailable(context: Context): Boolean =
        Flags.forceInvertColor() && isExpandedDarkModeSelected()

    override fun dependencies(context: Context): Array<String> =
        arrayOf(ExpandedDarkModeSelectorPreference.KEY)

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        preference.fragment = DarkThemePerAppSettings::class.java.name
    }

    private fun isExpandedDarkModeSelected(): Boolean =
        modeStorage.getValue(ExpandedDarkModeSelectorPreference.KEY, Boolean::class.java) == true

    companion object {
        const val KEY = "dark_theme_per_app_settings"
    }
}
