package io.branch.referral;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

/**
 * <p>
 * Default Android Application class for  Branch SDK. You should use this as your application class
 * in your manifest if you are not creating an Application class. If you already have an Application
 * class then extend your Application class with this.
 * </p>
 * <p/>
 * <p>
 * Add this entry to the manifest if you don't have an Application class :
 * </p>
 * <p/>
 * <pre style="background:#fff;padding:10px;border:2px solid silver;">
 *      <application
 *      -----
 *      android:name="io.branch.referal.BranchApp">
 *
 * <p>
 * Add your Branch keys to the manifest file.Use io.branch.sdk.
 * Use TestMode key to specify whether to use Branch test key or live key
 * </p>
 * <pre style="background:#fff;padding:10px;border:2px solid silver;">
 *      <meta-data android:name="io.branch.sdk.TestMode" android:value="true" />
 *      <meta-data android:name="io.branch.sdk.BranchKey" android:value="Your_Branch_Live_Key" />
 *      <meta-data android:name="io.branch.sdk.BranchKey.test" android:value="Your_Branch_Test_Key" />
 */
public class BranchApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        if (isTestModeEnabled() == false) {
            Branch.getInstance(this);
        } else {
            Branch.getTestInstance(this);
        }
    }

    /**
     * Get the value of "io.branch.sdk.TestMode" entry in application manifest.
     *
     * @return   value of "io.branch.sdk.TestMode" entry in application manifest.
     *          false if "io.branch.sdk.TestMode" is not added in the manifest.
     */

    private boolean isTestModeEnabled() {
        boolean isTestMode_ = false;
        String testModeKey = "io.branch.sdk.TestMode";
        try {
            final ApplicationInfo ai = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
            if (ai.metaData != null) {
                isTestMode_ = ai.metaData.getBoolean(testModeKey, false);
            }
        } catch (final PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return isTestMode_;
    }
}
