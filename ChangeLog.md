# Branch Android SDK change log
- v5.14.0
* _*Master Release*_ - Nov 18, 2024
  - Added `setConsumerProtectionAttributionLevel` for controlling attribution data collection
  - Deprecated `disableTracking` in favor of `setConsumerProtectionAttributionLevel(NONE)`
  
- v5.13.0
* _*Master Release*_ - Oct 29, 2024
  - Removal of internal event caching behavior
  - Addition of unique id per request in POST body
  - Change in behavior of tracking controller, when re-enabled will run inititialization tasks.

- v5.12.4
  * _*Master Release*_ - Sep 30, 2024
  - Reverted the minimum SDK build version back to 21
  
- v5.12.3
  * _*Master Release*_ - Sep 25, 2024
  - Fix to properly fetch install referrers when using deferred init in plugins
  
- v5.12.2
  * _*Master Release*_ - Jul 19, 2024
  - Add minification config for optional Billing Client
  - Add additional detail in exception messages

- v5.12.1
  * _*Master Release*_ - Jun 7, 2024
  - Added support for setting FB App ID through `branch.json`

- v5.12.0
  * _*Master Release*_ - Apr 24, 2024
  - Added new enableLogging method to set preferred logging level

- v5.11.0
  * _*Master Release*_ - Apr 1, 2024
  - Added support for setting Branch API URL through `branch.json`
  - Improved performance for best case of user agent string fetch
  
- v5.10.2
  * _*Master Release*_ - Mar 27, 2024
  - Improved start up performance for applications that are using `Branch.setIsUserAgentSync(true)`

- v5.10.1
  * _*Master Release*_ - Mar 13, 2024
    - Track Meta Install Referrer view-through installs
- v5.10.0
  * _*Master Release*_ - Mar 8, 2024
    - Introduced Meta Install Referrer tracking
    - Added new method for using the native Android share sheet
    - Added additional logging with improved formatting
- v5.9.0
  * _*Master Release*_ - Feb 15, 2024
    - Added new useEUEndpoint method
    - Added support for setting DMA Compliance Parameters
    - Removed v1/profile and v1/logout requests
- v5.8.2
  * _*Release Branch 5.8.2*_ - Jan 26, 2024
    - Removed deprecated field target SDK version from library manifest.
    - Updated Gradle.
- v5.8.1
  * _*Release Branch 5.8.1*_ - Jan 18, 2024
    - Preserve logging to console when SDK is not in debug.

- v5.8.0
  * _*Release Branch 5.8.0*_ - Dec 11, 2023
    - Fix a condition where the SDK would not init if the Google service was unavailable to provide the Ad ID.
    - Add a simple interface to echo SDK logs for developer's implementation

- v5.7.5
  * _*Release Branch 5.7.5*_ - Nov 27, 2023
    - Fix a race condition where two quickly queued consecutive init/reInit requests would overwrite uri arguments and return no params.
    - Added more verbose trace logging
- v5.7.4
  * _*Release Branch 5.7.4*_ - Nov 21, 2023
    - Added check for non-hierarchical URIs when parsing query parameters.
- v5.7.3
  * _*Release Branch 5.7.3*_ - Nov 9, 2023
    - Updated network debug logging to reduce confusion when client side errors occur rather than attribute to server status.
    - Added exception message to init callback error message.
- v5.7.2
  * _*Release Branch 5.7.2*_ - Oct 20, 2023
    - Added additional fields to initialization and event requests
- v5.7.1
  * _*Master Release*_ - Oct 3, 2023
    - Fixed bug that prevented session from initializing after opening a link with gclid
    - Removed remaining TUNE references
    - Factored out queueing code from the Branch object
- v5.7.0
  * _*Master Release*_ - Sep 1, 2023
    - SDK internal logging behavior has changed. Messages will now be sent through standard channels (`ERROR`, `WARN`, `INFO`, `DEBUG`, `VERBOSE`). Before, nearly all logs were sent to `INFO`.
    - Fixed a minification warning-as-failure thrown by the non-inclusion of the GAID library.
    - Removal of
      - `enableFacebookAppLinkCheck`
      - `getCrossPlatformIds`
      - `userCompletedAction`
      - `sendCommerceEvent`
- v5.6.4
  * _*Master Release*_ - Aug 11, 2023
    - Bug fix: Added proguard policy to not break minification builds because of newly added compile only classes.
- v5.6.3
  * _*Master Release*_ - Aug 9, 2023
    - Updated Samsung Galaxy Store install referrer, to enable import `implementation("store.galaxy.samsung.installreferrer:samsung_galaxystore_install_referrer:4.0.0")`
    - Xiaomi referrer also available from Maven Central, import `implementation("com.miui.referrer:homereferrer:1.0.0.7")`
    - Removed old no op `enablePlayStoreReferrer`
    - Bug fixes: 
        - Install referrer tasks are now done in parallel
        - Non-critical JSON exception is now logged as a warning
        - When sharing a link by share sheet, the `userCompletedAction` event is replaced by v2/event
- v5.6.2
  * _*Master Release*_ - Jul 31, 2023
    - Removing
      - Credits end points
      - Jetifier flag
      - Firebase App Indexing
      - Long deprecated public functions
        setDebug
        enableDebugMode
        disableDebugMode
        enableSimulateInstalls
        disableSimulateInstalls
        ShareLinkBuilder (use BranchShareSheetBuilder instead)
    - Short links can now be made while tracking is disabled.
    - Google Play Billing Library is now optional. To enable, import com.android.billingclient:billing

- v5.6.1
  * _*Master Release*_ - Jun 30, 2023
    - Revert the Kotlin version update from 1.8.22 back to 1.6.20.

- v5.6.0
  * _*Master Release*_ - Jun 28, 2023
    - Added new method for logging a Branch Event with a callback, `logEvent(Context context, final BranchLogEventCallback callback)`
    - Added proguard consumer rules to prevent R8 warning on Gradle 8 builds
    
- v5.5.0
  * _*Master Release*_ - Jun 14, 2023
    - Added support for easily logging Play Store transactions as Branch Events with `logEventWithPurchase`.
    - Updated handling of URL query parameters.
    - Updated URI skiplist.

- v5.4.0
  * _*Master Release*_ - Mar 10, 2023
    - Added support for Snap partner parameters with `addSnapPartnerParameter`
    - For more info see: https://help.branch.io/using-branch/docs/snap-advanced-conversions#enabling-snap-advanced-conversions

- v5.3.0
  * _*Master Release*_ - Mar 3, 2023
  * New feature for plugin SDK developers to defer native Branch Android SDK until plugin notifies when ready 
    - To enable, enter this key value pair in your `branch.json` file inside of your `/src/main/assets/`
    - `"deferInitForPluginRuntime": true`
    - This feature is opt in only for now.
    - Requires plugin SDK to implement notification to native modules.
  * Fixes `enableLogging` feature if set in `branch.json`
    - `"enableLogging": true`

- v5.2.7
  * _*Master Release*_ - Dec 7, 2022
  * Fixes a bug with setIdentity not consistently calling v1/profile
  * Adds switch to prioritize referrer links for attribution over preinstall metadata
    - To enable, call `setReferringLinkAttributionForPreinstalledAppsEnabled` before `Branch.getAutoInstance(this)`

- v5.2.6
  * _*Master Release*_ - Oct 25, 2022
  * `debug` field is now included in all events, previously only on init requests
  * `source` field moved from `data` block to top level in v1 url requests
    
- v5.2.5
  * _*Master Release*_ - Oct 3, 2022
  * For user agent string, move webview instance to ui thread on init
    * To use this method to obtain the user agent string, set `Branch.setIsUserAgentSync(true)`
    * Static WebSettings is the default

- v5.2.4
  * _*Master Release*_ - Sep 30, 2022
  * Using alternative method to obtain user agent through WebView instance with static as fallback 

- v5.2.3
  * _*Master Release*_ - Aug 29, 2022
  * Updated compile and target API to 32
  * Changed POM scope for firebase-appindexing from "runtime" to "compile"

- v5.2.2
  * _*Master Release*_ - Aug 15, 2022
  * Sending app_store install source on requests
  * Added default retry cap for no-internet request queues 
  
- v5.2.1
  * _*Master Release*_ - Aug 8, 2022
  * Removed unused IMEI strings and references in the Java
  * Change setIdentity call to immediately save provided identity string instead of waiting for network call. Additionally it is sent as part of v1 open and install requests.

- v5.2.0
  * _*Master Release*_ - Jun 15, 2022
  * Added methods to generate Branch QR codes.
      New methods:
        - getQRCodeAsData
        - getQRCodeAsImage()
      Utility methods:
        - setCodeColor()
        - setBackgroundColor()
        - setCenterLogo()
        - setWidth()
        - setMargin()
        - setImageFormat()
  * Added support for additional store referrer APIs. No code needed other than to import the store's referrer API artifact into your app. 
      New app stores:
        - Huawei App Gallery
        - Samsung Galaxy Store*
        - Xiaomi GetApps*
        * Contact your Samsung or Xiaomi representative for assistance with obtaining their store referrer API artifact.
  * Upgraded Google Install Referrer API
  * Updated Android minimum SDK to 21
  * Replaced JCenter with mavenCentral in `allprojects.repositories` 

- v5.1.5
  * _*Master Release*_ - Jun 1, 2022
  * Fixes an issue with previously set randomly generated id values being skipped over in favor of newer values. Now old values, if they exist, are sent up without.    

- ⚠️ v5.1.4  
Warning: This release has a configuration issue that incorrectly marks opens as reinstalls. Please use 5.1.5.
  * _*Master Release*_ - Apr 29, 2022
  * Fix retry logic for network requests when there is no internet availability.
  * Replace "identity_id" with "randomized_bundle_token" and "device_fingerprint_id" with "randomized_device_token"

- v5.1.3
  * _*Master Release*_ - Mar 25, 2022
  * Collect and persist GCLID
    * A custom expiration window for the GCLID can be set by `public void setReferrerGclidValidForWindow(long window)`
  * Credits related methods and fields marked as deprecated
  * Fixed IllegalAccessException on API 31, using DisplayManager instead of WindowManager

- v5.1.2
  * _*Master Release*_ - Mar 9, 2022
  * Update payloads to include generated UUID when valid GAID is retrieved, replacing hardware ID. UUID is persisted locally.
  * Fix bug which spammed the log for no-op event retry attempts when tracking is disabled.

- v5.1.1
  * _*Master Release*_ - Feb 18, 2022
  * Fix NPE if intent is null while getting referrer

- v5.1.0
  * _*Master Release*_ - Feb 9, 2022
  * Added BranchPluginSupport class with deviceDescription() method. Will be used by the AdobeBranchExtension to pass device data.
  * Resolves timeout and non-initialization issues introducing a connect timeout, and retrying init requests.
    * Possible behavior change is with the increased timeout, any operations awaiting these requests may see longer timeouts.
    * To set the timeouts:
      * `public void setConnectTimeout(int connectTimeout)`
      * `public void setTimeout(int timeout)`
      * Time is in milliseconds

- v5.0.15
  * _*Master Release*_ - Nov 10, 2021
  * Bug fixes: IntegrationValidator (ConcurrentModificationException, Decoding Resource Strings)

- v5.0.14
  * _*Master Release*_ - Oct 15, 2021
  * Bug fixes: improper shutdown of resource, multiline server responses
  * Fix crash when using Branch JSON config

- v5.0.13
  * _*Master Release*_ - Oct 1, 2021
  * Always include Google Play Store referrer in install 
  * IntegrationValidator now recoginizes String resources
  * Fix edge case that could cause a stack overflow

- v5.0.12
  * _*Master Release*_ - Sep 21, 2021
  * Avoid NPE when Branch reference in BranchPostTask is garbage collected

- v5.0.11
  * _*Master Release*_ - Aug 23, 2021
  * Avoid NPE when Activity reference is null

- v5.0.10
  * _*Master Release*_ - Aug 4, 2021
  * Collect initial_referrer
  
- v5.0.9
  * _*Master Release*_ - April 29, 2021
  * Avoid potential NPE
  * When reinitializing session without an expected intent flag, invoke callback with error rather than ignoring it.

- v5.0.8
  * _*Master Release*_ - April 29, 2021
  * Fix bug can block UI thread
  * Fix race condition between session initialization requests invoked by client and SDK
  
- v5.0.7
  * _*Master Release*_ - March 1, 2021
  * Patch: fix getSdkVersionNumber() API in v2 events as well
  
- v5.0.6
  * _*Master Release*_ - February 26, 2021
  * Add INITIATE_STREAM and COMPLETE_STREAM standard events
  * Fix getSdkVersionNumber() API
  
- v5.0.5
  * _*Master Release*_ - February 17, 2021
  * Add API to pass hashed partner parameters (Facebook integration)
  * Fix bugs in the networking pipeline of CPID and LATD requests
  * Fix bug preventing callback invocation when url creation request times out
  * Add support for static SKD configuration via branch.json file
  * Deprecate SDK initializers with isReferrable flag
  
- v5.0.4
  * _*Master Release*_ - December 9, 2020
  * Reduce calls to v1/close
  * Fix validator errors
  * Fix setNetworkTimeout(...) API

- v5.0.3
  * _*Master Release*_ - August 10, 2020
  * Upgrade referrer library
  
- v5.0.2
  * _*Master Release*_ - July 22, 2020
  * Remove content discovery

- v5.0.1
  * _*Master Release*_ - March 8, 2020
  * Report GAID when test key is used
  * Detect TUNE migrations, report them via the `update` field in open/install payload
  * Deprecate enableSimulateInstalls
  * Deprecate enableDebugMode
  * Modify enableTestMode
  * Support for air-preload campaigns via Play Install Referrer API

- v5.0.0
  * _*Master Release*_ - March 17, 2020
  * Bump up major version to signify switch to session builder from initSession (old functionality is maintained)
  * Added documentation with 1:1 session builder replacements of deprecated initSession methods
  * Add disableAdNetworkCallouts(boolean) API

- v4.4.0
  * _*Master Release*_ - March 12, 2020
  * Do not check Facebook app links unless feature is explicitly enabled
  * Introduce Branch.sessionBuilder().<...>.init() to replace all overloaded variations of initSession
  * Introduce Branch.expectDelayedSessionInitialization(boolean) which disables session self-initialization in RESUMED state, users MUST initialize themselves when using this method
  * Introduce Branch.sessionBuilder().withDelay(X_MILLIS) to facilitate delaying session initialization until user calls branchInstance.removeSessionInitializationDelay() or X_MILLIS pass.
  * Prevent duplicated session initialization in certain intra-app linking scenarios
  * Collect OAID from HMS when available
  * Prevent potential NPE in the CPID API
  * Improvements in instant deep-linking

- v4.3.2
  * _*Master Release*_ - January 29, 2020
  * Accommodate single activity architecture/navigation component (reintroduced "SDK already initialized" error and ensured reInitSession() only fires when intent contains branch data).
  * Do not self-initialize if SDK is bundled with a plugin.
  
- v4.3.1
  * _*Master Release*_ - January 23, 2020
  * Hotfix revert CPID and LATD listener paths to the newer version.
  * Replace "SDK already initialized" error with logs and return latest referring parameters.

- v4.3.0
  * _*Master Release*_ - January 22, 2020
  * Drop broadcast receiver and bundle Play Install Referrer Library.
  * Fix CPID and LATD listener paths.
  * Annotate @Nullable initSession callback parameters, so callback is not dropped in kotlin.
  
- v4.2.1
  * _*Master Release*_ - December 19, 2019
  * Make attribution window optional for LATD requests
  * Remove fake email stub from share sheet on Android TVs
  
- v4.2.0
  * _*Master Release*_ - November 19, 2019
  * Remove initialization race conditions.
  * Only self-initialize sessions if user has not done so and some Activity is entering the RESUMED state.
  * Start returning an error on consecutive session initializations. Note, this means users who require deeplinking functionality now must call initSession() from LauncherActivity.onStart() (the requirement was already implied in the documentation, though was not enforced, the SDK was returning latestReferringParams instead, which users in need of a workaround can still do).
  * Overload reInitSession with different callbacks and start advertising it to users as the official way to handle session reinitialization in cases where activity is in foreground and is being reopened.
  * Make sure carrier field does not contain an empty string, omit the field instead.
  
- v4.1.2
  * _*Master Release*_ - October 30, 2019
  * Enabled setting custom CDN URL
  * Fixed button clickability on devices without touchscreen
  * Added extra fields to open/install and event endpoints

- v4.1.1
  * _*Master Release*_ - October 7, 2019
  * Support for Fire OS
  * Do not collect advertising ID when limit ad tracking is enabled after the app was initialized

- v4.1.0
  * _*Master Release*_ - September 26, 2019
  * Support for CPID
  * Support for Last Attributed Touch Data

- v4.0.1
  * _*Master Release*_ - September 18, 2019
  * SDK-237 corrected customer_event_alias function
  * SDK-211 support for optional plugin

- v4.0.0
  * _*Master Release*_ - August 6, 2019
  * Switched to using Android X from the Android Support Library
  * Added new standard event type: customer_event_alias
  * Implemented support for pre-install analytics
  * Added the option to set a custom base URL

- v3.2.0
  * _*Master Release*_ - May 2, 2019
  * SDK-271 Allow short link creation while privacy is enabled

- v3.1.2
  * _*Master Release*_ - April 16, 2019
  * Hardware ID is now included in every request
  * Cleaned up compiler warnings, and updated tools to the latest versions

- v3.1.1
  * Added support for push notifications while the application is in the foreground

- v3.1.0
  * Fixed a synchronization issue around the event queue saving preferences while in a synchronized block.
  * Added new standard events for parity with Tune.
  * Ensure that Google Aid is present in all requests.
  * Refactored how Debug works, including making sure all Debug messages can be turned off if not in debug mode.

- v3.0.4
  * Fixed a TLS1.2 issue with HttpsURLConnection on API Level 16~19 devices
  * Add SDK version tag to the Android SDK to aid Google Scanning APIs
  * The SDK now supports deeplinking with enableForcedSession() for apps which choose to finish the Launcher Activity within onStart() method

- v3.0.3
  * _*Master Release*_ - December 6, 2018
  * Fixed Android InstallListener exception when not on UI thread. SDK-87

- v3.0.2
  * _*Master Release*_ - November 30, 2018
  * Fix DeadSystemException crash in System Observer. INTENG-4460

- v3.0.1
  * _*Master Release*_ - November 8, 2018
  * Fix unstable share sheet row height. DEVEX-835
  * Added new method to force init session. INTENG-4322
  * Support untagged sockets. DEVEX-888

- v3.0.0
  * _*Master Release*_ - October 26, 2018
  * Upgrade to api2.branch.io for TLS 1.2+ support. DEVEX-809
  * _Breaking Change_ Min SDK version is now 16. If you want to support minimum sdk level 9 please consider using version 1.14.5. If you want to to support a minimum SDK of 15, use the 2.x versions.

- v2.19.5
  * _*Master Release*_ - October 8, 2018
  * Added new way to init session for attribution (`initSessionForced`) INTENG-4285

- v2.19.4
  * _*Master Release*_ - October 1, 2018
  * Adding Branch Universal Object to custom event request. DEVEX-761
  * Send GAID with v1/event. DEVEX-766
  * Adding ability to do instant deep linking with App Links. DEVEX-776

- v2.19.3
  * _*Master Release*_ - August 22, 2018
  * Changed post request logic to close streams. [#600](https://github.com/BranchMetrics/android-branch-deep-linking/issues/600)
  * Added check for if `BranchEvent` is same name as `BRANCH_STANDARD_EVENT` name, send with `v2/event/standard` request. DEVEX-751

- v2.19.2
  * _*Master Release*_ - August 10, 2018
  * Added notify network API to process requests when network is available. DEVEX-711

- v2.19.1 Fixed GAID fetch issue.

- v2.19.0 Support for adding custom install metadata. Fix for an ANR in debug mode. Fix for crash caused by reading user agent. SDK Integration validator.

- v2.18.1 Fixing issue with facebook app link check caused by incorrect value for facebook_app_link_checked state. Fix for app indexing to run on separate thread.

- v2.18.0 Removing unnecessary String conversion for deeplink data JSONObject. This will fix the issue of additional escape characters present in the deep link data

- v2.17.1 Hot fix : Preference items cleared over app re-open if Branch key is missing in manifest file.

- v2.17.0 Adding tracking disable feature, this is useful for GDPR compliance. Fixing share sheet to show correct selection. Fix for updating device params to Branch requests before sending. Updating to latest Gradle version. Few other minor fixes.

- v2.16.0 Adding support for collecting data from selected URIs with remote skip list update feature. Fixing an issue with handling BUO metadata.

- v2.15.1 Hot fix : Install requests getting stuck in the request queue if failed once. Caused by improper install referrer wait lock addition to install request.

- v2.15.0 Adding support for Android install referrer lib. Changing instant deep linking into opt-in feature. Collecting install timestamps for better install or update attributions.

- v2.14.5 Hot fix: Re-open after offline install event fails. Support for modifying deep link data and link params on share sheet events.

- v2.14.4 Fixing a possible request queue concurrent execution. Adding support for opt-out IDL. Fix for maintaining strong typing for arrays when BUO is serialised. Adding extended catch for dealing with dead object. Support for "rating" property in content metadata.

- v2.14.3 Fixing a corner case crash from concurrent modification case. Fixing issue with deep link param delivery after orientation change.

- v2.14.2 Adding FB limit app tracking. Fix for instant deep liking when activities are launched from the stack. Few other minor fixes.

- v2.14.1 Ensure backward compatibility in case deprecated BUO methods are used.

- v2.14.0 Adding support for Branch reserved events. Support to update delayed request metadata. Skipping instant deep linking on forcing new session.

- v2.13.1 Hot fix : Referral params are not returned when initSession is called mutiple times while an init session is in progress

- v2.13.0 Adding instant deeplinking support

- v2.12.2 Fixing play store referrer capture issue on Android 6. Changes to collect UI_Mode to identity different platforms.  Fix for stale intents when activities are launched from history.

- v2.12.1 Fixing discrepancy in Branch driven app open count in Answers' dashboard. Adding bounds to share sheet and option to set bounds.

- v2.12.0 Adding support for Firebase based app indexing and local content indexing. Few proactive protections for parcel errors caused by malformed parcels in the intent. Url encoding for long link params.

- v2.11.1 Removing app listing related implementations.

- v2.11.0 Disabling external app listing is by default.

- v2.10.3 Fix for reading intent from launcher activity only unless forced to restart session. Instant app utility methods fix for Android `O`

- v2.10.2 More reliable install referrer capture. URL encoding for supporting referring links added in play store referrer

- v2.10.1 Deprecating enablePlayStoreReferrer()

- v2.10.0 Added changes to capture raw play store referrer string when Google search install referrer is present

- v2.9.1 Hot fix for a possible app crash on instant app to full app conversion

- v2.9.0 Adding network layer abstraction to build custom network layer. Apps can build their own network implementation to handle Branch network requests. By default Branch will be using URL connection for handling network requests.

- v2.8.0 Adding Instant App support. Supports Instant App content deep linking, Full app conversion with deep linking and associated conversion attributions.

- v2.7.0 Moved `BranchLinkUsed` to intent extra. Fixed ability to simulate is_first_session:true when testing. CD Revamp.

- v2.6.1 Adding environment variables. Enhancement to CD.

- v2.6.0 Fix for NPE edge case. Added configurable delay between strong match url- init. Added support for Google Install Referrer ID.

- v2.5.9 Fix for strong match NPE.

- v2.5.8 Fix for getAppVersion, added matching via PlayStore install broadcast intent, modifications to strong match url.

- v2.5.7 Fix for ShareSheet Airplane mode, updated commerceevent defaults, Added feature for sharesheet to whitelist/blacklist apps by package name. Added synchronous
  getLatestReferringParams and getFirstReferringParams.

- v2.5.6 Fix for sharesheet title, extra intent data fix, added commerceevent function.

- v2.5.5 Added function to init Branch w/ key programmatically (vs having to use Manifest file).

- v2.5.4 Fix to better register opens/installs. Added enableTestMode/disableTestMode static functions.

- v2.5.3 Fix to disable FB app link check by default.

- v2.5.2 Fix for Android/iOS link-click compatibility when used with BUO. Fixed doc formatting for latest Android Studio version.

- v2.5.1 Fix for init session not being called when used from onCreate().

- v2.5.0 Changing chrome tabs dependency optional. Adding option to simulate and test install. Few corner error  handling

- v2.4.7 Fix for cold start issue with deeplink parameters on slower internet connection. Added local ip address as part of device params

- v2.4.6 Fixed issue with receiving deep link prams through push notification while app is running in foreground. Added device locale info for stronger matching.

- v2.4.5 Adding fix for transaction too large exception while auto deep linking. Fixing invalid argument exception while reading params from intent.

- v2.4.4 Adding ability to skip collecting external intent data specific to URI host. Adding NPE protection for custom tab session access

- v2.4.3 Adding ability to skip collecting external intent data specific to URI paths

- v2.4.2 Removes the unnecessary support package dependencies

- v2.4.1 Updating minimum SDK version to 15.

- v2.4.0 Adding Cookie based matching support. Fixing few format issues and updating documentation.

- v2.3.1 Correcting BUO Action name View to VIEW

- v2.3.0 Enhancements to Branch universal object. Support for campaign in link properties

- v2.2.0 Adding Branch Content Discovery feature.

- v2.1.0 Replacing answers-shim dependency with Crashlytics answers shim to fix build conflict when Branch is used along with Digits.

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
