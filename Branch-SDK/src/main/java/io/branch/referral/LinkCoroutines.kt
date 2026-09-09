package io.branch.referral

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
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

            val listener = Branch.BranchLinkCreateListener { url, error ->
                when {
                    url != null -> continuation.resume(url)
                    request.isDefaultToLongUrl -> continuation.resume(request.longUrl)
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
