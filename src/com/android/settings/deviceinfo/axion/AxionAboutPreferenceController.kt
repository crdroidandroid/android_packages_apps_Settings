/*
 * Copyright (C) 2025 AxionOS
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
package com.android.settings.deviceinfo.axion

import android.content.Context
import android.content.Intent
import androidx.preference.Preference
import com.android.settings.core.BasePreferenceController
import com.android.settings.deviceinfo.DeviceNamePreferenceController

class AxionAboutPreferenceController(context: Context, key: String) : 
    BasePreferenceController(context, key) {

    override fun getAvailabilityStatus(): Int = AVAILABLE

    override fun getSummary(): CharSequence {
        val deviceNameController = DeviceNamePreferenceController(mContext, "unused_key")
        return deviceNameController.summary
    }

    override fun handlePreferenceTreeClick(preference: Preference): Boolean {
        if (preference.key == preferenceKey) {
            val intent = Intent(mContext, AxionAboutActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            mContext.startActivity(intent)
            return true
        }
        return super.handlePreferenceTreeClick(preference)
    }
}
