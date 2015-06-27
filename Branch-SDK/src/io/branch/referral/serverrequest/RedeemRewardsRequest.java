package io.branch.referral.serverrequest;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import io.branch.referral.Branch;
import io.branch.referral.BranchError;
import io.branch.referral.Defines;
import io.branch.referral.PrefHelper;
import io.branch.referral.ServerRequest;
import io.branch.referral.ServerResponse;

/**
 * * <p>
 * The server request for redeeming rewards. Handles request creation and execution.
 * </p>
 */
public class RedeemRewardsRequest extends ServerRequest {


    Branch.BranchReferralStateChangedListener callback_;
    int actualNumOfCreditsToRedeem_ = 0;

    /**
     * <p>Create an instance of {@link RedeemRewardsRequest} to Redeem the specified number of credits
     * from the named bucket, if there are sufficient credits within it. If the number to redeem exceeds
     * the number available in the bucket, all of the available credits will be redeemed instead.</p>
     *
     * @param context              Current {@link Application} context
     * @param bucketName           A {@link String} value containing the name of the referral bucket to attempt
     *                             to redeem credits from.
     * @param numOfCreditsToRedeem A {@link Integer} specifying the number of credits to attempt to redeem from
     *                             the specified bucket.
     * @param callback             A {@link Branch.BranchReferralStateChangedListener} callback instance that will
     *                             trigger actions defined therein upon a executing redeem rewards.
     */
    public RedeemRewardsRequest(Context context, String bucketName, int numOfCreditsToRedeem, Branch.BranchReferralStateChangedListener callback) {
        super(context, Defines.RequestPath.RedeemRewards.getPath());

        callback_ = callback;


        int availableCredits = prefHelper_.getCreditCount(bucketName);
        actualNumOfCreditsToRedeem_ = numOfCreditsToRedeem;
        if (numOfCreditsToRedeem > availableCredits) {
            actualNumOfCreditsToRedeem_ = availableCredits;
            Log.i("BranchSDK", "Branch Warning: You're trying to redeem more credits than are available. Have you updated loaded rewards");
        }
        if (actualNumOfCreditsToRedeem_ > 0) {
            JSONObject post = new JSONObject();
            try {
                post.put(Defines.Jsonkey.IdentityID.getKey(), prefHelper_.getIdentityID());
                post.put(Defines.Jsonkey.DeviceFingerprintID.getKey(), prefHelper_.getDeviceFingerPrintID());
                post.put(Defines.Jsonkey.SessionID.getKey(), prefHelper_.getSessionID());
                if (!prefHelper_.getLinkClickID().equals(PrefHelper.NO_STRING_VALUE)) {
                    post.put(Defines.Jsonkey.LinkClickID.getKey(), prefHelper_.getLinkClickID());
                }
                post.put(Defines.Jsonkey.Bucket.getKey(), bucketName);
                post.put(Defines.Jsonkey.Amount.getKey(), numOfCreditsToRedeem);
                setPost(post);
            } catch (JSONException ex) {
                ex.printStackTrace();
                constructError_ = true;
            }
        }

    }

    public RedeemRewardsRequest(String requestPath, JSONObject post, Context context) {
        super(requestPath, post, context);
    }

    @Override
    public boolean handleErrors(Context context) {
        if (!super.doesAppHasInternetPermission(context)) {
            callback_.onStateChanged(false, new BranchError("Trouble redeeming rewards.", BranchError.ERR_NO_INTERNET_PERMISSION));
            return true;
        }
        if (actualNumOfCreditsToRedeem_ <= 0) {
            callback_.onStateChanged(false, new BranchError("Trouble redeeming rewards.", BranchError.ERR_BRANCH_REDEEM_REWARD));
            return true;
        }
        return false;
    }

    @Override
    public void onRequestSucceeded(ServerResponse resp, Branch branch) {
        boolean isRedemptionSucceeded = false;
        JSONObject post = getPost();
        if (post != null) {
            if (post.has("bucket") && post.has("amount")) {
                try {
                    int redeemedCredits = post.getInt("amount");
                    String creditBucket = post.getString("bucket");
                    isRedemptionSucceeded = redeemedCredits > 0;

                    int updatedCreditCount = prefHelper_.getCreditCount(creditBucket) - redeemedCredits;
                    prefHelper_.setCreditCount(creditBucket, updatedCreditCount);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        if (callback_ != null) {
            BranchError branchError = isRedemptionSucceeded ? null : new BranchError("Trouble redeeming rewards.", BranchError.ERR_BRANCH_REDEEM_REWARD);
            callback_.onStateChanged(isRedemptionSucceeded, branchError);
        }

    }

    @Override
    public void handleFailure(int statusCode) {
        if (callback_ != null) {
            callback_.onStateChanged(false, new BranchError("Trouble redeeming rewards.", statusCode));
        }
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
