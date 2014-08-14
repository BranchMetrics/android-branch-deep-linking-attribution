## Installation

Current compiled SDK footprint is *25kb*

### Install library project

Download code from here:
https://s3-us-west-1.amazonaws.com/branchhost/Branch-Android-SDK.zip

The testbed project:
https://s3-us-west-1.amazonaws.com/branchhost/Branch-Android-TestBed.zip

Or just clone this project!

### Register you app

You can sign up for your own app id at http://dashboard.branchmetrics.io

## Configuration (for tracking)

Ideally, you want to use our links any time you have an external link pointing to your app (share, invite, referral, etc) because:

1. Our dashboard can tell you where your installs are coming from
1. Our links are the highest possible converting channel to new downloads and users
1. You can pass that shared data across install to give new users a custom welcome or show them the content they expect to see

Our linking infrastructure will support anything you want to build. If it doesn't, we'll fix it so that it does: just reach out to alex@branchmetrics.io with requests.

### Register an activity for direct deep linking (optional but recommended)

In your project's manifest file, you can register your app to respond to direct deep links (yourapp:// in a mobile browser) by adding the second intent filter block. Also, make sure to change **yourapp** to a unique string that represents your app name.

Typically, you would register some sort of splash activitiy that handles routing for your app.

```xml
<activity
	android:name="com.yourapp.SplashActivity"
	android:label="@string/app_name" >
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

### Initialize SDK And Register Deep Link Routing Function

Called in your splash activity where you handle. If you created a custom link with your own custom dictionary data, you probably want to know when the user session init finishes, so you can check that data. Think of this callback as your "deep link router". If your app opens with some data, you want to route the user depending on the data you passed in. Otherwise, send them to a generic install flow.

This deep link routing callback is called 100% of the time on init, with your link params or an empty dictionary if none present.

```java
@Override
public void onStart() {
	super.onStart();

	Branch branch = Branch.getInstance(getApplicationContext(), "your app key");
	branch.initUserSession(new BranchReferralInitListener(){
		@Override
		public void onInitFinished(JSONObject referringParams) {
			// params are the deep linked params associated with the link that the user clicked before showing up
			// params will be empty if no data found

			// here is the data from the example below if a new user clicked on Joe's link and installed the app
			String name = referringParams.getString("user"); // returns Joe
			String profileUrl = referringParams.getString("profile_pic"); // returns https://s3-us-west-1.amazonaws.com/myapp/joes_pic.jpg
			String description = referringParams.getString("description"); // returns Joe likes long walks on the beach...

			// route to a profile page in the app for Joe
			// show a customer welcome
		}
	}, this.getIntent().getData());
}
```

#### Close session

Optional: If you want to optionally track session lengths, please add this line to your onStop or cleanup routine

```java
@Override
public void onStop() {
	super.onStop();
	Branch.getInstance().closeSession();
}
```

#### Retrieve session (install or open) parameters

These session parameters will be available at any point later on with this command. If no params, the dictionary will be empty. This refreshes with every new session (app installs AND app opens)
```java
Branch branch = Branch.getInstance();
JSONObject sessionParams = branch.getReferringParams(); 
```

#### Retrieve install (install only) parameters

If you ever want to access the original session params (the parameters passed in for the first install event only), you can use this line. This is useful if you only want to reward users who newly installed the app from a referral link or something.
```java
Branch branch = Branch.getInstance();
JSONObject installParams = branch.getInstallReferringParams(); 
```

### Persistent identities

Often, you might have your own user IDs, or want referral and event data to persist across platforms or uninstall/reinstall. It's helpful if you know your users access your service from different devices. This where we introduce the concept of an 'identity'.

To identify a user, just call:
```java
Branch branch = Branch.getInstance();
if (!branch.hasIdentity())
	branch.identifyUser(@"your user id"); 
```

#### Logout

If you provide a logout function in your app, be sure to clear the user when the logout completes. This will ensure that all the stored parameters get cleared and all events are properly attributed to the right identity.

```java
Branch.getInstance().clearUser();
```

### Register custom events

```java
Branch branch = Branch.getInstance(getApplicationContext(), "your app key");
branch.userCompletedAction("your_custom_event"); 
```

OR if you want to store some state with the event

```java
Branch branch = Branch.getInstance(getApplicationContext(), "your app key");
branch.userCompletedAction("your_custom_event", (JSONObject)appState); 
```

Some example events you might want to track:
```java
"complete_purchase"
"wrote_message"
"finished_level_ten"
```

## Generate Tracked, Deep Linking URLs (pass data across install and open)

### Shortened links

There are a bunch of options for creating these links. You can tag them for analytics in the dashboard, or you can even pass data to the new installs or opens that come from the link click. How awesome is that? You need to pass a callback for when you link is prepared (which should return very quickly, ~ 100 ms to process). If you don't want a callback, and can tolerate long links, check out the section right below.

```java
// associate data with a link
// you can access this data from any instance that installs or opens the app from this link (amazing...)

JSONObject dataToInclude = new JSONObject();
try {
	dataToInclude.put("user", "Joe");
	dataToInclude.put("profile_pic", "https://s3-us-west-1.amazonaws.com/myapp/joes_pic.jpg");
	dataToInclude.put("description", "Joe likes long walks on the beach...")
} catch (JSONException ex) {
	
}

// associate a url with a set of tags, channel, feature, and stage for better analytics.
// tags: null or example set of tags could be "version1", "trial6", etc
// channel: null or examples: "facebook", "twitter", "text_message", etc
// feature: null or examples: Branch.FEATURE_TAG_SHARE, Branch.FEATURE_TAG_REFERRAL, "unlock", etc
// stage: null or examples: "past_customer", "logged_in", "level_6"

ArrayList<String> tags = new ArrayList<String>();
tags.put("version1");
tags.put("trial6");

Branch branch = Branch.getInstance();
branch.getShortUrl(tags, "text_message", Branch.FEATURE_TAG_SHARE, "level_3", dataToInclude, new BranchLinkCreateListener() {
	@Override
	public void onLinkCreate(String url) {
		// show the link to the user or share it immediately
	}
});
```

There are other methods which exclude tags and data if you don't want to pass those. Explore the autocomplete functionality.

**Note** 
You can customize the Facebook OG tags of each URL if you want to dynamically share content by using the following optional keys in the params JSONObject:
```java
"$og_app_id"
"$og_title"
"$og_description"
"$og_image_url"
```

Also, you do custom redirection by inserting the following optional keys in the dictionary
```java
"$desktop_url"
"$android_url"
"$ios_url"
"$ipad_url"
```

## Referral system rewarding functionality

In a standard referral system, you have 2 parties: the original user and the invitee. Our system is flexible enough to handle rewards for all users. Here are a couple example scenarios:

1) Reward the original user for taking action (eg. inviting, purchasing, etc)

2) Reward the invitee for installing the app from the original user's referral link

3) Reward the original user when the invitee takes action (eg. give the original user credit when their the invitee buys something)

These reward definitions are created on the dashboard, under the 'Referral Program Configuration' **coming soon** Please contact alex@branchmetrics.io and he will create these rules manually for you.

Warning: For a referral program, you should not use unique awards for custom events and redeem pre-identify call. This can allow users to cheat the system.

### Get reward balance

Reward balances change randomly on the backend when certain actions are taken (defined by your rules), so you'll need to make an asynchronous call to retrieve the balance. Here is the syntax:

```java
Branch branch = Branch.getInstance();
branch.loadRewards(new BranchReferralStateChangedListener() {
	@Override
	public void onStateChanged(boolean changed) {
		// changed boolean will indicate if the balance changed from what is currently in memory

		// will return the balance of the current user's credits
		int credits = branch.getCredits();
	}
});
```

### Redeem all or some of the reward balance (store state)

We will store how many of the rewards have been deployed so that you don't have to track it on your end. In order to save that you gave the credits to the user, you can call redeem. Redemptions will reduce the balance of outstanding credits permanently.

```java
Branch branch = Branch.getInstance();
branch.redeemRewards(5);
```
