package io.branch.branchandroidtestbed;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import io.branch.indexing.BranchUniversalObject;
import io.branch.referral.Branch;
import io.branch.referral.BranchError;
import io.branch.referral.BranchLogger;
import io.branch.referral.util.BranchEvent;
import io.branch.referral.util.LinkProperties;

/**
 * Test utility helpers extracted from MainActivity to reduce class size.
 * Used by E2E test infrastructure (MobileBoost/GPTDriver) and manual testing.
 */
public class TestBedHelper {

    /**
     * Shows a long-duration toast that displays twice (effectively ~6s visibility).
     * Useful for E2E test assertions where the AI driver needs time to read the toast.
     */
    public static void showLongToast(Context context, String message) {
        Toast toast = Toast.makeText(context.getApplicationContext(), message, Toast.LENGTH_LONG);
        toast.show();
        new Handler(Looper.getMainLooper()).postDelayed(toast::show, 3000);
    }

    /**
     * Initializes Branch session and creates test events after successful initialization.
     */
    public static void initializeSessionWithEventTests(Activity activity, int eventCount) {
        Branch.sessionBuilder(activity)
                .withCallback(new SessionInitHandler(activity, eventCount))
                .withData(activity.getIntent().getData())
                .init();
    }

    /**
     * Handler for Branch session initialization with event creation capability.
     */
    static class SessionInitHandler implements Branch.BranchUniversalReferralInitListener {
        private final Activity activity;
        private final int eventCount;

        SessionInitHandler(Activity activity, int eventCount) {
            this.activity = activity;
            this.eventCount = eventCount;
        }

        @Override
        public void onInitFinished(BranchUniversalObject branchUniversalObject, LinkProperties linkProperties, BranchError error) {
            if (error != null) {
                BranchLogger.d("branch init failed. Caused by -" + error.getMessage());
                return;
            }

            BranchLogger.d("branch init complete!");
            if (branchUniversalObject != null) {
                BranchLogger.d("title " + branchUniversalObject.getTitle());
                BranchLogger.d("CanonicalIdentifier " + branchUniversalObject.getCanonicalIdentifier());
                BranchLogger.d("metadata " + branchUniversalObject.getContentMetadata().convertToJson());
            }

            if (linkProperties != null) {
                BranchLogger.d("Channel " + linkProperties.getChannel());
                BranchLogger.d("control params " + linkProperties.getControlParams());
            }

            BranchLogger.d("Creating " + eventCount + " test events after session initialization");
            for (int i = 0; i < eventCount; i++) {
                new BranchEvent("Event " + i).logEvent(activity);
            }
        }
    }
}
