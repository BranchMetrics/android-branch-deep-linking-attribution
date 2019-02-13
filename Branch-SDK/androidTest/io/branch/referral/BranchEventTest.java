package io.branch.referral;

import android.content.Context;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;

import io.branch.referral.util.BRANCH_STANDARD_EVENT;
import io.branch.referral.util.BranchEvent;
import io.branch.referral.util.CurrencyType;

/**
 * BranchEvent class tests.
 */
@RunWith(AndroidJUnit4.class)
public class BranchEventTest extends BranchTest {
    private static final String TAG = "Branch::EventTest";
    private static final String TEST_KEY = "key_live_testkey";

    @Test
    public void testStandardEvent() throws Throwable {
        BRANCH_STANDARD_EVENT eventType = BRANCH_STANDARD_EVENT.PURCHASE;
        Assert.assertEquals("PURCHASE", eventType.getName());

        BranchEvent branchEvent = new BranchEvent(eventType);
        Assert.assertTrue(isStandardEvent(branchEvent));
    }

    @Test
    public void testCustomEvent() throws Throwable {
        BranchEvent branchEvent = new BranchEvent("CustomEvent");
        Assert.assertFalse(isStandardEvent(branchEvent));
    }

    @Test
    public void testCustomEventWithStandardName() throws Throwable {
        // We assert that creating an event using a String *will be considered a custom event*
        BRANCH_STANDARD_EVENT eventType = BRANCH_STANDARD_EVENT.PURCHASE;
        BranchEvent branchEvent = new BranchEvent(eventType.getName());
        Assert.assertTrue(isStandardEvent(branchEvent));
    }

    @Test
    public void testAllStandardEvents() throws Throwable {
        for (BRANCH_STANDARD_EVENT eventType : BRANCH_STANDARD_EVENT.values()) {
            BranchEvent branchEvent = new BranchEvent(eventType);
            Assert.assertTrue(isStandardEvent(branchEvent));

            // We assert that creating an event using a String *will be considered a standard event*
            String eventName = eventType.getName();
            BranchEvent branchEventStr = new BranchEvent(eventName);
            Assert.assertTrue(isStandardEvent(branchEventStr));
        }
    }

    @Test
    public void addAllEventExtras() {
        BranchEvent event = new BranchEvent("CustomEvent");

        event.setTransactionID("123");
        event.setAffiliation("CustomAffiliation");
        event.setCoupon("test coupon");
        event.setCurrency(CurrencyType.BZD);
        event.setDescription("Test Event");
        event.setRevenue(123.456);
        event.setSearchQuery("Love");
        event.setShipping(0.001);
        event.setTax(10);

        event.addCustomDataProperty("test", "test value");
    }

    @Test
    public void testLogEvent() throws Throwable {
        Branch.getInstance(getTestContext(), TEST_KEY);

        BRANCH_STANDARD_EVENT eventType = BRANCH_STANDARD_EVENT.PURCHASE;
        BranchEvent branchEvent = new BranchEvent(eventType);

        branchEvent.logEvent(getTestContext());

        // There is some async stuff going on.  Let's wait until that is complete.
        Thread.sleep(2000);

        // Verify that the server queue has two events
        // 1. Install Event
        // 2. This Event
        ServerRequestQueue queue = ServerRequestQueue.getInstance(getTestContext());
        Assert.assertEquals(2, queue.getSize());
    }

    @Test
    public void testLogEvent_queue() throws Throwable {
        Branch.getInstance(getTestContext(), TEST_KEY);
        initQueue(getTestContext());

        BRANCH_STANDARD_EVENT eventType = BRANCH_STANDARD_EVENT.PURCHASE;
        BranchEvent branchEvent = new BranchEvent(eventType);

        ServerRequest serverRequest = logEvent(getTestContext(), branchEvent);
        JSONObject jsonObject = serverRequest.getGetParams();

        Assert.assertEquals(BRANCH_STANDARD_EVENT.PURCHASE.getName(), jsonObject.optString("name"));
    }

//    // =============================================================================================
//    // ====== GAID TESTING =========================================================================
//    // =============================================================================================
//
//    /**
//     [X] ServerRequestActionCompleted.java          testActionCompleted()
//     [ ] ServerRequestCreateUrl.java
//     [-] ServerRequestGetRewardHistory.java         testCreditHistory()
//     [-] ServerRequestGetRewards.java               testLoadRewards()
//     [-] ServerRequestIdentifyUserRequest.java      testIdentity()
//     [X] ServerRequestInitSession.java              testInitSession()
//     [-] ServerRequestLogout.java                   testLogout()
//     [-] ServerRequestPing.java                     testPing()
//     [X] ServerRequestRActionCompleted.java         testCommerceEvent()
//     [-] ServerRequestRedeemRewards.java            testRedeemRewards()
//     [-] ServerRequestRegisterClose.java            testClose()
//     [ ] ServerRequestRegisterInstall.java
//     [ ] ServerRequestRegisterOpen.java
//     [ ] ServerRequestRegisterView.java
//     */
//
//    @Test
//    public void testInitSession_hasGAIDv1() throws Throwable {
//        Branch.getInstance(getTestContext());
//        initQueue(getTestContext());
//
//        ServerRequestQueue queue = ServerRequestQueue.getInstance(getTestContext());
//        Assert.assertEquals(1, queue.getSize());
//
//        ServerRequest initRequest = queue.peekAt(0);
//        doFinalUpdate(initRequest);
//
//        Assert.assertTrue(hasV1GAID(initRequest));
//        Assert.assertFalse(hasV2GAID(initRequest));
//    }
//
//    @Test
//    public void testActionCompleted_hasGAIDv1() throws Throwable {
//        Branch.getInstance(getTestContext());
//        initQueue(getTestContext());
//
//        JSONObject params = new JSONObject();
//        params.put("name", "Alex");
//        params.put("boolean", true);
//        params.put("int", 1);
//        params.put("double", 0.13415512301);
//
//        // final CountDownLatch latch = new CountDownLatch(1);
//        Branch.getInstance().userCompletedAction("buy", params);
//
//        ServerRequest serverRequest = findEventOnQueue(getTestContext(), "event", "buy");
//
//        Assert.assertNotNull(serverRequest);
//        doFinalUpdate(serverRequest);
//
//        Assert.assertTrue(hasV1GAID(serverRequest));
//        Assert.assertFalse(hasV2GAID(serverRequest));
//    }
//
//    @Test
//    public void testCommerceEvent_hasGAIDv1() throws Throwable {
//        Branch.getInstance(getTestContext(), TEST_KEY);
//        initQueue(getTestContext());
//
//        CommerceEvent commerceEvent = new CommerceEvent();
//        commerceEvent.setTransactionID("123XYZ");
//        commerceEvent.setRevenue(3.14);
//        commerceEvent.setTax(.314);
//        commerceEvent.setCoupon("MyCoupon");
//
//        Branch.getInstance().sendCommerceEvent(commerceEvent);
//        ServerRequest serverRequest = findEventOnQueue(getTestContext(), "event", "purchase");
//
//        Assert.assertNotNull(serverRequest);
//        doFinalUpdate(serverRequest);
//
//        Assert.assertTrue(hasV1GAID(serverRequest));
//        Assert.assertFalse(hasV2GAID(serverRequest));
//
//        DebugLogQueue(getTestContext());
//    }
//
//    @Test
//    public void testLoadRewards_hasGAIDv1() throws Throwable {
//        // TODO:  loadRewards() puts an empty JSON object on the queue
//    }
//
//    @Test
//    public void testRedeemAwards_hasGAIDv1() throws Throwable {
//        // TODO: redeemRewards() does not have GAID
//
//        Branch.getInstance(getTestContext(), TEST_KEY);
//        initQueue(getTestContext());
//
//        // Backdoor to set credits before we try to redeem them.
//        PrefHelper prefHelper = PrefHelper.getInstance(getTestContext());
//        prefHelper.setCreditCount(100);
//
//        Branch.getInstance().redeemRewards(100);
//        ServerRequest serverRequest = getLastEventOnQueue(getTestContext(), 2);
//
//        Assert.assertNotNull(serverRequest);
//        doFinalUpdate(serverRequest);
//
//        Assert.assertFalse(hasV1GAID(serverRequest));
//        Assert.assertFalse(hasV2GAID(serverRequest));
//    }
//
//    @Test
//    public void testCreditHistory_hasGAIDv1() throws Throwable {
//        // TODO: getCreditHistory() does not have GAID
//
//        Branch.getInstance(getTestContext(), TEST_KEY);
//        initQueue(getTestContext());
//
//        Branch.getInstance().getCreditHistory(null);
//        ServerRequest serverRequest = getLastEventOnQueue(getTestContext(), 2);
//
//        Assert.assertNotNull(serverRequest);
//        doFinalUpdate(serverRequest);
//
//        Assert.assertFalse(hasV1GAID(serverRequest));
//        Assert.assertFalse(hasV2GAID(serverRequest));
//    }
//
//    @Test
//    public void testIdentity_hasGAIDv1() throws Throwable {
//        // TODO: setIdentity() does not have GAID
//
//        Branch.getInstance(getTestContext(), TEST_KEY);
//        initQueue(getTestContext());
//
//        Branch.getInstance().setIdentity("Alex");
//        ServerRequest serverRequest = getLastEventOnQueue(getTestContext(), 2);
//
//        Assert.assertNotNull(serverRequest);
//        doFinalUpdate(serverRequest);
//
//        Assert.assertFalse(hasV1GAID(serverRequest));
//        Assert.assertFalse(hasV2GAID(serverRequest));
//    }
//
//    @Test
//    public void testLogout_hasGAIDv1() throws Throwable {
//        // TODO: initSession needed first
//    }
//
//    @Test
//    public void testPing_hasGAIDv1() throws Throwable {
//        // TODO: Ping does not get enqueued
//    }
//
//    @Test
//    public void testClose_hasGAIDv1() throws Throwable {
//        // TODO: Close happens in a Session context
//        // Note that closeSessionInternal cannot be run on a non-UI thread
//    }
//
//    @Test
//    public void testStandardEvent_hasGAIDv2() throws Throwable {
//        Branch.getInstance(getTestContext(), TEST_KEY);
//        initQueue(getTestContext());
//
//        BRANCH_STANDARD_EVENT eventType = BRANCH_STANDARD_EVENT.PURCHASE;
//        BranchEvent branchEvent = new BranchEvent(eventType);
//
//        ServerRequest serverRequest = logEvent(getTestContext(), branchEvent);
//        Assert.assertNotNull(serverRequest);
//
//        Assert.assertFalse(hasV1GAID(serverRequest));
//        Assert.assertTrue(hasV2GAID(serverRequest));
//    }
//
//
//
//
    // Dig out the variable for isStandardEvent from the BranchEvent object.
    private boolean isStandardEvent(BranchEvent event) throws Throwable {
        // Use Reflection to find if it is considered a "Standard Event"
        Field f = event.getClass().getDeclaredField("isStandardEvent"); //NoSuchFieldException
        f.setAccessible(true);
        return (boolean) f.get(event); //IllegalAccessException
    }

    // This is an attempt to initialize the queue by adding an event and waiting for it to appear.
    // Once it appears, we remove it.
    // Note that adding an event to the queue the first time generates an install event as a side effect.
    void initQueue(Context context) throws Throwable {
        final String EVENT_NAME = "XXXyyyXXX";
        Branch.getInstance(getTestContext(), TEST_KEY);
        ServerRequestQueue queue = ServerRequestQueue.getInstance(context);

        // Queue should be empty when we initialize
        Assert.assertEquals(0, queue.getSize());

        // Create a test event and add it to the queue
        BranchEvent testEvent = new BranchEvent(EVENT_NAME);
        Assert.assertNotNull(addEventToQueueAndWait(context, testEvent));

        // Remove the event from the queue.  It should be the last one.
        if (queue.getSize() > 0) {
            int index = queue.getSize() - 1;
            queue.removeAt(index);
        }

        // We expect that the install event is the only event still on the queue
        if (queue.getSize() > 1) {
            for (int i = 0; i < queue.getSize(); i++) {
                ServerRequest request = queue.peekAt(i);
                Log.d(TAG, "Request " + i + ": " + request.getGetParams().toString());
            }
        }
        Assert.assertEquals(1, queue.getSize());
    }

    ServerRequest addEventToQueueAndWait(Context context, BranchEvent event) throws Throwable {
        event.logEvent(context);
        return findEventOnQueue(context, "name", event.getEventName());
    }

    // Obtain the ServerRequest that is on the queue that matches the BranchEvent to be logged.
    ServerRequest logEvent(Context context, BranchEvent event) throws Throwable {
        ServerRequestQueue queue = ServerRequestQueue.getInstance(context);
        int queueSizeIn = queue.getSize();

        ServerRequest queuedEvent = addEventToQueueAndWait(context, event);
        Assert.assertNotNull(queuedEvent);

        int queueSizeOut = queue.getSize();
        Assert.assertEquals(queueSizeOut, (queueSizeIn + 1));

        return doFinalUpdate(queuedEvent);
    }

    ServerRequest findEventOnQueue(Context context, String key, String eventName) throws Throwable {
        ServerRequestQueue queue = ServerRequestQueue.getInstance(context);

        int wait_remaining = 2000;
        final int interval = 50;

        while (wait_remaining > 0) {
            Thread.sleep(interval);
            if (queue.getSize() > 0) {
                int index = queue.getSize() - 1;

                ServerRequest request = queue.peekAt(index);
                JSONObject jsonObject = request.getGetParams();

                String name = jsonObject.optString(key);
                if (name.equals(eventName)) {
                    // Found it.
                    return request;
                }
            }
            wait_remaining -= interval;
        }

        return null;
    }

    ServerRequest getLastEventOnQueue(Context context, int minimumQueueSize) {
        ServerRequestQueue queue = ServerRequestQueue.getInstance(context);

        int size = queue.getSize();
        if (size >= minimumQueueSize) {
            return queue.peekAt(size - 1);
        }

        return null;
    }

    // Black Magic here.   In order to really find out what is going to be in this request,
    // we need to pretend like we are updating it right before sending it out.  We obviously
    // don't *actually* want to send this, we just want to verify that parameters are fully set.
    ServerRequest doFinalUpdate(ServerRequest request) {
        request.doFinalUpdateOnBackgroundThread();
        return request;
    }

    void DebugLogQueue(Context context) {
        ServerRequestQueue queue = ServerRequestQueue.getInstance(context);

        for (int i = 0; i < queue.getSize(); i++) {
            ServerRequest request = queue.peekAt(i);
            doFinalUpdate(request);

            JSONObject jsonObject = request.getGetParams();
            Log.d(TAG, "QUEUE " + i + ": " + jsonObject.toString());
        }

    }
}

