package io.branch.referral.util;

import android.content.Context;
import io.branch.referral.Branch;
import io.branch.referral.BranchError;
import io.branch.referral.Defines.RequestPath;
import io.branch.referral.ServerRequest;
import io.branch.referral.ServerResponse;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by --vbajpai on --2019-09-14 at --18:44 for --android-branch-deep-linking-attribution
 */
public class BranchLastAttributedTouchData {

    private BranchLastAttributedTouchDataListener callback;

    public BranchLastAttributedTouchData(
            BranchLastAttributedTouchDataListener callback, Context context) {
        this.callback = callback;
        if (context != null) {
            getLastAttributedTouchData(context);
        }
    }

    private void getLastAttributedTouchData(Context context) {
        String reqPath = RequestPath.GetLATD.getPath();
        if (Branch.getInstance() != null) {
            Branch.getInstance().handleNewRequest(new ServerRequestGetLATD(context, reqPath));
        }
    }

    private class ServerRequestGetLATD extends ServerRequest {

        ServerRequestGetLATD(Context context, String requestPath) {
            super(context, requestPath);
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
    }

    public interface BranchLastAttributedTouchDataListener {

        void onDataFetched(JSONObject jsonObject, BranchError error);
    }


}
