package io.branch.referral

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Coroutine counterpart of [BranchShortLinkBuilder.generateShortUrl]. Main-safe. Honors
 * `setDefaultToLongUrl`, returning the long URL rather than throwing when creation fails.
 *
 * @throws BranchException if no URL could be produced.
 */
suspend fun BranchShortLinkBuilder.createLink(): String =
    withContext(Dispatchers.IO) {
        val branch = Branch.getInstance()
            ?: throw BranchException(
                BranchError(
                    "Trouble creating a URL.",
                    BranchError.ERR_BRANCH_NOT_INSTANTIATED
                )
            )

        suspendCancellableCoroutine { continuation ->
            lateinit var request: ServerRequestCreateUrl
            // Guards against a retry resuming an already-resumed continuation.
            val resumed = AtomicBoolean(false)

            val listener = Branch.BranchLinkCreateListener { url, error ->
                if (!resumed.compareAndSet(false, true)) return@BranchLinkCreateListener
                when {
                    url != null -> continuation.resume(url)
                    // Read via a throwaway request: `request.longUrl` would re-enter this listener on throw.
                    request.isDefaultToLongUrl -> continuation.resume(
                        createUrlRequest(Branch.BranchLinkCreateListener { _, _ -> }, true).longUrl
                    )
                    error != null -> continuation.resumeWithException(BranchException(error))
                    else -> continuation.resumeWithException(
                        BranchException(
                            BranchError("Trouble creating a URL.", BranchError.ERR_OTHER)
                        )
                    )
                }
            }

            request = createUrlRequest(listener, true)
            branch.generateShortLinkInternal(request)
        }
    }
