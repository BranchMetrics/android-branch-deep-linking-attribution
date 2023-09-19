package io.branch.coroutines

import android.content.Context
import android.provider.Settings
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import io.branch.data.AdvertisingInfoObjectResult
import io.branch.referral.BranchLogger
import io.branch.referral.SystemObserver
import io.branch.referral.util.classExists
import io.branch.referral.util.huaweiAdvertisingIdClientClass
import io.branch.referral.util.playStoreAdvertisingIdClientClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun getGoogleAdvertisingInfoObject(context: Context): AdvertisingInfoObjectResult? {
    return withContext(Dispatchers.Default) {
        try {
            val info = AdvertisingIdClient.getAdvertisingIdInfo(context)

            val lat = info.isLimitAdTrackingEnabled
            var aid: String? = null

            // Only record advertising id if limit ad tracking is false
            if (!lat) {
                aid = info.id
            }

            AdvertisingInfoObjectResult(if (lat) 1 else 0, aid)
        }
        catch (exception: Exception) {
            BranchLogger.d("getGoogleAdvertisingInfoObject exception: $exception")
            null
        }
    }
}

suspend fun getHuaweiAdvertisingInfoObject(context: Context):  AdvertisingInfoObjectResult? {
    return withContext(Dispatchers.Default) {
        try {
            val info = com.huawei.hms.ads.identifier.AdvertisingIdClient.getAdvertisingIdInfo(context)

            val lat = info.isLimitAdTrackingEnabled
            var aid: String? = null

            // Only record advertising id if limit ad tracking is false
            if (!lat) {
                aid = info.id
            }

            AdvertisingInfoObjectResult(if (lat) 1 else 0, aid)
        }
        catch (exception: Exception) {
            BranchLogger.d("getHuaweiAdvertisingInfoObject exception: $exception")
            null
        }
    }
}

suspend fun getAmazonFireAdvertisingInfoObject(context: Context): AdvertisingInfoObjectResult? {
    return withContext(Dispatchers.Default) {
        try {
            val cr = context.contentResolver

            val lat = Settings.Secure.getInt(cr, "limit_ad_tracking")
            var aid: String? = null

            // limit ad tracking false is 0, true is 1
            // Only record advertising id if limit ad tracking is false
            if(lat == 0){
                aid = Settings.Secure.getString(cr, "advertising_id")
            }

            AdvertisingInfoObjectResult(lat, aid)
        }
        catch (exception: Exception) {
            BranchLogger.d("getAmazonFireAdvertisingInfo exception: $exception")
            null
        }
    }
}

suspend fun getAdvertisingInfoObject(context: Context): AdvertisingInfoObjectResult? {
    return withContext(Dispatchers.Default) {
        try {
            if (SystemObserver.isFireOSDevice()) {
                return@withContext getAmazonFireAdvertisingInfoObject(context)
            }
            else if (SystemObserver.isHuaweiMobileServicesAvailable(context) && classExists(huaweiAdvertisingIdClientClass)) {
                return@withContext getHuaweiAdvertisingInfoObject(context)
            }
            else if (classExists(playStoreAdvertisingIdClientClass)) {
                return@withContext getGoogleAdvertisingInfoObject(context)
            }
        }
        catch (exception: Exception){
            BranchLogger.d(exception.message)
        }

        null
    }
}

