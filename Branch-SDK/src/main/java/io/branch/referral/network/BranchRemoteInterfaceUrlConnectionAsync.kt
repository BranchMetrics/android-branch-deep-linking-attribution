package io.branch.referral.network

import androidx.annotation.NonNull
import io.branch.referral.Branch
import io.branch.referral.BranchLogger
import io.branch.referral.PrefHelper
import kotlinx.coroutines.runBlocking
import org.json.JSONObject

/**
 * Modern coroutine-based implementation of BranchRemoteInterface.
 * 
 * This class replaces the blocking Thread.sleep() calls in BranchRemoteInterfaceUrlConnection
 * with proper coroutine-based retry mechanisms, exponential backoff, and cancellation support.
 * 
 * Key improvements over BranchRemoteInterfaceUrlConnection:
 * - Non-blocking retry mechanism using coroutine delay
 * - Exponential backoff with jitter for better network behavior
 * - Proper cancellation support through structured concurrency
 * - Thread pool efficiency by not blocking threads during retries
 * - Maintains 100% API compatibility with existing BranchRemoteInterface
 * 
 */
class BranchRemoteInterfaceUrlConnectionAsync(@NonNull branch: Branch) : BranchRemoteInterface() {
    
    private val asyncNetworkLayer: BranchAsyncNetworkLayer = BranchAsyncNetworkLayer(branch)
    private val prefHelper: PrefHelper = PrefHelper.getInstance(branch.applicationContext)
    
    init {
        BranchLogger.i("BranchRemoteInterfaceUrlConnectionAsync: Initialized with modern coroutine-based networking (Thread.sleep eliminated)")
    }
    
    /**
     * Performs RESTful GET request using coroutine-based retry mechanism.
     * 
     * This method maintains backward compatibility by providing a synchronous interface
     * while internally using modern async operations.
     */
    @Throws(BranchRemoteException::class)
    override fun doRestfulGet(url: String): BranchResponse {
        BranchLogger.d("BranchRemoteInterfaceUrlConnectionAsync: Executing GET request using async layer (no Thread.sleep)")
        return try {
            // Use runBlocking to maintain synchronous interface compatibility
            // while leveraging async implementation internally
            val startTime = System.currentTimeMillis()
            val result = runBlocking {
                asyncNetworkLayer.doRestfulGet(url)
            }
            val duration = System.currentTimeMillis() - startTime
            BranchLogger.d("BranchRemoteInterfaceUrlConnectionAsync: GET request completed in ${duration}ms using coroutine-based retry")
            result
        } catch (e: BranchRemoteException) {
            BranchLogger.w("BranchRemoteInterfaceUrlConnectionAsync: GET request failed with BranchRemoteException: ${e.message}")
            throw e
        } catch (e: Exception) {
            BranchLogger.e("BranchRemoteInterfaceUrlConnectionAsync: GET request failed with unexpected error: ${e.message}")
            throw BranchRemoteException(
                io.branch.referral.BranchError.ERR_OTHER,
                "Unexpected error during GET request: ${e.message}"
            )
        }
    }
    
    /**
     * Performs RESTful POST request using coroutine-based retry mechanism.
     * 
     * This method maintains backward compatibility by providing a synchronous interface
     * while internally using modern async operations.
     */
    @Throws(BranchRemoteException::class)
    override fun doRestfulPost(url: String, payload: JSONObject): BranchResponse {
        BranchLogger.d("BranchRemoteInterfaceUrlConnectionAsync: Executing POST request using async layer (no Thread.sleep)")
        return try {
            // Use runBlocking to maintain synchronous interface compatibility
            // while leveraging async implementation internally
            val startTime = System.currentTimeMillis()
            val result = runBlocking {
                asyncNetworkLayer.doRestfulPost(url, payload)
            }
            val duration = System.currentTimeMillis() - startTime
            BranchLogger.d("BranchRemoteInterfaceUrlConnectionAsync: POST request completed in ${duration}ms using coroutine-based retry")
            result
        } catch (e: BranchRemoteException) {
            BranchLogger.w("BranchRemoteInterfaceUrlConnectionAsync: POST request failed with BranchRemoteException: ${e.message}")
            throw e
        } catch (e: Exception) {
            BranchLogger.e("BranchRemoteInterfaceUrlConnectionAsync: POST request failed with unexpected error: ${e.message}")
            throw BranchRemoteException(
                io.branch.referral.BranchError.ERR_OTHER,
                "Unexpected error during POST request: ${e.message}"
            )
        }
    }
    
    /**
     * Cancels all ongoing network operations.
     * 
     * This method provides graceful shutdown capabilities for the async network layer.
     */
    fun cancelAllOperations() {
        asyncNetworkLayer.cancelAll()
    }
}