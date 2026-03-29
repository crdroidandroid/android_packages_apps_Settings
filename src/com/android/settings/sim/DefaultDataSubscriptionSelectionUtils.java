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

package com.android.settings.sim;

import android.content.Context;
import android.content.SharedPreferences;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.android.settings.network.SubscriptionUtil;

import java.util.List;

/**
 * Persists the user selected default data subscription using reboot-stable identifiers.
 *
 * <p>Subscription ids can change after reboot, so we remember the selected ICCID and slot index
 * and use them to restore the preferred data SIM if the framework reports a missing default.
 */
public final class DefaultDataSubscriptionSelectionUtils {
    private static final String TAG = "DefaultDataSubSelect";

    @VisibleForTesting
    static final String PREFS_NAME = "default_data_subscription_selection";

    @VisibleForTesting
    static final String KEY_ICC_ID = "icc_id";

    @VisibleForTesting
    static final String KEY_SLOT_INDEX = "slot_index";

    private DefaultDataSubscriptionSelectionUtils() {}

    public static void rememberSelection(
            @Nullable Context context,
            @Nullable SubscriptionManager subscriptionManager,
            int subId) {
        if (context == null) {
            return;
        }
        if (!SubscriptionManager.isValidSubscriptionId(subId)) {
            clearSelection(context);
            return;
        }
        if (subscriptionManager == null) {
            return;
        }

        SubscriptionInfo subInfo = subscriptionManager.getActiveSubscriptionInfo(subId);
        if (subInfo == null) {
            subInfo = SubscriptionUtil.getSubById(subscriptionManager, subId);
        }
        if (subInfo == null) {
            Log.w(TAG, "Unable to persist default data SIM for missing subId: " + subId);
            return;
        }

        final SharedPreferences.Editor editor = getPrefs(context).edit();
        final String iccId = subInfo.getIccId();
        if (TextUtils.isEmpty(iccId)) {
            editor.remove(KEY_ICC_ID);
        } else {
            editor.putString(KEY_ICC_ID, iccId);
        }
        editor.putInt(KEY_SLOT_INDEX, subInfo.getSimSlotIndex()).apply();
        Log.d(TAG, "Remembered default data SIM. subId=" + subId
                + ", slotIndex=" + subInfo.getSimSlotIndex());
    }

    public static boolean restoreSelectionIfNeeded(
            @Nullable Context context,
            @Nullable SubscriptionManager subscriptionManager) {
        if (context == null || subscriptionManager == null) {
            return false;
        }

        final int currentDefaultDataSubId = subscriptionManager.getDefaultDataSubscriptionId();
        if (SubscriptionManager.isUsableSubscriptionId(currentDefaultDataSubId)
                && subscriptionManager.isActiveSubscriptionId(currentDefaultDataSubId)) {
            rememberSelection(context, subscriptionManager, currentDefaultDataSubId);
            return false;
        }

        final SharedPreferences prefs = getPrefs(context);
        final String iccId = prefs.getString(KEY_ICC_ID, null);
        final int slotIndex =
                prefs.getInt(KEY_SLOT_INDEX, SubscriptionManager.INVALID_SIM_SLOT_INDEX);
        if (TextUtils.isEmpty(iccId)
                && slotIndex == SubscriptionManager.INVALID_SIM_SLOT_INDEX) {
            return false;
        }

        final SubscriptionInfo matchedSubInfo =
                findMatchingSubscription(
                        SubscriptionUtil.getActiveSubscriptions(subscriptionManager),
                        iccId,
                        slotIndex);
        if (matchedSubInfo == null) {
            Log.i(TAG, "Unable to restore default data SIM. No active subscription matched.");
            return false;
        }

        final int restoredSubId = matchedSubInfo.getSubscriptionId();
        if (!SubscriptionManager.isUsableSubscriptionId(restoredSubId)
                || !subscriptionManager.isActiveSubscriptionId(restoredSubId)) {
            Log.i(TAG, "Unable to restore default data SIM. Invalid restored subId: "
                    + restoredSubId);
            return false;
        }

        Log.i(TAG, "Restoring default data SIM to subId=" + restoredSubId);
        subscriptionManager.setDefaultDataSubId(restoredSubId);
        rememberSelection(context, subscriptionManager, restoredSubId);
        return true;
    }

    @VisibleForTesting
    static void clearSelection(Context context) {
        getPrefs(context).edit().clear().apply();
    }

    @Nullable
    @VisibleForTesting
    static SubscriptionInfo findMatchingSubscription(
            List<SubscriptionInfo> activeSubscriptions,
            @Nullable String iccId,
            int slotIndex) {
        if (!TextUtils.isEmpty(iccId)) {
            for (SubscriptionInfo subInfo : activeSubscriptions) {
                if (TextUtils.equals(iccId, subInfo.getIccId())) {
                    return subInfo;
                }
            }
        }

        if (slotIndex == SubscriptionManager.INVALID_SIM_SLOT_INDEX) {
            return null;
        }
        for (SubscriptionInfo subInfo : activeSubscriptions) {
            if (subInfo.getSimSlotIndex() == slotIndex) {
                return subInfo;
            }
        }
        return null;
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
