package io.branch.referral;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class DeviceInfoTest extends BranchTest {

    @Test
    public void testDeviceInfoExists() {
        initBranchInstance();
        Assert.assertNotNull(DeviceInfo.getInstance());
    }

    @Test
    public void testHardwareIdDebug() {
        // NOTE that this is essentially the same test as using Simulated Installs
        initBranchInstance();
        Assert.assertNotNull(DeviceInfo.getInstance());

        SystemObserver.UniqueId uniqueId1 = DeviceInfo.getInstance().getHardwareID();
        SystemObserver.UniqueId uniqueId2 = DeviceInfo.getInstance().getHardwareID();
        Assert.assertEquals(uniqueId1, uniqueId2);
    }

    @Test
    public void testHardwareIdSimulatedInstall() {
        // NOTE that this is essentially the same test as using Debug
        initBranchInstance();
        Assert.assertNotNull(DeviceInfo.getInstance());

        // Start with simulation mode off and get a hardwareId
        SystemObserver.UniqueId uniqueId1 = DeviceInfo.getInstance().getHardwareID();

        // Enable simulated installs
        Branch.disableDeviceIDFetch(true);
        SystemObserver.UniqueId uniqueSimulatedId1 = DeviceInfo.getInstance().getHardwareID();
        SystemObserver.UniqueId uniqueSimulatedId2 = DeviceInfo.getInstance().getHardwareID();

        // Per design, two requests for simulated IDs must be different.
        Assert.assertNotEquals(uniqueSimulatedId1, uniqueSimulatedId2);

        // A "Real" hardware Id should always be identical, even after switching simulation mode on and off.
        Branch.disableDeviceIDFetch(false);
        SystemObserver.UniqueId uniqueId2 = DeviceInfo.getInstance().getHardwareID();
        Assert.assertEquals(uniqueId1, uniqueId2);
    }

    // TODO: Need to mock advertising ID providers
    //  Tests are written to assume a valid AID is obtainable from the system
    @Test
    public void testGAIDFetch() throws InterruptedException {
        initBranchInstance();
        Assert.assertNotNull(DeviceInfo.getInstance());

        final CountDownLatch latch = new CountDownLatch(1);
        DeviceInfo.getInstance().getSystemObserver().prefetchAdsParams(getTestContext(), new SystemObserver.AdsParamsFetchEvents() {
            @Override
            public void onAdsParamsFetchFinished() {
                latch.countDown();
            }
        });

        Assert.assertTrue(latch.await(5000, TimeUnit.MILLISECONDS));

        Assert.assertFalse(DeviceInfo.isNullOrEmptyOrBlank(DeviceInfo.getInstance().getSystemObserver().getAID()));
    }
}
