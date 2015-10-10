package io.branch.referral;

import android.app.Application;
import android.content.Context;
import android.util.DisplayMetrics;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * * <p>
 * The server request for registering an app install to Branch API. Handles request creation and execution.
 * </p>
 */
class ServerRequestRegisterInstall extends ServerRequestInitSession {

    Branch.BranchReferralInitListener callback_;

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
                                        SystemObserver sysObserver, String installID) {

        super(context, Defines.RequestPath.RegisterInstall.getPath());

        callback_ = callback;
        JSONObject installPost = new JSONObject();
        try {
            if (!installID.equals(PrefHelper.NO_STRING_VALUE))
                installPost.put(Defines.Jsonkey.LinkClickID.getKey(), installID);
            String uniqId = sysObserver.getUniqueID(prefHelper_.getExternDebug());
            if (!uniqId.equals(SystemObserver.BLANK)) {
                installPost.put(Defines.Jsonkey.HardwareID.getKey(), uniqId);
                installPost.put(Defines.Jsonkey.IsHardwareIDReal.getKey(), sysObserver.hasRealHardwareId());
            }
            if (!sysObserver.getAppVersion().equals(SystemObserver.BLANK))
                installPost.put(Defines.Jsonkey.AppVersion.getKey(), sysObserver.getAppVersion());
            if (!sysObserver.getCarrier().equals(SystemObserver.BLANK))
                installPost.put(Defines.Jsonkey.Carrier.getKey(), sysObserver.getCarrier());
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
                installPost.put(Defines.Jsonkey.Bluetooth.getKey(), sysObserver.getBluetoothPresent());
            }
            if (!sysObserver.getBluetoothVersion().equals(SystemObserver.BLANK))
                installPost.put(Defines.Jsonkey.BluetoothVersion.getKey(), sysObserver.getBluetoothVersion());
            installPost.put(Defines.Jsonkey.HasNfc.getKey(), sysObserver.getNFCPresent());
            installPost.put(Defines.Jsonkey.HasTelephone.getKey(), sysObserver.getTelephonePresent());
            if (!sysObserver.getPhoneBrand().equals(SystemObserver.BLANK))
                installPost.put(Defines.Jsonkey.Brand.getKey(), sysObserver.getPhoneBrand());
            if (!sysObserver.getPhoneModel().equals(SystemObserver.BLANK))
                installPost.put(Defines.Jsonkey.Model.getKey(), sysObserver.getPhoneModel());
            if (!sysObserver.getOS().equals(SystemObserver.BLANK))
                installPost.put(Defines.Jsonkey.OS.getKey(), sysObserver.getOS());
            // Read and update the URI scheme only if running in debug mode
            if(prefHelper_.isDebug()) {
                String uriScheme = sysObserver.getURIScheme();
                if (!uriScheme.equals(SystemObserver.BLANK))
                    installPost.put(Defines.Jsonkey.URIScheme.getKey(), uriScheme);
            }
            installPost.put(Defines.Jsonkey.OSVersion.getKey(), sysObserver.getOSVersion());
            DisplayMetrics dMetrics = sysObserver.getScreenDisplay();
            installPost.put(Defines.Jsonkey.ScreenDpi.getKey(), dMetrics.densityDpi);
            installPost.put(Defines.Jsonkey.ScreenHeight.getKey(), dMetrics.heightPixels);
            installPost.put(Defines.Jsonkey.ScreenWidth.getKey(), dMetrics.widthPixels);
            installPost.put(Defines.Jsonkey.WiFi.getKey(), sysObserver.getWifiConnected());
            installPost.put(Defines.Jsonkey.IsReferrable.getKey(), prefHelper_.getIsReferrable());
            installPost.put(Defines.Jsonkey.Update.getKey(), sysObserver.getUpdateState(true));
            if (!prefHelper_.getLinkClickIdentifier().equals(PrefHelper.NO_STRING_VALUE)) {
                installPost.put(Defines.Jsonkey.LinkIdentifier.getKey(), prefHelper_.getLinkClickIdentifier());
            }
            if (!prefHelper_.getAppLink().equals(PrefHelper.NO_STRING_VALUE)) {
                installPost.put(Defines.Jsonkey.AndroidAppLinkURL.getKey(), prefHelper_.getAppLink());
            }

            installPost.put(Defines.Jsonkey.Debug.getKey(), prefHelper_.isDebug() || prefHelper_.getExternDebug());
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
        try {
            prefHelper_.setDeviceFingerPrintID(resp.getObject().getString(Defines.Jsonkey.DeviceFingerprintID.getKey()));
            prefHelper_.setUserURL(resp.getObject().getString(Defines.Jsonkey.Link.getKey()));
            prefHelper_.setLinkClickIdentifier(PrefHelper.NO_STRING_VALUE);
            prefHelper_.setAppLink(PrefHelper.NO_STRING_VALUE);

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
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    public void setInitFinishedCallback(Branch.BranchReferralInitListener callback) {
        if(callback != null) {  // Update callback if set with valid callback instance.
            callback_ = callback;
        }
    }

    @Override
    public void handleFailure(int statusCode) {
        if (callback_ != null) {
            JSONObject obj = new JSONObject();
            try {
                obj.put("error_message", "Trouble reaching server. Please try again in a few minutes");
            } catch (JSONException ex) {
                ex.printStackTrace();
            }
            callback_.onInitFinished(obj, new BranchError("Trouble initializing Branch.", statusCode));
        }
    }

    @Override
    public boolean handleErrors(Context context) {
        if (!super.doesAppHasInternetPermission(context)) {
            callback_.onInitFinished(null, new BranchError("Trouble initializing Branch.", BranchError.ERR_NO_INTERNET_PERMISSION));
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
}
