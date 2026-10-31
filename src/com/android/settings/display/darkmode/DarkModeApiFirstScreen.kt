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

package com.android.settings.display.darkmode

import android.Manifest
import android.Manifest.permission.WRITE_SECURE_SETTINGS
import android.app.UiModeManager
import android.content.Context
import android.provider.Settings
import com.android.settings.R
import com.android.settings.accessibility.Flags as AccFlags
import com.android.settings.flags.Flags
import com.android.settings.fuelgauge.batterysaver.BatterySaverPreference
import com.android.settings.fuelgauge.batterysaver.BatterySaverScreen
import com.android.settingslib.metadata.MUSTPASS_SET
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.preconditions.Allowed
import com.android.settingslib.metadata.preferencesapi.preconditions.InvalidPreference
import com.android.settingslib.metadata.preferencesapi.types.CustomEnum
import com.android.settingslib.metadata.preferencesapi.types.EnumApiWithRes
import com.android.settingslib.metadata.preferencesapi.types.TimeOfDay
import java.time.LocalTime

// LINT.IfChange
@ProvidePreferenceScreen(DarkModeApiFirstScreen.KEY)
class DarkModeApiFirstScreen :
    PreferencesApiScreen(
        key = KEY,
        topLevelSettingsCategory = Category.DISPLAY,
        fragment = DarkModeSettingsFragment::class,
        purpose = R.string.dark_ui_mode_purpose_api,
        alreadyPartiallyMigrated = DarkModeScreen::class,
    ) {
    init {
        flag { Flags.catalystMigration26q2() }
        preconditions(R.string.api_dark_theme_screen_preconditions) {
            val isScreenPrecondition =
                if (AccFlags.allowToEnterDarkThemeSettingsWhenBatterySaver()) true
                else !DarkThemeBatterySaver.isLocked(context)
            if (isScreenPrecondition) {
                Allowed
            } else {
                InvalidPreference(
                    otherPreferenceScreenKey = BatterySaverScreen.KEY,
                    otherPreferenceKey = "battery_saver",
                    reason = R.string.api_dark_theme_screen_invalid_preference,
                )
            }
        }
        preference(
            key = RADIO_PREFERENCE_KEY,
            purpose = R.string.dark_theme_mode_selector_purpose,
            type =
                CustomEnum(
                    DarkThemeMode::class,
                    R.string.dark_theme_mode_selector_customenum_description,
                ),
        ) {
            sensitivityLevel(SensitivityLevel.NO_SENSITIVITY)
            get {
                execute {
                    val isExpanded =
                        Settings.Secure.getInt(
                            context.contentResolver,
                            Settings.Secure.ACCESSIBILITY_FORCE_INVERT_COLOR_ENABLED,
                            STANDARD_DARK_THEME,
                        ) == EXPANDED_DARK_THEME

                    if (isExpanded) {
                        DarkThemeMode.EXPANDED
                    } else {
                        DarkThemeMode.STANDARD
                    }
                }
            }

            set {
                permissions(WRITE_SECURE_SETTINGS)
                execute { value ->
                    var darkThemeMode = STANDARD_DARK_THEME
                    when (value) {
                        DarkThemeMode.STANDARD -> darkThemeMode = STANDARD_DARK_THEME
                        DarkThemeMode.EXPANDED -> darkThemeMode = EXPANDED_DARK_THEME
                    }
                    Settings.Secure.putInt(
                        context.contentResolver,
                        Settings.Secure.ACCESSIBILITY_FORCE_INVERT_COLOR_ENABLED,
                        darkThemeMode,
                    )
                }
            }
        }

        preference(
            key = DARK_THEME_START_TIME,
            purpose = R.string.dark_theme_start_time_purpose,
            type = TimeOfDay,
        ) {
            sensitivityLevel(SensitivityLevel.NO_SENSITIVITY)
            tags(MUSTPASS_SET)
            permissions(Manifest.permission.MODIFY_DAY_NIGHT_MODE)

            preconditions(R.string.dark_theme_custom_time_precondition) {
                val uiModeManager = context.getSystemService(UiModeManager::class.java)
                if (
                    uiModeManager?.nightMode == UiModeManager.MODE_NIGHT_CUSTOM &&
                        uiModeManager.nightModeCustomType ==
                            UiModeManager.MODE_NIGHT_CUSTOM_TYPE_SCHEDULE
                ) {
                    Allowed
                } else {
                    InvalidPreference(
                        otherPreferenceScreenKey = KEY,
                        otherPreferenceKey = DarkModeSchedulePreference.KEY,
                        reason = R.string.dark_theme_custom_time_invalid_preference,
                    )
                }
            }

            get {
                execute {
                    val uiModeManager = context.getSystemService(UiModeManager::class.java)
                    uiModeManager?.customNightModeStart ?: LocalTime.of(0, 0)
                }
            }

            set {
                preconditions(R.string.dark_theme_power_saver_precondition) {
                    if (!DarkThemeBatterySaver.isLocked(context)) {
                        Allowed
                    } else {
                        InvalidPreference(
                            otherPreferenceScreenKey = BatterySaverScreen.KEY,
                            otherPreferenceKey = BatterySaverPreference.KEY,
                            reason = R.string.api_dark_theme_screen_invalid_preference,
                        )
                    }
                }
                execute { value ->
                    val uiModeManager = context.getSystemService(UiModeManager::class.java)
                    uiModeManager?.customNightModeStart = value
                }
            }
        }

        preference(
            key = DARK_THEME_END_TIME,
            purpose = R.string.dark_theme_end_time_purpose,
            type = TimeOfDay,
        ) {
            tags(MUSTPASS_SET)
            sensitivityLevel(SensitivityLevel.NO_SENSITIVITY)
            permissions(Manifest.permission.MODIFY_DAY_NIGHT_MODE)
            preconditions(R.string.dark_theme_custom_time_precondition) {
                val uiModeManager = context.getSystemService(UiModeManager::class.java)
                if (
                    uiModeManager?.nightMode == UiModeManager.MODE_NIGHT_CUSTOM &&
                        uiModeManager.nightModeCustomType ==
                            UiModeManager.MODE_NIGHT_CUSTOM_TYPE_SCHEDULE
                ) {
                    Allowed
                } else {
                    InvalidPreference(
                        otherPreferenceScreenKey = DarkModeScreen.KEY,
                        otherPreferenceKey = DarkModeSchedulePreference.KEY,
                        reason = R.string.dark_theme_custom_time_invalid_preference,
                    )
                }
            }

            get {
                execute {
                    val uiModeManager = context.getSystemService(UiModeManager::class.java)
                    uiModeManager?.customNightModeEnd ?: LocalTime.of(0, 0)
                }
            }

            set {
                preconditions(R.string.dark_theme_power_saver_precondition) {
                    if (!DarkThemeBatterySaver.isLocked(context)) {
                        Allowed
                    } else {
                        InvalidPreference(
                            otherPreferenceScreenKey = BatterySaverScreen.KEY,
                            otherPreferenceKey = BatterySaverPreference.KEY,
                            reason = R.string.api_dark_theme_screen_invalid_preference,
                        )
                    }
                }

                execute { value ->
                    val uiModeManager = context.getSystemService(UiModeManager::class.java)
                    uiModeManager?.customNightModeEnd = value
                }
            }
        }
    }

    companion object {
        const val KEY = "api_dark_ui_mode"
        internal const val RADIO_PREFERENCE_KEY = "dark_theme_mode_selector"
        internal const val STANDARD_DARK_THEME = 0
        internal const val EXPANDED_DARK_THEME = 1
        internal const val DARK_THEME_START_TIME = "dark_theme_start_time"
        internal const val DARK_THEME_END_TIME = "dark_theme_end_time"
    }
}

// LINT.ThenChange(DarkModeSettingsFragment.java,
//                 ../DarkUIPreferenceController.java,
//                 ForceInvertPreferenceController.java)

internal enum class DarkThemeMode(override val asApiValue: Int, override val purpose: Int) :
    EnumApiWithRes<Int> {
    STANDARD(
        DarkModeApiFirstScreen.STANDARD_DARK_THEME,
        R.string.accessibility_standard_dark_theme_title,
    ),
    EXPANDED(
        DarkModeApiFirstScreen.EXPANDED_DARK_THEME,
        R.string.accessibility_expanded_dark_theme_title,
    ),
}
