package io.branch.referral.serverrequest;

import android.app.Application;
import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

import io.branch.referral.Branch;
import io.branch.referral.BranchError;
import io.branch.referral.Defines;
import io.branch.referral.ServerRequest;
import io.branch.referral.ServerResponse;

/**
 * * <p>
 * The server request for retrieving rewards for the current session. Handles request creation and execution.
 * </p>
 */
public class GetRewardsRequest extends ServerRequest {

    Branch.BranchReferralStateChangedListener callback_;


    /**
     * <p>Create an instance of {@link GetRewardsRequest} to retrieve rewards for the current session,
     * with a callback to perform a predefined action following successful report of state change.
     * You'll then need to call getCredits in the callback to update the credit totals in your UX.</p>
     *
     * @param context  Current {@link Application} context
     * @param callback A {@link Branch.BranchReferralStateChangedListener} callback instance that will
     *                 trigger actions defined therein upon a referral state change.
     */
    public GetRewardsRequest(Context context, Branch.BranchReferralStateChangedListener callback) {
        super(context, Defines.RequestPath.GetCredits.getPath());
        callback_ = callback;
    }

    public GetRewardsRequest(String requestPath, JSONObject post, Context context) {
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
    public void handleFailure(int statusCode) {
        if (callback_ != null) {
            callback_.onStateChanged(false, new BranchError("Trouble retrieving user credits.", statusCode));
        }
    }

    @Override
    public boolean handleErrors(Context context) {
        if (!super.doesAppHasInternetPermission(context)) {
            callback_.onStateChanged(false, new BranchError("Trouble retrieving user credits.", BranchError.ERR_NO_INTERNET_PERMISSION));
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
