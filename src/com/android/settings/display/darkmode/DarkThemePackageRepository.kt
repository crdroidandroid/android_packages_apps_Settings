/*
 * SPDX-FileCopyrightText: crDroid Android Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.display.darkmode

import android.content.ContentResolver
import android.database.ContentObserver
import android.net.Uri
import android.os.UserHandle
import android.provider.Settings

class DarkThemePackageRepository(private val resolver: ContentResolver) {

    fun getExcludedPackages(): Set<String> {
        val csv = Settings.System.getStringForUser(
            resolver,
            SETTING_KEY,
            UserHandle.myUserId(),
        ) ?: return emptySet()
        return csv.normalizeToSet()
    }

    fun setExcludedPackages(packages: Collection<String>) {
        val value = packages.asSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .toCollection(LinkedHashSet())
            .joinToString(SEPARATOR)
        Settings.System.putStringForUser(
            resolver,
            SETTING_KEY,
            value,
            UserHandle.myUserId(),
        )
    }

    fun setExcluded(packageName: String, excluded: Boolean) {
        val current = getExcludedPackages().toMutableSet()
        val changed = if (excluded) current.add(packageName) else current.remove(packageName)
        if (changed) setExcludedPackages(current)
    }

    fun isExcluded(packageName: String): Boolean =
        getExcludedPackages().contains(packageName)

    fun registerObserver(observer: ContentObserver) =
        resolver.registerContentObserver(SETTING_URI, /* notifyForDescendants = */ false, observer)

    fun unregisterObserver(observer: ContentObserver) =
        resolver.unregisterContentObserver(observer)

    private fun String.normalizeToSet(): Set<String> =
        splitToSequence(SEPARATOR)
            .map(String::trim)
            .filter(String::isNotEmpty)
            .toCollection(LinkedHashSet())

    companion object {
        val SETTING_KEY: String =
            Settings.System.ACCESSIBILITY_FORCE_INVERT_COLOR_OVERRIDE_PACKAGES_TO_DISABLE

        val SETTING_URI: Uri = Settings.System.getUriFor(SETTING_KEY)

        private const val SEPARATOR = ","
    }
}
