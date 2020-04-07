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
        Assert.assertNotNull(Branch.getInstance(getTestContext()));
        Assert.assertNotNull(DeviceInfo.getInstance());
    }

    @Test
    public void testHardwareIdDebug() {
        // NOTE that this is essentially the same test as using Simulated Installs
        Assert.assertNotNull(Branch.getInstance(getTestContext()));
        Assert.assertNotNull(DeviceInfo.getInstance());

        // Start with debug mode off and get a hardwareId
        SystemObserver.UniqueId uniqueId1 = DeviceInfo.getInstance().getHardwareID();

        // Enable debug mode
        Branch.enableDebugMode();
        SystemObserver.UniqueId uniqueDebugId1 = DeviceInfo.getInstance().getHardwareID();
        SystemObserver.UniqueId uniqueDebugId2 = DeviceInfo.getInstance().getHardwareID();

        // Per design, two requests for debug IDs must be different.
        Assert.assertNotEquals(uniqueDebugId1, uniqueDebugId2);

        // A "Real" hardware Id should always be identical, even after switching debug on and off.
        Branch.disableDebugMode();
        SystemObserver.UniqueId uniqueId2 = DeviceInfo.getInstance().getHardwareID();
        Assert.assertEquals(uniqueId1, uniqueId2);
    }

    @Test
    public void testHardwareIdSimulatedInstall() {
        // NOTE that this is essentially the same test as using Debug
        Assert.assertNotNull(Branch.getInstance(getTestContext()));
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

    @Test
    public void testGAIDFetch() {
        Assert.assertNotNull(Branch.getInstance(getTestContext()));
        Assert.assertNotNull(DeviceInfo.getInstance());

        final CountDownLatch latch = new CountDownLatch(1);
        DeviceInfo.getInstance().getSystemObserver().prefetchAdsParams(getTestContext(), new SystemObserver.AdsParamsFetchEvents() {
            @Override
            public void onAdsParamsFetchFinished() {
                latch.countDown();
            }
        });

        try {
            latch.await(5000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Assert.fail();
        }

        Assert.assertFalse(DeviceInfo.isNullOrEmptyOrBlank(DeviceInfo.getInstance().getSystemObserver().getAID()));
    }
}
