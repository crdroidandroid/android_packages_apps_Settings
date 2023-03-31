package com.android.settings.network.telephony;

import android.content.Context;
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;
import android.os.SystemProperties;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.internal.telephony.util.ArrayUtils;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

/**
 * Preference controller for "Force LTE_CA" hack
 */
public class ForceLteCaPreferenceController extends TelephonyTogglePreferenceController
        implements LifecycleObserver, OnStart, OnStop {

    private static final String TAG = "ForceLteCaSettings";

    private static final String KEY_PROP = "persist.sys.radio.force_lte_ca";

    @VisibleForTesting
    Preference mPreference;
    private TelephonyManager mTelephonyManager;
    private PhoneCallStateTelephonyCallback mTelephonyCallback;
    private Integer mCallState;

    public ForceLteCaPreferenceController(Context context, String key) {
        super(context, key);
        mTelephonyManager = context.getSystemService(TelephonyManager.class);
    }

    /**
     * Initial this PreferenceController.
     * @param subId The subscription Id.
     * @return This PreferenceController.
     */
    public ForceLteCaPreferenceController init(int subId) {
        Log.d(TAG, "init: ");
        if (mTelephonyCallback == null) {
            mTelephonyCallback = new PhoneCallStateTelephonyCallback();
        }

        mSubId = subId;

        if (mTelephonyManager == null) {
            mTelephonyManager = mContext.getSystemService(TelephonyManager.class);
        }

        if (SubscriptionManager.isValidSubscriptionId(subId)) {
            mTelephonyManager = mTelephonyManager.createForSubscriptionId(subId);
        }

        PersistableBundle carrierConfig = getCarrierConfigForSubId(subId);
        if (carrierConfig == null) {
            return this;
        }
        return this;
    }

    @Override
    public int getAvailabilityStatus(int subId) {
        init(subId);
        if (SubscriptionManager.isValidSubscriptionId(mSubId)) {
            return AVAILABLE;
        }
        return CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public void onStart() {
        if (mTelephonyCallback == null) {
            return;
        }
        mTelephonyCallback.register(mTelephonyManager);
    }

    @Override
    public void onStop() {
        if (mTelephonyCallback == null) {
            return;
        }
        mTelephonyCallback.unregister();
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (preference == null) {
            return;
        }
        final SwitchPreference switchPreference = (SwitchPreference) preference;
        switchPreference.setEnabled(isUserControlAllowed());
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        if (!SubscriptionManager.isValidSubscriptionId(mSubId)) {
            return false;
        }
        Log.d(TAG, "setChecked: " + isChecked);
        SystemProperties.set(KEY_PROP, String.valueOf(isChecked));
        return true;
    }

    @Override
    public boolean isChecked() {
        return SystemProperties.getBoolean(KEY_PROP, false);
    }

    private boolean isUserControlAllowed() {
        return SubscriptionManager.isValidSubscriptionId(mSubId);
    }

    private class PhoneCallStateTelephonyCallback extends TelephonyCallback implements
            TelephonyCallback.CallStateListener {

        private TelephonyManager mLocalTelephonyManager;

        @Override
        public void onCallStateChanged(int state) {
            mCallState = state;
            updateState(mPreference);
        }

        public void register(TelephonyManager telephonyManager) {
            mLocalTelephonyManager = telephonyManager;

            // assign current call state so that it helps to show correct preference state even
            // before first onCallStateChanged() by initial registration.
            mCallState = mLocalTelephonyManager.getCallState();
            mLocalTelephonyManager.registerTelephonyCallback(
                    mContext.getMainExecutor(), mTelephonyCallback);
        }

        public void unregister() {
            mCallState = null;
            if (mLocalTelephonyManager != null) {
                mLocalTelephonyManager.unregisterTelephonyCallback(this);
            }
        }
    }
}
