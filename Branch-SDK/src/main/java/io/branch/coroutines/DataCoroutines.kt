package io.branch.coroutines

import android.content.Context
import android.os.RemoteException
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.android.installreferrer.api.ReferrerDetails
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import io.branch.referral.PrefHelper
import kotlinx.coroutines.CompletableDeferred
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


