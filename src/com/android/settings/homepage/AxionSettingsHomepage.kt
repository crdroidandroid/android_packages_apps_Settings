/*
 * Copyright (C) 2025 AxionOS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http:
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.settings.homepage

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.UserHandle
import android.provider.Settings
import android.app.settings.SettingsEnums
import androidx.compose.ui.platform.ComposeView
import com.android.settings.deviceinfo.axion.AxionAboutActivity
import com.android.settings.deviceinfo.DeviceNamePreferenceController
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.*
import androidx.core.content.ContextCompat.startActivity
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import android.graphics.Bitmap
import com.android.axion.compose.theme.AxionTheme
import com.android.settings.R
import com.android.settings.Utils
import com.android.settings.core.SubSettingLauncher
import com.android.settings.overlay.FeatureFactory
import com.android.settingslib.drawable.CircleFramedDrawable
import java.util.function.Consumer

@Composable
fun AxionSettingsHomepage(
    onSearchClick: () -> Unit,
    onAvatarClick: () -> Unit,
    onPreferenceClick: (String) -> Unit,
    userName: String? = null,
    avatarBitmap: Bitmap? = null,
    isCommunalAvailable: Boolean = false,
    isSafetyCenterAvailable: Boolean = false,
    isEmergencyAvailable: Boolean = true,
    isSupportAvailable: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    AxionTheme {
        Scaffold(
            modifier = modifier,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            topBar = {
                AxionSettingsHeader(
                    onSearchClick = onSearchClick
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                
                item {
                    UserCard(
                        userName = userName ?: "Unknown",
                        avatarBitmap = avatarBitmap,
                        onClick = onAvatarClick
                    )
                }
                
                item {
                    SettingsGroupCard {
                        SettingsItemWithDivider(
                            icon = Icons.Outlined.Wifi,
                            title = stringResource(R.string.network_dashboard_title),
                            onClick = { onPreferenceClick("top_level_network") }
                        )
                        SettingsItem(
                            icon = Icons.Outlined.Devices,
                            title = stringResource(R.string.connected_devices_dashboard_title),
                            onClick = { onPreferenceClick("top_level_connected_devices") }
                        )
                    }
                }

                
                item {
                    SettingsGroupCard {
                        SettingsItemWithDivider(
                            icon = Icons.Outlined.Dashboard,
                            title = "Axion Hub",
                            onClick = { onPreferenceClick("axion_hub") }
                        )
                        if (isCommunalAvailable) {
                            SettingsItemWithDivider(
                                icon = Icons.Outlined.Home,
                                title = stringResource(R.string.communal_settings_title),
                                onClick = { onPreferenceClick("top_level_communal") }
                            )
                        }
                        SettingsItemWithDivider(
                            icon = Icons.Outlined.Wallpaper,
                            title = stringResource(R.string.wallpaper_settings_title),
                            onClick = { onPreferenceClick("top_level_wallpaper") }
                        )
                        SettingsItem(
                            icon = Icons.Outlined.LightMode,
                            title = stringResource(R.string.display_settings),
                            onClick = { onPreferenceClick("top_level_display") }
                        )
                    }
                }
                
                
                item {
                    SettingsGroupCard {
                        SettingsItemWithDivider(
                            icon = Icons.Outlined.VolumeUp,
                            title = stringResource(R.string.sound_settings),
                            onClick = { onPreferenceClick("top_level_sound") }
                        )
                        SettingsItemWithDivider(
                            icon = Icons.Outlined.Notifications,
                            title = stringResource(R.string.configure_notification_settings),
                            onClick = { onPreferenceClick("top_level_notifications") }
                        )
                        SettingsItem(
                            icon = Icons.Outlined.DoNotDisturb,
                            title = stringResource(R.string.zen_modes_list_title),
                            onClick = { onPreferenceClick("top_level_priority_modes") }
                        )
                    }
                }

                
                 item {
                    SettingsGroupCard {
                        if (isSafetyCenterAvailable) {
                             SettingsItemWithDivider(
                                icon = Icons.Outlined.GppGood,
                                title = stringResource(R.string.safety_center_title),
                                onClick = { onPreferenceClick("top_level_safety_center") }
                            )
                        } else {
                             SettingsItemWithDivider(
                                icon = Icons.Outlined.Security,
                                title = stringResource(R.string.security_settings_title),
                                onClick = { onPreferenceClick("top_level_security") }
                            )
                             SettingsItemWithDivider(
                                icon = Icons.Outlined.PrivacyTip,
                                title = stringResource(R.string.privacy_dashboard_title),
                                onClick = { onPreferenceClick("top_level_privacy") }
                            )
                        }
                         SettingsItemWithDivider(
                            icon = Icons.Outlined.LocationOn,
                            title = stringResource(R.string.location_settings_title),
                            onClick = { onPreferenceClick("top_level_location") }
                        )
                        if (isEmergencyAvailable) {
                             SettingsItem(
                                icon = Icons.Outlined.Emergency,
                                title = stringResource(R.string.emergency_settings_preference_title),
                                onClick = { onPreferenceClick("top_level_emergency") }
                            )
                        }
                    }
                }

                
                item {
                    SettingsGroupCard {
                         SettingsItemWithDivider(
                            icon = Icons.Outlined.Apps,
                            title = stringResource(R.string.apps_dashboard_title),
                            onClick = { onPreferenceClick("top_level_apps") }
                        )
                         SettingsItemWithDivider(
                            icon = Icons.Outlined.BatteryStd,
                            title = stringResource(R.string.power_usage_summary_title),
                            onClick = { onPreferenceClick("top_level_battery") }
                        )
                        SettingsItemWithDivider(
                            icon = Icons.Outlined.Storage,
                            title = stringResource(R.string.storage_settings),
                            onClick = { onPreferenceClick("top_level_storage") }
                        )
                         SettingsItemWithDivider(
                            icon = Icons.Outlined.Settings,
                            title = stringResource(R.string.header_category_system),
                            onClick = { onPreferenceClick("top_level_system") }
                        )
                        
                        SettingsItem(
                            icon = Icons.Outlined.Info,
                            title = stringResource(R.string.about_settings),
                            onClick = { onPreferenceClick("top_level_about_device") }
                        )
                    }
                }

                
                item {
                    SettingsGroupCard {
                         SettingsItemWithDivider(
                            icon = Icons.Outlined.AccountCircle,
                            title = stringResource(R.string.account_dashboard_title_with_passkeys),
                            onClick = { onPreferenceClick("top_level_accounts") }
                        )
                        if (isSupportAvailable) {
                            SettingsItemWithDivider(
                                icon = Icons.Outlined.Accessibility,
                                title = stringResource(R.string.accessibility_settings),
                                onClick = { onPreferenceClick("top_level_accessibility") }
                            )
                            SettingsItem(
                                icon = Icons.Outlined.HelpOutline,
                                title = stringResource(R.string.page_tab_title_support),
                                onClick = { onPreferenceClick("top_level_support") }
                            )
                        } else {
                            SettingsItem(
                                icon = Icons.Outlined.Accessibility,
                                title = stringResource(R.string.accessibility_settings),
                                onClick = { onPreferenceClick("top_level_accessibility") }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AxionSettingsHeader(
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = statusBarHeight)
            .padding(horizontal = 16.dp)
            .padding(top = 24.dp, bottom = 16.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_label),
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(modifier = Modifier.height(20.dp))
        
        
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clickable(onClick = onSearchClick),
            shape = RoundedCornerShape(25.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.search_settings),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
fun UserCard(
    userName: String,
    avatarBitmap: Bitmap? = null,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceBright
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                 if (avatarBitmap != null) {
                     Image(
                         bitmap = avatarBitmap.asImageBitmap(),
                         contentDescription = "User Account",
                         contentScale = ContentScale.Crop,
                         modifier = Modifier.fillMaxSize()
                     )
                 } else {
                     Icon(
                         painter = painterResource(id = R.drawable.ic_settings_about_device),
                         contentDescription = "User Account",
                         tint = MaterialTheme.colorScheme.onPrimaryContainer,
                         modifier = Modifier.size(28.dp)
                     )
                 }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = userName,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Manage users and profiles",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun SettingsGroupCard(
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceBright
        )
    ) {
        Column(
            content = content
        )
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Text(
            modifier = Modifier.weight(1f),
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SettingsItemWithDivider(
    icon: ImageVector,
    title: String,
    showDivider: Boolean = true,
    onClick: () -> Unit
) {
    Column {
        SettingsItem(
            icon = icon,
            title = title,
            onClick = onClick
        )
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 56.dp, end = 16.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.surfaceContainer
            )
        }
    }
}

object AxionSettingsInterop {
    private var userNameState: MutableState<String?>? = null
    private var avatarBitmapState: MutableState<Bitmap?>? = null

    @JvmStatic
    fun setContent(
        view: ComposeView,
        onSearchClick: Runnable,
        onAvatarClick: Runnable,
        onPreferenceClick: Consumer<String>,
        userName: String? = null,
        avatarBitmap: Bitmap? = null,
        isCommunalAvailable: Boolean = false,
        isSafetyCenterAvailable: Boolean = false,
        isEmergencyAvailable: Boolean = true,
        isSupportAvailable: Boolean = false
    ) {
        view.setContent {
            val userNameMutableState = remember { mutableStateOf(userName) }
            val avatarBitmapMutableState = remember { mutableStateOf(avatarBitmap) }
            userNameState = userNameMutableState
            avatarBitmapState = avatarBitmapMutableState

            AxionSettingsHomepage(
                onSearchClick = { onSearchClick.run() },
                onAvatarClick = { onAvatarClick.run() },
                onPreferenceClick = { onPreferenceClick.accept(it) },
                userName = userNameMutableState.value,
                avatarBitmap = avatarBitmapMutableState.value,
                isCommunalAvailable = isCommunalAvailable,
                isSafetyCenterAvailable = isSafetyCenterAvailable,
                isEmergencyAvailable = isEmergencyAvailable,
                isSupportAvailable = isSupportAvailable
            )
        }
    }

    @JvmStatic
    fun updateUserInfo(userName: String?, avatarBitmap: Bitmap?) {
        userNameState?.value = userName
        avatarBitmapState?.value = avatarBitmap
    }
}
