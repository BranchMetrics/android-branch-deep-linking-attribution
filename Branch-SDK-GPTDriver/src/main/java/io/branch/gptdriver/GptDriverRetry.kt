package io.branch.gptdriver

import android.util.Log
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Retry helper for MobileBoost / GPTDriver calls that are sensitive to
 * transient network flakes on the emulator.
 *
 * The failure modes we retry:
 *   - `UnknownHostException` — emulator DNS briefly can't resolve
 *     `api.mobileboost.io`. Intermittent and self-heals within seconds.
 *   - `SocketTimeoutException` — TCP / TLS handshake stalled mid-request.
 *   - `IOException` whose message contains an HTTP 5xx code — MobileBoost
 *     cloud returned `500`/`502`/`503`/`504`. Retry-friendly on the client
 *     side.
 *
 * We do NOT retry on:
 *   - `AssertionError` thrown by `driver.assertCondition` / `driver.assertBulk`
 *     when the underlying AI evaluation returns `false`. That's a real
 *     negative signal; masking it would defeat the test.
 *   - Any other exception — we want unexpected errors to surface loudly.
 *
 * The retry cadence is exponential backoff: 2s → 4s → 8s by default.
 * Each failed attempt is logged with the attempt number and the exception
 * class so dashboard investigation remains possible when the retries
 * ultimately succeed.
 */
private const val RETRY_TAG = "GptDriverRetry"

private val HTTP_5XX_REGEX = Regex("""code=5\d\d""")

/**
 * Execute [block] with up to [times] attempts. Transient network errors
 * trigger a retry with exponential backoff starting at [initialDelayMs].
 *
 * Not marked `inline` on purpose: the block runs network I/O, so the small
 * call-site overhead of a lambda is negligible and we avoid the
 * `@PublishedApi` ceremony required by `inline` for the private constants.
 */
fun <T> withRetry(
    times: Int = 3,
    initialDelayMs: Long = 2_000,
    block: () -> T
): T {
    var lastError: Throwable? = null
    var delay = initialDelayMs

    for (attempt in 1..times) {
        try {
            return block()
        } catch (e: UnknownHostException) {
            lastError = e
            Log.w(
                RETRY_TAG,
                "UnknownHostException on attempt $attempt/$times: ${e.message}"
            )
        } catch (e: SocketTimeoutException) {
            lastError = e
            Log.w(
                RETRY_TAG,
                "SocketTimeoutException on attempt $attempt/$times: ${e.message}"
            )
        } catch (e: IOException) {
            val message = e.message.orEmpty()
            if (HTTP_5XX_REGEX.containsMatchIn(message)) {
                lastError = e
                Log.w(
                    RETRY_TAG,
                    "HTTP 5xx on attempt $attempt/$times: $message"
                )
            } else {
                throw e
            }
        }

        if (attempt < times) {
            Thread.sleep(delay)
            delay *= 2
        }
    }

    throw lastError
        ?: IllegalStateException("withRetry exhausted without capturing an error")
}
