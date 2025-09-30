package io.branch.referral;

import org.json.JSONObject;

public interface IBranchRequestTracingCallback {
    void onRequestCompleted(String uri, JSONObject request, JSONObject response, String error, String requestUrl);
}
