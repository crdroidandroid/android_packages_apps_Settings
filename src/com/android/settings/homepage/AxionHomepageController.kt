package com.android.settings.homepage

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.UserHandle
import android.os.UserManager
import android.text.TextUtils
import android.util.Log
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.preference.Preference
import com.android.internal.util.UserIcons
import com.android.settings.R
import com.android.settings.communal.CommunalPreferenceController
import com.android.settings.overlay.FeatureFactory
import com.android.settings.safetycenter.SafetyCenterManagerWrapper
import com.android.settingslib.appfunctions.SettingsEnums

class AxionHomepageController(private val activity: SettingsHomepageActivity) {

    private var userInfoReceiver: BroadcastReceiver? = null

    companion object {
        @JvmStatic
        fun getUseAxHomepage() = true
    }

    fun onCreate() {
        setupEdgeToEdge()
        val composeView = ComposeView(activity)
        activity.setContentView(composeView)

        AxionSettingsInterop.setContent(
            composeView,
            onSearchClick = {
                val intent = FeatureFactory.getFeatureFactory().searchFeatureProvider
                    .buildSearchIntent(activity, SettingsEnums.SETTINGS_HOMEPAGE)
                activity.startActivity(intent)
            },
            onAvatarClick = {
                val intent = Intent(android.provider.Settings.ACTION_USER_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                activity.startActivity(intent)
            },
            onPreferenceClick = { key ->
                if (TextUtils.equals(key, "top_level_wallpaper")) {
                    val intent = Intent()
                    intent.setClassName("com.android.axion.themepicker", "com.android.axion.themepicker.ui.MainActivity")
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    intent.putExtra("is_launch_extra", true)
                    activity.startActivity(intent)
                } else if (TextUtils.equals(key, "axion_hub")) {
                    val intent = Intent()
                    intent.setClassName("com.android.axion.axionparts", "com.android.axion.axionparts.DashboardActivity")
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    activity.startActivity(intent)
                } else if (TextUtils.equals(key, "airplane_mode")) {
                    val intent = Intent(android.provider.Settings.ACTION_AIRPLANE_MODE_SETTINGS)
                    activity.startActivity(intent)
                } else if (TextUtils.equals(key, "wifi_settings")) {
                    val intent = Intent(android.provider.Settings.ACTION_WIFI_SETTINGS)
                    activity.startActivity(intent)
                } else if (TextUtils.equals(key, "bluetooth_settings")) {
                    val intent = Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)
                    activity.startActivity(intent)
                } else if (TextUtils.equals(key, "mobile_network")) {
                    val intent = Intent(android.provider.Settings.ACTION_NETWORK_OPERATOR_SETTINGS)
                    activity.startActivity(intent)
                } else {
                    val mainFragment = activity.mainFragment
                    if (mainFragment != null) {
                        val pref = mainFragment.preferenceScreen.findPreference<Preference>(key)
                        if (pref != null) {
                            mainFragment.onPreferenceTreeClick(pref)
                        }
                    }
                }
            },
            userName = getUserName(),
            avatarBitmap = getAvatarBitmap(),
            isCommunalAvailable = isCommunalAvailable(),
            isSafetyCenterAvailable = isSafetyCenterAvailable(),
            isEmergencyAvailable = isEmergencyAvailable(),
            isSupportAvailable = isSupportAvailable()
        )
    }

    fun onStart() {
        registerUserInfoReceiver()
    }

    fun onResume() {
        AxionSettingsInterop.updateUserInfo(getUserName(), getAvatarBitmap())
    }

    fun onStop() {
        unregisterUserInfoReceiver()
    }

    private fun setupEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)
        ViewCompat.setOnApplyWindowInsetsListener(activity.findViewById(android.R.id.content)) { v, windowInsets ->
            val insets = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            v.setPadding(insets.left, 0, insets.right, insets.bottom)
            WindowInsetsCompat.CONSUMED
        }
    }

    private fun getUserName(): String {
        return activity.getSystemService(UserManager::class.java).userName
    }

    private fun getAvatarBitmap(): Bitmap {
        val userManager = activity.getSystemService(UserManager::class.java)
        var bitmapUserIcon = userManager.getUserIcon(UserHandle.myUserId())
        if (bitmapUserIcon == null) {
            val defaultUserIcon = UserIcons.getDefaultUserIcon(
                activity.resources, UserHandle.myUserId(), false
            )
            bitmapUserIcon = UserIcons.convertToBitmap(defaultUserIcon)
        }
        return bitmapUserIcon
    }

    private fun registerUserInfoReceiver() {
        if (userInfoReceiver != null) {
            return
        }
        userInfoReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                AxionSettingsInterop.updateUserInfo(getUserName(), getAvatarBitmap())
            }
        }
        val filter = IntentFilter(Intent.ACTION_USER_INFO_CHANGED)
        activity.registerReceiver(userInfoReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
    }

    private fun unregisterUserInfoReceiver() {
        if (userInfoReceiver != null) {
            activity.unregisterReceiver(userInfoReceiver)
            userInfoReceiver = null
        }
    }

    private fun isCommunalAvailable(): Boolean {
        return CommunalPreferenceController.isAvailable(activity)
    }

    private fun isSafetyCenterAvailable(): Boolean {
        return SafetyCenterManagerWrapper.get().isEnabled(activity)
    }

    private fun isEmergencyAvailable(): Boolean {
        return activity.resources.getBoolean(R.bool.config_show_emergency_settings)
    }

    private fun isSupportAvailable(): Boolean {
        return FeatureFactory.getFeatureFactory().supportFeatureProvider != null
    }
}
