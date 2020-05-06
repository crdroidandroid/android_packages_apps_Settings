/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.security;

import android.app.AppLockManager;
import android.content.Context;
import android.hardware.face.FaceManager;
import android.hardware.fingerprint.FingerprintManager;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.BasePreferenceController;

import com.android.settings.custom.biometrics.FaceUtils;

public class TopLevelSecurityEntryPreferenceController extends BasePreferenceController {

    public TopLevelSecurityEntryPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public CharSequence getSummary() {
        final FingerprintManager fpm =
                Utils.getFingerprintManagerOrNull(mContext);
        final FaceManager faceManager =
                Utils.getFaceManagerOrNull(mContext);
        final AppLockManager appLockManager =
                Utils.getAppLockManager(mContext);
        CharSequence summary = null;
        if (fpm != null && fpm.isHardwareDetected() && FaceUtils.isFaceUnlockSupported()) {
            summary = mContext.getText(R.string.security_dashboard_summary_face_and_fingerprint);
        } else if (fpm != null && fpm.isHardwareDetected()) {
            summary = mContext.getText(R.string.security_dashboard_summary);
        } else if (faceManager != null && faceManager.isHardwareDetected()) {
            summary = mContext.getText(R.string.security_dashboard_summary_face);
        } else {
            summary = mContext.getText(R.string.security_dashboard_summary_no_fingerprint);
        }
        if (appLockManager == null) {
            return summary;
        } else {
            return summary + ", " + mContext.getText(R.string.applock_title);
        }
    }
}
