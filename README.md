# Branch Metrics Android SDK 

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.branch.sdk.android/library/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.branch.sdk.android/library)
[![Javadocs](http://javadoc-badge.appspot.com/io.branch.sdk.android/library.svg?label=javadoc)](http://javadoc-badge.appspot.com/io.branch.sdk.android/library)

This is a repository of our open source Android SDK, and the information presented here serves as a reference manual for our Android SDK. See the table of contents below for a complete list of the content featured in this document.

___

## Android Reference

1. External resources
  + [Full integration guide](https://docs.branch.io/pages/apps/android/)
  + [Change log](https://github.com/BranchMetrics/android-branch-deep-linking/blob/master/ChangeLog.md)
  + [Testing resources](https://docs.branch.io/pages/apps/android/#test-your-branch-integration)
  + [Support portal](http://support.branch.io)
  + [Test app resources](#get-the-demo-app)

2. Getting started
  + [Library installation](#installation)
  + [Register for Branch key](#register-your-app)
  + [Register a activity for deep linking](#register-an-activity-for-direct-deep-linking-optional-but-recommended)
  + [Leverage Android App Links for deep linking](#leverage-android-app-links-for-deep-linking)
  + [Deep link from push notifications](#deeplink-via-push-notification)
  + [Add Branch key to your manifest](#configure-your-androidmanifestxml)
  + [Support for Android Instant Apps](#instant-app-deep-linking-and-attribution-support)

3. Branch general methods
  + [Initialize Branch and register deep link router](#initialization)
  + [Auto deep link functionality](#auto-deep-link-activities)
  + [Retrieve latest deep linking params](#retrieve-session-install-or-open-parameters)
  + [Retrieve the user's first deep linking params](#retrieve-install-install-only-parameters)
  + [Setting the user id for tracking influencers](#persistent-identities)
  + [Logging a user out](#logout)
  + [Tracking custom events](#register-custom-events)
  + [Matching through Install Listener](#matching-through-install-listener)

4. Branch Universal Objects
  + [Instantiate a Branch Universal Object](#defining-the-branch-universal-object)
  + [Register user actions on an object](#register-user-actions-on-an-object)
  + [List content on Google Search](#list-links-in-google-search-with-app-indexing)
  + [Creating a short link referencing the object](#creating-a-deep-link)
  + [Triggering a share sheet to share a link](#showing-a-custom-share-sheet)

5. Referral rewards methods
  + [Get reward balance](#get-reward-balance)
  + [Redeem rewards](#redeem-all-or-some-of-the-reward-balance-store-state)
  + [Get credit history](#get-credit-history)

6. General support
  + [Troubleshooting](#troubleshooting)
___

## Get the Demo App

This is the readme file of our open source Android SDK. There's a full demo app embedded in this repository, but you should also check out our live demo: [Branch Monster Factory](https://play.google.com/store/apps/details?id=io.branch.branchster). We've [open sourced the Branchster's app](https://github.com/BranchMetrics/Branchster-Android) as well if you'd like to dig in.

# Getting Started

## Installation

**The compiled Android SDK footprint is 187kb**

### Install library project

Just add `implementation 'io.branch.sdk.android:library:2.+'` to the dependencies section of your `build.gradle` file.

#### Some notes:

- If you don't plan to use the `Fabric Answers` integration, and don't want to import the `answers-shim`, just import your project as follows:
```
implementation ('io.branch.sdk.android:library:3.+') {
  exclude module: 'answers-shim'
}
```
- This supports minimum sdk level 16. If you want to support minimum sdk level 9 please consider using version 1.14.5. If you want to to support a minimum SDK of 15, use the 2.x versions.
- Android SDK versions 3.x and above communicate over TLS1.2.
- If you want to import the AAR directly, you can find the build in Nexus here: https://search.maven.org/artifact/io.branch.sdk.android/library. 
- Or you can clone this repo and import the source as a library into your project

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

If you are building applications targeting for Android M or above, Branch make it really easy to configure your app for deep linking using App Links. In your project's manifest file, you can register activities to for App Linking by adding an intent filter as follows.

#### If using app.link

With app.link, there's no need to use the encoded id and you just need to list the domains.

```xml
<activity android:name="com.yourapp.your_activity">
    <!-- App Link your activity to Branch links-->
    <intent-filter android:autoVerify="true">
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
         <data android:scheme="https" android:host="yourapp-alternate.app.link" />
         <data android:scheme="https" android:host="yourapp.app.link" />
    </intent-filter>
</activity>
```

#### If using bnc.lt or a custom domain

You only need to know `live_app_alpha_encoded_id` and `test_app_alpha_encoded_id` which you can obtain from the Branch dash board once you enable App Linking support for your application.

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

### Deeplink via push notification
You can deep link to content from push notifications just by adding a Branch link to your result intent. Simply pass the a Branch link with gcm payload and add it to your resultIntent with key `branch`.

```java
        Intent resultIntent = new Intent(this, TargetClass.class);
        intent.putExtra("branch","http://bnc.lt/testlink");
        .....
       PendingIntent resultPendingIntent =  PendingIntent.getActivity(this, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
```

If you would like to support push notification based routing while your app already in foreground, please add the following to your notification intent.

```java
       intent.putExtra("branch_force_new_session",true);
```

### Guaranteed Matching
Branch support hundred percent guaranteed matching with cookie based matching using Custom Chrome Tabs. This is highly recommended if you like to do user authentication through deep link metadata.
Just add the following to your build.gradle file to enable guaranteed matching

```
    implementation 'com.android.support:customtabs:23.3.0'
```

Note : Adding additional dependencies may overrun the dex limit and lead to `NoClassDefFoundError` or `ClassNotFoundException`. Please make sure you have enabled multi-dex support to solve this issue. For more information on enabling multi-dex support please refer to [Troubleshooting](#troubleshooting)

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

### Proguard settings for leveraging Branch's pooled matching

To collect the Google Advertising ID, you must ensure that proguard doesn't remove the necessary Google Ads class. The surest way to do this is add it to your proguard rules. If your Application is enabled with proguard, add the following instruction to your `proguard.cfg` or `proguard-rules.pro` file:

```bash
-keep class com.google.android.gms.ads.identifier.** { *; }
```
In case you are using Facebook SDK to support deep linking through Facebook ads, please make sure to keep the Facebook SDK classes in proguard

```bash
-keep class com.facebook.applinks.** { *; }
-keepclassmembers class com.facebook.applinks.** { *; }
-keep class com.facebook.FacebookSdk { *; }
```

### Instant App Deep Linking and Attribution Support

The Branch SDK make it easier to deep link and attribute your Instant Apps. Since Branch automatically configures and hosts the assetlinks.json file for you, you don't need to worry about all the complexity involved in setting up Android App Links. Additionally, the Branch SDK makes it easy to deferred deep link from the Instant App through the full Android app install, providing attribution for every step of the way. Please make sure you have the following added along with regular Branch implementation for instant app enabled project

You can check out a [full demo application](https://github.com/BranchMetrics/Branch-Monster-Factory-Example-Android-Instant-Apps) on our Github. We've replicated our [original Android demo application](https://github.com/BranchMetrics/Branch-Example-Deep-Linking-Branchster-Android) and modified it to support Android Instant Apps.

**1. Initialize the Branch SDK**

Head to your _core library project_, where your Application class is defined and drop in the snippet of code to the onCreate() method as follows.

```java
public void onCreate() {
  super.onCreate();

  // Initialize the Branch SDK
  Branch.getAutoInstance(this);
}
```

**2. Add your Branch keys and register for Install Referrer**

Instant Apps can be rather confusing as there are many different manifests, but you want to find the Manifest that contains your `application` tags. Make sure your Application class name is defined here, and then specify the Branch keys _inside_ the `application` element.

```xml
<application
        android:allowBackup="true"
        android:label="@string/app_name"
        android:theme="@style/AppTheme"
        android:supportsRtl="true"
        android:name=".MyApplication">

  <meta-data android:name="io.branch.sdk.TestMode" android:value="false" /> <!-- Set to true to use Branch_Test_Key -->
  <meta-data android:name="io.branch.sdk.BranchKey" android:value="key_live_my_live_key" />
  <meta-data android:name="io.branch.sdk.BranchKey.test" android:value="key_test_my_test_key" />

  <receiver android:name="io.branch.referral.InstallListener" android:exported="true">
    <intent-filter>
       <action android:name="com.android.vending.INSTALL_REFERRER" />
    </intent-filter>
  </receiver>
</application>
```

**3. Configure your Branch links as Android App Links**

This guide presumes that you've already configured Branch for Android App Links in the past. If you haven't configured your full native app to use Branch as Android App Links, [please complete this guide](https://docs.branch.io/pages/deep-linking/android-app-links/) which will correctly configure the dashboard and manifest.

Now, you simply need to edit the above manifest and paste in the following snippet _inside_ the `application` element. Then you'll need to replace the `xxxx` with your own custom subdomain which will be visible on [the Branch link settings dashboard](https://dashboard.branch.io/link-settings) at the bottom of the page. If you're using a custom subdomain, you can find the advanced instructions in the above link regarding configuring Android App Links.

```xml
<application
  ......
  
  <intent-filter android:autoVerify="true">
      <action android:name="android.intent.action.VIEW" />
      <category android:name="android.intent.category.DEFAULT" />
      <category android:name="android.intent.category.BROWSABLE" />
      <data android:scheme="https" android:host="xxxx.app.link" />
      <data android:scheme="https" android:host="xxxx-alternate.app.link" />
  </intent-filter>
  
</application>
```

**4. Retrieve Branch deep link data**

Now that you've outfitted your Instant App project with the above, you can now [register a deep link router function](#initialization) for activities you want to receive the deep link data in any Activity split, similar to how you would retrieve deep link data in the full app.

**5. Configure the deep linking from Instant App to your Full App**

Now, the user has arrived in your Instant App and you're ready to convert them to install your full native app. Don't worry, Branch as got your covered! We have overridden the default `showInstallPrompt` with a method that auto configures the Google Play prompt with all of the deep link data you need to carry context through install. Additionally, we can provide you the full set of attribution on how many users conver through this prompt.

Branch SDK provides convenient methods to check for app types and full app conversion. This eliminates the dependency on Google IA support SDK ('com.google.android.instantapp'). Here are some of the methods that makes life easy

- `Branch#isInstantApp()`

This convenience methods checks whether the current version of app running is Instant App or Full Android App to allow you convenience

- `Branch#showInstallPrompt()`
      
This methods shows an install prompt for the full Android app, allowing you an easy way to pass Branch referring deep data to the full app through the install process. Similar to how deferred deep linking works for Branch normally, the full app will receive the deep link params in the handle callback.

The below example shows how to create a custom Branch Universal Object, the associate it with the installation prompt that will be passed through to your full native Android app after the user installs.

```java
if (Branch.isInstantApp(this)) {
  myFullAppInstallButton.setVisibility(View.VISIBLE);
  myFullAppInstallButton.setOnClickListener(new OnClickListener() {
    @Override
    public void onClick(View v) {
       BranchUniversalObject branchUniversalObject = new BranchUniversalObject()
           .setCanonicalIdentifier("item/12345")
           .setTitle("My Content Title")
           .setContentDescription("My Content Description")
           .setContentImageUrl("https://example.com/mycontent-12345.png")
           .setContentMetadata(new ContentMetadata()
                 .addCustomMetadata("property1", "blue")
                 .addCustomMetadata("property2", "red"));

      Branch.showInstallPrompt(myActivity, activity_ret_code, branchUniversalObject);
    }
  });
} else {
  myFullAppInstallButton.setVisibility(View.GONE);
}
```

# Branch General Methods

## Initialization

If your minimum sdk level is 15+, To receive the deep link parameters from the Branch SDK, call initSession and pass in the BranchReferralInitListener. This will return the dictionary of referringParams associated with the link that was just clicked. You can call this anywhere at any time to get the params.

If you need to support pre 15, Branch must know when the app opens or closes to properly handle the deep link parameters retrieval. You can see more details on how to do this at [this docs site](https://docs.branch.io/pages/apps/android/). Basically, if you don't close the Branch session, you'll see strange behaviors like deep link parameters not showing up after clicking a link the second time.

#### Initialize Branch lifecycle

Initialising and closing session is done automatically with our new _automatic session management_.

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

To retrieve this information synchronously, call the following from a non-UI thread:

```java
JSONObject sessionParams = branch.getLatestReferringParamsSync();
```

#### Retrieve install (install only) parameters

If you ever want to access the original session params (the parameters passed in for the first install event only), you can use this line. This is useful if you only want to reward users who newly installed the app from a referral link or something.

```java
Branch branch = Branch.getInstance(getApplicationContext());
JSONObject installParams = branch.getFirstReferringParams();
```

To retrieve this information synchronously, call the following from a non-UI thread:

```java
JSONObject sessionParams = branch.getFirstReferringParamsSync();
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

# Branch Universal Object

## Branch Universal Object (for deep links, content analytics and indexing)

As more methods have evolved in Android, we've found that it was increasingly hard to manage them all. We abstracted as many as we could into the concept of a Branch Universal Object. This is the object that is associated with the thing you want to share (content or user). You can set all the metadata associated with the object and then call action methods on it to get a link or register a view.

### Branch Universal Object best practices

Here are a set of best practices to ensure that your analytics are correct, and your content is ranking on Spotlight effectively.

1. Set the `canonicalIdentifier` to a unique, de-duped value across instances of the app
2. Ensure that the `title`, `contentDescription` and `imageUrl` properly represent the object
3. Initialize the Branch Universal Object and call `userCompletedAction` with the `BranchEvent.VIEW` **on page load**
4. Call `showShareSheet` and `createShortLink` later in the life cycle, when the user takes an action that needs a link
5. Call the additional object events (purchase, share completed, etc) when the corresponding user action is taken

Practices to _avoid_:
1. Don't set the same `title`, `contentDescription` and `imageUrl` across all objects
2. Don't wait to initialize the object and register views until the user goes to share
3. Don't wait to initialize the object until you conveniently need a link
4. Don't create many objects at once and register views in a `for` loop.

### Defining the Branch Universal Object

The universal object is where you define all of the custom metadata associated with the content that you want to link to or index. Please use the builder format below to create one.

```java
 BranchUniversalObject branchUniversalObject = new BranchUniversalObject()

 			// The identifier is what Branch will use to de-dupe the content across many different Universal Objects
            .setCanonicalIdentifier("item/12345")

            // The canonical URL for SEO purposes (optional)
            .setCanonicalUrl("https://branch.io/deepviews")

            // This is where you define the open graph structure and how the object will appear on Facebook or in a deepview
            .setTitle("My Content Title")
            .setContentDescription("My Content Description")
            .setContentImageUrl("https://example.com/mycontent-12345.png")

            // You use this to specify whether this content can be discovered publicly - default is public
            .setContentIndexingMode(BranchUniversalObject.CONTENT_INDEX_MODE.PUBLIC)

     		// Here is where you can add custom keys/values to the deep link data
            .setContentMetadata(new ContentMetadata()
                             .addCustomMetadata("property1", "blue")
                             .addCustomMetadata("property2", "red"));
```

#### Parameters

**canonicalIdentifier**: This is the unique identifier for content that will help Branch dedupe across many instances of the same thing. If you have a website with pathing, feel free to use that. Or if you have database identifiers for entities, use those.

**title**: This is the name for the content and will automatically be used for the OG tags. It will insert $og_title into the data dictionary of any link created.

**contentDescription**: This is the description for the content and will automatically be used for the OG tags. It will insert $og_description into the data dictionary of any link created.

**imageUrl**: This is the image URL for the content and will automatically be used for the OG tags. It will insert $og_image_url into the data dictionary of any link created.

**metadata**: These are any extra parameters you'd like to associate with the Branch Universal Object. These will be made available to you after the user clicks the link and opens up the app. To add more keys/values, just use the method `addMetadataKey`.

**price**: The price of the item to be used in conjunction with the commerce related events below.

**currency**: The currency representing the price in [ISO 4217 currency code](http://en.wikipedia.org/wiki/ISO_4217). Default is USD.

**contentIndexMode**: Can be set to the ENUM of either `ContentIndexModePublic` or `ContentIndexModePrivate`. Public indicates that you'd like this content to be discovered by other apps. Currently, this is only used for Spotlight indexing but will be used by Branch in the future.

**expirationDate**: The date when the content will not longer be available or valid. Currently, this is only used for Spotlight indexing but will be used by Branch in the future.


### Tracking User Actions and Events
Use `BranchEvent` class to track special user actions or application specific events beyond app installs, opens, and sharing. You can track events such as when a user adds an item to an on-line shopping cart, or searches for a keyword etc.
`BranchEvent` provides an interface to add content(s) represented by a BranchUniversalObject in order to associate content(s) with events.
You can view analytics for the BranchEvents you fire on the Branch dashboard. BranchEvents provide a seamless integration with many third party analytics providers like Google Analytics, Criteo.
`BRANCH_STANDARD_EVENT` enumerate the most commonly tracked events and event parameters that can be used with `BranchEvent` for the best results. You can always use custom event names and event parameters.

```java
new BranchEvent(BRANCH_STANDARD_EVENT.PURCHASE)
    .setAffiliation("affiliation_value")
    .setCoupon("coupon_value")
    .setCurrency(CurrencyType.USD)
    .setTax(12.3)
    .setRevenue(1.5)
    .setDescription("Event_description")
    .setSearchQuery("related_search_query")
    .addCustomDataProperty("Custom_Event_Property_Key", "Custom_Event_Property_Val")
    .addContentItems(contentBUO1, contentBUO2)
    .logEvent(context);
```

```java
new BranchEvent("My_Custom_Event")
    .addCustomDataProperty("key1", "value1")
    .addCustomDataProperty("key2", "value2")
    .logEvent(context);
```

### Register User Actions On An Object
This functionality is deprecated. Please consider using `BranchEvent` for tracking user action and events as described [here](#tracking-user-actions-and-events).

We've added a series of custom events that you'll want to start tracking for rich analytics and targeting. Here's a list below with a sample snippet that calls the register view event.

| Key | Value
| --- | ---
| BranchEvent.VIEW | User viewed the object
| BranchEvent.ADD_TO_WISH_LIST | User added the object to their wishlist
| BranchEvent.ADD_TO_CART | User added object to cart
| BranchEvent.PURCHASE_STARTED | User started to check out
| BranchEvent.PURCHASED | User purchased the item
| BranchEvent.SHARE_STARTED | User started to share the object
| BranchEvent.SHARE_COMPLETED | User completed a share

```java
branchUniversalObject.userCompletedAction(BranchEvent.VIEW);
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
| "$fallback_url" | Where to send the user for all platforms when app is not installed. Note that Branch will forward all robots to this URL, overriding any OG tags entered in the link.
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
### List links in Google Search with App Indexing
Getting your Branch link and app content listed in Google search is very easy with BranchUniversalObject. Once you've created the BUO, use the following API to list your app contents in Google Search via Firebase App Indexing API. Your app will be opened with deep link data upon user clicking the search result and the session will be tracked.
This also allow your contents Indexed locally and shown in Google `In Apps` search. By default the the BranchUniversal objects are locally indexable. Set your BranchUniversalObject's local index mode to `private` if intended to avoid indexing locally

```java
branchUniversalObject.listOnGoogleSearch(context);
```
**NOTE** Please make sure Firebase app indexing is enabled for your application and has Firebase app indexing dependency added
```java
'implementation 'com.google.firebase:firebase-appindexing:10.0.1'
```
To test content indexing follow instructions[here](https://firebase.google.com/docs/app-indexing/android/test#test-public-content-indexing). 
Please enable debug mode and check the logs to get the Branch link used for indexing and use it with the above link to verify content indexing.

**NOTE** Use `BranchUniversalObject#removeFromLocalIndexing` method to remove the contents from local index. Please make sure to remove user specific contents from local index up on user logout.

### Showing a Custom Share Sheet

We’ve realized that Android had some very poor offerings for native share sheet functionality, so we built our own and bundled it into the core SDK. This share sheet it customizable and will automatically generate a link when the user selects a channel to share to.

![Android Share Sheet](https://dev.branch.io/img/pages/getting-started/branch-universal-object/android_share_sheet.png)

To use it, first define the custom share sheet style like so. Most of these are completely optional and we've got a great set of defaults if you don't want to spend hours customizing.

```java
ShareSheetStyle shareSheetStyle = new ShareSheetStyle(MainActivity.this, "Check this out!", "This stuff is awesome: ")
                .setCopyUrlStyle(getResources().getDrawable(android.R.drawable.ic_menu_send), "Copy", "Added to clipboard")
                .setMoreOptionStyle(getResources().getDrawable(android.R.drawable.ic_menu_search), "Show more")
                .addPreferredSharingOption(SharingHelper.SHARE_WITH.FACEBOOK)
                .addPreferredSharingOption(SharingHelper.SHARE_WITH.EMAIL)
                .setAsFullWidthStyle(true)
                .setSharingTitle("Share With");
```

To show the share sheet, call the following on your Branch Universal Object. There are plenty of callback methods for you to hook into as you need for your own customizations on top of our animation.

```java
branchUniversalObject.showShareSheet(this, 
                                      linkProperties,
                                      shareSheetStyle,
                                       new Branch.ExtendedBranchLinkShareListener() {
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
    @Override
    public boolean onChannelSelected(String channelName, BranchUniversalObject buo, LinkProperties linkProperties) {
        return false;
    }
});
```

# Referral Rewards

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


### Enable or Disable User Tracking
In order to comply with tracking requirements, you can disable tracking at the SDK level. Simply call:
 ```java
Branch.getInstance().disableTracking(true);
```

This will prevent any Branch requests from being sent across the network, except for the case of deep linking. If someone clicks a Branch link, but has expressed not to be tracked, we will return deep linking data back to the client but without tracking information captured.

In do-not-track mode, you will still be able to create and share links. They will not have identifiable information. Event tracking won’t pass data back to the server if a user has expressed to not be tracked. You can change this behavior at any time, but calling the above function. This information will be saved and persisted.


## Troubleshooting

### Troubleshooting your Branch SDK Integration

Test your Branch Integration by calling `IntegrationValidator.validate` in your MainActivity's onStart(). Check your ADB Logcat to make sure all the SDK Integration tests pass and make sure to comment out or remove `IntegrationValidator.validate` in your production code.

```java
IntegrationValidator.validate(MainActivity.this);
```

### Troubleshooting Deeplink routing for your Branch links

Append `?bnc_validate=true` to any of your app's Branch links and click it on your mobile device (not the Simulator!) to start the test. For instance, to validate a link like: `"https://<yourapp\>.app.link/NdJ6nFzRbK"` click on: `"https://<yourapp\>.app.link/NdJ6nFzRbK?bnc_validate=true"`

### ClassNotFoundException : Branch.Java

In case of having other SDKs along with Branch and exceeding the Dex limit, please make sure you have enabled multi-dex support for your application. Check the following to ensure multi-dex is configured properly

1) Make sure you have enabled multi-dex support in your build.gradle file

```java
 defaultConfig {
    multiDexEnabled true
}
```

2) Make sure your `Application` class is extending `MultiDexApplication`

3) Make sure dex files are properly loaded from .apk file. In your application class make sure you have the following

```java
@Override
protected void attachBaseContext(Context base) {
    super.attachBaseContext(base);
    MultiDex.install(this);
}
```

### InvalidClassException, ClassLoadingError or VerificationError

This is often caused by a Proguard bug with optimization. Please try to use the latest Proguard version or disable Proguard optimisation by setting `-dontoptimize` option

### Proguard warning or errors with `answers-shim` module
This is often caused when you exclude the `answers-shim` module from Branch SDK depending on your proguard settings. Please add the following to your proguard file to solve this issue
`-dontwarn com.crashlytics.android.answers.shim.**`

### Proguard warning or errors with `appindexing` module
Branch SDK has optional dependencies on Firebase app indexing and Android install referrer classes to provide new Firebase content listing features and support new Android install referrer library. This may cause a proguard warning depending on your proguard settings.
Please add the following to your proguard file to solve this issue
`-dontwarn com.google.firebase.appindexing.**`
`-dontwarn com.android.installreferrer.api.**`
