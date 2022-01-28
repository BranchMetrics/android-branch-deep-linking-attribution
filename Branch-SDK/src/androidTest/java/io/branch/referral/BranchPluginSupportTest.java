package io.branch.referral;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;

@RunWith(AndroidJUnit4.class)
public class BranchPluginSupportTest extends BranchTest {

    Map<String, Object> deviceData = new HashMap<>();
    @Before
    public void initializeValues(){
        //assign map
        initBranchInstance();
        deviceData = BranchPluginSupport.getInstance().deviceDescription();
    }

    @Test
    public void testBranchPluginSupportExists() {
        Assert.assertNotNull(BranchPluginSupport.getInstance());
    }

    @Test
    public void testHardwareIdDebug() {
        SystemObserver.UniqueId uniqueId1 = BranchPluginSupport.getInstance().getHardwareID();
        SystemObserver.UniqueId uniqueId2 = BranchPluginSupport.getInstance().getHardwareID();
        Assert.assertEquals(uniqueId1, uniqueId2);
    }

    @Test
    public void testAppVersion() {
        System.out.println("deviceData Log: " + deviceData.toString());
        Assert.assertEquals("bnc_no_value", deviceData.get("app_version"));
    }

    @Test
    public void testCountry() {
        Assert.assertNotNull(deviceData.get("country"));
    }

    @Test
    public void testOSVersion() {
        Assert.assertNotNull(deviceData.get("os_version_android"));
    }

    @Test
    public void testScreenWidth() {
        Assert.assertNotNull(deviceData.get("screen_width"));
    }

    @Test
    public void testScreenHeight() {
        Assert.assertNotNull(deviceData.get("screen_height"));
    }

    @Test
    public void testScreenDPI() {
        Assert.assertNotNull(deviceData.get("screen_dpi"));
    }

    @Test
    public void testOS() {
        Assert.assertEquals("Android", deviceData.get("os"));
    }

    @Test
    public void testLanguage() {
        Assert.assertNotNull(deviceData.get("language"));
    }

    @Test
    public void testModel() {
        Assert.assertNotNull(deviceData.get("model"));
    }

    @Test
    public void testAndroidID() {
        Assert.assertNotNull(deviceData.get("android_id"));
    }

}
