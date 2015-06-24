package io.branch.referral.serverrequest;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import io.branch.referral.Branch;
import io.branch.referral.Defines;
import io.branch.referral.PrefHelper;
import io.branch.referral.ServerRequest;
import io.branch.referral.ServerResponse;
import io.branch.referral.errors.BranchInternetPermissionError;

/**
 * * <p>
 * The server request for logging out  current user from Branch API. Handles request creation and execution.
 * </p>
 */
public class LogoutRequest extends ServerRequest {

    /**
     * <p>Create an instance of {@link LogoutRequest} to signal when  different person is about to use the app. For example,
     * if you allow users to log out and let their friend use the app, you should call this to notify Branch
     * to create a new user for this device. This will clear the first and latest params, as a new session is created.</p>
     *
     * @param context Current {@link Application} context
     */
    public LogoutRequest(Context context) {
        super(context, Defines.RequestPath.Logout.getPath());
        JSONObject post = new JSONObject();
        try {
            post.put("identity_id", prefHelper_.getIdentityID());
            post.put("device_fingerprint_id", prefHelper_.getDeviceFingerPrintID());
            post.put("session_id", prefHelper_.getSessionID());
            if (!prefHelper_.getLinkClickID().equals(PrefHelper.NO_STRING_VALUE)) {
                post.put("link_click_id", prefHelper_.getLinkClickID());
            }
            setPost(post);
        } catch (JSONException ex) {
            ex.printStackTrace();
            constructError_ = true;
        }
    }

    public LogoutRequest(String requestPath, JSONObject post, Context context) {
        super(requestPath, post, context);
    }

    @Override
    public void onRequestSucceeded(ServerResponse resp, Branch branch) {
        try {
            prefHelper_.setSessionID(resp.getObject().getString("session_id"));
            prefHelper_.setIdentityID(resp.getObject().getString("identity_id"));
            prefHelper_.setUserURL(resp.getObject().getString("link"));

            prefHelper_.setInstallParams(PrefHelper.NO_STRING_VALUE);
            prefHelper_.setSessionParams(PrefHelper.NO_STRING_VALUE);
            prefHelper_.setIdentity(PrefHelper.NO_STRING_VALUE);
            prefHelper_.clearUserValues();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void handleFailure(boolean isInitNotStarted) {
        //No implementation on purpose
    }

    @Override
    public boolean handleErrors(Context context) {
        if (!super.doesAppHasInternetPermission(context)) {
            Log.i("BranchSDK", "Trouble executing your request. Please add 'android.permission.INTERNET' in your applications manifest file");
            return true;
        }
        return false;
    }

    @Override
    public boolean isGetRequest() {
        return false;
    }
}
