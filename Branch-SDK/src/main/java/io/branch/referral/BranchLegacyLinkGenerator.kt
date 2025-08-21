package io.branch.referral

import io.branch.referral.network.BranchRemoteInterface
import org.json.JSONException
import java.net.HttpURLConnection
import android.os.AsyncTask
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Legacy link generator utility for backward compatibility with pre-modern implementations.
 * 
 * This utility class consolidates all legacy AsyncTask-based link generation logic to eliminate
 * code duplication and provide a clean separation between modern coroutine-based approaches
 * and legacy compatibility requirements.
 * 
 * ## Purpose
 * - Provides fallback mechanisms for environments where modern coroutine-based link generation fails
 * - Maintains 100% backward compatibility with original AsyncTask-based implementations
 * - Centralizes legacy code to avoid duplication between Branch.java and ModernLinkGenerator.kt
 * 
 * ## Usage Patterns
 * ```kotlin
 * val legacyGenerator = BranchLegacyLinkGenerator(prefHelper, networkInterface)
 * 
 * // For AsyncTask compatibility (maintains original Branch SDK behavior)
 * val url = legacyGenerator.generateShortLinkSyncLegacy(request, linkCache)
 * 
 * // For direct network calls (when AsyncTask is unavailable)
 * val url = legacyGenerator.generateShortLinkSyncDirect(linkData, defaultToLongUrl, longUrl, cache)
 * ```
 * 
 * ## Thread Safety
 * - All methods are thread-safe and can be called from multiple threads concurrently
 * - Cache operations use thread-safe ConcurrentHashMap implementations
 * - Network calls are synchronous and blocking (as per legacy behavior)
 * 
 * ## Error Handling
 * - Network failures are logged and gracefully handled with appropriate fallbacks
 * - JSON parsing errors return null with error logging
 * - Timeout exceptions are caught and logged with fallback behavior
 * 
 * @param prefHelper PrefHelper instance providing configuration values (timeouts, URLs, keys)
 * @param branchRemoteInterface Network interface for performing REST API calls to Branch servers
 * 
 * @since 5.3.0
 * @author Branch SDK Team
 */
internal class BranchLegacyLinkGenerator(
    private val prefHelper: PrefHelper,
    private val branchRemoteInterface: BranchRemoteInterface
) {
    
    /**
     * Generate short link using legacy AsyncTask pattern for maximum compatibility.
     * 
     * This method provides a direct replacement for the original Branch.generateShortLinkSync
     * implementation, maintaining identical behavior including timeout handling, error processing,
     * and caching mechanisms.
     * 
     * ## Implementation Details
     * - Uses internal LegacyAsyncTask to mirror original GetShortLinkTask behavior
     * - Applies the same timeout calculation: `prefHelper.timeout + 2000ms`
     * - Handles InterruptedException, ExecutionException, and TimeoutException identically
     * - Caches successful results using the same key strategy as original implementation
     * 
     * ## Fallback Behavior
     * - If `request.isDefaultToLongUrl` is true, returns `request.longUrl` on API failure
     * - If API call fails and no default URL configured, returns null
     * - JSON parsing errors are logged and treated as API failures
     * 
     * ## Thread Safety
     * This method is thread-safe and can be called concurrently. The AsyncTask execution
     * is isolated per call, and cache operations use thread-safe ConcurrentHashMap.
     * 
     * @param request The ServerRequestCreateUrl containing all link generation parameters
     * @param linkCache Thread-safe cache to store generated links for future retrieval
     * @return Generated short URL string, long URL fallback, or null on complete failure
     * 
     * @throws IllegalArgumentException if request contains invalid parameters (rare)
     * 
     * @see ServerRequestCreateUrl
     * @see BranchLinkData
     */
    fun generateShortLinkSyncLegacy(
        request: ServerRequestCreateUrl,
        linkCache: ConcurrentHashMap<BranchLinkData, String>
    ): String? {
        var response: ServerResponse? = null
        
        try {
            val timeOut = prefHelper.timeout + 2000 // Time out is set to slightly more than link creation time to prevent any edge case
            response = LegacyAsyncTask(branchRemoteInterface, prefHelper)
                .execute(request)
                .get(timeOut.toLong(), TimeUnit.MILLISECONDS)
        } catch (e: InterruptedException) {
            BranchLogger.d("Legacy link generation interrupted: ${e.message}")
        } catch (e: ExecutionException) {
            BranchLogger.d("Legacy link generation execution failed: ${e.message}")
        } catch (e: TimeoutException) {
            BranchLogger.d("Legacy link generation timed out: ${e.message}")
        }
        
        return processLegacyResponse(response, request, linkCache)
    }
    
    /**
     * Generate short link using direct network call without AsyncTask wrapper.
     * 
     * This method provides an alternative to the AsyncTask-based approach for environments
     * where AsyncTask is deprecated, unavailable, or undesirable. It makes a direct
     * synchronous network call to the Branch API.
     * 
     * ## When To Use
     * - Modern environments where AsyncTask is deprecated (API 30+)
     * - Coroutine fallback scenarios where blocking calls are acceptable
     * - Testing environments where AsyncTask behavior is difficult to mock
     * - Performance-critical paths where AsyncTask overhead should be avoided
     * 
     * ## Implementation Details
     * - Makes direct synchronous call to `branchRemoteInterface.make_restful_post()`
     * - Uses identical API endpoint and parameters as AsyncTask version
     * - Processes JSON response with same parsing logic
     * - Handles HTTP status codes identically to original implementation
     * 
     * ## Performance Characteristics
     * - Blocks calling thread until network operation completes
     * - No AsyncTask creation/execution overhead
     * - Direct exception propagation (caught and logged)
     * - Immediate cache update on successful response
     * 
     * ## Error Handling
     * - Network exceptions are caught, logged, and result in null return
     * - JSON parsing errors are caught and logged with graceful fallback
     * - HTTP error status codes result in fallback to longUrl if configured
     * 
     * @param linkData The BranchLinkData containing link generation parameters
     * @param defaultToLongUrl If true, returns longUrl parameter on API failure
     * @param longUrl Fallback URL to return when API fails and defaultToLongUrl is true
     * @param linkCache Thread-safe String-based cache for generated URLs
     * @return Generated short URL, longUrl fallback, or null on complete failure
     * 
     * @see BranchLinkData
     * @see BranchRemoteInterface.make_restful_post
     */
    fun generateShortLinkSyncDirect(
        linkData: BranchLinkData,
        defaultToLongUrl: Boolean,
        longUrl: String?,
        linkCache: ConcurrentHashMap<String, String>
    ): String? {
        var response: ServerResponse? = null
        
        try {
            // Direct network call similar to original AsyncTask doInBackground
            response = branchRemoteInterface.make_restful_post(
                linkData,
                prefHelper.apiBaseUrl + Defines.RequestPath.GetURL.path,
                Defines.RequestPath.GetURL.path,
                prefHelper.branchKey
            )
        } catch (e: Exception) {
            BranchLogger.d("Legacy direct link generation failed: ${e.message}")
        }
        
        return processDirectResponse(response, linkData, defaultToLongUrl, longUrl, linkCache)
    }
    
    /**
     * Process server response from AsyncTask-based link generation call.
     * 
     * This method handles the response processing logic that was originally embedded
     * within the AsyncTask's onPostExecute method. It maintains identical behavior
     * including JSON parsing, error handling, and cache management.
     * 
     * @param response Server response from the Branch API, may be null on network failure
     * @param request Original request containing fallback configuration
     * @param linkCache Cache for storing successful link generation results
     * @return Processed URL string or null if all fallback options are exhausted
     */
    private fun processLegacyResponse(
        response: ServerResponse?,
        request: ServerRequestCreateUrl,
        linkCache: ConcurrentHashMap<BranchLinkData, String>
    ): String? {
        var url: String? = null
        
        // Set default URL if configured
        if (request.isDefaultToLongUrl) {
            url = request.longUrl
        }
        
        // Process successful response
        if (response != null && response.statusCode == HttpURLConnection.HTTP_OK) {
            try {
                url = response.`object`.getString("url")
                
                // Cache successful result
                if (request.linkPost != null) {
                    linkCache[request.linkPost] = url
                }
            } catch (e: JSONException) {
                BranchLogger.e("Error parsing URL from legacy response: ${e.message}")
            }
        }
        
        return url
    }
    
    /**
     * Process server response from direct network call approach.
     * 
     * Handles response processing for the direct network call method, including
     * JSON parsing, cache management, and fallback URL handling. Uses string-based
     * cache keys to match the internal caching strategy of ModernLinkGenerator.
     * 
     * @param response Server response from Branch API, may be null on network failure  
     * @param linkData Original link data used to generate cache key
     * @param defaultToLongUrl Whether to return longUrl on API failure
     * @param longUrl Fallback URL for when API fails and defaultToLongUrl is true
     * @param linkCache String-based cache for storing generated URLs
     * @return Processed URL string or null if all options are exhausted
     */
    private fun processDirectResponse(
        response: ServerResponse?,
        linkData: BranchLinkData,
        defaultToLongUrl: Boolean,
        longUrl: String?,
        linkCache: ConcurrentHashMap<String, String>
    ): String? {
        var url: String? = null
        
        // Set default URL if configured
        if (defaultToLongUrl) {
            url = longUrl
        }
        
        // Process successful response
        if (response != null && response.statusCode == HttpURLConnection.HTTP_OK) {
            try {
                url = response.`object`.getString("url")
                
                // Cache successful result using linkData toString as key
                linkCache[linkData.toString()] = url
            } catch (e: JSONException) {
                BranchLogger.e("Error parsing URL from direct response: ${e.message}")
            }
        }
        
        return url
    }
    
    /**
     * Legacy AsyncTask implementation mirroring the original Branch.GetShortLinkTask.
     * 
     * This internal AsyncTask provides maximum compatibility with the original Branch SDK
     * implementation. It performs the exact same background network operation as the
     * original GetShortLinkTask, ensuring identical behavior for existing integrations.
     * 
     * ## Background Operation
     * - Executes `make_restful_post` call on background thread
     * - Uses ServerRequest.getPost() to extract POST parameters
     * - Applies Branch API URL construction with proper path and key
     * - Returns ServerResponse object for processing by calling method
     * 
     * ## Thread Safety
     * - Each instance handles one request independently  
     * - No shared state between AsyncTask instances
     * - Thread-safe due to isolated execution model
     * 
     * @suppress("DEPRECATION") AsyncTask is deprecated but required for compatibility
     */
    private class LegacyAsyncTask(
        private val branchRemoteInterface: BranchRemoteInterface,
        private val prefHelper: PrefHelper
    ) : AsyncTask<ServerRequestCreateUrl, Void, ServerResponse>() {
        
        override fun doInBackground(vararg requests: ServerRequestCreateUrl): ServerResponse? {
            return if (requests.isNotEmpty()) {
                branchRemoteInterface.make_restful_post(
                    requests[0].post,
                    prefHelper.apiBaseUrl + Defines.RequestPath.GetURL.path,
                    Defines.RequestPath.GetURL.path,
                    prefHelper.branchKey
                )
            } else {
                null
            }
        }
    }
}