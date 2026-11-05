/*
 * Copyright (C) 2026 The crDroid Android Project
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

package com.android.settings.display.darkmode

import android.Manifest.permission.WRITE_SECURE_SETTINGS
import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import com.android.settingslib.datastore.AbstractKeyedDataObservable
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.Permissions
import com.android.settingslib.metadata.PreferenceChangeReason

/**
 * Storage for [Settings.Secure.BATTERY_SAVER_DARK_THEME].
 *
 * The setting defaults to enabled when unset, so this store can't simply be backed by
 * SettingsSecureStore, which would report an unset key as off.
 */
@Suppress("UNCHECKED_CAST")
class BatterySaverDarkThemeStorage(private val context: Context) :
    AbstractKeyedDataObservable<String>(), KeyValueStore {

    private val settingsObserver =
        object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                notifyChange(KEY, PreferenceChangeReason.VALUE)
            }
        }

    override fun contains(key: String) = key == KEY

    override fun <T : Any> getValue(key: String, valueType: Class<T>): T? =
        DarkThemeBatterySaver.isForceDarkEnabled(context) as T

    override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {
        val enabled = value as? Boolean ?: true
        Settings.Secure.putInt(context.contentResolver, KEY, if (enabled) 1 else 0)
        // Notify immediately for UI responsiveness; the observer will follow.
        notifyChange(key, PreferenceChangeReason.VALUE)
    }

    override fun onFirstObserverAdded() {
        context.contentResolver.registerContentObserver(
            Settings.Secure.getUriFor(KEY),
            false,
            settingsObserver,
        )
    }

    override fun onLastObserverRemoved() {
        context.contentResolver.unregisterContentObserver(settingsObserver)
    }

    companion object {
        const val KEY = Settings.Secure.BATTERY_SAVER_DARK_THEME

        fun getReadPermissions() = Permissions.EMPTY

        fun getWritePermissions() = Permissions.allOf(WRITE_SECURE_SETTINGS)
    }
}
