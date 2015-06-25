package io.branch.referral.serverrequest;

import android.app.Application;
import android.content.Context;
import android.nfc.tech.IsoDep;

import org.json.JSONException;
import org.json.JSONObject;

import io.branch.referral.Branch;
import io.branch.referral.Defines;
import io.branch.referral.PrefHelper;
import io.branch.referral.ServerRequest;
import io.branch.referral.ServerResponse;
import io.branch.referral.errors.BranchInternetPermissionError;
import io.branch.referral.errors.BranchNotInitError;
import io.branch.referral.errors.BranchSetIdentityError;

/**
 * * <p>
 * The server request for identifying current user to Branch API. Handles request creation and execution.
 * </p>
 */
public class IdentifyUserRequest extends ServerRequest {
    Branch.BranchReferralInitListener callback_;
    String userId_ = null;
    /**
     * <p>Create an instance of {@link IdentifyUserRequest} to Identify the current user to the Branch API
     * by supplying a unique identifier as a {@link String} value, with a callback specified to perform a
     * defined action upon successful response to request.</p>
     *
     * @param context  Current {@link Application} context
     * @param userId   A {@link String} value containing the unique identifier of the user.
     * @param callback A {@link Branch.BranchReferralInitListener} callback instance that will return
     *                 the data associated with the user id being assigned, if available.
     */
    public IdentifyUserRequest(Context context, Branch.BranchReferralInitListener callback, String userId) {
        super(context, Defines.RequestPath.IdentifyUser.getPath());

        callback_ = callback;
        userId_ = userId;

        JSONObject post = new JSONObject();
        try {
            post.put("identity_id", prefHelper_.getIdentityID());
            post.put("device_fingerprint_id", prefHelper_.getDeviceFingerPrintID());
            post.put("session_id", prefHelper_.getSessionID());
            if (!prefHelper_.getLinkClickID().equals(PrefHelper.NO_STRING_VALUE)) {
                post.put("link_click_id", prefHelper_.getLinkClickID());
            }
            post.put("identity", userId);
            setPost(post);
        } catch (JSONException ex) {
            ex.printStackTrace();
            constructError_ = true;
        }
    }

    public IdentifyUserRequest(String requestPath, JSONObject post, Context context) {
        super(requestPath, post, context);
    }


    public void onRequestSucceeded(ServerResponse resp, Branch branch) {
        try {
            if (getPost() != null && getPost().has("identity")) {
                prefHelper_.setIdentity(getPost().getString("identity"));
            }

            prefHelper_.setIdentityID(resp.getObject().getString("identity_id"));
            prefHelper_.setUserURL(resp.getObject().getString("link"));

            if (resp.getObject().has("referring_data")) {
                String params = resp.getObject().getString("referring_data");
                prefHelper_.setInstallParams(params);
            }

            if (callback_ != null) {
                callback_.onInitFinished(branch.getFirstReferringParams(), null);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
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
            if (isInitNotStarted)
                callback_.onInitFinished(obj, new BranchNotInitError());
            else
                callback_.onInitFinished(obj, new BranchSetIdentityError());
        }
    }

    @Override
    public boolean handleErrors(Context context) {
        if (!super.doesAppHasInternetPermission(context)) {
            callback_.onInitFinished(null, new BranchInternetPermissionError());
            return true;
        }
        else{
            if (userId_ == null || userId_.length() == 0 || userId_.equals(prefHelper_.getIdentity())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isGetRequest() {
        return false;
    }

    /**
     * Return true if the user id provided for user identification is the same as existing id
     * @return
     */
    public boolean isExistingID(){
        return (userId_ != null && userId_.equals(prefHelper_.getIdentity()));
    }

    /*
     * Callback with existing first referral params.
     *
     * @param branch {@link Branch} instance.
     */
    public void handleUserExist(Branch branch){
        if (callback_ != null) {
            callback_.onInitFinished(branch.getFirstReferringParams(), null);
        }
    }

    @Override
    public void clearCallbacks() {
        callback_ = null;
    }

    @Override
    public boolean shouldRetryOnFail() {
        return true;   //Identify user request need to retry on failure.
    }
}
