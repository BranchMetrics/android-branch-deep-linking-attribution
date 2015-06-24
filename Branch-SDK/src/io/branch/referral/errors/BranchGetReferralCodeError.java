package io.branch.referral.errors;

import io.branch.referral.BranchError;

/**
 * <p>{@link BranchError} class containing the message to display in logs where the referral
 * code has not been received properly by the server. This can occur where a poor quality
 * connection is losing packets containing the full referral code submission request or
 * response.</p>
 */
public class BranchGetReferralCodeError extends BranchError {
    @Override
    public String getMessage() {
        return "Trouble retrieving the referral code. Check network connectivity and that you properly initialized";
    }
}