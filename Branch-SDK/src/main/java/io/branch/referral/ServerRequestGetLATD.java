package io.branch.referral;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

public class ServerRequestGetLATD extends ServerRequest {

    private BranchLastAttributedTouchDataListener callback;
    // defaultAttributionWindow is the "default" for the SDK's side, server interprets it as 30 days
    protected static final int defaultAttributionWindow = -1;
    private int attributionWindow;

    ServerRequestGetLATD(Context context, Defines.RequestPath requestPath, BranchLastAttributedTouchDataListener callback) {
        this(context, requestPath, callback, PrefHelper.getInstance(context).getLATDAttributionWindow());
    }

    ServerRequestGetLATD(Context context, Defines.RequestPath requestPath,
                         BranchLastAttributedTouchDataListener callback, int attributionWindow) {
        super(context, requestPath);
        this.callback = callback;
        this.attributionWindow = attributionWindow;
        JSONObject reqBody = new JSONObject();
        try {
            setPost(reqBody);
        } catch (JSONException e) {
            e.printStackTrace();
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
        if (callback == null) {
            return;
        }

        if (response != null) {
            callback.onDataFetched(response.getObject(), null);
        } else {
            handleFailure(BranchError.ERR_BRANCH_INVALID_REQUEST, "Failed to get last attributed touch data");
        }
    }

    @Override
    public void handleFailure(int statusCode, String causeMsg) {
        if (callback != null) {
            callback.onDataFetched(null, new BranchError("Failed to get last attributed touch data", statusCode));
        }
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
        return BRANCH_API_VERSION.V1_LATD;
    }

    @Override
    protected boolean shouldUpdateLimitFacebookTracking() {
        return true;
    }

    public interface BranchLastAttributedTouchDataListener {
        void onDataFetched(JSONObject jsonObject, BranchError error);
    }
}
