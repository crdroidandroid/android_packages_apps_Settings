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
import android.location.LocationManager
import androidx.preference.DropDownPreference
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.display.TwilightLocationDialog
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.metadata.DiscreteStringValue
import com.android.settingslib.metadata.PersistentPreference
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.UI_ONLY_PREFERENCE
import com.android.settingslib.preference.PreferenceBinding

// LINT.IfChange
class DarkModeSchedulePreference(
    context: Context,
    val isUiOnly: Boolean,
    private val scheduleStorage: DarkModeScheduleStorage = DarkModeScheduleStorage(context),
    private val bedtimeSettings: BedtimeSettings = BedtimeSettings(context),
) :
    PersistentPreference<String>,
    DiscreteStringValue,
    PreferenceBinding,
    PreferenceSummaryProvider,
    Preference.OnPreferenceChangeListener {

    override val key: String
        get() = KEY

    override val purpose: Int
        get() = R.string.dark_ui_auto_mode_purpose

    override val keywords: Int
        get() = R.string.keywords_dark_ui_mode

    override val title: Int
        get() = R.string.dark_ui_auto_mode_title

    override val values: Int
        get() =
            if (bedtimeSettings.getBedtimeSettingsIntent() != null) {
                R.array.dark_ui_scheduler_with_bedtime_preference_titles
            } else {
                R.array.dark_ui_scheduler_preference_titles
            }

    override val valuesDescription: Int
        get() =
            if (bedtimeSettings.getBedtimeSettingsIntent() != null) {
                R.array.dark_ui_scheduler_with_bedtime_preference_titles
            } else {
                R.array.dark_ui_scheduler_preference_titles
            }

    override val valueType: Class<String>
        get() = String::class.javaObjectType

    override val sensitivityLevel
        get() = SensitivityLevel.NO_SENSITIVITY

    override fun tags(context: Context): Array<String> {
        if (isUiOnly) {
            return arrayOf(UI_ONLY_PREFERENCE)
        }
        return super.tags(context)
    }

    override fun storage(context: Context): KeyValueStore = scheduleStorage

    override fun getReadPermissions(context: Context) = DarkModeScheduleStorage.getReadPermissions()

    override fun getWritePermissions(context: Context) =
        DarkModeScheduleStorage.getWritePermissions()

    override fun getWritePermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override val supportsWrite = true

    override fun getSummary(context: Context): CharSequence? = "%s"

    override fun createWidget(context: Context) = DropDownPreference(context)

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        preference as DropDownPreference
        preference.setValue(scheduleStorage.getString(KEY))
        preference.onPreferenceChangeListener = this
    }

    override fun isEnabled(context: Context): Boolean =
        !DarkThemeBatterySaver.isLocked(context)

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        if (newValue == (preference as DropDownPreference).value) {
            return false
        }

        val context = preference.context
        if (newValue == context.getString(R.string.dark_ui_auto_mode_auto)) {
            val locationManager = context.getSystemService(LocationManager::class.java)
            if (locationManager?.isLocationEnabled == false) {
                TwilightLocationDialog.showLocationOff(context)
                return false
            }

            if (locationManager?.lastLocation == null) {
                TwilightLocationDialog.showLocationPending(context)
                return false
            }
        }
        preference.setValue(newValue as String)
        return true
    }

    companion object {
        const val KEY = "dark_ui_auto_mode"
    }
}
// LINT.ThenChange(DarkModeScheduleSelectorController.java)
