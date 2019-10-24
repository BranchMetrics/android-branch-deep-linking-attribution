package io.branch.referral;

import android.app.Application;

/**
 * <p>
 * Default Android Application class for  Branch SDK. You should use this as your application class
 * in your manifest if you are not creating an Application class. If you already have an Application
 * class then extend your Application class with this.
 * </p>
 * <p>
 * Add this entry to the manifest if you don't have an Application class :
 * </p>
 * <pre style="background:#fff;padding:10px;border:2px solid silver;">
 *      &lt;application
 *      -----
 *      android:name="io.branch.referral.BranchApp"&gt;
 *</pre>
 * <p>
 * Add your Branch keys to the manifest file.Use io.branch.sdk.
 * Use TestMode key to specify whether to use Branch test key or live key
 * </p>
 * <pre style="background:#fff;padding:10px;border:2px solid silver;">
 *      &lt;meta-data android:name="io.branch.sdk.TestMode" android:value="true" /&gt;
 *      &lt;meta-data android:name="io.branch.sdk.BranchKey" android:value="Your_Branch_Live_Key" /&gt;
 *      &lt;meta-data android:name="io.branch.sdk.BranchKey.test" android:value="Your_Branch_Test_Key" /&gt;
 * </pre>
 */
public class BranchApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        if (!BranchUtil.checkTestMode(this)) {
            Branch.getInstance(this);
        } else {
            Branch.getTestInstance(this);
        }
    }
}
