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
    
    Branch.BranchReferralInitListener callback_;
    
    /**
     * <p>Create an instance of {@link ServerRequestRegisterInstall} to notify Branch API on app open event.</p>
     *
     * @param context     Current {@link Application} context
     * @param callback    A {@link Branch.BranchReferralInitListener} callback instance that will return
     *                    the data associated with new install registration.
     * @param sysObserver {@link SystemObserver} instance.
     */
    ServerRequestRegisterOpen(Context context, Branch.BranchReferralInitListener callback,
                              SystemObserver sysObserver) {
        super(context, Defines.RequestPath.RegisterOpen.getPath(), sysObserver);
        callback_ = callback;
        JSONObject openPost = new JSONObject();
        try {
            openPost.put(Defines.Jsonkey.DeviceFingerprintID.getKey(), prefHelper_.getDeviceFingerPrintID());
            openPost.put(Defines.Jsonkey.IdentityID.getKey(), prefHelper_.getIdentityID());
            setPost(openPost);
        } catch (JSONException ex) {
            ex.printStackTrace();
            constructError_ = true;
        }
        
    }
    
    ServerRequestRegisterOpen(String requestPath, JSONObject post, Context context) {
        super(requestPath, post, context);
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
                JSONObject dataObj = new JSONObject(resp.getObject().getString(Defines.Jsonkey.Data.getKey()));
                // If Clicked on a branch link
                if (dataObj.has(Defines.Jsonkey.Clicked_Branch_Link.getKey())
                        && dataObj.getBoolean(Defines.Jsonkey.Clicked_Branch_Link.getKey())) {
                    
                    // Check if there is any install params. Install param will be empty on until click a branch link
                    // or When a user logout
                    if (prefHelper_.getInstallParams().equals(PrefHelper.NO_STRING_VALUE)) {
                        // if clicked on link then check for is Referrable state
                        if (prefHelper_.getIsReferrable() == 1) {
                            String params = resp.getObject().getString(Defines.Jsonkey.Data.getKey());
                            prefHelper_.setInstallParams(params);
                        }
                    }
                }
            }
            
            if (resp.getObject().has(Defines.Jsonkey.Data.getKey())) {
                String params = resp.getObject().getString(Defines.Jsonkey.Data.getKey());
                prefHelper_.setSessionParams(params);
            } else {
                prefHelper_.setSessionParams(PrefHelper.NO_STRING_VALUE);
            }
            
            if (callback_ != null && !branch.isInitReportedThroughCallBack) {
                callback_.onInitFinished(branch.getLatestReferringParams(), null);
            }
            
            prefHelper_.setAppVersion(systemObserver_.getAppVersion());
            
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        onInitSessionCompleted(resp, branch);
    }
    
    void setInitFinishedCallback(Branch.BranchReferralInitListener callback) {
        if (callback != null) {      // Update callback if set with valid callback instance.
            callback_ = callback;
        }
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
    public boolean hasCallBack() {
        return callback_ != null;
    }
    
    @Override
    public String getRequestActionName() {
        return ACTION_OPEN;
    }
    
}
