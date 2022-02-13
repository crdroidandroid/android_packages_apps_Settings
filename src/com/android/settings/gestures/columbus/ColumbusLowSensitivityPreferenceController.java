package com.android.settings.gestures.columbus;

import android.content.Context;
import android.content.IntentFilter;
import com.android.settings.slices.SliceBackgroundWorker;
import com.google.android.settings.gestures.columbus.ColumbusTogglePreferenceController;

import android.os.SystemProperties;

public class ColumbusLowSensitivityPreferenceController extends ColumbusTogglePreferenceController {
    public ColumbusLowSensitivityPreferenceController(Context context, String str) {
        super(context, str, 1747);
    }

    @Override // com.android.settings.core.BasePreferenceController
    public int getAvailabilityStatus() {
        if (SystemProperties.getBoolean("persist.columbus.use_ap_sensor", true)){
            return UNSUPPORTED_ON_DEVICE;
        }
        return super.getAvailabilityStatus();
    }
}