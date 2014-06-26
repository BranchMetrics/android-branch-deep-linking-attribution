## Installation

### Install library project

Download code from here:
https://s3-us-west-1.amazonaws.com/branchhost/Branch-Android-SDK.zip

The testbed project:
https://s3-us-west-1.amazonaws.com/branchhost/Branch-Android-TestBed.zip

Or just clone this project!

### Initialize SDK (registers install/open events)

Called when app first initializes a session. It's safe to call this multiple times on a session if you have trouble isolating it to a single call per session.
```java
Branch branch = Branch.getInstance(getApplicationContext(), "your app key");
branch.initUserSession();
```

#### OR

If you created a custom link with your own custom dictionary data, you probably want to know when the user session init finishes, so you can check that data. If so, you can init with this callback:
```java
Branch branch = Branch.getInstance(getApplicationContext(), "your app key");
branch.initUserSession(new BranchReferralInitListener(){
	@Override
	public void onInitFinished(JSONObject referringParams) {
		// show the user some custom stuff or do some action based on what data you associate with a link
		// will be empty if no data found

		// here is the data from the example below if a new user clicked on Joe's link and installed the app
		String name = referringParams.getString("user"); // returns Joe
		String profileUrl = referringParams.getString("profile_pic"); // returns https://s3-us-west-1.amazonaws.com/myapp/joes_pic.jpg
		String description = referringParams.getString("description"); // returns Joe likes long walks on the beach...
	}
});
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
JSONObject sessionParams = branch.getInstallReferringParams(); 
```

### Persistent identities

Often, you might have your own user IDs, or want referral and event data to persist across platforms or uninstall/reinstall. It's helpful if you know your users access your service from different devices. This where we introduce the concept of an 'identity'.

To identify a user, just call:
```java
Branch branch = Branch.getInstance();
JSONObject sessionParams = branch.identifyUser(@"your user id"); 
```

#### OR

We store these identities, and associate the referral connections among them. Therefore, if we see that you are identifying a user that already exists, we'll return the parameters associated with the first creation of that identity. You just need to register for the callback block.

```java
Branch branch = Branch.getInstance();
JSONObject sessionParams = branch.identifyUser(@"your user id", new BranchReferralInitListener() {	
	@Override
	public void onInitFinished(JSONObject installParams) {
		// here is the data from the example below if a new user clicked on Joe's link and installed the app
		String name = installParams.getString("user"); // returns Joe
		String profileUrl = installParams.getString("profile_pic"); // returns https://s3-us-west-1.amazonaws.com/myapp/joes_pic.jpg
		String description = installParams.getString("description"); // returns Joe likes long walks on the beach...
	}
});
```

You can access these parameters at any time thereafter using this call.
```java
Branch branch = Branch.getInstance();
JSONObject installParams = branch.getInstallReferringParams();
```

#### Logout

If you provide a logout function in your app, be sure to clear the user when the logout completes. This will ensure that all the stored parameters get cleared and all events are properly attributed to the right identity.

```java
Branch branch = Branch.getInstance();
JSONObject installParams = branch.clearUser();
```


### Register custom events

```java
Branch branch = Branch.getInstance(getApplicationContext(), "your app key");
branch.userCompletedAction("your_custom_event"); 
```

Some example events you might want to track:
```java
"complete_purchase"
"wrote_message"
"finished_level_ten"
```

## Use

### Generate URLs

#### Short links (for social media sharing)

There are a bunch of options for creating these links. You can tag them for analytics in the dashboard, or you can even pass data to the new installs or opens that come from the link click. How awesome is that? You need to pass a callback for when you link is prepared (which should return very quickly, ~ 100 ms to process). If you don't want a callback, and can tolerate long links, check out the section right below.

```java
// get a simple url to track events with
Branch branch = Branch.getInstance(getApplicationContext(), "your app key");
branch.getShortUrl(new BranchLinkCreateListener() {
	@Override
	public void onLinkCreate(String url) {
		// show the link to the user or share it immediately
	}
});

// or 
// associate data with a link
// you can access this data from anyone instance that installs or opens the app from this link (amazing...)
JSONObject dataToInclude = new JSONObject();
try {
	dataToInclude.put("user", "Joe");
	dataToInclude.put("profile_pic", "https://s3-us-west-1.amazonaws.com/myapp/joes_pic.jpg");
	dataToInclude.put("description", "Joe likes long walks on the beach...")
} catch (JSONException ex) {
	
}

branch.getShortUrl(dataToInclude, new BranchLinkCreateListener() {
	@Override
	public void onLinkCreate(String url) {
		// show the link to the user or share it immediately
	}
});

// or 
// get a url with a tag for analytics in the dashboard
// example tag could be "fb", "email", "twitter"

branch.getShortUrl("twitter", new BranchLinkCreateListener() {
	@Override
	public void onLinkCreate(String url) {
		// show the link to the user or share it immediately
	}
});

// or

branch.getShortUrl("twitter", dataToInclude, new BranchLinkCreateListener() {
	@Override
	public void onLinkCreate(String url) {
		// show the link to the user or share it immediately
	}
});
```

#### Long links (immediate return but no shortening done)

Generating long links are immediate return, but can be long as the associated parameters are base64 encoded into the url itself.

```java
// get a simple url to track events with
Branch branch = Branch.getInstance(getApplicationContext(), "your app key");
String urlToShare = branch.getLongURL();
```

all of the above options with tagging and data passing are available.

### Referral system rewarding functionality

In a standard referral system, you have 2 parties: the original user and the invitee. Our system is flexible enough to handle rewards for all users. Here are a couple example scenarios:

1) Reward the original user for taking action (eg. inviting, purchasing, etc)

2) Reward the invitee for installing the app from the original user's referral link

3) Reward the original user when the invitee takes action (eg. give the original user credit when their the invitee buys something)

These reward definitions are created on the dashboard, under the 'Referral Program Configuration' **coming soon** Please contact alex@branchmetrics.io and he will create these rules manually for you.

#### Get reward balance

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

#### Redeem all or some of the reward balance (store state)

We will store how many of the rewards have been deployed so that you don't have to track it on your end. In order to save that you gave the credits to the user, you can call redeem. Redemptions will reduce the balance of outstanding credits permanently.

```java
Branch branch = Branch.getInstance();
branch.redeemRewards(5);
```