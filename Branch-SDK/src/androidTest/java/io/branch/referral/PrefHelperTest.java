package io.branch.referral;

import android.content.Context;
import android.os.Build;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class PrefHelperTest extends BranchTest {
    Context context;
    PrefHelper prefHelper;

    void assertDefaultURL() {
        String actual = prefHelper.getAPIBaseUrl();

        if (Build.VERSION.SDK_INT >= 20) {
            Assert.assertEquals(PrefHelper.BRANCH_BASE_URL_V2, actual);
        } else {
            Assert.assertEquals(PrefHelper.BRANCH_BASE_URL_V1, actual);
        }
    }

    @Before
    public void init(){
        context = getTestContext();
        prefHelper = PrefHelper.getInstance(context);
    }

    @Test
    public void testGetAPIBaseUrl() {
        assertDefaultURL();
    }

    @Test
    public void testSetAPIUrl_Example() {
        PrefHelper.setAPIUrl("https://www.example.com/");
        String actual = prefHelper.getAPIBaseUrl();
        Assert.assertEquals("https://www.example.com/", actual);
    }

    @Test
    public void testSetAPIUrl_InvalidHttp() {
        PrefHelper.setAPIUrl("http://www.example.com/");
        assertDefaultURL();
    }

    @Test
    public void testSetAPIUrl_InvalidNull() {
        PrefHelper.setAPIUrl(null);
        assertDefaultURL();
    }

    @Test
    public void testSetAPIUrl_InvalidEmpty() {
        PrefHelper.setAPIUrl("");
        assertDefaultURL();
    }

    @Test
    public void testSetAdNetworkCalloutsDisabled() {
        prefHelper.setAdNetworkCalloutsDisabled(true);

        Assert.assertTrue(prefHelper.getAdNetworkCalloutsDisabled());
    }

    @Test
    public void testSetAdNetworkCalloutsEnabled() {
        prefHelper.setAdNetworkCalloutsDisabled(false);

        Assert.assertFalse(prefHelper.getAdNetworkCalloutsDisabled());
    }

    @Test
    public void testSetTimeout(){
        int TEST_TIMEOUT = 1;
        prefHelper.setTimeout(TEST_TIMEOUT);

        int result = prefHelper.getTimeout();
        Assert.assertEquals(TEST_TIMEOUT, result);
    }

    @Test
    public void testSetConnectTimeout(){
        int TEST_CONNECT_TIMEOUT = 2;
        prefHelper.setConnectTimeout(TEST_CONNECT_TIMEOUT);

        int result = prefHelper.getConnectTimeout();
        Assert.assertEquals(TEST_CONNECT_TIMEOUT, result);
    }

    @Test
    public void testSetTaskTimeout(){
        int TEST_TIMEOUT = 3;
        int TEST_CONNECT_TIMEOUT = 4;

        prefHelper.setTimeout(TEST_TIMEOUT);
        prefHelper.setConnectTimeout(TEST_CONNECT_TIMEOUT);


        int result = prefHelper.getTaskTimeout();
        Assert.assertEquals(TEST_TIMEOUT + TEST_CONNECT_TIMEOUT, result);
    }

    @Test
    public void testSetReferrerGclidExpirationWindow(){
        long testExpirationWindow = 1L;

        prefHelper.setReferrerGclidExpirationWindow(testExpirationWindow);

        long result = prefHelper.getReferrerGclidExpirationWindow();
        Assert.assertEquals(testExpirationWindow, result);
    }

    @Test
    public void testSetGclid(){
        String testGclid = "test_gclid";

        prefHelper.setReferrerGclid(testGclid);

        String result = prefHelper.getReferrerGclid();
        Assert.assertEquals(testGclid, result);
    }

    @Test
    public void testSetGclid_Expired(){
        String testGclid = "testSetGclid_Expired";

        prefHelper.setReferrerGclidExpirationWindow(1L);
        prefHelper.setReferrerGclid(testGclid);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException interruptedException) {
            interruptedException.printStackTrace();
        }

        String result = prefHelper.getReferrerGclid();
        Assert.assertNull(result);
        prefHelper.setReferrerGclidExpirationWindow(PrefHelper.DEFAULT_EXPIRATION_WINDOW_REFERRER_GCLID);
    }
}