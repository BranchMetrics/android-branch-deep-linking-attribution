package io.branch.referral.errors;

import io.branch.referral.BranchError;

/**
 * <p>{@link BranchError} class containing the message to display in logs where the
 * referral code is invalid, suggesting an implementation error in handling generated codes, or
 * input validation failure where the code is input manually by the user.</p>
 */
public class BranchInvalidReferralCodeError extends BranchError {
    @Override
    public String getMessage() {
        return "That Branch referral code was invalid";
    }
}