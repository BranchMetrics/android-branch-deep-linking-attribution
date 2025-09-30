package io.branch.referral.util

import io.branch.referral.BranchLogger
import io.branch.referral.BranchLogger.v
import io.branch.referral.BranchLogger.w

fun classExists(className: String): Boolean {
    return try {
        Class.forName(className)
        true
    } catch (e: ClassNotFoundException) {
        BranchLogger.v("Could not find $className. If expected, import the dependency into your app.")
        false
    }
}

const val playStoreInstallReferrerClass = "com.android.installreferrer.api.InstallReferrerClient"

const val playStoreAdvertisingIdClientClass =
    "com.google.android.gms.ads.identifier.AdvertisingIdClient"

const val huaweiAdvertisingIdClientClass = "com.huawei.hms.ads.identifier.AdvertisingIdClient";

const val huaweiInstallReferrerClass =
    "com.huawei.hms.ads.installreferrer.api.InstallReferrerClient"

const val samsungInstallReferrerClass =
    "com.samsung.android.sdk.sinstallreferrer.api.InstallReferrerClient"

const val xiaomiInstallReferrerClass = "com.miui.referrer.api.GetAppsReferrerClient"

const val billingGooglePlayClass = "com.android.billingclient.api.BillingClient"

const val androidBrowserClass = "androidx.browser.customtabs.CustomTabsIntent"

fun dependencyMajorVersionFinder(dependencyNameString: String): String {
    val majorIndex = dependencyNameString.indexOf(".")
    val majorVersion = dependencyNameString.substring(0, majorIndex)
    return majorVersion
}

fun getBillingLibraryVersion(): String? {
    var result = ""
    try {
        val billingClientClass = Class.forName("com.android.billingclient.BuildConfig")
        val versionNameField = billingClientClass.getField("VERSION_NAME")
        val field = versionNameField.get(null)
        result = (field as String?).toString()
    } catch (e: Exception) {
        w("Google Play Billing client not imported.")
    }

    v("Found Google Play Billing client version $result")
    return result
}