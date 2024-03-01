package io.branch.coroutines

import android.content.Context
import android.net.Uri
import android.os.RemoteException
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import io.branch.data.InstallReferrerResult
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
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.net.URLDecoder

private const val installReferrer = "install_referrer"
private const val isCt = "is_ct"
private const val actualTimestamp = "actual_timestamp"

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
                                BranchLogger.d("getGooglePlayStoreReferrerDetails installReferrer exception: $e")
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
            BranchLogger.d("getGooglePlayStoreReferrerDetails exception: $exception")
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

suspend fun getMetaInstallReferrerDetails(context: Context): InstallReferrerResult? = withContext(Dispatchers.Default) {
    try {
        val fbAppID = PrefHelper.fbAppId_

        if (fbAppID.isNullOrEmpty()) {
            BranchLogger.d("No Facebook App ID provided. Can't check for Meta Install Referrer")
            null
        } else {
            queryMetaInstallReferrer(context, fbAppID)
        }
    } catch (exception: Exception) {
        BranchLogger.e("Exception in getMetaInstallReferrerDetails: $exception")
        null
    }
}
private fun queryMetaInstallReferrer(context: Context, fbAppId: String): InstallReferrerResult? {
    val facebookProvider = "content://com.facebook.katana.provider.InstallReferrerProvider/$fbAppId"
    val instagramProvider = "content://com.instagram.contentprovider.InstallReferrerProvider/$fbAppId"

    val facebookResult = queryProvider(context, facebookProvider)
    val instagramResult = queryProvider(context, instagramProvider)

    // Check both Facebook and Instagram for install referrers and return the latest one
    val result: InstallReferrerResult?

    if (facebookResult != null && instagramResult != null) {
        if (facebookResult.latestClickTimestamp > instagramResult.latestClickTimestamp) {
            result = facebookResult
        } else {
            result = instagramResult
        }
    } else {
         result = facebookResult ?: instagramResult
    }

    return result
}

private fun queryProvider(context: Context, provider: String): InstallReferrerResult? {
    val projection = arrayOf(installReferrer, isCt, actualTimestamp)

    context.contentResolver.query(Uri.parse(provider), projection, null, null, null)?.use { cursor ->

        if (!cursor.moveToFirst()) {
            BranchLogger.d("getMetaInstallReferrerDetails - cursor is empty or null for provider $provider")
            return null
        }

        val timestampIndex = cursor.getColumnIndex(actualTimestamp)
        val clickThroughIndex = cursor.getColumnIndex(isCt)
        val referrerIndex = cursor.getColumnIndex(installReferrer)

        if (timestampIndex == -1 || clickThroughIndex == -1 || referrerIndex == -1) {
            BranchLogger.w("getMetaInstallReferrerDetails - Required column not found in cursor for provider $provider")
            return null
        }

        val actualTimestamp = cursor.getLong(timestampIndex)
        val isClickThrough = cursor.getInt(clickThroughIndex) == 1
        val installReferrerString = cursor.getString(referrerIndex)

        val utmContentValue = try {
            URLDecoder.decode(installReferrerString, "UTF-8").substringAfter("utm_content=", "")
        } catch (e: IllegalArgumentException) {
            BranchLogger.w("getMetaInstallReferrerDetails - Error decoding URL: $e")
            return null
        }

        if (utmContentValue.isEmpty()) {
            BranchLogger.w("getMetaInstallReferrerDetails - utm_content is empty for provider $provider")
            return null
        }

        BranchLogger.i("getMetaInstallReferrerDetails - Got Meta Install Referrer from provider $provider: $installReferrerString")

        try {
            val json = JSONObject(utmContentValue)
            val latestInstallTimestamp = json.getLong("t")

            return InstallReferrerResult(
                Jsonkey.Meta_Install_Referrer.key,
                latestInstallTimestamp,
                installReferrerString,
                actualTimestamp,
                isClickThrough
            )
        } catch (e: JSONException) {
            BranchLogger.w("getMetaInstallReferrerDetails - JSONException in queryProvider: $e")
            return null
        }
    }

    return null
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
        val metaReferrer = async { getMetaInstallReferrerDetails(context) }

        val allReferrers: List<InstallReferrerResult?> = listOf(googleReferrer.await(), huaweiReferrer.await(), samsungReferrer.await(), xiaomiReferrer.await(), metaReferrer.await())
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

    if (allReferrers.filterNotNull().any { it.appStore == Jsonkey.Meta_Install_Referrer.key }) {
        val latestReferrer = handleMetaInstallReferrer(allReferrers, result!!)
        if (latestReferrer?.appStore == Jsonkey.Meta_Install_Referrer.key) {
            latestReferrer?.appStore = Jsonkey.Google_Play_Store.key
        }
        return latestReferrer
    }

    return result
}

//Handle the deduplication and click vs view logic for Meta install referrer
private fun handleMetaInstallReferrer(allReferrers: List<InstallReferrerResult?>, latestReferrer: InstallReferrerResult): InstallReferrerResult? {
    val result: InstallReferrerResult?
    val metaReferrer = allReferrers.filterNotNull().firstOrNull { it.appStore == Jsonkey.Meta_Install_Referrer.key }

    if (metaReferrer!!.isClickThrough) {
        //The Meta Referrer is click through. Return it if it or the matching Play Store referrer is the latest
        if (latestReferrer.appStore == Jsonkey.Google_Play_Store.key) {
            //Deduplicate the Meta and Play Store referrers
            if (latestReferrer.latestClickTimestamp == metaReferrer.latestClickTimestamp) {
                return  metaReferrer
            }
        }

        result =  latestReferrer
    } else {
        //The Meta Referrer is view through. Return it if the Play Store referrer is organic (latestClickTimestamp is 0)
        val googleReferrer = allReferrers.filterNotNull().firstOrNull { it.appStore == Jsonkey.Google_Play_Store.key }
        if (googleReferrer?.latestClickTimestamp == 0L) {
            result =  metaReferrer
        } else {
            val referrersWithoutMeta = allReferrers.filterNotNull().filterNot { it.appStore == Jsonkey.Meta_Install_Referrer.key }
            result = referrersWithoutMeta.maxByOrNull {
                it.latestInstallTimestamp
            }
        }
    }

    return result
}