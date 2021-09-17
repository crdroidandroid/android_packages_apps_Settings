/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.deviceinfo;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.ims.ImsMmTelManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.DeviceInfoUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static android.content.Context.CLIPBOARD_SERVICE;
import static androidx.lifecycle.Lifecycle.Event;

public class PhoneNumberPreferenceController extends BasePreferenceController
        implements LifecycleObserver {
    private static final String TAG = PhoneNumberPreferenceController.class.getSimpleName();
    // This delay is used to make sure telephony framework has enough time to parse
    // the phone number from the IMS registration indication message.
    private static final long DELAY_MILLIS = 500L;

    private static final String KEY_PHONE_NUMBER = "phone_number";
    private static final String KEY_PREFERENCE_CATEGORY = "basic_info_category";

    private final TelephonyManager mTelephonyManager;
    private final SubscriptionManager mSubscriptionManager;
    private final List<Preference> mPreferenceList = new ArrayList<>();
    private HashMap<Integer, ImsConnector> mImsConnectorMap = new HashMap<>();
    private int mPhoneCount;
    private Handler mHandler;

    private final ImsMmTelManager.RegistrationCallback mImsRegistrationCallback =
            new ImsMmTelManager.RegistrationCallback() {
                @Override
                public void onRegistered(int imsTransportType) {
                    Log.d(TAG, "onRegistered: imsTransportType=" + imsTransportType);
                    if (mHandler.hasMessagesOrCallbacks()) {
                        Log.d(TAG, "onRegistered: optimize to remove unhandled runnables");
                        mHandler.removeCallbacksAndMessages(null);
                    }
                    mHandler.postDelayed(() -> updateState(null), DELAY_MILLIS);
                }
            };

    public PhoneNumberPreferenceController(Context context, String key) {
        super(context, key);
        mTelephonyManager = mContext.getSystemService(TelephonyManager.class);
        mSubscriptionManager = mContext.getSystemService(SubscriptionManager.class);
        mPhoneCount = mTelephonyManager.getPhoneCount();
        mHandler = new Handler(Looper.getMainLooper());
        initImsConnectors();
    }

    @Override
    public int getAvailabilityStatus() {
        return mTelephonyManager.isVoiceCapable() ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public CharSequence getSummary() {
        return getFirstPhoneNumber();
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        final Preference preference = screen.findPreference(getPreferenceKey());
        final PreferenceCategory category = screen.findPreference(KEY_PREFERENCE_CATEGORY);
        mPreferenceList.add(preference);

        final int phonePreferenceOrder = preference.getOrder();
        // Add additional preferences for each sim in the device
        for (int simSlotNumber = 1; simSlotNumber < mTelephonyManager.getPhoneCount();
                simSlotNumber++) {
            final Preference multiSimPreference = createNewPreference(screen.getContext());
            multiSimPreference.setOrder(phonePreferenceOrder + simSlotNumber);
            multiSimPreference.setKey(KEY_PHONE_NUMBER + simSlotNumber);
            multiSimPreference.setSelectable(false);
            category.addPreference(multiSimPreference);
            mPreferenceList.add(multiSimPreference);
        }
    }

    @Override
    public void updateState(Preference preference) {
        for (int simSlotNumber = 0; simSlotNumber < mPreferenceList.size(); simSlotNumber++) {
            final Preference simStatusPreference = mPreferenceList.get(simSlotNumber);
            simStatusPreference.setTitle(getPreferenceTitle(simSlotNumber));
            simStatusPreference.setSummary(getPhoneNumber(simSlotNumber));
        }
    }

    @Override
    public boolean useDynamicSliceSummary() {
        return true;
    }

    @Override
    public void copy() {
        final ClipboardManager clipboard = (ClipboardManager) mContext.getSystemService(
                CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("text", getFirstPhoneNumber()));

        final String toast = mContext.getString(R.string.copyable_slice_toast,
                mContext.getText(R.string.status_number));
        Toast.makeText(mContext, toast, Toast.LENGTH_SHORT).show();
    }

    private CharSequence getFirstPhoneNumber() {
        final List<SubscriptionInfo> subscriptionInfoList =
                mSubscriptionManager.getActiveSubscriptionInfoList();
        if (subscriptionInfoList == null || subscriptionInfoList.isEmpty()) {
            return mContext.getText(R.string.device_info_default);
        }

        // For now, We only return first result for slice view.
        return getFormattedPhoneNumber(subscriptionInfoList.get(0));
    }

    private CharSequence getPhoneNumber(int simSlot) {
        final SubscriptionInfo subscriptionInfo = getSubscriptionInfo(simSlot);
        if (subscriptionInfo == null) {
            return mContext.getText(R.string.device_info_default);
        }

        return getFormattedPhoneNumber(subscriptionInfo);
    }

    private CharSequence getPreferenceTitle(int simSlot) {
        return mTelephonyManager.getPhoneCount() > 1 ? mContext.getString(
                R.string.status_number_sim_slot, simSlot + 1) : mContext.getString(
                R.string.status_number);
    }

    @VisibleForTesting
    SubscriptionInfo getSubscriptionInfo(int simSlot) {
        final List<SubscriptionInfo> subscriptionInfoList =
                mSubscriptionManager.getActiveSubscriptionInfoList();
        if (subscriptionInfoList != null) {
            for (SubscriptionInfo info : subscriptionInfoList) {
                if (info.getSimSlotIndex() == simSlot) {
                    return info;
                }
            }
        }
        return null;
    }

    @VisibleForTesting
    CharSequence getFormattedPhoneNumber(SubscriptionInfo subscriptionInfo) {
        final String phoneNumber = DeviceInfoUtils.getBidiFormattedPhoneNumber(mContext,
                subscriptionInfo);
        return TextUtils.isEmpty(phoneNumber) ? mContext.getString(R.string.device_info_default)
                : phoneNumber;
    }

    @VisibleForTesting
    protected Preference createNewPreference(Context context) {
        return new PhoneNumberSummaryPreference(context);
    }

    public void init(Lifecycle lifecycle) {
        if (null != lifecycle) {
            lifecycle.addObserver(this);
        } else {
            Log.e(TAG, "init: lifecycle is null, invalid param");
        }
    }

    @OnLifecycleEvent(Event.ON_RESUME)
    public void onResume() {
        connect();
    }

    @OnLifecycleEvent(Event.ON_PAUSE)
    public void onPause() {
        disconnect();
        mHandler.removeCallbacksAndMessages(null);
    }

    private void initImsConnectors() {
        for (int slotId = 0; slotId < mPhoneCount; slotId++) {
            ImsConnector imsConnector = new ImsConnector(mContext, slotId,
                    mImsRegistrationCallback);
            mImsConnectorMap.put(Integer.valueOf(slotId), imsConnector);
        }
    }

    private void connect() {
        if (mImsConnectorMap.isEmpty()) {
            Log.e(TAG, "connect: need init ims connectors");
            return;
        }

        int size = mImsConnectorMap.size();
        for (int index = 0; index < size; index++) {
            ImsConnector connector = mImsConnectorMap.get(Integer.valueOf(index));
            if (null != connector) {
                connector.connect();
            }
        }
    }

    private void disconnect() {
        if (mImsConnectorMap.isEmpty()) {
            Log.d(TAG, "disconnect: need do nothing");
            return;
        }

        int size = mImsConnectorMap.size();
        for (int index = 0; index < size; index++) {
            ImsConnector connector = mImsConnectorMap.get(Integer.valueOf(index));
            if (null != connector) {
                connector.disconnect();
            }
        }
    }

    @Override
    protected void finalize() throws Throwable {
        mImsConnectorMap.clear();
        super.finalize();
    }
}
