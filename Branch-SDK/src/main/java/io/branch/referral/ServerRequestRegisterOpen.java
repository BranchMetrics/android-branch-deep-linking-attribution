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
            ex.printStackTrace();
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
            Branch.getInstance().setInstantDeepLinkPossible(false);
        }
    }

    @Override
    public void onRequestSucceeded(ServerResponse resp, Branch branch) {
        super.onRequestSucceeded(resp, branch);
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

            if (callback_ != null) {
                callback_.onInitFinished(branch.getLatestReferringParams(), null);
            }
            
            prefHelper_.setAppVersion(DeviceInfo.getInstance().getAppVersion());
            
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        onInitSessionCompleted(resp, branch);
    }
    
    @Override
    public void handleFailure(int statusCode, String causeMsg) {
        if (callback_ != null) {
            JSONObject obj = new JSONObject();
            try {
                obj.put("error_message", "Trouble reaching server. Please try again in a few minutes");
            } catch (JSONException ex) {
                ex.printStackTrace();
            }
            callback_.onInitFinished(obj, new BranchError("Trouble initializing Branch. " + causeMsg, statusCode));
        }
    }
    
    @Override
    public boolean handleErrors(Context context) {
        if (!super.doesAppHasInternetPermission(context)) {
            if (callback_ != null) {
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
        callback_ = null;
    }
    
    @Override
    public String getRequestActionName() {
        return ACTION_OPEN;
    }

    @Override
    public boolean shouldRetryOnFail() {
        return true;   //Open request needs to retry on failure.
    }
}
