package io.branch.referral.errors;

import io.branch.referral.BranchError;

/**
 * <p>{@link BranchError} class containing the message to display in logs where the same
 * referral code has been applied already, potentially identifying an erroneously repeated
 * code block or poorly implemented loop.</p>
 */
public class BranchDuplicateReferralCodeError extends BranchError {
    @Override
    public String getMessage() {
        return "That Branch referral code is already in use";
    }
}
