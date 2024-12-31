package ir.ayantech.ayanadmanager.utils

import android.content.Context
import android.os.Build

fun getOsVersion() = Build.VERSION.SDK_INT

fun getAppVersion(context: Context): Long {
    return try {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            packageInfo.versionCode.toLong()
        }
    } catch (e: Exception) {
        -1L
    }
}