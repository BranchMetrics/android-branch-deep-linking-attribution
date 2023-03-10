package io.branch.referral;

import static io.branch.referral.Defines.Jsonkey.PartnerData;

import android.content.Context;
import android.os.Build;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.util.UUID;

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

    @Before
    public void setUp() {
        super.setUp();
        initBranchInstance();
        Branch.getInstance().disableTracking(false);
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
    public void testSetReferrerGclidValidForWindow(){
        long testValidForWindow = 1L;

        prefHelper.setReferrerGclidValidForWindow(testValidForWindow);

        long result = prefHelper.getReferrerGclidValidForWindow();
        Assert.assertEquals(testValidForWindow, result);
        prefHelper.setReferrerGclidValidForWindow(PrefHelper.DEFAULT_VALID_WINDOW_FOR_REFERRER_GCLID);
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

        prefHelper.setReferrerGclidValidForWindow(1L);
        prefHelper.setReferrerGclid(testGclid);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException interruptedException) {
            interruptedException.printStackTrace();
        }

        String result = prefHelper.getReferrerGclid();
        Assert.assertNull(result);
        prefHelper.setReferrerGclidValidForWindow(PrefHelper.DEFAULT_VALID_WINDOW_FOR_REFERRER_GCLID);
    }

    @Test
    public void testSetGclid_PastDateReturnsDefault(){
        String testGclid = "testSetGclid_PastDateReturnsDefault";

        //1 millisecond in the past
        prefHelper.setReferrerGclidValidForWindow(-1L);
        prefHelper.setReferrerGclid(testGclid);

        long result = prefHelper.getReferrerGclidValidForWindow();
        Assert.assertEquals(PrefHelper.DEFAULT_VALID_WINDOW_FOR_REFERRER_GCLID, result);

        String resultGclid = prefHelper.getReferrerGclid();
        Assert.assertEquals(testGclid, resultGclid);
        prefHelper.setReferrerGclidValidForWindow(PrefHelper.DEFAULT_VALID_WINDOW_FOR_REFERRER_GCLID);
    }

    @Test
    public void testSetGclid_OverMaximumReturnsDefault(){
        String testGclid = "testSetGclid_OverMaximumReturnsDefault";

        prefHelper.setReferrerGclidValidForWindow(Long.MAX_VALUE);
        prefHelper.setReferrerGclid(testGclid);

        long result = prefHelper.getReferrerGclidValidForWindow();
        Assert.assertEquals(PrefHelper.DEFAULT_VALID_WINDOW_FOR_REFERRER_GCLID, result);

        String resultGclid = prefHelper.getReferrerGclid();
        Assert.assertEquals(testGclid, resultGclid);
        prefHelper.setReferrerGclidValidForWindow(PrefHelper.DEFAULT_VALID_WINDOW_FOR_REFERRER_GCLID);
    }

    @Test
    public void testSetRandomlyGeneratedUuid(){
        String uuid = UUID.randomUUID().toString();

        prefHelper.setRandomlyGeneratedUuid(uuid);
        String result = prefHelper.getRandomlyGeneratedUuid();

        Assert.assertEquals(uuid, result);
    }

    @Test
    public void testSetNoConnectionRetryMaxReturnsDefault(){
        Assert.assertEquals(prefHelper.getNoConnectionRetryMax(), PrefHelper.DEFAULT_NO_CONNECTION_RETRY_MAX);
    }

    @Test
    public void testSetNoConnectionRetryMax(){
        int max = 10;
        prefHelper.setNoConnectionRetryMax(max);

        Assert.assertEquals(max, prefHelper.getNoConnectionRetryMax());
    }

    @Test
    public void testAppStoreSource(){
        prefHelper.setAppStoreSource(Defines.Jsonkey.Google_Play_Store.getKey());

        String result = prefHelper.getAppStoreSource();
        Assert.assertEquals(Defines.Jsonkey.Google_Play_Store.getKey(), result);
    }

    @Test
    public void testFBPartnerParameters(){
        Branch.getInstance().addFacebookPartnerParameterWithName("em", "11234e56af071e9c79927651156bd7a10bca8ac34672aba121056e2698ee7088");

        JSONObject body = new JSONObject();
        try {
            prefHelper.loadPartnerParams(body);
            JSONAssert.assertEquals("{\"fb\":{\"em\":\"11234e56af071e9c79927651156bd7a10bca8ac34672aba121056e2698ee7088\"}}", body.getJSONObject(PartnerData.getKey()).toString(), JSONCompareMode.LENIENT);
        } catch (JSONException e) {
            Assert.fail();
        }
    }

    @Test
    public void testFBPartnerParametersTrackingDisabled(){
        Branch.getInstance().disableTracking(true);
        Branch.getInstance().addFacebookPartnerParameterWithName("em", "0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF");

        JSONObject body = new JSONObject();
        try {
            prefHelper.loadPartnerParams(body);
            JSONAssert.assertEquals("{}", body.getJSONObject(PartnerData.getKey()).toString(), JSONCompareMode.LENIENT);
        } catch (JSONException e) {
            Assert.fail();
        }
    }

    @Test
    public void testFBPartnerParametersTrackingDisabledClearsExistingParams(){
        Branch.getInstance().addFacebookPartnerParameterWithName("em", "11234e56af071e9c79927651156bd7a10bca8ac34672aba121056e2698ee7088");

        JSONObject body = new JSONObject();
        try {
            prefHelper.loadPartnerParams(body);
            JSONAssert.assertEquals("{\"fb\":{\"em\":\"11234e56af071e9c79927651156bd7a10bca8ac34672aba121056e2698ee7088\"}}", body.getJSONObject(PartnerData.getKey()).toString(), JSONCompareMode.LENIENT);
        } catch (JSONException e) {
            Assert.fail();
        }

        body = new JSONObject();
        Branch.getInstance().disableTracking(true);
        try {
            prefHelper.loadPartnerParams(body);
            JSONAssert.assertEquals("{}", body.getJSONObject(PartnerData.getKey()).toString(), JSONCompareMode.LENIENT);
        } catch (JSONException e) {
            Assert.fail();
        }
    }
    @Test
    public void testSnapPartnerParameters(){
        Branch.getInstance().addSnapPartnerParameterWithName("hashed_email_address", "11234e56af071e9c79927651156bd7a10bca8ac34672aba121056e2698ee7088");

        JSONObject body = new JSONObject();
        try {
            prefHelper.loadPartnerParams(body);
            JSONAssert.assertEquals("{\"snap\":{\"hashed_email_address\":\"11234e56af071e9c79927651156bd7a10bca8ac34672aba121056e2698ee7088\"}}", body.getJSONObject(PartnerData.getKey()).toString(), JSONCompareMode.LENIENT);
        } catch (JSONException e) {
            Assert.fail();
        }
    }

    @Test
    public void testSnapPartnerParametersTrackingDisabled(){
        Branch.getInstance().disableTracking(true);
        Branch.getInstance().addSnapPartnerParameterWithName("hashed_email_address", "0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF");

        JSONObject body = new JSONObject();
        try {
            prefHelper.loadPartnerParams(body);
            JSONAssert.assertEquals("{}", body.getJSONObject(PartnerData.getKey()).toString(), JSONCompareMode.LENIENT);
        } catch (JSONException e) {
            Assert.fail();
        }
    }

    @Test
    public void testSnapPartnerParametersTrackingDisabledClearsExistingParams(){
        Branch.getInstance().addSnapPartnerParameterWithName("hashed_email_address", "11234e56af071e9c79927651156bd7a10bca8ac34672aba121056e2698ee7088");

        JSONObject body = new JSONObject();
        try {
            prefHelper.loadPartnerParams(body);
            JSONAssert.assertEquals("{\"snap\":{\"hashed_email_address\":\"11234e56af071e9c79927651156bd7a10bca8ac34672aba121056e2698ee7088\"}}", body.getJSONObject(PartnerData.getKey()).toString(), JSONCompareMode.LENIENT);
        } catch (JSONException e) {
            Assert.fail();
        }

        body = new JSONObject();
        Branch.getInstance().disableTracking(true);
        try {
            prefHelper.loadPartnerParams(body);
            JSONAssert.assertEquals("{}", body.getJSONObject(PartnerData.getKey()).toString(), JSONCompareMode.LENIENT);
        } catch (JSONException e) {
            Assert.fail();
        }
    }
}