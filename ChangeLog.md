Branch Android SDK change log

- v2.0.3 Hot fix for NPE with get request when Branch API services is down.

- v2.0.2 Support for delayed "onNewIntent()" call on certain Android versions. Clean up for unused server requests.

- v2.0.1 Adding an option to white list the Uri schemes that should be collected by Branch SDK when app is opened with an external intent.

- v2.0.0: Removing support for Manual session handling. This version onwards only Auto session management is supported. This requires minimum API level set to 14 or above.
  If not using auto session already please call "Branch.getAutoInstance(application)" from your Application Classes "onCreate()" method.
  If your application doesn't have an Application class just use "BranchApp" as your application class by adding it in manifest(add "android:name="BranchApp" in application tag")
  There is not need to call "closeSession()" API explicitly. If you want to support minimum API level less than 9 please consider using SDK version 1.14.5 (support minimum sdk version 9)

- v1.14.5: Using Thread Pool to execute Branch request in parallel to improve Branch API response time. Optimizations for performance improvements

- v1.14.4: Adding more styling options to Share Sheet. Disabling Facebook App-Link check by default.

- v1.14.3: Cleaner fix for jcenter dependency issue.

- v1.14.2: HotFix: Build version not updated issue.

- v1.14.1: adding update option for Answers-Shim SDK.

- v1.14.0: Fabric Answers integration.

- v1.13.1: Hot Fix : Design changes to ensure init callbacks are always called. Relocating fabric properties file.

- v1.13.0: Adding feature to turn on/off collecting device id.

- v1.12.1: Hot fix : initSession is not called back on some scenarios with the new feature added on 1.12.0.

- v1.12.0: Added check for Facebook App Links on client side. Added support for list BUO in Google search. Support for adding metadata to Branch requests.

- v1.11.3: Adding more client side error handling for BranchViews.

- v1.11.2: Removing unnecessary device param capture.

- v1.11.1: Adding support for customising share sheet messages.

- v1.11.0: Adding SDK support for application landing pages.

- v1.10.8: Hot fix for NPE caused by 1.10.7.

- v1.10.7: Hot fix for a possible memory leak caused by holding strong reference to context.

- v1.10.6: Few enhancements to cached link data matching before making a create url call.

- v1.10.5: Removing touch debug feature and some deprecated methods.

- v1.10.4: Fix for corner case concurrent modification error present in 1.10.3.

- v1.10.3: Fix for incorrect params on init with app-links. Fix for concurrent modification error and few other bugs. Added error codes for Branch errors.

- v1.10.2: Added instrumentation support and deep link debug feature.

- v1.10.1: Added fix for crash issues with share sheet. Added more specific error messages.

- v1.10.0: Added support for the brand new BranchUniversalObject for easy tracking of views on content, easy creation of links and easy sharing

- v1.9.0: Support for Android App-links. Deprecating loadActionCount API

- v1.8.12: Fix for issue link parameters are provided in init session callback multiple times. Bug fixes for handling requests queued before initialising session.

- v1.8.11: Fix for issue deep link params are empty when session initialised on with a delay with a multi threaded environment.

- v1.8.10: Fix for a possible NPE on sharing link in slow network conditions.

- v1.8.9: Fix issue requests not removed from request queue on resource conflict. Fixing couple of possible NPE and Memory leaks.

- v1.8.8: Fix for callback not invoked on session error when creating a short link with builder.

- v1.8.7: Adding internationalization support for sharing link builder. Additional callbacks are also added to notify sharing dialog is launch and dismiss events.

- v1.8.6: Adding option to specify email/sms subject to the share link builder.

- v1.8.5: Fix crash on install app from market due to referrer string parse error.

- v1.8.4: Deprecate setDebug and migrate to use <meta-data android:name="io.branch.sdk.TestMode" android:value="true" /> in Manifest instead. Added support to specify deep link path to Activity.

- v1.8.3: Fix for SMV issue caused by resource leak. Changes to add link parameters to intent for auto deep linked Activities.

- v1.8.2: Fix issue test and live keys are not set properly with auto session management when not using BranchApp.

- v1.8.1: Removing unnecessary reading of URI scheme. URI Scheme is read only when running in debug mode.

- v1.8.0: Adding new auto deep linking feature.

- v1.7.2: Fixed bug with handling Activity intent in auto session management.

- v1.7.1: Fixed bug with getting  Google Ads ID.

- v1.7.0: Adding an easy way to share custom deep links with social media and other applications. Provides a customisable chooser dialog for selecting applications.

- v1.6.2: Removed the BranchException thrown by SDK method. Added more logs to notify errors.

- v1.6.1: Fixed crashing issue when used with API-level below 14.

- v1.6.O: Improved thread handling and request processing mechanism. Fixed few bugs related with session management in auto mode. Also added fixes for handling network down condition properly.

- v1.5.11: Fix for a handling pending open request in queue while there is no valid identity_id.

- v1.5.10: Fix for a possible memory leak with touch-debugging feature.

- v1.5.9: Enhanced debugger triggering with the new session management. Also, fixed a bug in the new session management when the app already has a class extending from BranchApp.

- v1.5.8: Fix automated session management. Changes added to block multiple Activity life cycle listeners getting registered.

- v1.5.7: Added automated session management. Getting rid of session handling with each Activity's life cycle methods.

- v1.5.6: Add limit to queue retrieval to prevent legacy stack overflow ghosts.

- v1.5.5: Fix null server request tag bug.

- v1.5.4: Fix concurrent modification bug in ServerRequestQueue. Some code was added where there was rare possibility of modifying the ServerRequest body at the same time we were iterating through it.

- v1.5.3: Simplified branch_key setup by directly specifying it in Manifest.xml

- v1.5.2: Added retry count to all queries + a little better handling of retries

- v1.5.1: Added Javadocs for easy explanation of all methods

- v1.5.0: We have deprecated the use of `bnc_app_key` and are now using `branch_key`, which can be obtained in the settings page of your Dashboard. The replacement in the SDK should happen in the manifest and strings.xml (see README for details), as well as in `public static Branch getInstance(Context context, String branchKey)` if necessary.

- v1.4.5: Better handling of intent data for state transitions from Facebook's AppLinks

- v1.4.4: Fixed potential issue where update state was improperly set if no isReferrable specified

- v1.4.3: Added new update state to differentiate between update and open

- v1.4.2.2: Bullet proof prevention of duplicate callClose (reported StackOverflow error)

- v1.4.2.1: Catch OOM during parsing public dir

- v1.4.2: Added Unit tests

- v1.4.1: Fixed synchronization issue in persistence queue

- v1.4.0: Exposed duration in getShortUrl for tuning link click match duration

- v1.3.9: Added API Key to GET

- v1.3.8: Check memory when getting URI Scheme

- v1.3.7: Added app listing

- v1.3.6: Fix pre Honeycomb (3.0) bug

- v1.3.5: Added advertising id to init params (optional)

- v1.3.4: Added optional advertising id to install params

- v1.3.3: Fixed issue with null tags

- v1.3.2: Added API's to getShortURL synchronously

- v1.3.1: Enforce setting app key before any API call; Provided ability to turn off smart session

- v1.3.0: Added setDebug call to enable logging and use a random hardware ID (helpful for referral testings). Also, cacheing of links to save on network requests

- v1.2.9: Moved app key to strings.xml; Added constants for OG tags and redirect URLs

- v1.2.8: Fixed close issue due to rotation

- v1.2.7: Check if URI is hierarchical

- v1.2.6: Handle not init-ed case

- v1.2.5: Proper debug connection

- v1.2.4: Fixed rare race condition

- v1.2.3: Added BranchError to callbacks

- v1.2.2: Added Branch remote debug feature

- v1.2.1: Proper network callbacks; Added query limit

- v1.2.0: Cleanup semaphore issue

- v1.1.9: Fixed request before init issue

- v1.1.8: Added link alias