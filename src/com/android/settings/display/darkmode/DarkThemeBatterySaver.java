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

package com.android.settings.display.darkmode;

import android.content.Context;
import android.os.PowerManager;
import android.os.UserHandle;
import android.provider.Settings;

/**
 * Helper for the interaction between Battery Saver and the dark theme.
 *
 * <p>Battery Saver only forces the dark theme as long as the user setting
 * {@code battery_saver_dark_theme} is enabled (default). The dark theme controls must only be
 * locked while that is actually the case.
 */
public final class DarkThemeBatterySaver {

    public static final String SETTING = "battery_saver_dark_theme";

    private DarkThemeBatterySaver() {
    }

    /** Whether Battery Saver is allowed to force the dark theme. */
    public static boolean isForceDarkEnabled(Context context) {
        return Settings.Secure.getIntForUser(context.getContentResolver(), SETTING, 1,
                UserHandle.myUserId()) != 0;
    }

    /** Whether the dark theme UI has to be locked because Battery Saver forces dark theme. */
    public static boolean isLocked(Context context) {
        PowerManager powerManager = context.getSystemService(PowerManager.class);
        return powerManager != null && powerManager.isPowerSaveMode()
                && isForceDarkEnabled(context);
    }
}
