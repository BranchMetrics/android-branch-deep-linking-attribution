package io.branch.branchandroidtestbed;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import io.branch.referral.Branch;
import io.branch.referral.BranchLogger;
import io.branch.referral.util.BranchEvent;

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
     * Logs test events to exercise the user agent fetch under load.
     *
     * @param activity   the activity the events are logged from
     * @param eventCount how many test events to log
     */
    public static void logTestEvents(Activity activity, int eventCount) {
        BranchLogger.d("Creating " + eventCount + " test events");
        for (int i = 0; i < eventCount; i++) {
            new BranchEvent("Event " + i).logEvent(activity);
        }
    }

}
