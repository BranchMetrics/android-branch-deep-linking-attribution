package io.branch.referral.errors;

import io.branch.referral.BranchError;

/**
 * <p>{@link BranchError} class containing the message to display in logs where an alias request
 * has been submitted that has different parameters attached. This indicates that either there
 * is missing information from the alias request, or that the same alias has been requested
 * before by a different owner.</p>
 */
public class BranchDuplicateUrlError extends BranchError {
    @Override
    public String getMessage() {
        return "Trouble creating a URL with that alias. If you want to reuse the alias, make sure to submit the same properties for all arguments and that the user is the same owner";
    }
}