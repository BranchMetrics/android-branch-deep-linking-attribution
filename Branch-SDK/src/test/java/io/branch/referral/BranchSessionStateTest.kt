package io.branch.referral

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * Unit tests for BranchSessionState sealed class.
 * Tests all states and their behavior.
 */
@RunWith(JUnit4::class)
class BranchSessionStateTest {

    @Test
    fun testUninitializedState() {
        val state = BranchSessionState.Uninitialized
        
        assertFalse(state.canPerformOperations())
        assertFalse(state.hasActiveSession())
        assertFalse(state.isErrorState())
        assertEquals("Uninitialized", state.toString())
    }
    
    @Test
    fun testInitializingState() {
        val state = BranchSessionState.Initializing
        
        assertFalse(state.canPerformOperations())
        assertFalse(state.hasActiveSession())
        assertFalse(state.isErrorState())
        assertEquals("Initializing", state.toString())
    }
    
    @Test
    fun testInitializedState() {
        val state = BranchSessionState.Initialized
        
        assertTrue(state.canPerformOperations())
        assertTrue(state.hasActiveSession())
        assertFalse(state.isErrorState())
        assertEquals("Initialized", state.toString())
    }
    
    @Test
    fun testFailedState() {
        val error = BranchError("Test error message", BranchError.ERR_BRANCH_INIT_FAILED)
        val state = BranchSessionState.Failed(error)
        
        assertFalse(state.canPerformOperations())
        assertFalse(state.hasActiveSession())
        assertTrue(state.isErrorState())
        assertTrue(state.toString().contains("Failed"))
        assertTrue(state.toString().contains("Test error message"))
        assertEquals(error, state.error)
    }
    
    @Test
    fun testResettingState() {
        val state = BranchSessionState.Resetting
        
        assertFalse(state.canPerformOperations())
        assertFalse(state.hasActiveSession())
        assertFalse(state.isErrorState())
        assertEquals("Resetting", state.toString())
    }
    
    @Test
    fun testStateEquality() {
        // Test object equality for singleton states
        assertEquals(BranchSessionState.Uninitialized, BranchSessionState.Uninitialized)
        assertEquals(BranchSessionState.Initializing, BranchSessionState.Initializing)
        assertEquals(BranchSessionState.Initialized, BranchSessionState.Initialized)
        assertEquals(BranchSessionState.Resetting, BranchSessionState.Resetting)
        
        // Test Failed state equality
        val error1 = BranchError("Error 1", BranchError.ERR_BRANCH_INIT_FAILED)
        val error2 = BranchError("Error 2", BranchError.ERR_BRANCH_KEY_INVALID)
        val failed1 = BranchSessionState.Failed(error1)
        val failed2 = BranchSessionState.Failed(error1)
        val failed3 = BranchSessionState.Failed(error2)
        
        assertEquals(failed1, failed2)
        assertNotEquals(failed1, failed3)
    }
    
    @Test
    fun testStateInequality() {
        // Test that different states are not equal
        assertNotEquals(BranchSessionState.Uninitialized, BranchSessionState.Initializing)
        assertNotEquals(BranchSessionState.Initializing, BranchSessionState.Initialized)
        assertNotEquals(BranchSessionState.Initialized, BranchSessionState.Resetting)
        
        val error = BranchError("Test error", BranchError.ERR_BRANCH_INIT_FAILED)
        val failed = BranchSessionState.Failed(error)
        assertNotEquals(BranchSessionState.Uninitialized, failed)
        assertNotEquals(BranchSessionState.Initializing, failed)
        assertNotEquals(BranchSessionState.Initialized, failed)
        assertNotEquals(BranchSessionState.Resetting, failed)
    }
    
    @Test
    fun testCanPerformOperationsOnlyForInitialized() {
        val states = listOf(
            BranchSessionState.Uninitialized,
            BranchSessionState.Initializing,
            BranchSessionState.Initialized,
            BranchSessionState.Failed(BranchError("Error", BranchError.ERR_BRANCH_INIT_FAILED)),
            BranchSessionState.Resetting
        )
        
        states.forEach { state ->
            if (state is BranchSessionState.Initialized) {
                assertTrue("${state::class.simpleName} should allow operations", state.canPerformOperations())
            } else {
                assertFalse("${state::class.simpleName} should not allow operations", state.canPerformOperations())
            }
        }
    }
    
    @Test
    fun testHasActiveSessionOnlyForInitialized() {
        val states = listOf(
            BranchSessionState.Uninitialized,
            BranchSessionState.Initializing,
            BranchSessionState.Initialized,
            BranchSessionState.Failed(BranchError("Error", BranchError.ERR_BRANCH_INIT_FAILED)),
            BranchSessionState.Resetting
        )
        
        states.forEach { state ->
            if (state is BranchSessionState.Initialized) {
                assertTrue("${state::class.simpleName} should have active session", state.hasActiveSession())
            } else {
                assertFalse("${state::class.simpleName} should not have active session", state.hasActiveSession())
            }
        }
    }
    
    @Test
    fun testIsErrorStateOnlyForFailed() {
        val states = listOf(
            BranchSessionState.Uninitialized,
            BranchSessionState.Initializing,
            BranchSessionState.Initialized,
            BranchSessionState.Failed(BranchError("Error", BranchError.ERR_BRANCH_INIT_FAILED)),
            BranchSessionState.Resetting
        )
        
        states.forEach { state ->
            if (state is BranchSessionState.Failed) {
                assertTrue("${state::class.simpleName} should be error state", state.isErrorState())
            } else {
                assertFalse("${state::class.simpleName} should not be error state", state.isErrorState())
            }
        }
    }
    
    @Test
    fun testToStringForAllStates() {
        assertEquals("Uninitialized", BranchSessionState.Uninitialized.toString())
        assertEquals("Initializing", BranchSessionState.Initializing.toString())
        assertEquals("Initialized", BranchSessionState.Initialized.toString())
        assertEquals("Resetting", BranchSessionState.Resetting.toString())
        
        val error = BranchError("Connection failed", BranchError.ERR_BRANCH_NO_CONNECTIVITY)
        val failed = BranchSessionState.Failed(error)
        val failedString = failed.toString()
        // Check that the string contains the expected elements
        assertTrue("Failed toString should start with 'Failed('", failedString.startsWith("Failed("))
        assertTrue("Failed toString should contain error message", failedString.contains("Connection failed"))
        assertTrue("Failed toString should end with ')'", failedString.endsWith(")"))
    }
    
    @Test
    fun testFailedStateWithDifferentErrors() {
        val initError = BranchError("Init failed", BranchError.ERR_BRANCH_INIT_FAILED)
        val networkError = BranchError("Network error", BranchError.ERR_BRANCH_NO_CONNECTIVITY)
        val keyError = BranchError("Key error", BranchError.ERR_BRANCH_KEY_INVALID)
        
        val failedInit = BranchSessionState.Failed(initError)
        val failedNetwork = BranchSessionState.Failed(networkError)
        val failedKey = BranchSessionState.Failed(keyError)
        
        assertFalse(failedInit.canPerformOperations())
        assertFalse(failedNetwork.canPerformOperations())
        assertFalse(failedKey.canPerformOperations())
        
        assertFalse(failedInit.hasActiveSession())
        assertFalse(failedNetwork.hasActiveSession())
        assertFalse(failedKey.hasActiveSession())
        
        assertTrue(failedInit.isErrorState())
        assertTrue(failedNetwork.isErrorState())
        assertTrue(failedKey.isErrorState())
        
        // Verify error objects are correctly stored
        assertEquals(initError, failedInit.error)
        assertEquals(networkError, failedNetwork.error)
        assertEquals(keyError, failedKey.error)
    }
} 