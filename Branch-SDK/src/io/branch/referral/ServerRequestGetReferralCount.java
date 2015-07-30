package io.branch.referral;

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
 * The server request for getting referral count. Handles request creation and execution.
 * </p>
 */
class ServerRequestGetReferralCount extends ServerRequest {
    Branch.BranchReferralStateChangedListener callback_;

    /**
     * p>Create an instance of ServerRequestGetReferralCount to get the number of referrals.</p>
     *
     * @param context  Current {@link Application} context
     * @param callback A {@link Branch.BranchReferralStateChangedListener} callback instance that will
     *                 trigger actions defined therein upon receipt of a response to a referral count request.
     */
    public ServerRequestGetReferralCount(Context context, Branch.BranchReferralStateChangedListener callback) {
        super(context, Defines.RequestPath.Referrals.getPath());
        callback_ = callback;
    }

    public ServerRequestGetReferralCount(String requestPath, JSONObject post, Context context) {
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
                JSONObject counts = resp.getObject().getJSONObject(key);
                int total = counts.getInt(Defines.Jsonkey.Total.getKey());
                int unique = counts.getInt(Defines.Jsonkey.Unique.getKey());

                if (total != prefHelper_.getActionTotalCount(key) || unique != prefHelper_.getActionUniqueCount(key)) {
                    updateListener = true;
                }
                prefHelper_.setActionTotalCount(key, total);
                prefHelper_.setActionUniqueCount(key, unique);
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
            callback_.onStateChanged(false, new BranchError("Trouble retrieving referral counts.", statusCode));
        }
    }

    @Override
    public boolean handleErrors(Context context) {
        if (!super.doesAppHasInternetPermission(context)) {
            callback_.onStateChanged(false, new BranchError("Trouble retrieving referral counts.", BranchError.ERR_NO_INTERNET_PERMISSION));
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
