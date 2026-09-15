package io.branch.referral

import android.content.Context
import android.net.Uri
import io.branch.coroutines.RequestDeepLink
import io.branch.referral.util.BranchEvent
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Logs this event and suspends until the server responds. Main-safe. Cancelling detaches
 * this caller but leaves the event queued. Not named `logEvent`: the member would shadow it.
 *
 * @throws BranchException if the event could not be logged.
 */
suspend fun BranchEvent.awaitLogEvent(context: Context): Unit =
    suspendCancellableCoroutine { continuation ->
        // Guards against a second callback resuming an already-resumed continuation.
        val resumed = AtomicBoolean(false)
        logEvent(context, object : BranchEvent.BranchLogEventCallback {
            override fun onSuccess(responseCode: Int) {
                if (resumed.compareAndSet(false, true)) continuation.resume(Unit)
            }

            override fun onFailure(e: Exception) {
                if (resumed.compareAndSet(false, true)) continuation.resumeWithException(e)
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
        // Guards against a retry resuming an already-resumed continuation and killing the queue.
        val resumed = AtomicBoolean(false)
        val request = RequestDeepLink(
            applicationContext,
            uri,
            Branch.BranchReferralInitListener { referringParams, error ->
                if (!resumed.compareAndSet(false, true)) return@BranchReferralInitListener
                when {
                    error != null -> continuation.resumeWithException(BranchException(error))
                    referringParams != null -> continuation.resume(referringParams)
                    else -> continuation.resume(JSONObject())
                }
            },
            false
        )

        // Enqueue first: invokeOnCancellation fires immediately for an already-cancelled
        // continuation, and removing before enqueueing would let the request send anyway.
        requestQueue_.handleNewRequest(request)
        continuation.invokeOnCancellation { requestQueue_.remove(request) }
    }
