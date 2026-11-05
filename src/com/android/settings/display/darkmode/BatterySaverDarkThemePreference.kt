/*
 * Copyright (C) 2026 The crDroid Android Project
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

package com.android.settings.display.darkmode

import android.content.Context
import androidx.preference.SwitchPreferenceCompat
import com.android.settings.R
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.metadata.BooleanValuePreference
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.UI_ONLY_PREFERENCE
import com.android.settingslib.preference.PreferenceBinding

/**
 * Whether Battery Saver forces the dark theme (and locks the dark theme controls).
 *
 * Backed by [android.provider.Settings.Secure.BATTERY_SAVER_DARK_THEME], default on.
 */
class BatterySaverDarkThemePreference(context: Context, val isUiOnly: Boolean) :
    PreferenceMetadata,
    BooleanValuePreference,
    PreferenceBinding,
    PreferenceLifecycleProvider {

    private val storage = BatterySaverDarkThemeStorage(context)
    private var dependentsObserver: KeyedObserver<String>? = null

    override val key: String
        get() = KEY

    override val purpose: Int
        get() = R.string.battery_saver_dark_theme_purpose

    override val title: Int
        get() = R.string.battery_saver_dark_theme_title

    override val summary: Int
        get() = R.string.battery_saver_dark_theme_summary

    override val keywords: Int
        get() = R.string.keywords_dark_ui_mode

    override val supportsWrite = true

    override val sensitivityLevel
        get() = SensitivityLevel.NO_SENSITIVITY

    override fun tags(context: Context): Array<String> =
        if (isUiOnly) arrayOf(UI_ONLY_PREFERENCE) else super<PreferenceMetadata>.tags(context)

    override fun storage(context: Context): KeyValueStore = storage

    override fun getReadPermissions(context: Context) =
        BatterySaverDarkThemeStorage.getReadPermissions()

    override fun getWritePermissions(context: Context) =
        BatterySaverDarkThemeStorage.getWritePermissions()

    override fun getReadPermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun getWritePermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun createWidget(context: Context) =
        SwitchPreferenceCompat(context).apply { isPersistent = false }

    override fun onStart(context: PreferenceLifecycleContext) {
        // The other controls on this page lock themselves via DarkThemeBatterySaver.isLocked(),
        // which reads this setting. Rebind them when it flips so the lock state is applied
        // live while battery saver is on.
        val observer = KeyedObserver<String> { _, _ ->
            DEPENDENT_KEYS.forEach { context.notifyPreferenceChange(it) }
        }
        dependentsObserver = observer
        storage.addObserver(KEY, observer, HandlerExecutor.main)
    }

    override fun onStop(context: PreferenceLifecycleContext) {
        dependentsObserver?.let {
            storage.removeObserver(KEY, it)
            dependentsObserver = null
        }
    }

    companion object {
        const val KEY = BatterySaverDarkThemeStorage.KEY

        private val DEPENDENT_KEYS =
            listOf(
                DarkModeMainSwitchPreference.KEY,
                DarkModeSchedulePreference.KEY,
                StartTimePreference.KEY,
                EndTimePreference.KEY,
            )
    }
}
