package io.branch.referral

import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.mockito.Mockito.*
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Simple test to verify ModernLinkGenerator basic functionality
 */
class ModernLinkGeneratorSimpleTest {
    
    @Test
    fun `ModernLinkGenerator should be instantiable`() {
        // Given
        val mockContext = mock(android.content.Context::class.java)
        val mockRemoteInterface = mock(BranchRemoteInterface::class.java)
        val mockPrefHelper = mock(PrefHelper::class.java)
        
        // When
        val linkGenerator = ModernLinkGenerator(
            context = mockContext,
            branchRemoteInterface = mockRemoteInterface,
            prefHelper = mockPrefHelper
        )
        
        // Then
        assertNotNull(linkGenerator)
        assertTrue(linkGenerator.getCacheSize() == 0)
    }
    
    @Test
    fun `ModernLinkGenerator should handle shutdown gracefully`() {
        // Given
        val mockContext = mock(android.content.Context::class.java)
        val mockRemoteInterface = mock(BranchRemoteInterface::class.java)
        val mockPrefHelper = mock(PrefHelper::class.java)
        
        val linkGenerator = ModernLinkGenerator(
            context = mockContext,
            branchRemoteInterface = mockRemoteInterface,
            prefHelper = mockPrefHelper
        )
        
        // When
        linkGenerator.shutdown()
        
        // Then
        assertTrue(linkGenerator.getCacheSize() == 0)
    }
}