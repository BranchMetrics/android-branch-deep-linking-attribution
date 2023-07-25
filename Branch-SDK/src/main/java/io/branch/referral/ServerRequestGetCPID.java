package io.branch.referral;

import android.content.Context;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import io.branch.referral.util.BranchCPID;

public class ServerRequestGetCPID extends ServerRequest {

    private BranchCrossPlatformIdListener callback;

    ServerRequestGetCPID(Context context, BranchCrossPlatformIdListener callback) {
        super(context, Defines.RequestPath.GetCPID);
        this.callback = callback;
        JSONObject reqBody = new JSONObject();
        try {
            setPost(reqBody);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        updateEnvironment(context, reqBody);
    }

    @Override
    public boolean handleErrors(Context context) {
        return false;
    }

    @Override
    public void onRequestSucceeded(ServerResponse response, Branch branch) {
        if (callback == null) {
            return;
        }

        if (response != null) {
            callback.onDataFetched(new BranchCPID(response.getObject()), null);
        } else {
            callback.onDataFetched(null,
                    new BranchError("Failed to get the Cross Platform IDs",
                            BranchError.ERR_BRANCH_INVALID_REQUEST));
        }
    }

    @Override
    public void handleFailure(int statusCode, String causeMsg) {
        if (callback == null) {
            return;
        }

        callback.onDataFetched(null, new BranchError("Failed to get the Cross Platform IDs", statusCode));
    }

    @Override
    public boolean isGetRequest() {
        return false;
    }

    @Override
    public void clearCallbacks() {
        callback = null;
    }

    @Override
    public BRANCH_API_VERSION getBranchRemoteAPIVersion() {
        return BRANCH_API_VERSION.V1_CPID;
    }

    @Override
    protected boolean shouldUpdateLimitFacebookTracking() {
        return true;
    }

    public interface BranchCrossPlatformIdListener {
        void onDataFetched(BranchCPID branchCPID, BranchError error);
    }
}