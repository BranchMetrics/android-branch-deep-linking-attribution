package io.branch.coroutines

import android.content.Context
import android.provider.Settings
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import io.branch.referral.BranchLogger
import io.branch.referral.PrefHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun getGoogleAdvertisingInfoObject(context: Context): AdvertisingIdClient.Info? {
    return withContext(Dispatchers.Default) {
        try {
            AdvertisingIdClient.getAdvertisingIdInfo(context)
        }
        catch (exception: Exception) {
            BranchLogger.w("Caught getGoogleAdvertisingInfoObject exception: $exception")
            null
        }
    }
}

suspend fun getHuaweiAdvertisingInfoObject(context: Context):  com.huawei.hms.ads.identifier.AdvertisingIdClient.Info? {
    return withContext(Dispatchers.Default) {
        try {
            com.huawei.hms.ads.identifier.AdvertisingIdClient.getAdvertisingIdInfo(context)
        }
        catch (exception: Exception) {
            BranchLogger.w("Caught getHuaweiAdvertisingInfoObject exception: $exception")
            null
        }
    }
}

suspend fun getAmazonFireAdvertisingInfoObject(context: Context): Pair<Int, String>? {
    return withContext(Dispatchers.Default) {
        try {
            val cr = context.contentResolver
            Pair(
                Settings.Secure.getInt(cr, "limit_ad_tracking"),
                Settings.Secure.getString(cr, "advertising_id")
            )
        }
        catch (exception: Exception) {
            BranchLogger.w("getAmazonFireAdvertisingInfo exception: $exception")
            null
        }
    }
}