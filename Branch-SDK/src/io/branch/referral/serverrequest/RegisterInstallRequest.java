package io.branch.referral.serverrequest;

import android.app.Application;
import android.content.Context;
import android.util.DisplayMetrics;

import org.json.JSONException;
import org.json.JSONObject;

import io.branch.referral.Branch;
import io.branch.referral.Defines;
import io.branch.referral.PrefHelper;
import io.branch.referral.ServerRequest;
import io.branch.referral.ServerResponse;
import io.branch.referral.SystemObserver;
import io.branch.referral.errors.BranchInitError;
import io.branch.referral.errors.BranchInternetPermissionError;

/**
 * * <p>
 * The server request for registering an app install to Branch API. Handles request creation and execution.
 * </p>
 */
public class RegisterInstallRequest extends ServerRequest {

    Branch.BranchReferralInitListener callback_;

    /**
     * <p>Create an instance of {@link RegisterInstallRequest} to notify Branch API on a new install.</p>
     *
     * @param context     Current {@link Application} context
     * @param callback    A {@link Branch.BranchReferralInitListener} callback instance that will return
     *                    the data associated with new install registration.
     * @param sysObserver {@link SystemObserver} instance.
     * @param installID   installation ID.                                   .
     */
    public RegisterInstallRequest(Context context, Branch.BranchReferralInitListener callback,
                                  SystemObserver sysObserver, String installID) {

        super(context, Defines.RequestPath.RegisterInstall.getPath());

        callback_ = callback;
        JSONObject installPost = new JSONObject();
        try {
            if (!installID.equals(PrefHelper.NO_STRING_VALUE))
                installPost.put("link_click_id", installID);
            String uniqId = sysObserver.getUniqueID(prefHelper_.getExternDebug());
            if (!uniqId.equals(SystemObserver.BLANK)) {
                installPost.put("hardware_id", uniqId);
                installPost.put("is_hardware_id_real", sysObserver.hasRealHardwareId());
            }
            if (!sysObserver.getAppVersion().equals(SystemObserver.BLANK))
                installPost.put("app_version", sysObserver.getAppVersion());
            if (!sysObserver.getCarrier().equals(SystemObserver.BLANK))
                installPost.put("carrier", sysObserver.getCarrier());
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
                installPost.put("bluetooth", sysObserver.getBluetoothPresent());
            }
            if (!sysObserver.getBluetoothVersion().equals(SystemObserver.BLANK))
                installPost.put("bluetooth_version", sysObserver.getBluetoothVersion());
            installPost.put("has_nfc", sysObserver.getNFCPresent());
            installPost.put("has_telephone", sysObserver.getTelephonePresent());
            if (!sysObserver.getPhoneBrand().equals(SystemObserver.BLANK))
                installPost.put("brand", sysObserver.getPhoneBrand());
            if (!sysObserver.getPhoneModel().equals(SystemObserver.BLANK))
                installPost.put("model", sysObserver.getPhoneModel());
            if (!sysObserver.getOS().equals(SystemObserver.BLANK))
                installPost.put("os", sysObserver.getOS());
            String uriScheme = sysObserver.getURIScheme();
            if (!uriScheme.equals(SystemObserver.BLANK))
                installPost.put("uri_scheme", uriScheme);
            installPost.put("os_version", sysObserver.getOSVersion());
            DisplayMetrics dMetrics = sysObserver.getScreenDisplay();
            installPost.put("screen_dpi", dMetrics.densityDpi);
            installPost.put("screen_height", dMetrics.heightPixels);
            installPost.put("screen_width", dMetrics.widthPixels);
            installPost.put("wifi", sysObserver.getWifiConnected());
            installPost.put("is_referrable", prefHelper_.getIsReferrable());
            installPost.put("update", sysObserver.getUpdateState(true));
            if (!prefHelper_.getLinkClickIdentifier().equals(PrefHelper.NO_STRING_VALUE)) {
                installPost.put("link_identifier", prefHelper_.getLinkClickIdentifier());
            }
            String advertisingId = sysObserver.getAdvertisingId();
            if (advertisingId != null) {
                installPost.put("google_advertising_id", advertisingId);
            }

            int latVal = sysObserver.getLATValue();
            installPost.put("lat_val", latVal);
            installPost.put("debug", prefHelper_.isDebug());

            setPost(installPost);

        } catch (JSONException ex) {
            ex.printStackTrace();
            constructError_ = true;
        }

    }


    public RegisterInstallRequest(String requestPath, JSONObject post, Context context) {
        super(requestPath, post, context);
    }

    @Override
    public void onRequestSucceeded(ServerResponse resp, Branch branch) {
        try {
            prefHelper_.setDeviceFingerPrintID(resp.getObject().getString("device_fingerprint_id"));
            prefHelper_.setIdentityID(resp.getObject().getString("identity_id"));
            prefHelper_.setUserURL(resp.getObject().getString("link"));
            prefHelper_.setSessionID(resp.getObject().getString("session_id"));
            prefHelper_.setLinkClickIdentifier(PrefHelper.NO_STRING_VALUE);

            if (prefHelper_.getIsReferrable() == 1) {
                if (resp.getObject().has("data")) {
                    String params = resp.getObject().getString("data");
                    prefHelper_.setInstallParams(params);
                } else {
                    prefHelper_.setInstallParams(PrefHelper.NO_STRING_VALUE);
                }
            }

            if (resp.getObject().has("link_click_id")) {
                prefHelper_.setLinkClickID(resp.getObject().getString("link_click_id"));
            } else {
                prefHelper_.setLinkClickID(PrefHelper.NO_STRING_VALUE);
            }

            if (resp.getObject().has("data")) {
                String params = resp.getObject().getString("data");
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
        callback_ = callback;
    }

    @Override
    public void handleFailure(boolean isInitNotStarted) {
        if (callback_ != null) {
            JSONObject obj = new JSONObject();
            try {
                obj.put("error_message", "Trouble reaching server. Please try again in a few minutes");
            } catch (JSONException ex) {
                ex.printStackTrace();
            }
            callback_.onInitFinished(obj, new BranchInitError());
        }
    }

    @Override
    public boolean handleErrors(Context context) {
        if (!super.doesAppHasInternetPermission(context)) {
            callback_.onInitFinished(null, new BranchInternetPermissionError());
            return true;
        }
        return false;
    }

    @Override
    public boolean isGetRequest() {
        return false;
    }
}
