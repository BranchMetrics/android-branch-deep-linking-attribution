package io.branch.referral.serverrequest;

import android.app.Application;
import android.content.Context;

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
 * The server request for registering an app open event to Branch API. Handles request creation and execution.
 * </p>
 */
public class RegisterOpenRequest extends ServerRequest {

    Branch.BranchReferralInitListener callback_;

    /**
     * <p>Create an instance of {@link RegisterInstallRequest} to notify Branch API on app open event.</p>
     *
     * @param context     Current {@link Application} context
     * @param callback    A {@link Branch.BranchReferralInitListener} callback instance that will return
     *                    the data associated with new install registration.
     * @param sysObserver {@link SystemObserver} instance.
     */
    public RegisterOpenRequest(Context context, Branch.BranchReferralInitListener callback,
                               SystemObserver sysObserver) {
        super(context, Defines.RequestPath.RegisterOpen.getPath());

        callback_ = callback;
        JSONObject openPost = new JSONObject();
        try {
            openPost.put("device_fingerprint_id", prefHelper_.getDeviceFingerPrintID());
            openPost.put("identity_id", prefHelper_.getIdentityID());
            openPost.put("is_referrable", prefHelper_.getIsReferrable());
            if (!sysObserver.getAppVersion().equals(SystemObserver.BLANK))
                openPost.put("app_version", sysObserver.getAppVersion());
            openPost.put("os_version", sysObserver.getOSVersion());
            openPost.put("update", sysObserver.getUpdateState(true));
            String uriScheme = sysObserver.getURIScheme();
            if (!uriScheme.equals(SystemObserver.BLANK))
                openPost.put("uri_scheme", uriScheme);
            if (!sysObserver.getOS().equals(SystemObserver.BLANK))
                openPost.put("os", sysObserver.getOS());
            if (!prefHelper_.getLinkClickIdentifier().equals(PrefHelper.NO_STRING_VALUE)) {
                openPost.put("link_identifier", prefHelper_.getLinkClickIdentifier());
            }
            String advertisingId = sysObserver.getAdvertisingId();
            if (advertisingId != null) {
                openPost.put("google_advertising_id", advertisingId);
            }

            int latVal = sysObserver.getLATValue();
            openPost.put("lat_val", latVal);
            openPost.put("debug", prefHelper_.isDebug());

            setPost(openPost);
        } catch (JSONException ex) {
            ex.printStackTrace();
            constructError_ = true;
        }

    }

    public RegisterOpenRequest(String requestPath, JSONObject post, Context context) {
        super(requestPath, post, context);
    }

    @Override
    public void onRequestSucceeded(ServerResponse resp, Branch branch) {
        try {
            prefHelper_.setSessionID(resp.getObject().getString("session_id"));
            prefHelper_.setDeviceFingerPrintID(resp.getObject().getString("device_fingerprint_id"));
            prefHelper_.setLinkClickIdentifier(PrefHelper.NO_STRING_VALUE);
            if (resp.getObject().has("identity_id")) {
                prefHelper_.setIdentityID(resp.getObject().getString("identity_id"));
            }
            if (resp.getObject().has("link_click_id")) {
                prefHelper_.setLinkClickID(resp.getObject().getString("link_click_id"));
            } else {
                prefHelper_.setLinkClickID(PrefHelper.NO_STRING_VALUE);
            }

            if (prefHelper_.getIsReferrable() == 1) {
                if (resp.getObject().has("data")) {
                    String params = resp.getObject().getString("data");
                    prefHelper_.setInstallParams(params);
                }
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

    @Override
    public void clearCallbacks() {
        callback_ = null;
    }
}
