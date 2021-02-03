package io.branch.referral;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.branch.referral.util.AdType;
import io.branch.referral.util.BRANCH_STANDARD_EVENT;
import io.branch.referral.util.BranchEvent;
import io.branch.referral.util.CurrencyType;

@RunWith(AndroidJUnit4.class)
public class BranchEventTest extends BranchEventTestUtil {

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
    public void testLogEvent() throws InterruptedException {
        initBranchInstance(TEST_KEY);

        new BranchEvent(BRANCH_STANDARD_EVENT.PURCHASE).logEvent(getTestContext());
        final ServerRequestQueue queue = ServerRequestQueue.getInstance(getTestContext());
        Assert.assertEquals(1, queue.getSize());

        ServerRequest eventRequest = queue.peekAt(0);
        Assert.assertEquals(Defines.RequestPath.TrackStandardEvent.getPath(), eventRequest.getRequestPath());
        Assert.assertTrue(eventRequest.isWaitingOnProcessToFinish());

        initSessionResumeActivity(new Runnable() {
            @Override
            public void run() {
                Assert.assertEquals(2, queue.getSize());
            }
        });
    }

    @Test
    public void testLogEvent_queue() throws Throwable {
        initBranchInstance(TEST_KEY);

        ServerRequest serverRequest = logEvent(getTestContext(), new BranchEvent(BRANCH_STANDARD_EVENT.PURCHASE));
        JSONObject jsonObject = serverRequest.getGetParams();

        Assert.assertEquals(BRANCH_STANDARD_EVENT.PURCHASE.getName(), jsonObject.optString(Defines.Jsonkey.Name.getKey()));
    }

    @Test
    public void testAdType() throws Throwable {
        initBranchInstance(TEST_KEY);

        BranchEvent branchEvent = new BranchEvent(BRANCH_STANDARD_EVENT.VIEW_AD);
        branchEvent.setAdType(AdType.BANNER);

        ServerRequest serverRequest = logEvent(getTestContext(), branchEvent);
        JSONObject jsonObject = serverRequest.getGetParams();

        // Verify that the ad_type was set correctly.
        JSONObject eventData = jsonObject.getJSONObject(Defines.Jsonkey.EventData.getKey());
        String adType = eventData.getString(Defines.Jsonkey.AdType.getKey());

        Assert.assertEquals(adType, AdType.BANNER.getName());
    }
}
