package io.branch.referral.network

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.TrafficStats
import android.os.NetworkOnMainThreadException
import android.util.Base64
import androidx.annotation.NonNull
import com.google.android.gms.common.util.Strings
import io.branch.referral.Branch
import io.branch.referral.BranchError
import io.branch.referral.BranchLogger
import io.branch.referral.Defines
import io.branch.referral.PrefHelper
import kotlinx.coroutines.*
import org.json.JSONException
import org.json.JSONObject
import java.io.*
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import kotlin.math.min
import kotlin.math.pow

/**
 * Modern coroutine-based network layer for Branch SDK.
 * 
 * Replaces blocking Thread.sleep() calls with proper coroutine delay mechanisms,
 * implements exponential backoff, and provides cancellation support.
 * 
 * Key improvements:
 * - Non-blocking retry mechanism using coroutine delay
 * - Exponential backoff with jitter for better network behavior
 * - Proper cancellation support through structured concurrency
 * - Thread pool efficiency by not blocking threads during retries
 * - Configurable timeout and retry policies
 */
class BranchAsyncNetworkLayer(
    @NonNull private val branch: Branch,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {
    
    companion object {
        private const val THREAD_TAG_POST = 102
        private const val RETRY_NUMBER = "retryNumber"
        
        // Exponential backoff configuration
        private const val BASE_DELAY_MS = 1000L // 1 second base delay
        private const val MAX_DELAY_MS = 30000L // 30 seconds max delay
        private const val BACKOFF_MULTIPLIER = 2.0
        private const val JITTER_FACTOR = 0.1 // 10% jitter
    }
    
    private val prefHelper: PrefHelper = PrefHelper.getInstance(branch.applicationContext)
    private val retryLimit: Int = prefHelper.retryCount
    
    // State tracking for debugging
    private var lastResponseCode = -1
    private var lastResponseMessage = ""
    private var lastRequestId = ""
    
    /**
     * Performs a RESTful GET request with coroutine-based retry mechanism.
     */
    suspend fun doRestfulGet(url: String): BranchRemoteInterface.BranchResponse {
        BranchLogger.d("BranchAsyncNetworkLayer: Starting GET request to $url")
        return withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            try {
                val result = executeWithRetry { retryNumber ->
                    performGetRequest(url, retryNumber)
                }
                val duration = System.currentTimeMillis() - startTime
                BranchLogger.d("BranchAsyncNetworkLayer: GET request completed successfully in ${duration}ms with response code ${result.getResponseCode()}")
                result
            } catch (e: Exception) {
                val duration = System.currentTimeMillis() - startTime
                BranchLogger.e("BranchAsyncNetworkLayer: GET request failed after ${duration}ms: ${e.message}")
                throw e
            }
        }
    }
    
    /**
     * Performs a RESTful POST request with coroutine-based retry mechanism.
     */
    suspend fun doRestfulPost(url: String, payload: JSONObject): BranchRemoteInterface.BranchResponse {
        BranchLogger.d("BranchAsyncNetworkLayer: Starting POST request to $url with payload size ${payload.toString().length} chars")
        return withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            try {
                val result = executeWithRetry { retryNumber ->
                    performPostRequest(url, payload, retryNumber)
                }
                val duration = System.currentTimeMillis() - startTime
                BranchLogger.d("BranchAsyncNetworkLayer: POST request completed successfully in ${duration}ms with response code ${result.getResponseCode()}")
                result
            } catch (e: Exception) {
                val duration = System.currentTimeMillis() - startTime
                BranchLogger.e("BranchAsyncNetworkLayer: POST request failed after ${duration}ms: ${e.message}")
                throw e
            }
        }
    }
    
    /**
     * Generic retry mechanism with exponential backoff and proper cancellation support.
     */
    private suspend fun executeWithRetry(
        operation: suspend (retryNumber: Int) -> BranchRemoteInterface.BranchResponse
    ): BranchRemoteInterface.BranchResponse {
        var retryNumber = 0
        BranchLogger.d("BranchAsyncNetworkLayer: Starting request with retry limit $retryLimit")
        
        while (retryNumber <= retryLimit) {
            try {
                BranchLogger.v("BranchAsyncNetworkLayer: Executing request attempt #$retryNumber")
                val response = operation(retryNumber)
                
                // Check if we need to retry based on response code
                if (shouldRetry(response.getResponseCode(), retryNumber)) {
                    val delay = calculateRetryDelay(retryNumber)
                    BranchLogger.w("BranchAsyncNetworkLayer: Response code ${response.getResponseCode()} requires retry #$retryNumber. Using coroutine delay of ${delay}ms (eliminates Thread.sleep!)")
                    
                    // Non-blocking delay with cancellation support - THIS REPLACES Thread.sleep()!
                    delay(delay)
                    retryNumber++
                    continue
                }
                
                BranchLogger.d("BranchAsyncNetworkLayer: Request successful on attempt #$retryNumber with response code ${response.getResponseCode()}")
                return response
                
            } catch (e: CancellationException) {
                BranchLogger.i("BranchAsyncNetworkLayer: Network request cancelled via coroutine cancellation")
                throw e
            } catch (e: Exception) {
                if (shouldRetryOnException(e, retryNumber)) {
                    val delay = calculateRetryDelay(retryNumber)
                    BranchLogger.w("BranchAsyncNetworkLayer: Exception occurred (${e.javaClass.simpleName}), retrying #$retryNumber after ${delay}ms coroutine delay: ${e.message}")
                    
                    // Non-blocking delay with cancellation support - THIS REPLACES Thread.sleep()!
                    delay(delay)
                    retryNumber++
                    continue
                }
                
                BranchLogger.e("BranchAsyncNetworkLayer: Request failed permanently after $retryNumber attempts: ${e.message}")
                // Convert to appropriate BranchRemoteException
                throw convertToBranchRemoteException(e)
            }
        }
        
        // Should not reach here, but provide fallback
        BranchLogger.e("BranchAsyncNetworkLayer: Max retries ($retryLimit) exceeded")
        throw BranchRemoteInterface.BranchRemoteException(
            BranchError.ERR_BRANCH_REQ_TIMED_OUT,
            "Maximum retry attempts exceeded"
        )
    }
    
    /**
     * Calculates retry delay using exponential backoff with jitter.
     * This replaces the old linear retry with Thread.sleep().
     */
    private fun calculateRetryDelay(retryNumber: Int): Long {
        val baseDelay = BASE_DELAY_MS * BACKOFF_MULTIPLIER.pow(retryNumber).toLong()
        val cappedDelay = min(baseDelay, MAX_DELAY_MS)
        
        // Add jitter to prevent thundering herd
        val jitter = (cappedDelay * JITTER_FACTOR * Math.random()).toLong()
        val finalDelay = cappedDelay + jitter
        
        BranchLogger.v("BranchAsyncNetworkLayer: Calculated exponential backoff delay: ${finalDelay}ms for retry #$retryNumber (base: ${baseDelay}ms, jitter: ${jitter}ms)")
        return finalDelay
    }
    
    /**
     * Determines if we should retry based on HTTP response code.
     */
    private fun shouldRetry(responseCode: Int, retryNumber: Int): Boolean {
        return responseCode >= 500 && retryNumber < retryLimit
    }
    
    /**
     * Determines if we should retry based on exception type.
     */
    private fun shouldRetryOnException(exception: Exception, retryNumber: Int): Boolean {
        return when (exception) {
            is SocketTimeoutException,
            is InterruptedIOException,
            is IOException -> retryNumber < retryLimit
            else -> false
        }
    }
    
    /**
     * Converts generic exceptions to BranchRemoteException.
     */
    private fun convertToBranchRemoteException(exception: Exception): BranchRemoteInterface.BranchRemoteException {
        return when (exception) {
            is SocketException -> BranchRemoteInterface.BranchRemoteException(
                BranchError.ERR_BRANCH_NO_CONNECTIVITY, 
                exception.message ?: "Network connectivity error"
            )
            is SocketTimeoutException -> BranchRemoteInterface.BranchRemoteException(
                BranchError.ERR_BRANCH_REQ_TIMED_OUT,
                exception.message ?: "Request timed out"
            )
            is InterruptedIOException -> BranchRemoteInterface.BranchRemoteException(
                BranchError.ERR_BRANCH_TASK_TIMEOUT,
                exception.message ?: "Task timeout"
            )
            is NetworkOnMainThreadException -> BranchRemoteInterface.BranchRemoteException(
                BranchError.ERR_NETWORK_ON_MAIN,
                "Cannot make network request on main thread"
            )
            is IOException -> BranchRemoteInterface.BranchRemoteException(
                BranchError.ERR_BRANCH_NO_CONNECTIVITY,
                exception.message ?: "Network connectivity error"
            )
            else -> BranchRemoteInterface.BranchRemoteException(
                BranchError.ERR_OTHER,
                exception.message ?: "Unknown network error"
            )
        }
    }
    
    /**
     * Performs the actual GET request.
     */
    private suspend fun performGetRequest(url: String, retryNumber: Int): BranchRemoteInterface.BranchResponse {
        return withContext(Dispatchers.IO) {
            var connection: HttpsURLConnection? = null
            
            try {
                val timeout = prefHelper.timeout
                val connectTimeout = prefHelper.connectTimeout
                val appendKey = if (url.contains("?")) "&" else "?"
                val modifiedUrl = "$url$appendKey$RETRY_NUMBER=$retryNumber"
                
                val urlObject = URL(modifiedUrl)
                connection = urlObject.openConnection() as HttpsURLConnection
                connection.connectTimeout = connectTimeout
                connection.readTimeout = timeout
                
                val requestId = connection.getHeaderField(Defines.HeaderKey.RequestId.key)
                val responseCode = connection.responseCode
                
                lastResponseCode = responseCode
                lastResponseMessage = connection.responseMessage ?: ""
                lastRequestId = requestId ?: ""
                
                val result = if (responseCode != HttpsURLConnection.HTTP_OK && connection.errorStream != null) {
                    BranchRemoteInterface.BranchResponse(getResponseString(connection.errorStream), responseCode)
                } else {
                    BranchRemoteInterface.BranchResponse(getResponseString(connection.inputStream), responseCode)
                }
                
                result.requestId = Strings.emptyToNull(requestId)
                result
                
            } catch (e: FileNotFoundException) {
                BranchLogger.e(getNetworkErrorMessage(e, url, retryNumber))
                BranchRemoteInterface.BranchResponse(null, lastResponseCode)
            } finally {
                connection?.disconnect()
                resetStats()
            }
        }
    }
    
    /**
     * Performs the actual POST request.
     */
    private suspend fun performPostRequest(
        url: String, 
        payload: JSONObject, 
        retryNumber: Int
    ): BranchRemoteInterface.BranchResponse {
        return withContext(Dispatchers.IO) {
            var connection: HttpsURLConnection? = null
            
            try {
                // Set thread stats tag for POST if API 26+
                if (android.os.Build.VERSION.SDK_INT >= 26) {
                    TrafficStats.setThreadStatsTag(THREAD_TAG_POST)
                }
                
                val timeout = prefHelper.timeout
                val connectTimeout = prefHelper.connectTimeout
                
                // Add retry number to payload
                try {
                    payload.put(RETRY_NUMBER, retryNumber)
                } catch (e: JSONException) {
                    BranchLogger.e("Failed to add retry number: ${e.message}")
                }
                
                val urlObject = URL(url)
                connection = urlObject.openConnection() as HttpsURLConnection
                connection.connectTimeout = connectTimeout
                connection.readTimeout = timeout
                connection.doInput = true
                connection.doOutput = true
                
                if (url.contains(Defines.Jsonkey.QRCodeTag.key)) {
                    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                    connection.setRequestProperty("Accept", "image/*")
                } else {
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.setRequestProperty("Accept", "application/json")
                }
                connection.requestMethod = "POST"
                
                // Write payload
                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(payload.toString())
                    writer.flush()
                }
                
                val requestId = connection.getHeaderField(Defines.HeaderKey.RequestId.key)
                val responseCode = connection.responseCode
                
                lastRequestId = requestId ?: ""
                lastResponseCode = responseCode
                lastResponseMessage = connection.responseMessage ?: ""
                
                BranchLogger.d("Response: $lastResponseMessage")
                
                val result = if (responseCode != HttpsURLConnection.HTTP_OK && connection.errorStream != null) {
                    BranchLogger.e(
                        "Branch Networking Error: " +
                        "\nURL: $url" +
                        "\nResponse Code: $lastResponseCode" +
                        "\nResponse Message: $lastResponseMessage" +
                        "\nRetry number: $retryNumber" +
                        "\nFinal attempt: true" +
                        "\nrequestId: $lastRequestId"
                    )
                    BranchRemoteInterface.BranchResponse(getResponseString(connection.errorStream), responseCode)
                } else {
                    val responseData = if (url.contains(Defines.Jsonkey.QRCodeTag.key)) {
                        // Handle QR code binary data
                        val inputStream = connection.inputStream
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        val baos = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
                        val bytes = baos.toByteArray()
                        Base64.encodeToString(bytes, Base64.DEFAULT)
                    } else {
                        getResponseString(connection.inputStream)
                    }
                    
                    BranchLogger.v(
                        "Branch Networking Success" +
                        "\nURL: $url" +
                        "\nResponse Code: $lastResponseCode" +
                        "\nResponse Message: $lastResponseMessage" +
                        "\nRetry number: $retryNumber" +
                        "\nrequestId: $lastRequestId"
                    )
                    
                    BranchRemoteInterface.BranchResponse(responseData, responseCode)
                }
                
                result.requestId = requestId
                result
                
            } catch (e: FileNotFoundException) {
                BranchLogger.e(getNetworkErrorMessage(e, url, retryNumber))
                BranchRemoteInterface.BranchResponse(null, lastResponseCode)
            } finally {
                connection?.disconnect()
                resetStats()
            }
        }
    }
    
    /**
     * Reads response string from input stream.
     */
    private fun getResponseString(inputStream: InputStream?): String? {
        if (inputStream == null) return null
        
        return try {
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                reader.readText()
            }
        } catch (e: IOException) {
            BranchLogger.d(e.message ?: "Error reading response")
            null
        }
    }
    
    /**
     * Resets internal state tracking.
     */
    private fun resetStats() {
        lastRequestId = ""
        lastResponseCode = -1
        lastResponseMessage = ""
    }
    
    /**
     * Generates detailed error message for debugging.
     */
    private fun getNetworkErrorMessage(exception: Exception, url: String, retry: Int): String {
        return "Branch Networking Error: " +
                "\nURL: $url" +
                "\nResponse Code: $lastResponseCode" +
                "\nResponse Message: $lastResponseMessage" +
                "\nCaught exception type: ${exception.javaClass.canonicalName}" +
                "\nRetry number: $retry" +
                "\nrequestId: $lastRequestId" +
                "\nFinal attempt: ${retry >= retryLimit}" +
                "\nException Message: ${exception.message}" +
                "\nStacktrace: ${BranchLogger.stackTraceToString(exception)}"
    }
    
    /**
     * Cancels all ongoing network operations.
     */
    fun cancelAll() {
        scope.cancel("Network layer cancelled")
    }
}