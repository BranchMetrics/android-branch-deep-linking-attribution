package io.branch.referral;

import android.content.Context;
import android.support.test.runner.AndroidJUnit4;

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

    @Test
    public void addAllEventExtras() throws Throwable {
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

    // Dig out the variable for isStandardEvent from the BranchEvent object.
    private boolean isStandardEvent(BranchEvent event) throws Throwable {
        // Use Reflection to find if it is considered a "Standard Event"
        Field f = event.getClass().getDeclaredField("isStandardEvent"); //NoSuchFieldException
        f.setAccessible(true);
        return (boolean) f.get(event); //IllegalAccessException
    }

    // Mark if the queue has been initialized or not
    private boolean queueInitialized = false;

    // This is an attempt to initialize the queue by adding an event and waiting for it to appear.
    // Once it appears, we remove it.
    // Note that adding an event to the queue the first time generates an install event as a side effect.
    private void initQueue(Context context) throws Throwable {
        final String EVENT_NAME = "XXXyyyXXX";
        Branch.getInstance(getTestContext(), TEST_KEY);
        ServerRequestQueue queue = ServerRequestQueue.getInstance(context);

        // Create a test event and add it to the queue
        BranchEvent testEvent = new BranchEvent(EVENT_NAME);
        Assert.assertTrue(addEventToQueueAndWait(context, testEvent));

        // Remove the event from the queue.  It should be the last one.
        if (queue.getSize() > 0) {
            int index = queue.getSize() - 1;
            queue.removeAt(index);
        }

        // We expect that the install event is still on the queue
        Assert.assertEquals(1, queue.getSize());

        queueInitialized = true;
    }

    // Obtain the ServerRequest that is on the queue that matches the BranchEvent to be logged.
    private ServerRequest logEvent(Context context, BranchEvent event) throws Throwable {
        ServerRequestQueue queue = ServerRequestQueue.getInstance(context);
        int queueSizeIn = queue.getSize();

        Assert.assertTrue(addEventToQueueAndWait(context, event));

        int queueSizeOut = queue.getSize();

        Assert.assertEquals(queueSizeOut, (queueSizeIn + 1));

        ServerRequest queuedEvent = queue.peekAt(queueSizeOut - 1);
        Assert.assertNotNull(queuedEvent);

        // Black Magic here.   In order to really find out what is going to be in this request,
        // we need to pretend like we are updating it right before sending it out.  We obviously
        // don't *actually* want to send this, we just want to verify that parameters are fully set.
        queuedEvent.doFinalUpdateOnBackgroundThread();

        return queuedEvent;
    }

    private boolean addEventToQueueAndWait(Context context, BranchEvent event) throws Throwable {
        ServerRequestQueue queue = ServerRequestQueue.getInstance(context);
        event.logEvent(context);

        boolean found = false;
        int wait_remaining = 2000;
        final int interval = 50;

        while (wait_remaining > 0) {
            Thread.sleep(interval);
            if (queue.getSize() > 0) {
                int index = queue.getSize() - 1;

                ServerRequest test = queue.peekAt(index);
                JSONObject jsonObject = test.getGetParams();

                String eventName = jsonObject.optString("name");
                if (eventName.equals(event.getEventName())) {
                    // Found it.
                    found = true;
                    break;
                }
            }
            wait_remaining -= interval;
        }

        return found;
    }

}

