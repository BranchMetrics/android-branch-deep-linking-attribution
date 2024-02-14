package io.branch.coroutines

import android.content.Context
import android.os.RemoteException
import android.util.Log
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import io.branch.data.InstallReferrerResult
import io.branch.referral.AppStoreReferrer
import io.branch.referral.BranchLogger
import io.branch.referral.Defines.Jsonkey
import io.branch.referral.PrefHelper
import io.branch.referral.util.classExists
import io.branch.referral.util.huaweiInstallReferrerClass
import io.branch.referral.util.samsungInstallReferrerClass
import io.branch.referral.util.xiaomiInstallReferrerClass
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext

suspend fun getGooglePlayStoreReferrerDetails(context: Context): InstallReferrerResult? {
    return withContext(Dispatchers.Default) {
        try {
            val deferredReferrerDetails = CompletableDeferred<InstallReferrerResult?>()
            val client = InstallReferrerClient.newBuilder(context.applicationContext).build()

            client.startConnection(object : InstallReferrerStateListener {
                override fun onInstallReferrerSetupFinished(responseInt: Int) {
                    BranchLogger.d("getGooglePlayStoreReferrerDetails onInstallReferrerSetupFinished response code: $responseInt")

                    if (responseInt == InstallReferrerClient.InstallReferrerResponse.OK) {
                        deferredReferrerDetails.complete(
                            try {
                                val result = client.installReferrer
                                InstallReferrerResult(Jsonkey.Google_Play_Store.key, result.installBeginTimestampSeconds, result.installReferrer, result.referrerClickTimestampSeconds)
                            }
                            catch (e: Exception) {
                                BranchLogger.w("Caught getGooglePlayStoreReferrerDetails installReferrer exception: $e")
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

            deferredReferrerDetails.await()
        }
        catch (exception: Exception) {
            BranchLogger.w("Caught getGooglePlayStoreReferrerDetails exception: $exception")
            null
        }
    }
}

suspend fun getHuaweiAppGalleryReferrerDetails(context: Context): InstallReferrerResult? {
    return withContext(Dispatchers.Default) {
        if(classExists(huaweiInstallReferrerClass)) {
            try {
                val deferredReferrerDetails =
                    CompletableDeferred<InstallReferrerResult?>()
                val client =
                    com.huawei.hms.ads.installreferrer.api.InstallReferrerClient.newBuilder(context)
                        .build()

                client.startConnection(object :
                    com.huawei.hms.ads.installreferrer.api.InstallReferrerStateListener {
                    override fun onInstallReferrerSetupFinished(responseInt: Int) {
                        BranchLogger.d("getHuaweiAppGalleryReferrerDetails onInstallReferrerSetupFinished response code: $responseInt")

                        if (responseInt == com.huawei.hms.ads.installreferrer.api.InstallReferrerClient.InstallReferrerResponse.OK) {
                            deferredReferrerDetails.complete(
                                try {
                                    val result = client.installReferrer
                                    InstallReferrerResult(
                                        Jsonkey.Huawei_App_Gallery.key,
                                        result.installBeginTimestampSeconds,
                                        result.installReferrer,
                                        result.referrerClickTimestampSeconds
                                    )
                                } catch (e: Exception) {
                                    BranchLogger.d("getHuaweiAppGalleryReferrerDetails exception: $e")
                                    null
                                }
                            )
                        } else {
                            BranchLogger.d("getHuaweiAppGalleryReferrerDetails response code: $responseInt")
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

                deferredReferrerDetails.await()
            } catch (exception: Exception) {
                BranchLogger.d("getHuaweiAppGalleryReferrerDetails exception: $exception")
                null
            }
        }
        else{
            null
        }
    }
}

suspend fun getSamsungGalaxyStoreReferrerDetails(context: Context): InstallReferrerResult? {
    return withContext(Dispatchers.Default) {
        if(classExists(samsungInstallReferrerClass)) {
            try {
                val deferredReferrerDetails =
                    CompletableDeferred<InstallReferrerResult?>()
                val client =
                    com.samsung.android.sdk.sinstallreferrer.api.InstallReferrerClient.newBuilder(
                        context
                    ).build()

                client.startConnection(object :
                    com.samsung.android.sdk.sinstallreferrer.api.InstallReferrerStateListener {
                    override fun onInstallReferrerSetupFinished(p0: Int) {
                        BranchLogger.d("getSamsungGalaxyStoreReferrerDetails onInstallReferrerSetupFinished response code: $p0")

                        if (p0 == com.samsung.android.sdk.sinstallreferrer.api.InstallReferrerClient.InstallReferrerResponse.OK) {
                            deferredReferrerDetails.complete(
                                try {
                                    val result = client.installReferrer
                                    InstallReferrerResult(
                                        Jsonkey.Samsung_Galaxy_Store.key,
                                        result.installBeginTimestampSeconds,
                                        result.installReferrer,
                                        result.referrerClickTimestampSeconds
                                    )
                                } catch (e: RemoteException) {
                                    BranchLogger.d("getSamsungGalaxyStoreReferrerDetails exception: $e")
                                    null
                                }
                            )
                        } else {
                            BranchLogger.d("getSamsungGalaxyStoreReferrerDetails response code: $p0")
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

                deferredReferrerDetails.await()
            } catch (exception: Exception) {
                BranchLogger.d("getSamsungGalaxyStoreReferrerDetails exception: $exception")
                null
            }
        }
        else {
            null
        }
    }
}

suspend fun getXiaomiGetAppsReferrerDetails(context: Context): InstallReferrerResult? {
    return withContext(Dispatchers.Default) {
        if(classExists(xiaomiInstallReferrerClass)) {
            try {
                val deferredReferrerDetails = CompletableDeferred<InstallReferrerResult?>()
                val client = com.miui.referrer.api.GetAppsReferrerClient.newBuilder(context).build()

                client.startConnection(object : com.miui.referrer.api.GetAppsReferrerStateListener {
                    override fun onGetAppsReferrerSetupFinished(state: Int) {
                        BranchLogger.d("getXiaomiGetAppsReferrerDetails onInstallReferrerSetupFinished response code: $state")

                        if (state == com.miui.referrer.annotation.GetAppsReferrerResponse.OK) {
                            deferredReferrerDetails.complete(
                                try {
                                    val result = client.installReferrer
                                    InstallReferrerResult(
                                        Jsonkey.Xiaomi_Get_Apps.key,
                                        result.installBeginTimestampSeconds,
                                        result.installReferrer,
                                        result.referrerClickTimestampSeconds
                                    )
                                } catch (e: RemoteException) {
                                    BranchLogger.d("getXiaomiGetAppsReferrerDetails exception: $e")
                                    null
                                }
                            )
                        } else {
                            BranchLogger.d("getXiaomiGetAppsReferrerDetails response code: $state")
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
                deferredReferrerDetails.await()
            } catch (exception: Exception) {
                BranchLogger.d("getXiaomiGetAppsReferrerDetails exception: $exception")
                null
            }
        }
        else {
            null
        }
    }
}

/**
 * Invokes the source install referrer's coroutines in parallel.
 * Await all and then do list operations
 */
suspend fun fetchLatestInstallReferrer(context: Context): InstallReferrerResult? {
    return supervisorScope {
        val googleReferrer = async { getGooglePlayStoreReferrerDetails(context) }
        val huaweiReferrer = async { getHuaweiAppGalleryReferrerDetails(context) }
        val samsungReferrer = async { getSamsungGalaxyStoreReferrerDetails(context) }
        val xiaomiReferrer = async { getXiaomiGetAppsReferrerDetails(context) }

        val allReferrers: List<InstallReferrerResult?> = listOf(googleReferrer.await(), huaweiReferrer.await(), samsungReferrer.await(), xiaomiReferrer.await())
        val latestReferrer = getLatestValidReferrerStore(allReferrers)

        latestReferrer
    }
}

/**
 * Given a list of InstallReferrerResults, select the one with the latest install timestamp
 * Note that the Play Store, an organic install, will still report a raw referrer string
 * with install and click timestamp of 0. All other stores return null string.
 */
fun getLatestValidReferrerStore(allReferrers: List<InstallReferrerResult?>): InstallReferrerResult? {
    val result = allReferrers.filterNotNull().maxByOrNull {
        it.latestInstallTimestamp
    }

    return result
}