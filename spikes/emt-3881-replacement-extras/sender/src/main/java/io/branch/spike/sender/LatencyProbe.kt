package io.branch.spike.sender

import android.util.Log
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

private const val TAG = "EMT3881_SPIKE_LATENCY"

data class LatencyResult(
    val n: Int,
    val wallClockMs: Double,
    val p50Ms: Double,
    val p95Ms: Double,
    val failures: Int,
)

/**
 * Proxy measurement for "how long would it take to mint N per-target short
 * links before the chooser can open". This fires N GET requests in parallel
 * against a configurable HTTP(S) endpoint — it does NOT call the real Branch
 * link-creation API. Point [run]'s endpoint at a stub/echo service with a
 * comparable response shape for a more realistic number; see README.md.
 */
object LatencyProbe {

    suspend fun run(n: Int, endpoint: String): LatencyResult = coroutineScope {
        val start = System.nanoTime()
        val perRequestMs = (1..n).map { index ->
            async(Dispatchers.IO) { requestOnce(index, endpoint) }
        }.awaitAll()
        val wallClockMs = (System.nanoTime() - start) / 1_000_000.0
        val successMs = perRequestMs.filterNotNull().sorted()

        LatencyResult(
            n = n,
            wallClockMs = wallClockMs,
            p50Ms = percentile(successMs, 50.0),
            p95Ms = percentile(successMs, 95.0),
            failures = perRequestMs.count { it == null },
        )
    }

    private fun requestOnce(index: Int, endpoint: String): Double? {
        val reqStart = System.nanoTime()
        return try {
            val connection = URL(endpoint).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.connect()
            connection.responseCode
            connection.disconnect()
            (System.nanoTime() - reqStart) / 1_000_000.0
        } catch (e: Exception) {
            Log.w(TAG, "Request #$index to $endpoint failed: ${e.message}")
            null
        }
    }

    private fun percentile(sorted: List<Double>, p: Double): Double {
        if (sorted.isEmpty()) return Double.NaN
        val rank = (p / 100.0 * (sorted.size - 1)).toInt().coerceIn(0, sorted.size - 1)
        return sorted[rank]
    }
}
