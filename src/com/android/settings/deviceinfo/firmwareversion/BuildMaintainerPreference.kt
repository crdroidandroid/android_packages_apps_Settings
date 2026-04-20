/*
 * Copyright (C) 2026 crDroid Android Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.deviceinfo.firmwareversion

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.content.res.Resources
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemProperties
import android.text.TextUtils
import android.util.Log
import androidx.preference.Preference
import com.android.settings.R
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.preference.PreferenceBinding
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.Executors

class BuildMaintainerPreference :
    PreferenceMetadata, PreferenceSummaryProvider, PreferenceBinding,
    Preference.OnPreferenceClickListener {

    private val executor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "BuildMaintainerFetch").apply { isDaemon = true }
    }
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile private var resolvedMaintainer: String = ""
    @Volatile private var resolvedDonateUrl: String? = null
    @Volatile private var fetchStarted: Boolean = false

    override val key: String
        get() = "build_maintainer"

    override val purpose: Int
        get() = R.string.os_firmware_version_purpose

    override val title: Int
        get() = R.string.build_maintainer_title

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        preference.isCopyingEnabled = true
        preference.onPreferenceClickListener = this

        val context = preference.context

        if (!isOfficiallySigned(context)) {
            resolvedMaintainer = ""
            resolvedDonateUrl = null
            applyUnofficialState(preference)
            return
        }

        val overlayMaintainer = safeGetString(context, R.string.build_maintainer_summary).trim()
        val overlayDonateUrl = safeGetString(context, R.string.build_maintainer_donate_url)

        if (resolvedMaintainer.isEmpty()) {
            resolvedMaintainer = overlayMaintainer
        }
        if (resolvedDonateUrl == null) {
            resolvedDonateUrl = sanitizeUrlOrNull(overlayDonateUrl)
        }

        applyOfficialState(preference)

        if (!fetchStarted) {
            fetchStarted = true
            fetchMaintainerFromOta(preference, overlayMaintainer, overlayDonateUrl)
        }
    }

    override fun getSummary(context: Context): CharSequence? {
        if (!isOfficiallySigned(context)) {
            return unofficialLabel(context)
        }
        val maintainer = resolvedMaintainer
        if (maintainer.isNotEmpty()) return maintainer
        return safeGetString(context, R.string.build_maintainer_summary).trim()
            .ifEmpty { null }
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        if (!isOfficiallySigned(preference.context)) return true
        val url = resolvedDonateUrl ?: return true
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            preference.context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "No activity found for donate URL: $url", e)
        }
        return true
    }

    private fun applyUnofficialState(preference: Preference) {
        preference.isVisible = true
        preference.summary = unofficialLabel(preference.context)
        preference.isSelectable = false
    }

    private fun applyOfficialState(preference: Preference) {
        val maintainer = resolvedMaintainer
        val hasMaintainer = maintainer.isNotEmpty()
        preference.isVisible = hasMaintainer
        if (hasMaintainer) {
            preference.summary = maintainer
        }
        preference.isSelectable = hasMaintainer && resolvedDonateUrl != null
    }

    private fun unofficialLabel(context: Context): String =
        safeGetString(context, R.string.build_maintainer_unofficial)
            .trim()
            .ifEmpty { "Unofficial" }

    private fun isOfficiallySigned(context: Context): Boolean {
        officialSigned?.let { return it }

        val result = try {
            val pi = context.packageManager.getPackageInfo(
                PLATFORM_PACKAGE, PackageManager.GET_SIGNING_CERTIFICATES
            )
            val info = pi.signingInfo
            if (info == null) {
                false
            } else {
                val certs: Array<Signature> = if (info.hasMultipleSigners()) {
                    info.apkContentsSigners
                } else {
                    info.signingCertificateHistory
                } ?: emptyArray()

                val allowed = officialCertDigests()
                certs.any { sha256Hex(it.toByteArray()) in allowed }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Platform signature check failed", e)
            false
        }

        Log.d(TAG, "Official platform signature: $result")
        officialSigned = result
        return result
    }

    private fun officialCertDigests(): Set<String> =
        OFFICIAL_PLATFORM_CERT_SHA256
            .map { it.replace(":", "").trim().lowercase(Locale.ROOT) }
            .filter { it.length == 64 && it.all { c -> c in '0'..'9' || c in 'a'..'f' } }
            .toSet()

    private fun sha256Hex(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { "%02x".format(it) }

    private fun fetchMaintainerFromOta(
        preference: Preference,
        overlayMaintainer: String,
        overlayDonateUrl: String,
    ) {
        executor.execute {
            var info: MaintainerInfo? = null
            var matchedCodename: String? = null
            for (codename in getCodenameCandidates()) {
                info = fetchMaintainerInfoForCodename(codename)
                if (info != null) {
                    matchedCodename = codename
                    break
                }
            }

            val finalInfo = info
            val finalCodename = matchedCodename
            mainHandler.post {
                if (finalInfo != null) {
                    resolvedMaintainer = finalInfo.maintainer.trim()
                    resolvedDonateUrl = sanitizeUrlOrNull(finalInfo.donateUrl)
                    Log.d(TAG, "Using OTA maintainer for $finalCodename: $resolvedMaintainer")
                } else {
                    resolvedMaintainer = overlayMaintainer
                    resolvedDonateUrl = sanitizeUrlOrNull(overlayDonateUrl)
                    Log.d(TAG, "Using overlay maintainer: no eligible OTA entry found")
                }
                applyOfficialState(preference)
            }
        }
    }

    private fun getCodenameCandidates(): Set<String> {
        val result = LinkedHashSet<String>()
        addIfNotEmpty(result, SystemProperties.get("ro.crdroid.device", null))
        addIfNotEmpty(result, SystemProperties.get("ro.product.device", null))
        addIfNotEmpty(result, SystemProperties.get("ro.product.vendor.device", null))
        addIfNotEmpty(result, SystemProperties.get("ro.build.product", null))
        return result
    }

    private fun addIfNotEmpty(target: MutableSet<String>, value: String?) {
        val clean = value?.trim().orEmpty()
        if (clean.isNotEmpty()) target.add(clean)
    }

    private fun fetchMaintainerInfoForCodename(codename: String): MaintainerInfo? {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(OTA_JSON_URL.format(codename))
            connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 5000
                readTimeout = 5000
            }
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                Log.d(TAG, "No OTA entry for $codename (HTTP ${connection.responseCode})")
                return null
            }

            val json = readFully(connection.inputStream)
            val root = JSONObject(json)
            val response = root.optJSONArray("response")
            if (response == null || response.length() == 0) {
                Log.d(TAG, "No OTA response array data for $codename")
                return null
            }

            val entry = response.optJSONObject(0)
            if (entry == null) {
                Log.d(TAG, "Invalid OTA response object for $codename")
                return null
            }

            val maintainer = entry.optString("maintainer", "").trim()
            val paypal = entry.optString("paypal", "").trim()

            if (maintainer.isEmpty()) {
                Log.d(TAG, "Skipping $codename: maintainer is empty")
                return null
            }
            MaintainerInfo(maintainer, paypal)
        } catch (e: Exception) {
            Log.d(TAG, "Failed OTA lookup for $codename", e)
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun readFully(input: InputStream): String {
        BufferedInputStream(input).use { bis ->
            ByteArrayOutputStream().use { bos ->
                val buffer = ByteArray(4096)
                while (true) {
                    val read = bis.read(buffer)
                    if (read == -1) break
                    bos.write(buffer, 0, read)
                }
                return bos.toString(StandardCharsets.UTF_8.name())
            }
        }
    }

    private fun safeGetString(context: Context, resId: Int): String = try {
        context.getString(resId).orEmpty()
    } catch (e: Resources.NotFoundException) {
        Log.w(TAG, "Missing string resource: $resId", e)
        ""
    }

    private fun sanitizeUrlOrNull(url: String?): String? {
        val clean = url?.trim().orEmpty()
        return if (isValidHttpUrl(clean)) clean else null
    }

    private fun isValidHttpUrl(url: String): Boolean {
        if (TextUtils.isEmpty(url)) return false
        val scheme = Uri.parse(url).scheme
        return "http".equals(scheme, ignoreCase = true) ||
            "https".equals(scheme, ignoreCase = true)
    }

    private data class MaintainerInfo(val maintainer: String, val donateUrl: String)

    companion object {
        private const val TAG = "BuildMaintainerPreference"
        private const val PLATFORM_PACKAGE = "android"

        private const val OTA_JSON_URL =
            "https://raw.githubusercontent.com/crdroidandroid/" +
                "android_vendor_crDroidOTA/refs/heads/17.0/%s.json"

        private val OFFICIAL_PLATFORM_CERT_SHA256: Set<String> = setOf(
            "73fdfbf41fe6255a3fbc3cfda8b6666f4124f940f499bdd82f1566da68e45732",
        )

        @Volatile private var officialSigned: Boolean? = null
    }
}
