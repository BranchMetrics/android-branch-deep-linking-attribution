package io.branch.referral

import android.content.Context
import android.net.Uri
import io.branch.coroutines.RequestDeepLink
import io.branch.referral.util.BranchEvent
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Logs this event and suspends until the server responds. Main-safe. Cancelling detaches
 * this caller but leaves the event queued. Not named `logEvent`: the member would shadow it.
 *
 * @throws Exception raw, as [BranchEvent.BranchLogEventCallback.onFailure] supplies it.
 */
suspend fun BranchEvent.awaitLogEvent(context: Context): Unit =
    suspendCancellableCoroutine { continuation ->
        logEvent(context, object : BranchEvent.BranchLogEventCallback {
            override fun onSuccess(responseCode: Int) {
                continuation.resume(Unit)
            }

            override fun onFailure(e: Exception) {
                continuation.resumeWithException(e)
            }
        })
    }

/**
 * Resolves [uri] against `v3/deeplink` and suspends until the referring params arrive.
 * Main-safe. Cancelling de-queues the request if it has not been sent yet.
 *
 * @throws BranchException if the deep link could not be resolved.
 */
suspend fun Branch.requestDeepLinkData(uri: Uri): JSONObject =
    suspendCancellableCoroutine { continuation ->
        val request = RequestDeepLink(
            applicationContext,
            uri,
            Branch.BranchReferralInitListener { referringParams, error ->
                when {
                    error != null -> continuation.resumeWithException(BranchException(error))
                    referringParams != null -> continuation.resume(referringParams)
                    else -> continuation.resume(JSONObject())
                }
            },
            false
        )

        continuation.invokeOnCancellation { requestQueue_.remove(request) }
        requestQueue_.handleNewRequest(request)
    }
