/*
 * Copyright (C) crDroid Android Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.deviceinfo.firmwareversion

import android.content.Context
import android.util.Log
import androidx.preference.Preference
import com.android.settings.R
import com.android.settingslib.DeviceInfoUtils
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.preference.PreferenceBinding
import java.io.BufferedReader
import java.io.FileReader
import java.io.IOException

// LINT.IfChange
class KernelVersionPreference :
    PreferenceMetadata, PreferenceSummaryProvider, PreferenceBinding,
    Preference.OnPreferenceClickListener {

    private var fullKernelVersion = false

    override val key: String
        get() = "kernel_version"

    override val title: Int
        get() = R.string.kernel_version

    override fun getSummary(context: Context): CharSequence? =
        DeviceInfoUtils.getFormattedKernelVersion(context)

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        preference.isSelectable = true
        preference.isCopyingEnabled = true
        preference.onPreferenceClickListener = this
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        if (fullKernelVersion) {
            preference.summary = DeviceInfoUtils.getFormattedKernelVersion(preference.context)
            fullKernelVersion = false
        } else {
            preference.summary = getFullKernelVersion()
            fullKernelVersion = true
        }
        return true
    }

    private fun getFullKernelVersion(): CharSequence =
        try {
            readLine(FILENAME_PROC_VERSION) ?: "Unavailable"
        } catch (e: IOException) {
            Log.e(LOG_TAG, "IO Exception when getting kernel version for Device Info screen", e)
            "Unavailable"
        }

    companion object {
        private const val LOG_TAG = "KernelVersionPreference"
        private const val FILENAME_PROC_VERSION = "/proc/version"

        @Throws(IOException::class)
        private fun readLine(filename: String): String? =
            BufferedReader(FileReader(filename), 256).use { it.readLine() }
    }
}
// LINT.ThenChange(KernelVersionPreferenceController.java)
