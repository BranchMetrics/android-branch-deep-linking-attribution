package io.branch.referral

import android.app.Activity
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
 * Resolves the link in [activity]'s launch intent like the `Uri` variant, and sends the intent's
 * context with the launch.
 *
 * @param activity The Activity that received the launch intent.
 * @throws BranchException if the deep link could not be resolved.
 */
suspend fun Branch.requestDeepLinkData(activity: Activity): JSONObject =
    requestLaunchDeepLinkData(activity.intent?.data, activity)

/**
 * Resolves [uri] against `v3/deeplink` and suspends until the referring params arrive.
 * Main-safe. Cancelling de-queues the request if it has not been sent yet. Each resolve also
 * sends one open event; while attribution is off, it is sent when the user opts in.
 *
 * @param uri The URI (App Link or Scheme) to resolve, or null to look up a deferred deep link.
 * @throws BranchException if the deep link could not be resolved.
 */
suspend fun Branch.requestDeepLinkData(uri: Uri?): JSONObject = requestLaunchDeepLinkData(uri, null)

private suspend fun Branch.requestLaunchDeepLinkData(uri: Uri?, activity: Activity?): JSONObject =
    suspendCancellableCoroutine { continuation ->
        // Guards against a retry resuming an already-resumed continuation and killing the queue.
        val resumed = AtomicBoolean(false)
        val request = RequestDeepLink(
            applicationContext,
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
        readLaunchLink(uri, activity)
        enqueueLaunchRequest(request)
        continuation.invokeOnCancellation { requestQueue_.remove(request) }
    }
