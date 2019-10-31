package io.branch.referral.util;

import android.content.Context;
import android.text.TextUtils;
import io.branch.referral.Branch;
import io.branch.referral.BranchError;
import io.branch.referral.Defines.RequestPath;
import io.branch.referral.ServerRequest;
import io.branch.referral.ServerResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by --vbajpai on --2019-08-30 at --13:54 for --android-branch-deep-linking-attribution
 */
public class BranchCrossPlatformId {

    private BranchCrossPlatformIdListener callback;

    public BranchCrossPlatformId(BranchCrossPlatformIdListener callback, Context context) {
        this.callback = callback;
        if (context != null) {
            getUserCrossPlatformIds(context);
        }
    }

    private void getUserCrossPlatformIds(Context context) {
        String reqPath = RequestPath.GetCPID.getPath();
        if (Branch.getInstance() != null) {
            Branch.getInstance().handleNewRequest(new ServerRequestGetCPID(context, reqPath));
        }
    }

    private class ServerRequestGetCPID extends ServerRequest {

        ServerRequestGetCPID(Context context, String requestPath) {
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
                    callback.onDataFetched(new BranchCPID(response.getObject()), null);
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
            return BRANCH_API_VERSION.V1_CPID;
        }

        @Override
        protected boolean shouldUpdateLimitFacebookTracking() {
            return true;
        }

        public boolean shouldRetryOnFail() {
            return true; // Branch event need to be retried on failure.
        }
    }

    public interface BranchCrossPlatformIdListener {

        void onDataFetched(BranchCPID branchCPID, BranchError error);
    }

    public class BranchCPID {

        JSONObject cpidData;

        public BranchCPID() {
        }

        public BranchCPID(JSONObject cpidData) {
            this.cpidData = cpidData;
        }

        public String getCrossPlatformID() {
            if (cpidData != null && cpidData.length() != 0) {
                try {
                    return cpidData.getJSONObject("user_data").getString("cross_platform_id");
                } catch (JSONException e) {
                    return null;
                }
            }
            return null;
        }

        public JSONArray getPastCrossPlatformIds() {
            if (cpidData != null && cpidData.length() != 0) {
                try {
                    return cpidData.getJSONObject("user_data")
                            .getJSONArray("past_cross_platform_ids");
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
            return null;
        }

        public JSONArray getProbabilisticCrossPlatformIds() {
            if (cpidData != null && cpidData.length() != 0) {
                try {
                    JSONArray probCPIDs = cpidData.getJSONObject("user_data")
                            .getJSONArray("prob_cross_platform_ids");

                    JSONArray finalCPIDsArray = new JSONArray();
                    for (int i = 0, size = probCPIDs.length(); i < size; i++) {
                        finalCPIDsArray.put(new ProbabilisticCPID(probCPIDs.getString(i),
                                probCPIDs.getDouble(i)));
                    }
                    return finalCPIDsArray;
                } catch (JSONException e) {
                    return null;
                }

            }
            return null;
        }

        public String getDeveloperIdentity() {
            if (cpidData != null && cpidData.length() != 0) {
                try {
                    return cpidData.getJSONObject("user_data").getString("developer_identity");
                } catch (JSONException e) {
                    return null;
                }
            }
            return null;
        }

        public class ProbabilisticCPID {

            public String id;

            public Double probability;

            public ProbabilisticCPID(String id, Double probability) {
                this.id = id;
                this.probability = probability;
            }

            public String getCPID() {
                if (!TextUtils.isEmpty(id)) {
                    return id;
                }
                return null;
            }

            public Double getCPIDProbablity() {
                if (probability != null) {
                    return probability;
                }
                return null;
            }
        }

    }
}
