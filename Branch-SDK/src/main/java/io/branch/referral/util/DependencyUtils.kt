package io.branch.referral.util

import io.branch.referral.PrefHelper

fun classExists(className: String): Boolean {
    return try {
        Class.forName(className)
        true
    } catch (e: ClassNotFoundException) {
        PrefHelper.Debug("Could not find $className. If expected, import the dependency into your app.")
        false
    }
}

const val playStoreInstallReferrerClass = "com.android.installreferrer.api.InstallReferrerClient"

const val playStoreAdvertisingIdClientClass =
    "com.google.android.gms.ads.identifier.AdvertisingIdClient"

const val huaweiInstallReferrerClass =
    "com.huawei.hms.ads.installreferrer.api.InstallReferrerClient"

const val galaxyStoreInstallReferrerClass =
    "com.sec.android.app.samsungapps.installreferrer.api.InstallReferrerClient"

const val xiaomiInstallReferrerClass = "com.miui.referrer.api.GetAppsReferrerClient"

const val billingGooglePlayClass = "com.android.billingclient.api.BillingClient"
