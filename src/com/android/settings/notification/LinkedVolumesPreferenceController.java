/*
 * Copyright (C) 2018 The LineageOS Project
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

package com.android.settings.notification;

import android.content.Context;
import android.os.UserHandle;
import android.provider.DeviceConfig;
import android.provider.Settings;

import com.android.internal.config.sysui.SystemUiDeviceConfigFlags;

import com.android.settings.Utils;
import com.android.settings.core.TogglePreferenceController;

public class LinkedVolumesPreferenceController extends TogglePreferenceController {

    private static final boolean CONFIG_SEPARATE_NOTIFICATION_DEFAULT_VAL = false;

    public LinkedVolumesPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        Settings.Secure.putIntForUser(mContext.getContentResolver(),
                Settings.Secure.VOLUME_LINK_NOTIFICATION, isChecked ? 1 : 0, UserHandle.USER_CURRENT);
        return true;
    }

    @Override
    public boolean isChecked() {
        int defaultValue = DeviceConfig.getBoolean(DeviceConfig.NAMESPACE_SYSTEMUI,
                SystemUiDeviceConfigFlags.VOLUME_SEPARATE_NOTIFICATION,
                CONFIG_SEPARATE_NOTIFICATION_DEFAULT_VAL) ? 0 : 1;
        return Settings.Secure.getIntForUser(mContext.getContentResolver(),
                Settings.Secure.VOLUME_LINK_NOTIFICATION, defaultValue, UserHandle.USER_CURRENT) != 0;
    }

    @Override
    public int getAvailabilityStatus() {
        return Utils.isVoiceCapable(mContext) ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public final boolean isSliceable() {
        return false;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        // not needed since it's not sliceable
        return NO_RES;
    }
}
