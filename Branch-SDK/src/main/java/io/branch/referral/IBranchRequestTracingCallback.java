package io.branch.referral;

import org.json.JSONObject;

public interface IBranchRequestTracingCallback {
    void onRequestCompleted(String localRequestId, JSONObject request, JSONObject response, String error);
}
