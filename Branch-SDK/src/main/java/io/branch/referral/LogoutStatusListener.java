package io.branch.referral;

/**
 * <p>
 * Callback interface for listening logout status
 * </p>
 */
public interface LogoutStatusListener {
    /**
     * Called on finishing the the logout process
     *
     * @param loggedOut A {@link Boolean} which is set to true if logout succeeded
     * @param error     An instance of {@link BranchError} to notify any error occurred during logout.
     *                  A null value is set if logout succeeded.
     */
    void onLogoutFinished(boolean loggedOut, BranchError error);
}
