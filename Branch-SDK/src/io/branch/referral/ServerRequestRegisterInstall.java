package io.branch.referral;

import android.app.Application;
import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * * <p>
 * The server request for registering an app install to Branch API. Handles request creation and execution.
 * </p>
 */
class ServerRequestRegisterInstall extends ServerRequestInitSession {

    Branch.BranchReferralInitListener callback_;
    SystemObserver systemObserver_;
    /**
     * <p>Create an instance of {@link ServerRequestRegisterInstall} to notify Branch API on a new install.</p>
     *
     * @param context     Current {@link Application} context
     * @param callback    A {@link Branch.BranchReferralInitListener} callback instance that will return
     *                    the data associated with new install registration.
     * @param sysObserver {@link SystemObserver} instance.
     * @param installID   installation ID.                                   .
     */
    public ServerRequestRegisterInstall(Context context, Branch.BranchReferralInitListener callback,
                                        SystemObserver sysObserver, String installID, String googleSearchInstallReferrerID) {

        super(context, Defines.RequestPath.RegisterInstall.getPath());
        systemObserver_ = sysObserver;
        callback_ = callback;
        JSONObject installPost = new JSONObject();
        try {
            if (!installID.equals(PrefHelper.NO_STRING_VALUE)) {
                installPost.put(Defines.Jsonkey.LinkClickID.getKey(), installID);
            }
            if (!googleSearchInstallReferrerID.equals(PrefHelper.NO_STRING_VALUE)) {
                installPost.put(Defines.Jsonkey.GoogleSearchInstallReferrer.getKey(), googleSearchInstallReferrerID);
            }
            if (!sysObserver.getAppVersion().equals(SystemObserver.BLANK)) {
                installPost.put(Defines.Jsonkey.AppVersion.getKey(), sysObserver.getAppVersion());
            }
            if (!prefHelper_.getInstallReferrerParams().equals(PrefHelper.NO_STRING_VALUE)) {
                installPost.put(Defines.Jsonkey.InstallReferrer.getKey(), prefHelper_.getInstallReferrerParams());
            }

            // Read and update the URI scheme only if running in debug mode
            if (prefHelper_.getExternDebug()) {
                String uriScheme = sysObserver.getURIScheme();
                if (!uriScheme.equals(SystemObserver.BLANK))
                    installPost.put(Defines.Jsonkey.URIScheme.getKey(), uriScheme);
            }

            installPost.put(Defines.Jsonkey.FaceBookAppLinkChecked.getKey(), prefHelper_.getIsAppLinkTriggeredInit());
            installPost.put(Defines.Jsonkey.IsReferrable.getKey(), prefHelper_.getIsReferrable());
            installPost.put(Defines.Jsonkey.Update.getKey(), sysObserver.getUpdateState());
            installPost.put(Defines.Jsonkey.Debug.getKey(), prefHelper_.getExternDebug());
            setPost(installPost);

        } catch (JSONException ex) {
            ex.printStackTrace();
            constructError_ = true;
        }


    }

    public ServerRequestRegisterInstall(String requestPath, JSONObject post, Context context) {
        super(requestPath, post, context);
    }

    @Override
    public boolean hasCallBack() {
        return callback_ != null;
    }

    @Override
    public void onRequestSucceeded(ServerResponse resp, Branch branch) {
        super.onRequestSucceeded(resp, branch);
        try {
            prefHelper_.setUserURL(resp.getObject().getString(Defines.Jsonkey.Link.getKey()));

            //clear out install referrer string
            prefHelper_.setInstallReferrerParams(PrefHelper.NO_STRING_VALUE);

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

//            if (resp.getObject().has(Defines.Jsonkey.LinkClickID.getKey())) {
//                prefHelper_.setLinkClickID(resp.getObject().getString(Defines.Jsonkey.LinkClickID.getKey()));
//            } else {
//                prefHelper_.setLinkClickID(PrefHelper.NO_STRING_VALUE);
//            }

            if (resp.getObject().has(Defines.Jsonkey.Data.getKey())) {
                String params = resp.getObject().getString(Defines.Jsonkey.Data.getKey());
                prefHelper_.setSessionParams(params);
            } else {
                prefHelper_.setSessionParams(PrefHelper.NO_STRING_VALUE);
            }
            if (callback_ != null) {
                callback_.onInitFinished(branch.getLatestReferringParams(), null);
            }

            prefHelper_.setAppVersion(systemObserver_.getAppVersion());

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        onInitSessionCompleted(resp, branch);
    }

    public void setInitFinishedCallback(Branch.BranchReferralInitListener callback) {
        if (callback != null) {  // Update callback if set with valid callback instance.
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
    public String getRequestActionName() {
        return ACTION_INSTALL;
    }
}
