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
package com.android.settings.network;

import static android.net.ConnectivitySettingsManager.PRIVATE_DNS_MODE_OFF;
import static android.net.ConnectivitySettingsManager.PRIVATE_DNS_MODE_OPPORTUNISTIC;
import static android.net.ConnectivitySettingsManager.PRIVATE_DNS_MODE_PROVIDER_HOSTNAME;

import static com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;

import android.app.settings.SettingsEnums;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivitySettingsManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.utils.AnnotationSpan;
import com.android.settingslib.CustomDialogPreferenceCompat;
import com.android.settingslib.HelpUtils;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtilsInternal;

import com.google.android.material.textfield.TextInputLayout;
import com.google.common.net.InternetDomainName;

import java.util.HashMap;
import java.util.Map;

/**
 * Dialog to set the Private DNS
 */
public class PrivateDnsModeDialogPreference extends CustomDialogPreferenceCompat implements
        RadioGroup.OnCheckedChangeListener, TextWatcher {

    public static final String ANNOTATION_URL = "url";

    private static final String TAG = "PrivateDnsModeDialog";
    // DNS_MODE -> RadioButton id
    private static final Map<Integer, Integer> PRIVATE_DNS_MAP;

    // Only used in Settings, update on additions to ConnectivitySettingsUtils
    private static final int PRIVATE_DNS_MODE_CLOUDFLARE = 4;
    private static final int PRIVATE_DNS_MODE_CLOUDFLARE_BLOCK_MALWARE = 5;
    private static final int PRIVATE_DNS_MODE_CLOUDFLARE_BLOCK_MALWARE_AND_ADULT_CONTENT = 6;
    private static final int PRIVATE_DNS_MODE_ADGUARD = 7;
    private static final int PRIVATE_DNS_MODE_OPEN_DNS = 8;
    private static final int PRIVATE_DNS_MODE_CLEANBROWSING = 9;
    private static final int PRIVATE_DNS_MODE_QUAD9 = 10;
    private static final int PRIVATE_DNS_MODE_QUAD9_UNSECURED = 11;
    private static final int PRIVATE_DNS_MODE_QUAD9_ECS = 12;
    private static final int PRIVATE_DNS_MODE_QUAD9_UNSECURED_ECS = 13;

    static {
        PRIVATE_DNS_MAP = new HashMap<>();
        PRIVATE_DNS_MAP.put(PRIVATE_DNS_MODE_OFF, R.id.private_dns_mode_off);
        PRIVATE_DNS_MAP.put(PRIVATE_DNS_MODE_CLOUDFLARE, R.id.private_dns_mode_cloudflare);
        PRIVATE_DNS_MAP.put(PRIVATE_DNS_MODE_CLOUDFLARE_BLOCK_MALWARE, R.id.private_dns_mode_cloudflare_block_malware);
        PRIVATE_DNS_MAP.put(PRIVATE_DNS_MODE_CLOUDFLARE_BLOCK_MALWARE_AND_ADULT_CONTENT, R.id.private_dns_mode_cloudflare_block_malware_and_adult_content);
        PRIVATE_DNS_MAP.put(PRIVATE_DNS_MODE_ADGUARD, R.id.private_dns_mode_adguard);
        PRIVATE_DNS_MAP.put(PRIVATE_DNS_MODE_OPEN_DNS, R.id.private_dns_mode_open_dns);
        PRIVATE_DNS_MAP.put(PRIVATE_DNS_MODE_CLEANBROWSING, R.id.private_dns_mode_cleanbrowsing);
        PRIVATE_DNS_MAP.put(PRIVATE_DNS_MODE_QUAD9, R.id.private_dns_mode_quad9);
        PRIVATE_DNS_MAP.put(PRIVATE_DNS_MODE_QUAD9_UNSECURED, R.id.private_dns_mode_quad9_unsecured);
        PRIVATE_DNS_MAP.put(PRIVATE_DNS_MODE_QUAD9_ECS, R.id.private_dns_mode_quad9_ecs);
        PRIVATE_DNS_MAP.put(PRIVATE_DNS_MODE_QUAD9_UNSECURED_ECS, R.id.private_dns_mode_quad9_unsecured_ecs);
        PRIVATE_DNS_MAP.put(PRIVATE_DNS_MODE_OPPORTUNISTIC, R.id.private_dns_mode_opportunistic);
        PRIVATE_DNS_MAP.put(PRIVATE_DNS_MODE_PROVIDER_HOSTNAME, R.id.private_dns_mode_provider);
    }

    @VisibleForTesting
    TextInputLayout mHostnameLayout;
    @VisibleForTesting
    EditText mHostnameText;
    @VisibleForTesting
    RadioGroup mRadioGroup;
    @VisibleForTesting
    int mMode;

    public PrivateDnsModeDialogPreference(Context context) {
        super(context);
    }

    public PrivateDnsModeDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PrivateDnsModeDialogPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public PrivateDnsModeDialogPreference(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    private final AnnotationSpan.LinkInfo mUrlLinkInfo = new AnnotationSpan.LinkInfo(
            ANNOTATION_URL, (widget) -> {
        final Context context = widget.getContext();
        final Intent intent = HelpUtils.getHelpIntent(context,
                context.getString(R.string.help_uri_private_dns),
                context.getClass().getName());
        if (intent != null) {
            try {
                widget.startActivityForResult(intent, 0);
            } catch (ActivityNotFoundException e) {
                Log.w(TAG, "Activity was not found for intent, " + intent.toString());
            }
        }
    });

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        if (isDisabledByAdmin()) {
            // If the preference is disabled by the admin, set the inner item as enabled so
            // it could act as a click target. The preference itself will have been disabled
            // by the controller.
            holder.itemView.setEnabled(true);
        }

        setSaveButtonListener();
    }

    @Override
    protected void onBindDialogView(View view) {
        final Context context = getContext();
        mMode = ConnectivitySettingsManager.getPrivateDnsMode(context);
        if (mMode == PRIVATE_DNS_MODE_PROVIDER_HOSTNAME) {
            final String privateDnsHostname =
                    ConnectivitySettingsManager.getPrivateDnsHostname(context);
            final String cloudflareHostname =
                    context.getString(R.string.private_dns_hostname_cloudflare);
            final String cloudflareblockmalwareHostname =
                    context.getString(R.string.private_dns_hostname_cloudflare_block_malware);
            final String cloudflareblockmalwareandadultcontentHostname =
                    context.getString(R.string.private_dns_hostname_cloudflare_block_malware_and_adult_content);
            final String adguardHostname =
                    context.getString(R.string.private_dns_hostname_adguard);
            final String opendnsHostname =
                    context.getString(R.string.private_dns_hostname_open_dns);
            final String cleanbrowsingHostname =
                    context.getString(R.string.private_dns_hostname_cleanbrowsing);
            final String quad9Hostname =
                    context.getString(R.string.private_dns_hostname_quad9);
            final String quad9unsecuredHostname =
                    context.getString(R.string.private_dns_hostname_quad9_unsecured);
            final String quad9ecsHostname =
                    context.getString(R.string.private_dns_hostname_quad9_ecs);
            final String quad9unsecuredecsHostname =
                    context.getString(R.string.private_dns_hostname_quad9_unsecured_ecs);
            if (privateDnsHostname.equals(cloudflareHostname)) {
                mMode = PRIVATE_DNS_MODE_CLOUDFLARE;
            } else if (privateDnsHostname.equals(cloudflareblockmalwareHostname)) {
                mMode = PRIVATE_DNS_MODE_CLOUDFLARE_BLOCK_MALWARE;
            } else if (privateDnsHostname.equals(cloudflareblockmalwareandadultcontentHostname)) {
                mMode = PRIVATE_DNS_MODE_CLOUDFLARE_BLOCK_MALWARE_AND_ADULT_CONTENT;
            } else if (privateDnsHostname.equals(adguardHostname)) {
                mMode = PRIVATE_DNS_MODE_ADGUARD;
            } else if (privateDnsHostname.equals(opendnsHostname)) {
                mMode = PRIVATE_DNS_MODE_OPEN_DNS;
            } else if (privateDnsHostname.equals(cleanbrowsingHostname)) {
                mMode = PRIVATE_DNS_MODE_CLEANBROWSING;
            } else if (privateDnsHostname.equals(quad9Hostname)) {
                mMode = PRIVATE_DNS_MODE_QUAD9;
            } else if (privateDnsHostname.equals(quad9unsecuredHostname)) {
                mMode = PRIVATE_DNS_MODE_QUAD9_UNSECURED;
            } else if (privateDnsHostname.equals(quad9ecsHostname)) {
                mMode = PRIVATE_DNS_MODE_QUAD9_ECS;
            } else if (privateDnsHostname.equals(quad9unsecuredecsHostname)) {
                mMode = PRIVATE_DNS_MODE_QUAD9_UNSECURED_ECS;
            }
        }
        mRadioGroup = view.findViewById(R.id.private_dns_radio_group);
        mRadioGroup.check(PRIVATE_DNS_MAP.getOrDefault(mMode, R.id.private_dns_mode_opportunistic));
        mRadioGroup.setOnCheckedChangeListener(this);

        // Initial radio button text
        final RadioButton offRadioButton = view.findViewById(R.id.private_dns_mode_off);
        offRadioButton.setText(com.android.settingslib.R.string.private_dns_mode_off);
        final RadioButton cloudflareRadioButton =
                view.findViewById(R.id.private_dns_mode_cloudflare);
        cloudflareRadioButton.setText(R.string.private_dns_mode_cloudflare);
        final RadioButton cloudflareblockmalwareRadioButton =
                view.findViewById(R.id.private_dns_mode_cloudflare_block_malware);
        cloudflareblockmalwareRadioButton.setText(R.string.private_dns_mode_cloudflare_block_malware);
        final RadioButton cloudflareblockmalwareandadultcontentRadioButton =
                view.findViewById(R.id.private_dns_mode_cloudflare_block_malware_and_adult_content);
        cloudflareblockmalwareandadultcontentRadioButton.setText(R.string.private_dns_mode_cloudflare_block_malware_and_adult_content);
        final RadioButton adguardRadioButton =
                view.findViewById(R.id.private_dns_mode_adguard);
        adguardRadioButton.setText(R.string.private_dns_mode_adguard);
        final RadioButton opendnsRadioButton =
                view.findViewById(R.id.private_dns_mode_open_dns);
        opendnsRadioButton.setText(R.string.private_dns_mode_open_dns);
        final RadioButton cleanbrowsingRadioButton =
                view.findViewById(R.id.private_dns_mode_cleanbrowsing);
        cleanbrowsingRadioButton.setText(R.string.private_dns_mode_cleanbrowsing);
        final RadioButton quad9RadioButton =
                view.findViewById(R.id.private_dns_mode_quad9);
        quad9RadioButton.setText(R.string.private_dns_mode_quad9);
        final RadioButton quad9unsecuredRadioButton =
                view.findViewById(R.id.private_dns_mode_quad9_unsecured);
        quad9unsecuredRadioButton.setText(R.string.private_dns_mode_quad9_unsecured);
        final RadioButton quad9ecsRadioButton =
                view.findViewById(R.id.private_dns_mode_quad9_ecs);
        quad9ecsRadioButton.setText(R.string.private_dns_mode_quad9_ecs);
        final RadioButton quad9unsecuredecsRadioButton =
                view.findViewById(R.id.private_dns_mode_quad9_unsecured_ecs);
        quad9unsecuredecsRadioButton.setText(R.string.private_dns_mode_quad9_unsecured_ecs);
        final RadioButton opportunisticRadioButton =
                view.findViewById(R.id.private_dns_mode_opportunistic);
        opportunisticRadioButton.setText(
                com.android.settingslib.R.string.private_dns_mode_opportunistic);
        final RadioButton providerRadioButton = view.findViewById(R.id.private_dns_mode_provider);
        providerRadioButton.setText(com.android.settingslib.R.string.private_dns_mode_provider);

        mHostnameLayout = view.findViewById(R.id.private_dns_mode_provider_hostname_layout);
        mHostnameText = view.findViewById(R.id.private_dns_mode_provider_hostname);
        if (mHostnameText != null) {
            mHostnameText.setText(ConnectivitySettingsManager.getPrivateDnsHostname(context));
            mHostnameText.addTextChangedListener(this);
        }

        final TextView helpTextView = view.findViewById(R.id.private_dns_help_info);
        helpTextView.setMovementMethod(LinkMovementMethod.getInstance());
        final Intent helpIntent = HelpUtils.getHelpIntent(context,
                context.getString(R.string.help_uri_private_dns),
                context.getClass().getName());
        final AnnotationSpan.LinkInfo linkInfo = new AnnotationSpan.LinkInfo(context,
                ANNOTATION_URL, helpIntent);
        if (linkInfo.isActionable()) {
            helpTextView.setText(AnnotationSpan.linkify(
                    context.getText(R.string.private_dns_help_message), linkInfo));
        } else {
            helpTextView.setVisibility(View.GONE);
        }

        updateDialogInfo();
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        if (checkedId == R.id.private_dns_mode_off) {
            mMode = PRIVATE_DNS_MODE_OFF;
        } else if (checkedId == R.id.private_dns_mode_cloudflare) {
            mMode = PRIVATE_DNS_MODE_CLOUDFLARE;
        } else if (checkedId == R.id.private_dns_mode_cloudflare_block_malware) {
            mMode = PRIVATE_DNS_MODE_CLOUDFLARE_BLOCK_MALWARE;
        } else if (checkedId == R.id.private_dns_mode_cloudflare_block_malware_and_adult_content) {
            mMode = PRIVATE_DNS_MODE_CLOUDFLARE_BLOCK_MALWARE_AND_ADULT_CONTENT;
        } else if (checkedId == R.id.private_dns_mode_adguard) {
            mMode = PRIVATE_DNS_MODE_ADGUARD;
        } else if (checkedId == R.id.private_dns_mode_open_dns) {
            mMode = PRIVATE_DNS_MODE_OPEN_DNS;
        } else if (checkedId == R.id.private_dns_mode_cleanbrowsing) {
            mMode = PRIVATE_DNS_MODE_CLEANBROWSING;
        } else if (checkedId == R.id.private_dns_mode_quad9) {
            mMode = PRIVATE_DNS_MODE_QUAD9;
        } else if (checkedId == R.id.private_dns_mode_quad9_unsecured) {
            mMode = PRIVATE_DNS_MODE_QUAD9_UNSECURED;
        } else if (checkedId == R.id.private_dns_mode_quad9_ecs) {
            mMode = PRIVATE_DNS_MODE_QUAD9_ECS;
        } else if (checkedId == R.id.private_dns_mode_quad9_unsecured_ecs) {
            mMode = PRIVATE_DNS_MODE_QUAD9_UNSECURED_ECS;
        } else if (checkedId == R.id.private_dns_mode_opportunistic) {
            mMode = PRIVATE_DNS_MODE_OPPORTUNISTIC;
        } else if (checkedId == R.id.private_dns_mode_provider) {
            mMode = PRIVATE_DNS_MODE_PROVIDER_HOSTNAME;
        }
        updateDialogInfo();
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        updateDialogInfo();
    }

    @Override
    public void performClick() {
        EnforcedAdmin enforcedAdmin = getEnforcedAdmin();

        if (enforcedAdmin == null) {
            // If the restriction is not restricted by admin, continue as usual.
            super.performClick();
        } else {
            // Show a dialog explaining to the user why they cannot change the preference.
            RestrictedLockUtils.sendShowAdminSupportDetailsIntent(getContext(), enforcedAdmin);
        }
    }

    private EnforcedAdmin getEnforcedAdmin() {
        return RestrictedLockUtilsInternal.checkIfRestrictionEnforced(
                getContext(), UserManager.DISALLOW_CONFIG_PRIVATE_DNS, UserHandle.myUserId());
    }

    private boolean isDisabledByAdmin() {
        return getEnforcedAdmin() != null;
    }

    private void updateDialogInfo() {
        final boolean modeProvider = PRIVATE_DNS_MODE_PROVIDER_HOSTNAME == mMode;
        if (mHostnameLayout != null) {
            mHostnameLayout.setEnabled(modeProvider);
            mHostnameLayout.setErrorEnabled(false);
        }
    }

    private void setSaveButtonListener() {
        View.OnClickListener onClickListener = v -> doSaveButton();
        DialogInterface.OnShowListener onShowListener = dialog -> {
            if (dialog == null) {
                Log.e(TAG, "The DialogInterface is null!");
                return;
            }
            Button saveButton = ((AlertDialog) dialog).getButton(DialogInterface.BUTTON_POSITIVE);
            if (saveButton == null) {
                Log.e(TAG, "Can't get the save button!");
                return;
            }
            saveButton.setOnClickListener(onClickListener);
        };
        setOnShowListener(onShowListener);
    }

    @VisibleForTesting
    void doSaveButton() {
        Context context = getContext();
        int modeToSet = mMode;
        if (mMode == PRIVATE_DNS_MODE_PROVIDER_HOSTNAME) {
            if (mHostnameLayout == null || mHostnameText == null) {
                Log.e(TAG, "Can't find hostname resources!");
                return;
            }
            if (mHostnameText.getText().isEmpty()) {
                mHostnameLayout.setError(context.getString(R.string.private_dns_field_require));
                Log.w(TAG, "The hostname is empty!");
                return;
            }
            if (!InternetDomainName.isValid(mHostnameText.getText().toString())) {
                mHostnameLayout.setError(context.getString(R.string.private_dns_hostname_invalid));
                Log.w(TAG, "The hostname is invalid!");
                return;
            }

            ConnectivitySettingsManager.setPrivateDnsHostname(context,
                    mHostnameText.getText().toString());
        } else if (mMode == PRIVATE_DNS_MODE_CLOUDFLARE) {
            final String cloudflareHostname =
                    context.getString(R.string.private_dns_hostname_cloudflare);
            ConnectivitySettingsManager.setPrivateDnsHostname(context, cloudflareHostname);
            modeToSet = PRIVATE_DNS_MODE_PROVIDER_HOSTNAME;
        } else if (mMode == PRIVATE_DNS_MODE_CLOUDFLARE_BLOCK_MALWARE) {
            final String cloudflareblockmalwareHostname =
                    context.getString(R.string.private_dns_hostname_cloudflare_block_malware);
            ConnectivitySettingsManager.setPrivateDnsHostname(context, cloudflareblockmalwareHostname);
            modeToSet = PRIVATE_DNS_MODE_PROVIDER_HOSTNAME;
        } else if (mMode == PRIVATE_DNS_MODE_CLOUDFLARE_BLOCK_MALWARE_AND_ADULT_CONTENT) {
            final String cloudflareblockmalwareandadultcontentHostname =
                    context.getString(R.string.private_dns_hostname_cloudflare_block_malware_and_adult_content);
            ConnectivitySettingsManager.setPrivateDnsHostname(context, cloudflareblockmalwareandadultcontentHostname);
            modeToSet = PRIVATE_DNS_MODE_PROVIDER_HOSTNAME;
        } else if (mMode == PRIVATE_DNS_MODE_ADGUARD) {
            final String adguardHostname =
                    context.getString(R.string.private_dns_hostname_adguard);
            ConnectivitySettingsManager.setPrivateDnsHostname(context, adguardHostname);
            modeToSet = PRIVATE_DNS_MODE_PROVIDER_HOSTNAME;
        } else if (mMode == PRIVATE_DNS_MODE_OPEN_DNS) {
            final String opendnsHostname =
                    context.getString(R.string.private_dns_hostname_open_dns);
            ConnectivitySettingsManager.setPrivateDnsHostname(context, opendnsHostname);
            modeToSet = PRIVATE_DNS_MODE_PROVIDER_HOSTNAME;
        } else if (mMode == PRIVATE_DNS_MODE_CLEANBROWSING) {
            final String cleanbrowsingHostname =
                    context.getString(R.string.private_dns_hostname_cleanbrowsing);
            ConnectivitySettingsManager.setPrivateDnsHostname(context, cleanbrowsingHostname);
            modeToSet = PRIVATE_DNS_MODE_PROVIDER_HOSTNAME;
        } else if (mMode == PRIVATE_DNS_MODE_QUAD9) {
            final String quad9Hostname =
                    context.getString(R.string.private_dns_hostname_quad9);
            ConnectivitySettingsManager.setPrivateDnsHostname(context, quad9Hostname);
            modeToSet = PRIVATE_DNS_MODE_PROVIDER_HOSTNAME;
        } else if (mMode == PRIVATE_DNS_MODE_QUAD9_UNSECURED) {
            final String quad9unsecuredHostname =
                    context.getString(R.string.private_dns_hostname_quad9_unsecured);
            ConnectivitySettingsManager.setPrivateDnsHostname(context, quad9unsecuredHostname);
            modeToSet = PRIVATE_DNS_MODE_PROVIDER_HOSTNAME;
        } else if (mMode == PRIVATE_DNS_MODE_QUAD9_ECS) {
            final String quad9ecsHostname =
                    context.getString(R.string.private_dns_hostname_quad9_ecs);
            ConnectivitySettingsManager.setPrivateDnsHostname(context, quad9ecsHostname);
            modeToSet = PRIVATE_DNS_MODE_PROVIDER_HOSTNAME;
        } else if (mMode == PRIVATE_DNS_MODE_QUAD9_UNSECURED_ECS) {
            final String quad9unsecuredecsHostname =
                    context.getString(R.string.private_dns_hostname_quad9_unsecured_ecs);
            ConnectivitySettingsManager.setPrivateDnsHostname(context, quad9unsecuredecsHostname);
            modeToSet = PRIVATE_DNS_MODE_PROVIDER_HOSTNAME;
        }

        ConnectivitySettingsManager.setPrivateDnsMode(context, modeToSet);

        FeatureFactory.getFeatureFactory().getMetricsFeatureProvider()
                .action(context, SettingsEnums.ACTION_PRIVATE_DNS_MODE, modeToSet);
        getDialog().dismiss();
    }
}
