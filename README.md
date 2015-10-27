# Branch Metrics Android SDK

This is the readme file of our open source Android SDK. There's a full demo app embedded in this repository, but you should also check out our live demo: [Branch Monster Factory](https://play.google.com/store/apps/details?id=io.branch.branchster). We've [open sourced the Branchster's app](https://github.com/BranchMetrics/Branchster-Android) as well if you'd like to dig in.

## [SDK Implementation Guide](https://dev.branch.io) and [support portal with user forums](http://support.branch.io)

=======




### Configure your AndroidManifest.xml
**1. Provide internet permission. Branch SDK need internet access to talk to Branch APIs.**

**2. Specify a versionName attribute in the manifest for Branch to identify the application version.**

**3. Add your Branch key to your project.**

After you register your app, your Branch key can be retrieved on the [Settings](https://dashboard.branch.io/#/settings) page of the dashboard. Add it (them, if you want to do it for both your live and test apps) to your project's manifest file as a meta data.

 Edit your manifest file to have the above items
```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="io.branch.sample"
    android:versionCode="1"
    android:versionName="1.0" >

    <uses-permission android:name="android.permission.INTERNET" />

    <application>
        <!-- Other existing entries -->

        <!-- Add this meta-data below, and change "key_live_xxxxxxx" to your actual live Branch key -->
        <meta-data android:name="io.branch.sdk.BranchKey" android:value="key_live_xxxxxxx" />

        <!-- For your test app, if you have one; Again, use your actual test Branch key -->
        <meta-data android:name="io.branch.sdk.BranchKey.test" android:value="key_test_yyyyyyy" />
    </application>
</manifest>
```

### Proguard settings
If your Application is enabled with proguard, add the following instruction to your proguard.cfg file:
```bash
-keep class com.google.android.gms.ads.identifier.** { *; }
```



Once you do any of the below, there is no need to close or init sessions in your Activities. Branch SDK will do all that for you. You can get your Branch instance at any time as follows.

If you are not creating or using an Application class throughout your project, all you need to do is declare `BranchApp` as your application class in your manifest.

```xml
 <application
    android:name="io.branch.referral.BranchApp">
```

