package io.branch.referral;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.json.JSONException;
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
 [-] ServerRequestIdentifyUserRequest.java      testIdentity()
 [X] ServerRequestInitSession.java              testInitSession()
 [-] ServerRequestLogout.java                   testLogout()
 [-] ServerRequestPing.java                     testPing()
 [X] ServerRequestActionCompleted.java          testCommerceEvent()
 [-] ServerRequestRegisterClose.java            testClose()
 [ ] ServerRequestRegisterInstall.java
 [ ] ServerRequestRegisterOpen.java
 */

@RunWith(AndroidJUnit4.class)
public class BranchGAIDTest extends BranchTest {
    private static final String TAG = "BranchGAIDTest";

    @Test
    public void testInitSession_hasGAIDv1() {
        initBranchInstance();
        final ServerRequestQueue queue = ServerRequestQueue.getInstance(getTestContext());
        initSessionResumeActivity(new Runnable() {
            @Override
            public void run() {
                Assert.assertEquals(1, queue.getSize());

                ServerRequest initRequest = queue.peekAt(0);
                doFinalUpdate(initRequest);

                assumingLatIsDisabledHasGAIDv1(initRequest, true);
                assumingLatIsDisabledHasAdIdFromAdIdsObjectV1(initRequest, true);
                assumingLatIsDisabledHasGAIDv2(initRequest, false);
                assumingLatIsDisabledHasAdIdFromAdIdsObjectV2(initRequest, false);
            }
        }, null);
    }

    @Test
    public void testActionCompleted_hasGAIDv1() {
        initBranchInstance();
        initSessionResumeActivity(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject params = new JSONObject();
                    params.put("name", "Alex");
                    params.put("boolean", true);
                    params.put("int", 1);
                    params.put("double", 0.13415512301);

                    // final CountDownLatch latch = new CountDownLatch(1);
                    Branch.getInstance().userCompletedAction("buy", params);

                    ServerRequest serverRequest = findRequestOnQueue(getTestContext(), "event", "buy");

                    Assert.assertNotNull(serverRequest);
                    doFinalUpdate(serverRequest);

                    assumingLatIsDisabledHasGAIDv1(serverRequest, true);
                    assumingLatIsDisabledHasAdIdFromAdIdsObjectV1(serverRequest, true);
                    assumingLatIsDisabledHasGAIDv2(serverRequest, false);
                    assumingLatIsDisabledHasAdIdFromAdIdsObjectV2(serverRequest, false);
                } catch (Exception e) {
                    Assert.fail();
                }
            }
        }, null);
    }

    @Test
    public void testCommerceEvent_hasGAIDv1() {
        initBranchInstance(TEST_KEY);
        initSessionResumeActivity(new Runnable() {
            @Override
            public void run() {
                try {
                    CommerceEvent commerceEvent = new CommerceEvent();
                    commerceEvent.setTransactionID("123XYZ");
                    commerceEvent.setRevenue(3.14);
                    commerceEvent.setTax(.314);
                    commerceEvent.setCoupon("MyCoupon");

                    branch.sendCommerceEvent(commerceEvent);
                    ServerRequest serverRequest = findRequestOnQueue(getTestContext(), "event", BRANCH_STANDARD_EVENT.PURCHASE.getName());

                    Assert.assertNotNull(serverRequest);
                    doFinalUpdate(serverRequest);

                    assumingLatIsDisabledHasGAIDv1(serverRequest, true);
                    assumingLatIsDisabledHasAdIdFromAdIdsObjectV1(serverRequest, true);
                    assumingLatIsDisabledHasGAIDv2(serverRequest, false);
                    assumingLatIsDisabledHasAdIdFromAdIdsObjectV2(serverRequest, false);
                } catch (Exception e) {
                    Assert.fail();
                }
            }
        }, null);
    }

    @Test
    public void testIdentity_hasGAIDv1() {
        initBranchInstance(TEST_KEY);
        initSessionResumeActivity(new Runnable() {
            @Override
            public void run() {
                try {
                    Branch.getInstance().setIdentity("Alex");
                    ServerRequest serverRequest = getLastRequestOnQueue(getTestContext(), 1);

                    Assert.assertNotNull(serverRequest);
                    doFinalUpdate(serverRequest);

                    assumingLatIsDisabledHasGAIDv1(serverRequest, true);
                    assumingLatIsDisabledHasAdIdFromAdIdsObjectV1(serverRequest, true);
                    assumingLatIsDisabledHasGAIDv2(serverRequest, false);
                    assumingLatIsDisabledHasAdIdFromAdIdsObjectV2(serverRequest, false);
                } catch (Exception e) {
                    Assert.fail();
                }
            }
        }, null);
    }

    @Test
    public void testLogout_hasGAIDv1() {
        // TODO: initSession needed first
    }

    @Test
    public void testPing_hasGAIDv1() {
        // TODO: Ping does not get enqueued
    }

    @Test
    public void testClose_hasGAIDv1() {
        // TODO: Close happens in a Session context
        // Note that closeSessionInternal cannot be run on a non-UI thread
    }

    @Test
    public void testStandardEvent_hasGAIDv2() {
        initBranchInstance(TEST_KEY);
        initSessionResumeActivity(new Runnable() {
            @Override
            public void run() {
                try {
                    BRANCH_STANDARD_EVENT eventType = BRANCH_STANDARD_EVENT.PURCHASE;
                    BranchEvent branchEvent = new BranchEvent(eventType);

                    ServerRequest serverRequest = logEvent(getTestContext(), branchEvent);
                    Assert.assertNotNull(serverRequest);

                    assumingLatIsDisabledHasGAIDv1(serverRequest, false);
                    assumingLatIsDisabledHasAdIdFromAdIdsObjectV1(serverRequest, false);
                    assumingLatIsDisabledHasGAIDv2(serverRequest, true);
                    assumingLatIsDisabledHasAdIdFromAdIdsObjectV2(serverRequest, true);
                } catch (Exception e) {
                    Assert.fail();
                }
            }
        }, null);
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

        if (getOSFromAdIdObject(jsonObject).contains("amazon")) {
            return adIdsObject.optString(Defines.Jsonkey.FireAdId.getKey());
        } else {
            return adIdsObject.optString(Defines.Jsonkey.AAID.getKey());
        }
    }

    private String getOSFromAdIdObject(JSONObject jsonObject) {
        if(jsonObject == null) return "";

        //check for user_data child object
        JSONObject userDataObject = jsonObject.optJSONObject(Defines.Jsonkey.UserData.getKey());
        if(userDataObject != null) {
            return userDataObject.optString(Defines.Jsonkey.OS.getKey()).toLowerCase();
        }

        //data is in root object
        return jsonObject.optString(Defines.Jsonkey.OS.getKey()).toLowerCase();
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

