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
import io.branch.referral.errors.BranchGetCreditHistoryError;
import io.branch.referral.errors.BranchInternetPermissionError;
import io.branch.referral.errors.BranchNotInitError;

/**
 * * <p>
 * The server request for getting credit history. Handles request creation and execution.
 * </p>
 */
public class GetRewardHistoryRequest extends ServerRequest {

    Branch.BranchListResponseListener callback_;

    /**
     * <p>Create an instance of {@link GetRewardHistoryRequest} to get the credit history of the specified
     * bucket and triggers a callback to handle the response.</p>
     *
     * @param context  Current {@link Application} context
     * @param bucket   A {@link String} value containing the name of the referral bucket that the
     *                 code will belong to.
     * @param afterId  A {@link String} value containing the ID of the history record to begin after.
     *                 This allows for a partial history to be retrieved, rather than the entire
     *                 credit history of the bucket.
     * @param length   A {@link Integer} value containing the number of credit history records to
     *                 return.
     * @param order    A {@link Branch.CreditHistoryOrder} object indicating which order the results should
     *                 be returned in.
     *                 <p/>
     *                 <p>Valid choices:</p>
     *                 <p/>
     *                 <ul>
     *                 <li>{@link Branch.CreditHistoryOrder#kMostRecentFirst}</li>
     *                 <li>{@link Branch.CreditHistoryOrder#kLeastRecentFirst}</li>
     *                 </ul>
     * @param callback A {@link Branch.BranchListResponseListener} callback instance that will trigger
     *                 actions defined therein upon receipt of a response to a create link request.
     */
    public GetRewardHistoryRequest(Context context, String bucket, String afterId, int length,
                                   Branch.CreditHistoryOrder order, Branch.BranchListResponseListener callback) {

        super(context, Defines.RequestPath.GetCreditHistory.getPath());
        callback_ = callback;

        JSONObject getCreditHistoryPost = new JSONObject();
        try {
            getCreditHistoryPost.put("identity_id", prefHelper_.getIdentityID());
            getCreditHistoryPost.put("device_fingerprint_id", prefHelper_.getDeviceFingerPrintID());
            getCreditHistoryPost.put("session_id", prefHelper_.getSessionID());
            if (!prefHelper_.getLinkClickID().equals(PrefHelper.NO_STRING_VALUE)) {
                getCreditHistoryPost.put("link_click_id", prefHelper_.getLinkClickID());
            }
            getCreditHistoryPost.put("length", length);
            getCreditHistoryPost.put("direction", order.ordinal());

            if (bucket != null) {
                getCreditHistoryPost.put("bucket", bucket);
            }

            if (afterId != null) {
                getCreditHistoryPost.put("begin_after_id", afterId);
            }
            setPost(getCreditHistoryPost);
        } catch (JSONException ex) {
            ex.printStackTrace();
            constructError_ = true;
        }

    }


    public GetRewardHistoryRequest(String requestPath, JSONObject post, Context context) {
        super(requestPath, post, context);
    }

    @Override
    public void onRequestSucceeded(ServerResponse resp, Branch branch) {
        if (callback_ != null) {
            callback_.onReceivingResponse(resp.getArray(), null);
        }
    }


    @Override
    public void handleFailure(boolean isInitNotStarted) {
        if (callback_ != null) {
            if (isInitNotStarted)
                callback_.onReceivingResponse(null, new BranchNotInitError());
            else
                callback_.onReceivingResponse(null, new BranchGetCreditHistoryError());
        }
    }

    @Override
    public boolean handleErrors(Context context) {
        if (!super.doesAppHasInternetPermission(context)) {
            callback_.onReceivingResponse(null, new BranchInternetPermissionError());
            return true;
        }
        return false;
    }

    @Override
    public boolean isGetRequest() {
        return false;
    }
}
