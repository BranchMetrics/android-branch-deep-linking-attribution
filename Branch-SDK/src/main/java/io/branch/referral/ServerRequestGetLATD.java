package io.branch.referral;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

class ServerRequestGetLATD extends ServerRequest {

    private BranchLastAttributedTouchDataListener callback;

    ServerRequestGetLATD(Context context, String requestPath, BranchLastAttributedTouchDataListener callback) {
        super(context, requestPath);
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
        if (response != null) {
            if (callback != null) {
                callback.onDataFetched(response.getObject(), null);
            }
        } else {
            callback.onDataFetched(null,
                    new BranchError("Failed to get the Cross Platform IDs",
                            BranchError.ERR_BRANCH_INVALID_REQUEST));
        }
    }

    @Override
    public void handleFailure(int statusCode, String causeMsg) {
        callback.onDataFetched(null,
                new BranchError("Failed to get the Cross Platform IDs",
                        BranchError.ERR_BRANCH_INVALID_REQUEST));
    }

    @Override
    public boolean isGetRequest() {
        return false;
    }

    @Override
    public void clearCallbacks() {
    }

    @Override
    public BRANCH_API_VERSION getBranchRemoteAPIVersion() {
        return BRANCH_API_VERSION.V1_LATD;
    }

    @Override
    protected boolean shouldUpdateLimitFacebookTracking() {
        return true;
    }

    public boolean shouldRetryOnFail() {
        return true; // Branch event need to be retried on failure.
    }

    public interface BranchLastAttributedTouchDataListener {
        void onDataFetched(JSONObject jsonObject, BranchError error);
    }
}
