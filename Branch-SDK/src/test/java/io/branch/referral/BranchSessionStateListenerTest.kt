package io.branch.referral

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * Unit tests for BranchSessionStateListener interface and related components.
 */
@RunWith(JUnit4::class)
class BranchSessionStateListenerTest {

    @Test
    fun testBranchSessionStateListenerInterface() {
        var receivedPreviousState: BranchSessionState? = null
        var receivedCurrentState: BranchSessionState? = null
        var callCount = 0

        val listener = object : BranchSessionStateListener {
            override fun onSessionStateChanged(previousState: BranchSessionState?, currentState: BranchSessionState) {
                receivedPreviousState = previousState
                receivedCurrentState = currentState
                callCount++
            }
        }

        // Test initial state change (no previous state)
        listener.onSessionStateChanged(null, BranchSessionState.Initializing)
        
        assertNull(receivedPreviousState)
        assertEquals(BranchSessionState.Initializing, receivedCurrentState)
        assertEquals(1, callCount)

        // Test state transition with previous state
        listener.onSessionStateChanged(BranchSessionState.Initializing, BranchSessionState.Initialized)
        
        assertEquals(BranchSessionState.Initializing, receivedPreviousState)
        assertEquals(BranchSessionState.Initialized, receivedCurrentState)
        assertEquals(2, callCount)
    }

    @Test
    fun testSimpleBranchSessionStateListener() {
        var receivedState: BranchSessionState? = null
        var callCount = 0

        val simpleListener = SimpleBranchSessionStateListener { state ->
            receivedState = state
            callCount++
        }

        // Test state change
        simpleListener.onStateChanged(BranchSessionState.Initialized)
        
        assertEquals(BranchSessionState.Initialized, receivedState)
        assertEquals(1, callCount)

        // Test another state change
        simpleListener.onStateChanged(BranchSessionState.Uninitialized)
        
        assertEquals(BranchSessionState.Uninitialized, receivedState)
        assertEquals(2, callCount)
    }

    @Test
    fun testSimpleBranchSessionStateListenerToBranchSessionStateListener() {
        var receivedState: BranchSessionState? = null
        var callCount = 0

        val simpleListener = SimpleBranchSessionStateListener { state ->
            receivedState = state
            callCount++
        }

        // Convert to BranchSessionStateListener
        val convertedListener = simpleListener.toBranchSessionStateListener()

        // Test that it only passes the current state, ignoring previous state
        convertedListener.onSessionStateChanged(BranchSessionState.Uninitialized, BranchSessionState.Initializing)
        
        assertEquals(BranchSessionState.Initializing, receivedState)
        assertEquals(1, callCount)

        // Test with null previous state
        convertedListener.onSessionStateChanged(null, BranchSessionState.Initialized)
        
        assertEquals(BranchSessionState.Initialized, receivedState)
        assertEquals(2, callCount)
    }

    @Test
    fun testMultipleStateTransitions() {
        val stateHistory = mutableListOf<Pair<BranchSessionState?, BranchSessionState>>()

        val listener = object : BranchSessionStateListener {
            override fun onSessionStateChanged(previousState: BranchSessionState?, currentState: BranchSessionState) {
                stateHistory.add(Pair(previousState, currentState))
            }
        }

        // Simulate a typical state transition flow
        listener.onSessionStateChanged(null, BranchSessionState.Uninitialized)
        listener.onSessionStateChanged(BranchSessionState.Uninitialized, BranchSessionState.Initializing)
        listener.onSessionStateChanged(BranchSessionState.Initializing, BranchSessionState.Initialized)

        assertEquals(3, stateHistory.size)
        
        // Verify first transition (initial state)
        assertNull(stateHistory[0].first)
        assertEquals(BranchSessionState.Uninitialized, stateHistory[0].second)
        
        // Verify second transition
        assertEquals(BranchSessionState.Uninitialized, stateHistory[1].first)
        assertEquals(BranchSessionState.Initializing, stateHistory[1].second)
        
        // Verify third transition
        assertEquals(BranchSessionState.Initializing, stateHistory[2].first)
        assertEquals(BranchSessionState.Initialized, stateHistory[2].second)
    }

    @Test
    fun testErrorStateTransitions() {
        val stateHistory = mutableListOf<BranchSessionState>()

        val listener = object : BranchSessionStateListener {
            override fun onSessionStateChanged(previousState: BranchSessionState?, currentState: BranchSessionState) {
                stateHistory.add(currentState)
            }
        }

        val error = BranchError("Test error", BranchError.ERR_BRANCH_INIT_FAILED)
        val failedState = BranchSessionState.Failed(error)

        // Test transition to error state
        listener.onSessionStateChanged(BranchSessionState.Initializing, failedState)
        listener.onSessionStateChanged(failedState, BranchSessionState.Initializing) // Retry
        listener.onSessionStateChanged(BranchSessionState.Initializing, BranchSessionState.Initialized)

        assertEquals(3, stateHistory.size)
        assertEquals(failedState, stateHistory[0])
        assertEquals(BranchSessionState.Initializing, stateHistory[1])
        assertEquals(BranchSessionState.Initialized, stateHistory[2])
    }

    @Test
    fun testResetStateTransitions() {
        val stateHistory = mutableListOf<BranchSessionState>()

        val listener = object : BranchSessionStateListener {
            override fun onSessionStateChanged(previousState: BranchSessionState?, currentState: BranchSessionState) {
                stateHistory.add(currentState)
            }
        }

        // Test reset flow
        listener.onSessionStateChanged(BranchSessionState.Initialized, BranchSessionState.Resetting)
        listener.onSessionStateChanged(BranchSessionState.Resetting, BranchSessionState.Uninitialized)

        assertEquals(2, stateHistory.size)
        assertEquals(BranchSessionState.Resetting, stateHistory[0])
        assertEquals(BranchSessionState.Uninitialized, stateHistory[1])
    }

    @Test
    fun testSimpleListenerWithAllStates() {
        val receivedStates = mutableListOf<BranchSessionState>()

        val simpleListener = SimpleBranchSessionStateListener { state ->
            receivedStates.add(state)
        }

        val allStates = listOf(
            BranchSessionState.Uninitialized,
            BranchSessionState.Initializing,
            BranchSessionState.Initialized,
            BranchSessionState.Failed(BranchError("Error", BranchError.ERR_BRANCH_INIT_FAILED)),
            BranchSessionState.Resetting
        )

        allStates.forEach { state ->
            simpleListener.onStateChanged(state)
        }

        assertEquals(allStates.size, receivedStates.size)
        assertEquals(allStates, receivedStates)
    }

    @Test
    fun testListenerExceptionHandling() {
        // Test that listeners can throw exceptions without affecting the test framework
        val throwingListener = object : BranchSessionStateListener {
            override fun onSessionStateChanged(previousState: BranchSessionState?, currentState: BranchSessionState) {
                throw RuntimeException("Test exception")
            }
        }

        // This should not crash the test - we're just testing the interface contract
        try {
            throwingListener.onSessionStateChanged(null, BranchSessionState.Initialized)
            fail("Expected exception was not thrown")
        } catch (e: RuntimeException) {
            assertEquals("Test exception", e.message)
        }
    }

    @Test
    fun testConvertedListenerBehavior() {
        var lastReceivedState: BranchSessionState? = null
        var callCount = 0

        val simpleListener = SimpleBranchSessionStateListener { state ->
            lastReceivedState = state
            callCount++
        }

        val convertedListener = simpleListener.toBranchSessionStateListener()

        // Test that converted listener ignores previous state parameter
        convertedListener.onSessionStateChanged(BranchSessionState.Initialized, BranchSessionState.Resetting)
        assertEquals(BranchSessionState.Resetting, lastReceivedState)
        assertEquals(1, callCount)

        convertedListener.onSessionStateChanged(BranchSessionState.Resetting, BranchSessionState.Uninitialized)
        assertEquals(BranchSessionState.Uninitialized, lastReceivedState)
        assertEquals(2, callCount)

        // Verify that different previous states don't affect the simple listener
        convertedListener.onSessionStateChanged(BranchSessionState.Uninitialized, BranchSessionState.Initializing)
        convertedListener.onSessionStateChanged(BranchSessionState.Initialized, BranchSessionState.Initializing) // Different previous state, same current
        
        assertEquals(BranchSessionState.Initializing, lastReceivedState)
        assertEquals(4, callCount) // Should have been called for both
    }
} 