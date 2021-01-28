package io.branch.referral;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import android.util.Log;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;

import io.branch.referral.util.AdType;
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
    public void testAddAllEventExtras() {
        BranchEvent event = new BranchEvent("CustomEvent");

        event.setTransactionID("123");
        for (AdType adType : AdType.values()) {
            event.setAdType(adType);
        }
        event.setAffiliation("CustomAffiliation");
        event.setCoupon("test coupon");
        event.setCurrency(CurrencyType.BZD);
        event.setDescription("Test Event");
        event.setRevenue(123.456);
        event.setSearchQuery("Love");
        event.setShipping(0.001);
        event.setTax(10);
        event.setCustomerEventAlias("potato_event");

        event.addCustomDataProperty("test", "test value");
    }

    @Test
    public void testLogEvent() {
        initBranchInstance(TEST_KEY);

        new BranchEvent(BRANCH_STANDARD_EVENT.PURCHASE).logEvent(getTestContext());
        ServerRequestQueue queue = ServerRequestQueue.getInstance(getTestContext());
        Assert.assertEquals(1, queue.getSize());

        ServerRequest eventRequest = queue.peekAt(0);
        Assert.assertEquals(Defines.RequestPath.TrackStandardEvent.getPath(), eventRequest.getRequestPath());
        Assert.assertTrue(eventRequest.isWaitingOnProcessToFinish());

        initTestSession();

        Assert.assertEquals(2, queue.getSize());
    }

    @Test
    public void testLogEvent_queue() throws Throwable {
        initBranchInstance(TEST_KEY);
        initQueue(getTestContext());

        ServerRequest serverRequest = logEvent(getTestContext(), new BranchEvent(BRANCH_STANDARD_EVENT.PURCHASE));
        JSONObject jsonObject = serverRequest.getGetParams();

        Assert.assertEquals(BRANCH_STANDARD_EVENT.PURCHASE.getName(), jsonObject.optString(Defines.Jsonkey.Name.getKey()));
    }

    @Test
    public void testAdType() throws Throwable {
        initBranchInstance(TEST_KEY);
        initQueue(getTestContext());

        BranchEvent branchEvent = new BranchEvent(BRANCH_STANDARD_EVENT.VIEW_AD);
        branchEvent.setAdType(AdType.BANNER);

        ServerRequest serverRequest = logEvent(getTestContext(), branchEvent);
        JSONObject jsonObject = serverRequest.getGetParams();

        // Verify that the ad_type was set correctly.
        JSONObject eventData = jsonObject.getJSONObject(Defines.Jsonkey.EventData.getKey());
        String adType = eventData.getString(Defines.Jsonkey.AdType.getKey());

        Assert.assertEquals(adType, AdType.BANNER.getName());
    }

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
        initBranchInstance(TEST_KEY);
        ServerRequestQueue queue = ServerRequestQueue.getInstance(context);

        // Queue should be empty when we initialize
        Assert.assertEquals(0, queue.getSize());

        // Create a test event and add it to the queue
        BranchEvent testEvent = new BranchEvent(EVENT_NAME);
        Assert.assertNotNull(addEventToQueueAndWait(context, testEvent));
        Assert.assertEquals(1, queue.getSize());

        // Remove the event from the queue.  It should be the last one.
        if (queue.getSize() > 0) {
            int index = queue.getSize() - 1;
            queue.removeAt(index);
        }

        Assert.assertEquals(0, queue.getSize());
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

    ServerRequest doFinalUpdateOnMainThread(ServerRequest request) {
        request.doFinalUpdateOnMainThread();
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

