package io.branch.referral.errors;

import android.app.Activity;
import android.net.Uri;

import io.branch.referral.BranchError;
import io.branch.referral.SystemObserver;

/**
 * <p>{@link BranchError} class containing the message to display in logs when calls have been
 * made to apply a referral code, but the Branch object has not been properly initialised or
 * cannot contact the server due to a network connectivity issue.</p>
 * <p/>
 * <p>See the <a href="https://github.com/BranchMetrics/Branch-Integration-Guides/blob/master/android-quick-start.md#step-4---create-a-branch-session">
 * Android Quick Start guide</a> for detailed instructions on integrating the SDK correctly.</p>
 *
 * @see SystemObserver#getWifiConnected()
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
public class BranchApplyReferralCodeError extends BranchError {
    @Override
    public String getMessage() {
        return "Trouble applying the referral code. Check network connectivity and that you properly initialized";
    }
}