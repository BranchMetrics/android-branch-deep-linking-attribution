package io.branch.referral

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SystemObserverTests : BranchTest() {
    @Test
    fun fetchAdIdLatNotEnabledGaidNotNull(){
        initBranchInstance()
        val so = DeviceInfo.getInstance().systemObserver

        so.fetchAdId(
            testContext
        ) {
            if(so.latVal == 0){
                Assert.assertNotNull(so.aid)
            }
        }
    }

    @Test
    fun fetchAdIdLatEnabledAidNull(){
        initBranchInstance()
        val so = DeviceInfo.getInstance().systemObserver

        so.fetchAdId(
            testContext
        ) {
            if(so.latVal == 1){
                Assert.assertNull(so.aid)
            }
        }
    }
}