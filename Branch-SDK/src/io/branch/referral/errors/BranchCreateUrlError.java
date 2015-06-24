package io.branch.referral.errors;

import io.branch.referral.BranchError;

/**
 * <p>{@link BranchError} class containing the message to display in logs when a Branch referral
 * URL could not be created. This is will usually be caused by a connectivity issue.</p>
 */
public class BranchCreateUrlError extends BranchError {
    @Override
    public String getMessage() {
        return "Trouble creating a URL. Check network connectivity and that you properly initialized";
    }
}
