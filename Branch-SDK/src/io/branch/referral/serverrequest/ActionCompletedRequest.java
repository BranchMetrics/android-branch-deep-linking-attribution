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

/**
 * * <p>
 * The server request for Action completed event. Handles request creation and execution.
 * </p>
 */
public class ActionCompletedRequest extends ServerRequest {

    /**
     * <p>Creates an ActionCompleteRequest instance. This request take care of reporting specific user
     * actions to Branch API, with additional app-defined meta data to go along with that action.</p>
     *
     * @param context  Current {@link Application} context
     * @param action   A {@link String} value to be passed as an action that the user has carried
     *                 out. For example "registered" or "logged in".
     * @param metadata A {@link JSONObject} containing app-defined meta-data to be attached to a
     *                 user action that has just been completed.
     */
    public ActionCompletedRequest(Context context, String action, JSONObject metadata) {
        super(context, Defines.RequestPath.CompletedAction.getPath());
        JSONObject post = new JSONObject();

        try {
            post.put("identity_id", prefHelper_.getIdentityID());
            post.put("device_fingerprint_id", prefHelper_.getDeviceFingerPrintID());
            post.put("session_id", prefHelper_.getSessionID());
            if (!prefHelper_.getLinkClickID().equals(PrefHelper.NO_STRING_VALUE)) {
                post.put("link_click_id", prefHelper_.getLinkClickID());
            }
            post.put("event", action);
            if (metadata != null)
                post.put("metadata", metadata);

            setPost(post);
        } catch (JSONException ex) {
            ex.printStackTrace();
            constructError_ = true;
        }
    }

    public ActionCompletedRequest(String requestPath, JSONObject post, Context context) {
        super(requestPath, post, context);
    }

    @Override
    public void onRequestSucceeded(ServerResponse resp, Branch branch) {
        //No implementation on purpose
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
        //No implementation on purpose
    }

    @Override
    public boolean shouldRetryOnFail() {
        return true;   //Action completed request need to retry on failure.
    }
}
