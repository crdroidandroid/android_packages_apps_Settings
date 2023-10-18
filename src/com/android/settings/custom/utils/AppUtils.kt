package com.android.settings.custom.utils

import android.content.Context
import android.content.pm.PackageInfo
import android.os.UserHandle

class AppUtils {
    public fun getCloneableAppList(context: Context): List<PackageInfo> {
        var packageList: List<PackageInfo> =
            context.getPackageManager().getInstalledPackagesAsUser(/* flags*/ 0, UserHandle.myUserId())

        var filteredList: List<PackageInfo> = packageList.filter {
            val packageName = it.applicationInfo?.packageName
            val isSystemApp = it.applicationInfo?.isSystemApp()
            val isResourceOverlay = it.applicationInfo?.isResourceOverlay()
            
            val cloneableApps = context.resources.getStringArray(com.android.internal.R.array.cloneable_apps)?.toList()
            
            cloneableApps?.contains(packageName) == true || (isSystemApp == false && isResourceOverlay == false)
        }
    
        return filteredList
    }

    public fun getCloneableAppListStr(context: Context): List<String> {
        return getCloneableAppList(context).map {
                x -> x.packageName
            }.toList()
    }
}
