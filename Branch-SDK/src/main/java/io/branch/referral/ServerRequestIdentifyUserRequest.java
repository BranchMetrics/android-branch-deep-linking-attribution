package io.branch.referral;

import android.app.Application;
import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * * <p>
 * The server request for identifying current user to Branch API. Handles request creation and execution.
 * </p>
 */
class ServerRequestIdentifyUserRequest extends ServerRequest {
    Branch.BranchReferralInitListener callback_;
    String userId_ = null;

    /**
     * <p>Create an instance of {@link ServerRequestIdentifyUserRequest} to Identify the current user to the Branch API
     * by supplying a unique identifier as a {@link String} value, with a callback specified to perform a
     * defined action upon successful response to request.</p>
     *
     * @param context  Current {@link Application} context
     * @param userId   A {@link String} value containing the unique identifier of the user.
     * @param callback A {@link Branch.BranchReferralInitListener} callback instance that will return
     *                 the data associated with the user id being assigned, if available.
     */
    public ServerRequestIdentifyUserRequest(Context context, Branch.BranchReferralInitListener callback, String userId) {
        super(context, Defines.RequestPath.IdentifyUser);

        callback_ = callback;
        userId_ = userId;

        JSONObject post = new JSONObject();
        try {
            post.put(Defines.Jsonkey.RandomizedBundleToken.getKey(), prefHelper_.getRandomizedBundleToken());
            post.put(Defines.Jsonkey.RandomizedDeviceToken.getKey(), prefHelper_.getRandomizedDeviceToken());
            post.put(Defines.Jsonkey.SessionID.getKey(), prefHelper_.getSessionID());
            if (!prefHelper_.getLinkClickID().equals(PrefHelper.NO_STRING_VALUE)) {
                post.put(Defines.Jsonkey.LinkClickID.getKey(), prefHelper_.getLinkClickID());
            }
            post.put(Defines.Jsonkey.Identity.getKey(), userId);
            setPost(post);
        } catch (JSONException ex) {
            ex.printStackTrace();
            constructError_ = true;
        }
    }

    public ServerRequestIdentifyUserRequest(Defines.RequestPath requestPath, JSONObject post, Context context) {
        super(requestPath, post, context);
    }

    public void onRequestSucceeded(ServerResponse resp, Branch branch) {
        try {
            if (getPost() != null && getPost().has(Defines.Jsonkey.Identity.getKey())) {
                prefHelper_.setIdentity(getPost().getString(Defines.Jsonkey.Identity.getKey()));
            }

            prefHelper_.setRandomizedBundleToken(resp.getObject().getString(Defines.Jsonkey.RandomizedBundleToken.getKey()));
            prefHelper_.setUserURL(resp.getObject().getString(Defines.Jsonkey.Link.getKey()));

            if (resp.getObject().has(Defines.Jsonkey.ReferringData.getKey())) {
                String params = resp.getObject().getString(Defines.Jsonkey.ReferringData.getKey());
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
    public void handleFailure(int statusCode, String causeMsg) {
        if (callback_ != null) {
            JSONObject obj = new JSONObject();
            try {
                obj.put("error_message", "Trouble reaching server. Please try again in a few minutes");
            } catch (JSONException ex) {
                ex.printStackTrace();
            }
            callback_.onInitFinished(obj, new BranchError("Trouble setting the user alias. " + causeMsg, statusCode));
        }
    }

    @Override
    public boolean handleErrors(Context context) {
        if (!super.doesAppHasInternetPermission(context)) {
            if (callback_ != null) {
                callback_.onInitFinished(null, new BranchError("Trouble setting the user alias.", BranchError.ERR_NO_INTERNET_PERMISSION));
            }
            return true;
        } else {
            try {
                String userId = getPost().getString(Defines.Jsonkey.Identity.getKey());
                if (userId == null || userId.length() == 0 || userId.equals(prefHelper_.getIdentity())) {
                    return true;
                }
            } catch (JSONException ignore) {
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
     *
     * @return True if the user id refferes to the existing user
     */
    public boolean isExistingID() {
        try {
            String userId= getPost().getString(Defines.Jsonkey.Identity.getKey());
            return (userId != null && userId.equals(prefHelper_.getIdentity()));
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }

    }

    /*
     * Callback with existing first referral params.
     *
     * @param branch {@link Branch} instance.
     */
    public void handleUserExist(Branch branch) {
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
