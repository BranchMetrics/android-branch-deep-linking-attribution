package io.branch.referral.errors;

import io.branch.referral.BranchError;

/**
 * <p>{@link BranchError} class containing the message to display in logs where a request to the
 * server to redeem user's reward has failed since user doesn't have credits available to redeem.
 * </p>
 */
public class BranchRedeemRewardsError extends BranchError {
    @Override
    public String getMessage() {
        return "Trouble redeeming rewards. Please make sure you have credits available to redeem";
    }
}