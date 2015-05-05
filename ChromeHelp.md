### How to file a Chrome issue for typed-in links issue

1. Visit https://code.google.com/p/chromium/issues/list and click **New issue** at the top left

2. Enter/choose the following values into the first page of the form and click Next at the bottom:

	_Chrome version:_ 40 +

	_Operating system:_ Android

	_Version:_ Any

	_Channel:_ Stable

	_Flash version:_ n/a

3. Choose "Content" and click Next at the bottom

4. Enter/choose the following values:

	_Please enter a one-line summary:_ Typed-in links disallow redirect to Chrome intent to open native app

	_What specific URL can reproduce the problem?_ https://bnc.lt/chromeissue

	_Does the problem occur on multiple sites?_ Yes

	_Is it a problem with a plugin?_ No

	_Does this feature work correctly other browsers?:_ Yes

	_Steps to reproduce the problem:_

		1. Type-in or paste in the link directly to Android Chrome 40+
		2. Press Go
		3. See an empty, blank screen

	_What is the expected behavior?_

		Should redirect to the Play Store if the app is not installed, or open up the app if it is installed. We're setting window.location to the proper intent string as specified here: 
		https://developer.chrome.com/multidevice/android/intents

		If you instead send yourself that same URL via email, SMS or whatever, then click it, you'll notice that the redirect to the Play Store or to the app occurs correctly.

	_What went wrong?_

		In some release in Chrome 40, it was decided that typed-in URLs should prevent automated redirects. It seems that there wasn't much of an issue that was resolved here, and the intention was lost as the issue was passed from person to person.
		https://code.google.com/p/chromium/issues/detail?id=331571

		This change unfortunately hurts developers who don't have mobile websites and depend on redirects to drive a majority of their app traffic. We have had to build an intermediate splash page with a giant button that launches the Chrome intent when pressed, when the most optimal user experience would be a redirect to the Play Store or the app.

	_Did this work before?_ Yes

	_When did it work?_ Pre 40
	
	_Any other comments?_ Please help us.