package com.android.settings.deviceinfo.firmwareversion

import android.content.Context
import androidx.preference.Preference
import com.android.settings.R
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.preference.PreferenceBinding

class LogoPreference :
    PreferenceMetadata,
    PreferenceAvailabilityProvider,
    PreferenceBinding {

    override val key: String
        get() = "crdroid_logo"

    // No title for this item
    override val title: Int
        get() = 0

    override fun isAvailable(context: Context) = true

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        preference.layoutResource = R.layout.crdroid_logo
        preference.isSelectable = false
    }
}
