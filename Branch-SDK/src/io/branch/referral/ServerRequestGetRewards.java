package io.branch.referral;

import android.app.Application;
import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

/**
 * * <p>
 * The server request for retrieving rewards for the current session. Handles request creation and execution.
 * </p>
 */
class ServerRequestGetRewards extends ServerRequest {

    Branch.BranchReferralStateChangedListener callback_;

    /**
     * <p>Create an instance of {@link ServerRequestGetRewards} to retrieve rewards for the current session,
     * with a callback to perform a predefined action following successful report of state change.
     * You'll then need to call getCredits in the callback to update the credit totals in your UX.</p>
     *
     * @param context  Current {@link Application} context
     * @param callback A {@link Branch.BranchReferralStateChangedListener} callback instance that will
     *                 trigger actions defined therein upon a referral state change.
     */
    public ServerRequestGetRewards(Context context, Branch.BranchReferralStateChangedListener callback) {
        super(context, Defines.RequestPath.GetCredits.getPath());
        callback_ = callback;
    }

    public ServerRequestGetRewards(String requestPath, JSONObject post, Context context) {
        super(requestPath, post, context);
    }

    @Override
    public String getRequestUrl() {
        return super.getRequestUrl() + prefHelper_.getIdentityID();
    }

    @Override
    public void onRequestSucceeded(ServerResponse resp, Branch branch) {
        boolean updateListener = false;
        Iterator<?> keys = resp.getObject().keys();
        while (keys.hasNext()) {
            String key = (String) keys.next();

            try {
                int credits = resp.getObject().getInt(key);

                if (credits != prefHelper_.getCreditCount(key)) {
                    updateListener = true;
                }
                prefHelper_.setCreditCount(key, credits);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        if (callback_ != null) {
            callback_.onStateChanged(updateListener, null);
        }
    }

    @Override
    public void handleFailure(int statusCode, String causeMsg) {
        if (callback_ != null) {
            callback_.onStateChanged(false, new BranchError("Trouble retrieving user credits."+ causeMsg, statusCode));
        }
    }

    @Override
    public boolean handleErrors(Context context) {
        if (!super.doesAppHasInternetPermission(context)) {
            if (callback_ != null) {
                callback_.onStateChanged(false, new BranchError("Trouble retrieving user credits.", BranchError.ERR_NO_INTERNET_PERMISSION));
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean isGetRequest() {
        return true;
    }

    @Override
    public void clearCallbacks() {
        callback_ = null;
    }
}
