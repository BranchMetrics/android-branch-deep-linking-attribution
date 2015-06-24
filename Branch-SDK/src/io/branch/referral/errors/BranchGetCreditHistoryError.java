package io.branch.referral.errors;

import io.branch.referral.BranchError;

/**
 * <p>{@link BranchError} class containing the message to display in logs where a request to the
 * server to fetch a user's credit history has failed due to poor connectivity or an internal
 * system error.</p>
 */
public class BranchGetCreditHistoryError extends BranchError {
    @Override
    public String getMessage() {
        return "Trouble retrieving user credit history. Check network connectivity and that you properly initialized";
    }
}