package io.branch.referral;

import android.app.Application;
import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * * <p>
 * The server request for registering an app open event to Branch API. Handles request creation and execution.
 * </p>
 */
class ServerRequestRegisterOpen extends ServerRequestInitSession {
    
    /**
     * <p>Create an instance of {@link ServerRequestRegisterInstall} to notify Branch API on app open event.</p>
     *
     * @param context     Current {@link Application} context
     * @param callback    A {@link Branch.BranchReferralInitListener} callback instance that will return
     *                    the data associated with new install registration.
     */
    ServerRequestRegisterOpen(Context context, Branch.BranchReferralInitListener callback, boolean isAutoInitialization) {
        super(context, Defines.RequestPath.RegisterOpen, isAutoInitialization);
        callback_ = callback;
        JSONObject openPost = new JSONObject();
        try {
            openPost.put(Defines.Jsonkey.RandomizedDeviceToken.getKey(), prefHelper_.getRandomizedDeviceToken());
            openPost.put(Defines.Jsonkey.RandomizedBundleToken.getKey(), prefHelper_.getRandomizedBundleToken());
            setPost(openPost);
        } catch (JSONException ex) {
            BranchLogger.w("Caught JSONException " + ex.getMessage());
            constructError_ = true;
        }
        
    }
    
    ServerRequestRegisterOpen(Defines.RequestPath requestPath, JSONObject post, Context context, boolean isAutoInitialization) {
        super(requestPath, post, context, isAutoInitialization);
    }

    @Override
    public void onPreExecute() {
        super.onPreExecute();
        // Instant Deep Link if possible. This can happen when activity initializing the session is
        // already on stack, in which case we delay parsing out data and invoking the callback until
        // onResume to ensure that we have the latest intent data.
        if (Branch.getInstance().isInstantDeepLinkPossible()) {
            if (callback_ != null) {
                callback_.onInitFinished(Branch.getInstance().getLatestReferringParams(), null);
            }
            Branch.getInstance().requestQueue_.addExtraInstrumentationData(Defines.Jsonkey.InstantDeepLinkSession.getKey(), "true");
            Branch.getInstance().setInstantDeepLinkPossible(false);
        }
    }

    @Override
    public void onRequestSucceeded(ServerResponse resp, Branch branch) {
        super.onRequestSucceeded(resp, branch);
        BranchLogger.v("onRequestSucceeded " + this + " " + resp + " on callback " + callback_);
        try {
            if (resp.getObject().has(Defines.Jsonkey.LinkClickID.getKey())) {
                prefHelper_.setLinkClickID(resp.getObject().getString(Defines.Jsonkey.LinkClickID.getKey()));
            } else {
                prefHelper_.setLinkClickID(PrefHelper.NO_STRING_VALUE);
            }
            
            if (resp.getObject().has(Defines.Jsonkey.Data.getKey())) {
                String params = resp.getObject().getString(Defines.Jsonkey.Data.getKey());
                prefHelper_.setSessionParams(params);
            } else {
                prefHelper_.setSessionParams(PrefHelper.NO_STRING_VALUE);
            }

            if (callback_ != null && !Branch.getInstance().isIDLSession()) {
                callback_.onInitFinished(branch.getLatestReferringParams(), null);
            }
            
            prefHelper_.setAppVersion(DeviceInfo.getInstance().getAppVersion());
            
        } catch (Exception ex) {
            BranchLogger.w("Caught Exception ServerRequestRegisterOpen onRequestSucceeded: " + ex.getMessage());
        }
        onInitSessionCompleted(resp, branch);
    }
    
    @Override
    public void handleFailure(int statusCode, String causeMsg) {
        if (callback_ != null && !Branch.getInstance().isIDLSession()) {
            JSONObject obj = new JSONObject();
            try {
                obj.put("error_message", "Trouble reaching server. Please try again in a few minutes");
            } catch (JSONException ex) {
                BranchLogger.w("Caught JSONException " + ex.getMessage());
            }
            callback_.onInitFinished(obj, new BranchError("Trouble initializing Branch. " + this + " failed. " + causeMsg, statusCode));
        }
    }
    
    @Override
    public boolean handleErrors(Context context) {
        if (!super.doesAppHasInternetPermission(context)) {
            if (callback_ != null && !Branch.getInstance().isIDLSession()) {
                callback_.onInitFinished(null, new BranchError("Trouble initializing Branch.", BranchError.ERR_NO_INTERNET_PERMISSION));
            }
            return true;
        }
        return false;
    }
    
    @Override
    public boolean isGetRequest() {
        return false;
    }
    
    @Override
    public void clearCallbacks() {
        BranchLogger.v(this + " clearCallbacks " + callback_);
        callback_ = null;
    }
    
    @Override
    public String getRequestActionName() {
        return ACTION_OPEN;
    }

    @Override
    public boolean shouldRetryOnFail() {
        return false;
    }
}
