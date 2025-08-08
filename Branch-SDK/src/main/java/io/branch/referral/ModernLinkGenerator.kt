package io.branch.referral

import android.content.Context
import io.branch.referral.network.BranchRemoteInterface
import kotlinx.coroutines.*
import org.json.JSONException
import java.net.HttpURLConnection
import java.util.concurrent.ConcurrentHashMap

/**
 * Modern coroutine-based link generator replacing the deprecated AsyncTask pattern.
 * 
 * Key improvements over AsyncTask implementation:
 * - Non-blocking coroutine execution
 * - Structured timeout control
 * - Result-based error handling
 * - Thread-safe caching
 * - Proper exception handling
 */
class ModernLinkGenerator(
    private val context: Context,
    private val branchRemoteInterface: BranchRemoteInterface,
    private val prefHelper: PrefHelper,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    private val defaultTimeoutMs: Long = 10_000L
) {
    
    /**
     * Java-compatible constructor with default parameters
     */
    @JvmOverloads
    constructor(
        context: Context,
        branchRemoteInterface: BranchRemoteInterface,
        prefHelper: PrefHelper
    ) : this(
        context,
        branchRemoteInterface,
        prefHelper,
        CoroutineScope(SupervisorJob() + Dispatchers.IO),
        10_000L
    )
    
    // Thread-safe cache for generated links - prevents duplicate requests
    private val linkCache = ConcurrentHashMap<String, String>()
    
    /**
     * Generate short link asynchronously using coroutines.
     * 
     * @param linkData The link data for creating the URL
     * @param timeoutMs Timeout in milliseconds (default: 10 seconds)
     * @return Result containing the generated URL or failure
     */
    internal suspend fun generateShortLink(
        linkData: BranchLinkData,
        timeoutMs: Long = defaultTimeoutMs
    ): Result<String> = withContext(Dispatchers.IO) {
        
        try {
            // Check cache first to avoid duplicate requests
            val cacheKey = linkData.toString()
            linkCache[cacheKey]?.let { cachedUrl ->
                return@withContext Result.success(cachedUrl)
            }
            
            // Apply timeout to prevent ANR
            withTimeout(timeoutMs) {
                val response = performLinkRequest(linkData)
                processLinkResponse(response, cacheKey)
            }
        } catch (e: TimeoutCancellationException) {
            Result.failure(
                BranchLinkGenerationException.TimeoutException(
                    "Link generation timed out after ${timeoutMs}ms",
                    e
                )
            )
        } catch (e: BranchLinkGenerationException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(
                BranchLinkGenerationException.GeneralException(
                    "Unexpected error during link generation: ${e.message}",
                    e
                )
            )
        }
    }
    
    /**
     * Generate short link synchronously for compatibility with existing API.
     * 
     * @param request The ServerRequestCreateUrl containing link parameters
     * @return Generated URL string or null on failure
     */
    internal fun generateShortLinkSync(request: ServerRequestCreateUrl): String? {
        return try {
            runBlocking {
                val linkData = request.getLinkPost() ?: return@runBlocking null
                val timeout = (prefHelper.timeout + 2000).toLong() // Match original timeout logic
                
                val result = generateShortLink(linkData, timeout)
                result.getOrElse { 
                    // Fallback to long URL if configured
                    if (request.isDefaultToLongUrl) request.longUrl else null
                }
            }
        } catch (e: Exception) {
            BranchLogger.e("Error in synchronous link generation: ${e.message}")
            if (request.isDefaultToLongUrl) request.longUrl else null
        }
    }
    
    /**
     * Generate short link with callback for compatibility with existing async API.
     * 
     * @param request The ServerRequestCreateUrl containing link parameters
     * @param callback Callback to receive the result
     */
    internal fun generateShortLinkAsync(
        request: ServerRequestCreateUrl,
        callback: Branch.BranchLinkCreateListener?
    ) {
        scope.launch {
            try {
                val linkData = request.getLinkPost()
                if (linkData == null) {
                    callback?.onLinkCreate(
                        null,
                        BranchError("Invalid link data", BranchError.ERR_BRANCH_INVALID_REQUEST)
                    )
                    return@launch
                }
                
                val result = generateShortLink(linkData)
                
                // Switch to main thread for callback
                withContext(Dispatchers.Main) {
                    result.fold(
                        onSuccess = { url ->
                            callback?.onLinkCreate(url, null)
                        },
                        onFailure = { exception ->
                            val branchError = convertToBranchError(exception)
                            callback?.onLinkCreate(null, branchError)
                        }
                    )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback?.onLinkCreate(
                        null,
                        BranchError(
                            "Async link generation failed: ${e.message}",
                            BranchError.ERR_OTHER
                        )
                    )
                }
            }
        }
    }
    
    /**
     * Clear the link cache.
     */
    fun clearCache() {
        linkCache.clear()
    }
    
    /**
     * Get cache size for monitoring.
     */
    fun getCacheSize(): Int = linkCache.size
    
    /**
     * Shutdown the generator and cleanup resources.
     */
    fun shutdown() {
        scope.cancel()
        linkCache.clear()
    }
    
    // Internal bridge methods for Java interop
    @JvmName("generateShortLinkSyncFromJava")
    internal fun generateShortLinkSync(
        linkData: BranchLinkData?,
        defaultToLongUrl: Boolean,
        longUrl: String?,
        timeout: Int
    ): String? {
        if (linkData == null) return if (defaultToLongUrl) longUrl else null
        
        return try {
            runBlocking {
                val result = generateShortLink(linkData, (timeout + 2000).toLong())
                result.getOrElse { 
                    if (defaultToLongUrl) longUrl else null
                }
            }
        } catch (e: Exception) {
            BranchLogger.e("Error in synchronous link generation: ${e.message}")
            if (defaultToLongUrl) longUrl else null
        }
    }
    
    @JvmName("generateShortLinkAsyncFromJava")
    internal fun generateShortLinkAsync(
        linkData: BranchLinkData?,
        callback: Branch.BranchLinkCreateListener?
    ) {
        scope.launch {
            try {
                if (linkData == null) {
                    withContext(Dispatchers.Main) {
                        callback?.onLinkCreate(
                            null,
                            BranchError("Invalid link data", BranchError.ERR_BRANCH_INVALID_REQUEST)
                        )
                    }
                    return@launch
                }
                
                val result = generateShortLink(linkData)
                
                withContext(Dispatchers.Main) {
                    result.fold(
                        onSuccess = { url ->
                            callback?.onLinkCreate(url, null)
                        },
                        onFailure = { exception ->
                            val branchError = convertToBranchError(exception)
                            callback?.onLinkCreate(null, branchError)
                        }
                    )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback?.onLinkCreate(
                        null,
                        BranchError(
                            "Async link generation failed: ${e.message}",
                            BranchError.ERR_OTHER
                        )
                    )
                }
            }
        }
    }
    
    // PRIVATE METHODS
    
    /**
     * Perform the actual network request for link creation.
     */
    private suspend fun performLinkRequest(linkData: BranchLinkData): ServerResponse {
        return withContext(Dispatchers.IO) {
            try {
                branchRemoteInterface.make_restful_post(
                    linkData,
                    prefHelper.apiBaseUrl + Defines.RequestPath.GetURL.path,
                    Defines.RequestPath.GetURL.path,
                    prefHelper.branchKey
                )
            } catch (e: Exception) {
                throw BranchLinkGenerationException.NetworkException(
                    "Network request failed: ${e.message}",
                    e
                )
            }
        }
    }
    
    /**
     * Process the server response and extract the URL.
     */
    private fun processLinkResponse(response: ServerResponse, cacheKey: String): Result<String> {
        return try {
            when (response.statusCode) {
                HttpURLConnection.HTTP_OK -> {
                    val url = response.`object`.getString("url")
                    // Cache successful result
                    linkCache[cacheKey] = url
                    Result.success(url)
                }
                HttpURLConnection.HTTP_CONFLICT -> {
                    Result.failure(
                        BranchLinkGenerationException.ServerException(
                            response.statusCode,
                            "Resource conflict - alias may already exist"
                        )
                    )
                }
                in 400..499 -> {
                    Result.failure(
                        BranchLinkGenerationException.InvalidRequestException(
                            "Invalid request parameters (Status: ${response.statusCode})"
                        )
                    )
                }
                in 500..599 -> {
                    Result.failure(
                        BranchLinkGenerationException.ServerException(
                            response.statusCode,
                            "Server error during link generation"
                        )
                    )
                }
                else -> {
                    Result.failure(
                        BranchLinkGenerationException.ServerException(
                            response.statusCode,
                            "Unexpected response status"
                        )
                    )
                }
            }
        } catch (e: JSONException) {
            Result.failure(
                BranchLinkGenerationException.GeneralException(
                    "Failed to parse server response: ${e.message}",
                    e
                )
            )
        }
    }
    
    /**
     * Convert modern exceptions to legacy BranchError format for compatibility.
     */
    private fun convertToBranchError(exception: Throwable): BranchError {
        return when (exception) {
            is BranchLinkGenerationException.TimeoutException -> 
                BranchError("Link generation timeout", BranchError.ERR_BRANCH_REQ_TIMED_OUT)
            
            is BranchLinkGenerationException.InvalidRequestException -> 
                BranchError(exception.message ?: "Invalid request", BranchError.ERR_BRANCH_INVALID_REQUEST)
            
            is BranchLinkGenerationException.ServerException -> {
                val errorCode = when (exception.statusCode) {
                    HttpURLConnection.HTTP_CONFLICT -> BranchError.ERR_BRANCH_RESOURCE_CONFLICT
                    in 500..599 -> BranchError.ERR_BRANCH_UNABLE_TO_REACH_SERVERS
                    else -> BranchError.ERR_BRANCH_INVALID_REQUEST
                }
                BranchError(exception.message ?: "Server error", errorCode)
            }
            
            is BranchLinkGenerationException.NetworkException -> 
                BranchError(exception.message ?: "Network error", BranchError.ERR_BRANCH_NO_CONNECTIVITY)
            
            is BranchLinkGenerationException.NotInitializedException -> 
                BranchError(exception.message ?: "Not initialized", BranchError.ERR_BRANCH_NOT_INSTANTIATED)
            
            else -> 
                BranchError(exception.message ?: "Unknown error", BranchError.ERR_OTHER)
        }
    }
}