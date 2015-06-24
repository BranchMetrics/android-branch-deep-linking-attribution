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

/**
 * * <p>
 * The server request for closing any open session. Handles request creation and execution.
 * </p>
 */
public class RegisterCloseRequest extends ServerRequest {

    /**
     * <p>Perform the state-safe actions required to terminate any open session, and report the
     * closed application event to the Branch API.</p>
     *
     * @param context Current {@link Application} context
     */
    public RegisterCloseRequest(Context context) {
        super(context, Defines.RequestPath.RegisterClose.getPath());
        JSONObject closePost = new JSONObject();
        try {
            closePost.put("device_fingerprint_id", prefHelper_.getDeviceFingerPrintID());
            closePost.put("identity_id", prefHelper_.getIdentityID());
            closePost.put("session_id", prefHelper_.getSessionID());
            if (!prefHelper_.getLinkClickID().equals(PrefHelper.NO_STRING_VALUE)) {
                closePost.put("link_click_id", prefHelper_.getLinkClickID());
            }
            setPost(closePost);
        } catch (JSONException ex) {
            ex.printStackTrace();
            constructError_ = true;
        }
    }

    public RegisterCloseRequest(String requestPath, JSONObject post, Context context) {
        super(requestPath, post, context);
    }

    @Override
    public boolean hasErrors() {
        return false;
    }

    @Override
    public void onRequestSucceeded(ServerResponse resp, Branch branch) {
        //No Implementation on purpose
    }

    @Override
    public void handleFailure(boolean isInitNotStarted) {
        //No implementation on purpose
    }

    @Override
    public boolean isGetRequest() {
        return false;
    }
}
