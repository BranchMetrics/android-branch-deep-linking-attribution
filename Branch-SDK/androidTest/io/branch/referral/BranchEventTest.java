package io.branch.referral;

import android.support.test.runner.AndroidJUnit4;
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
        Branch.getInstance(getTestContext(), "key_live_testkey");

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

        Assert.assertTrue(event.logEvent(getTestContext()));
    }

    // Dig out the variable for isStandardEvent from the BranchEvent object.
    private boolean isStandardEvent(BranchEvent event) throws Throwable {
        // Use Reflection to find if it is considered a "Standard Event"
        Field f = event.getClass().getDeclaredField("isStandardEvent"); //NoSuchFieldException
        f.setAccessible(true);
        return (boolean) f.get(event); //IllegalAccessException
    }
}
