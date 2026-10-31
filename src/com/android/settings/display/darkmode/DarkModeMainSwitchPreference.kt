/*
 * Copyright (C) 2025 The Android Open Source Project
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
import com.android.settings.R
import com.android.settings.accessibility.Flags
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.metadata.BooleanValuePreference
import com.android.settingslib.metadata.MUSTPASS_SET
import com.android.settingslib.metadata.MUSTPASS_SET
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.UI_ONLY_PREFERENCE
import com.android.settingslib.widget.MainSwitchPreferenceBinding
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability


// LINT.IfChange
class DarkModeMainSwitchPreference(private val dataStore: DarkModeStorage, val isUiOnly: Boolean) :
    PreferenceMetadata, MainSwitchPreferenceBinding, BooleanValuePreference {

    override val key: String
        get() = KEY

    override val purpose: Int
        get() = R.string.dark_ui_activated_purpose

    override val title: Int
        get() = R.string.dark_theme_main_switch_title

    override val indexable
        get() = false

    override fun tags(context: Context): Array<String> {
        if (isUiOnly) {
            return arrayOf(UI_ONLY_PREFERENCE)
        }
        return arrayOf(MUSTPASS_SET)
    }

    override fun getEnabledDescription(): String = if (!Flags.allowToEnterDarkThemeSettingsWhenBatterySaver()) "Battery saver must be turned off." else "Always enabled."

    override fun getEnabledStability() = PreconditionStability.UNSTABLE

    override fun isEnabled(context: Context): Boolean =
        if (!Flags.allowToEnterDarkThemeSettingsWhenBatterySaver())
            !DarkThemeBatterySaver.isLocked(context)
        else true

    override fun storage(context: Context): KeyValueStore = dataStore

    override fun getReadPermissions(context: Context) = DarkModeStorage.getReadPermissions()

    override fun getWritePermissions(context: Context) = DarkModeStorage.getWritePermissions()

    override fun getReadPermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun getWritePermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override val supportsWrite = true
    override val sensitivityLevel
        get() = SensitivityLevel.NO_SENSITIVITY

    companion object {
        const val KEY = "dark_ui_activated"
    }
}
// LINT.ThenChange(DarkModeActivationPreferenceController.java)
