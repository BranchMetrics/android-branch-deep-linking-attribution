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
    public ServerRequestRegisterOpen(Context context, Branch.BranchReferralInitListener callback,
                                     SystemObserver sysObserver) {
        super(context, Defines.RequestPath.RegisterOpen.getPath());

        callback_ = callback;
        JSONObject openPost = new JSONObject();
        try {
            openPost.put(Defines.Jsonkey.DeviceFingerprintID.getKey(), prefHelper_.getDeviceFingerPrintID());
            openPost.put(Defines.Jsonkey.IdentityID.getKey(), prefHelper_.getIdentityID());
            openPost.put(Defines.Jsonkey.IsReferrable.getKey(), prefHelper_.getIsReferrable());

            if (!sysObserver.getAppVersion().equals(SystemObserver.BLANK)) {
                openPost.put(Defines.Jsonkey.AppVersion.getKey(), sysObserver.getAppVersion());
            }
            if (!prefHelper_.getLinkClickIdentifier().equals(PrefHelper.NO_STRING_VALUE)) {
                openPost.put(Defines.Jsonkey.LinkIdentifier.getKey(), prefHelper_.getLinkClickIdentifier());
            }
            if (!prefHelper_.getAppLink().equals(PrefHelper.NO_STRING_VALUE)) {
                openPost.put(Defines.Jsonkey.AndroidAppLinkURL.getKey(), prefHelper_.getAppLink());
            }
            if (!prefHelper_.getPushIdentifier().equals(PrefHelper.NO_STRING_VALUE)) {
                openPost.put(Defines.Jsonkey.AndroidPushIdentifier.getKey(), prefHelper_.getPushIdentifier());
            }
            // External URI or Extras if exist
            if (!prefHelper_.getExternalIntentUri().equals(PrefHelper.NO_STRING_VALUE)) {
                openPost.put(Defines.Jsonkey.External_Intent_URI.getKey(), prefHelper_.getExternalIntentUri());
            }
            if (!prefHelper_.getExternalIntentExtra().equals(PrefHelper.NO_STRING_VALUE)) {
                openPost.put(Defines.Jsonkey.External_Intent_Extra.getKey(), prefHelper_.getExternalIntentExtra());
            }

            openPost.put(Defines.Jsonkey.Update.getKey(), sysObserver.getUpdateState(true));
            openPost.put(Defines.Jsonkey.Debug.getKey(), prefHelper_.isDebug() || prefHelper_.getExternDebug());

            setPost(openPost);
        } catch (JSONException ex) {
            ex.printStackTrace();
            constructError_ = true;
        }

    }

    public ServerRequestRegisterOpen(String requestPath, JSONObject post, Context context) {
        super(requestPath, post, context);
    }

    @Override
    public void onRequestSucceeded(ServerResponse resp, Branch branch) {
        try {
            prefHelper_.setLinkClickIdentifier(PrefHelper.NO_STRING_VALUE);
            prefHelper_.setExternalIntentUri(PrefHelper.NO_STRING_VALUE);
            prefHelper_.setExternalIntentExtra(PrefHelper.NO_STRING_VALUE);
            prefHelper_.setAppLink(PrefHelper.NO_STRING_VALUE);
            prefHelper_.setPushIdentifier(PrefHelper.NO_STRING_VALUE);
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

            if (callback_ != null) {
                callback_.onInitFinished(branch.getLatestReferringParams(), null);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    public void setInitFinishedCallback(Branch.BranchReferralInitListener callback) {
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
}
