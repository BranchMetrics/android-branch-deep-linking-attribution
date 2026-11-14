@file:JvmName("InstallerAppStore")

package io.branch.referral

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

/**
 * Returns the package name of the app that installed [packageName], or null if it is unknown
 * or can't be read.
 */
@SuppressLint("NewApi")
internal fun getInstallerPackageName(packageManager: PackageManager, packageName: String, sdkInt: Int): String? {
    return try {
        if (sdkInt >= Build.VERSION_CODES.R) {
            packageManager.getInstallSourceInfo(packageName).installingPackageName
        } else {
            @Suppress("DEPRECATION")
            packageManager.getInstallerPackageName(packageName)
        }
    } catch (e: Exception) {
        BranchLogger.w("Caught exception reading installer package name: $e")
        null
    }
}

/**
 * Maps an installer package name to its app_store value, or null if the installer isn't a mapped store.
 */
internal fun installerToAppStore(installer: String?): String? {
    return when (installer) {
        "com.android.vending" -> Defines.Jsonkey.Google_Play_Store.key
        "com.huawei.appmarket" -> Defines.Jsonkey.Huawei_App_Gallery.key
        "com.sec.android.app.samsungapps" -> Defines.Jsonkey.Samsung_Galaxy_Store.key
        "com.xiaomi.mipicks", "com.xiaomi.market" -> Defines.Jsonkey.Xiaomi_Get_Apps.key
        else -> null
    }
}

/**
 * Returns the app_store value to store, given the stored value and the installer's store.
 * A stored Meta value is kept unless the installer is a store other than Google Play.
 */
internal fun resolveAppStore(stored: String?, installerStore: String?): String? {
    if (stored == Defines.Jsonkey.Meta_Install_Referrer.key
        && (installerStore == null || installerStore == Defines.Jsonkey.Google_Play_Store.key)) {
        return Defines.Jsonkey.Meta_Install_Referrer.key
    }
    return installerStore
}

/**
 * Reads the installer of this app and saves the resulting app_store value in [prefHelper].
 */
internal fun updateAppStoreSource(context: Context, prefHelper: PrefHelper) {
    val installer = getInstallerPackageName(context.packageManager, context.packageName, Build.VERSION.SDK_INT)
    val stored = prefHelper.appStoreSource.takeUnless { it == PrefHelper.NO_STRING_VALUE }
    val appStore = resolveAppStore(stored, installerToAppStore(installer))
    prefHelper.setAppStoreSource(appStore ?: PrefHelper.NO_STRING_VALUE)
    BranchLogger.v("Installer package: $installer, app_store: $appStore")
}
