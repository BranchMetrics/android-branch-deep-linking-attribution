package io.branch.referral.errors;

import android.app.Activity;
import android.net.Uri;

import io.branch.referral.BranchError;

/**
 * <p>{@link BranchError} class containing the message to display in logs for when calls have
 * been made to a Branch object when a connection has not been established.</p>
 * <p/>
 * <p>The first call required when a Branch object is instantiated is {@link #initSession()},
 * or one of its relatives (see below referenced methods). If this has not been done pending
 * calls cannot be queued up, so this error is thrown in order to notify the developer/tester
 * via debug logs that methods have been called out of sequence so that the implementation has
 * been corrected.</p>
 * <p/>
 * <p>See the <a href="https://github.com/BranchMetrics/Branch-Integration-Guides/blob/master/android-quick-start.md#step-4---create-a-branch-session">
 * Android Quick Start guide</a> for detailed instructions on integrating the SDK correctly.</p>
 *
 * @see Branch#initSession(BranchReferralInitListener)
 * @see Branch#initSession(BranchReferralInitListener, Activity)
 * @see Branch#initSession(BranchReferralInitListener, Uri)
 * @see Branch#initSession(BranchReferralInitListener, Uri, Activity)
 * @see Branch#initSession()
 * @see Branch#initSession(Activity)
 * @see Branch#initSessionWithData(Uri)
 * @see Branch#initSessionWithData(Uri, Activity)
 * @see Branch#initSession(boolean)
 * @see Branch#initSession(boolean, Activity)
 * @see Branch#initSession(BranchReferralInitListener, boolean, Uri)
 * @see Branch#initSession(BranchReferralInitListener, boolean, Uri, Activity)
 * @see Branch#initSession(BranchReferralInitListener, boolean)
 * @see Branch#initSession(BranchReferralInitListener, boolean, Activity)
 */
public class BranchNotInitError extends BranchError {
    @Override
    public String getMessage() {
        return "Did you forget to call init? Make sure you init the session before making Branch calls";
    }
}