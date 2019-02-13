package io.branch.referral;

import android.support.test.runner.AndroidJUnit4;

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
 [X] ServerRequestRActionCompleted.java         testCommerceEvent()
 [-] ServerRequestRedeemRewards.java            testRedeemRewards()
 [-] ServerRequestRegisterClose.java            testClose()
 [ ] ServerRequestRegisterInstall.java
 [ ] ServerRequestRegisterOpen.java
 [ ] ServerRequestRegisterView.java
 */

@RunWith(AndroidJUnit4.class)
public class BranchGAIDTest extends BranchEventTest {
    private static final String TAG = "Branch::GAIDTest";
    private static final String TEST_KEY = "key_live_testkey";

    @Test
    public void testInitSession_hasGAIDv1() throws Throwable {
        Branch.getInstance(getTestContext());
        initQueue(getTestContext());

        ServerRequestQueue queue = ServerRequestQueue.getInstance(getTestContext());
        Assert.assertEquals(1, queue.getSize());

        ServerRequest initRequest = queue.peekAt(0);
        doFinalUpdate(initRequest);

        Assert.assertTrue(hasV1GAID(initRequest));
        Assert.assertFalse(hasV2GAID(initRequest));
    }

    @Test
    public void testActionCompleted_hasGAIDv1() throws Throwable {
        Branch.getInstance(getTestContext());
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

        Assert.assertTrue(hasV1GAID(serverRequest));
        Assert.assertFalse(hasV2GAID(serverRequest));
    }

    @Test
    public void testCommerceEvent_hasGAIDv1() throws Throwable {
        Branch.getInstance(getTestContext(), TEST_KEY);
        initQueue(getTestContext());

        CommerceEvent commerceEvent = new CommerceEvent();
        commerceEvent.setTransactionID("123XYZ");
        commerceEvent.setRevenue(3.14);
        commerceEvent.setTax(.314);
        commerceEvent.setCoupon("MyCoupon");

        Branch.getInstance().sendCommerceEvent(commerceEvent);
        ServerRequest serverRequest = findEventOnQueue(getTestContext(), "event", "purchase");

        Assert.assertNotNull(serverRequest);
        doFinalUpdate(serverRequest);

        Assert.assertTrue(hasV1GAID(serverRequest));
        Assert.assertFalse(hasV2GAID(serverRequest));

        DebugLogQueue(getTestContext());
    }

    @Test
    public void testLoadRewards_hasGAIDv1() throws Throwable {
        // TODO:  loadRewards() puts an empty JSON object on the queue
    }

    @Test
    public void testRedeemAwards_hasGAIDv1() throws Throwable {
        // TODO: redeemRewards() does not have GAID

        Branch.getInstance(getTestContext(), TEST_KEY);
        initQueue(getTestContext());

        // Backdoor to set credits before we try to redeem them.
        PrefHelper prefHelper = PrefHelper.getInstance(getTestContext());
        prefHelper.setCreditCount(100);

        Branch.getInstance().redeemRewards(100);
        ServerRequest serverRequest = getLastEventOnQueue(getTestContext(), 2);

        Assert.assertNotNull(serverRequest);
        doFinalUpdate(serverRequest);

        Assert.assertFalse(hasV1GAID(serverRequest));
        Assert.assertFalse(hasV2GAID(serverRequest));
    }

    @Test
    public void testCreditHistory_hasGAIDv1() throws Throwable {
        // TODO: getCreditHistory() does not have GAID

        Branch.getInstance(getTestContext(), TEST_KEY);
        initQueue(getTestContext());

        Branch.getInstance().getCreditHistory(null);
        ServerRequest serverRequest = getLastEventOnQueue(getTestContext(), 2);

        Assert.assertNotNull(serverRequest);
        doFinalUpdate(serverRequest);

        Assert.assertFalse(hasV1GAID(serverRequest));
        Assert.assertFalse(hasV2GAID(serverRequest));
    }

    @Test
    public void testIdentity_hasGAIDv1() throws Throwable {
        // TODO: setIdentity() does not have GAID

        Branch.getInstance(getTestContext(), TEST_KEY);
        initQueue(getTestContext());

        Branch.getInstance().setIdentity("Alex");
        ServerRequest serverRequest = getLastEventOnQueue(getTestContext(), 2);

        Assert.assertNotNull(serverRequest);
        doFinalUpdate(serverRequest);

        Assert.assertFalse(hasV1GAID(serverRequest));
        Assert.assertFalse(hasV2GAID(serverRequest));
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
        Branch.getInstance(getTestContext(), TEST_KEY);
        initQueue(getTestContext());

        BRANCH_STANDARD_EVENT eventType = BRANCH_STANDARD_EVENT.PURCHASE;
        BranchEvent branchEvent = new BranchEvent(eventType);

        ServerRequest serverRequest = logEvent(getTestContext(), branchEvent);
        Assert.assertNotNull(serverRequest);

        Assert.assertFalse(hasV1GAID(serverRequest));
        Assert.assertTrue(hasV2GAID(serverRequest));
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

}

