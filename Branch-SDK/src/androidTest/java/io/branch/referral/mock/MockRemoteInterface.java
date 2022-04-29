package io.branch.referral.mock;

import org.json.JSONObject;

import java.util.UUID;

import io.branch.referral.Branch;
import io.branch.referral.BranchTest;
import io.branch.referral.PrefHelper;
import io.branch.referral.network.BranchRemoteInterface;

import static io.branch.referral.Defines.RequestPath.GetCPID;
import static io.branch.referral.Defines.RequestPath.GetCreditHistory;
import static io.branch.referral.Defines.RequestPath.GetCredits;
import static io.branch.referral.Defines.RequestPath.GetURL;
import static io.branch.referral.Defines.RequestPath.IdentifyUser;
import static io.branch.referral.Defines.RequestPath.RegisterInstall;
import static io.branch.referral.Defines.RequestPath.RegisterOpen;

public class MockRemoteInterface extends BranchRemoteInterface {
    private final static String TAG = "MockRemoteInterface";

    // TODO: Revisit with MockWebServer and mock out different response codes
    // since most tests use TEST_TIMEOUT to await network requests, lower it here, so TEST_TIMEOUT
    // ends up including a little bit of a buffer for scheduling network requests.
    private final long networkRequestDuration = BranchTest.TEST_REQUEST_TIMEOUT / 2;

    @Override
    public BranchResponse doRestfulGet(String url) throws BranchRemoteException {
        try {
            Thread.sleep(networkRequestDuration);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        PrefHelper.Debug(TAG + ", doRestfulGet, url: " + url);
        return new BranchResponse(pathForSuccessResponse(url), 200);
    }

    @Override
    public BranchResponse doRestfulPost(String url, JSONObject payload) throws BranchRemoteException {
        try {
            Thread.sleep(networkRequestDuration);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        PrefHelper.Debug(TAG + ", doRestfulPost, url: " + url + ", payload: " + payload);
        return new BranchResponse(pathForSuccessResponse(url), 200);
    }

    public static String pathForSuccessResponse(String url) {
        if (url.contains(GetURL.getPath())) {
            return "{\"url\":\"https://bnc.lt/l/randomized_test_route_" + UUID.randomUUID().toString() + "\"}";
        } else if (url.contains(IdentifyUser.getPath())) {
            return "{\"session_id\":\"880938553235373649\",\"randomized_bundle_token\":\"880938553226608667\",\"link\":\"https://branchster.test-app.link?%24randomized_bundle_token=880938553226608667\",\"data\":\"{\\\"+clicked_branch_link\\\":false,\\\"+is_first_session\\\":false}\",\"randomized_device_token\":\"867130134518497054\"}";
        } else if (url.contains(RegisterInstall.getPath()) || url.contains(RegisterOpen.getPath())) {
            return "{\"session_id\":\"880938553235373649\",\"randomized_bundle_token\":\"880938553226608667\",\"link\":\"https://branchster.test-app.link?%24randomized_bundle_token=880938553226608667\",\"data\":\"{\\\"+clicked_branch_link\\\":false,\\\"+is_first_session\\\":false}\",\"randomized_device_token\":\"867130134518497054\"}";
        } else if (url.contains(GetCPID.getPath())) {
            return "{\"user_data\":{\"cross_platform_id\":\"afb3e7f98b18dc6c90ebaeade4dbc6cac67fbb8e3f34e9cd8217490bf8f24b1f\",\"past_cross_platform_ids\":[],\"prob_cross_platform_ids\":[],\"developer_identity\":\"880938553226608667\"}}";
        } else {
            return "{}";
        }
    }
}