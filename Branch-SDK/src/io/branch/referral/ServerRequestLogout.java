package io.branch.referral;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * * <p>
 * The server request for logging out  current user from Branch API. Handles request creation and execution.
 * </p>
 */
class ServerRequestLogout extends ServerRequest {

    /**
     * <p>Create an instance of {@link ServerRequestLogout} to signal when  different person is about to use the app. For example,
     * if you allow users to log out and let their friend use the app, you should call this to notify Branch
     * to create a new user for this device. This will clear the first and latest params, as a new session is created.</p>
     *
     * @param context Current {@link Application} context
     */
    public ServerRequestLogout(Context context) {
        super(context, Defines.RequestPath.Logout.getPath());
        JSONObject post = new JSONObject();
        try {
            post.put(Defines.Jsonkey.IdentityID.getKey(), prefHelper_.getIdentityID());
            post.put(Defines.Jsonkey.DeviceFingerprintID.getKey(), prefHelper_.getDeviceFingerPrintID());
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

    public ServerRequestLogout(String requestPath, JSONObject post, Context context) {
        super(requestPath, post, context);
    }

    @Override
    public void onRequestSucceeded(ServerResponse resp, Branch branch) {
        try {
            prefHelper_.setSessionID(resp.getObject().getString(Defines.Jsonkey.SessionID.getKey()));
            prefHelper_.setIdentityID(resp.getObject().getString(Defines.Jsonkey.IdentityID.getKey()));
            prefHelper_.setUserURL(resp.getObject().getString(Defines.Jsonkey.Link.getKey()));

            prefHelper_.setInstallParams(PrefHelper.NO_STRING_VALUE);
            prefHelper_.setSessionParams(PrefHelper.NO_STRING_VALUE);
            prefHelper_.setIdentity(PrefHelper.NO_STRING_VALUE);
            prefHelper_.clearUserValues();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void handleFailure(int statusCode) {
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

    @Override
    public void clearCallbacks() {
        //No Implementation on purpose
    }
}
