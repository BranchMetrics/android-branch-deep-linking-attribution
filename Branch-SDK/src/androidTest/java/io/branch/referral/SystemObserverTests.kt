package io.branch.referral

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class SystemObserverTests : BranchTest() {
    @Test
    fun testAnonID() {
        initBranchInstance()
        val anonID = SystemObserver.getAnonID(testContext)
        try {
            UUID.fromString(anonID)
        } catch (e: Exception) {
            Assert.fail()
        }
    }

    @Test
    fun testAnonIDChangesWithDisableTracking() {
        // TODO: figure out how to handle disable tracking, seems the tracking controller is not very testable
    }

    @Test
    fun fetchAdIdLat    (){
        initBranchInstance()
        val so = DeviceInfo.getInstance().systemObserver

        so.fetchAdId(
            testContext
        ) {
            if(so.latVal == 0){
                Assert.assertNotNull(so.aid)
            }
            else{
                Assert.assertNull(so.aid)
            }
        }
    }
}