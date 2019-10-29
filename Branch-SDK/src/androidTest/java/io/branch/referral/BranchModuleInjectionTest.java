package io.branch.referral;

import io.branch.referral.Defines.ModuleNameKeys;
import io.branch.referral.util.BRANCH_STANDARD_EVENT;
import io.branch.referral.util.CommerceEvent;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by --vbajpai on --2019-08-29 at --21:56 for --android-branch-deep-linking-attribution
 */
public class BranchModuleInjectionTest extends BranchEventTest {

    @Test
    public void testResultSuccess() throws Throwable {
        Branch branch = Branch.getInstance(getTestContext());
        JSONObject branchFileJson = new JSONObject("{\"imei\":\"1234567890\"}");
        branch.addModule(branchFileJson);

        initQueue(getTestContext());

        ServerRequestQueue queue = ServerRequestQueue.getInstance(getTestContext());
        Assert.assertEquals(1, queue.getSize());

        ServerRequest initRequest = queue.peekAt(0);
        doFinalUpdate(initRequest);
        doFinalUpdateOnMainThread(initRequest);

        Assert.assertTrue(hasV1InstallImeiData(initRequest));
    }

    @Test
    public void testNoModuleAddedWhenModuleNameMismatch() throws Throwable {
        Branch branch = Branch.getInstance(getTestContext());
        JSONObject branchFileJson = new JSONObject("{\"imei_rouge\":\"1234567890\"}");
        branch.addModule(branchFileJson);

        initQueue(getTestContext());

        ServerRequestQueue queue = ServerRequestQueue.getInstance(getTestContext());
        Assert.assertEquals(1, queue.getSize());

        ServerRequest initRequest = queue.peekAt(0);
        doFinalUpdate(initRequest);
        doFinalUpdateOnMainThread(initRequest);

        Assert.assertTrue(doesNotHaveV1InstallImeiData(initRequest));
    }

    @Test
    public void testCommerceEventHasImeiData() throws Throwable {
        Branch branch = Branch.getInstance(getTestContext());
        JSONObject branchFileJson = new JSONObject("{\"imei\":\"1234567890\"}");
        branch.addModule(branchFileJson);

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

        Assert.assertTrue(hasCommerceImeiData(serverRequest));
    }

    // Check to see if the module injected imei is in the install request
    private boolean hasV1InstallImeiData(ServerRequest request) {
        JSONObject jsonObject = request.getGetParams();
        String imeiValue = jsonObject.optString(ModuleNameKeys.imei.getKey());
        return (imeiValue.equals("1234567890"));
    }

    // Check for null imei
    private boolean doesNotHaveV1InstallImeiData(ServerRequest request) {
        JSONObject jsonObject = request.getGetParams();
        String imeiValue = jsonObject.optString(ModuleNameKeys.imei.getKey());
        return (imeiValue.length()==0);
    }

    // Check to see if the module injected imei is in the install request
    private boolean hasCommerceImeiData(ServerRequest request) {
        JSONObject jsonObject = request.getGetParams();
        String imeiValue = jsonObject.optString(ModuleNameKeys.imei.getKey());
        return (imeiValue.equals("1234567890"));
    }
}
