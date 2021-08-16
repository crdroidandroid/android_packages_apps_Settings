/*
 * Copyright (C) 2021 crDroid Android Project
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

package com.android.settings.fuelgauge.batterysaver;

import static android.provider.Settings.System.LOW_POWER_REFRESH_RATE;

import android.content.Context;
import android.provider.Settings;
import android.view.Display;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BatterySaverRefreshRatePreferenceController extends TogglePreferenceController {

    private static final String KEY_BATTERY_SAVER_RATE = "battery_saver_rr_switch";

    private List<String> mEntries = new ArrayList<>();
    private List<String> mValues = new ArrayList<>();

    public BatterySaverRefreshRatePreferenceController(Context context) {
        super(context, KEY_BATTERY_SAVER_RATE);

        if (mContext.getResources().getBoolean(R.bool.config_show_refresh_rate_controls)) {
            Display.Mode mode = mContext.getDisplay().getMode();
            Display.Mode[] modes = mContext.getDisplay().getSupportedModes();
            for (Display.Mode m : modes) {
                if (m.getPhysicalWidth() == mode.getPhysicalWidth() &&
                        m.getPhysicalHeight() == mode.getPhysicalHeight()) {
                    mEntries.add(String.format("%.02fHz", m.getRefreshRate())
                            .replaceAll("[\\.,]00", ""));
                    mValues.add(String.format(Locale.US, "%.02f", m.getRefreshRate()));
                }
            }
        }
    }

    @Override
    public boolean isChecked() {
        int val = Settings.System.getInt(mContext.getContentResolver(), LOW_POWER_REFRESH_RATE, 1);
        return val == 1;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        int val = isChecked ? 1 : 0;
        return Settings.System.putInt(mContext.getContentResolver(), LOW_POWER_REFRESH_RATE, val);
    }

    @Override
    public int getAvailabilityStatus() {
        return mEntries.size() > 1 ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_BATTERY_SAVER_RATE;
    }
}
