## Installation

### Install library project

Download code from here (not yet live):
https://s3-us-west-1.amazonaws.com/branchhost/Branch-SDK.zip

The testbed project (not working yet):
https://s3-us-west-1.amazonaws.com/branchhost/Branch-SDK-TestBed.zip

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
	public void onInitFinished() {
		JSONObject myParams = branch.getReferringParams();
		// show the user some custom stuff or do some action based on what data you associate with a link
		// will be empty if no data found

		// here is the data from the example below if a new user clicked on Joe's link and installed the app
		String name = myParams.getString("user"); // returns Joe
		String profileUrl = myParams.getString("profile_pic"); // returns https://s3-us-west-1.amazonaws.com/myapp/joes_pic.jpg
		String description = myParams.getString("description"); // returns Joe likes long walks on the beach...
	}
});
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

#### Long links (immediate return but no shortening done)

There are a bunch of options for creating these links. You can tag them for analytics in the dashboard, or you can even pass data to the new installs or opens that come from the link click. How awesome is that?

```java
// get a simple url to track events with
Branch branch = Branch.getInstance(getApplicationContext(), "your app key");
String urlToShare = branch.getLongURL();

// get a url with a tag for analytics in the dashboard
// example tag could be "fb", "email", "twitter"
Branch branch = Branch.getInstance(getApplicationContext(), "your app key");
String urlToShare = branch.getLongURL("twitter");

// associate data with a link
// you can access this data from anyone instance that installs or opens the app from this link (amazing...)
JSONObject dataToInclude = new JSONObject();
try {
	dataToInclude.put("user", "Joe");
	dataToInclude.put("profile_pic", "https://s3-us-west-1.amazonaws.com/myapp/joes_pic.jpg");
	dataToInclude.put("description", "Joe likes long walks on the beach...")
} catch (JSONException ex) {
	
}
Branch branch = Branch.getInstance(getApplicationContext(), "your app key");
String urlToShare = branch.getLongURL(dataToInclude);

// or

String urlToShare = branch.getLongURL("twitter", dataToInclude);

```

#### Short links (for social media sharing)

All of the above options are the same (ie tagging, data passing) but you need to pass a callback. This will be called when you link is prepared (which should return very quickly, ~ 100 ms to process)

```java
// get a simple url to track events with
Branch branch = Branch.getInstance(getApplicationContext(), "your app key");
branch.getShortUrl(new BranchLinkCreateListener() {
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
	public void onLinkCreate(String url) {
		// show the link to the user or share it immediately
	}
});

// or 
// get a url with a tag for analytics in the dashboard
// example tag could be "fb", "email", "twitter"

branch.getShortUrl("twitter", new BranchLinkCreateListener() {
	public void onLinkCreate(String url) {
		// show the link to the user or share it immediately
	}
});

// or

branch.getShortUrl("twitter", dataToInclude, new BranchLinkCreateListener() {
	public void onLinkCreate(String url) {
		// show the link to the user or share it immediately
	}
});
```

### Get/reward event points

These functions will help you reward your users for sharing their links, and save the fact that you rewarded them to our server.


To get the number of install events that occurred from this user's links:

```java

Branch branch = Branch.getInstance(getApplicationContext(), "your app key");
branch.loadPoints(new BranchReferralStateChangedListener() {
	public void onStateChanged() {
		int newInstallsFromUser = branch.getBalance("install")

		// reward the user
	}
});


// adds two credits towards the outstanding balance
// this will reduce the number returned by getBalance("install") by 2
branch.creditUserForReferral("install", 2);

```
