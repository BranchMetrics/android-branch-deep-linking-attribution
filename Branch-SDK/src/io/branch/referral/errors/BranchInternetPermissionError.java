package io.branch.referral.errors;

import io.branch.referral.BranchError;

/**
 * <p>{@link BranchError} class containing the message to display in logs if the application is not
 * having internet permission. Application should have internet permission inorder to execute any Branch API
 * </p>
 */
public class BranchInternetPermissionError extends BranchError {
    @Override
    public String getMessage() {
        return "Trouble executing your request. Please add 'android.permission.INTERNET' in your applications manifest file";
    }
}