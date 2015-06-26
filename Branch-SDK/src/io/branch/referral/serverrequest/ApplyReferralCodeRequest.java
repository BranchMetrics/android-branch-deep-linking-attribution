package io.branch.referral.serverrequest;

import android.app.Application;
import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import io.branch.referral.Branch;
import io.branch.referral.BranchError;
import io.branch.referral.Defines;
import io.branch.referral.PrefHelper;
import io.branch.referral.ServerRequest;
import io.branch.referral.ServerResponse;

/**
 * * <p>
 * The server request for Applying a referral code. Handles request creation and execution.
 * </p>
 */
public class ApplyReferralCodeRequest extends ServerRequest {

    Branch.BranchReferralInitListener callback_;

    /**
     * <p>Creates an instance of aApplyReferralCodeRequest.This request take care of applying
     * supplied referral code to the current user session upon initialisation.</p>
     *
     * @param context  Current {@link Application} context
     * @param code     A {@link String} object containing the referral code supplied.
     * @param callback A {@link Branch.BranchReferralInitListener} callback to handle the server
     *                 response of the referral submission request.
     * @see Branch.BranchReferralInitListener
     */
    public ApplyReferralCodeRequest(Context context, Branch.BranchReferralInitListener callback,
                                    String code) {
        super(context, Defines.RequestPath.ApplyReferralCode.getPath());

        callback_ = callback;
        JSONObject post = new JSONObject();
        try {
            post.put("identity_id", prefHelper_.getIdentityID());
            post.put("device_fingerprint_id", prefHelper_.getDeviceFingerPrintID());
            post.put("session_id", prefHelper_.getSessionID());
            if (!prefHelper_.getLinkClickID().equals(PrefHelper.NO_STRING_VALUE)) {
                post.put("link_click_id", prefHelper_.getLinkClickID());
            }
            post.put("referral_code", code);
            setPost(post);
        } catch (JSONException ex) {
            ex.printStackTrace();
            constructError_ = true;
        }
    }

    public ApplyReferralCodeRequest(String requestPath, JSONObject post, Context context) {
        super(requestPath, post, context);
    }

    @Override
    public String getRequestUrl() {
        String code = "";
        try {
            code = getPost().getString("referral_code");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return super.getRequestUrl() + code;
    }


    @Override
    public void onRequestSucceeded(ServerResponse resp, Branch branch) {
        if (callback_ != null) {
            try {
                JSONObject json;
                BranchError error = null;
                // check if a valid referral code json is returned
                if (!resp.getObject().has(Branch.REFERRAL_CODE)) {
                    json = new JSONObject();
                    json.put("error_message", "Invalid referral code");
                    error = new BranchError("Trouble applying referral code.", BranchError.ERR_INVALID_REFERRAL_CODE);
                } else {
                    json = resp.getObject();
                }
                callback_.onInitFinished(json, error);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void handleFailure(int statusCode) {
        if (callback_ != null) {
            callback_.onInitFinished(null, new BranchError("Trouble applying the referral code.", statusCode));
        }
    }

    @Override
    public boolean handleErrors(Context context) {
        if (!super.doesAppHasInternetPermission(context)) {
            callback_.onInitFinished(null, new BranchError("Trouble applying the referral code.", BranchError.ERR_NO_INTERNET_PERMISSION));
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
