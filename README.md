# Branch Metrics Android SDK

This is a repository of our open source Android SDK, and the information presented here serves as a reference manual for our Android SDK. See the table of contents below for a complete list of the content featured in this document.

## Get the Demo App

This is the readme file of our open source Android SDK. There's a full demo app embedded in this repository, but you should also check out our live demo: [Branch Monster Factory](https://play.google.com/store/apps/details?id=io.branch.branchster). We've [open sourced the Branchster's app](https://github.com/BranchMetrics/Branchster-Android) as well if you'd like to dig in.

## Additional Resources
- [Integration guide](https://dev.branch.io/recipes/quickstart_guide/android/) *Start Here*
- [Changelog](https://github.com/BranchMetrics/Android-Deferred-Deep-Linking-SDK/blob/master/ChangeLog.md)
- [Testing](https://dev.branch.io/recipes/testing_your_integration/android/)
- [Support portal, FAQ](http://support.branch.io)

## Installation

Current compiled SDK footprint is *40kb*

### Install library project

Just add `compile 'io.branch.sdk.android:library:1.+'` to the dependencies section of your `build.gradle` file.

Or download the JAR file from here:
https://s3-us-west-1.amazonaws.com/branchhost/Branch-Android-SDK.zip

The testbed project:
https://s3-us-west-1.amazonaws.com/branchhost/Branch-Android-TestBed.zip

Or just clone this project!

### Register Your App

You can sign up for your own app id at [https://dashboard.branch.io](https://dashboard.branch.io)

### Register an activity for direct deep linking (optional but recommended)

In your project's manifest file, you can register your app to respond to direct deep links (yourapp:// in a mobile browser) by adding the second intent filter block. Also, make sure to change **yourapp** to a unique string that represents your app name.

Secondly, make sure that this activity is launched as a singleTask. This is important to handle proper deep linking from other apps like Facebook.

Typically, you would register some sort of splash activitiy that handles routing for your app.

```xml
<activity
    android:name="com.yourapp.SplashActivity"
    android:label="@string/app_name"
    <!-- Make sure the activity is launched as "singleTask" -->
    android:launchMode="singleTask"
     >
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>

    <!-- Add this intent filter below, and change yourapp to your app name -->
    <intent-filter>
        <data android:scheme="yourapp" android:host="open" />
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
    </intent-filter>
</activity>
```
### Leverage Android App Links for deep linking

If you are building applications targeting for Android M or above, Branch make it really easy to configure your app for deep linking using App Links.
In your project's manifest file, you can register activities to for App Linking by adding an intent filter as follows. You only need to know `live_app_alpha_encoded_id`
and `test_app_alpha_encoded_id` which you can obtain from the Branch dash board once you enable App Linking support for your application.

```xml
<activity android:name="com.yourapp.your_activity">
    <!-- App Link your activity to Branch links-->
    <intent-filter android:autoVerify="true">
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
         <data android:scheme="https" android:host="bnc.lt" android:pathPrefix="/live_app_alpha_encoded_id" /> <!-- live_app_alpha_encoded_id can be obtained from the Branch Dashboard here: https://dashboard.branch.io/#/settings/link -->
         <data android:scheme="https" android:host="bnc.lt" android:pathPrefix="/test_app_alpha_encoded_id" /> <!-- test_app_alpha_encoded_id can be obtained from the Branch Dashboard here: https://dashboard.branch.io/#/settings/link -->
        <!-- If you set up a white label for your links in your Branch link settings then  only need to add the white label domain -->
        <data android:scheme="https" android:host="your.app.com"/>
    </intent-filter>
</activity>
```

That's all you need. Deep linked parameters associated with the link is passed through Branch initialization process.

Note: While using App Links please make sure you have registered the Activity for deep linking using Branch URI scheme as discussed in the previous session inorder to get deep link work on previous versions of Android
(which does not support App Links).

### Configure your AndroidManifest.xml

Note: Provide internet permission. Branch SDK need internet access to talk to Branch APIs.

#### Add your Branch key to your project.

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

## Initialization

Branch must be notified when the app opens and when it closes, so that we know when to query the API for a new deep link. We recently discovered an Android mechanism that was exposed in version 14, that allows us to track behind-the-scenes when the app is opened and closed. It makes the integration **a lot** easier, so we've split it out from the legacy integration.

If you support below 14, you'll want to skip this section and head to [this one right below](initialization-to-support-android-pre-14-harder).

### Initialization to support Android 14+ (4.0+) (easy)

To receive the deep link parameters from the Branch SDK, call initSession and pass in the BranchReferralInitListener. This will return the dictionary of referringParams associated with the link that was just clicked. You can call this anywhere at any time to get the params.

If you need to support pre 14, Branch must know when the app opens or closes to properly handle the deep link parameters retrieval. You can see more details on how to do this at [this docs site](https://dev.branch.io/recipes/quickstart_guide/android/#initialization-to-support-android-pre-14). If you don't close the Branch session, you'll see strange behaviors like deep link parameters not showing up after clicking a link the second time.

#### Initialize Branch lifecycle

Initialising and closing session is done automatically with our new _automatic session management_. Automatic session management can work only with API level 14 and above. Once you do any of the below, there is no need to close or init sessions in your Activities. Branch SDK will do all that for you. You can get your Branch instance at any time as follows.

##### Alt 1: You Have A Custom Application Class

If you have a custom Application class, just add this call in `onCreate`

```java
public void onCreate() {
    super.onCreate();
    Branch.getAutoInstance(this);
}
```

##### Alt 2: You Don't Have A Custom Application Class

If you are not creating or using an Application class throughout your project, all you need to do is declare `BranchApp` as your application class in your manifest.

```xml
 <application
    android:name="io.branch.referral.BranchApp">
```

#### Register deep link router

```java
Branch branch = Branch.getInstance(getApplicationContext());
branch.initSession(new BranchReferralInitListener(){
    @Override
    public void onInitFinished(JSONObject referringParams, BranchError error) {
        if (error == null) {
            // params are the deep linked params associated with the link that the user clicked -> was re-directed to this app
            // params will be empty if no data found
            // ... insert custom logic here ...
        } else {
            Log.i("MyApp", error.getMessage());
        }
    }
}, this.getIntent().getData(), this);
```

**NOTE** if you're calling this inside a fragment, please use getActivity() instead of passing in `this`. Also, `this.getIntent().getData()` refers to the data associated with an incoming intent.

Next, you'll need to hook into the `onNewIntent` method specified inside the Activity lifecycle and set the intent. This is required for conformity with Facebook's AppLinks. Verify that the activity you're implementing has *launchMode* set to *singleTask* inside the Manifest declaration. Once that is done, go to said activity and do something like the following:

```java
@Override
public void onNewIntent(Intent intent) {
    this.setIntent(intent);
}
```

##### Branch-provided data parameters in initSession callback

Previously, Branch did not return any information to the app if `initSession` was called but the user hadn't clicked on a link. Now Branch returns explicit parameters every time. Here is a list, and a description of what each represents.

* `~` denotes analytics
* `+` denotes information added by Branch
* (for the curious, `$` denotes reserved keywords used for controlling how the Branch service behaves)

| **Parameter** | **Meaning**
| --- | ---
| ~channel | The channel on which the link was shared, specified at link creation time
| ~feature | The feature, such as `invite` or `share`, specified at link creation time
| ~tags | Any tags, specified at link creation time
| ~campaign | The campaign the link is associated with, specified at link creation time
| ~stage | The stage, specified at link creation time
| ~creation_source | Where the link was created ('API', 'Dashboard', 'SDK', 'iOS SDK', 'Android SDK', or 'Web SDK')
| +match_guaranteed | True or false as to whether the match was made with 100% accuracy
| +referrer | The referrer for the link click, if a link was clicked
| +phone_number | The phone number of the user, if the user texted himself/herself the app
| +is_first_session | Denotes whether this is the first session (install) or any other session (open)
| +clicked_branch_link | Denotes whether or not the user clicked a Branch link that triggered this session
| +click_timestamp | Epoch timestamp of when the click occurred

#### Retrieve session (install or open) parameters

These session parameters will be available at any point later on with this command. If no params, the dictionary will be empty. This refreshes with every new session (app installs AND app opens)
```java
Branch branch = Branch.getInstance(getApplicationContext());
JSONObject sessionParams = branch.getLatestReferringParams();
```

#### Retrieve install (install only) parameters

If you ever want to access the original session params (the parameters passed in for the first install event only), you can use this line. This is useful if you only want to reward users who newly installed the app from a referral link or something.
```java
Branch branch = Branch.getInstance(getApplicationContext());
JSONObject installParams = branch.getFirstReferringParams();
```

### Persistent identities

Often, you might have your own user IDs, or want referral and event data to persist across platforms or uninstall/reinstall. It's helpful if you know your users access your service from different devices. This where we introduce the concept of an 'identity'.

To identify a user, just call:
```java
Branch branch = Branch.getInstance(getApplicationContext());
branch.setIdentity(your user id); // your user id should not exceed 127 characters
```

#### Logout

If you provide a logout function in your app, be sure to clear the user when the logout completes. This will ensure that all the stored parameters get cleared and all events are properly attributed to the right identity.

**Warning** this call will clear the referral credits and attribution on the device.

```java
Branch.getInstance(getApplicationContext()).logout();
```

### Register custom events

```java
Branch branch = Branch.getInstance(getApplicationContext());
branch.userCompletedAction("your_custom_event"); // your custom event name should not exceed 63 characters
```

OR if you want to store some state with the event

```java
Branch branch = Branch.getInstance(getApplicationContext());
branch.userCompletedAction("your_custom_event", (JSONObject)appState); // same 63 characters max limit
```

Some example events you might want to track:
```java
"complete_purchase"
"wrote_message"
"finished_level_ten"
```

## Branch Universal Object (for deep links, content analytics and indexing)

As more methods have evolved in Android, we've found that it was increasingly hard to manage them all. We abstracted as many as we could into the concept of a Branch Universal Object. This is the object that is associated with the thing you want to share (content or user). You can set all the metadata associated with the object and then call action methods on it to get a link or register a view.

### Defining the Branch Universal Object

The universal object is where you define all of the custom metadata associated with the content that you want to link to or index. Please use the builder format below to create one.

```java
 BranchUniversalObject branchUniversalObject = new BranchUniversalObject()

 			// The identifier is what Branch will use to de-dupe the content across many different Universal Objects
            .setCanonicalIdentifier("item/12345")

            // This is where you define the open graph structure and how the object will appear on Facebook or in a deepview
            .setTitle("My Content Title")
            .setContentDescription("My Content Description")
            .setContentImageUrl("https://example.com/mycontent-12345.png")

            // You use this to specify whether this content can be discovered publicly - default is public
            .setContentIndexingMode(BranchUniversalObject.CONTENT_INDEX_MODE.PUBLIC)

     		// Here is where you can add custom keys/values to the deep link data
            .addContentMetadata("property1", "blue")
            .addContentMetadata("property2", "red");
```

### Registering a View

Branch recently launched [Content Analytics](https://branch.io/content-analytics) which gives you insight into how engaging your content and how well it drives growth. You'd likely want to observe how many views each piece of content has as well. Once you've created the Universal Object, it's a simple call when the page loads.

```java
branchUniversalObject.registerView();
```

### Creating a Deep Link

Once you've created your `Branch Universal Object`, which is the reference to the content you're interested in, you can then get a link back to it with the mechanisms described below. First define the properties of the link itself.

```java
LinkProperties linkProperties = new LinkProperties()
               .setChannel("facebook")
               .setFeature("sharing")
               .addControlParameter("$desktop_url", "http://example.com/home")
               .addControlParameter("$ios_url", "http://example.com/ios");
```

You do custom redirection by inserting the following _optional keys in the control params_:

| Key | Value
| --- | ---
| "$fallback_url" | Where to send the user for all platforms when app is not installed.
| "$desktop_url" | Where to send the user on a desktop or laptop. By default it is the Branch-hosted text-me service
| "$android_url" | The replacement URL for the Play Store to send the user if they don't have the app. _Only necessary if you want a mobile web splash_
| "$ios_url" | The replacement URL for the App Store to send the user if they don't have the app. _Only necessary if you want a mobile web splash_
| "$ipad_url" | Same as above but for iPad Store
| "$fire_url" | Same as above but for Amazon Fire Store
| "$blackberry_url" | Same as above but for Blackberry Store
| "$windows_phone_url" | Same as above but for Windows Store

You have the ability to control the direct deep linking of each link by inserting the following _optional keys in the control params_:

| Key | Value
| --- | ---
| "$deeplink_path" | The value of the deep link path that you'd like us to append to your URI. For example, you could specify "$deeplink_path": "radio/station/456" and we'll open the app with the URI "yourapp://radio/station/456?link_click_id=branch-identifier". This is primarily for supporting legacy deep linking infrastructure.
| "$always_deeplink" | true or false. (default is not to deep link first) This key can be specified to have our linking service force try to open the app, even if we're not sure the user has the app installed. If the app is not installed, we fall back to the respective app store or $platform_url key. By default, we only open the app if we've seen a user initiate a session in your app from a Branch link (has been cookied and deep linked by Branch)

Then, make a request to the Universal Object in order to create the URL.

```java
branchUniversalObject.generateShortUrl(this, linkProperties, new BranchLinkCreateListener() {
    @Override
    public void onLinkCreate(String url, BranchError error) {
        if (error == null) {
            Log.i("MyApp", "got my Branch link to share: " + url);
        }
    }
});
```

### Showing a Custom Share Sheet

We’ve realized that Android had some very poor offerings for native share sheet functionality, so we built our own and bundled it into the core SDK. This share sheet it customizable and will automatically generate a link when the user selects a channel to share to.

![Android Share Sheet](https://dev.branch.io/img/ingredients/sdk_links/android_share_sheet.png)

To use it, first define the custom share sheet style like so. Most of these are completely optional and we've got a great set of defaults if you don't want to spend hours customizing.

```java
ShareSheetStyle shareSheetStyle = new ShareSheetStyle(MainActivity.this, "Check this out!", "This stuff is awesome: ")
                .setCopyUrlStyle(getResources().getDrawable(android.R.drawable.ic_menu_send), "Copy", "Added to clipboard")
                .setMoreOptionStyle(getResources().getDrawable(android.R.drawable.ic_menu_search), "Show more")
                .addPreferredSharingOption(SharingHelper.SHARE_WITH.FACEBOOK)
                .addPreferredSharingOption(SharingHelper.SHARE_WITH.EMAIL);
```

To show the share sheet, call the following on your Branch Universal Object. There are plenty of callback methods for you to hook into as you need for your own customizations on top of our animation.

```java
branchUniversalObject.showShareSheet(this, 
                                      linkProperties,
                                      shareSheetStyle,
                                       new Branch.BranchLinkShareListener() {
    @Override
    public void onShareLinkDialogLaunched() {
    }
    @Override
    public void onShareLinkDialogDismissed() {
    }
    @Override
    public void onLinkShareResponse(String sharedLink, String sharedChannel, BranchError error) {
    }
    @Override
    public void onChannelSelected(String channelName) {
    }
});
```

### Auto Deep link Activities

Branch provides a very easy and powerful automatic deep linking to Activities. You can configure Activities to be launched on clicking a link. Here is how you configure an Activity for auto deep linking.

**0)** Pick the key from the deep link data

When you create a deep link, you'll reference a Branch Universal Object. You have the option to add a ton of custom key/values to the metadata dictionary. You must first pick one that you want to automatically launch the selected Activity in step 1. For example, you'd use `picture_id` if you Universal Objects were mapping to pictures and you stuffed that key in the metadata.

**1)** Configure auto deep link keys for Activity in manifest file

```xml
<activity android:name=".AutoDeepLinkTestActivity">
     <!-- Keys for auto deep linking this activity -->
     <meta-data android:name="io.branch.sdk.auto_link_keys" android:value="auto_deeplink_key_1,auto_deeplink_key_2" />
     <!-- Optional request ID for launching this activity on auto deep link key matches -->
     <meta-data android:name="io.branch.sdk.auto_link_request_code" android:value="@integer/AutoDeeplinkRequestCode" />
</activity>
```

**2)** Retrieve deep link data in your auto deep link activity

Your deep linked parameters are added to the intent extra with the same key you have used in the JSONObject for creating the link.

```java
String name = getIntent().getExtras().getString("name");
```

You can also get the deep linked parameters as the original JSONObject that you used for link creation

```java
JSONObject linkedParams = Branch.getInstance().getLatestReferringParams();
```

**3)** Receive deep link activity finish call in main Activity

Branch will auto open the activity when it detects the specified key in the deep link dict. Do this if you want to handle something on auto-deep-linked-Activity finish on your main activity as the example below.

```java
@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    //Checking if the previous activity is launched on Branch Auto deep link.
    if(requestCode == getResources().getInteger(R.integer.AutoDeeplinkRequestCode)){
        //Decide here where  to navigate  when an auto deep linked activity finishes.For e.g. go to HomeActivity or a  SignUp Activity.
        Intent i = new Intent(getApplicationContext(), SignUpActivity.class);
        startActivity(i);
    }
}
```

## Referral system rewarding functionality

### Get reward balance

Reward balances change randomly on the backend when certain actions are taken (defined by your rules), so you'll need to make an asynchronous call to retrieve the balance. Here is the syntax:

```java
Branch branch = Branch.getInstance(getApplicationContext());
branch.loadRewards(new BranchReferralStateChangedListener() {
    @Override
    public void onStateChanged(boolean changed, Branch.BranchError error) {
        // changed boolean will indicate if the balance changed from what is currently in memory

        // will return the balance of the current user's credits
        int credits = branch.getCredits();
    }
});
```

### Redeem all or some of the reward balance (store state)

We will store how many of the rewards have been deployed so that you don't have to track it on your end. In order to save that you gave the credits to the user, you can call redeem. Redemptions will reduce the balance of outstanding credits permanently.

```java
Branch branch = Branch.getInstance(getApplicationContext());
branch.redeemRewards(5);
```

### Get credit history

This call will retrieve the entire history of credits and redemptions from the individual user. To use this call, implement like so:

```java
Branch branch = Branch.getInstance(getApplicationContext());
branch.getCreditHistory(new BranchListResponseListener() {
    public void onReceivingResponse(JSONArray list, Branch.BranchError error) {
        if (error == null) {
            // show the list in your app
        } else {
            Log.i("MyApp", error.getMessage());
        }
    }
});
```

The response will return an array that has been parsed from the following JSON:
```json
[
    {
        "transaction": {
                           "date": "2014-10-14T01:54:40.425Z",
                           "id": "50388077461373184",
                           "bucket": "default",
                           "type": 0,
                           "amount": 5
                       },
        "event" : {
            "name": "event name",
            "metadata": { your event metadata if present }
        },
        "referrer": "12345678",
        "referree": null
    },
    {
        "transaction": {
                           "date": "2014-10-14T01:55:09.474Z",
                           "id": "50388199301710081",
                           "bucket": "default",
                           "type": 2,
                           "amount": -3
                       },
        "event" : {
            "name": "event name",
            "metadata": { your event metadata if present }
        },
        "referrer": null,
        "referree": "12345678"
    }
]
```
**referrer**
: The id of the referring user for this credit transaction. Returns null if no referrer is involved. Note this id is the user id in developer's own system that's previously passed to Branch's identify user API call.

**referree**
: The id of the user who was referred for this credit transaction. Returns null if no referree is involved. Note this id is the user id in developer's own system that's previously passed to Branch's identify user API call.

**type**
: This is the type of credit transaction

1. _0_ - A reward that was added automatically by the user completing an action or referral
1. _1_ - A reward that was added manually
2. _2_ - A redemption of credits that occurred through our API or SDKs
3. _3_ - This is a very unique case where we will subtract credits automatically when we detect fraud