package io.branch.referral.coroutines

import android.content.Context
import android.util.Log
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun getAdvertisingInfoObject(context: Context): AdvertisingIdClient.Info? {
    return withContext(Dispatchers.Default) {
        try {
            val info = AdvertisingIdClient.getAdvertisingIdInfo(context)
            Log.d("BranchSDK", "getAdvertisingIdInfo received: $info")
            info
        } catch (exception: Exception) {
            Log.d("BranchSDK", "getAdvertisingIdInfo exception: $exception")
            null
        }
    }
}
