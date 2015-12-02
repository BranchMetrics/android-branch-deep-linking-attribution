package io.branch.referral;

import android.app.Application;
import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * * <p>
 * The server request for getting credit history. Handles request creation and execution.
 * </p>
 */
class ServerRequestGetRewardHistory extends ServerRequest {

    Branch.BranchListResponseListener callback_;

    /**
     * <p>Create an instance of {@link ServerRequestGetRewardHistory} to get the credit history of the specified
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
    public ServerRequestGetRewardHistory(Context context, String bucket, String afterId, int length,
                                         Branch.CreditHistoryOrder order, Branch.BranchListResponseListener callback) {

        super(context, Defines.RequestPath.GetCreditHistory.getPath());
        callback_ = callback;
        queueTimerId_ = 27;
        requestTimerId_ = 28;

        JSONObject getCreditHistoryPost = new JSONObject();
        try {
            getCreditHistoryPost.put(Defines.Jsonkey.IdentityID.getKey(), prefHelper_.getIdentityID());
            getCreditHistoryPost.put(Defines.Jsonkey.DeviceFingerprintID.getKey(), prefHelper_.getDeviceFingerPrintID());
            getCreditHistoryPost.put(Defines.Jsonkey.SessionID.getKey(), prefHelper_.getSessionID());
            if (!prefHelper_.getLinkClickID().equals(PrefHelper.NO_STRING_VALUE)) {
                getCreditHistoryPost.put(Defines.Jsonkey.LinkClickID.getKey(), prefHelper_.getLinkClickID());
            }
            getCreditHistoryPost.put(Defines.Jsonkey.Length.getKey(), length);
            getCreditHistoryPost.put(Defines.Jsonkey.Direction.getKey(), order.ordinal());

            if (bucket != null) {
                getCreditHistoryPost.put(Defines.Jsonkey.Bucket.getKey(), bucket);
            }

            if (afterId != null) {
                getCreditHistoryPost.put(Defines.Jsonkey.BeginAfterID.getKey(), afterId);
            }
            setPost(getCreditHistoryPost);
        } catch (JSONException ex) {
            ex.printStackTrace();
            constructError_ = true;
        }

    }

    public ServerRequestGetRewardHistory(String requestPath, JSONObject post, Context context) {
        super(requestPath, post, context);
    }

    @Override
    public void onRequestSucceeded(ServerResponse resp, Branch branch) {
        if (callback_ != null) {
            callback_.onReceivingResponse(resp.getArray(), null);
        }
    }

    @Override
    public void handleFailure(int statusCode, String causeMsg) {
        if (callback_ != null) {
            callback_.onReceivingResponse(null, new BranchError("Trouble retrieving user credit history. " + causeMsg, statusCode));
        }
    }

    @Override
    public boolean handleErrors(Context context) {
        if (!super.doesAppHasInternetPermission(context)) {
            if (callback_ != null) {
                callback_.onReceivingResponse(null, new BranchError("Trouble retrieving user credit history.", BranchError.ERR_NO_INTERNET_PERMISSION));
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
}
