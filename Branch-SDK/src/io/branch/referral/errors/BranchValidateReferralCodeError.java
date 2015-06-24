package io.branch.referral.errors;

import io.branch.referral.BranchError;

/**
 * <p>{@link BranchError} class containing the message to display in logs where the referral
 * code cannot be validated due to a lack of communication, or valid response from, the Branch
 * server.</p>
 */
public class BranchValidateReferralCodeError extends BranchError {
    @Override
    public String getMessage() {
        return "Trouble validating the referral code. Check network connectivity and that you properly initialized";
    }
}