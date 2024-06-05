/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.wifi.repository;

import static android.net.TetheringManager.TETHERING_WIFI;
import static android.net.wifi.SoftApConfiguration.BAND_2GHZ;
import static android.net.wifi.SoftApConfiguration.BAND_5GHZ;
import static android.net.wifi.SoftApConfiguration.BAND_6GHZ;
import static android.net.wifi.SoftApConfiguration.SECURITY_TYPE_OPEN;
import static android.net.wifi.SoftApConfiguration.SECURITY_TYPE_WPA3_SAE;
import static android.net.wifi.SoftApConfiguration.SECURITY_TYPE_WPA3_SAE_TRANSITION;
import static android.net.wifi.WifiAvailableChannel.OP_MODE_SAP;
import static android.net.wifi.WifiManager.WIFI_AP_STATE_DISABLED;
import static android.net.wifi.WifiManager.WIFI_AP_STATE_ENABLED;

import android.content.Context;
import android.net.TetheringManager;
import android.net.wifi.SoftApCapability;
import android.net.wifi.SoftApConfiguration;
import android.net.wifi.WifiAvailableChannel;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiScanner;
import android.net.wifi.WifiSsid;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseIntArray;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.android.settings.R;
import com.android.settings.flags.Flags;
import com.android.settings.overlay.FeatureFactory;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Wi-Fi Hotspot Repository
 */
public class WifiHotspotRepository {
    private static final String TAG = "WifiHotspotRepository";

    private static final int RESTART_INTERVAL_MS = 100;

    /** Wi-Fi hotspot band 2.4GHz and 5GHz. */
    public static final int BAND_2GHZ_5GHZ = BAND_2GHZ | BAND_5GHZ;
    /** Wi-Fi hotspot band 2.4GHz and 5GHz and 6GHz. */
    public static final int BAND_2GHZ_5GHZ_6GHZ = BAND_2GHZ | BAND_5GHZ | BAND_6GHZ;

    /** Wi-Fi hotspot speed unknown. */
    public static final int SPEED_UNKNOWN = 0;
    /** Wi-Fi hotspot speed 2.4GHz. */
    public static final int SPEED_2GHZ = 1;
    /** Wi-Fi hotspot speed 5GHz. */
    public static final int SPEED_5GHZ = 2;
    /** Wi-Fi hotspot speed 2.4GHz and 5GHz. */
    public static final int SPEED_2GHZ_5GHZ = 3;
    /** Wi-Fi hotspot speed 6GHz. */
    public static final int SPEED_6GHZ = 4;
    /** Wi-Fi hotspot speed 2.4GHz and 6GHz. */
    public static final int SPEED_2GHZ_6GHZ = 5;

    private final Context mAppContext;
    private final WifiManager mWifiManager;
    private final TetheringManager mTetheringManager;

    protected String mLastPassword;
    protected LastPasswordListener mLastPasswordListener = new LastPasswordListener();

    protected MutableLiveData<Integer> mSecurityType;
    protected MutableLiveData<Integer> mSpeedType;

    protected Boolean mIsDualBand;
    protected Boolean mIs5gBandSupported;
    protected SapBand mBand5g = new SapBand(WifiScanner.WIFI_BAND_5_GHZ_WITH_DFS);
    protected MutableLiveData<Boolean> m5gAvailable;
    protected Boolean mIs6gBandSupported;
    protected SapBand mBand6g = new SapBand(WifiScanner.WIFI_BAND_6_GHZ);
    protected MutableLiveData<Boolean> m6gAvailable;

    @VisibleForTesting
    Boolean mIsConfigShowSpeed;
    private Boolean mIsSpeedFeatureAvailable;
    private Boolean mIsWpa3TransitionAllowedFor2g6gDbs;

    @VisibleForTesting
    SoftApCallback mSoftApCallback = new SoftApCallback();
    @VisibleForTesting
    StartTetheringCallback mStartTetheringCallback;
    @VisibleForTesting
    int mWifiApState = WIFI_AP_STATE_DISABLED;

    @VisibleForTesting
    boolean mIsRestarting;
    @VisibleForTesting
    MutableLiveData<Boolean> mRestarting;

    public WifiHotspotRepository(@NonNull Context appContext, @NonNull WifiManager wifiManager,
            @NonNull TetheringManager tetheringManager) {
        mAppContext = appContext;
        mWifiManager = wifiManager;
        mTetheringManager = tetheringManager;
        mWifiManager.registerSoftApCallback(mAppContext.getMainExecutor(), mSoftApCallback);
    }

    /**
     * Query the last configured Tethered Ap Passphrase since boot.
     */
    public void queryLastPasswordIfNeeded() {
        SoftApConfiguration config = mWifiManager.getSoftApConfiguration();
        if (config.getSecurityType() != SoftApConfiguration.SECURITY_TYPE_OPEN) {
            return;
        }
        mWifiManager.queryLastConfiguredTetheredApPassphraseSinceBoot(mAppContext.getMainExecutor(),
                mLastPasswordListener);
    }

    /**
     * Generate password.
     */
    public String generatePassword() {
        return !TextUtils.isEmpty(mLastPassword) ? mLastPassword : generateRandomPassword();
    }

    @VisibleForTesting
    String generatePassword(SoftApConfiguration config) {
        String password = config.getPassphrase();
        if (TextUtils.isEmpty(password)) {
            password = generatePassword();
        }
        return password;
    }

    private class LastPasswordListener implements Consumer<String> {
        @Override
        public void accept(String password) {
            mLastPassword = password;
        }
    }

    private static String generateRandomPassword() {
        String randomUUID = UUID.randomUUID().toString();
        //first 12 chars from xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx
        return randomUUID.substring(0, 8) + randomUUID.substring(9, 13);
    }

    /**
     * Gets the Wi-Fi tethered AP Configuration.
     *
     * @return AP details in {@link SoftApConfiguration}
     */
    public SoftApConfiguration getSoftApConfiguration() {
        return mWifiManager.getSoftApConfiguration();
    }

    /**
     * Sets the tethered Wi-Fi AP Configuration.
     *
     * @param config A valid SoftApConfiguration specifying the configuration of the SAP.
     */
    public void setSoftApConfiguration(@NonNull SoftApConfiguration config) {
        if (mIsRestarting) {
            Log.e(TAG, "Skip setSoftApConfiguration because hotspot is restarting.");
            return;
        }
        mWifiManager.setSoftApConfiguration(config);
        refresh();
        restartTetheringIfNeeded();
    }

    /**
     * Refresh data from the SoftApConfiguration.
     */
    public void refresh() {
        updateSecurityType();
        update6gAvailable();
        update5gAvailable();
        updateSpeedType();
    }

    /**
     * Gets SecurityType LiveData
     */
    public LiveData<Integer> getSecurityType() {
        if (mSecurityType == null) {
            mSecurityType = new MutableLiveData<>();
            updateSecurityType();
            log("getSecurityType():" + mSecurityType.getValue());
        }
        return mSecurityType;
    }

    protected void updateSecurityType() {
        if (mSecurityType == null) {
            return;
        }
        SoftApConfiguration config = mWifiManager.getSoftApConfiguration();
        int securityType = (config != null) ? config.getSecurityType() : SECURITY_TYPE_OPEN;
        log("updateSecurityType(), securityType:" + securityType);
        mSecurityType.setValue(securityType);
    }

    /**
     * Sets SecurityType
     *
     * @param securityType the Wi-Fi hotspot security type.
     */
    public void setSecurityType(int securityType) {
        log("setSecurityType():" + securityType);
        if (mSecurityType == null) {
            getSecurityType();
        }
        if (securityType == mSecurityType.getValue()) {
            Log.w(TAG, "setSecurityType() is no changed! mSecurityType:"
                    + mSecurityType.getValue());
            return;
        }
        SoftApConfiguration config = mWifiManager.getSoftApConfiguration();
        if (config == null) {
            mSecurityType.setValue(SECURITY_TYPE_OPEN);
            Log.e(TAG, "setSecurityType(), WifiManager#getSoftApConfiguration() return null!");
            return;
        }
        SoftApConfiguration.Builder configBuilder = new SoftApConfiguration.Builder(config);
        String passphrase = (securityType == SECURITY_TYPE_OPEN) ? null : generatePassword(config);
        configBuilder.setPassphrase(passphrase, securityType);
        setSoftApConfiguration(configBuilder.build());

        mWifiManager.queryLastConfiguredTetheredApPassphraseSinceBoot(
                mAppContext.getMainExecutor(), mLastPasswordListener);
    }

    /**
     * Gets SpeedType LiveData
     */
    public LiveData<Integer> getSpeedType() {
        if (mSpeedType == null) {
            mSpeedType = new MutableLiveData<>();
            updateSpeedType();
            log("getSpeedType():" + mSpeedType.getValue());
        }
        return mSpeedType;
    }

    /**
     * Get the intended speed type of a SoftApConfiguration, taking into account the currently
     * available channels and dual band capabilities.
     *
     * Single-band configurations may be upgraded to DBS by the framework if available.
     * (see config_wifiSoftapUpgradeTetheredTo2g5gBridgedIfBandsAreSubset).
     */
    private int getSpeedTypeOfConfiguration(@NonNull SoftApConfiguration config) {
        boolean specifies2ghz = false;
        boolean specifies5ghz = false;
        boolean specifies6ghz = false;
        SparseIntArray configuredChannels = config.getChannels();
        for (int i = 0; i < configuredChannels.size(); i++) {
            int band = configuredChannels.keyAt(i);
            if ((band & BAND_2GHZ) != 0) specifies2ghz = true;
            if ((band & BAND_5GHZ) != 0) specifies5ghz = true;
            if ((band & BAND_6GHZ) != 0) specifies6ghz = true;
        }
        log("getSpeedTypeOfConfiguration(): channels=" + configuredChannels
                + ", specifies2ghz=" + specifies2ghz
                + ", specifies5ghz=" + specifies5ghz
                + ", specifies6ghz=" + specifies6ghz
        );

        // Check configured bands in order of compatibility.
        if (specifies6ghz && is6gAvailable()) {
            if (isDualBand() && Flags.enable2And6GhzHotspotSpeed()) return SPEED_2GHZ_6GHZ;
            return SPEED_6GHZ;
        }

        if (specifies5ghz && is5gAvailable()) {
            if (isDualBand()) return SPEED_2GHZ_5GHZ;
            return SPEED_5GHZ;
        }

        if (specifies2ghz) { // Assume 2 GHz is always available
            // Upgrade to 2 + 5 GHz if available
            if (isDualBand() && is5gAvailable()) return SPEED_2GHZ_5GHZ;
            return SPEED_2GHZ;
        }

        return SPEED_UNKNOWN;
    }

    protected void updateSpeedType() {
        if (mSpeedType == null) {
            return;
        }
        SoftApConfiguration config = mWifiManager.getSoftApConfiguration();
        if (config == null) {
            mSpeedType.setValue(SPEED_UNKNOWN);
            return;
        }

        int speedType = getSpeedTypeOfConfiguration(config);
        log("updateSpeedType():" + speedType);
        mSpeedType.setValue(speedType);
    }

    private boolean has6Ghz(@NonNull SoftApConfiguration config) {
        SparseIntArray channels = config.getChannels();
        for (int i = 0; i < channels.size(); i++) {
            if ((channels.keyAt(i) & BAND_6GHZ) != 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Sets SpeedType
     *
     * @param speedType the Wi-Fi hotspot speed type.
     */
    public void setSpeedType(int speedType) {
        log("setSpeedType():" + speedType);
        if (mSpeedType == null) {
            getSpeedType();
        }
        if (speedType == mSpeedType.getValue()) {
            Log.w(TAG, "setSpeedType() is no changed! mSpeedType:" + mSpeedType.getValue());
            return;
        }

        SoftApConfiguration config = mWifiManager.getSoftApConfiguration();
        if (config == null) {
            mSpeedType.setValue(SPEED_UNKNOWN);
            Log.e(TAG, "setSpeedType(), WifiManager#getSoftApConfiguration() return null!");
            return;
        }
        SoftApConfiguration.Builder configBuilder = new SoftApConfiguration.Builder(config);

        boolean newSpeedHas6g = false;
        switch (speedType) {
            case SPEED_2GHZ:
                log("setSpeedType(), setBand(BAND_2GHZ)");
                configBuilder.setBand(BAND_2GHZ);
                break;
            case SPEED_5GHZ:
                log("setSpeedType(), setBand(BAND_2GHZ_5GHZ)");
                configBuilder.setBand(BAND_2GHZ_5GHZ);
                break;
            case SPEED_6GHZ:
                log("setSpeedType(), setBand(BAND_2GHZ_5GHZ_6GHZ)");
                configBuilder.setBand(BAND_2GHZ_5GHZ_6GHZ);
                newSpeedHas6g = true;
                break;
            case SPEED_2GHZ_5GHZ:
                log("setSpeedType(), setBands({BAND_2GHZ, BAND_2GHZ_5GHZ})");
                configBuilder.setBands(new int[]{BAND_2GHZ, BAND_2GHZ_5GHZ});
                break;
            case SPEED_2GHZ_6GHZ:
                log("setSpeedType(), setBands({BAND_2GHZ, BAND_2GHZ_5GHZ_6GHZ})");
                configBuilder.setBands(new int[]{BAND_2GHZ, BAND_2GHZ_5GHZ_6GHZ});
                newSpeedHas6g = true;
                break;
        }

        if (newSpeedHas6g && config.getSecurityType() != SECURITY_TYPE_WPA3_SAE) {
            // If we're moving to 6Ghz, set the security type to WPA3-SAE since 6GHz requires it.
            String password = generatePassword(config);
            if (speedType == SPEED_2GHZ_6GHZ && isWpa3TransitionAllowedFor2g6gDbs()) {
                log("setSpeedType(), setPassphrase(SECURITY_TYPE_WPA3_SAE_TRANSITION)");
                configBuilder.setPassphrase(password, SECURITY_TYPE_WPA3_SAE_TRANSITION);
            } else {
                log("setSpeedType(), setPassphrase(SECURITY_TYPE_WPA3_SAE)");
                configBuilder.setPassphrase(password, SECURITY_TYPE_WPA3_SAE);
            }
        } else if (has6Ghz(config) && !newSpeedHas6g) {
            // If we're moving away from 6Ghz, reset the security type back to WPA2/WPA3 transition
            // for maximum compatibility.
            String passphrase = generatePassword(config);
            if (passphrase.length() >= 8) {
                log("setSpeedType(), setPassphrase(SECURITY_TYPE_WPA3_SAE_TRANSITION)");
                configBuilder.setPassphrase(passphrase, SECURITY_TYPE_WPA3_SAE_TRANSITION);
            }
        }

        setSoftApConfiguration(configBuilder.build());
    }

    /**
     * Return whether Wi-Fi Dual Band is supported or not.
     *
     * @return {@code true} if Wi-Fi Dual Band is supported
     */
    public boolean isDualBand() {
        if (mIsDualBand == null) {
            mIsDualBand = mWifiManager.isBridgedApConcurrencySupported();
            log("isDualBand():" + mIsDualBand);
        }
        return mIsDualBand;
    }

    /**
     * Return whether Wi-Fi 5 GHz band is supported or not.
     *
     * @return {@code true} if Wi-Fi 5 GHz Band is supported
     */
    public boolean is5GHzBandSupported() {
        if (mIs5gBandSupported == null) {
            mIs5gBandSupported = mWifiManager.is5GHzBandSupported();
            log("is5GHzBandSupported():" + mIs5gBandSupported);
        }
        return mIs5gBandSupported;
    }

    /**
     * Return whether Wi-Fi Hotspot 5 GHz band is available or not.
     *
     * @return {@code true} if Wi-Fi Hotspot 5 GHz Band is available
     */
    public boolean is5gAvailable() {
        return is5GHzBandSupported();
    }

    /**
     * Gets is5gAvailable LiveData
     */
    public LiveData<Boolean> get5gAvailable() {
        if (m5gAvailable == null) {
            m5gAvailable = new MutableLiveData<>();
            m5gAvailable.setValue(is5gAvailable());
        }
        return m5gAvailable;
    }

    protected void update5gAvailable() {
        if (m5gAvailable != null) {
            m5gAvailable.setValue(is5gAvailable());
        }
    }

    /**
     * Return whether Wi-Fi 6 GHz band is supported or not.
     *
     * @return {@code true} if Wi-Fi 6 GHz Band is supported
     */
    public boolean is6GHzBandSupported() {
        if (mIs6gBandSupported == null) {
            mIs6gBandSupported = mWifiManager.is6GHzBandSupported();
            log("is6GHzBandSupported():" + mIs6gBandSupported);
        }
        return mIs6gBandSupported;
    }

    /**
     * Return whether Wi-Fi Hotspot 6 GHz band is available or not.
     *
     * @return {@code true} if Wi-Fi Hotspot 6 GHz Band is available
     */
    public boolean is6gAvailable() {
        return is6GHzBandSupported();
    }

    /**
     * Returns whether WPA3-SAE Transition is allowed for 2 and 6 GHz DBS. If so, the HAL will
     * use WPA3-SAE for the 6 GHz instance.
     */
    public boolean isWpa3TransitionAllowedFor2g6gDbs() {
        if (mIsWpa3TransitionAllowedFor2g6gDbs == null) {
            SoftApConfiguration.Builder configBuilder = new SoftApConfiguration.Builder();
            configBuilder.setWifiSsid(WifiSsid.fromBytes("Test SSID".getBytes()));
            configBuilder.setBands(new int[]{BAND_2GHZ, BAND_6GHZ});
            configBuilder.setPassphrase("Test passphrase", SECURITY_TYPE_WPA3_SAE_TRANSITION);
            mIsWpa3TransitionAllowedFor2g6gDbs =
                    mWifiManager.validateSoftApConfiguration(configBuilder.build());
        }
        return mIsWpa3TransitionAllowedFor2g6gDbs;
    }

    /**
     * Gets is6gAvailable LiveData
     */
    public LiveData<Boolean> get6gAvailable() {
        if (m6gAvailable == null) {
            m6gAvailable = new MutableLiveData<>();
            m6gAvailable.setValue(is6gAvailable());
        }
        return m6gAvailable;
    }

    protected void update6gAvailable() {
        if (m6gAvailable != null) {
            m6gAvailable.setValue(is6gAvailable());
        }
    }

    /**
     * Return whether the Hotspot channel is available or not.
     *
     * @param sapBand      The SapBand#band constants defined in {@code WifiScanner#WIFI_BAND_*}
     *                     1. {@code WifiScanner#WIFI_BAND_5_GHZ_WITH_DFS}
     *                     2. {@code WifiScanner#WIFI_BAND_6_GHZ}
     */
    @VisibleForTesting
    boolean isChannelAvailable(SapBand sapBand) {
        try {
            List<WifiAvailableChannel> channels =
                    mWifiManager.getAllowedChannels(sapBand.band, OP_MODE_SAP);
            log("isChannelAvailable(), band:" + sapBand.band + ", allowedChannels:" + channels);
            sapBand.hasChannels = (channels != null && channels.size() > 0);
            sapBand.isChannelsUnsupported = false;
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Querying SAP channels failed, band:" + sapBand.band);
            sapBand.hasChannels = false;
            sapBand.isChannelsUnsupported = true;
        } catch (UnsupportedOperationException e) {
            // This is expected on some hardware.
            Log.e(TAG, "Querying SAP channels is unsupported, band:" + sapBand.band);
            sapBand.hasChannels = false;
            sapBand.isChannelsUnsupported = true;
        }
        sapBand.isChannelsReady = true;
        log("isChannelAvailable(), " + sapBand);
        return sapBand.isAvailable();
    }

    private boolean isConfigShowSpeed() {
        if (mIsConfigShowSpeed == null) {
            mIsConfigShowSpeed = mAppContext.getResources()
                    .getBoolean(R.bool.config_show_wifi_hotspot_speed);
            log("isConfigShowSpeed():" + mIsConfigShowSpeed);
        }
        return mIsConfigShowSpeed;
    }

    /**
     * Return whether Wi-Fi Hotspot Speed Feature is available or not.
     *
     * @return {@code true} if Wi-Fi Hotspot Speed Feature is available
     */
    public boolean isSpeedFeatureAvailable() {
        if (mIsSpeedFeatureAvailable != null) {
            return mIsSpeedFeatureAvailable;
        }

        // Check config to show Wi-Fi hotspot speed feature
        if (!isConfigShowSpeed()) {
            mIsSpeedFeatureAvailable = false;
            log("isSpeedFeatureAvailable():false, isConfigShowSpeed():false");
            return false;
        }

        // Check if 5 GHz band is not supported
        if (!is5GHzBandSupported()) {
            mIsSpeedFeatureAvailable = false;
            log("isSpeedFeatureAvailable():false, 5 GHz band is not supported on this device");
            return false;
        }

        mIsSpeedFeatureAvailable = true;
        log("isSpeedFeatureAvailable():true");
        return true;
    }

    /**
     * Return whether Wi-Fi Hotspot is restarting or not.
     *
     * @return {@code true} if Wi-Fi Hotspot is restarting
     */
    public boolean isRestarting() {
        return mIsRestarting;
    }

    /**
     * Gets Restarting LiveData
     */
    public LiveData<Boolean> getRestarting() {
        if (mRestarting == null) {
            mRestarting = new MutableLiveData<>();
            mRestarting.setValue(mIsRestarting);
        }
        return mRestarting;
    }

    private void setRestarting(boolean isRestarting) {
        log("setRestarting(), isRestarting:" + isRestarting);
        mIsRestarting = isRestarting;
        if (mRestarting != null) {
            mRestarting.setValue(mIsRestarting);
        }
    }

    /**
     * Restarts the Tethering Service if it's enabled.
     */
    public void restartTetheringIfNeeded() {
        if (mWifiApState != WIFI_AP_STATE_ENABLED) {
            return;
        }
        log("restartTetheringIfNeeded()");
        mAppContext.getMainThreadHandler().postDelayed(() -> {
            setRestarting(true);
            stopTethering();
        }, RESTART_INTERVAL_MS);
    }

    @VisibleForTesting
    void startTethering() {
        if (mStartTetheringCallback == null) {
            mStartTetheringCallback = new StartTetheringCallback();
        }
        log("startTethering()");
        mTetheringManager.startTethering(TETHERING_WIFI, mAppContext.getMainExecutor(),
                mStartTetheringCallback);
    }

    private void stopTethering() {
        log("stopTethering()");
        mTetheringManager.stopTethering(TETHERING_WIFI);
    }

    @VisibleForTesting
    void updateCapabilityChanged() {
        if (mBand5g.isChannelsUnsupported) {
            update5gAvailable();
            log("updateCapabilityChanged(), " + mBand5g);
        }
        if (mBand6g.isChannelsUnsupported) {
            update6gAvailable();
            log("updateCapabilityChanged(), " + mBand6g);
        }
        if (mBand5g.isChannelsUnsupported || mBand6g.isChannelsUnsupported) {
            updateSpeedType();
        }
    }

    @VisibleForTesting
    class SoftApCallback implements WifiManager.SoftApCallback {

        @Override
        public void onStateChanged(int state, int failureReason) {
            Log.d(TAG, "onStateChanged(), state:" + state + ", failureReason:" + failureReason);
            mWifiApState = state;
            if (!mIsRestarting) {
                return;
            }
            if (state == WIFI_AP_STATE_DISABLED) {
                mAppContext.getMainThreadHandler().postDelayed(() -> startTethering(),
                        RESTART_INTERVAL_MS);
                return;
            }
            if (state == WIFI_AP_STATE_ENABLED) {
                refresh();
                setRestarting(false);
            }
        }

        @Override
        public void onCapabilityChanged(@NonNull SoftApCapability softApCapability) {
            log("onCapabilityChanged(), softApCapability:" + softApCapability);
            mBand5g.hasCapability = softApCapability.getSupportedChannelList(BAND_5GHZ).length > 0;
            mBand6g.hasCapability = softApCapability.getSupportedChannelList(BAND_6GHZ).length > 0;
            updateCapabilityChanged();
        }
    }

    @VisibleForTesting
    class StartTetheringCallback implements TetheringManager.StartTetheringCallback {
        @Override
        public void onTetheringStarted() {
            log("onTetheringStarted()");
        }

        @Override
        public void onTetheringFailed(int error) {
            Log.e(TAG, "onTetheringFailed(), error:" + error);
            if (isRestarting()) {
                Log.w(TAG, "Stop tethering due to restart failure!");
                stopTethering();
                refresh();
                setRestarting(false);
            }
        }
    }

    /**
     * Wi-Fi Hotspot SoftAp Band
     */
    @VisibleForTesting
    static class SapBand {
        public int band;
        public boolean isChannelsReady;
        public boolean hasChannels;
        public boolean isChannelsUnsupported;
        public boolean hasCapability;

        SapBand(int band) {
            this.band = band;
        }

        /**
         * Return whether SoftAp band is available or not.
         */
        public boolean isAvailable() {
            return isChannelsUnsupported ? hasCapability : hasChannels;
        }

        @Override
        @NonNull
        public String toString() {
            return "SapBand{"
                    + "band:" + band
                    + ",isChannelsReady:" + isChannelsReady
                    + ",hasChannels:" + hasChannels
                    + ",isChannelsUnsupported:" + isChannelsUnsupported
                    + ",hasCapability:" + hasCapability
                    + '}';
        }
    }

    private void log(String msg) {
        FeatureFactory.getFeatureFactory().getWifiFeatureProvider().verboseLog(TAG, msg);
    }
}
