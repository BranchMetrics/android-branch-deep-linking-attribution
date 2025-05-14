package io.branch.referral.util

import io.branch.referral.BranchLogger

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