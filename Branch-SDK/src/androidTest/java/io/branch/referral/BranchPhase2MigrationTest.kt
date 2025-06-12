package io.branch.referral

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class BranchPhase2MigrationTest : BranchTest() {

    @Test
    fun testBranchInstanceUsesNewQueue() = runTest {
        initBranchInstance()
        val branch = Branch.getInstance()
        
        // Verify that Branch is using the new adapter
        Assert.assertNotNull(branch.requestQueue_)
        Assert.assertTrue("Request queue should be BranchRequestQueueAdapter", 
            branch.requestQueue_ is BranchRequestQueueAdapter)
    }

    @Test
    fun testSessionInitializationWithNewQueue() = runTest {
        initBranchInstance()
        val branch = Branch.getInstance()
        
        // Test that the queue works for session initialization
        Assert.assertNotNull(branch.requestQueue_)
        Assert.assertEquals(0, branch.requestQueue_.getSize())
        
        // Test hasUser functionality
        val hasUser = branch.requestQueue_.hasUser()
        Assert.assertFalse("Initially should not have user", hasUser)
    }

    @Test
    fun testInstrumentationDataIntegration() = runTest {
        initBranchInstance()
        val branch = Branch.getInstance()
        
        // Test that instrumentation data works through the new queue
        branch.requestQueue_.addExtraInstrumentationData("test_phase2", "migration_success")
        
        // Verify data is stored
        val underlyingQueue = BranchRequestQueue.getInstance(testContext)
        Assert.assertEquals("migration_success", underlyingQueue.instrumentationExtraData["test_phase2"])
    }

    @Test
    fun testQueueOperationsCompatibility() = runTest {
        initBranchInstance()
        val branch = Branch.getInstance()
        
        // Test all the queue operations used in Branch.java work
        Assert.assertEquals(0, branch.requestQueue_.getSize())
        
        // Test print queue (should not crash)
        branch.requestQueue_.printQueue()
        
        // Test process next queue item (should not crash)
        branch.requestQueue_.processNextQueueItem("test")
        
        // Test unlock process wait (should not crash)
        branch.requestQueue_.unlockProcessWait(ServerRequest.PROCESS_WAIT_LOCK.SDK_INIT_WAIT_LOCK)
        
        // Test clear
        branch.requestQueue_.clear()
        Assert.assertEquals(0, branch.requestQueue_.getSize())
    }

    @Test
    fun testBranchShutdownWithNewQueue() = runTest {
        initBranchInstance()
        val branch = Branch.getInstance()
        Assert.assertNotNull(branch.requestQueue_)
        
        // Test shutdown doesn't crash
        Branch.shutDown()
        
        // Reinitialize for cleanup
        initBranchInstance()
    }

    @Test
    fun testGetInstallOrOpenRequestWithNewQueue() = runTest {
        initBranchInstance()
        val branch = Branch.getInstance()
        
        // Test that getInstallOrOpenRequest works with new queue
        val request = branch.getInstallOrOpenRequest(null, true)
        Assert.assertNotNull(request)
        
        // Should be install request since no user exists yet
        Assert.assertTrue("Should be install request", request is ServerRequestRegisterInstall)
    }

    @Test
    fun testBranchMethodsStillWork() = runTest {
        initBranchInstance()
        val branch = Branch.getInstance()
        
        // Test that core Branch methods still work with new queue
        val firstParams = branch.firstReferringParams
        Assert.assertNotNull(firstParams)
        
        val latestParams = branch.latestReferringParams
        Assert.assertNotNull(latestParams)
        
        // Test session state management
        val initState = branch.initState
        Assert.assertEquals(Branch.SESSION_STATE.UNINITIALISED, initState)
    }

    @Test
    fun testUnlockSDKInitWaitLock() = runTest {
        initBranchInstance()
        val branch = Branch.getInstance()
        
        // Test that unlockSDKInitWaitLock works with new queue
        // This should not crash
        branch.unlockSDKInitWaitLock()
        
        Assert.assertNotNull(branch.requestQueue_)
    }

    @Test
    fun testClearPendingRequests() = runTest {
        initBranchInstance()
        val branch = Branch.getInstance()
        
        // Test that clearPendingRequests works
        branch.clearPendingRequests()
        
        Assert.assertEquals(0, branch.requestQueue_.getSize())
    }

    @Test
    fun testNotifyNetworkAvailable() = runTest {
        initBranchInstance()
        val branch = Branch.getInstance()
        
        // Test that notifyNetworkAvailable works with new queue
        // This should not crash
        branch.notifyNetworkAvailable()
        
        Assert.assertNotNull(branch.requestQueue_)
    }
} 