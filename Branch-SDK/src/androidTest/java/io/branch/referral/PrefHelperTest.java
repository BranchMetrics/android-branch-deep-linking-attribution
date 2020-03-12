package io.branch.referral;

import android.content.Context;
import android.os.Build;

import org.junit.Assert;
import org.junit.Test;

public class PrefHelperTest extends BranchTest {

    void assertDefaultURL() {
        Context context = getTestContext();
        PrefHelper helper = PrefHelper.getInstance(context);
        String actual = helper.getAPIBaseUrl();

        if (Build.VERSION.SDK_INT >= 20) {
            Assert.assertEquals(PrefHelper.BRANCH_BASE_URL_V2, actual);
        } else {
            Assert.assertEquals(PrefHelper.BRANCH_BASE_URL_V1, actual);
        }
    }

    @Test
    public void testGetAPIBaseUrl() {
        assertDefaultURL();
    }

    @Test
    public void testSetAPIUrl_Example() {
        PrefHelper.setAPIUrl("https://www.example.com/");

        Context context = getTestContext();
        PrefHelper helper = PrefHelper.getInstance(context);
        String actual = helper.getAPIBaseUrl();
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
        Context context = getTestContext();
        PrefHelper prefHelper = PrefHelper.getInstance(context);
        prefHelper.setAdNetworkCalloutsDisabled(true);

        Assert.assertTrue(prefHelper.getAdNetworkCalloutsDisabled());
    }

    @Test
    public void testSetAdNetworkCalloutsEnabled() {
        Context context = getTestContext();
        PrefHelper prefHelper = PrefHelper.getInstance(context);
        prefHelper.setAdNetworkCalloutsDisabled(false);

        Assert.assertFalse(prefHelper.getAdNetworkCalloutsDisabled());
    }
}