package io.branch.referral;

import static io.branch.referral.BranchError.ERR_BRANCH_TASK_TIMEOUT;
import static io.branch.referral.BranchPreinstall.getPreinstallSystemData;
import static io.branch.referral.BranchUtil.isTestModeEnabled;
import static io.branch.referral.PrefHelper.isValidBranchKey;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import io.branch.referral.Defines.PreinstallKey;
import io.branch.referral.ServerRequestGetLATD.BranchLastAttributedTouchDataListener;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.branch.indexing.BranchUniversalObject;
import io.branch.referral.network.BranchRemoteInterface;
import io.branch.referral.network.BranchRemoteInterfaceUrlConnection;
import io.branch.referral.util.BRANCH_STANDARD_EVENT;
import io.branch.referral.util.BranchEvent;
import io.branch.referral.util.CommerceEvent;
import io.branch.referral.util.LinkProperties;

/**
 * <p>
 * The core object required when using Branch SDK. You should declare an object of this type at
 * the class-level of each Activity or Fragment that you wish to use Branch functionality within.
 * </p>
 * <p>
 * Normal instantiation of this object would look like this:
 * </p>
 * <!--
 * <pre style="background:#fff;padding:10px;border:2px solid silver;">
 * Branch.getInstance(this.getApplicationContext()) // from an Activity
 * Branch.getInstance(getActivity().getApplicationContext())    // from a Fragment
 * </pre>
 * -->
 */
public class Branch implements BranchViewHandler.IBranchViewEvents, SystemObserver.AdsParamsFetchEvents, StoreReferrerGooglePlayStore.IGoogleInstallReferrerEvents, StoreReferrerHuaweiAppGallery.IHuaweiInstallReferrerEvents, StoreReferrerSamsungGalaxyStore.ISamsungInstallReferrerEvents, StoreReferrerXiaomiGetApps.IXiaomiInstallReferrerEvents {

    private static final String BRANCH_LIBRARY_VERSION = "io.branch.sdk.android:library:" + Branch.getSdkVersionNumber();
    private static final String GOOGLE_VERSION_TAG = "!SDK-VERSION-STRING!" + ":" + BRANCH_LIBRARY_VERSION;

    /**
     * Hard-coded {@link String} that denotes a {@link BranchLinkData#tags}; applies to links that
     * are shared with others directly as a user action, via social media for instance.
     */
    public static final String FEATURE_TAG_SHARE = "share";
    
    /**
     * The redirect URL provided when the link is handled by a desktop client.
     */
    public static final String REDIRECT_DESKTOP_URL = "$desktop_url";
    
    /**
     * The redirect URL provided when the link is handled by an Android device.
     */
    public static final String REDIRECT_ANDROID_URL = "$android_url";
    
    /**
     * The redirect URL provided when the link is handled by an iOS device.
     */
    public static final String REDIRECT_IOS_URL = "$ios_url";
    
    /**
     * The redirect URL provided when the link is handled by a large form-factor iOS device such as
     * an iPad.
     */
    public static final String REDIRECT_IPAD_URL = "$ipad_url";
    
    /**
     * The redirect URL provided when the link is handled by an Amazon Fire device.
     */
    public static final String REDIRECT_FIRE_URL = "$fire_url";

    /**
     * Open Graph: The title of your object as it should appear within the graph, e.g., "The Rock".
     *
     * @see <a href="http://ogp.me/#metadata">Open Graph - Basic Metadata</a>
     */
    public static final String OG_TITLE = "$og_title";
    
    /**
     * The description of the object to appear in social media feeds that use
     * Facebook's Open Graph specification.
     *
     * @see <a href="http://ogp.me/#metadata">Open Graph - Basic Metadata</a>
     */
    public static final String OG_DESC = "$og_description";
    
    /**
     * An image URL which should represent your object to appear in social media feeds that use
     * Facebook's Open Graph specification.
     *
     * @see <a href="http://ogp.me/#metadata">Open Graph - Basic Metadata</a>
     */
    public static final String OG_IMAGE_URL = "$og_image_url";
    
    /**
     * A URL to a video file that complements this object.
     *
     * @see <a href="http://ogp.me/#metadata">Open Graph - Basic Metadata</a>
     */
    public static final String OG_VIDEO = "$og_video";
    
    /**
     * The canonical URL of your object that will be used as its permanent ID in the graph.
     *
     * @see <a href="http://ogp.me/#metadata">Open Graph - Basic Metadata</a>
     */
    public static final String OG_URL = "$og_url";
    
    /**
     * Unique identifier for the app in use.
     */
    public static final String OG_APP_ID = "$og_app_id";
    
    /**
     * {@link String} value denoting the deep link path to override Branch's default one. By
     * default, Branch will use yourapp://open?link_click_id=12345. If you specify this key/value,
     * Branch will use yourapp://'$deeplink_path'?link_click_id=12345
     */
    public static final String DEEPLINK_PATH = "$deeplink_path";
    
    /**
     * {@link String} value indicating whether the link should always initiate a deep link action.
     * By default, unless overridden on the dashboard, Branch will only open the app if they are
     * 100% sure the app is installed. This setting will cause the link to always open the app.
     * Possible values are "true" or "false"
     */
    public static final String ALWAYS_DEEPLINK = "$always_deeplink";
    
    /**
     * An {@link Integer} value indicating the link type. In this case, the link can be used an
     * unlimited number of times.
     */
    public static final int LINK_TYPE_UNLIMITED_USE = 0;
    
    /**
     * An {@link Integer} value indicating the link type. In this case, the link can be used only
     * once. After initial use, subsequent attempts will not validate.
     */
    public static final int LINK_TYPE_ONE_TIME_USE = 1;

    /**
     * If true, instantiate a new webview instance ui thread to retrieve user agent string
     */
    static boolean userAgentSync;

    /**
     * Package private user agent string cached to save on repeated queries
     */
    static String _userAgentString = "";

    /* Json object containing key-value pairs for debugging deep linking */
    private JSONObject deeplinkDebugParams_;
    
    private static boolean disableDeviceIDFetch_;
    
    private boolean enableFacebookAppLinkCheck_ = false;
    
    static boolean bypassWaitingForIntent_ = false;
    
    private static boolean bypassCurrentActivityIntentState_ = false;

    static boolean disableAutoSessionInitialization;

    static boolean checkInstallReferrer_ = true;
    private static long playStoreReferrerWaitTime = 1500;
    public static final long NO_PLAY_STORE_REFERRER_WAIT = 0;

    static boolean referringLinkAttributionForPreinstalledAppsEnabled = false;
    
    /**
     * <p>A {@link Branch} object that is instantiated on init and holds the singleton instance of
     * the class during application runtime.</p>
     */
    public static Branch branchReferral_;

    private BranchRemoteInterface branchRemoteInterface_;
    final PrefHelper prefHelper_;
    private final DeviceInfo deviceInfo_;
    private final BranchPluginSupport branchPluginSupport_;
    private final Context context_;

    private final BranchQRCodeCache branchQRCodeCache_;

    private final Semaphore serverSema_ = new Semaphore(1);

    final ServerRequestQueue requestQueue_;
    
    int networkCount_ = 0;

    final ConcurrentHashMap<BranchLinkData, String> linkCache_ = new ConcurrentHashMap<>();

    /* Set to true when {@link Activity} life cycle callbacks are registered. */
    private static boolean isActivityLifeCycleCallbackRegistered_ = false;


    /* Enumeration for defining session initialisation state. */
    enum SESSION_STATE {
        INITIALISED, INITIALISING, UNINITIALISED
    }
    
    
    enum INTENT_STATE {
        PENDING,
        READY
    }

    /* Holds the current intent state. Default is set to PENDING. */
    private INTENT_STATE intentState_ = INTENT_STATE.PENDING;
    
    /* Holds the current Session state. Default is set to UNINITIALISED. */
    SESSION_STATE initState_ = SESSION_STATE.UNINITIALISED;

    /* */
    static boolean deferInitForPluginRuntime = false;

    /* Flag to indicate if the `v1/close` is expected by the server at the end of this session. */
    public boolean closeRequestNeeded = false;

    /* Instance  of share link manager to share links automatically with third party applications. */
    private ShareLinkManager shareLinkManager_;
    
    /* The current activity instance for the application.*/
    WeakReference<Activity> currentActivityReference_;
    
    /* Key for Auto Deep link param. The activities which need to automatically deep linked should define in this in the activity metadata. */
    private static final String AUTO_DEEP_LINK_KEY = "io.branch.sdk.auto_link_keys";
    
    /* Path for $deeplink_path or $android_deeplink_path to auto deep link. The activities which need to automatically deep linked should define in this in the activity metadata. */
    private static final String AUTO_DEEP_LINK_PATH = "io.branch.sdk.auto_link_path";
    
    /* Key for disabling auto deep link feature. Setting this to true in manifest will disable auto deep linking feature. */
    private static final String AUTO_DEEP_LINK_DISABLE = "io.branch.sdk.auto_link_disable";
    
    /*Key for defining a request code for an activity. should be added as a metadata for an activity. This is used as a request code for launching a an activity on auto deep link. */
    private static final String AUTO_DEEP_LINK_REQ_CODE = "io.branch.sdk.auto_link_request_code";
    
    /* Request code  used to launch and activity on auto deep linking unless DEF_AUTO_DEEP_LINK_REQ_CODE is not specified for teh activity in manifest.*/
    private static final int DEF_AUTO_DEEP_LINK_REQ_CODE = 1501;
    
    final ConcurrentHashMap<String, String> instrumentationExtraData_ = new ConcurrentHashMap<>();

    /* In order to get Google's advertising ID an AsyncTask is needed, however Fire OS does not require AsyncTask, so isGAParamsFetchInProgress_ would remain false */
    private boolean isGAParamsFetchInProgress_ = false;

    private static String cookieBasedMatchDomain_ = "app.link"; // Domain name used for cookie based matching.
    
    private static final int LATCH_WAIT_UNTIL = 2500; //used for getLatestReferringParamsSync and getFirstReferringParamsSync, fail after this many milliseconds
    
    /* List of keys whose values are collected from the Intent Extra.*/
    private static final String[] EXTERNAL_INTENT_EXTRA_KEY_WHITE_LIST = new String[]{
            "extra_launch_uri",   // Key for embedded uri in FB ads triggered intents
            "branch_intent"       // A boolean that specifies if this intent is originated by Branch
    };

    public static String installDeveloperId = null;

    CountDownLatch getFirstReferringParamsLatch = null;
    CountDownLatch getLatestReferringParamsLatch = null;

    private boolean waitingForHuaweiInstallReferrer = false;
    private boolean waitingForGoogleInstallReferrer = false;
    private boolean waitingForSamsungInstallReferrer = false;
    private boolean waitingForXiaomiInstallReferrer = false;

    /* Flag for checking of Strong matching is waiting on GAID fetch */
    private boolean performCookieBasedStrongMatchingOnGAIDAvailable = false;
    boolean isInstantDeepLinkPossible = false;
    private BranchActivityLifecycleObserver activityLifeCycleObserver;
    /* Flag to turn on or off instant deeplinking feature. IDL is disabled by default */
    private static boolean enableInstantDeepLinking = false;
    private final TrackingController trackingController;

    /** Variables for reporting plugin type and version (some TUNE customers do that), plus helps
     * us make data driven decisions. */
    private static String pluginVersion = null;
    private static String pluginName = null;

    private BranchReferralInitListener deferredCallback;
    private Uri deferredUri;
    InitSessionBuilder deferredSessionBuilder;

    /**
     * <p>The main constructor of the Branch class is private because the class uses the Singleton
     * pattern.</p>
     * <p>Use {@link #getAutoInstance(Context)} method when instantiating.</p>
     *
     * @param context A {@link Context} from which this call was made.
     */
    private Branch(@NonNull Context context) {
        context_ = context;
        prefHelper_ = PrefHelper.getInstance(context);
        trackingController = new TrackingController(context);
        branchRemoteInterface_ = new BranchRemoteInterfaceUrlConnection(this);
        deviceInfo_ = new DeviceInfo(context);
        branchPluginSupport_ = new BranchPluginSupport(context);
        branchQRCodeCache_ = new BranchQRCodeCache(context);
        requestQueue_ = ServerRequestQueue.getInstance(context);
        if (!trackingController.isTrackingDisabled()) { // Do not get GAID when tracking is disabled
            isGAParamsFetchInProgress_ = deviceInfo_.getSystemObserver().prefetchAdsParams(context,this);
        }
    }

    /**
     * <p>Singleton method to return the pre-initialised object of the type {@link Branch}.
     * Make sure your app is instantiating {@link BranchApp} before calling this method
     * or you have created an instance of Branch already by calling getInstance(Context ctx).</p>
     *
     * @return An initialised singleton {@link Branch} object
     */
    synchronized public static Branch getInstance() {
        if (branchReferral_ == null) {
            PrefHelper.Debug("Branch instance is not created yet. Make sure you call getAutoInstance(Context).");
        }
        return branchReferral_;
    }

    synchronized private static Branch initBranchSDK(@NonNull Context context, String branchKey) {
        if (branchReferral_ != null) {
            PrefHelper.Debug("Warning, attempted to reinitialize Branch SDK singleton!");
            return branchReferral_;
        }
        branchReferral_ = new Branch(context.getApplicationContext());

        if (TextUtils.isEmpty(branchKey)) {
            PrefHelper.Debug("Warning: Please enter your branch_key in your project's Manifest file!");
            branchReferral_.prefHelper_.setBranchKey(PrefHelper.NO_STRING_VALUE);
        } else {
            branchReferral_.prefHelper_.setBranchKey(branchKey);
        }

        /* If {@link Application} is instantiated register for activity life cycle events. */
        if (context instanceof Application) {
            branchReferral_.setActivityLifeCycleObserver((Application) context);
        }

        // Cache the user agent from a webview instance if needed
        if(userAgentSync && DeviceInfo.getInstance() != null){
            DeviceInfo.getInstance().getUserAgentStringSync(context);
        }

        return branchReferral_;
    }

    /**
     * <p>Singleton method to return the pre-initialised, or newly initialise and return, a singleton
     * object of the type {@link Branch}.</p>
     * <p>Use this whenever you need to call a method directly on the {@link Branch} object.</p>
     *
     * @param context A {@link Context} from which this call was made.
     * @return An initialised {@link Branch} object, either fetched from a pre-initialised
     * instance within the singleton class, or a newly instantiated object where
     * one was not already requested during the current app lifecycle.
     */
    synchronized public static Branch getAutoInstance(@NonNull Context context) {
        if (branchReferral_ == null) {
            if(BranchUtil.getEnableLoggingConfig(context)){
                enableLogging();
            }

            // Should only be set in json config
            deferInitForPluginRuntime(BranchUtil.getDeferInitForPluginRuntimeConfig(context));

            BranchUtil.setTestMode(BranchUtil.checkTestMode(context));
            branchReferral_ = initBranchSDK(context, BranchUtil.readBranchKey(context));
            getPreinstallSystemData(branchReferral_, context);
        }
        return branchReferral_;
    }

    /**
     * <p>Singleton method to return the pre-initialised, or newly initialise and return, a singleton
     * object of the type {@link Branch}.</p>
     * <p>Use this whenever you need to call a method directly on the {@link Branch} object.</p>
     *
     * @param context   A {@link Context} from which this call was made.
     * @param branchKey A {@link String} value used to initialize Branch.
     * @return An initialised {@link Branch} object, either fetched from a pre-initialised
     * instance within the singleton class, or a newly instantiated object where
     * one was not already requested during the current app lifecycle.
     */
    public static Branch getAutoInstance(@NonNull Context context, @NonNull String branchKey) {
        if (branchReferral_ == null) {
            if(BranchUtil.getEnableLoggingConfig(context)){
                enableLogging();
            }

            // Should only be set in json config
            deferInitForPluginRuntime(BranchUtil.getDeferInitForPluginRuntimeConfig(context));

            BranchUtil.setTestMode(BranchUtil.checkTestMode(context));
            // If a Branch key is passed already use it. Else read the key
            if (!isValidBranchKey(branchKey)) {
                PrefHelper.Debug("Warning, Invalid branch key passed! Branch key will be read from manifest instead!");
                branchKey = BranchUtil.readBranchKey(context);
            }
            branchReferral_ = initBranchSDK(context, branchKey);
            getPreinstallSystemData(branchReferral_, context);
        }
        return branchReferral_;
    }

    public Context getApplicationContext() {
        return context_;
    }

    /**
     * Sets a custom Branch Remote interface for handling RESTful requests. Call this for implementing a custom network layer for handling communication between
     * Branch SDK and remote Branch server
     *
     * @param remoteInterface A instance of class extending {@link BranchRemoteInterface} with
     *                        implementation for abstract RESTful GET or POST methods, if null
     *                        is passed, the SDK will use its default.
     */
    public void setBranchRemoteInterface(BranchRemoteInterface remoteInterface) {
        if (remoteInterface == null) {
            branchRemoteInterface_ = new BranchRemoteInterfaceUrlConnection(this);
        } else {
            branchRemoteInterface_ = remoteInterface;
        }
    }

    public BranchRemoteInterface getBranchRemoteInterface() {
        return branchRemoteInterface_;
    }
    
    /**
     * <p>
     * Enables the test mode for the SDK. This will use the Branch Test Keys. This is same as setting
     * "io.branch.sdk.TestMode" to "True" in Manifest file.
     *
     * Note: As of v5.0.1, enableTestMode has been changed. It now uses the test key but will not log or randomize
     * the device IDs. If you wish to enable logging, please invoke enableLogging. If you wish to simulate
     * installs, please see add a Test Device (https://help.branch.io/using-branch/docs/adding-test-devices)
     * then reset your test device's data (https://help.branch.io/using-branch/docs/adding-test-devices#section-resetting-your-test-device-data).
     * </p>
     */
    public static void enableTestMode() {
        BranchUtil.setTestMode(true);
        PrefHelper.LogAlways("enableTestMode has been changed. It now uses the test key but will not" +
                " log or randomize the device IDs. If you wish to enable logging, please invoke enableLogging." +
                " If you wish to simulate installs, please see add a Test Device (https://help.branch.io/using-branch/docs/adding-test-devices)" +
                " then reset your test device's data (https://help.branch.io/using-branch/docs/adding-test-devices#section-resetting-your-test-device-data).");
    }

    /**
     * <p>
     * Disables the test mode for the SDK.
     * </p>
     */
    public static void disableTestMode() {
        BranchUtil.setTestMode(false);
    }

    /**
     * Disable (or re-enable) ad network callouts. This setting is persistent.
     *
     * @param disabled (@link Boolean) whether ad network callouts should be disabled.
     */
    public void disableAdNetworkCallouts(boolean disabled) {
        PrefHelper.getInstance(context_).setAdNetworkCalloutsDisabled(disabled);
    }

    /**
     * Temporarily disables auto session initialization until user initializes themselves.
     *
     * Context: Branch expects session initialization to be started in LauncherActivity.onStart(),
     * if session initialization has not been started/completed by the time ANY Activity resumes,
     * Branch will auto-initialize. This allows Branch to keep an accurate count of all app sessions,
     * including instances when app is launched from a recent apps list and the first visible Activity
     * is not LauncherActivity.
     *
     * However, in certain scenarios users may need to delay session initialization (e.g. to asynchronously
     * retrieve some data that needs to be passed to Branch prior to session initialization). In those
     * cases, use expectDelayedSessionInitialization() to temporarily disable auto self initialization.
     * Once the user initializes the session themselves, the flag will be reset and auto session initialization
     * will be re-enabled.
     *
     * @param expectDelayedInit A {@link Boolean} to set the expectation flag.
     */
    public static void expectDelayedSessionInitialization(boolean expectDelayedInit) {
        disableAutoSessionInitialization = expectDelayedInit;
    }

    /**
     * <p>Sets a custom base URL for all calls to the Branch API.  Requires https.</p>
     * @param url The {@link String} URL base URL that the Branch API uses.
     */
    public static void setAPIUrl(String url) {
        PrefHelper.setAPIUrl(url);
    }

    /**
     * <p>Sets a custom CDN base URL.</p>
     * @param url The {@link String} base URL for CDN endpoints.
     */
    public static void setCDNBaseUrl(String url) {
        PrefHelper.setCDNBaseUrl(url);
    }

    /**
     * Method to change the Tracking state. If disabled SDK will not track any user data or state. SDK will not send any network calls except for deep linking when tracking is disabled
     */
    public void disableTracking(boolean disableTracking) {
        trackingController.disableTracking(context_, disableTracking);
    }
    
    /**
     * Checks if tracking is disabled. See {@link #disableTracking(boolean)}
     *
     * @return {@code true} if tracking is disabled
     */
    public boolean isTrackingDisabled() {
        return trackingController.isTrackingDisabled();
    }
    
    /**
     * Set timeout for Play Store Referrer library. Play Store Referrer library allows Branch to provide
     * more accurate tracking and attribution. This delays Branch initialization only the first time user opens the app.
     * This method allows to override the maximum wait time for play store referrer to arrive.
     * <p>
     *
     * @param delay {@link Long} Maximum wait time for install referrer broadcast in milli seconds. Set to {@link Branch#NO_PLAY_STORE_REFERRER_WAIT} if you don't want to wait for play store referrer
     */
    public static void setPlayStoreReferrerCheckTimeout(long delay) {
        checkInstallReferrer_ = delay > 0;
        playStoreReferrerWaitTime = delay;
    }
    
    /**
     * <p>
     * Disables or enables the instant deep link functionality.
     * </p>
     *
     * @param disableIDL Value {@code true} disables the  instant deep linking. Value {@code false} enables the  instant deep linking.
     */
    public static void disableInstantDeepLinking(boolean disableIDL) {
        enableInstantDeepLinking = !disableIDL;
    }

    // Package Private
    // For Unit Testing, we need to reset the Branch state
    static void shutDown() {
        ServerRequestQueue.shutDown();
        PrefHelper.shutDown();
        BranchUtil.shutDown();

        // BranchStrongMatchHelper.shutDown();
        // BranchViewHandler.shutDown();
        // DeepLinkRoutingValidator.shutDown();
        // GooglePlayStoreAttribution.shutDown();
        // InstantAppUtil.shutDown();
        // IntegrationValidator.shutDown();
        // ShareLinkManager.shutDown();
        // UniversalResourceAnalyser.shutDown();

        // Release these contexts immediately.

        // Reset all of the statics.
        branchReferral_ = null;
        bypassCurrentActivityIntentState_ = false;
        enableInstantDeepLinking = false;
        isActivityLifeCycleCallbackRegistered_ = false;

        bypassWaitingForIntent_ = false;

        checkInstallReferrer_ = true;
    }


    /**
     * <p>Manually sets the {@link Boolean} value, that indicates that the Branch API connection has
     * been initialised, to false - forcing re-initialisation.</p>
     */
    public void resetUserSession() {
        setInitState(SESSION_STATE.UNINITIALISED);
    }
    
    /**
     * Sets the max number of times to re-attempt a timed-out request to the Branch API, before
     * considering the request to have failed entirely. Default to 3. Note that the the network
     * timeout, as set in {@link #setNetworkTimeout(int)}, together with the retry interval value from
     * {@link #setRetryInterval(int)} will determine if the max retry count will be attempted.
     *
     * @param retryCount An {@link Integer} specifying the number of times to retry before giving
     *                   up and declaring defeat.
     */
    public void setRetryCount(int retryCount) {
        if (prefHelper_ != null && retryCount >= 0) {
            prefHelper_.setRetryCount(retryCount);
        }
    }
    
    /**
     * Sets the amount of time in milliseconds to wait before re-attempting a timed-out request
     * to the Branch API. Default 1000 ms.
     *
     * @param retryInterval An {@link Integer} value specifying the number of milliseconds to
     *                      wait before re-attempting a timed-out request.
     */
    public void setRetryInterval(int retryInterval) {
        if (prefHelper_ != null && retryInterval > 0) {
            prefHelper_.setRetryInterval(retryInterval);
        }
    }
    
    /**
     * <p>Sets the duration in milliseconds that the system should wait for a response before timing
     * out any Branch API. Default 5500 ms. Note that this is the total time allocated for all request
     * retries as set in {@link #setRetryCount(int)}.
     *
     * @param timeout An {@link Integer} value specifying the number of milliseconds to wait before
     *                considering the request to have timed out.
     */
    public void setNetworkTimeout(int timeout) {
        if (prefHelper_ != null && timeout > 0) {
            prefHelper_.setTimeout(timeout);
        }
    }

    /**
     * <p>Sets the duration in milliseconds that the system should wait for initializing a network
     * * request.</p>
     *
     * @param connectTimeout An {@link Integer} value specifying the number of milliseconds to wait before
     *                considering the initialization to have timed out.
     */
    public void setNetworkConnectTimeout(int connectTimeout) {
        if (prefHelper_ != null && connectTimeout > 0) {
            prefHelper_.setConnectTimeout(connectTimeout);
        }
    }

    /**
     * In cases of persistent no internet connection or offline modes,
     * set a maximum number of attempts for the Branch Request to be tried.
     *
     * Must be greater than 0
     * Defaults to 3
     * @param retryMax
     */
    public void setNoConnectionRetryMax(int retryMax){
        if(prefHelper_ != null && retryMax > 0){
            prefHelper_.setNoConnectionRetryMax(retryMax);
        }
    }

    /**
     * Sets the window for the referrer GCLID field. The GCLID will be persisted locally from the
     * time it is set + window in milliseconds. Thereafter, it will be deleted.
     *
     * By default, the window is set to 30 days, or 2592000000L in millseconds
     * Minimum of 0 milliseconds
     * Maximum of 3 years
     * @param window A {@link Long} value specifying the number of milliseconds to wait before
     *               deleting the locally persisted GCLID value.
     */
    public void setReferrerGclidValidForWindow(long window){
        if(prefHelper_ != null){
            prefHelper_.setReferrerGclidValidForWindow(window);
        }
    }
    
    /**
     * Method to control reading Android ID from device. Set this to true to disable reading the device id.
     * This method should be called from your {@link Application#onCreate()} method before creating Branch auto instance by calling {@link Branch#getAutoInstance(Context)}
     *
     * @param deviceIdFetch {@link Boolean with value true to disable reading the Android id from device}
     */
    public static void disableDeviceIDFetch(Boolean deviceIdFetch) {
        disableDeviceIDFetch_ = deviceIdFetch;
    }
    
    /**
     * Returns true if reading device id is disabled
     *
     * @return {@link Boolean} with value true to disable reading Andoid ID
     */
    public static boolean isDeviceIDFetchDisabled() {
        return disableDeviceIDFetch_;
    }
    
    /**
     * Sets the key-value pairs for debugging the deep link. The key-value set in debug mode is given back with other deep link data on branch init session.
     * This method should be called from onCreate() of activity which listens to Branch Init Session callbacks
     *
     * @param debugParams A {@link JSONObject} containing key-value pairs for debugging branch deep linking
     */
    public void setDeepLinkDebugMode(JSONObject debugParams) {
        deeplinkDebugParams_ = debugParams;
    }
    
    /**
     * <p>
     * Enable Facebook app link check operation during Branch initialisation.
     * </p>
     */
    public void enableFacebookAppLinkCheck() {
        enableFacebookAppLinkCheck_ = true;
    }
    
    /**
     * Enables or disables app tracking with Branch or any other third parties that Branch use internally
     *
     * @param isLimitFacebookTracking {@code true} to limit app tracking
     */
    public void setLimitFacebookTracking(boolean isLimitFacebookTracking) {
        prefHelper_.setLimitFacebookTracking(isLimitFacebookTracking);
    }
    
    /**
     * <p>Add key value pairs to all requests</p>
     */
    public void setRequestMetadata(@NonNull String key, @NonNull String value) {
        prefHelper_.setRequestMetadata(key, value);
    }

    /**
     * <p>
     * This API allows to tag the install with custom attribute. Add any key-values that qualify or distinguish an install here.
     * Please make sure this method is called before the Branch init, which is on the onStartMethod of first activity.
     * A better place to call this  method is right after Branch#getAutoInstance()
     * </p>
     */
    public Branch addInstallMetadata(@NonNull String key, @NonNull String value) {
        prefHelper_.addInstallMetadata(key, value);
        return this;
    }

    /**
     * <p>
     *   wrapper method to add the pre-install campaign analytics
     * </p>
     */
    public Branch setPreinstallCampaign(@NonNull String preInstallCampaign) {
        addInstallMetadata(PreinstallKey.campaign.getKey(), preInstallCampaign);
        return this;
    }

    /**
     * <p>
     *   wrapper method to add the pre-install campaign analytics
     * </p>
     */
    public Branch setPreinstallPartner(@NonNull String preInstallPartner) {
        addInstallMetadata(PreinstallKey.partner.getKey(), preInstallPartner);
        return this;
    }

    /**
     * Enables referring url attribution for preinstalled apps.
     *
     * By default, Branch prioritizes preinstall attribution on preinstalled apps.
     * Some clients prefer the referring link, when present, to be prioritized over preinstall attribution.
     */
    public static void setReferringLinkAttributionForPreinstalledAppsEnabled() {
        referringLinkAttributionForPreinstalledAppsEnabled = true;
    }

    public static boolean isReferringLinkAttributionForPreinstalledAppsEnabled() {
        return referringLinkAttributionForPreinstalledAppsEnabled;
    }

    public static void setIsUserAgentSync(boolean sync){
        userAgentSync = sync;
    }
    
    /*
     * <p>Closes the current session. Should be called by on getting the last actvity onStop() event.
     * </p>
     */
    void closeSessionInternal() {
        clearPartnerParameters();
        executeClose();
        prefHelper_.setExternalIntentUri(null);
        trackingController.updateTrackingState(context_); // Update the tracking state for next cold start
    }
    
    /**
     * Clears all pending requests in the queue
     */
    void clearPendingRequests() {
        requestQueue_.clear();
    }
    
    /**
     * <p>
     * Enabled Strong matching check using chrome cookies. This method should be called before
     * Branch#getAutoInstance(Context).</p>
     *
     * @param cookieMatchDomain The domain for the url used to match the cookie (eg. example.app.link)
     */
    public static void enableCookieBasedMatching(String cookieMatchDomain) {
        cookieBasedMatchDomain_ = cookieMatchDomain;
    }
    
    /**
     * <p>
     * Enabled Strong matching check using chrome cookies. This method should be called before
     * Branch#getAutoInstance(Context).</p>
     *
     * @param cookieMatchDomain The domain for the url used to match the cookie (eg. example.app.link)
     * @param delay             Time in millisecond to wait for the strong match to check to finish before Branch init session is called.
     *                          Default time is 750 msec.
     */
    public static void enableCookieBasedMatching(String cookieMatchDomain, int delay) {
        cookieBasedMatchDomain_ = cookieMatchDomain;
        BranchStrongMatchHelper.getInstance().setStrongMatchUrlHitDelay(delay);
    }
    
    /**
     * <p>Perform the state-safe actions required to terminate any open session, and report the
     * closed application event to the Branch API.</p>
     */
    private void executeClose() {
        if (initState_ != SESSION_STATE.UNINITIALISED) {
            ServerRequest req = new ServerRequestRegisterClose(context_);
            if (closeRequestNeeded) {
                handleNewRequest(req);
            } else {
                req.onRequestSucceeded(null, null);
            }
            setInitState(SESSION_STATE.UNINITIALISED);
        }
        closeRequestNeeded = false;
    }

    public static void registerPlugin(String name, String version) {
        pluginName = name;
        pluginVersion = version;
    }

    public static String getPluginVersion() {
        return pluginVersion;
    }

    static String getPluginName() {
        return pluginName;
    }

    void readAndStripParam(Uri data, Activity activity) {
        if (enableInstantDeepLinking) {

            // If activity is launched anew (i.e. not from stack), then its intent can be readily consumed.
            // Otherwise, we have to wait for onResume, which ensures that we will have the latest intent.
            // In the latter case, IDL works only partially because the callback is delayed until onResume.
            boolean activityHasValidIntent = intentState_ == INTENT_STATE.READY ||
                    !activityLifeCycleObserver.isCurrentActivityLaunchedFromStack();

            // Skip IDL if intent contains an unused Branch link.
            boolean noUnusedBranchLinkInIntent = !isRestartSessionRequested(activity != null ? activity.getIntent() : null);

            if (activityHasValidIntent && noUnusedBranchLinkInIntent) {
                extractSessionParamsForIDL(data, activity);
            }
        }

        if (bypassCurrentActivityIntentState_) {
            intentState_ = INTENT_STATE.READY;
        }

        if (intentState_ == INTENT_STATE.READY) {

            // Capture the intent URI and extra for analytics in case started by external intents such as google app search
            extractExternalUriAndIntentExtras(data, activity);

            // if branch link is detected we don't need to look for click ID or app link anymore and can terminate early
            if (extractBranchLinkFromIntentExtra(activity)) return;

            // Check for link click id or app link
            if (!isActivityLaunchedFromHistory(activity)) {
                // if click ID is detected we don't need to look for app link anymore and can terminate early
                if (extractClickID(data, activity)) return;

                // Check if the clicked url is an app link pointing to this app
                extractAppLink(data, activity);
            }
        }
    }

    void unlockSDKInitWaitLock() {
        if (requestQueue_ == null) return;
        requestQueue_.unlockProcessWait(ServerRequest.PROCESS_WAIT_LOCK.SDK_INIT_WAIT_LOCK);
        processNextQueueItem();
    }
    
    private boolean isIntentParamsAlreadyConsumed(Activity activity) {
        return activity != null && activity.getIntent() != null &&
                activity.getIntent().getBooleanExtra(Defines.IntentKeys.BranchLinkUsed.getKey(), false);
    }
    
    private boolean isActivityLaunchedFromHistory(Activity activity) {
        return activity != null && activity.getIntent() != null &&
                (activity.getIntent().getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0;
    }

    /**
     * Package Private.
     * @return the link which opened this application session if opened by a link click.
     */
    String getSessionReferredLink() {
        String link = prefHelper_.getExternalIntentUri();
        return (link.equals(PrefHelper.NO_STRING_VALUE) ? null : link);
    }
    
    @Override
    public void onAdsParamsFetchFinished() {
        isGAParamsFetchInProgress_ = false;
        requestQueue_.unlockProcessWait(ServerRequest.PROCESS_WAIT_LOCK.GAID_FETCH_WAIT_LOCK);
        if (performCookieBasedStrongMatchingOnGAIDAvailable) {
            performCookieBasedStrongMatch();
            performCookieBasedStrongMatchingOnGAIDAvailable = false;
        } else {
            processNextQueueItem();
        }
    }
    
    @Override
    public void onGoogleInstallReferrerEventsFinished() {
        requestQueue_.unlockProcessWait(ServerRequest.PROCESS_WAIT_LOCK.GOOGLE_INSTALL_REFERRER_FETCH_WAIT_LOCK);
        waitingForGoogleInstallReferrer = false;
        tryProcessNextQueueItemAfterInstallReferrer();
    }

    @Override
    public void onHuaweiInstallReferrerEventsFinished() {
        requestQueue_.unlockProcessWait(ServerRequest.PROCESS_WAIT_LOCK.HUAWEI_INSTALL_REFERRER_FETCH_WAIT_LOCK);
        waitingForHuaweiInstallReferrer = false;
        tryProcessNextQueueItemAfterInstallReferrer();
    }

    @Override
    public void onSamsungInstallReferrerEventsFinished() {
        requestQueue_.unlockProcessWait(ServerRequest.PROCESS_WAIT_LOCK.SAMSUNG_INSTALL_REFERRER_FETCH_WAIT_LOCK);
        waitingForSamsungInstallReferrer = false;
        tryProcessNextQueueItemAfterInstallReferrer();
    }

    @Override
    public void onXiaomiInstallReferrerEventsFinished() {
        requestQueue_.unlockProcessWait(ServerRequest.PROCESS_WAIT_LOCK.XIAOMI_INSTALL_REFERRER_FETCH_WAIT_LOCK);
        waitingForXiaomiInstallReferrer = false;
        tryProcessNextQueueItemAfterInstallReferrer();
    }

    private void tryProcessNextQueueItemAfterInstallReferrer() {
        if(!(waitingForGoogleInstallReferrer || waitingForHuaweiInstallReferrer || waitingForSamsungInstallReferrer || waitingForXiaomiInstallReferrer)){
            String store = StoreReferrerUtils.getLatestValidReferrerStore();
            StoreReferrerUtils.writeLatestInstallReferrer(context_, store);
            processNextQueueItem();
        }
    }

    /**
     * Branch collect the URLs in the incoming intent for better attribution. Branch SDK extensively check for any sensitive data in the URL and skip if exist.
     * However the following method provisions application to set SDK to collect only URLs in particular form. This method allow application to specify a set of regular expressions to white list the URL collection.
     * If whitelist is not empty SDK will collect only the URLs that matches the white list.
     * <p>
     * This method should be called immediately after calling {@link Branch#getAutoInstance(Context)}
     *
     * @param urlWhiteListPattern A regular expression with a URI white listing pattern
     * @return {@link Branch} instance for successive method calls
     */
    public Branch addWhiteListedScheme(String urlWhiteListPattern) {
        if (urlWhiteListPattern != null) {
            UniversalResourceAnalyser.getInstance(context_).addToAcceptURLFormats(urlWhiteListPattern);
        }
        return this;
    }
    
    /**
     * Branch collect the URLs in the incoming intent for better attribution. Branch SDK extensively check for any sensitive data in the URL and skip if exist.
     * However the following method provisions application to set SDK to collect only URLs in particular form. This method allow application to specify a set of regular expressions to white list the URL collection.
     * If whitelist is not empty SDK will collect only the URLs that matches the white list.
     * <p>
     * This method should be called immediately after calling {@link Branch#getAutoInstance(Context)}
     *
     * @param urlWhiteListPatternList {@link List} of regular expressions with URI white listing pattern
     * @return {@link Branch} instance for successive method calls
     */
    public Branch setWhiteListedSchemes(List<String> urlWhiteListPatternList) {
        if (urlWhiteListPatternList != null) {
            UniversalResourceAnalyser.getInstance(context_).addToAcceptURLFormats(urlWhiteListPatternList);
        }
        return this;
    }
    
    /**
     * Branch collect the URLs in the incoming intent for better attribution. Branch SDK extensively check for any sensitive data in the URL and skip if exist.
     * This method allows applications specify SDK to skip any additional URL patterns to be skipped
     * <p>
     * This method should be called immediately after calling {@link Branch#getAutoInstance(Context)}
     *
     * @param urlSkipPattern {@link String} A URL pattern that Branch SDK should skip from collecting data
     * @return {@link Branch} instance for successive method calls
     */
    public Branch addUriHostsToSkip(String urlSkipPattern) {
        if (!TextUtils.isEmpty(urlSkipPattern))
            UniversalResourceAnalyser.getInstance(context_).addToSkipURLFormats(urlSkipPattern);
        return this;
    }
    
    /**
     * Check and update the URL / URI Skip list in case an update is available.
     */
    void updateSkipURLFormats() {
        UniversalResourceAnalyser.getInstance(context_).checkAndUpdateSkipURLFormats(context_);
    }
    
    /**
     * <p>Identifies the current user to the Branch API by supplying a unique identifier as a
     * {@link String} value. No callback.</p>
     *
     * @param userId A {@link String} value containing the unique identifier of the user.
     */
    public void setIdentity(@NonNull String userId) {
        setIdentity(userId, null);
    }
    
    /**
     * <p>Identifies the current user to the Branch API by supplying a unique identifier as a
     * {@link String} value, with a callback specified to perform a defined action upon successful
     * response to request.</p>
     *
     * @param userId   A {@link String} value containing the unique identifier of the user.
     * @param callback A {@link BranchReferralInitListener} callback instance that will return
     *                 the data associated with the user id being assigned, if available.
     */
    public void setIdentity(@NonNull String userId, @Nullable BranchReferralInitListener
            callback) {

        installDeveloperId = userId;

        ServerRequestIdentifyUserRequest req = new ServerRequestIdentifyUserRequest(context_, callback, userId);
        if (!req.constructError_ && !req.handleErrors(context_)) {
            handleNewRequest(req);
        } else {
            if (req.isExistingID()) {
                req.handleUserExist(branchReferral_);
            }
        }
    }

    /**
     * Gets all available cross platform ids.
     *
     * @param callback An instance of {@link io.branch.referral.ServerRequestGetCPID.BranchCrossPlatformIdListener}
     *                to callback with cross platform ids
     *
     */
    public void getCrossPlatformIds(@NonNull ServerRequestGetCPID.BranchCrossPlatformIdListener callback) {
        if (context_ != null) {
            handleNewRequest(new ServerRequestGetCPID(context_, callback));
        }
    }

    /**
     * Gets the available last attributed touch data. The attribution window is set to the value last
     * saved via PreferenceHelper.setLATDAttributionWindow(). If no value has been saved, Branch
     * defaults to a 30 day attribution window (SDK sends -1 to request the default from the server).
     *
     * @param callback An instance of {@link io.branch.referral.ServerRequestGetLATD.BranchLastAttributedTouchDataListener}
     *                 to callback with last attributed touch data
     *
     */
    public void getLastAttributedTouchData(@NonNull BranchLastAttributedTouchDataListener callback) {
        if (context_ != null) {
            handleNewRequest(new ServerRequestGetLATD(context_, Defines.RequestPath.GetLATD, callback));
        }
    }

    /**
     * Gets the available last attributed touch data with a custom set attribution window.
     *
     * @param callback An instance of {@link io.branch.referral.ServerRequestGetLATD.BranchLastAttributedTouchDataListener}
     *                to callback with last attributed touch data
     * @param attributionWindow An {@link int} to bound the the window of time in days during which
     *                          the attribution data is considered valid. Note that, server side, the
     *                          maximum value is 90.
     *
     */
    public void getLastAttributedTouchData(BranchLastAttributedTouchDataListener callback, int attributionWindow) {
        if (context_ != null) {
            handleNewRequest(new ServerRequestGetLATD(context_, Defines.RequestPath.GetLATD, callback, attributionWindow));
        }
    }

    /**
     * Indicates whether or not this user has a custom identity specified for them. Note that this is independent of installs.
     * If you call setIdentity, this device will have that identity associated with this user until logout is called.
     * This includes persisting through uninstalls, as we track device id.
     *
     * @return A {@link Boolean} value that will return <i>true</i> only if user already has an identity.
     */
    public boolean isUserIdentified() {
        return !prefHelper_.getIdentity().equals(PrefHelper.NO_STRING_VALUE);
    }
    
    /**
     * <p>This method should be called if you know that a different person is about to use the app. For example,
     * if you allow users to log out and let their friend use the app, you should call this to notify Branch
     * to create a new user for this device. This will clear the first and latest params, as a new session is created.</p>
     */
    public void logout() {
        logout(null);
    }
    
    /**
     * <p>This method should be called if you know that a different person is about to use the app. For example,
     * if you allow users to log out and let their friend use the app, you should call this to notify Branch
     * to create a new user for this device. This will clear the first and latest params, as a new session is created.</p>
     *
     * @param callback An instance of {@link io.branch.referral.Branch.LogoutStatusListener} to callback with the logout operation status.
     */
    public void logout(LogoutStatusListener callback) {
        ServerRequest req = new ServerRequestLogout(context_, callback);
        if (!req.constructError_ && !req.handleErrors(context_)) {
            handleNewRequest(req);
        }
    }
    
    /**
     * <p>A void call to indicate that the user has performed a specific action and for that to be
     * reported to the Branch API, with additional app-defined meta data to go along with that action.</p>
     *
     * @param action   A {@link String} value to be passed as an action that the user has carried
     *                 out. For example "registered" or "logged in".
     * @param metadata A {@link JSONObject} containing app-defined meta-data to be attached to a
     *                 user action that has just been completed.
     * @deprecated     Please use {@link BranchEvent} for your event tracking use cases.
     *                 You can refer to <a href="https://help.branch.io/developers-hub/docs/tracking-commerce-content-lifecycle-and-custom-events">Track Commerce, 
     *                 Content, Lifecycle and Custom Events</a> for additional information.
     */
    @Deprecated
    public void userCompletedAction(@NonNull final String action, JSONObject metadata) {
        userCompletedAction(action, metadata, null);
    }
    
    /**
     * <p>A void call to indicate that the user has performed a specific action and for that to be
     * reported to the Branch API.</p>
     *
     * @param action A {@link String} value to be passed as an action that the user has carried
     *               out. For example "registered" or "logged in".
     * @deprecated   Please use {@link BranchEvent} for your event tracking use cases.
     *               You can refer to <a href="https://help.branch.io/developers-hub/docs/tracking-commerce-content-lifecycle-and-custom-events">Track Commerce,
     *               Content, Lifecycle and Custom Events</a> for additional information.
     */
    @Deprecated
    public void userCompletedAction(final String action) {
        userCompletedAction(action, null, null);
    }
    
    /**
     * <p>A void call to indicate that the user has performed a specific action and for that to be
     * reported to the Branch API.</p>
     *
     * @param action   A {@link String} value to be passed as an action that the user has carried
     *                 out. For example "registered" or "logged in".
     * @param callback instance of {@link BranchViewHandler.IBranchViewEvents} to listen Branch view events
     * @deprecated     Please use {@link BranchEvent} for your event tracking use cases.
     *                 You can refer to <a href="https://help.branch.io/developers-hub/docs/tracking-commerce-content-lifecycle-and-custom-events">Track Commerce, 
     *                 Content, Lifecycle and Custom Events</a> for additional information.
     */
    @Deprecated
    public void userCompletedAction(final String action, BranchViewHandler.
            IBranchViewEvents callback) {
        userCompletedAction(action, null, callback);
    }
    
    /**
     * <p>A void call to indicate that the user has performed a specific action and for that to be
     * reported to the Branch API, with additional app-defined meta data to go along with that action.</p>
     *
     * @param action   A {@link String} value to be passed as an action that the user has carried
     *                 out. For example "registered" or "logged in".
     * @param metadata A {@link JSONObject} containing app-defined meta-data to be attached to a
     *                 user action that has just been completed.
     * @param callback instance of {@link BranchViewHandler.IBranchViewEvents} to listen Branch view events
     * @deprecated     Please use {@link BranchEvent} for your event tracking use cases.
     *                 You can refer to <a href="https://help.branch.io/developers-hub/docs/tracking-commerce-content-lifecycle-and-custom-events">Track Commerce, 
     *                 Content, Lifecycle and Custom Events</a> for additional information.
     */
    @Deprecated
    public void userCompletedAction(@NonNull final String action, JSONObject metadata,
                                    BranchViewHandler.IBranchViewEvents callback) {
        PrefHelper.LogAlways("'userCompletedAction' has been deprecated. Please use BranchEvent for your event tracking use cases.You can refer to  https://help.branch.io/developers-hub/docs/tracking-commerce-content-lifecycle-and-custom-events for additional information.");
        ServerRequest req = new ServerRequestActionCompleted(context_,
                action, null, metadata, callback);
        if (!req.constructError_ && !req.handleErrors(context_)) {
            handleNewRequest(req);
        }
    }
    
    /**
     * @deprecated  Please use {@link BranchEvent} for your event tracking use cases.You can refer to
     *              <a href="https://help.branch.io/developers-hub/docs/tracking-commerce-content-lifecycle-and-custom-events">Track Commerce,
     *              Content, Lifecycle and Custom Events</a> for additional information.
     */
    @Deprecated
    public void sendCommerceEvent(@NonNull CommerceEvent commerceEvent, JSONObject metadata,
                                  BranchViewHandler.IBranchViewEvents callback) {
        PrefHelper.LogAlways("'sendCommerceEvent' has been deprecated. Please use BranchEvent for your event tracking use cases.You can refer to  https://help.branch.io/developers-hub/docs/tracking-commerce-content-lifecycle-and-custom-events for additional information.");
        ServerRequest req = new ServerRequestActionCompleted(context_,
                BRANCH_STANDARD_EVENT.PURCHASE.getName(), commerceEvent, metadata, callback);
        if (!req.constructError_ && !req.handleErrors(context_)) {
            handleNewRequest(req);
        }
    }
    
    /**
     * @deprecated  Please use {@link BranchEvent} for your event tracking use cases.You can refer to
     *              <a href="https://help.branch.io/developers-hub/docs/tracking-commerce-content-lifecycle-and-custom-events">Track Commerce,
     *              Content, Lifecycle and Custom Events</a> for additional information.
     */
    @Deprecated
    public void sendCommerceEvent(@NonNull CommerceEvent commerceEvent) {
        sendCommerceEvent(commerceEvent, null, null);
    }
    
    /**
     * <p>Returns the parameters associated with the link that referred the user. This is only set once,
     * the first time the user is referred by a link. Think of this as the user referral parameters.
     * It is also only set if isReferrable is equal to true, which by default is only true
     * on a fresh install (not upgrade or reinstall). This will change on setIdentity (if the
     * user already exists from a previous device) and logout.</p>
     *
     * @return A {@link JSONObject} containing the install-time parameters as configured
     * locally.
     */
    public JSONObject getFirstReferringParams() {
        String storedParam = prefHelper_.getInstallParams();
        JSONObject firstReferringParams = convertParamsStringToDictionary(storedParam);
        firstReferringParams = appendDebugParams(firstReferringParams);
        return firstReferringParams;
    }

    @SuppressWarnings("WeakerAccess")
    public void removeSessionInitializationDelay() {
        requestQueue_.unlockProcessWait(ServerRequest.PROCESS_WAIT_LOCK.USER_SET_WAIT_LOCK);
        processNextQueueItem();
    }
    
    /**
     * <p>This function must be called from a non-UI thread! If Branch has no install link data,
     * and this func is called, it will return data upon initializing, or until LATCH_WAIT_UNTIL.
     * Returns the parameters associated with the link that referred the user. This is only set once,
     * the first time the user is referred by a link. Think of this as the user referral parameters.
     * It is also only set if isReferrable is equal to true, which by default is only true
     * on a fresh install (not upgrade or reinstall). This will change on setIdentity (if the
     * user already exists from a previous device) and logout.</p>
     *
     * @return A {@link JSONObject} containing the install-time parameters as configured
     * locally.
     */
    public JSONObject getFirstReferringParamsSync() {
        getFirstReferringParamsLatch = new CountDownLatch(1);
        if (prefHelper_.getInstallParams().equals(PrefHelper.NO_STRING_VALUE)) {
            try {
                getFirstReferringParamsLatch.await(LATCH_WAIT_UNTIL, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
            }
        }
        String storedParam = prefHelper_.getInstallParams();
        JSONObject firstReferringParams = convertParamsStringToDictionary(storedParam);
        firstReferringParams = appendDebugParams(firstReferringParams);
        getFirstReferringParamsLatch = null;
        return firstReferringParams;
    }
    
    /**
     * <p>Returns the parameters associated with the link that referred the session. If a user
     * clicks a link, and then opens the app, initSession will return the parameters of the link
     * and then set them in as the latest parameters to be retrieved by this method. By default,
     * sessions persist for the duration of time that the app is in focus. For example, if you
     * minimize the app, these parameters will be cleared when closeSession is called.</p>
     *
     * @return A {@link JSONObject} containing the latest referring parameters as
     * configured locally.
     */
    public JSONObject getLatestReferringParams() {
        String storedParam = prefHelper_.getSessionParams();
        JSONObject latestParams = convertParamsStringToDictionary(storedParam);
        latestParams = appendDebugParams(latestParams);
        return latestParams;
    }
    
    /**
     * <p>This function must be called from a non-UI thread! If Branch has not been initialized
     * and this func is called, it will return data upon initialization, or until LATCH_WAIT_UNTIL.
     * Returns the parameters associated with the link that referred the session. If a user
     * clicks a link, and then opens the app, initSession will return the parameters of the link
     * and then set them in as the latest parameters to be retrieved by this method. By default,
     * sessions persist for the duration of time that the app is in focus. For example, if you
     * minimize the app, these parameters will be cleared when closeSession is called.</p>
     *
     * @return A {@link JSONObject} containing the latest referring parameters as
     * configured locally.
     */
    public JSONObject getLatestReferringParamsSync() {
        getLatestReferringParamsLatch = new CountDownLatch(1);
        try {
            if (initState_ != SESSION_STATE.INITIALISED) {
                getLatestReferringParamsLatch.await(LATCH_WAIT_UNTIL, TimeUnit.MILLISECONDS);
            }
        } catch (InterruptedException e) {
        }
        String storedParam = prefHelper_.getSessionParams();
        JSONObject latestParams = convertParamsStringToDictionary(storedParam);
        latestParams = appendDebugParams(latestParams);
        getLatestReferringParamsLatch = null;
        return latestParams;
    }

    /**
     * Add a Partner Parameter for Facebook.
     * Once set, this parameter is attached to installs, opens and events until cleared or the app restarts.
     *
     * See Facebook's documentation for details on valid parameters
     */
    public void addFacebookPartnerParameterWithName(@NonNull String key, @NonNull String value) {
        if (!trackingController.isTrackingDisabled()) {
            prefHelper_.partnerParams_.addFacebookParameter(key, value);
        }
    }

    /**
     * Add a Partner Parameter for Snap.
     * Once set, this parameter is attached to installs, opens and events until cleared or the app restarts.
     *
     * See Snap's documentation for details on valid parameters
     */
    public void addSnapPartnerParameterWithName(@NonNull String key, @NonNull String value) {
        if (!trackingController.isTrackingDisabled()) {
            prefHelper_.partnerParams_.addSnapParameter(key, value);
        }
    }

    /**
     * Clears all Partner Parameters
     */
    public void clearPartnerParameters() {
        prefHelper_.partnerParams_.clearAllParameters();
    }
    
    /**
     * Append the deep link debug params to the original params
     *
     * @param originalParams A {@link JSONObject} original referrer parameters
     * @return A new {@link JSONObject} with debug params appended.
     */
    private JSONObject appendDebugParams(JSONObject originalParams) {
        try {
            if (originalParams != null && deeplinkDebugParams_ != null) {
                if (deeplinkDebugParams_.length() > 0) {
                    PrefHelper.Debug("You're currently in deep link debug mode. Please comment out 'setDeepLinkDebugMode' to receive the deep link parameters from a real Branch link");
                }
                Iterator<String> keys = deeplinkDebugParams_.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    originalParams.put(key, deeplinkDebugParams_.get(key));
                }
            }
        } catch (Exception ignore) {
        }
        return originalParams;
    }
    
    public JSONObject getDeeplinkDebugParams() {
        if (deeplinkDebugParams_ != null && deeplinkDebugParams_.length() > 0) {
            PrefHelper.Debug("You're currently in deep link debug mode. Please comment out 'setDeepLinkDebugMode' to receive the deep link parameters from a real Branch link");
        }
        return deeplinkDebugParams_;
    }
    
    
    //-----------------Generate Short URL      -------------------------------------------//
    
    /**
     * <p> Generates a shorl url for the given {@link ServerRequestCreateUrl} object </p>
     *
     * @param req An instance  of {@link ServerRequestCreateUrl} with parameters create the short link.
     * @return A url created with the given request if the request is synchronous else null.
     * Note : This method can be used only internally. Use {@link BranchUrlBuilder} for creating short urls.
     */
    String generateShortLinkInternal(ServerRequestCreateUrl req) {
        if (!req.constructError_ && !req.handleErrors(context_)) {
            if (linkCache_.containsKey(req.getLinkPost())) {
                String url = linkCache_.get(req.getLinkPost());
                req.onUrlAvailable(url);
                return url;
            }
            if (req.isAsync()) {
                handleNewRequest(req);
            } else {
                return generateShortLinkSync(req);
            }
        }
        return null;
    }



    /**
     * <p>Creates options for sharing a link with other Applications. Creates a link with given attributes and shares with the
     * user selected clients.</p>
     *
     * @param builder A {@link BranchShareSheetBuilder} instance to build share link.
     */
    void shareLink(BranchShareSheetBuilder builder) {
        //Cancel any existing sharing in progress.
        if (shareLinkManager_ != null) {
            shareLinkManager_.cancelShareLinkDialog(true);
        }
        shareLinkManager_ = new ShareLinkManager();
        shareLinkManager_.shareLink(builder);
    }
    
    /**
     * <p>Cancel current share link operation and Application selector dialog. If your app is not using auto session management, make sure you are
     * calling this method before your activity finishes inorder to prevent any window leak. </p>
     *
     * @param animateClose A {@link Boolean} to specify whether to close the dialog with an animation.
     *                     A value of true will close the dialog with an animation. Setting this value
     *                     to false will close the Dialog immediately.
     */
    public void cancelShareLinkDialog(boolean animateClose) {
        if (shareLinkManager_ != null) {
            shareLinkManager_.cancelShareLinkDialog(animateClose);
        }
    }
    
    // PRIVATE FUNCTIONS
    
    private String generateShortLinkSync(ServerRequestCreateUrl req) {
        if (trackingController.isTrackingDisabled()) {
            return req.getLongUrl();
        }
        if (initState_ == SESSION_STATE.INITIALISED) {
            ServerResponse response = null;
            try {
                int timeOut = prefHelper_.getTimeout() + 2000; // Time out is set to slightly more than link creation time to prevent any edge case
                response = new GetShortLinkTask().execute(req).get(timeOut, TimeUnit.MILLISECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException ignore) {
            }
            String url = null;
            if (req.isDefaultToLongUrl()) {
                url = req.getLongUrl();
            }
            if (response != null && response.getStatusCode() == HttpURLConnection.HTTP_OK) {
                try {
                    url = response.getObject().getString("url");
                    if (req.getLinkPost() != null) {
                        linkCache_.put(req.getLinkPost(), url);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            return url;
        } else {
            PrefHelper.Debug("Warning: User session has not been initialized");
        }
        return null;
    }
    
    private JSONObject convertParamsStringToDictionary(String paramString) {
        if (paramString.equals(PrefHelper.NO_STRING_VALUE)) {
            return new JSONObject();
        } else {
            try {
                return new JSONObject(paramString);
            } catch (JSONException e) {
                byte[] encodedArray = Base64.decode(paramString.getBytes(), Base64.NO_WRAP);
                try {
                    return new JSONObject(new String(encodedArray));
                } catch (JSONException ex) {
                    ex.printStackTrace();
                    return new JSONObject();
                }
            }
        }
    }
    
    void processNextQueueItem() {
        try {
            serverSema_.acquire();
            if (networkCount_ == 0 && requestQueue_.getSize() > 0) {
                networkCount_ = 1;
                ServerRequest req = requestQueue_.peek();
                
                serverSema_.release();
                if (req != null) {
                    PrefHelper.Debug("processNextQueueItem, req " + req.getClass().getSimpleName());
                    if (!req.isWaitingOnProcessToFinish()) {
                        // All request except Install request need a valid RandomizedBundleToken
                        if (!(req instanceof ServerRequestRegisterInstall) && !hasUser()) {
                            PrefHelper.Debug("Branch Error: User session has not been initialized!");
                            networkCount_ = 0;
                            req.handleFailure(BranchError.ERR_NO_SESSION, "");
                        }
                        // Determine if a session is needed to execute (SDK-271)
                        else if (requestNeedsSession(req) && !isSessionAvailableForRequest()) {
                            networkCount_ = 0;
                            req.handleFailure(BranchError.ERR_NO_SESSION, "");
                        } else {
                            executeTimedBranchPostTask(req, prefHelper_.getTaskTimeout());
                        }
                    } else {
                        networkCount_ = 0;
                    }
                } else {
                    requestQueue_.remove(null); //In case there is any request nullified remove it.
                }
            } else {
                serverSema_.release();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void executeTimedBranchPostTask(final ServerRequest req, final int timeout) {
        final CountDownLatch latch = new CountDownLatch(1);
        final BranchPostTask postTask = new BranchPostTask(this, req, latch);

        postTask.executeTask();
        if (Looper.myLooper() == Looper.getMainLooper()) {
            new Thread(new Runnable() {
                @Override public void run() {
                    awaitTimedBranchPostTask(latch, timeout, postTask);
                }
            }).start();
        } else {
            awaitTimedBranchPostTask(latch, timeout, postTask);
        }
    }

    private void awaitTimedBranchPostTask(CountDownLatch latch, int timeout, BranchPostTask postTask) {
        try {
            if (!latch.await(timeout, TimeUnit.MILLISECONDS)) {
                postTask.cancel(true);
                postTask.onPostExecuteInner(new ServerResponse(postTask.thisReq_.getRequestPath(), ERR_BRANCH_TASK_TIMEOUT, ""));
            }
        } catch (InterruptedException e) {
            postTask.cancel(true);
            postTask.onPostExecuteInner(new ServerResponse(postTask.thisReq_.getRequestPath(), ERR_BRANCH_TASK_TIMEOUT, ""));
        }
    }

    // Determine if a Request needs a Session to proceed.
    private boolean requestNeedsSession(ServerRequest request) {
        if (request instanceof ServerRequestInitSession) {
            return false;
        } else if (request instanceof ServerRequestCreateUrl) {
            return false;
        }

        // All other Request Types need a session.
        return true;
    }

    // Determine if a Session is available for a Request to proceed.
    private boolean isSessionAvailableForRequest() {
        return (hasSession() && hasRandomizedDeviceToken());
    }
    
    void updateAllRequestsInQueue() {
        try {
            for (int i = 0; i < requestQueue_.getSize(); i++) {
                ServerRequest req = requestQueue_.peekAt(i);
                if (req != null) {
                    JSONObject reqJson = req.getPost();
                    if (reqJson != null) {
                        if (reqJson.has(Defines.Jsonkey.SessionID.getKey())) {
                            req.getPost().put(Defines.Jsonkey.SessionID.getKey(), prefHelper_.getSessionID());
                        }
                        if (reqJson.has(Defines.Jsonkey.RandomizedBundleToken.getKey())) {
                            req.getPost().put(Defines.Jsonkey.RandomizedBundleToken.getKey(), prefHelper_.getRandomizedBundleToken());
                        }
                        if (reqJson.has(Defines.Jsonkey.RandomizedDeviceToken.getKey())) {
                            req.getPost().put(Defines.Jsonkey.RandomizedDeviceToken.getKey(), prefHelper_.getRandomizedDeviceToken());
                        }
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public TrackingController getTrackingController() {
        return trackingController;
    }

    public DeviceInfo getDeviceInfo() {
        return deviceInfo_;
    }

    public BranchPluginSupport getBranchPluginSupport() {
        return branchPluginSupport_;
    }

    public BranchQRCodeCache getBranchQRCodeCache() {
        return branchQRCodeCache_;
    }

    PrefHelper getPrefHelper() {
        return prefHelper_;
    }

    boolean isGAParamsFetchInProgress() {
        return isGAParamsFetchInProgress_;
    }

    void setGAParamsFetchInProgress(boolean GAParamsFetchInProgress) {
        isGAParamsFetchInProgress_ = GAParamsFetchInProgress;
    }

    ShareLinkManager getShareLinkManager() {
        return shareLinkManager_;
    }

    void setIntentState(INTENT_STATE intentState) {
        this.intentState_ = intentState;
    }

    void setInitState(SESSION_STATE initState) {
        this.initState_ = initState;
    }

    SESSION_STATE getInitState() {
        return initState_;
    }
    
    private boolean hasSession() {
        return !prefHelper_.getSessionID().equals(PrefHelper.NO_STRING_VALUE);
    }

    public void setInstantDeepLinkPossible(boolean instantDeepLinkPossible) {
        isInstantDeepLinkPossible = instantDeepLinkPossible;
    }

    public boolean isInstantDeepLinkPossible() {
        return isInstantDeepLinkPossible;
    }
    
    private boolean hasRandomizedDeviceToken() {
        return !prefHelper_.getRandomizedDeviceToken().equals(PrefHelper.NO_STRING_VALUE);
    }
    
    private boolean hasUser() {
        return !prefHelper_.getRandomizedBundleToken().equals(PrefHelper.NO_STRING_VALUE);
    }
    
    private void insertRequestAtFront(ServerRequest req) {
        if (networkCount_ == 0) {
            requestQueue_.insert(req, 0);
        } else {
            requestQueue_.insert(req, 1);
        }
    }

    void initializeSession(ServerRequestInitSession initRequest, int delay) {
        if ((prefHelper_.getBranchKey() == null || prefHelper_.getBranchKey().equalsIgnoreCase(PrefHelper.NO_STRING_VALUE))) {
            setInitState(SESSION_STATE.UNINITIALISED);
            //Report Key error on callback
            if (initRequest.callback_ != null) {
                initRequest.callback_.onInitFinished(null, new BranchError("Trouble initializing Branch.", BranchError.ERR_BRANCH_KEY_INVALID));
            }
            PrefHelper.Debug("Warning: Please enter your branch_key in your project's manifest");
            return;
        } else if (isTestModeEnabled()) {
            PrefHelper.Debug("Warning: You are using your test app's Branch Key. Remember to change it to live Branch Key during deployment.");
        }

        if (initState_ == SESSION_STATE.UNINITIALISED && getSessionReferredLink() == null && enableFacebookAppLinkCheck_) {
            // Check if opened by facebook with deferred install data
            boolean appLinkRqSucceeded = DeferredAppLinkDataHandler.fetchDeferredAppLinkData(
                    context_, new DeferredAppLinkDataHandler.AppLinkFetchEvents() {
                @Override
                public void onAppLinkFetchFinished(String nativeAppLinkUrl) {
                    prefHelper_.setIsAppLinkTriggeredInit(true); // callback returns when app link fetch finishes with success or failure. Report app link checked in both cases
                    if (nativeAppLinkUrl != null) {
                        Uri appLinkUri = Uri.parse(nativeAppLinkUrl);
                        String bncLinkClickId = appLinkUri.getQueryParameter(Defines.Jsonkey.LinkClickID.getKey());
                        if (!TextUtils.isEmpty(bncLinkClickId)) {
                            prefHelper_.setLinkClickIdentifier(bncLinkClickId);
                        }
                    }
                    requestQueue_.unlockProcessWait(ServerRequest.PROCESS_WAIT_LOCK.FB_APP_LINK_WAIT_LOCK);
                    processNextQueueItem();
                }
            });
            if (appLinkRqSucceeded) {
                initRequest.addProcessWaitLock(ServerRequest.PROCESS_WAIT_LOCK.FB_APP_LINK_WAIT_LOCK);
            }
        }

        if (delay > 0) {
            initRequest.addProcessWaitLock(ServerRequest.PROCESS_WAIT_LOCK.USER_SET_WAIT_LOCK);
            new Handler().postDelayed(new Runnable() {
                @Override public void run() {
                    removeSessionInitializationDelay();
                }
            }, delay);
        }

        // Re 'forceBranchSession':
        // Check if new session is being forced. There are two use cases for setting the ForceNewBranchSession to true:
        // 1. Launch an activity via a push notification while app is in foreground but does not have
        // the particular activity in the backstack, in such cases, users can't utilize reInitSession() because
        // it's called from onNewIntent() which is never invoked
        // todo: this is tricky for users, get rid of ForceNewBranchSession if possible. (if flag is not set, the content from Branch link is lost)
        // 2. Some users navigate their apps via Branch links so they would have to set ForceNewBranchSession to true
        // which will blow up the session count in analytics but does the job.
        Intent intent = getCurrentActivity() != null ? getCurrentActivity().getIntent() : null;
        boolean forceBranchSession = isRestartSessionRequested(intent);

        if (getInitState() == SESSION_STATE.UNINITIALISED || forceBranchSession) {
            if (forceBranchSession && intent != null) {
                intent.removeExtra(Defines.IntentKeys.ForceNewBranchSession.getKey()); // SDK-881, avoid double initialization
            }
            registerAppInit(initRequest, false);
        } else if (initRequest.callback_ != null) {
            // Else, let the user know session initialization failed because it's already initialized.
            initRequest.callback_.onInitFinished(null, new BranchError("Warning.", BranchError.ERR_BRANCH_ALREADY_INITIALIZED));
        }
    }
    
    /**
     * Registers app init with params filtered from the intent. Unless ignoreIntent = true, this
     * will wait on the wait locks to complete any pending operations
     */
     void registerAppInit(@NonNull ServerRequestInitSession request, boolean ignoreWaitLocks) {
        setInitState(SESSION_STATE.INITIALISING);

        if (!ignoreWaitLocks) {
            // Single top activities can be launched from stack and there may be a new intent provided with onNewIntent() call.
            // In this case need to wait till onResume to get the latest intent. Bypass this if bypassWaitingForIntent_ is true.
            if (intentState_ != INTENT_STATE.READY  && isWaitingForIntent()) {
                request.addProcessWaitLock(ServerRequest.PROCESS_WAIT_LOCK.INTENT_PENDING_WAIT_LOCK);
            }

            // Google Play Referrer lib should only be used once, so we use GooglePlayStoreAttribution.hasBeenUsed flag
            // just in case user accidentally queues up a couple install requests at the same time. During later sessions
            // request instanceof ServerRequestRegisterInstall = false
            if (checkInstallReferrer_ && request instanceof ServerRequestRegisterInstall) {

                // We may need to check if play store services exist, in the future
                // Obtain all needed locks before executing any fetches
                if(!StoreReferrerGooglePlayStore.hasBeenUsed) {
                    waitingForGoogleInstallReferrer = true;
                    request.addProcessWaitLock(ServerRequest.PROCESS_WAIT_LOCK.GOOGLE_INSTALL_REFERRER_FETCH_WAIT_LOCK);
                }

                if (classExists("com.huawei.hms.ads.installreferrer.api.InstallReferrerClient")
                && !StoreReferrerHuaweiAppGallery.hasBeenUsed) {
                    waitingForHuaweiInstallReferrer = true;
                    request.addProcessWaitLock(ServerRequest.PROCESS_WAIT_LOCK.HUAWEI_INSTALL_REFERRER_FETCH_WAIT_LOCK);
                }

                if (classExists("com.sec.android.app.samsungapps.installreferrer.api.InstallReferrerClient")
                && !StoreReferrerSamsungGalaxyStore.hasBeenUsed) {
                    waitingForSamsungInstallReferrer = true;
                    request.addProcessWaitLock(ServerRequest.PROCESS_WAIT_LOCK.SAMSUNG_INSTALL_REFERRER_FETCH_WAIT_LOCK);
                }

                if (classExists("com.miui.referrer.api.GetAppsReferrerClient")
                && !StoreReferrerXiaomiGetApps.hasBeenUsed) {
                    waitingForXiaomiInstallReferrer = true;
                    request.addProcessWaitLock(ServerRequest.PROCESS_WAIT_LOCK.XIAOMI_INSTALL_REFERRER_FETCH_WAIT_LOCK);
                }

                if(waitingForGoogleInstallReferrer){
                    StoreReferrerGooglePlayStore.fetch(context_, this);
                }

                if(waitingForHuaweiInstallReferrer){
                    StoreReferrerHuaweiAppGallery.fetch(context_, this);
                }

                if(waitingForSamsungInstallReferrer){
                    StoreReferrerSamsungGalaxyStore.fetch(context_, this);
                }

                if(waitingForXiaomiInstallReferrer){
                    StoreReferrerXiaomiGetApps.fetch(context_, this);
                }

                // StoreReferrer error are thrown synchronously, so we remove
                // *_INSTALL_REFERRER_FETCH_WAIT_LOCK manually
                if (StoreReferrerGooglePlayStore.erroredOut) {
                    request.removeProcessWaitLock(ServerRequest.PROCESS_WAIT_LOCK.GOOGLE_INSTALL_REFERRER_FETCH_WAIT_LOCK);
                }
                if (StoreReferrerHuaweiAppGallery.erroredOut) {
                    request.removeProcessWaitLock(ServerRequest.PROCESS_WAIT_LOCK.HUAWEI_INSTALL_REFERRER_FETCH_WAIT_LOCK);
                }
                if (StoreReferrerSamsungGalaxyStore.erroredOut) {
                    request.removeProcessWaitLock(ServerRequest.PROCESS_WAIT_LOCK.SAMSUNG_INSTALL_REFERRER_FETCH_WAIT_LOCK);
                }
                if (StoreReferrerXiaomiGetApps.erroredOut) {
                    request.removeProcessWaitLock(ServerRequest.PROCESS_WAIT_LOCK.XIAOMI_INSTALL_REFERRER_FETCH_WAIT_LOCK);
                }
            }
        }

        if (isGAParamsFetchInProgress_) {
            request.addProcessWaitLock(ServerRequest.PROCESS_WAIT_LOCK.GAID_FETCH_WAIT_LOCK);
        }

        ServerRequestInitSession r = requestQueue_.getSelfInitRequest();
        if (r == null) {
            insertRequestAtFront(request);
            processNextQueueItem();
        } else {
            r.callback_ = request.callback_;
        }
    }

    private boolean classExists(String className) {
        try  {
            Class.forName(className);
            return true;
        }  catch (ClassNotFoundException e) {
            PrefHelper.Debug("Could not find " + className + ". If expected, import the dependency into your app.");
            return false;
        }
    }
    
    ServerRequestInitSession getInstallOrOpenRequest(BranchReferralInitListener callback, boolean isAutoInitialization) {
        ServerRequestInitSession request;
        if (hasUser()) {
            // If there is user this is open
            request = new ServerRequestRegisterOpen(context_, callback, isAutoInitialization);
        } else {
            // If no user this is an Install
            request = new ServerRequestRegisterInstall(context_, callback, isAutoInitialization);
        }
        return request;
    }
    
    void onIntentReady(@NonNull Activity activity) {
        setIntentState(Branch.INTENT_STATE.READY);
        requestQueue_.unlockProcessWait(ServerRequest.PROCESS_WAIT_LOCK.INTENT_PENDING_WAIT_LOCK);

        boolean grabIntentParams = activity.getIntent() != null && getInitState() != Branch.SESSION_STATE.INITIALISED;

        if (grabIntentParams) {
            Uri intentData = activity.getIntent().getData();
            readAndStripParam(intentData, activity);
            // Check for cookie based matching only if Tracking is enabled
            if (!isTrackingDisabled() && cookieBasedMatchDomain_ != null &&
                    prefHelper_.getBranchKey() != null &&
                    !prefHelper_.getBranchKey().equalsIgnoreCase(PrefHelper.NO_STRING_VALUE)) {
                if (isGAParamsFetchInProgress_) {
                    // Wait for GAID to Available
                    performCookieBasedStrongMatchingOnGAIDAvailable = true;
                } else {
                    performCookieBasedStrongMatch();
                }
            }
        }
        processNextQueueItem();
    }

    private void performCookieBasedStrongMatch() {
        if (!trackingController.isTrackingDisabled()) {
            if (context_ != null) {
                requestQueue_.setStrongMatchWaitLock();
                BranchStrongMatchHelper.getInstance().checkForStrongMatch(context_, cookieBasedMatchDomain_,
                        deviceInfo_, prefHelper_, new BranchStrongMatchHelper.StrongMatchCheckEvents() {
                    @Override
                    public void onStrongMatchCheckFinished() {
                        requestQueue_.unlockProcessWait(ServerRequest.PROCESS_WAIT_LOCK.STRONG_MATCH_PENDING_WAIT_LOCK);
                        processNextQueueItem();
                    }
                });
            }
        }
    }
    
    /**
     * Handles execution of a new request other than open or install.
     * Checks for the session initialisation and adds a install/Open request in front of this request
     * if the request need session to execute.
     *
     * @param req The {@link ServerRequest} to execute
     */
    public void handleNewRequest(ServerRequest req) {
        // If Tracking is disabled fail all messages with ERR_BRANCH_TRACKING_DISABLED
        if (trackingController.isTrackingDisabled() && !req.prepareExecuteWithoutTracking()) {
            PrefHelper.Debug("Requested operation cannot be completed since tracking is disabled [" + req.requestPath_.getPath() + "]");
            req.handleFailure(BranchError.ERR_BRANCH_TRACKING_DISABLED, "");
            return;
        }
        //If not initialised put an open or install request in front of this request(only if this needs session)
        if (initState_ != SESSION_STATE.INITIALISED && !(req instanceof ServerRequestInitSession)) {
            if ((req instanceof ServerRequestLogout)) {
                req.handleFailure(BranchError.ERR_NO_SESSION, "");
                PrefHelper.Debug("Branch is not initialized, cannot logout");
                return;
            }
            if ((req instanceof ServerRequestRegisterClose)) {
                PrefHelper.Debug("Branch is not initialized, cannot close session");
                return;
            }
            if (requestNeedsSession(req)) {
                req.addProcessWaitLock(ServerRequest.PROCESS_WAIT_LOCK.SDK_INIT_WAIT_LOCK);
            }
        }

        requestQueue_.enqueue(req);
        req.onRequestQueued();

        processNextQueueItem();
    }

    /**
     * Notify Branch when network is available in order to process the next request in the queue.
     */
    public void notifyNetworkAvailable() {
        processNextQueueItem();
    }

    private void setActivityLifeCycleObserver(Application application) {
        try {
            activityLifeCycleObserver = new BranchActivityLifecycleObserver();
            /* Set an observer for activity life cycle events. */
            application.unregisterActivityLifecycleCallbacks(activityLifeCycleObserver);
            application.registerActivityLifecycleCallbacks(activityLifeCycleObserver);
            isActivityLifeCycleCallbackRegistered_ = true;
            
        } catch (NoSuchMethodError | NoClassDefFoundError Ex) {
            isActivityLifeCycleCallbackRegistered_ = false;
            /* LifeCycleEvents are  available only from API level 14. */
            PrefHelper.Debug(new BranchError("", BranchError.ERR_API_LVL_14_NEEDED).getMessage());
        }
    }

    /*
     * Check for forced session restart. The Branch session is restarted if the incoming intent has branch_force_new_session set to true.
     * This is for supporting opening a deep link path while app is already running in the foreground. Such as clicking push notification while app (namely, LauncherActivity) is in foreground.
     */
    boolean isRestartSessionRequested(Intent intent) {
        return checkIntentForSessionRestart(intent) || checkIntentForUnusedBranchLink(intent);
    }

    private boolean checkIntentForSessionRestart(Intent intent) {
        boolean forceSessionIntentKeyPresent = false;
        if (intent != null) {
            forceSessionIntentKeyPresent = intent.getBooleanExtra(Defines.IntentKeys.ForceNewBranchSession.getKey(), false);
        }
        return forceSessionIntentKeyPresent;
    }

    private boolean checkIntentForUnusedBranchLink(Intent intent) {
        boolean hasUnusedBranchLink = false;
        if (intent != null) {
            boolean hasBranchLink = intent.getStringExtra(Defines.IntentKeys.BranchURI.getKey()) != null;
            boolean branchLinkNotConsumedYet = !intent.getBooleanExtra(Defines.IntentKeys.BranchLinkUsed.getKey(), false);
            hasUnusedBranchLink = hasBranchLink && branchLinkNotConsumedYet;
        }
        return hasUnusedBranchLink;
    }
    
    /**
     * <p>An Interface class that is implemented by all classes that make use of
     * {@link BranchReferralInitListener}, defining a single method that takes a list of params in
     * {@link JSONObject} format, and an error message of {@link BranchError} format that will be
     * returned on failure of the request response.</p>
     *
     * @see JSONObject
     * @see BranchError
     */
    public interface BranchReferralInitListener {
        void onInitFinished(@Nullable JSONObject referringParams, @Nullable BranchError error);
    }
    
    /**
     * <p>An Interface class that is implemented by all classes that make use of
     * {@link BranchUniversalReferralInitListener}, defining a single method that provides
     * {@link BranchUniversalObject}, {@link LinkProperties} and an error message of {@link BranchError} format that will be
     * returned on failure of the request response.
     * In case of an error the value for {@link BranchUniversalObject} and {@link LinkProperties} are set to null.</p>
     *
     * @see BranchUniversalObject
     * @see LinkProperties
     * @see BranchError
     */
    public interface BranchUniversalReferralInitListener {
        void onInitFinished(@Nullable BranchUniversalObject branchUniversalObject, @Nullable LinkProperties linkProperties, @Nullable BranchError error);
    }
    
    /**
     * <p>An Interface class that is implemented by all classes that make use of
     * {@link BranchLinkCreateListener}, defining a single method that takes a URL
     * {@link String} format, and an error message of {@link BranchError} format that will be
     * returned on failure of the request response.</p>
     *
     * @see String
     * @see BranchError
     */
    public interface BranchLinkCreateListener {
        void onLinkCreate(String url, BranchError error);
    }
    
    /**
     * <p>An Interface class that is implemented by all classes that make use of
     * {@link BranchLinkShareListener}, defining methods to listen for link sharing status.</p>
     */
    public interface BranchLinkShareListener {
        /**
         * <p> Callback method to update when share link dialog is launched.</p>
         */
        void onShareLinkDialogLaunched();
        
        /**
         * <p> Callback method to update when sharing dialog is dismissed.</p>
         */
        void onShareLinkDialogDismissed();
        
        /**
         * <p> Callback method to update the sharing status. Called on sharing completed or on error.</p>
         *
         * @param sharedLink    The link shared to the channel.
         * @param sharedChannel Channel selected for sharing.
         * @param error         A {@link BranchError} to update errors, if there is any.
         */
        void onLinkShareResponse(String sharedLink, String sharedChannel, BranchError error);
        
        /**
         * <p>Called when user select a channel for sharing a deep link.
         * Branch will create a deep link for the selected channel and share with it after calling this
         * method. On sharing complete, status is updated by onLinkShareResponse() callback. Consider
         * having a sharing in progress UI if you wish to prevent user activity in the window between selecting a channel
         * and sharing complete.</p>
         *
         * @param channelName Name of the selected application to share the link. An empty string is returned if unable to resolve selected client name.
         */
        void onChannelSelected(String channelName);
    }
    
    /**
     * <p>An extended version of {@link BranchLinkShareListener} with callback that supports updating link data or properties after user select a channel to share
     * This will provide the extended callback {@link #onChannelSelected(String, BranchUniversalObject, LinkProperties)} only when sharing a link using Branch Universal Object.</p>
     */
    public interface ExtendedBranchLinkShareListener extends BranchLinkShareListener {
        /**
         * <p>
         * Called when user select a channel for sharing a deep link.
         * This method allows modifying the link data and properties by providing the params  {@link BranchUniversalObject} and {@link LinkProperties}
         * </p>
         *
         * @param channelName    The name of the channel user selected for sharing a link
         * @param buo            {@link BranchUniversalObject} BUO used for sharing link for updating any params
         * @param linkProperties {@link LinkProperties} associated with the sharing link for updating the properties
         * @return Return {@code true} to create link with any updates added to the data ({@link BranchUniversalObject}) or to the properties ({@link LinkProperties}).
         * Return {@code false} otherwise.
         */
        boolean onChannelSelected(String channelName, BranchUniversalObject buo, LinkProperties linkProperties);
    }
    
    /**
     * <p>An interface class for customizing sharing properties with selected channel.</p>
     */
    public interface IChannelProperties {
        /**
         * @param channel The name of the channel selected for sharing.
         * @return {@link String} with value for the message title for sharing the link with the selected channel
         */
        String getSharingTitleForChannel(String channel);
        
        /**
         * @param channel The name of the channel selected for sharing.
         * @return {@link String} with value for the message body for sharing the link with the selected channel
         */
        String getSharingMessageForChannel(String channel);
    }
    
    /**
     * <p>An Interface class that is implemented by all classes that make use of
     * {@link BranchListResponseListener}, defining a single method that takes a list of
     * {@link JSONArray} format, and an error message of {@link BranchError} format that will be
     * returned on failure of the request response.</p>
     *
     * @see JSONArray
     * @see BranchError
     */
    public interface BranchListResponseListener {
        void onReceivingResponse(JSONArray list, BranchError error);
    }
    
    /**
     * <p>
     * Callback interface for listening logout status
     * </p>
     */
    public interface LogoutStatusListener {
        /**
         * Called on finishing the the logout process
         *
         * @param loggedOut A {@link Boolean} which is set to true if logout succeeded
         * @param error     An instance of {@link BranchError} to notify any error occurred during logout.
         *                  A null value is set if logout succeeded.
         */
        void onLogoutFinished(boolean loggedOut, BranchError error);
    }
    
    /**
     * Async Task to create  a short link for synchronous methods
     */
    private class GetShortLinkTask extends AsyncTask<ServerRequest, Void, ServerResponse> {
        @Override protected ServerResponse doInBackground(ServerRequest... serverRequests) {
            return branchRemoteInterface_.make_restful_post(serverRequests[0].getPost(),
                    prefHelper_.getAPIBaseUrl() + Defines.RequestPath.GetURL.getPath(),
                    Defines.RequestPath.GetURL.getPath(), prefHelper_.getBranchKey());
        }
    }

    //-------------------Auto deep link feature-------------------------------------------//
    
    /**
     * <p>Checks if an activity is launched by Branch auto deep link feature. Branch launches activity configured for auto deep link on seeing matching keys.
     * Keys for auto deep linking should be specified to each activity as a meta data in manifest.</p>
     * Configure your activity in your manifest to enable auto deep linking as follows
     * <!--
     * <activity android:name=".YourActivity">
     * <meta-data android:name="io.branch.sdk.auto_link" android:value="DeepLinkKey1","DeepLinkKey2" />
     * </activity>
     * -->
     *
     * @param activity Instance of activity to check if launched on auto deep link.
     * @return A {Boolean} value whose value is true if this activity is launched by Branch auto deeplink feature.
     */
    public static boolean isAutoDeepLinkLaunch(Activity activity) {
        return (activity.getIntent().getStringExtra(Defines.IntentKeys.AutoDeepLinked.getKey()) != null);
    }
    
    void checkForAutoDeepLinkConfiguration() {
        JSONObject latestParams = getLatestReferringParams();
        String deepLinkActivity = null;
        
        try {
            //Check if the application is launched by clicking a Branch link.
            if (!latestParams.has(Defines.Jsonkey.Clicked_Branch_Link.getKey())
                    || !latestParams.getBoolean(Defines.Jsonkey.Clicked_Branch_Link.getKey())) {
                return;
            }
            if (latestParams.length() > 0) {
                // Check if auto deep link is disabled.
                ApplicationInfo appInfo = context_.getPackageManager().getApplicationInfo(context_.getPackageName(), PackageManager.GET_META_DATA);
                if (appInfo.metaData != null && appInfo.metaData.getBoolean(AUTO_DEEP_LINK_DISABLE, false)) {
                    return;
                }
                PackageInfo info = context_.getPackageManager().getPackageInfo(context_.getPackageName(), PackageManager.GET_ACTIVITIES | PackageManager.GET_META_DATA);
                ActivityInfo[] activityInfos = info.activities;
                int deepLinkActivityReqCode = DEF_AUTO_DEEP_LINK_REQ_CODE;
                
                if (activityInfos != null) {
                    for (ActivityInfo activityInfo : activityInfos) {
                        if (activityInfo != null && activityInfo.metaData != null && (activityInfo.metaData.getString(AUTO_DEEP_LINK_KEY) != null || activityInfo.metaData.getString(AUTO_DEEP_LINK_PATH) != null)) {
                            if (checkForAutoDeepLinkKeys(latestParams, activityInfo) || checkForAutoDeepLinkPath(latestParams, activityInfo)) {
                                deepLinkActivity = activityInfo.name;
                                deepLinkActivityReqCode = activityInfo.metaData.getInt(AUTO_DEEP_LINK_REQ_CODE, DEF_AUTO_DEEP_LINK_REQ_CODE);
                                break;
                            }
                        }
                    }
                }
                if (deepLinkActivity != null && getCurrentActivity() != null) {
                    Activity currentActivity = getCurrentActivity();

                    Intent intent = new Intent(currentActivity, Class.forName(deepLinkActivity));
                    intent.putExtra(Defines.IntentKeys.AutoDeepLinked.getKey(), "true");

                    // Put the raw JSON params as extra in case need to get the deep link params as JSON String
                    intent.putExtra(Defines.Jsonkey.ReferringData.getKey(), latestParams.toString());

                    // Add individual parameters in the data
                    Iterator<?> keys = latestParams.keys();
                    while (keys.hasNext()) {
                        String key = (String) keys.next();
                        intent.putExtra(key, latestParams.getString(key));
                    }
                    currentActivity.startActivityForResult(intent, deepLinkActivityReqCode);
                } else {
                    // This case should not happen. Adding a safe handling for any corner case
                    PrefHelper.Debug("No activity reference to launch deep linked activity");
                }
            }
        } catch (final PackageManager.NameNotFoundException e) {
            PrefHelper.Debug("Warning: Please make sure Activity names set for auto deep link are correct!");
        } catch (ClassNotFoundException e) {
            PrefHelper.Debug("Warning: Please make sure Activity names set for auto deep link are correct! Error while looking for activity " + deepLinkActivity);
        } catch (Exception ignore) {
            // Can get TransactionTooLarge Exception here if the Application info exceeds 1mb binder data limit. Usually results with manifest merge from SDKs
        }
    }
    
    private boolean checkForAutoDeepLinkKeys(JSONObject params, ActivityInfo activityInfo) {
        if (activityInfo.metaData.getString(AUTO_DEEP_LINK_KEY) != null) {
            String[] activityLinkKeys = activityInfo.metaData.getString(AUTO_DEEP_LINK_KEY).split(",");
            for (String activityLinkKey : activityLinkKeys) {
                if (params.has(activityLinkKey)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    private boolean checkForAutoDeepLinkPath(JSONObject params, ActivityInfo activityInfo) {
        String deepLinkPath = null;
        try {
            if (params.has(Defines.Jsonkey.AndroidDeepLinkPath.getKey())) {
                deepLinkPath = params.getString(Defines.Jsonkey.AndroidDeepLinkPath.getKey());
            } else if (params.has(Defines.Jsonkey.DeepLinkPath.getKey())) {
                deepLinkPath = params.getString(Defines.Jsonkey.DeepLinkPath.getKey());
            }
        } catch (JSONException ignored) {
        }
        if (activityInfo.metaData.getString(AUTO_DEEP_LINK_PATH) != null && deepLinkPath != null) {
            String[] activityLinkPaths = activityInfo.metaData.getString(AUTO_DEEP_LINK_PATH).split(",");
            for (String activityLinkPath : activityLinkPaths) {
                if (pathMatch(activityLinkPath.trim(), deepLinkPath)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    private boolean pathMatch(String templatePath, String path) {
        boolean matched = true;
        String[] pathSegmentsTemplate = templatePath.split("\\?")[0].split("/");
        String[] pathSegmentsTarget = path.split("\\?")[0].split("/");
        if (pathSegmentsTemplate.length != pathSegmentsTarget.length) {
            return false;
        }
        for (int i = 0; i < pathSegmentsTemplate.length && i < pathSegmentsTarget.length; i++) {
            String pathSegmentTemplate = pathSegmentsTemplate[i];
            String pathSegmentTarget = pathSegmentsTarget[i];
            if (!pathSegmentTemplate.equals(pathSegmentTarget) && !pathSegmentTemplate.contains("*")) {
                matched = false;
                break;
            }
        }
        return matched;
    }

    /**
     * Enable Logging, independent of Debug Mode.
     */
    public static void enableLogging() {
        PrefHelper.LogAlways(GOOGLE_VERSION_TAG);
        PrefHelper.enableLogging(true);
    }

    /**
     * Disable Logging, independent of Debug Mode.
     */
    public static void disableLogging() {
        PrefHelper.enableLogging(false);
    }

    /**
     * <p> Use this method cautiously, it is meant to enable the ability to start a session before
     * the user opens the app.
     *
     * The use case explained:
     * Users are expected to initialize session from Activity.onStart. However, by default, Branch actually
     * waits until Activity.onResume to start session initialization, so as to ensure that the latest intent
     * data is available (e.g. when activity is launched from stack via onNewIntent). Setting this flag to true
     * will bypass waiting for intent, so session could technically be initialized from a background service
     * or otherwise before the application is even opened.
     *
     * Note however that if the flag is not reset during normal app boot up, the SDK behavior is undefined
     * in certain cases.</p>
     *
     * @param bypassIntent a {@link Boolean} indicating if SDK should wait for onResume in order to fire the
     *                     session initialization request.
     */
    @SuppressWarnings("WeakerAccess")
    public static void bypassWaitingForIntent(boolean bypassIntent) { bypassWaitingForIntent_ = bypassIntent; }

    /**
     * Returns true if session initialization should bypass waiting for intent (retrieved after onResume).
     *
     * @return {@link Boolean} with value true to enable forced session
     */
    @SuppressWarnings("WeakerAccess")
    public static boolean isWaitingForIntent() { return !bypassWaitingForIntent_; }
    
    public static void enableBypassCurrentActivityIntentState() {
        bypassCurrentActivityIntentState_ = true;
    }

    @SuppressWarnings("WeakerAccess")
    public static boolean bypassCurrentActivityIntentState() {
        return bypassCurrentActivityIntentState_;
    }
    
    //------------------------ Content Indexing methods----------------------//
    
    public void registerView(BranchUniversalObject branchUniversalObject,
                             BranchUniversalObject.RegisterViewStatusListener callback) {
        if (context_ != null) {
            new BranchEvent(BRANCH_STANDARD_EVENT.VIEW_ITEM)
                    .addContentItems(branchUniversalObject)
                    .logEvent(context_);
        }
    }
    
    ///-------Instrumentation additional data---------------///
    
    /**
     * Update the extra instrumentation data provided to Branch
     *
     * @param instrumentationData A {@link HashMap} with key value pairs for instrumentation data.
     */
    public void addExtraInstrumentationData(HashMap<String, String> instrumentationData) {
        instrumentationExtraData_.putAll(instrumentationData);
    }
    
    /**
     * Update the extra instrumentation data provided to Branch
     *
     * @param key   A {@link String} Value for instrumentation data key
     * @param value A {@link String} Value for instrumentation data value
     */
    public void addExtraInstrumentationData(String key, String value) {
        instrumentationExtraData_.put(key, value);
    }
    
    
    //-------------------- Branch view handling--------------------//
    
    
    @Override
    public void onBranchViewVisible(String action, String branchViewID) {
        //No Implementation on purpose
    }
    
    @Override
    public void onBranchViewAccepted(String action, String branchViewID) {
        if (ServerRequestInitSession.isInitSessionAction(action)) {
            checkForAutoDeepLinkConfiguration();
        }
    }
    
    @Override
    public void onBranchViewCancelled(String action, String branchViewID) {
        if (ServerRequestInitSession.isInitSessionAction(action)) {
            checkForAutoDeepLinkConfiguration();
        }
    }
    
    @Override
    public void onBranchViewError(int errorCode, String errorMsg, String action) {
        if (ServerRequestInitSession.isInitSessionAction(action)) {
            checkForAutoDeepLinkConfiguration();
        }
    }
    
    /**
     * Interface for defining optional Branch view behaviour for Activities
     */
    public interface IBranchViewControl {
        /**
         * Defines if an activity is interested to show Branch views or not.
         * By default activities are considered as Branch view enabled. In case of activities which are not interested to show a Branch view (Splash screen for example)
         * should implement this and return false. The pending Branch view will be shown with the very next Branch view enabled activity
         *
         * @return A {@link Boolean} whose value is true if the activity don't want to show any Branch view.
         */
        boolean skipBranchViewsOnThisActivity();
    }
    
    
    ///----------------- Instant App  support--------------------------//
    
    /**
     * Checks if this is an Instant app instance
     *
     * @param context Current {@link Context}
     * @return {@code true}  if current application is an instance of instant app
     */
    public static boolean isInstantApp(@NonNull Context context) {
        return InstantAppUtil.isInstantApp(context);
    }
    
    /**
     * Method shows play store install prompt for the full app. Thi passes the referrer to the installed application. The same deep link params as the instant app are provided to the
     * full app up on Branch#initSession()
     *
     * @param activity    Current activity
     * @param requestCode Request code for the activity to receive the result
     * @return {@code true} if install prompt is shown to user
     */
    public static boolean showInstallPrompt(@NonNull Activity activity, int requestCode) {
        String installReferrerString = "";
        if (Branch.getInstance() != null) {
            JSONObject latestReferringParams = Branch.getInstance().getLatestReferringParams();
            String referringLinkKey = "~" + Defines.Jsonkey.ReferringLink.getKey();
            if (latestReferringParams != null && latestReferringParams.has(referringLinkKey)) {
                String referringLink = "";
                try {
                    referringLink = latestReferringParams.getString(referringLinkKey);
                    // Considering the case that url may contain query params with `=` and `&` with it and may cause issue when parsing play store referrer
                    referringLink = URLEncoder.encode(referringLink, "UTF-8");
                } catch (JSONException | UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                if (!TextUtils.isEmpty(referringLink)) {
                    installReferrerString = Defines.Jsonkey.IsFullAppConv.getKey() + "=true&" + Defines.Jsonkey.ReferringLink.getKey() + "=" + referringLink;
                }
            }
        }
        return InstantAppUtil.doShowInstallPrompt(activity, requestCode, installReferrerString);
    }
    
    /**
     * Method shows play store install prompt for the full app. Use this method only if you have custom parameters to pass to the full app using referrer else use
     * {@link #showInstallPrompt(Activity, int)}
     *
     * @param activity    Current activity
     * @param requestCode Request code for the activity to receive the result
     * @param referrer    Any custom referrer string to pass to full app (must be of format "referrer_key1=referrer_value1%26referrer_key2=referrer_value2")
     * @return {@code true} if install prompt is shown to user
     */
    public static boolean showInstallPrompt(@NonNull Activity activity, int requestCode, @Nullable String referrer) {
        String installReferrerString = Defines.Jsonkey.IsFullAppConv.getKey() + "=true&" + referrer;
        return InstantAppUtil.doShowInstallPrompt(activity, requestCode, installReferrerString);
    }
    
    /**
     * Method shows play store install prompt for the full app. Use this method only if you want the full app to receive a custom {@link BranchUniversalObject} to do deferred deep link.
     * Please see {@link #showInstallPrompt(Activity, int)}
     * NOTE :
     * This method will do a synchronous generation of Branch short link for the BUO. So please consider calling this method on non UI thread
     * Please make sure your instant app and full ap are using same Branch key in order for the deferred deep link working
     *
     * @param activity    Current activity
     * @param requestCode Request code for the activity to receive the result
     * @param buo         {@link BranchUniversalObject} to pass to the full app up on install
     * @return {@code true} if install prompt is shown to user
     */
    public static boolean showInstallPrompt(@NonNull Activity activity, int requestCode, @NonNull BranchUniversalObject buo) {
        String shortUrl = buo.getShortUrl(activity, new LinkProperties());
        String installReferrerString = Defines.Jsonkey.ReferringLink.getKey() + "=" + shortUrl;
        if (!TextUtils.isEmpty(installReferrerString)) {
            return showInstallPrompt(activity, requestCode, installReferrerString);
        } else {
            return showInstallPrompt(activity, requestCode, "");
        }
    }

    private void extractSessionParamsForIDL(Uri data, Activity activity) {
        if (activity == null || activity.getIntent() == null) return;

        Intent intent = activity.getIntent();
        try {
            if (data == null || isIntentParamsAlreadyConsumed(activity)) {
                // Considering the case of a deferred install. In this case the app behaves like a cold
                // start but still Branch can do probabilistic match. So skipping instant deep link feature
                // until first Branch open happens.
                if (!prefHelper_.getInstallParams().equals(PrefHelper.NO_STRING_VALUE)) {
                    JSONObject nonLinkClickJson = new JSONObject();
                    nonLinkClickJson.put(Defines.Jsonkey.IsFirstSession.getKey(), false);
                    prefHelper_.setSessionParams(nonLinkClickJson.toString());
                    isInstantDeepLinkPossible = true;
                }
            } else if (!TextUtils.isEmpty(intent.getStringExtra(Defines.IntentKeys.BranchData.getKey()))) {
                // If not cold start, check the intent data to see if there are deep link params
                String rawBranchData = intent.getStringExtra(Defines.IntentKeys.BranchData.getKey());
                if (rawBranchData != null) {
                    // Make sure the data received is complete and in correct format
                    JSONObject branchDataJson = new JSONObject(rawBranchData);
                    branchDataJson.put(Defines.Jsonkey.Clicked_Branch_Link.getKey(), true);
                    prefHelper_.setSessionParams(branchDataJson.toString());
                    isInstantDeepLinkPossible = true;
                }

                // Remove Branch data from the intent once used
                intent.removeExtra(Defines.IntentKeys.BranchData.getKey());
                activity.setIntent(intent);
            } else if (data.isHierarchical() && Boolean.valueOf(data.getQueryParameter(Defines.Jsonkey.Instant.getKey()))) {
                // If instant key is true in query params, use them for instant deep linking
                JSONObject branchDataJson = new JSONObject();
                for (String key : data.getQueryParameterNames()) {
                    branchDataJson.put(key, data.getQueryParameter(key));
                }
                branchDataJson.put(Defines.Jsonkey.Clicked_Branch_Link.getKey(), true);
                prefHelper_.setSessionParams(branchDataJson.toString());
                isInstantDeepLinkPossible = true;
            }
        } catch (JSONException ignored) {}
    }

    private void extractAppLink(Uri data, Activity activity) {
        if (data == null || activity == null) return;

        String scheme = data.getScheme();
        Intent intent = activity.getIntent();
        if (scheme != null && intent != null &&
                (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https")) &&
                !TextUtils.isEmpty(data.getHost()) &&
                !isIntentParamsAlreadyConsumed(activity)) {

            String strippedUrl = UniversalResourceAnalyser.getInstance(context_).getStrippedURL(data.toString());

            if (data.toString().equalsIgnoreCase(strippedUrl)) {
                // Send app links only if URL is not skipped.
                prefHelper_.setAppLink(data.toString());
            }
            intent.putExtra(Defines.IntentKeys.BranchLinkUsed.getKey(), true);
            activity.setIntent(intent);
        }
    }

    private boolean extractClickID(Uri data, Activity activity) {
        try {
            if (data == null || !data.isHierarchical()) return false;

            String linkClickID = data.getQueryParameter(Defines.Jsonkey.LinkClickID.getKey());
            if (linkClickID == null) return false;

            prefHelper_.setLinkClickIdentifier(linkClickID);
            String paramString = "link_click_id=" + linkClickID;
            String uriString = data.toString();

            if (paramString.equals(data.getQuery())) {
                paramString = "\\?" + paramString;
            } else if ((uriString.length() - paramString.length()) == uriString.indexOf(paramString)) {
                paramString = "&" + paramString;
            } else {
                paramString = paramString + "&";
            }

            Uri uriWithoutClickID = Uri.parse(uriString.replaceFirst(paramString, ""));
            activity.getIntent().setData(uriWithoutClickID);
            activity.getIntent().putExtra(Defines.IntentKeys.BranchLinkUsed.getKey(), true);
            return true;
        } catch (Exception ignore) {
            return false;
        }
    }

    private boolean extractBranchLinkFromIntentExtra(Activity activity) {
        //Check for any push identifier in case app is launched by a push notification
        try {
            if (activity != null && activity.getIntent() != null && activity.getIntent().getExtras() != null) {
                if (!isIntentParamsAlreadyConsumed(activity)) {
                    Object object = activity.getIntent().getExtras().get(Defines.IntentKeys.BranchURI.getKey());
                    String branchLink = null;

                    if (object instanceof String) {
                        branchLink = (String) object;
                    } else if (object instanceof Uri) {
                        Uri uri = (Uri) object;
                        branchLink = uri.toString();
                    }

                    if (!TextUtils.isEmpty(branchLink)) {
                        prefHelper_.setPushIdentifier(branchLink);
                        Intent thisIntent = activity.getIntent();
                        thisIntent.putExtra(Defines.IntentKeys.BranchLinkUsed.getKey(), true);
                        activity.setIntent(thisIntent);
                        return true;
                    }
                }
            }
        } catch (Exception ignore) {
        }
        return false;
    }

    private void extractExternalUriAndIntentExtras(Uri data, Activity activity) {
        try {
            if (!isIntentParamsAlreadyConsumed(activity)) {
                String strippedUrl = UniversalResourceAnalyser.getInstance(context_).getStrippedURL(data.toString());
                prefHelper_.setExternalIntentUri(strippedUrl);

                if (strippedUrl.equals(data.toString())) {
                    Bundle bundle = activity.getIntent().getExtras();
                    Set<String> extraKeys = bundle.keySet();
                    if (extraKeys.isEmpty()) return;

                    JSONObject extrasJson = new JSONObject();
                    for (String key : EXTERNAL_INTENT_EXTRA_KEY_WHITE_LIST) {
                        if (extraKeys.contains(key)) {
                            extrasJson.put(key, bundle.get(key));
                        }
                    }
                    if (extrasJson.length() > 0) {
                        prefHelper_.setExternalIntentExtra(extrasJson.toString());
                    }
                }
            }
        } catch (Exception ignore) {
        }
    }

    @Nullable Activity getCurrentActivity() {
        if (currentActivityReference_ == null) return null;
        return currentActivityReference_.get();
    }

    boolean isIDLSession() {
        return Boolean.parseBoolean(instrumentationExtraData_.get(Defines.Jsonkey.InstantDeepLinkSession.getKey()));
    }
    /**
     * <p> Create Branch session builder. Add configuration variables with the available methods
     * in the returned {@link InitSessionBuilder} class. Must be finished with init() or reInit(),
     * otherwise takes no effect.</p>
     *
     * @param activity     The calling {@link Activity} for context.
     */
    @SuppressWarnings("WeakerAccess")
    public static InitSessionBuilder sessionBuilder(Activity activity) {
        return new InitSessionBuilder(activity);
    }
    
    /**
     * Method will return the current Branch SDK version number
     * @return String value representing the current SDK version number (e.g. 4.3.2)
     */
    public static String getSdkVersionNumber() {
        return io.branch.referral.BuildConfig.VERSION_NAME;
    }


    /**
     * Scenario: Integrations using our plugin SDKs (React-Native, Capacitor, Unity, etc),
     * it is possible to have a race condition wherein the native layers finish their initialization
     * before the JS/C# layers have finished loaded and registering their receivers- dropping the
     * Branch parameters.
     *
     * Because these plugin delays are not deterministic, or consistent, a constant
     * offset to delay is not guaranteed to work in all cases, and possibly penalizes performant
     * devices.
     *
     * To solve, we wait for the plugin to signal when it is ready, and then begin native init
     *
     * Reusing disable autoinitialization to prevent uninitialization errors
     * @param isDeferred
     */
    static void deferInitForPluginRuntime(boolean isDeferred){
        PrefHelper.Debug("deferInitForPluginRuntime " + isDeferred);

        deferInitForPluginRuntime = isDeferred;
        if(isDeferred){
            expectDelayedSessionInitialization(isDeferred);
        }
    }

    /**
     * Method to be invoked from plugin to initialize the session originally built by the user
     * Only invokes the last session built
     */
    public static void notifyNativeToInit(){
        PrefHelper.Debug("notifyNativeToInit deferredSessionBuilder " + Branch.getInstance().deferredSessionBuilder);

        SESSION_STATE sessionState = Branch.getInstance().getInitState();
        if(sessionState == SESSION_STATE.UNINITIALISED) {
            deferInitForPluginRuntime = false;
            if (Branch.getInstance().deferredSessionBuilder != null) {
                Branch.getInstance().deferredSessionBuilder.init();
            }
        }
        else {
            PrefHelper.Debug("notifyNativeToInit session is not uninitialized. Session state is " + sessionState);
        }
    }
}
