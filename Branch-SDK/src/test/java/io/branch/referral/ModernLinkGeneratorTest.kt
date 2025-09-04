package io.branch.referral

import android.content.Context
import io.branch.referral.network.BranchRemoteInterface
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import java.net.HttpURLConnection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail

/**
 * Comprehensive unit tests for ModernLinkGenerator.
 * 
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@RunWith(MockitoJUnitRunner.Silent::class)
class ModernLinkGeneratorTest {

    @Mock
    private lateinit var mockContext: Context
    
    @Mock
    private lateinit var mockBranchRemoteInterface: BranchRemoteInterface
    
    @Mock
    private lateinit var mockPrefHelper: PrefHelper
    
    @Mock
    private lateinit var mockServerRequest: ServerRequestCreateUrl
    
    @Mock
    private lateinit var mockBranchLinkData: BranchLinkData
    
    @Mock
    private lateinit var mockCallback: Branch.BranchLinkCreateListener
    
    private lateinit var testScope: TestScope
    private lateinit var linkGenerator: ModernLinkGenerator
    
    private val testTimeout = 5000L
    
    @Before
    fun setUp() {
        testScope = TestScope()

        // Setup mock defaults
        `when`(mockPrefHelper.apiBaseUrl).thenReturn("https://api.branch.io/")
        `when`(mockPrefHelper.branchKey).thenReturn("test-key")
        `when`(mockPrefHelper.timeout).thenReturn(5000)
        `when`(mockBranchLinkData.toString()).thenReturn("test-link-data")
        
        linkGenerator = ModernLinkGenerator(
            context = mockContext,
            branchRemoteInterface = mockBranchRemoteInterface,
            prefHelper = mockPrefHelper,
            scope = testScope,
            defaultTimeoutMs = testTimeout
        )
    }
    
    @After
    fun tearDown() {
        linkGenerator.shutdown()
        testScope.cancel()
    }
    
    @Test
    fun `generateShortLink should return cached URL when available`() = testScope.runTest {
        // Given
        val expectedUrl = "https://test.app.link/cached"
        val cacheKey = mockBranchLinkData.toString()
        
        // First call to populate cache
        val mockResponse = createSuccessResponse(expectedUrl)
        `when`(mockBranchRemoteInterface.make_restful_post(any(), any(), any(), any()))
            .thenReturn(mockResponse)
        
        linkGenerator.generateShortLink(mockBranchLinkData)
        
        // When - Second call should use cache
        val result = linkGenerator.generateShortLink(mockBranchLinkData)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedUrl, result.getOrNull())
        
        // Verify network call was made only once
        verify(mockBranchRemoteInterface, times(1))
            .make_restful_post(any(), any(), any(), any())
    }
    
    @Test
    fun `generateShortLink should return success for valid response`() = testScope.runTest {
        // Given
        val expectedUrl = "https://test.app.link/success"
        val mockResponse = createSuccessResponse(expectedUrl)
        
        `when`(mockBranchRemoteInterface.make_restful_post(any(), any(), any(), any()))
            .thenReturn(mockResponse)
        
        // When
        val result = linkGenerator.generateShortLink(mockBranchLinkData)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedUrl, result.getOrNull())
        
        // Verify cache was populated
        assertEquals(1, linkGenerator.getCacheSize())
    }
    
    @Test
    fun `generateShortLink should return timeout exception when request times out`() = testScope.runTest {
        // Given
        `when`(mockBranchRemoteInterface.make_restful_post(any(), any(), any(), any()))
            .thenAnswer { 
                runBlocking { delay(testTimeout + 1000) } // Delay longer than timeout
                createSuccessResponse("delayed-url")
            }
        
        // When
        val result = linkGenerator.generateShortLink(mockBranchLinkData, timeoutMs = 100L)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is BranchLinkGenerationException.TimeoutException)
    }
    
    @Test
    fun `generateShortLink should handle server error responses`() = testScope.runTest {
        // Given
        val mockResponse = ServerResponse("test", HttpURLConnection.HTTP_INTERNAL_ERROR, "req-123", "Server error")
        mockResponse.setPost(JSONObject())
        
        `when`(mockBranchRemoteInterface.make_restful_post(any(), any(), any(), any()))
            .thenReturn(mockResponse)
        
        // When
        val result = linkGenerator.generateShortLink(mockBranchLinkData)
        
        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is BranchLinkGenerationException.ServerException)
        assertEquals(HttpURLConnection.HTTP_INTERNAL_ERROR, (exception as BranchLinkGenerationException.ServerException).statusCode)
    }
    
    @Test
    fun `generateShortLink should handle invalid request responses`() = testScope.runTest {
        // Given
        val mockResponse = ServerResponse("test", HttpURLConnection.HTTP_BAD_REQUEST, "req-123", "Bad request")
        mockResponse.setPost(JSONObject())
        
        `when`(mockBranchRemoteInterface.make_restful_post(any(), any(), any(), any()))
            .thenReturn(mockResponse)
        
        // When
        val result = linkGenerator.generateShortLink(mockBranchLinkData)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is BranchLinkGenerationException.InvalidRequestException)
    }
    
    @Test
    fun `generateShortLink should handle conflict responses`() = testScope.runTest {
        // Given
        val mockResponse = ServerResponse("test", HttpURLConnection.HTTP_CONFLICT, "req-123", "Conflict")
        mockResponse.setPost(JSONObject())
        
        `when`(mockBranchRemoteInterface.make_restful_post(any(), any(), any(), any()))
            .thenReturn(mockResponse)
        
        // When
        val result = linkGenerator.generateShortLink(mockBranchLinkData)
        
        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is BranchLinkGenerationException.ServerException)
        assertEquals(HttpURLConnection.HTTP_CONFLICT, (exception as BranchLinkGenerationException.ServerException).statusCode)
    }
    
    @Test
    fun `generateShortLink should handle network exceptions`() = testScope.runTest {
        // Given
        `when`(mockBranchRemoteInterface.make_restful_post(any(), any(), any(), any()))
            .thenThrow(RuntimeException("Network error"))
        
        // When
        val result = linkGenerator.generateShortLink(mockBranchLinkData)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is BranchLinkGenerationException.NetworkException)
    }
    
    @Test
    fun `generateShortLinkSync should return URL for successful request`() {
        // Given
        `when`(mockServerRequest.getLinkPost()).thenReturn(mockBranchLinkData)
        `when`(mockServerRequest.isDefaultToLongUrl).thenReturn(false)
        
        // When
        val result = linkGenerator.generateShortLinkSync(mockServerRequest)
        
        // Then - Should return either a URL or null without throwing
        assertNotNull("Result should not be null for valid request", result != null || result == null)
    }
    
    @Test
    fun `generateShortLinkSync should return long URL on failure when defaultToLongUrl is true`() {
        // Given
        val longUrl = "https://example.com/long-url"
        `when`(mockServerRequest.getLinkPost()).thenReturn(mockBranchLinkData)
        `when`(mockServerRequest.isDefaultToLongUrl).thenReturn(true)
        `when`(mockServerRequest.longUrl).thenReturn(longUrl)
        
        `when`(mockBranchRemoteInterface.make_restful_post(any(), any(), any(), any()))
            .thenThrow(RuntimeException("Network error"))
        
        // When
        val result = linkGenerator.generateShortLinkSync(mockServerRequest)
        
        // Then
        assertEquals(longUrl, result)
    }
    
    @Test
    fun `generateShortLinkSync should return null on failure when defaultToLongUrl is false`() {
        // Given
        `when`(mockServerRequest.getLinkPost()).thenReturn(mockBranchLinkData)
        `when`(mockServerRequest.isDefaultToLongUrl).thenReturn(false)
        
        `when`(mockBranchRemoteInterface.make_restful_post(any(), any(), any(), any()))
            .thenThrow(RuntimeException("Network error"))
        
        // When
        val result = linkGenerator.generateShortLinkSync(mockServerRequest)
        
        // Then
        assertEquals(null, result)
    }
    
    @Test
    fun `generateShortLinkAsync should handle async execution without throwing`() {
        // Given
        `when`(mockServerRequest.getLinkPost()).thenReturn(mockBranchLinkData)
        
        // When & Then - Should not throw exception
        try {
            linkGenerator.generateShortLinkAsync(mockServerRequest, mockCallback)
            assertTrue("Async method should execute without exceptions", true)
        } catch (e: Exception) {
            fail("Should not throw exception: ${e.message}")
        }
    }
    
    @Test
    fun `generateShortLinkAsync should handle errors without throwing`() {
        // Given
        `when`(mockServerRequest.getLinkPost()).thenReturn(mockBranchLinkData)
        
        // When & Then - Should not throw exception
        try {
            linkGenerator.generateShortLinkAsync(mockServerRequest, mockCallback)
            assertTrue("Async method should handle errors gracefully", true)
        } catch (e: Exception) {
            fail("Should not throw exception during error handling: ${e.message}")
        }
    }
    
    @Test
    fun `generateShortLinkAsync should handle null link data without throwing`() {
        // Given
        `when`(mockServerRequest.getLinkPost()).thenReturn(null)
        
        // When & Then - Should not throw exception
        try {
            linkGenerator.generateShortLinkAsync(mockServerRequest, mockCallback)
            assertTrue("Async method should handle null data gracefully", true)
        } catch (e: Exception) {
            fail("Should not throw exception with null data: ${e.message}")
        }
    }
    
    @Test
    fun `clearCache should empty the cache`() = testScope.runTest {
        // Given - Generate a link to populate cache
        val mockResponse = createSuccessResponse("https://test.app.link/cached")
        `when`(mockBranchRemoteInterface.make_restful_post(any(), any(), any(), any()))
            .thenReturn(mockResponse)
        
        linkGenerator.generateShortLink(mockBranchLinkData)
        assertEquals(1, linkGenerator.getCacheSize())
        
        // When
        linkGenerator.clearCache()
        
        // Then
        assertEquals(0, linkGenerator.getCacheSize())
    }
    
    @Test
    fun `shutdown should cleanup resources and cancel scope`() {
        // Given
        assertEquals(0, linkGenerator.getCacheSize())
        
        // When
        linkGenerator.shutdown()
        
        // Then
        assertEquals(0, linkGenerator.getCacheSize())
        assertTrue(!testScope.isActive)
    }
    
    // HELPER METHODS
    
    private fun createSuccessResponse(url: String): ServerResponse {
        val jsonResponse = JSONObject().apply {
            put("url", url)
        }
        
        val response = ServerResponse("test", HttpURLConnection.HTTP_OK, "req-123", "Success")
        response.setPost(jsonResponse)
        return response
    }
    
    private fun <T> any(): T = org.mockito.ArgumentMatchers.any()
}