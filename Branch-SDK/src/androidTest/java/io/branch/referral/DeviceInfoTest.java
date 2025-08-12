package io.branch.referral;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.test.annotation.UiThreadTest;
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
        SystemObserver.UniqueId uniqueSimulatedId1 = DeviceInfo.getInstance().getHardwareID();
        SystemObserver.UniqueId uniqueSimulatedId2 = DeviceInfo.getInstance().getHardwareID();

        // Per design, two requests for simulated IDs must be different.
        Assert.assertNotEquals(uniqueSimulatedId1, uniqueSimulatedId2);

        // A "Real" hardware Id should always be identical, even after switching simulation mode on and off.
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
        DeviceInfo.getInstance().getSystemObserver().fetchAdId(getTestContext(), new SystemObserver.AdsParamsFetchEvents() {
            @Override
            public void onAdsParamsFetchFinished() {
                latch.countDown();
            }
        });

        Assert.assertTrue(latch.await(5000, TimeUnit.MILLISECONDS));

        if(DeviceInfo.getInstance().getSystemObserver().getLATVal() == 0) {
            Assert.assertFalse(DeviceInfo.isNullOrEmptyOrBlank(DeviceInfo.getInstance().getSystemObserver().getAID()));
        }
        else{
            Assert.assertTrue(DeviceInfo.isNullOrEmptyOrBlank(DeviceInfo.getInstance().getSystemObserver().getAID()));
        }
    }

    @Test
    public void windowManagerAndDisplayManagerSameMetrics(){
        DisplayManager displayManager = (DisplayManager) getTestContext().getSystemService(Context.DISPLAY_SERVICE);
        Display display1 = displayManager.getDisplay(Display.DEFAULT_DISPLAY);
        DisplayMetrics displayMetrics1 = new DisplayMetrics();
        display1.getMetrics(displayMetrics1);

        WindowManager windowManager = (WindowManager) getTestContext().getSystemService(Context.WINDOW_SERVICE);
        Display display2 = windowManager.getDefaultDisplay();
        DisplayMetrics displayMetrics2 = new DisplayMetrics();
        display2.getMetrics(displayMetrics2);

        Assert.assertEquals(displayMetrics1.widthPixels, displayMetrics2.widthPixels);
        Assert.assertEquals(displayMetrics1.heightPixels, displayMetrics2.heightPixels);
        Assert.assertEquals(displayMetrics1.densityDpi, displayMetrics2.densityDpi);
    }

    @Test
    @UiThreadTest //instantiating the webview requires a handler
    public void userAgentStaticAndInstanceSameString(){

        String getDefaultUserAgentString = WebSettings.getDefaultUserAgent(getTestContext());
        WebView w = new WebView(getTestContext());
        String getUserAgentString = w.getSettings().getUserAgentString();

        // This is true assuming the webview package was not updated in between this test execution
        Assert.assertEquals(getDefaultUserAgentString, getUserAgentString);
    }
}
