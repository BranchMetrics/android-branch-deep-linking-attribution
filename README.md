# Branch Metrics Android SDK 

This is a repository of our open source Android SDK. See the table of contents below for a complete list of the content featured in this document.

**Note:** The content presented here is intented to be only a reference manual for our Android SDK. Go to our new [**Documentation Portal**] (https://dev.branch.io) where you can find all of our latest documentation and future documentation updates. 

Table of Contents| 
------------- | 
[Get the Demo App](#get-the-demo-app)| 
|[Class Reference Table](#class-reference)|
|[Important Migrations] (#important-migration-to-v078)|  
[Troubleshooting FAQ](#faq)|
[Installation](#installation)|
[Configuration (for Tracking)](#configuration-for-tracking)|
[Register an activity for direct deep linking (optional but recommended)](#register-an-activity-for-direct-deep-linking-optional-but-recommended)|
[Add Your Branch Key to Your Project](#add-your-branch-key-to-your-project)|

## Get the Demo App

There's a full demo app embedded in this repository, but you can also check out our live demo: [Branch Monster Factory](https://play.google.com/store/apps/details?id=io.branch.branchster). We've [open sourced the Branchster's app](https://github.com/BranchMetrics/Branchster-Android) as well if you're ready to dig in.


##Class Reference
For your reference, see the methods and parameters table below.   
  
**Class Reference Table**  
      
| Tasks          | Methods          | Parameters     |
|:------------- |:---------------:| -------------:|   
[Initialize SDK and Register Deep Link Routing Function](#initialize-sdk-and-register-deep-link-routing-function)|[Method](#methods)|[Parameter](#parameters)
|[Close session (session tracking to support for minSdkVersion < 14)](#close-session-session-tracking-to-support-for-minSdkVersion-< 14)|[Method](#methods-1)|[Parameter](#parameters-1)|
|[Retrieve Session (Install or Open) Parameters](#retrieve-session-install-or-open-parameters)|[Method](#methods-2)|[Parameter](#parameters-2)| 
|[Retrieve Install (Install Only) Parameters](#retrieve-install-install-only-parameters)|[Method](#methods-3)|[Parameter](#parameters-3)|
[Persistent Identities](#persistent-identities)|[Method](#methods-4)|[Parameter](#parameters-4)|
[Logout](#logout)|[Method](#methods-5)|[Parameter](#parameters-5)|
[Register Custom Events](#register-custom-events)|[Method](#methods-6)| [Parameter](#parameters-6)|
[Generate Tracked, Deep Linking URLs (Pass Data Across Install and Open)](#generate-tracked-deep-linking-urls-pass-data-across-install-and-open)|[Method](#methods-7)|[Parameter](#parameters-7)|
[Referral System Rewarding Functionality](#get-reward-balance)|[Method](#methods-8)|[Parameter](#parameters-8)| 
|[Get Reward Balance](#get-reward-balance)|[Method](#methods-9)|[Parameters] (#parameters-9)| 
[Redeem All or Some of the Reward Balance (Store State)](#redeem-all-or-some-of-the-reward-balance-store-state)|[Method](#methods-10)|[Parameter](#parameters-10)|
[Get Credit History](#get-credit-history)|[Method](#methods-11)|[Parameters] (#parameters-11)|
[Get Referral Code](#get-referral-code)|[Method](#methods-12)|[Parameter] (#parameters-12)|
[Create Referral Code](#create-referral-code)|[Method](#methods-13)|[Parameter] (#parameters-13)|
[Validate Referral Code](#validate-referral-code)|[Method](#methods-17)|[Parameter](#parameters-17)|
[Apply Referral Code](#apply-referral-code)|[Method](#methods-18)|[Parameter] (#parameters-18)|


## Important Migration to v1.5.0

We have deprecated the bnc\_appkey and replaced that with the new branch\_key. See [add branch key](#add-your-branch-key-to-your-project) for details.

## Important Migration to v1.4.5

Branch uses Facebook's App Links metatags automatically to provide the best linking from the Facebook platform. Unfortunately, Facebook changed the way they handle App Links routing in the latest update on April 8ish.

To properly handle deep links from Facebook, you must perform the following: 

* Make sure to update the Manifest so that the Activity with the intent filter for your URI scheme has *launchMode:singleTask*. See example [here](https://github.com/BranchMetrics/Branch-Android-SDK#register-an-activity-for-direct-deep-linking-optional-but-recommended)

* Make sure to add this snippet of code to the Activity registered as singleTask.
	```java
	@Override
	public void onNewIntent(Intent intent) {
		// Because the activity is a singleTask activity, the new intent won't be
		// launched but enters here, making handling it optional. For branch to work
		// the intent must be updated by calling the following:
		this.setIntent(intent);
	}
	```

* Update the SDK to v1.4.5 or higher.

## FAQ
Have questions? Need troubleshooting assistance? See our [FAQs](https://dev.branch.io/references/android_sdk/#faq) for in depth answers.

## Installation

The current compiled SDK footprint is *40kb*. You can clone this repository to keep up with the latest version, you can install the library project, or you can download the raw files.

### Install Library Project

Import the SDK as a Gradle dependency (for Android Studio):
* Right click on the main module within your project (this is called 'app' by default).
* Select **Open Module Settings**.
* Within the **Dependencies** tab, click the **+** button at the bottom of the window and select **Library Dependency**.
* Type *branch*, and hit the enter key to search Maven Central for the Branch SDK Library.
* Select the latest *io.branch.sdk.android:library* item listed and accept the changes.

**Note:** See the [Android Quick Start Guide for more details](https://github.com/BranchMetrics/Branch-Integration-Guides/blob/master/android-quick-start.md) and a screencasted walkthrough.

### Download Files

You can also install by downloading the raw files below.

* download the JAR file from here:
https://s3-us-west-1.amazonaws.com/branchhost/Branch-Android-SDK.zip

* The testbed project:
https://s3-us-west-1.amazonaws.com/branchhost/Branch-Android-TestBed.zip

Or can just clone this project!

### Register Your App

You can sign up for your own app id at [https://dashboard.branch.io](https://dashboard.branch.io).

## Configuration (for Tracking)

You can use our links any time you have an external link pointing to your app (share, invite, referral, etc.). For more information about configuration, see the [Configuration (for tracking)](https://dev.branch.io/references/android_sdk/#configuration-for-tracking) section in our new [documentation portal](https://dev.branch.io/references/android_sdk/#configuration-for-tracking).

### Register an Activity for Direct Deep Linking (Optional but Recommended)

In your project's manifest file, you can register your app to respond to direct deep links (yourapp:// in a mobile browser) by adding the second intent filter block. Also, make sure to change **yourapp** to a unique string that represents your app name.

Next, make sure that this activity is launched as a singleTask. This is important to handle proper deep linking from other apps like Facebook.

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

### Add a Branch Key to Your Project

After you register your app, your branch key can be retrieved on the [Settings](https://dashboard.branch.io/#/settings) page of the dashboard. Now you need to add it (or them if you want to do it for both your live and test apps) to your project.

Edit your manifest file by adding the following new meta-data:
    ```xml
    <application>
        <!-- Other existing entries -->

        <!-- Add this meta-data below, and change "key_live_xxxxxxx" to your actual live branch key -->
        <meta-data android:name="io.branch.sdk.BranchKey" android:value="key_live_xxxxxxx" />

        <!-- For your test app, if you have one; Again, use your actual test branch key -->
        <meta-data android:name="io.branch.sdk.BranchKey.test" android:value="key_test_yyyyyyy" />
    </application>
    ```

### Initialize SDK and Register Deep Link Routing Function

Called in your splash activity where you handle. If you created a custom link with your own custom dictionary data, you probably want to know when the user session init finishes, so you can check that data. Think of this callback as your "deep link router". If your app opens with some data, you want to route the user depending on the data you passed in. Otherwise, send them to a generic install flow.

This deep link routing callback is called 100% of the time on init, with your link params or an empty dictionary if none present.

####Method

```java
@Override
public void onStart() {
	super.onStart();

	Branch branch = Branch.getInstance(getApplicationContext());
	branch.initSession(new BranchReferralInitListener(){
		@Override
		public void onInitFinished(JSONObject referringParams, BranchError error) {
			if (error == null) {
				// params are the deep linked params associated with the link that the user clicked before showing up
				// params will be empty if no data found

				// here is the data from the example below if a new user clicked on Joe's link and installed the app
				String name = referringParams.getString("user"); // returns Joe
				String profileUrl = referringParams.getString("profile_pic"); // returns https://s3-us-west-1.amazonaws.com/myapp/joes_pic.jpg
				String description = referringParams.getString("description"); // returns Joe likes long walks on the beach...

				// route to a profile page in the app for Joe
				// show a customer welcome
			} else {
				Log.i("MyApp", error.getMessage());
			}
		}
	}, this.getIntent().getData(), this);
}

@Override
public void onNewIntent(Intent intent) {
	// Android makes you do this yourself for some reason, so make sure this snippet is in the Activity registered for the intent filter
	this.setIntent(intent);
}
```

If you want to use your test app during development, in onStart() you can initialize the Branch object like this:

```java
Branch branch = Branch.getTestInstance(getApplicationContext());
```

_Please note that you need SDK version >= 1.5.0 to use getTestInstance()_

Or

```java
Branch branch = Branch.getInstance(getApplicationContext(), "your test branch key"); // replace with your actual branch key
```

Either way, we recommend you put a `//TODO` to remind you to change back to live app during deployment later.
Also, note the Branch object is singleton, so you can and should still use `Branch.getInstance(getApplicationContext())` in all the other places (see examples below).

####Parameters

None

#### Automatic Session Management

Starting from Branch SDK version 1.5.7, there is no need for initialising and closing session with the new _automatic session management_. Automatic session management can work only with API level 14 and above, so make sure that your `minSdkVersion` is 14 or above.

**Requirement**
```xml
<uses-sdk
	android:minSdkVersion="14"
	   ------------        />
```

Once you do any of the below, there is no need to close or init sessions in your Activities. Branch SDK will do all that for you. You can get your Branch instance at any time as follows.

```java
  Branch branch = Branch.getInstance();
```

Branch SDK can do session management for you if you do one of the following:

##### Common: you do not use Application class

If you are not creating or using an Application class throughout your project, all you need to do is declare `BranchApp` as your application class in your manifest.

```xml
 <application
-----
android:name="io.branch.referal.BranchApp">
```

##### Rarer: you already use the Application class

If you already have an Application class, then extend your application class with `BranchApp`.

```java
public class YourApplication extends BranchApp
```

##### Very rare: you already use and extend the Application class

If you already have an Application class and don't want to extend it from `BranchApp` then create a Branch instance in your Application's `onCreate()` method.

```java
public void onCreate() {
	super.onCreate();
	//noinspection ConstantConditions
	if (!BuildConfig.DEBUG) {
		Branch.getAutoInstance(this);
	} else {
		Branch.getAutoTestInstance(this);
	}
}
```

###Close session (session tracking to support for minSdkVersion < 14)

**Note:** There is no need to use this method if you use _automatic session management_ as described above and only support minSdkVersion >= 14.

**Required:** this call will clear the deep link parameters when the app is closed, so they can be refreshed after a new link is clicked or the app is reopened.

####Method

```java
@Override
public void onStop() {
	super.onStop();
	Branch.getInstance(getApplicationContext()).closeSession();
}
```
####Parameters

None

###Retrieve Session (Install or Open) Parameters

These session parameters will be available at any point later on with this command. If no params, the dictionary will be empty. This refreshes with every new session (app installs AND app opens)
```java
Branch branch = Branch.getInstance(getApplicationContext());
JSONObject sessionParams = branch.getLatestReferringParams();
```

###Retrieve Install (Install Only) Parameters

If you ever want to access the original session params (the parameters passed in for the first install event only), you can use this line. This is useful if you only want to reward users who newly installed the app from a referral link or something.
```java
Branch branch = Branch.getInstance(getApplicationContext());
JSONObject installParams = branch.getFirstReferringParams();
```

### Persistent Identities

Often, you might have your own user IDs, or want referral and event data to persist across platforms or uninstall/reinstall. It's helpful if you know your users access your service from different devices. This where we introduce the concept of an 'identity'.

####Method

To identify a user, just call:

```java
Branch branch = Branch.getInstance(getApplicationContext());
branch.setIdentity(your user id); // your user id should not exceed 127 characters
```
####Parameters

None

### Logout

If you provide a logout function in your app, be sure to clear the user when the logout completes. This will ensure that all the stored parameters get cleared and all events are properly attributed to the right identity.

**Warning** this call will clear the referral credits and attribution on the device.

####Method

```java
Branch.getInstance(getApplicationContext()).logout();
```

####Parameters

None

### Register Custom Events

####Method

```java
Branch branch = Branch.getInstance(getApplicationContext());
branch.userCompletedAction("your_custom_event"); // your custom event name should not exceed 63 characters
```

OR if you want to store some state with the event:

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

## Generate Tracked, Deep Linking URLs (Pass Data Across Install and Open)

### Shortened Links

There are a bunch of options for creating these links. You can tag them for analytics in the dashboard, or you can even pass data to the new installs or opens that come from the link click. How awesome is that? You need to pass a callback for when you link is prepared (which should return very quickly, ~ 50 ms to process).

For more details on how to create links, see the [Branch link creation guide](https://github.com/BranchMetrics/Branch-Integration-Guides/blob/master/url-creation-guide.md).

####Method

```java
// associate data with a link
// you can access this data from any instance that installs or opens the app from this link (amazing...)

JSONObject dataToInclude = new JSONObject();
try {
	dataToInclude.put("user", "Joe");
	dataToInclude.put("profile_pic", "https://s3-us-west-1.amazonaws.com/myapp/joes_pic.jpg");
	dataToInclude.put("description", "Joe likes long walks on the beach...")

	// customize the display of the Branch link
	dataToInclude.put("$og_title", "Joe's My App Referral");
	dataToInclude.put("$og_image_url", "https://s3-us-west-1.amazonaws.com/myapp/joes_pic.jpg");
	dataToInclude.put("$og_description", "Join Joe in My App - it's awesome");

	// customize the desktop redirect location
	dataToInclude.put("$desktop_url", "http://myapp.com/desktop_splash");
} catch (JSONException ex) { }

// associate a url with a set of tags, channel, feature, and stage for better analytics.
// tags: null or example set of tags could be "version1", "trial6", etc; each tag should not exceed 64 characters
// channel: null or examples: "facebook", "twitter", "text_message", etc; should not exceed 128 characters
// feature: null or examples: Branch.FEATURE_TAG_SHARE, Branch.FEATURE_TAG_REFERRAL, "unlock", etc; should not exceed 128 characters
// stage: null or examples: "past_customer", "logged_in", "level_6"; should not exceed 128 characters

ArrayList<String> tags = new ArrayList<String>();
tags.put("version1");
tags.put("trial6");

// Link 'type' can be used for scenarios where you want the link to only deep link the first time.
// Use _null_, _LINK_TYPE_UNLIMITED_USE_ or _LINK_TYPE_ONE_TIME_USE_

// Link 'alias' can be used to label the endpoint on the link. For example: http://bnc.lt/AUSTIN28. Should not exceed 128 characters
// Be careful about aliases: these are immutable objects permanently associated with the data and associated paramters you pass into the link. When you create one in the SDK, it's tied to that user identity as well (automatically specified by the Branch internals). If you want to retrieve the same link again, you'll need to call getShortUrl with all of the same parameters from before.

Branch branch = Branch.getInstance(getApplicationContext());
branch.getShortUrl(tags, "text_message", Branch.FEATURE_TAG_SHARE, "level_3", dataToInclude, new BranchLinkCreateListener() {
	@Override
	public void onLinkCreate(String url, Branch.BranchError error) {
		if (error == null) {
			// show the link to the user or share it immediately
		} else {
			Log.i("MyApp", error.getMessage());
		}
	}
});

// The callback will return null if the link generation fails (or if the alias specified is aleady taken.)
```

There are other methods which exclude tags and data if you don't want to pass those. Explore the autocomplete functionality.

####Parameters

**alias**: The alias for a link.

**callback**: The callback that is called with the referral code object on success, or an error if it’s invalid.

**channel**: The channel for the link. Examples could be Facebook, Twitter, SMS, etc., depending on where it will be shared. 

**feature**: The feature the generated link will be associated with.

**params**: A dictionary to use while building up the Branch link.

**stage**: The stage used for the generated link, indicating what part of a funnel the user is in.

**tags**: An array of tag strings to be associated with the link.

**Note:**
You can customize the Facebook OG tags of each URL if you want to dynamically share content by using the following _optional keys in the data dictionary_. Please use this [Facebook tool](https://developers.facebook.com/tools/debug/og/object) to debug your OG tags!

| Key | Value
| --- | ---
| "$og_title" | The title you'd like to appear for the link in social media
| "$og_description" | The description you'd like to appear for the link in social media
| "$og_image_url" | The URL for the image you'd like to appear for the link in social media
| "$og_video" | The URL for the video
| "$og_url" | The URL you'd like to appear
| "$og_redirect" | If you want to bypass our OG tags and use your own, use this key with the URL that contains your site's metadata.

Also, you do custom redirection by inserting the following _optional keys in the dictionary_:

| Key | Value
| --- | ---
| "$desktop_url" | Where to send the user on a desktop or laptop. By default it is the Branch-hosted text-me service
| "$android_url" | The replacement URL for the Play Store to send the user if they don't have the app. _Only necessary if you want a mobile web splash_
| "$ios_url" | The replacement URL for the App Store to send the user if they don't have the app. _Only necessary if you want a mobile web splash_
| "$ipad_url" | Same as above but for iPad Store
| "$fire_url" | Same as above but for Amazon Fire Store
| "$blackberry_url" | Same as above but for Blackberry Store
| "$windows_phone_url" | Same as above but for Windows Store

You have the ability to control the direct deep linking of each link by inserting the following _optional keys in the dictionary_:

| Key | Value
| --- | ---
| "$deeplink_path" | The value of the deep link path that you'd like us to append to your URI. For example, you could specify "$deeplink_path": "radio/station/456" and we'll open the app with the URI "yourapp://radio/station/456?link_click_id=branch-identifier". This is primarily for supporting legacy deep linking infrastructure.
| "$always_deeplink" | true or false. (default is not to deep link first) This key can be specified to have our linking service force try to open the app, even if we're not sure the user has the app installed. If the app is not installed, we fall back to the respective app store or $platform_url key. By default, we only open the app if we've seen a user initiate a session in your app from a Branch link (has been cookied and deep linked by Branch)

## Referral System Rewarding Functionality

In a standard referral system, you have two parties: the original user and the invitee. Our system is flexible enough to handle rewards for all users. Here are some example scenarios:

* Reward the original user for taking action (eg. inviting, purchasing, etc.).

* Reward the invitee for installing the app from the original user's referral link.

* Reward the original user when the invitee takes action (eg. give the original user credit when their the invitee buys something).

These reward definitions are created on the dashboard, under the 'Reward Rules' section in the 'Referrals' tab on the dashboard.

***Warning:*** For a referral program, you should not use unique awards for custom events and redeem pre-identify call. This can allow users to cheat the system.

### Get Reward Balance

Reward balances change randomly on the backend when certain actions are taken (defined by your rules), so you'll need to make an asynchronous call to retrieve the balance. Here is the syntax:

####Method
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
####Parameters

None

### Redeem All or Some of the Reward Balance (Store State)

We will store how many of the rewards have been deployed so that you don't have to track it on your end. In order to save that you gave the credits to the user, you can call redeem. Redemptions will reduce the balance of outstanding credits permanently.

####Method

```java
Branch branch = Branch.getInstance(getApplicationContext());
branch.redeemRewards(5);
```
####Parameters
None

### Get Credit History

This call will retrieve the entire history of credits and redemptions from the individual user. To use this call, implement like so:

####Method

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

####Parameters

**referrer**
: The id of the referring user for this credit transaction. Returns null if no referrer is involved. Note this id is the user id in developer's own system that's previously passed to Branch's identify user API call.

**referree**
: The id of the user who was referred for this credit transaction. Returns null if no referree is involved. Note this id is the user id in developer's own system that's previously passed to Branch's identify user API call.

**type**
: This is the type of credit transaction.

1. _0_ - A reward that was added automatically by the user completing an action or referral.
1. _1_ - A reward that was added manually.
2. _2_ - A redemption of credits that occurred through our API or SDKs.
3. _3_ - This is a very unique case where we will subtract credits automatically when we detect fraud.

### Get Referral Code

Retrieve the referral code created by current user.

####Method

```java
Branch branch = Branch.getInstance(getApplicationContext());
branch.getReferralCode(new BranchReferralInitListener() {
	@Override
	public void onInitFinished(JSONObject referralCode, Branch.BranchError error) {
		try {
			String code = referralCode.getString("referral_code");
			// do whatever with code
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
});
```
####Parameters

FILL IN


### Create Referral Code

Create a new referral code for the current user, only if this user doesn't have any existing non-expired referral code.

In the simplest form, just specify an amount for the referral code.
The returned referral code is a 6 character long unique alpha-numeric string wrapped inside the params dictionary with key @"referral_code".

####Method

```java
// Create a referral code of 5 credits
Branch branch = Branch.getInstance(getApplicationContext());
branch.getReferralCode(5, new BranchReferralInitListener() {
	@Override
	public void onInitFinished(JSONObject referralCode, Branch.BranchError error) {
		try {
			String code = referralCode.getString("referral_code");
			// do whatever with code
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
});
```

####Parameters
**amount** _int_
: The amount of credit to redeem when user applies the referral code.

Alternatively, you can specify a prefix for the referral code.
The resulting code will have your prefix, concatenated with a two character long unique alpha-numeric string wrapped in the same data structure.

####Method

```java
Branch branch = Branch.getInstance(getApplicationContext());
branch.getReferralCode("BRANCH", 5, new BranchReferralInitListener() {  // prefix should not exceed 48 characters
	@Override
	public void onInitFinished(JSONObject referralCode, Branch.BranchError error) {
		try {
			String code = referralCode.getString("referral_code");
			// do whatever with code
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
});
```
####Parameters

**prefix** _String_
: The prefix to the referral code that you desire.

If you want to specify an expiration date for the referral code, you can add an expiration parameter.

The prefix parameter is optional here, i.e. it could be getReferralCode(5, expirationDate, new BranchReferralInitListener()...

####Method

```java
Branch branch = Branch.getInstance(getApplicationContext());
branch.getReferralCode("BRANCH", 5, expirationDate, new BranchReferralInitListener() {  // prefix should not exceed 48 characters
	@Override
	public void onInitFinished(JSONObject referralCode, Branch.BranchError error) {
		try {
			String code = referralCode.getString("referral_code");
			// do whatever with code
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
});
```
####Parameters

**expiration** _Date_
: The expiration date of the referral code.

####Method

```java
Branch branch = Branch.getInstance(getApplicationContext());
branch.getReferralCode("BRANCH", 5, expirationDate, "default", REFERRAL_CODE_AWARD_UNLIMITED, REFERRAL_CODE_LOCATION_REFERRING_USER, new BranchReferralInitListener() { // prefix should not exceed 48 characters
	@Override
	public void onInitFinished(JSONObject referralCode, Branch.BranchError error) {
		try {
			String code = referralCode.getString("referral_code");
			// do whatever with code
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
});
```
####Parameters

You can also tune the referral code to the finest granularity, with the following additional parameters:

**bucket** _String_ (max 63 characters)
: The name of the bucket to use. If none is specified, defaults to 'default.'

**calculation_type**  _int_
: This defines whether the referral code can be applied indefinitely, or only once per user.

1. _REFERRAL_CODE_AWARD_UNLIMITED_ - referral code can be applied continually
1. _REFERRAL_CODE_AWARD_UNIQUE_ - a user can only apply a specific referral code once

**location** _int_
: The user to reward for applying the referral code.

1. _REFERRAL_CODE_LOCATION_REFERREE_ - the user applying the referral code receives credit
1. _REFERRAL_CODE_LOCATION_REFERRING_USER_ - the user who created the referral code receives credit
1. _REFERRAL_CODE_LOCATION_BOTH_ - both the creator and applicant receive credit

### Validate Referral Code

Validate if a referral code exists in Branch system and is still valid.
A code is vaild if:

* It hasn't expired.
* If its calculation type is unique, it hasn't been applied by current user.

If valid, returns the referral code JSONObject in the call back.

####Method

```java
Branch branch = Branch.getInstance(getApplicationContext());
branch.validateReferralCode(code, new BranchReferralInitListener() {
	@Override
	public void onInitFinished(JSONObject referralCode, Branch.BranchError error) {
		try {
			if (!referralCode.has("error_message")) {		// will change to using a second callback parameter for error code soon!
				String referral_code = referralCode.getString("referral_code");
				if (referral_code.equals(code)) {
					// valid
				} else {
					// invalid (should never happen)
				}
			} else {
				// invalid
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
});
```

####Parameters

**code** _String_
: The referral code to validate.

### Apply Referral Code

Apply a referral code if it exists in Branch system and is still valid (see above).
If the code is valid, returns the referral code JSONObject in the call back.

####Method

```java
Branch branch = Branch.getInstance(getApplicationContext());
branch.applyReferralCode(code, new BranchReferralInitListener() {
	@Override
	public void onInitFinished(JSONObject referralCode, Branch.BranchError error) {
		try {
			if (!referralCode.has("error_message")) {
				// applied. you can get the referral code amount from the referralCode JSONObject and deduct it in your UI.
			} else {
				// invalid code
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
});
```
####Parameters

**code** _String_
: The referral code to apply.