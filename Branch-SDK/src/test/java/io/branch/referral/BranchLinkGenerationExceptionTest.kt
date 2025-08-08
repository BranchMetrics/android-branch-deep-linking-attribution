package io.branch.referral

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for BranchLinkGenerationException hierarchy.
 * 
 */
class BranchLinkGenerationExceptionTest {
    
    @Test
    fun `TimeoutException should have correct message and cause`() {
        // Given
        val message = "Request timed out"
        val cause = RuntimeException("Underlying timeout")
        
        // When
        val exception = BranchLinkGenerationException.TimeoutException(message, cause)
        
        // Then
        assertEquals(message, exception.message)
        assertEquals(cause, exception.cause)
        assertTrue(exception is BranchLinkGenerationException)
    }
    
    @Test
    fun `TimeoutException should have default message when none provided`() {
        // When
        val exception = BranchLinkGenerationException.TimeoutException()
        
        // Then
        assertEquals("Link generation timed out", exception.message)
        assertEquals(null, exception.cause)
    }
    
    @Test
    fun `InvalidRequestException should have correct message and cause`() {
        // Given
        val message = "Invalid parameters"
        val cause = IllegalArgumentException("Bad argument")
        
        // When
        val exception = BranchLinkGenerationException.InvalidRequestException(message, cause)
        
        // Then
        assertEquals(message, exception.message)
        assertEquals(cause, exception.cause)
        assertTrue(exception is BranchLinkGenerationException)
    }
    
    @Test
    fun `InvalidRequestException should have default message when none provided`() {
        // When
        val exception = BranchLinkGenerationException.InvalidRequestException()
        
        // Then
        assertEquals("Invalid link generation request", exception.message)
        assertEquals(null, exception.cause)
    }
    
    @Test
    fun `ServerException should include status code in message`() {
        // Given
        val statusCode = 500
        val message = "Internal server error"
        val cause = RuntimeException("Server failure")
        
        // When
        val exception = BranchLinkGenerationException.ServerException(statusCode, message, cause)
        
        // Then
        assertEquals("$message (Status: $statusCode)", exception.message)
        assertEquals(statusCode, exception.statusCode)
        assertEquals(cause, exception.cause)
        assertTrue(exception is BranchLinkGenerationException)
    }
    
    @Test
    fun `ServerException should have default message when none provided`() {
        // Given
        val statusCode = 404
        
        // When
        val exception = BranchLinkGenerationException.ServerException(statusCode)
        
        // Then
        assertEquals("Server error during link generation (Status: $statusCode)", exception.message)
        assertEquals(statusCode, exception.statusCode)
        assertEquals(null, exception.cause)
    }
    
    @Test
    fun `NetworkException should have correct message and cause`() {
        // Given
        val message = "Connection failed"
        val cause = java.net.UnknownHostException("Host not found")
        
        // When
        val exception = BranchLinkGenerationException.NetworkException(message, cause)
        
        // Then
        assertEquals(message, exception.message)
        assertEquals(cause, exception.cause)
        assertTrue(exception is BranchLinkGenerationException)
    }
    
    @Test
    fun `NetworkException should have default message when none provided`() {
        // When
        val exception = BranchLinkGenerationException.NetworkException()
        
        // Then
        assertEquals("Network error during link generation", exception.message)
        assertEquals(null, exception.cause)
    }
    
    @Test
    fun `NotInitializedException should have correct message and cause`() {
        // Given
        val message = "SDK not ready"
        val cause = IllegalStateException("Not initialized")
        
        // When
        val exception = BranchLinkGenerationException.NotInitializedException(message, cause)
        
        // Then
        assertEquals(message, exception.message)
        assertEquals(cause, exception.cause)
        assertTrue(exception is BranchLinkGenerationException)
    }
    
    @Test
    fun `NotInitializedException should have default message when none provided`() {
        // When
        val exception = BranchLinkGenerationException.NotInitializedException()
        
        // Then
        assertEquals("Branch SDK not initialized", exception.message)
        assertEquals(null, exception.cause)
    }
    
    @Test
    fun `GeneralException should have correct message and cause`() {
        // Given
        val message = "Something went wrong"
        val cause = Exception("Unknown error")
        
        // When
        val exception = BranchLinkGenerationException.GeneralException(message, cause)
        
        // Then
        assertEquals(message, exception.message)
        assertEquals(cause, exception.cause)
        assertTrue(exception is BranchLinkGenerationException)
    }
    
    @Test
    fun `GeneralException should have default message when none provided`() {
        // When
        val exception = BranchLinkGenerationException.GeneralException()
        
        // Then
        assertEquals("Link generation failed", exception.message)
        assertEquals(null, exception.cause)
    }
    
    @Test
    fun `all exceptions should be subclasses of BranchLinkGenerationException`() {
        // Given/When
        val timeoutException = BranchLinkGenerationException.TimeoutException()
        val invalidRequestException = BranchLinkGenerationException.InvalidRequestException()
        val serverException = BranchLinkGenerationException.ServerException(500)
        val networkException = BranchLinkGenerationException.NetworkException()
        val notInitializedException = BranchLinkGenerationException.NotInitializedException()
        val generalException = BranchLinkGenerationException.GeneralException()
        
        // Then
        assertTrue(timeoutException is BranchLinkGenerationException)
        assertTrue(invalidRequestException is BranchLinkGenerationException)
        assertTrue(serverException is BranchLinkGenerationException)
        assertTrue(networkException is BranchLinkGenerationException)
        assertTrue(notInitializedException is BranchLinkGenerationException)
        assertTrue(generalException is BranchLinkGenerationException)
    }
    
    @Test
    fun `all exceptions should be subclasses of Exception`() {
        // Given/When
        val timeoutException = BranchLinkGenerationException.TimeoutException()
        val invalidRequestException = BranchLinkGenerationException.InvalidRequestException()
        val serverException = BranchLinkGenerationException.ServerException(500)
        val networkException = BranchLinkGenerationException.NetworkException()
        val notInitializedException = BranchLinkGenerationException.NotInitializedException()
        val generalException = BranchLinkGenerationException.GeneralException()
        
        // Then
        assertTrue(timeoutException is Exception)
        assertTrue(invalidRequestException is Exception)
        assertTrue(serverException is Exception)
        assertTrue(networkException is Exception)
        assertTrue(notInitializedException is Exception)
        assertTrue(generalException is Exception)
    }
}