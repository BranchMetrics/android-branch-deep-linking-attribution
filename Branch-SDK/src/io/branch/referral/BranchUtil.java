package io.branch.referral;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

/**
 * Class for Branch utility methods
 */
class BranchUtil {

    /**
     * Get the value of "io.branch.sdk.TestMode" entry in application manifest.
     *
     * @return value of "io.branch.sdk.TestMode" entry in application manifest.
     * false if "io.branch.sdk.TestMode" is not added in the manifest.
     */
    public static boolean isTestModeEnabled(Context context) {
        boolean isTestMode_ = false;
        String testModeKey = "io.branch.sdk.TestMode";
        try {
            final ApplicationInfo ai = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            if (ai.metaData != null) {
                isTestMode_ = ai.metaData.getBoolean(testModeKey, false);
            }
        } catch (final PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return isTestMode_;
    }
}
