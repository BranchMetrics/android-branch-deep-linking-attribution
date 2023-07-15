package io.branch.referral.coroutines

import android.content.Context
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import io.branch.referral.PrefHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun getAdvertisingInfoObject(context: Context): AdvertisingIdClient.Info? {
    return withContext(Dispatchers.Default) {
        try {
            AdvertisingIdClient.getAdvertisingIdInfo(context)
        }
        catch (exception: Exception) {
            PrefHelper.Debug("getAdvertisingIdInfo exception: $exception")
            null
        }
    }
}
