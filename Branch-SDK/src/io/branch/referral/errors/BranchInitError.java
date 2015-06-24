package io.branch.referral.errors;

import io.branch.referral.BranchError;

/**
 * <p>{@link BranchError} class containing the message to display in logs where the Branch
 * initialisation process has failed due to poor connectivity, or because the App Key in use in
 * the current application is misconfigured. This can occur when there are invalid characters in
 * the App Key variable, where the variable itself is empty, or if the App Key in use does not
 * belong to an application registered in the Branch dashboard.</p>
 * <p/>
 * <p>To confirm that you are using the correct App Key for your project, visit the
 * <a href="https://dashboard.branch.io/#/settings">
 * Branch Dashboard Settings</a> page, or refer to the <a href="https://github.com/BranchMetrics/Branch-Integration-Guides/blob/master/android-quick-start.md">
 * Android Quick-Start Guide</a> to a walk through of the full process for getting your project
 * up and running with Branch.</p>
 *
 * @see <a href="https://github.com/BranchMetrics/Branch-Integration-Guides/blob/master/android-quick-start.md">Android Quick-Start Guide</a>
 * @see <a href="https://dashboard.branch.io/">Branch Dashboard</a>
 */
public class BranchInitError extends BranchError {
    @Override
    public String getMessage() {
        return "Trouble initializing Branch. Check network connectivity or that your branch key is valid";
    }
}