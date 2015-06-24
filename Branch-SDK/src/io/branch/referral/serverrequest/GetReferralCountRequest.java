package io.branch.referral.serverrequest;

import android.app.Application;
import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

import io.branch.referral.Branch;
import io.branch.referral.Defines;
import io.branch.referral.ServerRequest;
import io.branch.referral.ServerResponse;
import io.branch.referral.errors.BranchGetReferralsError;
import io.branch.referral.errors.BranchNotInitError;

/**
 * * <p>
 * The server request for getting referral count. Handles request creation and execution.
 * </p>
 */
public class GetReferralCountRequest extends ServerRequest {
    Branch.BranchReferralStateChangedListener callback_;

    /**
     * p>Create an instance of GetReferralCountRequest to get the number of referrals.</p>
     *
     * @param context  Current {@link Application} context
     * @param callback A {@link Branch.BranchReferralStateChangedListener} callback instance that will
     *                 trigger actions defined therein upon receipt of a response to a	referral count request.
     */
    public GetReferralCountRequest(Context context, Branch.BranchReferralStateChangedListener callback) {
        super(context, Defines.RequestPath.Referrals.getPath());
        callback_ = callback;
    }

    public GetReferralCountRequest(String requestPath, JSONObject post, Context context) {
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
                int total = counts.getInt("total");
                int unique = counts.getInt("unique");

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
    public void handleFailure(boolean isInitNotStarted) {
        if (callback_ != null) {
            if (isInitNotStarted)
                callback_.onStateChanged(false, new BranchNotInitError());
            else
                callback_.onStateChanged(false, new BranchGetReferralsError());
        }
    }

    @Override
    public boolean hasErrors() {
        return false;
    }

    @Override
    public boolean isGetRequest() {
        return true;
    }

}
