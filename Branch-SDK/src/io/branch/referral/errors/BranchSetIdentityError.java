package io.branch.referral.errors;

import io.branch.referral.BranchError;

/**
 * <p>{@link BranchError} class containing the message to display in logs in cases where the
 * user alias cannot be set. This can occur where a poor quality
 * connection is losing packets containing the alias setting request or response.</p>
 */
public class BranchSetIdentityError extends BranchError {
    @Override
    public String getMessage() {
        return "Trouble setting the user alias. Check network connectivity and that you properly initialized";
    }
}
