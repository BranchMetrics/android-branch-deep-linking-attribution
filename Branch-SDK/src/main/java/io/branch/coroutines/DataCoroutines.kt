package io.branch.coroutines

import android.content.Context
import android.os.RemoteException
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.android.installreferrer.api.ReferrerDetails
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun getAdvertisingInfoObject(context: Context): AdvertisingIdClient.Info? {
    return withContext(Dispatchers.Default) {
        try {
            AdvertisingIdClient.getAdvertisingIdInfo(context)
        }
        catch (exception: Exception) {
            null
        }
    }
}

suspend fun getGooglePlayStoreReferrerDetails(context: Context): ReferrerDetails? {
    val deferredReferrerDetails = CompletableDeferred<ReferrerDetails?>()
    val client = InstallReferrerClient.newBuilder(context.applicationContext).build()

    client.startConnection(object : InstallReferrerStateListener {
        override fun onInstallReferrerSetupFinished(responseInt: Int) {
            if (responseInt == InstallReferrerClient.InstallReferrerResponse.OK) {
                deferredReferrerDetails.complete(
                    try {
                        client.installReferrer
                    }
                    catch (e: RemoteException) {
                        null
                    }
                )
            }
            else {
                deferredReferrerDetails.complete(null)
            }
            client.endConnection()
        }

        override fun onInstallReferrerServiceDisconnected() {
            if (!deferredReferrerDetails.isCompleted) {
                deferredReferrerDetails.complete(null)
            }
        }
    })
    return deferredReferrerDetails.await()
}

suspend fun getHuaweiAppGalleryReferrerDetails(context: Context): com.huawei.hms.ads.installreferrer.api.ReferrerDetails? {
    val deferredReferrerDetails = CompletableDeferred<com.huawei.hms.ads.installreferrer.api.ReferrerDetails?>()
    val client = com.huawei.hms.ads.installreferrer.api.InstallReferrerClient.newBuilder(context).build()

    client.startConnection(object : com.huawei.hms.ads.installreferrer.api.InstallReferrerStateListener {
        override fun onInstallReferrerSetupFinished(responseInt: Int) {
            if (responseInt == com.huawei.hms.ads.installreferrer.api.InstallReferrerClient.InstallReferrerResponse.OK) {
                deferredReferrerDetails.complete(
                    try {
                        client.installReferrer
                    }
                    catch (e: RemoteException) {
                        null
                    }
                )
            }
            else {
                deferredReferrerDetails.complete(null)
            }
            client.endConnection()
        }

        override fun onInstallReferrerServiceDisconnected() {
            if (!deferredReferrerDetails.isCompleted) {
                deferredReferrerDetails.complete(null)
            }
        }
    })
    return deferredReferrerDetails.await()
}

suspend fun getXiaomiGetAppsReferrerDetails(context: Context): com.miui.referrer.api.GetAppsReferrerDetails? {
    val deferredReferrerDetails = CompletableDeferred<com.miui.referrer.api.GetAppsReferrerDetails?>()
    val client = com.miui.referrer.api.GetAppsReferrerClient.newBuilder(context).build()

    client.startConnection(object : com.miui.referrer.api.GetAppsReferrerStateListener {
        override fun onGetAppsReferrerSetupFinished(state: Int) {
            if (state == com.miui.referrer.annotation.GetAppsReferrerResponse.OK) {
                deferredReferrerDetails.complete(
                    try {
                        client.installReferrer
                    }
                    catch (e: RemoteException) {
                        null
                    }
                )
            }
            else {
                deferredReferrerDetails.complete(null)
            }
            client.endConnection()
        }

        override fun onGetAppsServiceDisconnected() {
            if (!deferredReferrerDetails.isCompleted) {
                deferredReferrerDetails.complete(null)
            }
        }
    })
    return deferredReferrerDetails.await()
}

suspend fun getSamsungGalaxyStoreReferrerDetails(context: Context): com.samsung.android.sdk.sinstallreferrer.api.ReferrerDetails? {
    val deferredReferrerDetails = CompletableDeferred<com.samsung.android.sdk.sinstallreferrer.api.ReferrerDetails?>()
    val client = com.samsung.android.sdk.sinstallreferrer.api.InstallReferrerClient.newBuilder(context).build()

    client.startConnection(object : com.samsung.android.sdk.sinstallreferrer.api.InstallReferrerStateListener {
        override fun onInstallReferrerSetupFinished(p0: Int) {
            if (p0 == com.miui.referrer.annotation.GetAppsReferrerResponse.OK) {
                deferredReferrerDetails.complete(
                    try {
                        client.installReferrer
                    }
                    catch (e: RemoteException) {
                        null
                    }
                )
            }
            else {
                deferredReferrerDetails.complete(null)
            }
            client.endConnection()
        }

        override fun onInstallReferrerServiceDisconnected() {
            if (!deferredReferrerDetails.isCompleted) {
                deferredReferrerDetails.complete(null)
            }
        }
    })
    return deferredReferrerDetails.await()
}

}
