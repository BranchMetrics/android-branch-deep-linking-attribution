package io.branch.referral;

import android.app.Application;
import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * * <p>
 * The server request for logging out  current user from Branch API. Handles request creation and execution.
 * </p>
 */
class ServerRequestLogout extends ServerRequest {

    private Branch.LogoutStatusListener callback_;

    /**
     * <p>Create an instance of {@link ServerRequestLogout} to signal when  different person is about to use the app. For example,
     * if you allow users to log out and let their friend use the app, you should call this to notify Branch
     * to create a new user for this device. This will clear the first and latest params, as a new session is created.</p>
     *
     * @param context  Current {@link Application} context
     * @param callback An instance of {@link io.branch.referral.Branch.LogoutStatusListener} to callback with the logout operation status.
     */
    public ServerRequestLogout(Context context, Branch.LogoutStatusListener callback) {
        super(context, Defines.RequestPath.Logout);
        callback_ = callback;
        JSONObject post = new JSONObject();
        try {
            post.put(Defines.Jsonkey.RandomizedBundleToken.getKey(), prefHelper_.getRandomizedBundleToken());
            post.put(Defines.Jsonkey.RandomizedDeviceToken.getKey(), prefHelper_.getRandomizedDeviceToken());
            post.put(Defines.Jsonkey.SessionID.getKey(), prefHelper_.getSessionID());
            if (!prefHelper_.getLinkClickID().equals(PrefHelper.NO_STRING_VALUE)) {
                post.put(Defines.Jsonkey.LinkClickID.getKey(), prefHelper_.getLinkClickID());
            }
            setPost(post);
        } catch (JSONException ex) {
            ex.printStackTrace();
            constructError_ = true;
        }
    }

    public ServerRequestLogout(Defines.RequestPath requestPath, JSONObject post, Context context) {
        super(requestPath, post, context);
    }

    @Override
    public void onRequestSucceeded(ServerResponse resp, Branch branch) {
        try {
            prefHelper_.setSessionID(resp.getObject().getString(Defines.Jsonkey.SessionID.getKey()));
            prefHelper_.setRandomizedBundleToken(resp.getObject().getString(Defines.Jsonkey.RandomizedBundleToken.getKey()));
            prefHelper_.setUserURL(resp.getObject().getString(Defines.Jsonkey.Link.getKey()));

            prefHelper_.setInstallParams(PrefHelper.NO_STRING_VALUE);
            prefHelper_.setSessionParams(PrefHelper.NO_STRING_VALUE);
            prefHelper_.setIdentity(PrefHelper.NO_STRING_VALUE);
            prefHelper_.clearUserValues();
        } catch (JSONException e) {
            e.printStackTrace();
        } finally {
            if (callback_ != null) {
                callback_.onLogoutFinished(true, null);
            }
        }
    }

    @Override
    public void handleFailure(int statusCode, String causeMsg) {
        if (callback_ != null) {
            callback_.onLogoutFinished(false, new BranchError("Logout error. " + causeMsg, statusCode));
        }
    }

    @Override
    public boolean handleErrors(Context context) {
        if (!super.doesAppHasInternetPermission(context)) {
            if (callback_ != null) {
                callback_.onLogoutFinished(false, new BranchError("Logout failed", BranchError.ERR_NO_INTERNET_PERMISSION));
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
    boolean isPersistable() {
        return false; // No need to retrieve logout from previous session
    }
}
