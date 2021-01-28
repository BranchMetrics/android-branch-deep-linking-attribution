package io.branch.referral;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.branch.referral.util.BRANCH_STANDARD_EVENT;
import io.branch.referral.util.BranchEvent;
import io.branch.referral.util.CommerceEvent;

/**
 BranchEvent class tests.

 [X] ServerRequestActionCompleted.java          testActionCompleted()
 [ ] ServerRequestCreateUrl.java
 [-] ServerRequestGetRewardHistory.java         testCreditHistory()
 [-] ServerRequestGetRewards.java               testLoadRewards()
 [-] ServerRequestIdentifyUserRequest.java      testIdentity()
 [X] ServerRequestInitSession.java              testInitSession()
 [-] ServerRequestLogout.java                   testLogout()
 [-] ServerRequestPing.java                     testPing()
 [X] ServerRequestActionCompleted.java          testCommerceEvent()
 [X] ServerRequestRedeemRewards.java            testRedeemRewards()
 [-] ServerRequestRegisterClose.java            testClose()
 [ ] ServerRequestRegisterInstall.java
 [ ] ServerRequestRegisterOpen.java
 */

@RunWith(AndroidJUnit4.class)
public class BranchGAIDTest extends BranchEventTest {
    private static final String TAG = "Branch::GAIDTest";
    private static final String TEST_KEY = "key_live_testkey";

    @Test
    public void testInitSession_hasGAIDv1() throws Throwable {
        initBranchInstance();
        initQueue(getTestContext());
        initTestSession();

        ServerRequestQueue queue = ServerRequestQueue.getInstance(getTestContext());
        Assert.assertEquals(1, queue.getSize());

        ServerRequest initRequest = queue.peekAt(0);
        doFinalUpdate(initRequest);

        assumingLatIsDisabledHasGAIDv1(initRequest, true);
        assumingLatIsDisabledHasAdIdFromAdIdsObjectV1(initRequest, true);
        assumingLatIsDisabledHasGAIDv2(initRequest, false);
        assumingLatIsDisabledHasAdIdFromAdIdsObjectV2(initRequest, false);
    }

    @Test
    public void testActionCompleted_hasGAIDv1() throws Throwable {
        Branch.getAutoInstance(getTestContext());
        initQueue(getTestContext());

        JSONObject params = new JSONObject();
        params.put("name", "Alex");
        params.put("boolean", true);
        params.put("int", 1);
        params.put("double", 0.13415512301);

        // final CountDownLatch latch = new CountDownLatch(1);
        Branch.getInstance().userCompletedAction("buy", params);

        ServerRequest serverRequest = findEventOnQueue(getTestContext(), "event", "buy");

        Assert.assertNotNull(serverRequest);
        doFinalUpdate(serverRequest);

        assumingLatIsDisabledHasGAIDv1(serverRequest, true);
        assumingLatIsDisabledHasAdIdFromAdIdsObjectV1(serverRequest, true);
        assumingLatIsDisabledHasGAIDv2(serverRequest, false);
        assumingLatIsDisabledHasAdIdFromAdIdsObjectV2(serverRequest, false);
    }

    @Test
    public void testCommerceEvent_hasGAIDv1() throws Throwable {
        initBranchInstance(TEST_KEY);
        initQueue(getTestContext());

        CommerceEvent commerceEvent = new CommerceEvent();
        commerceEvent.setTransactionID("123XYZ");
        commerceEvent.setRevenue(3.14);
        commerceEvent.setTax(.314);
        commerceEvent.setCoupon("MyCoupon");

        Branch.getInstance().sendCommerceEvent(commerceEvent);
        ServerRequest serverRequest = findEventOnQueue(getTestContext(), "event", BRANCH_STANDARD_EVENT.PURCHASE.getName());

        Assert.assertNotNull(serverRequest);
        doFinalUpdate(serverRequest);

        assumingLatIsDisabledHasGAIDv1(serverRequest, true);
        assumingLatIsDisabledHasAdIdFromAdIdsObjectV1(serverRequest, true);
        assumingLatIsDisabledHasGAIDv2(serverRequest, false);
        assumingLatIsDisabledHasAdIdFromAdIdsObjectV2(serverRequest, false);

        DebugLogQueue(getTestContext());
    }

    @Test
    public void testLoadRewards_hasGAIDv1() throws Throwable {
        // TODO:  loadRewards() puts an empty JSON object on the queue
    }

    @Test
    public void testRedeemAwards_hasGAIDv1() throws Throwable {
        initBranchInstance(TEST_KEY);
        initQueue(getTestContext());

        // Backdoor to set credits before we try to redeem them.
        PrefHelper prefHelper = PrefHelper.getInstance(getTestContext());
        prefHelper.setCreditCount(100);

        Branch.getInstance().redeemRewards(100);
        ServerRequest serverRequest = getLastEventOnQueue(getTestContext(), 1);

        Assert.assertNotNull(serverRequest);
        doFinalUpdate(serverRequest);

        assumingLatIsDisabledHasGAIDv1(serverRequest, true);
        assumingLatIsDisabledHasAdIdFromAdIdsObjectV1(serverRequest, true);
        assumingLatIsDisabledHasGAIDv2(serverRequest, false);
        assumingLatIsDisabledHasAdIdFromAdIdsObjectV2(serverRequest, false);
    }

    @Test
    public void testCreditHistory_hasGAIDv1() throws Throwable {
        initBranchInstance(TEST_KEY);
        initQueue(getTestContext());

        Branch.getInstance().getCreditHistory(null);
        ServerRequest serverRequest = getLastEventOnQueue(getTestContext(), 1);

        Assert.assertNotNull(serverRequest);
        doFinalUpdate(serverRequest);

        assumingLatIsDisabledHasGAIDv1(serverRequest, true);
        assumingLatIsDisabledHasAdIdFromAdIdsObjectV1(serverRequest, true);
        assumingLatIsDisabledHasGAIDv2(serverRequest, false);
        assumingLatIsDisabledHasAdIdFromAdIdsObjectV2(serverRequest, false);
    }

    @Test
    public void testIdentity_hasGAIDv1() throws Throwable {
        initBranchInstance(TEST_KEY);
        initQueue(getTestContext());

        Branch.getInstance().setIdentity("Alex");
        ServerRequest serverRequest = getLastEventOnQueue(getTestContext(), 1);

        Assert.assertNotNull(serverRequest);
        doFinalUpdate(serverRequest);

        assumingLatIsDisabledHasGAIDv1(serverRequest, true);
        assumingLatIsDisabledHasAdIdFromAdIdsObjectV1(serverRequest, true);
        assumingLatIsDisabledHasGAIDv2(serverRequest, false);
        assumingLatIsDisabledHasAdIdFromAdIdsObjectV2(serverRequest, false);
    }

    @Test
    public void testLogout_hasGAIDv1() throws Throwable {
        // TODO: initSession needed first
    }

    @Test
    public void testPing_hasGAIDv1() throws Throwable {
        // TODO: Ping does not get enqueued
    }

    @Test
    public void testClose_hasGAIDv1() throws Throwable {
        // TODO: Close happens in a Session context
        // Note that closeSessionInternal cannot be run on a non-UI thread
    }

    @Test
    public void testStandardEvent_hasGAIDv2() throws Throwable {
        initBranchInstance(TEST_KEY);
        initQueue(getTestContext());

        BRANCH_STANDARD_EVENT eventType = BRANCH_STANDARD_EVENT.PURCHASE;
        BranchEvent branchEvent = new BranchEvent(eventType);

        ServerRequest serverRequest = logEvent(getTestContext(), branchEvent);
        Assert.assertNotNull(serverRequest);

        assumingLatIsDisabledHasGAIDv1(serverRequest, false);
        assumingLatIsDisabledHasAdIdFromAdIdsObjectV1(serverRequest, false);
        assumingLatIsDisabledHasGAIDv2(serverRequest, true);
        assumingLatIsDisabledHasAdIdFromAdIdsObjectV2(serverRequest, true);
    }

    // Check to see if the LAT is available (V1)
    private boolean hasV1LAT(ServerRequest request) {
        JSONObject jsonObject = request.getGetParams();
        int lat = jsonObject.optInt(Defines.Jsonkey.LATVal.getKey(), -1);
        return lat >= 0;
    }

    // Check to see if the LAT is available (V2)
    private boolean hasV2LAT(ServerRequest request) {
        JSONObject jsonObject = request.getGetParams();
        JSONObject userDataObj = jsonObject.optJSONObject(Defines.Jsonkey.UserData.getKey());

        if (userDataObj == null) {
            return false;
        }

        int lat = userDataObj.optInt(Defines.Jsonkey.LimitedAdTracking.getKey(), -1);
        return lat >= 0;
    }

    private boolean LATIsEnabledV1(ServerRequest request) {
        JSONObject jsonObject = request.getGetParams();
        return jsonObject.optInt(Defines.Jsonkey.LATVal.getKey(), -1) == 1;
    }

    private boolean LATIsEnabledV2(ServerRequest request) {
        JSONObject jsonObject = request.getGetParams();
        JSONObject userDataObj = jsonObject.optJSONObject(Defines.Jsonkey.UserData.getKey());

        Assert.assertNotNull(userDataObj);

        return userDataObj.optInt(Defines.Jsonkey.LimitedAdTracking.getKey(), -1) == 1;
    }

    // Check to see if the GAID is available (V1)
    private boolean hasV1GAID(ServerRequest request) {
        JSONObject jsonObject = request.getGetParams();
        String gaid = jsonObject.optString(Defines.Jsonkey.GoogleAdvertisingID.getKey());
        return (gaid.length() > 0);
    }

    // Check to see if the GAID is available (V2)
    private boolean hasV2GAID(ServerRequest request) {
        JSONObject jsonObject = request.getGetParams();
        JSONObject userDataObj = jsonObject.optJSONObject(Defines.Jsonkey.UserData.getKey());

        if (userDataObj == null) {
            return false;
        }

        String gaid = userDataObj.optString(Defines.Jsonkey.AAID.getKey());
        return (gaid.length() > 0);
    }

    private void assumingLatIsDisabledHasGAIDv1(ServerRequest serverRequest, boolean assertTrue) {
        if (assertTrue) {
            Assert.assertTrue(hasV1LAT(serverRequest));

            if (LATIsEnabledV1(serverRequest)) {
                Assert.assertFalse(hasV1GAID(serverRequest));
            } else {
                Assert.assertTrue(hasV1GAID(serverRequest));
            }
        } else {
            Assert.assertFalse(hasV1LAT(serverRequest));
            Assert.assertFalse(hasV1GAID(serverRequest));
        }
    }

    private void assumingLatIsDisabledHasGAIDv2(ServerRequest serverRequest, boolean assertTrue) {
        if (assertTrue) {
            Assert.assertTrue(hasV2LAT(serverRequest));

            if (LATIsEnabledV2(serverRequest)) {
                Assert.assertFalse(hasV2GAID(serverRequest));
            } else {
                Assert.assertTrue(hasV2GAID(serverRequest));
            }
        } else {
            Assert.assertFalse(hasV2LAT(serverRequest));
            Assert.assertFalse(hasV2GAID(serverRequest));
        }
    }

    private String getAdIdFromAdIdsObject(ServerRequest request) {
        JSONObject jsonObject = request.getGetParams();
        JSONObject adIdsObject = jsonObject.optJSONObject(Defines.Jsonkey.AdvertisingIDs.getKey());
        if (adIdsObject == null) return "";

        if (jsonObject.optString(Defines.Jsonkey.OS.getKey()).toLowerCase().contains("amazon")) {
            return adIdsObject.optString(Defines.Jsonkey.FireAdId.getKey());
        } else {
            return adIdsObject.optString(Defines.Jsonkey.AAID.getKey());
        }
    }

    private void assumingLatIsDisabledHasAdIdFromAdIdsObjectV1(ServerRequest serverRequest, boolean assertTrue) {
        boolean hasAdIdFromAdIdsObject = getAdIdFromAdIdsObject(serverRequest).length() > 0;
        if (assertTrue) {
            Assert.assertTrue(hasV1LAT(serverRequest));

            if (LATIsEnabledV1(serverRequest)) {
                Assert.assertFalse(hasAdIdFromAdIdsObject);
            } else {
                Assert.assertTrue(hasAdIdFromAdIdsObject);
            }
        } else {
            Assert.assertFalse(hasV1LAT(serverRequest));
        }
    }

    private void assumingLatIsDisabledHasAdIdFromAdIdsObjectV2(ServerRequest serverRequest, boolean assertTrue) {
        boolean hasAdIdFromAdIdsObject = getAdIdFromAdIdsObject(serverRequest).length() > 0;
        if (assertTrue) {
            Assert.assertTrue(hasV2LAT(serverRequest));

            if (LATIsEnabledV2(serverRequest)) {
                Assert.assertFalse(hasAdIdFromAdIdsObject);
            } else {
                Assert.assertTrue(hasAdIdFromAdIdsObject);
            }
        } else {
            Assert.assertFalse(hasV2LAT(serverRequest));
        }
    }
}

