package io.branch.referral;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

public class ServerRequestGetLATD extends ServerRequest {

    private BranchLastAttributedTouchDataListenerWrapper callback;
    // defaultAttributionWindow is the "default" for the SDK's side, server interprets it as 30 days
    protected static final int defaultAttributionWindow = -1;
    private int attributionWindow;

    // needed to hotfix a bug
    private class BranchLastAttributedTouchDataListenerWrapper {
        io.branch.referral.util.BranchLastAttributedTouchData.BranchLastAttributedTouchDataListener oldCallbackRef;
        io.branch.referral.ServerRequestGetLATD.BranchLastAttributedTouchDataListener newCallbackRef;

        BranchLastAttributedTouchDataListenerWrapper(io.branch.referral.util.BranchLastAttributedTouchData.BranchLastAttributedTouchDataListener callback){
            oldCallbackRef = callback;
        }
        BranchLastAttributedTouchDataListenerWrapper(io.branch.referral.ServerRequestGetLATD.BranchLastAttributedTouchDataListener callback){
            newCallbackRef = callback;
        }

        void onDataFetched(JSONObject jsonObject, BranchError error) {
            if (newCallbackRef != null) {
                newCallbackRef.onDataFetched(jsonObject, error);
            } else if (oldCallbackRef != null) {
                oldCallbackRef.onDataFetched(jsonObject, error);
            } else {
                PrefHelper.Debug("Warning! Unexpected state in BranchLastAttributedTouchDataListenerWrapper.onDataFetched");
            }
        }
    }

    ServerRequestGetLATD(Context context, String requestPath, io.branch.referral.util.BranchLastAttributedTouchData.BranchLastAttributedTouchDataListener callback) {
        this(context, requestPath, callback, PrefHelper.getInstance(context).getLATDAttributionWindow());
    }

    ServerRequestGetLATD(Context context, String requestPath,
                         io.branch.referral.util.BranchLastAttributedTouchData.BranchLastAttributedTouchDataListener callback, int attributionWindow) {
        super(context, requestPath);
        this.callback = new BranchLastAttributedTouchDataListenerWrapper(callback);
        this.attributionWindow = attributionWindow;
        JSONObject reqBody = new JSONObject();
        try {
            setPost(reqBody);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        updateEnvironment(context, reqBody);
    }

    ServerRequestGetLATD(Context context, String requestPath, BranchLastAttributedTouchDataListener callback) {
        this(context, requestPath, callback, PrefHelper.getInstance(context).getLATDAttributionWindow());
    }

    ServerRequestGetLATD(Context context, String requestPath,
                         BranchLastAttributedTouchDataListener callback, int attributionWindow) {
        super(context, requestPath);
        this.callback = new BranchLastAttributedTouchDataListenerWrapper(callback);
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
        if (response != null) {
            if (callback != null) {
                callback.onDataFetched(response.getObject(), null);
            }
        } else {
            handleFailure(BranchError.ERR_BRANCH_INVALID_REQUEST, "Failed to get last attributed touch data");
        }
    }

    @Override
    public void handleFailure(int statusCode, String causeMsg) {
        if (callback != null) {
            callback.onDataFetched(null,
                    new BranchError("Failed to get last attributed touch data",
                            BranchError.ERR_BRANCH_INVALID_REQUEST));
        }
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
