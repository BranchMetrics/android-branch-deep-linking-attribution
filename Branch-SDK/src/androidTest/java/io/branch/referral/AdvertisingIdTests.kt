package io.branch.referral

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailabilityLight
import com.huawei.hms.api.HuaweiApiAvailability
import io.branch.coroutines.getAmazonFireAdvertisingInfoObject
import io.branch.coroutines.getGoogleAdvertisingInfoObject
import io.branch.coroutines.getHuaweiAdvertisingInfoObject
import io.branch.referral.SystemObserver.isFireOSDevice
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class AdvertisingIdTests: BranchTest() {

    @Test
    fun googleAdvertisingInfoObjectNotNullWhenServiceAvailable() = runTest {
        if(GoogleApiAvailabilityLight.getInstance().isGooglePlayServicesAvailable(testContext) == ConnectionResult.SUCCESS) {
            val googleAdvertisingInfoObject = getGoogleAdvertisingInfoObject(testContext)
            Assert.assertNotNull(googleAdvertisingInfoObject)
        }
    }

    @Test
    fun googleAdvertisingInfoObjectNullWhenServiceUnavailable() = runTest {
        if(GoogleApiAvailabilityLight.getInstance().isGooglePlayServicesAvailable(testContext) != ConnectionResult.SUCCESS) {
            val googleAdvertisingInfoObject = getGoogleAdvertisingInfoObject(testContext)
            Assert.assertNull(googleAdvertisingInfoObject)
        }
    }

    @Test
    fun huaweiAdvertisingInfoObjectNotNullWhenServiceAvailable() = runTest {
        if(HuaweiApiAvailability.getInstance().isHuaweiMobileServicesAvailable(testContext) == com.huawei.hms.api.ConnectionResult.SUCCESS) {
            val huaweiAdvertisingObject = getHuaweiAdvertisingInfoObject(testContext)
            Assert.assertNotNull(huaweiAdvertisingObject)
        }
    }

    @Test
    fun huaweiAdvertisingInfoObjectNullWhenServiceUnavailable() = runTest {
        if(HuaweiApiAvailability.getInstance().isHuaweiMobileServicesAvailable(testContext) != com.huawei.hms.api.ConnectionResult.SUCCESS) {
            val huaweiAdvertisingObject = getHuaweiAdvertisingInfoObject(testContext)
            Assert.assertNull(huaweiAdvertisingObject)
        }
    }

    @Test
    fun amazonAdvertisingInfoObjectNotNullWhenServiceAvailable() = runTest {
        if(isFireOSDevice()) {
            val amazonAdvertisingObject = getAmazonFireAdvertisingInfoObject(testContext)
            Assert.assertNotNull(amazonAdvertisingObject)
        }
    }

    @Test
    fun amazonAdvertisingInfoObjectNullWhenServiceUnavailable() = runTest {
        if(!isFireOSDevice()) {
            val amazonAdvertisingObject = getAmazonFireAdvertisingInfoObject(testContext)
            Assert.assertNull(amazonAdvertisingObject)
        }
    }
}