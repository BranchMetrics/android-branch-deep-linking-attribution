package io.branch.referral;

import android.content.Context;

import org.junit.Assert;
import org.junit.Test;


public class PrefHelperTest extends BranchTest {

    @Test
    public void getAPIBaseUrl() {
        Context context = getTestContext();
        PrefHelper helper = PrefHelper.getInstance(context);
        String actual = helper.getAPIBaseUrl();
        Assert.assertEquals("https://api2.branch.io/", actual);
    }

    @Test
    public void setAPIUrl_Example() {
        PrefHelper.setAPIUrl("https://www.example.com/");

        Context context = getTestContext();
        PrefHelper helper = PrefHelper.getInstance(context);
        String actual = helper.getAPIBaseUrl();
        Assert.assertEquals("https://www.example.com/", actual);
    }

    @Test
    public void setAPIUrl_InvalidHttp() {
        PrefHelper.setAPIUrl("http://www.example.com/");

        Context context = getTestContext();
        PrefHelper helper = PrefHelper.getInstance(context);
        String actual = helper.getAPIBaseUrl();
        Assert.assertEquals("https://api2.branch.io/", actual);
    }

    @Test
    public void setAPIUrl_InvalidNull() {
        PrefHelper.setAPIUrl(null);

        Context context = getTestContext();
        PrefHelper helper = PrefHelper.getInstance(context);
        String actual = helper.getAPIBaseUrl();
        Assert.assertEquals("https://api2.branch.io/", actual);
    }

    @Test
    public void setAPIUrl_InvalidEmpty() {
        PrefHelper.setAPIUrl("");

        Context context = getTestContext();
        PrefHelper helper = PrefHelper.getInstance(context);
        String actual = helper.getAPIBaseUrl();
        Assert.assertEquals("https://api2.branch.io/", actual);
    }
}