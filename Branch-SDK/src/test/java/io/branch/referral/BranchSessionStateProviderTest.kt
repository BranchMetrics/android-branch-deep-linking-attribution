package io.branch.referral

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

/**
 * Unit tests for BranchSessionStateProvider extension function.
 */
@RunWith(JUnit4::class)
class BranchSessionStateProviderTest {

    private lateinit var mockBranch: Branch
    private lateinit var stateProvider: BranchSessionStateProvider

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        mockBranch = mock(Branch::class.java)
        stateProvider = mockBranch.asSessionStateProvider()
    }

    @Test
    fun testIsInitializedWhenBranchHasActiveSessionAndCanPerformOperations() {
        // Mock branch with active session and can perform operations
        `when`(mockBranch.hasActiveSession()).thenReturn(true)
        `when`(mockBranch.canPerformOperations()).thenReturn(true)
        
        assertTrue(stateProvider.isInitialized())
        assertFalse(stateProvider.isInitializing())
        assertFalse(stateProvider.isUninitialized())
    }

    @Test
    fun testIsInitializingWhenBranchHasActiveSessionButCannotPerformOperations() {
        // Mock branch with active session but cannot perform operations
        `when`(mockBranch.hasActiveSession()).thenReturn(true)
        `when`(mockBranch.canPerformOperations()).thenReturn(false)
        
        assertFalse(stateProvider.isInitialized())
        assertTrue(stateProvider.isInitializing())
        assertFalse(stateProvider.isUninitialized())
    }

    @Test
    fun testIsUninitializedWhenBranchHasNoActiveSession() {
        // Mock branch with no active session
        `when`(mockBranch.hasActiveSession()).thenReturn(false)
        `when`(mockBranch.canPerformOperations()).thenReturn(false)
        
        assertFalse(stateProvider.isInitialized())
        assertFalse(stateProvider.isInitializing())
        assertTrue(stateProvider.isUninitialized())
    }

    @Test
    fun testIsUninitializedWhenBranchHasNoActiveSessionButCanPerformOperations() {
        // Edge case: no active session but can perform operations
        `when`(mockBranch.hasActiveSession()).thenReturn(false)
        `when`(mockBranch.canPerformOperations()).thenReturn(true)
        
        assertFalse(stateProvider.isInitialized())
        assertFalse(stateProvider.isInitializing())
        assertTrue(stateProvider.isUninitialized())
    }

    @Test
    fun testStateProviderReflectsCurrentBranchState() {
        // Test that state provider always reflects current branch state
        
        // Initially uninitialized
        `when`(mockBranch.hasActiveSession()).thenReturn(false)
        `when`(mockBranch.canPerformOperations()).thenReturn(false)
        assertTrue(stateProvider.isUninitialized())
        
        // Transition to initializing
        `when`(mockBranch.hasActiveSession()).thenReturn(true)
        `when`(mockBranch.canPerformOperations()).thenReturn(false)
        assertTrue(stateProvider.isInitializing())
        
        // Transition to initialized
        `when`(mockBranch.hasActiveSession()).thenReturn(true)
        `when`(mockBranch.canPerformOperations()).thenReturn(true)
        assertTrue(stateProvider.isInitialized())
        
        // Back to uninitialized
        `when`(mockBranch.hasActiveSession()).thenReturn(false)
        `when`(mockBranch.canPerformOperations()).thenReturn(false)
        assertTrue(stateProvider.isUninitialized())
    }

    @Test
    fun testMutualExclusivityOfStates() {
        // Test that exactly one state method returns true at any time
        
        val testCases = listOf(
            Pair(false, false), // Uninitialized
            Pair(true, false),  // Initializing
            Pair(true, true),   // Initialized
            Pair(false, true)   // Edge case
        )
        
        for ((hasActiveSession, canPerformOperations) in testCases) {
            `when`(mockBranch.hasActiveSession()).thenReturn(hasActiveSession)
            `when`(mockBranch.canPerformOperations()).thenReturn(canPerformOperations)
            
            val states = listOf(
                stateProvider.isInitialized(),
                stateProvider.isInitializing(),
                stateProvider.isUninitialized()
            )
            
            // Exactly one should be true
            assertEquals("Exactly one state should be true for hasActiveSession=$hasActiveSession, canPerformOperations=$canPerformOperations", 
                        1, states.count { it })
        }
    }

    @Test
    fun testExtensionFunctionCanBeCalledMultipleTimes() {
        // Test that calling asSessionStateProvider() multiple times works
        val provider1 = mockBranch.asSessionStateProvider()
        val provider2 = mockBranch.asSessionStateProvider()
        
        // Both should reflect the same state
        `when`(mockBranch.hasActiveSession()).thenReturn(true)
        `when`(mockBranch.canPerformOperations()).thenReturn(true)
        
        assertTrue(provider1.isInitialized())
        assertTrue(provider2.isInitialized())
        
        `when`(mockBranch.hasActiveSession()).thenReturn(false)
        `when`(mockBranch.canPerformOperations()).thenReturn(false)
        
        assertTrue(provider1.isUninitialized())
        assertTrue(provider2.isUninitialized())
    }

    @Test
    fun testStateProviderInterface() {
        // Test that the returned object implements BranchSessionStateProvider
        assertTrue(stateProvider is BranchSessionStateProvider)
        
        // Test that all interface methods are callable
        assertNotNull(stateProvider.isInitialized())
        assertNotNull(stateProvider.isInitializing())
        assertNotNull(stateProvider.isUninitialized())
    }

    @Test
    fun testStateProviderLogic() {
        // Test the specific logic of each state method
        
        // Initialized: hasActiveSession() && canPerformOperations()
        `when`(mockBranch.hasActiveSession()).thenReturn(true)
        `when`(mockBranch.canPerformOperations()).thenReturn(true)
        assertTrue("Should be initialized when has active session and can perform operations", 
                  stateProvider.isInitialized())
        
        // Initializing: hasActiveSession() && !canPerformOperations()
        `when`(mockBranch.hasActiveSession()).thenReturn(true)
        `when`(mockBranch.canPerformOperations()).thenReturn(false)
        assertTrue("Should be initializing when has active session but cannot perform operations", 
                  stateProvider.isInitializing())
        
        // Uninitialized: !hasActiveSession()
        `when`(mockBranch.hasActiveSession()).thenReturn(false)
        `when`(mockBranch.canPerformOperations()).thenReturn(true) // This doesn't matter for uninitialized
        assertTrue("Should be uninitialized when no active session", 
                  stateProvider.isUninitialized())
        
        `when`(mockBranch.hasActiveSession()).thenReturn(false)
        `when`(mockBranch.canPerformOperations()).thenReturn(false)
        assertTrue("Should be uninitialized when no active session", 
                  stateProvider.isUninitialized())
    }

    @Test
    fun testAllPossibleStateCombinations() {
        val combinations = listOf(
            Triple(false, false, "Uninitialized"),
            Triple(false, true, "Uninitialized"), 
            Triple(true, false, "Initializing"),
            Triple(true, true, "Initialized")
        )
        
        for ((hasActiveSession, canPerformOperations, expectedState) in combinations) {
            `when`(mockBranch.hasActiveSession()).thenReturn(hasActiveSession)
            `when`(mockBranch.canPerformOperations()).thenReturn(canPerformOperations)
            
            when (expectedState) {
                "Uninitialized" -> {
                    assertTrue("Should be uninitialized for hasActiveSession=$hasActiveSession, canPerformOperations=$canPerformOperations", 
                              stateProvider.isUninitialized())
                    assertFalse(stateProvider.isInitializing())
                    assertFalse(stateProvider.isInitialized())
                }
                "Initializing" -> {
                    assertTrue("Should be initializing for hasActiveSession=$hasActiveSession, canPerformOperations=$canPerformOperations", 
                              stateProvider.isInitializing())
                    assertFalse(stateProvider.isUninitialized())
                    assertFalse(stateProvider.isInitialized())
                }
                "Initialized" -> {
                    assertTrue("Should be initialized for hasActiveSession=$hasActiveSession, canPerformOperations=$canPerformOperations", 
                              stateProvider.isInitialized())
                    assertFalse(stateProvider.isUninitialized())
                    assertFalse(stateProvider.isInitializing())
                }
            }
        }
    }
} 