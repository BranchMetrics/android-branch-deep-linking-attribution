package io.branch.referral;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

public class ServerRequestGetLATD extends ServerRequest {

    // defaultAttributionWindow is the "default" for the SDK's side, server interprets it as 30 days
    protected static final int defaultAttributionWindow = -1;
    private int attributionWindow;

    ServerRequestGetLATD(Context context, Defines.RequestPath requestPath) {
        this(context, requestPath, PrefHelper.getInstance(context).getLATDAttributionWindow());
    }

    ServerRequestGetLATD(Context context, Defines.RequestPath requestPath, int attributionWindow) {
        super(context, requestPath);
        this.attributionWindow = attributionWindow;
        JSONObject reqBody = new JSONObject();
        try {
            setPost(reqBody);
        } catch (JSONException e) {
            BranchLogger.w("Caught JSONException " + e.getMessage());
        }
        updateEnvironment(context, reqBody);
    }

    protected int getAttributionWindow() {
        return attributionWindow;
    }

    @Override
    public boolean handleErrors(Context context) {
        return false;
    }

    @Override
    public void onRequestSucceeded(ServerResponse response, Branch branch) {
        // Remove the callback logic as per the instructions
    }

    @Override
    public void handleFailure(int statusCode, String causeMsg) {
        // Remove the callback logic as per the instructions
    }

    @Override
    public boolean isGetRequest() {
        return false;
    }

    @Override
    public void clearCallbacks() {
        // Remove the callback logic as per the instructions
    }

    @Override
    public BRANCH_API_VERSION getBranchRemoteAPIVersion() {
        return BRANCH_API_VERSION.V1_LATD;
    }

    @Override
    protected boolean shouldUpdateLimitFacebookTracking() {
        return true;
    }
}
