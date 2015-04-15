## Important migration to v1.5.0

We have deprecated the bnc_app_key and replaced that with the new branch_key. Please see [add branch key](#add-your-branch-key-to-your-project) for details.

## Important migration to v1.4.5

Branch uses Facebook's App Links metatags automatically to provide the best linking from the Facebook platform. Unfortunately, Facebook changed the way they handle App Links routing in the latest update on April 8ish.

Two important things to do in order to properly handle deep links from Facebook

1. Make sure to update the Manifest so that the Activity with the intent filter for your URI scheme has *launchMode:singleTask*. See example [here](https://github.com/BranchMetrics/Branch-Android-SDK#register-an-activity-for-direct-deep-linking-optional-but-recommended)

2. Make sure to add this snippet of code to the Activity registered as singleTask.
	```java
	@Override
	public void onNewIntent(Intent intent) {
		// Android makes you do this yourself for some reason, so make sure
		// this snippet is in the Activity registered for the intent filter
		this.setIntent(intent);
	}
	```

3. Update the SDK to v1.4.5 or higher

## FAQ

1.) __What if Branch goes down or there is a poor connection?__

At Branch, we live and breath uptime &amp; performance. We've developed mechanisms internal to the SDK to deal with network issues. We always call the callbacks with the error parameter describing the issue. If the phone is in airplane mode and the connection is not available, the callbacks are called immediately. If there is a server latency, we timeout after 3 seconds and will retry 4 more times with a 3 second pause in between each. These timeouts are adjustable on the singleton instance by calling: ```setNetworkTimeout``` (ms), ```setRetryCount``` and ```setRetryInterval``` (ms).

2.) __How can I debug or test the SDK?__

Just call ```setDebug()``` after you get a reference to the Branch singleton. We'll log all requests. More importantly, we won't reference the hardware ID of the phone so you can register installs after uninstalling and reinstalling the app.

*Note: Make sure to remove this line before releasing!*

3.) __Is there any way to start debugging dynamically?__

Yes. Even if you don't call setDebug(), you can still start debugging dynamically. When you are testing your app, just put four fingers on your phone screen (or just single touch on simulator) and hold for three seconds, and you should be able to see an indication of start debug session in the log. From then on, all requests will be logged. If you have signed into our dashboard at that time and are in the "Debug" page, this will even start a remote debug session. To enable this feature, make sure you pass "this" as the third parameter when you call ```initSession``` in the Activity's ```onStart()```.

4.) __Facebook deep links seem to not work?__

Branch uses the Facebook App Links protocol to pass the deep links through to your app from Facebook. Funny enough, if you also have a Facebook app configured in the developer portal and you choose 'Deep link from feed', Facebook ignores it's own protocol. Make sure to *uncheck* this option in your Facebook app.

5.) __Why do I not see any installs after I reinstall?__

We do a lot of smart things to give you an accurate read on the number of installs you actually have. The most common one is associating the user with the actual hardware ID of the phone. If a user uninstalls the app, then reinstalls, we'll know it's the same person from before and just register the user as 'open' instead of an 'install.' To register an install on the same phone again, see FAQ #2 about debugging.

6.) __Chrome seems to take me to Google Play all the time. Why?__

Chrome is very picky about opening up the app directly. Chrome utilizes the intent system to try to open up the app, and fails back to the Play Store far too easily. Here are 3 things to verify:

  1. Make sure the package name for the test build matches the package registered in the Branch settings and matches the package in Google play.

  2. Make sure the URI scheme of the test build matches the Branch settings.

  3. Verify that you've added the proper host 'open' in the Manifest - see [here](https://github.com/BranchMetrics/Branch-Android-SDK#register-an-activity-for-direct-deep-linking-optional-but-recommended)


7.) __Why is the most recent version of Chrome (40+) is showing me a page not found error?__

The Google Chrome team decided that it didn't want to try to open up the app if a user manually entered in a link into Chrome - see the history of this issue [here](https://code.google.com/p/chromium/issues/detail?id=331571). The scope of the bug applies to people who copy and paste a Branch link into Chrome, whereas anyone who clicks on a link in Chrome or something that opens the link in Chrome will properly redirect.

We're exploring a work around as well as campaigning the Chrome team to change this. If you work at Google, or know someone there, [help us get this resolved](https://branch.io/contact/) :)

## Installation

Current compiled SDK footprint is *40kb*

### Install library project

Import the SDK as a Gradle dependency (for Android Studio):
* Right click on the main module within your project (this is called 'app' by default).
* Select **Open Module Settings**.
* Within the **Dependencies** tab, click the **+** button at the bottom of the window and select **Library Dependency**.
* Type *branch*, and hit the enter key to search Maven Central for the Branch SDK Library.
* Select the latest *io.branch.sdk.android:library* item listed and accept the changes.

See the [Android Quick Start Guide for more detail](https://github.com/BranchMetrics/Branch-Integration-Guides/blob/master/android-quick-start.md) and a screencasted walkthrough.

Or download the JAR file from here:
https://s3-us-west-1.amazonaws.com/branchhost/Branch-Android-SDK.zip

The testbed project:
https://s3-us-west-1.amazonaws.com/branchhost/Branch-Android-TestBed.zip

Or just clone this project!

### Register Your App

You can sign up for your own app id at [https://dashboard.branch.io](https://dashboard.branch.io)

## Configuration (for tracking)

Ideally, you want to use our links any time you have an external link pointing to your app (share, invite, referral, etc) because:

1. Our dashboard can tell you where your installs are coming from
1. Our links are the highest possible converting channel to new downloads and users
1. You can pass that shared data across install to give new users a custom welcome or show them the content they expect to see

Our linking infrastructure will support anything you want to build. If it doesn't, we'll fix it so that it does: just reach out to alex@branch.io with requests.

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

### Add your branch key to your project

After you register your app, your branch key can be retrieved on the [Settings](https://dashboard.branch.io/#/settings) page of the dashboard. Now you need to add it (them, if you want to do it for both your live and test apps) to your project.

1. Edit your res/values/strings.xml file by adding a new string resource with "branch_key" as the name and your live branch key as the value
    ```xml
    <resources>
        <!-- Other existing resources -->

        <!-- Add this string resource below, and change "key_live_xxxxxxx" to your actual live branch key -->
        <string name="branch_key">key_live_xxxxxxx</string>

        <!-- For your test app, if you have one; Again, use your actual test branch key -->
        <string name="branch_key_test">key_test_yyyyyyy</string>
    </resources>
    ```

1. Edit your manifest file by adding the following new meta-data
    ```xml
    <application>
        <!-- Other existing entries -->

        <!-- Add this meta-data below; DO NOT changing the android:value -->
        <meta-data android:name="io.branch.sdk.BranchKey" android:value="@string/branch_key" />

        <!-- For your test app, if you have one -->
        <meta-data android:name="io.branch.sdk.BranchKey.test" android:value="@string/branch_key_test" />
    </application>
    ```

### Initialize SDK And Register Deep Link Routing Function

Called in your splash activity where you handle. If you created a custom link with your own custom dictionary data, you probably want to know when the user session init finishes, so you can check that data. Think of this callback as your "deep link router". If your app opens with some data, you want to route the user depending on the data you passed in. Otherwise, send them to a generic install flow.

This deep link routing callback is called 100% of the time on init, with your link params or an empty dictionary if none present.

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

#### Close session

Required: this call will clear the deep link parameters when the app is closed, so they can be refreshed after a new link is clicked or the app is reopened.

```java
@Override
public void onStop() {
	super.onStop();
	Branch.getInstance(getApplicationContext()).closeSession();
}
```

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

## Generate Tracked, Deep Linking URLs (pass data across install and open)

### Shortened links

There are a bunch of options for creating these links. You can tag them for analytics in the dashboard, or you can even pass data to the new installs or opens that come from the link click. How awesome is that? You need to pass a callback for when you link is prepared (which should return very quickly, ~ 50 ms to process).

For more details on how to create links, see the [Branch link creation guide](https://github.com/BranchMetrics/Branch-Integration-Guides/blob/master/url-creation-guide.md)

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

**Note**
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
| "$android_url" | The replacement URL for the Play Store to send the user if they don't have the app. Currently, Chrome does not support this override. _Only necessary if you want a mobile web splash_
| "$android_bypass_chrome_intent" | Pass in this key, with the value 1 or true to override the Chrome intent, and redirect 100% of the time to $android_url. Otherwise, on Chrome, it will either open the app or fall back to the Play Store
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

## Referral system rewarding functionality

In a standard referral system, you have 2 parties: the original user and the invitee. Our system is flexible enough to handle rewards for all users. Here are a couple example scenarios:

1) Reward the original user for taking action (eg. inviting, purchasing, etc)

2) Reward the invitee for installing the app from the original user's referral link

3) Reward the original user when the invitee takes action (eg. give the original user credit when their the invitee buys something)

These reward definitions are created on the dashboard, under the 'Reward Rules' section in the 'Referrals' tab on the dashboard.

Warning: For a referral program, you should not use unique awards for custom events and redeem pre-identify call. This can allow users to cheat the system.

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

### Get referral code

Retrieve the referral code created by current user

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

### Create referral code

Create a new referral code for the current user, only if this user doesn't have any existing non-expired referral code.

In the simplest form, just specify an amount for the referral code.
The returned referral code is a 6 character long unique alpha-numeric string wrapped inside the params dictionary with key @"referral_code".

**amount** _int_
: The amount of credit to redeem when user applies the referral code

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

Alternatively, you can specify a prefix for the referral code.
The resulting code will have your prefix, concatenated with a 4 character long unique alpha-numeric string wrapped in the same data structure.

**prefix** _String_
: The prefix to the referral code that you desire

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

If you want to specify an expiration date for the referral code, you can add an expiration parameter.
The prefix parameter is optional here, i.e. it could be getReferralCode(5, expirationDate, new BranchReferralInitListener()...

**expiration** _Date_
: The expiration date of the referral code

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

You can also tune the referral code to the finest granularity, with the following additional parameters:

**bucket** _String_ (max 63 characters)
: The name of the bucket to use. If none is specified, defaults to 'default'

**calculation_type**  _int_
: This defines whether the referral code can be applied indefinitely, or only once per user

1. _REFERRAL_CODE_AWARD_UNLIMITED_ - referral code can be applied continually
1. _REFERRAL_CODE_AWARD_UNIQUE_ - a user can only apply a specific referral code once

**location** _int_
: The user to reward for applying the referral code

1. _REFERRAL_CODE_LOCATION_REFERREE_ - the user applying the referral code receives credit
1. _REFERRAL_CODE_LOCATION_REFERRING_USER_ - the user who created the referral code receives credit
1. _REFERRAL_CODE_LOCATION_BOTH_ - both the creator and applicant receive credit

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

### Validate referral code

Validate if a referral code exists in Branch system and is still valid.
A code is vaild if:

1. It hasn't expired.
1. If its calculation type is uniqe, it hasn't been applied by current user.

If valid, returns the referral code JSONObject in the call back.

**code** _String_
: The referral code to validate

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

### Apply referral code

Apply a referral code if it exists in Branch system and is still valid (see above).
If the code is valid, returns the referral code JSONObject in the call back.

**code** _String_
: The referral code to apply

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
