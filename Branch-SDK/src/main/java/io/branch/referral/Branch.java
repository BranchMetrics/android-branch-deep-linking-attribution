package io.branch.referral;

import static io.branch.referral.BranchError.ERR_BRANCH_REQ_TIMED_OUT;
import static io.branch.referral.BranchPreinstall.getPreinstallSystemData;
import static io.branch.referral.BranchUtil.isTestModeEnabled;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;

import android.os.Handler;
import android.text.TextUtils;
import android.view.View;

import com.google.firebase.BuildConfig;

import io.branch.referral.Defines.PreinstallKey;
import io.branch.referral.ServerRequestGetLATD.BranchLastAttributedTouchDataListener;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.net.ssl.HttpsURLConnection;

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
public class Branch implements BranchViewHandler.IBranchViewEvents, SystemObserver.AdsParamsFetchEvents, GooglePlayStoreAttribution.IInstallReferrerEvents {
    
    private static final String BRANCH_LIBRARY_VERSION = "io.branch.sdk.android:library:" + BuildConfig.VERSION_NAME;
    private static final String GOOGLE_VERSION_TAG = "!SDK-VERSION-STRING!" + ":" + BRANCH_LIBRARY_VERSION;

    /**
     * Hard-coded {@link String} that denotes a {@link BranchLinkData#tags}; applies to links that
     * are shared with others directly as a user action, via social media for instance.
     */
    public static final String FEATURE_TAG_SHARE = "share";
    
    /**
     * Hard-coded {@link String} that denotes a 'referral' tag; applies to links that are associated
     * with a referral program, incentivized or not.
     */
    public static final String FEATURE_TAG_REFERRAL = "referral";
    
    /**
     * Hard-coded {@link String} that denotes a 'referral' tag; applies to links that are sent as
     * referral actions by users of an app using an 'invite contacts' feature for instance.
     */
    public static final String FEATURE_TAG_INVITE = "invite";
    
    /**
     * Hard-coded {@link String} that denotes a link that is part of a commercial 'deal' or offer.
     */
    public static final String FEATURE_TAG_DEAL = "deal";
    
    /**
     * Hard-coded {@link String} that denotes a link tagged as a gift action within a service or
     * product.
     */
    public static final String FEATURE_TAG_GIFT = "gift";
    
    /**
     * The code to be passed as part of a deal or gift; retrieved from the Branch object as a
     * tag upon initialisation. Of {@link String} format.
     */
    public static final String REDEEM_CODE = "$redeem_code";
    
    /**
     * <p>Default value of referral bucket; referral buckets contain credits that are used when users
     * are referred to your apps. These can be viewed in the Branch dashboard under Referrals.</p>
     */
    public static final String REFERRAL_BUCKET_DEFAULT = "default";
    
    /**
     * <p>Hard-coded value for referral code type. Referral codes will always result on "credit" actions.
     * Even if they are of 0 value.</p>
     */
    public static final String REFERRAL_CODE_TYPE = "credit";
    
    /**
     * Branch SDK version for the current release of the Branch SDK.
     */
    public static final int REFERRAL_CREATION_SOURCE_SDK = 2;
    
    /**
     * Key value for referral code as a parameter.
     */
    public static final String REFERRAL_CODE = "referral_code";
    
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
     * The redirect URL provided when the link is handled by a Blackberry device.
     */
    public static final String REDIRECT_BLACKBERRY_URL = "$blackberry_url";
    
    /**
     * The redirect URL provided when the link is handled by a Windows Phone device.
     */
    public static final String REDIRECT_WINDOWS_PHONE_URL = "$windows_phone_url";
    
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
     * An {@link Integer} value indicating the user to reward for applying a referral code. In this
     * case, the user applying the referral code receives credit.
     */
    public static final int REFERRAL_CODE_LOCATION_REFERREE = 0;
    
    /**
     * An {@link Integer} value indicating the user to reward for applying a referral code. In this
     * case, the user who created the referral code receives credit.
     */
    public static final int REFERRAL_CODE_LOCATION_REFERRING_USER = 2;
    
    /**
     * An {@link Integer} value indicating the user to reward for applying a referral code. In this
     * case, both the creator and applicant receive credit
     */
    public static final int REFERRAL_CODE_LOCATION_BOTH = 3;
    
    /**
     * An {@link Integer} value indicating the calculation type of the referral code. In this case,
     * the referral code can be applied continually.
     */
    public static final int REFERRAL_CODE_AWARD_UNLIMITED = 1;
    
    /**
     * An {@link Integer} value indicating the calculation type of the referral code. In this case,
     * a user can only apply a specific referral code once.
     */
    public static final int REFERRAL_CODE_AWARD_UNIQUE = 0;
    
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
    
    /**
     * <p>A {@link Branch} object that is instantiated on init and holds the singleton instance of
     * the class during application runtime.</p>
     */
    private static Branch branchReferral_;
    
    private BranchRemoteInterface branchRemoteInterface_;
    private final PrefHelper prefHelper_;
    private final DeviceInfo deviceInfo_;
    private final Context context_;
    
    private final Semaphore serverSema_ = new Semaphore(1);
    
    private final ServerRequestQueue requestQueue_;
    
    private int networkCount_ = 0;
    
    private boolean hasNetwork_ = true;
    
    private final Map<BranchLinkData, String> linkCache_ = new HashMap<>();
    
    
    /* Set to true when application is instantiating {@BranchApp} by extending or adding manifest entry. */
    private static boolean isAutoSessionMode_ = false;
    
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
    private SESSION_STATE initState_ = SESSION_STATE.UNINITIALISED;

    /* Flag to indicate if the `v1/close` is expected by the server at the end of this session. */
    public boolean closeRequestNeeded = false;

    /* Instance  of share link manager to share links automatically with third party applications. */
    private ShareLinkManager shareLinkManager_;
    
    /* The current activity instance for the application.*/
    WeakReference<Activity> currentActivityReference_;
    
    /* Key to indicate whether the Activity was launched by Branch or not. */
    private static final String AUTO_DEEP_LINKED = "io.branch.sdk.auto_linked";
    
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
    
    private final ConcurrentHashMap<String, String> instrumentationExtraData_ = new ConcurrentHashMap<>();

    /* In order to get Google's advertising ID an AsyncTask is needed, however Fire OS does not require AsyncTask, so isGAParamsFetchInProgress_ would remain false */
    private boolean isGAParamsFetchInProgress_ = false;

    private static String cookieBasedMatchDomain_ = "app.link"; // Domain name used for cookie based matching.
    
    private static final int LATCH_WAIT_UNTIL = 2500; //used for getLatestReferringParamsSync and getFirstReferringParamsSync, fail after this many milliseconds
    
    /* List of keys whose values are collected from the Intent Extra.*/
    private static final String[] EXTERNAL_INTENT_EXTRA_KEY_WHITE_LIST = new String[]{
            "extra_launch_uri",   // Key for embedded uri in FB ads triggered intents
            "branch_intent"       // A boolean that specifies if this intent is originated by Branch
    };
    
    private CountDownLatch getFirstReferringParamsLatch = null;
    private CountDownLatch getLatestReferringParamsLatch = null;
    
    /* Flag for checking of Strong matching is waiting on GAID fetch */
    private boolean performCookieBasedStrongMatchingOnGAIDAvailable = false;
    private boolean isInstantDeepLinkPossible = false;
    private BranchActivityLifecycleObserver activityLifeCycleObserver;
    /* Flag to turn on or off instant deeplinking feature. IDL is disabled by default */
    private static boolean enableInstantDeepLinking = false;
    private final TrackingController trackingController;

    /** Variables for reporting plugin type and version (some TUNE customers do that), plus helps
     * us make data driven decisions. */
    private static String pluginVersion = null;
    private static String pluginName = null;
    
    /**
     * <p>The main constructor of the Branch class is private because the class uses the Singleton
     * pattern.</p>     *
     * <p>Use {@link #getInstance(Context) getInstance} method when instantiating.</p>
     *
     * @param context A {@link Context} from which this call was made.
     */
    private Branch(@NonNull Context context) {
        context_ = context;
        prefHelper_ = PrefHelper.getInstance(context);
        trackingController = new TrackingController(context);
        branchRemoteInterface_ = new BranchRemoteInterfaceUrlConnection(this);
        deviceInfo_ = new DeviceInfo(context);
        requestQueue_ = ServerRequestQueue.getInstance(context);
        if (!trackingController.isTrackingDisabled()) { // Do not get GAID when tracking is disabled
            isGAParamsFetchInProgress_ = deviceInfo_.getSystemObserver().prefetchAdsParams(context,this);
        }
    }


    public Context getApplicationContext() {
        return context_;
    }

    /**
     * Sets a custom Branch Remote interface for handling RESTful requests. Call this for implementing a custom network layer for handling communication between
     * Branch SDK and remote Branch server
     *
     * @param remoteInterface A instance of class extending {@link BranchRemoteInterface} with implementation for abstract RESTful GET or POST methods
     */
    public void setBranchRemoteInterface(BranchRemoteInterface remoteInterface) {
        branchRemoteInterface_ = remoteInterface;
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
     * @deprecated This method is deprecated since INSTALL_REFERRER broadcasts were discontinued on 3/2020.
     * And Branch SDK bundles Play Store Referrer library since v4.2.2
     * Please use {@link #setPlayStoreReferrerCheckTimeout(long)} instead.
     */
    public static void enablePlayStoreReferrer(long delay) {
        setPlayStoreReferrerCheckTimeout(delay);
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
    
    /**
     * <p>Singleton method to return the pre-initialised object of the type {@link Branch}.
     * Make sure your app is instantiating {@link BranchApp} before calling this method
     * or you have created an instance of Branch already by calling getInstance(Context ctx).</p>
     *
     * @return An initialised singleton {@link Branch} object
     */
    public static Branch getInstance() {
        /* Check if BranchApp is instantiated. */
        if (branchReferral_ == null) {
            PrefHelper.Debug("Branch instance is not created yet. Make sure you have initialised Branch. [Consider Calling getInstance(Context ctx) if you still have issue.]");
        } else if (isAutoSessionMode_) {
            /* Check if Activity life cycle callbacks are set if in auto session mode. */
            if (!isActivityLifeCycleCallbackRegistered_) {
                PrefHelper.Debug("Branch instance is not properly initialised. Make sure your Application class is extending BranchApp class. " +
                        "If you are not extending BranchApp class make sure you are initialising Branch in your Applications onCreate()");
            }
        }
        return branchReferral_;
    }
    
    /**
     * <p>Singleton method to return the pre-initialised, or newly initialise and return, a singleton
     * object of the type {@link Branch}.</p>
     *
     * @param context   A {@link Context} from which this call was made.
     * @param branchKey Your Branch key as a {@link String}.
     * @return An initialised {@link Branch} object, either fetched from a pre-initialised
     * instance within the singleton class, or a newly instantiated object where
     * one was not already requested during the current app lifecycle.
     * @see <a href="https://github.com/BranchMetrics/Branch-Android-SDK/blob/05e234855f983ae022633eb01989adb05775532e/README.md#add-your-app-key-to-your-project">
     * Adding your app key to your project</a>
     */
    public static Branch getInstance(@NonNull Context context, @NonNull String branchKey) {
        if (branchReferral_ == null) {
            branchReferral_ = new Branch(context.getApplicationContext());
            if (branchReferral_.prefHelper_.isValidBranchKey(branchKey)) {
                branchReferral_.prefHelper_.setBranchKey(branchKey);
            } else {
                PrefHelper.Debug("Branch Key is invalid. Please check your BranchKey");
            }
        }
        return branchReferral_;
    }
    
    private static Branch getBranchInstance(@NonNull Context context, boolean isLive, String branchKey) {
        if (branchReferral_ == null) {
            branchReferral_ = new Branch(context.getApplicationContext());

            // Configure live or test mode
            boolean testModeAvailable = BranchUtil.checkTestMode(context);
            BranchUtil.setTestMode(isLive ? false : testModeAvailable);
            
            // If a Branch key is passed already use it. Else read the key
            if (TextUtils.isEmpty(branchKey)) {
                branchKey = BranchUtil.readBranchKey(context);
            }
            boolean isNewBranchKeySet;
            if (TextUtils.isEmpty(branchKey)) {
                PrefHelper.Debug("Warning: Please enter your branch_key in your project's Manifest file!");
                isNewBranchKeySet = branchReferral_.prefHelper_.setBranchKey(PrefHelper.NO_STRING_VALUE);
            } else {
                isNewBranchKeySet = branchReferral_.prefHelper_.setBranchKey(branchKey);
            }
            //on setting a new key clear link cache and pending requests
            if (isNewBranchKeySet) {
                branchReferral_.linkCache_.clear();
                branchReferral_.requestQueue_.clear();
            }

            /* If {@link Application} is instantiated register for activity life cycle events. */
            if (context instanceof Application) {
                isAutoSessionMode_ = true;
                branchReferral_.setActivityLifeCycleObserver((Application) context);
            }
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
    public static Branch getInstance(@NonNull Context context) {
        return getBranchInstance(context, true, null);
    }
    
    /**
     * <p>If you configured the your Strings file according to the guide, you'll be able to use
     * the test version of your app by just calling this static method before calling initSession.</p>
     *
     * @param context A {@link Context} from which this call was made.
     * @return An initialised {@link Branch} object.
     */
    public static Branch getTestInstance(@NonNull Context context) {
        return getBranchInstance(context, false, null);
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
    public static Branch getAutoInstance(@NonNull Context context) {
        isAutoSessionMode_ = true;
        boolean isTest = BranchUtil.checkTestMode(context);
        getBranchInstance(context, !isTest, null);
        getPreinstallSystemData(branchReferral_, context);
        return branchReferral_;
    }
    
    /**
     * <p>Singleton method to return the pre-initialised, or newly initialise and return, a singleton
     * object of the type {@link Branch}.</p>
     * <p>Use this whenever you need to call a method directly on the {@link Branch} object.</p>
     *
     * @param context      A {@link Context} from which this call was made.
     * @param isReferrable A {@link Boolean} value indicating whether initialising a session on this Branch instance
     *                     should be considered as potentially referrable or not. By default, a user is only referrable
     *                     if initSession results in a fresh install. Overriding this gives you control of who is referrable.
     * @return An initialised {@link Branch} object, either fetched from a pre-initialised
     * instance within the singleton class, or a newly instantiated object where
     * one was not already requested during the current app lifecycle.
     */
    public static Branch getAutoInstance(@NonNull Context context, boolean isReferrable) {
        isAutoSessionMode_ = true;
        boolean isTest = BranchUtil.checkTestMode(context);
        getBranchInstance(context, !isTest, null);
        getPreinstallSystemData(branchReferral_, context);
        branchReferral_.setIsReferrable(isReferrable);
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
        isAutoSessionMode_ = true;
        boolean isTest = BranchUtil.checkTestMode(context);
        getBranchInstance(context, !isTest, branchKey);
        
        if (branchReferral_.prefHelper_.isValidBranchKey(branchKey)) {
            boolean isNewBranchKeySet = branchReferral_.prefHelper_.setBranchKey(branchKey);
            //on setting a new key clear link cache and pending requests
            if (isNewBranchKeySet) {
                branchReferral_.linkCache_.clear();
                branchReferral_.requestQueue_.clear();
            }
        } else {
            PrefHelper.Debug("Branch Key is invalid. Please check your BranchKey");
        }
        getPreinstallSystemData(branchReferral_, context);
        return branchReferral_;
    }
    
    /**
     * <p>If you configured the your Strings file according to the guide, you'll be able to use
     * the test version of your app by just calling this static method before calling initSession.</p>
     *
     * @param context A {@link Context} from which this call was made.
     * @return An initialised {@link Branch} object.
     */
    public static Branch getAutoTestInstance(@NonNull Context context) {
        isAutoSessionMode_ = true;
        getBranchInstance(context, false, null);
        getPreinstallSystemData(branchReferral_, context);
        return branchReferral_;
    }
    
    /**
     * <p>If you configured the your Strings file according to the guide, you'll be able to use
     * the test version of your app by just calling this static method before calling initSession.</p>
     *
     * @param context      A {@link Context} from which this call was made.
     * @param isReferrable A {@link Boolean} value indicating whether initialising a session on this Branch instance
     *                     should be considered as potentially referrable or not. By default, a user is only referrable
     *                     if initSession results in a fresh install. Overriding this gives you control of who is referrable.
     * @return An initialised {@link Branch} object.
     */
    public static Branch getAutoTestInstance(@NonNull Context context, boolean isReferrable) {
        isAutoSessionMode_ = true;
        getBranchInstance(context, false, null);
        getPreinstallSystemData(branchReferral_, context);
        branchReferral_.setIsReferrable(isReferrable);
        return branchReferral_;
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
        isAutoSessionMode_ = false;

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
     * @deprecated Branch is not listing external apps any more from v2.11.0
     */
    public void disableAppList() {
        // Do nothing
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
     * <p>
     *     Add key value pairs from the injected modules to all requests
     * </p>
     */
    public void addModule(JSONObject module) {
        if (module!=null) {
            Iterator<String> keys = module.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                if (!TextUtils.isEmpty(key)) {
                    try {
                        prefHelper_.addSecondaryRequestMetadata(key, module.getString(key));
                    } catch (JSONException ignore) {
                    }
                }
            }
        }
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
            if (!hasNetwork_) {
                // if there's no network connectivity, purge the old install/open
                ServerRequest req = requestQueue_.peek();
                if (req instanceof ServerRequestRegisterInstall || req instanceof ServerRequestRegisterOpen) {
                    requestQueue_.dequeue();
                }
            } else {
                if (!requestQueue_.containsClose() && closeRequestNeeded) {
                    ServerRequest req = new ServerRequestRegisterClose(context_);
                    handleNewRequest(req);
                }
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

    private void readAndStripParam(Uri data, Activity activity) {
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
    public void onInstallReferrerEventsFinished() {
        requestQueue_.unlockProcessWait(ServerRequest.PROCESS_WAIT_LOCK.INSTALL_REFERRER_FETCH_WAIT_LOCK);
        processNextQueueItem();
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
            handleNewRequest(new ServerRequestGetCPID(context_, Defines.RequestPath.GetCPID, callback));
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
     * <p>Fire-and-forget retrieval of rewards for the current session. Without a callback.</p>
     */
    public void loadRewards() {
        loadRewards(null);
    }
    
    /**
     * <p>Retrieves rewards for the current session, with a callback to perform a predefined
     * action following successful report of state change. You'll then need to call getCredits
     * in the callback to update the credit totals in your UX.</p>
     *
     * @param callback A {@link BranchReferralStateChangedListener} callback instance that will
     *                 trigger actions defined therein upon a referral state change.
     */
    public void loadRewards(BranchReferralStateChangedListener callback) {
        ServerRequest req = new ServerRequestGetRewards(context_, callback);
        if (!req.constructError_ && !req.handleErrors(context_)) {
            handleNewRequest(req);
        }
    }
    
    /**
     * <p>Retrieve the number of credits available for the "default" bucket.</p>
     *
     * @return An {@link Integer} value of the number credits available in the "default" bucket.
     */
    public int getCredits() {
        return prefHelper_.getCreditCount();
    }
    
    /**
     * Returns an {@link Integer} of the number of credits available for use within the supplied
     * bucket name.
     *
     * @param bucket A {@link String} value indicating the name of the bucket to get credits for.
     * @return An {@link Integer} value of the number credits available in the specified
     * bucket.
     */
    public int getCreditsForBucket(String bucket) {
        return prefHelper_.getCreditCount(bucket);
    }
    
    
    /**
     * <p>Redeems the specified number of credits from the "default" bucket, if there are sufficient
     * credits within it. If the number to redeem exceeds the number available in the bucket, all of
     * the available credits will be redeemed instead.</p>
     *
     * @param count A {@link Integer} specifying the number of credits to attempt to redeem from
     *              the bucket.
     */
    public void redeemRewards(int count) {
        redeemRewards(Defines.Jsonkey.DefaultBucket.getKey(), count, null);
    }
    
    /**
     * <p>Redeems the specified number of credits from the "default" bucket, if there are sufficient
     * credits within it. If the number to redeem exceeds the number available in the bucket, all of
     * the available credits will be redeemed instead.</p>
     *
     * @param count    A {@link Integer} specifying the number of credits to attempt to redeem from
     *                 the bucket.
     * @param callback A {@link BranchReferralStateChangedListener} callback instance that will
     *                 trigger actions defined therein upon a executing redeem rewards.
     */
    public void redeemRewards(int count, BranchReferralStateChangedListener callback) {
        redeemRewards(Defines.Jsonkey.DefaultBucket.getKey(), count, callback);
    }
    
    /**
     * <p>Redeems the specified number of credits from the named bucket, if there are sufficient
     * credits within it. If the number to redeem exceeds the number available in the bucket, all of
     * the available credits will be redeemed instead.</p>
     *
     * @param bucket A {@link String} value containing the name of the referral bucket to attempt
     *               to redeem credits from.
     * @param count  A {@link Integer} specifying the number of credits to attempt to redeem from
     *               the specified bucket.
     */
    public void redeemRewards(@NonNull final String bucket, final int count) {
        redeemRewards(bucket, count, null);
    }
    
    
    /**
     * <p>Redeems the specified number of credits from the named bucket, if there are sufficient
     * credits within it. If the number to redeem exceeds the number available in the bucket, all of
     * the available credits will be redeemed instead.</p>
     *
     * @param bucket   A {@link String} value containing the name of the referral bucket to attempt
     *                 to redeem credits from.
     * @param count    A {@link Integer} specifying the number of credits to attempt to redeem from
     *                 the specified bucket.
     * @param callback A {@link BranchReferralStateChangedListener} callback instance that will
     *                 trigger actions defined therein upon a executing redeem rewards.
     */
    public void redeemRewards(@NonNull final String bucket,
                              final int count, BranchReferralStateChangedListener callback) {
        ServerRequestRedeemRewards req = new ServerRequestRedeemRewards(context_, bucket, count, callback);
        if (!req.constructError_ && !req.handleErrors(context_)) {
            handleNewRequest(req);
        }
    }
    
    /**
     * <p>Gets the credit history of the specified bucket and triggers a callback to handle the
     * response.</p>
     *
     * @param callback A {@link BranchListResponseListener} callback instance that will trigger
     *                 actions defined therein upon receipt of a response to a create link request.
     */
    public void getCreditHistory(BranchListResponseListener callback) {
        getCreditHistory(null, null, 100, CreditHistoryOrder.kMostRecentFirst, callback);
    }
    
    /**
     * <p>Gets the credit history of the specified bucket and triggers a callback to handle the
     * response.</p>
     *
     * @param bucket   A {@link String} value containing the name of the referral bucket that the
     *                 code will belong to.
     * @param callback A {@link BranchListResponseListener} callback instance that will trigger
     *                 actions defined therein upon receipt of a response to a create link request.
     */
    public void getCreditHistory(@NonNull final String bucket, BranchListResponseListener
            callback) {
        getCreditHistory(bucket, null, 100, CreditHistoryOrder.kMostRecentFirst, callback);
    }
    
    /**
     * <p>Gets the credit history of the specified bucket and triggers a callback to handle the
     * response.</p>
     *
     * @param afterId  A {@link String} value containing the ID of the history record to begin after.
     *                 This allows for a partial history to be retrieved, rather than the entire
     *                 credit history of the bucket.
     * @param length   A {@link Integer} value containing the number of credit history records to
     *                 return.
     * @param order    A {@link CreditHistoryOrder} object indicating which order the results should
     *                 be returned in.
     *                 <p>Valid choices:</p>
     *                 <ul>
     *                 <li>{@link CreditHistoryOrder#kMostRecentFirst}</li>
     *                 <li>{@link CreditHistoryOrder#kLeastRecentFirst}</li>
     *                 </ul>
     * @param callback A {@link BranchListResponseListener} callback instance that will trigger
     *                 actions defined therein upon receipt of a response to a create link request.
     */
    public void getCreditHistory(@NonNull final String afterId, final int length,
                                 @NonNull final CreditHistoryOrder order, BranchListResponseListener callback) {
        getCreditHistory(null, afterId, length, order, callback);
    }
    
    /**
     * <p>Gets the credit history of the specified bucket and triggers a callback to handle the
     * response.</p>
     *
     * @param bucket   A {@link String} value containing the name of the referral bucket that the
     *                 code will belong to.
     * @param afterId  A {@link String} value containing the ID of the history record to begin after.
     *                 This allows for a partial history to be retrieved, rather than the entire
     *                 credit history of the bucket.
     * @param length   A {@link Integer} value containing the number of credit history records to
     *                 return.
     * @param order    A {@link CreditHistoryOrder} object indicating which order the results should
     *                 be returned in.
     *                 <p>Valid choices:</p>
     *                 <ul>
     *                 <li>{@link CreditHistoryOrder#kMostRecentFirst}</li>
     *                 <li>{@link CreditHistoryOrder#kLeastRecentFirst}</li>
     *                 </ul>
     * @param callback A {@link BranchListResponseListener} callback instance that will trigger
     *                 actions defined therein upon receipt of a response to a create link request.
     */
    public void getCreditHistory(final String bucket, final String afterId, final int length,
                                 @NonNull final CreditHistoryOrder order, BranchListResponseListener callback) {
        ServerRequest req = new ServerRequestGetRewardHistory(context_, bucket, afterId, length, order, callback);
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
     */
    public void userCompletedAction(@NonNull final String action, JSONObject metadata) {
        userCompletedAction(action, metadata, null);
    }
    
    /**
     * <p>A void call to indicate that the user has performed a specific action and for that to be
     * reported to the Branch API.</p>
     *
     * @param action A {@link String} value to be passed as an action that the user has carried
     *               out. For example "registered" or "logged in".
     */
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
     */
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
     */
    public void userCompletedAction(@NonNull final String action, JSONObject metadata,
                                    BranchViewHandler.IBranchViewEvents callback) {
        ServerRequest req = new ServerRequestActionCompleted(context_,
                action, null, metadata, callback);
        if (!req.constructError_ && !req.handleErrors(context_)) {
            handleNewRequest(req);
        }
    }
    
    public void sendCommerceEvent(@NonNull CommerceEvent commerceEvent, JSONObject metadata,
                                  BranchViewHandler.IBranchViewEvents callback) {
        ServerRequest req = new ServerRequestActionCompleted(context_,
                BRANCH_STANDARD_EVENT.PURCHASE.getName(), commerceEvent, metadata, callback);
        if (!req.constructError_ && !req.handleErrors(context_)) {
            handleNewRequest(req);
        }
    }
    
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
        prefHelper_.partnerParams_.addFacebookParameter(key, value);
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
            } else {
                if (req.isAsync()) {
                    generateShortLinkAsync(req);
                } else {
                    return generateShortLinkSync(req);
                }
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
    
    private String convertDate(Date date) {
        return android.text.format.DateFormat.format("yyyy-MM-dd", date).toString();
    }
    
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
    
    private void generateShortLinkAsync(final ServerRequest req) {
        handleNewRequest(req);
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
    
    private void processNextQueueItem() {
        try {
            serverSema_.acquire();
            if (networkCount_ == 0 && requestQueue_.getSize() > 0) {
                networkCount_ = 1;
                ServerRequest req = requestQueue_.peek();
                
                serverSema_.release();
                if (req != null) {
                    if (!req.isWaitingOnProcessToFinish()) {
                        // All request except Install request need a valid IdentityID
                        if (!(req instanceof ServerRequestRegisterInstall) && !hasUser()) {
                            PrefHelper.Debug("Branch Error: User session has not been initialized!");
                            networkCount_ = 0;
                            handleFailure(requestQueue_.getSize() - 1, BranchError.ERR_NO_SESSION);
                        }
                        // Determine if a session is needed to execute (SDK-271)
                        else if (requestNeedsSession(req) && !isSessionAvailableForRequest()) {
                            networkCount_ = 0;
                            handleFailure(requestQueue_.getSize() - 1, BranchError.ERR_NO_SESSION);
                        } else {
                            final CountDownLatch latch = new CountDownLatch(1);
                            final BranchPostTask postTask = new BranchPostTask(req, latch);
                            postTask.executeTask();
                            startTimeoutTimer(latch, postTask, prefHelper_.getTimeout());
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

    private void startTimeoutTimer(final CountDownLatch latch, final BranchPostTask postTask, final int timeout) {
        new Thread(new Runnable() {@Override public void run() {
            try {
                if (!latch.await(timeout, TimeUnit.MILLISECONDS)) {
                    postTask.cancel(true);

                    // it takes time to cancel the thread, so we do the timeout state cleanup here instead of postTask.onCancelled().
                    postTask.thisReq_.handleFailure(ERR_BRANCH_REQ_TIMED_OUT,  "Timed out: " + postTask.thisReq_.getRequestUrl());
                    requestQueue_.remove(postTask.thisReq_);
                }
            } catch (InterruptedException ignored) {}
        }}).start();
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
        return (hasSession() && hasDeviceFingerPrint());
    }
    
    private void handleFailure(int index, int statusCode) {
        ServerRequest req;
        if (index >= requestQueue_.getSize()) {
            req = requestQueue_.peekAt(requestQueue_.getSize() - 1);
        } else {
            req = requestQueue_.peekAt(index);
        }
        handleFailure(req, statusCode);
    }
    
    private void handleFailure(final ServerRequest req, int statusCode) {
        if (req == null)
            return;
        req.handleFailure(statusCode, "");
    }
    
    private void updateAllRequestsInQueue() {
        try {
            for (int i = 0; i < requestQueue_.getSize(); i++) {
                ServerRequest req = requestQueue_.peekAt(i);
                if (req != null) {
                    JSONObject reqJson = req.getPost();
                    if (reqJson != null) {
                        if (reqJson.has(Defines.Jsonkey.SessionID.getKey())) {
                            req.getPost().put(Defines.Jsonkey.SessionID.getKey(), prefHelper_.getSessionID());
                        }
                        if (reqJson.has(Defines.Jsonkey.IdentityID.getKey())) {
                            req.getPost().put(Defines.Jsonkey.IdentityID.getKey(), prefHelper_.getIdentityID());
                        }
                        if (reqJson.has(Defines.Jsonkey.DeviceFingerprintID.getKey())) {
                            req.getPost().put(Defines.Jsonkey.DeviceFingerprintID.getKey(), prefHelper_.getDeviceFingerPrintID());
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
    
    private boolean hasDeviceFingerPrint() {
        return !prefHelper_.getDeviceFingerPrintID().equals(PrefHelper.NO_STRING_VALUE);
    }
    
    private boolean hasUser() {
        return !prefHelper_.getIdentityID().equals(PrefHelper.NO_STRING_VALUE);
    }
    
    private void insertRequestAtFront(ServerRequest req) {
        if (networkCount_ == 0) {
            requestQueue_.insert(req, 0);
        } else {
            requestQueue_.insert(req, 1);
        }
    }

    private void initializeSession(final BranchReferralInitListener callback) {
        initializeSession(callback, 0);
    }

    private void initializeSession(final BranchReferralInitListener callback, int delay) {
        if ((prefHelper_.getBranchKey() == null || prefHelper_.getBranchKey().equalsIgnoreCase(PrefHelper.NO_STRING_VALUE))) {
            setInitState(SESSION_STATE.UNINITIALISED);
            //Report Key error on callback
            if (callback != null) {
                callback.onInitFinished(null, new BranchError("Trouble initializing Branch.", BranchError.ERR_BRANCH_KEY_INVALID));
            }
            PrefHelper.Debug("Warning: Please enter your branch_key in your project's manifest");
            return;
        } else if (isTestModeEnabled()) {
            PrefHelper.Debug("Warning: You are using your test app's Branch Key. Remember to change it to live Branch Key during deployment.");
        }

        ServerRequestInitSession initRequest = getInstallOrOpenRequest(callback);
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

        // !isFirstInitialization condition equals true only when user calls reInitSession()

        if (getInitState() == SESSION_STATE.UNINITIALISED || forceBranchSession) {
            if (forceBranchSession && intent != null) {
                intent.removeExtra(Defines.IntentKeys.ForceNewBranchSession.getKey());
            }
            registerAppInit(initRequest, false);
        } else if (callback != null) {
            // Else, let the user know session initialization failed because it's already initialized.
            callback.onInitFinished(null, new BranchError("Warning.", BranchError.ERR_BRANCH_ALREADY_INITIALIZED));
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
            if (checkInstallReferrer_ && request instanceof ServerRequestRegisterInstall && !GooglePlayStoreAttribution.hasBeenUsed) {
                request.addProcessWaitLock(ServerRequest.PROCESS_WAIT_LOCK.INSTALL_REFERRER_FETCH_WAIT_LOCK);
                new GooglePlayStoreAttribution().captureInstallReferrer(context_, playStoreReferrerWaitTime, this);

                // GooglePlayStoreAttribution error are thrown synchronously, so we remove
                // INSTALL_REFERRER_FETCH_WAIT_LOCK manually (see GooglePlayStoreAttribution.erroredOut)
                if (GooglePlayStoreAttribution.erroredOut) {
                    request.removeProcessWaitLock(ServerRequest.PROCESS_WAIT_LOCK.INSTALL_REFERRER_FETCH_WAIT_LOCK);
                }
            }
        }

        if (isGAParamsFetchInProgress_) {
            request.addProcessWaitLock(ServerRequest.PROCESS_WAIT_LOCK.GAID_FETCH_WAIT_LOCK);
        }

        if (!requestQueue_.containsInitRequest()) {
            insertRequestAtFront(request);
            processNextQueueItem();
        } else {
            PrefHelper.Debug("Warning! Attempted to queue multiple init session requests");
        }
    }
    
    ServerRequestInitSession getInstallOrOpenRequest(BranchReferralInitListener callback) {
        ServerRequestInitSession request;
        if (hasUser()) {
            // If there is user this is open
            request = new ServerRequestRegisterOpen(context_, callback);
        } else {
            // If no user this is an Install
            request = new ServerRequestRegisterInstall(context_, callback);
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
            req.reportTrackingDisabledError();
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
            isAutoSessionMode_ = false;
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
     * {@link BranchReferralStateChangedListener}, defining a single method that takes a value of
     * {@link Boolean} format, and an error message of {@link BranchError} format that will be
     * returned on failure of the request response.</p>
     *
     * @see Boolean
     * @see BranchError
     */
    public interface BranchReferralStateChangedListener {
        void onStateChanged(boolean changed, @Nullable BranchError error);
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
     * <p>enum containing the sort options for return of credit history.</p>
     */
    public enum CreditHistoryOrder {
        kMostRecentFirst, kLeastRecentFirst
    }
    
    /**
     * Async Task to create  a shorlink for synchronous methods
     */
    private class GetShortLinkTask extends AsyncTask<ServerRequest, Void, ServerResponse> {
        @Override protected ServerResponse doInBackground(ServerRequest... serverRequests) {
            return branchRemoteInterface_.make_restful_post(serverRequests[0].getPost(),
                    prefHelper_.getAPIBaseUrl() + Defines.RequestPath.GetURL.getPath(),
                    Defines.RequestPath.GetURL.getPath(), prefHelper_.getBranchKey());
        }
    }

    /**
     * Asynchronous task handling execution of server requests. Execute the network task on background
     * thread and request are  executed in sequential manner. Handles the request execution in
     * Synchronous-Asynchronous pattern. Should be invoked only form main thread and  the results are
     * published in the main thread.
     */
    private class BranchPostTask extends BranchAsyncTask<Void, Void, ServerResponse> {
        ServerRequest thisReq_;
        private final CountDownLatch latch_;
        
        public BranchPostTask(ServerRequest request, CountDownLatch latch) {
            thisReq_ = request;
            latch_ = latch;
        }
        
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            thisReq_.onPreExecute();
            thisReq_.doFinalUpdateOnMainThread();
        }
        
        @Override
        protected ServerResponse doInBackground(Void... voids) {
            // update queue wait time
            addExtraInstrumentationData(thisReq_.getRequestPath() + "-" + Defines.Jsonkey.Queue_Wait_Time.getKey(), String.valueOf(thisReq_.getQueueWaitTime()));
            thisReq_.doFinalUpdateOnBackgroundThread();
            if (isTrackingDisabled() && thisReq_.prepareExecuteWithoutTracking() == false) {
                return new ServerResponse(thisReq_.getRequestPath(), BranchError.ERR_BRANCH_TRACKING_DISABLED, "");
            }
            if (thisReq_.isGetRequest()) {
                return branchRemoteInterface_.make_restful_get(thisReq_.getRequestUrl(), thisReq_.getGetParams(), thisReq_.getRequestPath(), prefHelper_.getBranchKey());
            } else {
                return branchRemoteInterface_.make_restful_post(thisReq_.getPostWithInstrumentationValues(instrumentationExtraData_), thisReq_.getRequestUrl(), thisReq_.getRequestPath(), prefHelper_.getBranchKey());
            }
        }
        
        @Override
        protected void onPostExecute(ServerResponse serverResponse) {
            super.onPostExecute(serverResponse);
            latch_.countDown();

            if (serverResponse == null || isCancelled()) return;

            try {
                int status = serverResponse.getStatusCode();
                hasNetwork_ = true;

                if (serverResponse.getStatusCode() == BranchError.ERR_BRANCH_TRACKING_DISABLED) {
                    thisReq_.reportTrackingDisabledError();
                    requestQueue_.remove(thisReq_);

                } else {
                    //If the request is not succeeded
                    if (status != 200) {
                        //If failed request is an initialisation request then mark session not initialised
                        if (thisReq_ instanceof ServerRequestInitSession) {
                            setInitState(SESSION_STATE.UNINITIALISED);
                        }
                        // On a bad request or in canse of a conflict notify with call back and remove the request.
                        if (status == 400 || status == 409) {
                            requestQueue_.remove(thisReq_);
                            if (thisReq_ instanceof ServerRequestCreateUrl) {
                                ((ServerRequestCreateUrl) thisReq_).handleDuplicateURLError();
                            } else {
                                PrefHelper.LogAlways("Branch API Error: Conflicting resource error code from API");
                                handleFailure(0, status);
                            }
                        }
                        //On Network error or Branch is down fail all the pending requests in the queue except
                        //for request which need to be replayed on failure.
                        else {
                            hasNetwork_ = false;
                            //Collect all request from the queue which need to be failed.
                            ArrayList<ServerRequest> requestToFail = new ArrayList<>();
                            for (int i = 0; i < requestQueue_.getSize(); i++) {
                                requestToFail.add(requestQueue_.peekAt(i));
                            }
                            //Remove the requests from the request queue first
                            for (ServerRequest req : requestToFail) {
                                if (req == null || !req.shouldRetryOnFail()) { // Should remove any nullified request object also from queue
                                    requestQueue_.remove(req);
                                }
                            }
                            // Then, set the network count to zero, indicating that requests can be started again.
                            networkCount_ = 0;

                            //Finally call the request callback with the error.
                            for (ServerRequest req : requestToFail) {
                                if (req != null) {
                                    req.handleFailure(status, serverResponse.getFailReason());
                                    //If request need to be replayed, no need for the callbacks
                                    if (req.shouldRetryOnFail())
                                        req.clearCallbacks();
                                }
                            }
                        }
                    }
                    // If the request succeeded
                    else {
                        hasNetwork_ = true;
                        //On create  new url cache the url.
                        if (thisReq_ instanceof ServerRequestCreateUrl) {
                            if (serverResponse.getObject() != null) {
                                final String url = serverResponse.getObject().getString("url");
                                // cache the link
                                linkCache_.put(((ServerRequestCreateUrl) thisReq_).getLinkPost(), url);
                            }
                        }
                        //On Logout clear the link cache and all pending requests
                        else if (thisReq_ instanceof ServerRequestLogout) {
                            linkCache_.clear();
                            requestQueue_.clear();
                        }
                        requestQueue_.dequeue();

                        // If this request changes a session update the session-id to queued requests.
                        if (thisReq_ instanceof ServerRequestInitSession
                                || thisReq_ instanceof ServerRequestIdentifyUserRequest) {
                            // Immediately set session and Identity and update the pending request with the params
                            JSONObject respJson = serverResponse.getObject();
                            if (respJson != null) {
                                boolean updateRequestsInQueue = false;
                                if (!isTrackingDisabled()) { // Update PII data only if tracking is disabled
                                    if (respJson.has(Defines.Jsonkey.SessionID.getKey())) {
                                        prefHelper_.setSessionID(respJson.getString(Defines.Jsonkey.SessionID.getKey()));
                                        updateRequestsInQueue = true;
                                    }
                                    if (respJson.has(Defines.Jsonkey.IdentityID.getKey())) {
                                        String new_Identity_Id = respJson.getString(Defines.Jsonkey.IdentityID.getKey());
                                        if (!prefHelper_.getIdentityID().equals(new_Identity_Id)) {
                                            //On setting a new identity Id clear the link cache
                                            linkCache_.clear();
                                            prefHelper_.setIdentityID(respJson.getString(Defines.Jsonkey.IdentityID.getKey()));
                                            updateRequestsInQueue = true;
                                        }
                                    }
                                    if (respJson.has(Defines.Jsonkey.DeviceFingerprintID.getKey())) {
                                        prefHelper_.setDeviceFingerPrintID(respJson.getString(Defines.Jsonkey.DeviceFingerprintID.getKey()));
                                        updateRequestsInQueue = true;
                                    }
                                }

                                if (updateRequestsInQueue) {
                                    updateAllRequestsInQueue();
                                }

                                if (thisReq_ instanceof ServerRequestInitSession) {
                                    setInitState(SESSION_STATE.INITIALISED);
                                    thisReq_.onRequestSucceeded(serverResponse, branchReferral_);
                                    if (!((ServerRequestInitSession) thisReq_).handleBranchViewIfAvailable((serverResponse))) {
                                        checkForAutoDeepLinkConfiguration();
                                    }
                                    // Count down the latch holding getLatestReferringParamsSync
                                    if (getLatestReferringParamsLatch != null) {
                                        getLatestReferringParamsLatch.countDown();
                                    }
                                    // Count down the latch holding getFirstReferringParamsSync
                                    if (getFirstReferringParamsLatch != null) {
                                        getFirstReferringParamsLatch.countDown();
                                    }
                                } else {
                                    // For setting identity just call only request succeeded
                                    thisReq_.onRequestSucceeded(serverResponse, branchReferral_);
                                }
                            }
                        } else {
                            //Publish success to listeners
                            thisReq_.onRequestSucceeded(serverResponse, branchReferral_);
                        }
                    }
                }
                networkCount_ = 0;
                if (hasNetwork_ && initState_ != SESSION_STATE.UNINITIALISED) {
                    processNextQueueItem();
                }
            } catch (JSONException ex) {
                ex.printStackTrace();
            }
        }

        @Override
        protected void onCancelled(ServerResponse v) {
            super.onCancelled();
            // Timeout cleanup happens in branch.startTimeoutTimer(...) to preserve timeout accuracy
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
        return (activity.getIntent().getStringExtra(AUTO_DEEP_LINKED) != null);
    }
    
    private void checkForAutoDeepLinkConfiguration() {
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
                    intent.putExtra(AUTO_DEEP_LINKED, "true");

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
     * @deprecated use Branch.bypassWaitingForIntent(true)
     */
    @Deprecated
    public static void enableForcedSession() { bypassWaitingForIntent(true); }


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
     * @deprecated use Branch.bypassWaitingForIntent(false)
     */
    @Deprecated
    public static void disableForcedSession() { bypassWaitingForIntent(false); }

    /**
     * Returns true if session initialization should bypass waiting for intent (retrieved after onResume).
     *
     * @return {@link Boolean} with value true to enable forced session
     *
     * @deprecated use Branch.isWaitingForIntent()
     */
    @Deprecated
    public static boolean isForceSessionEnabled() { return isWaitingForIntent(); }
    @SuppressWarnings("WeakerAccess")
    public static boolean isWaitingForIntent() { return !bypassWaitingForIntent_; }
    
    public static void enableBypassCurrentActivityIntentState() {
        bypassCurrentActivityIntentState_ = true;
    }

    @SuppressWarnings("WeakerAccess")
    public static boolean bypassCurrentActivityIntentState() {
        return bypassCurrentActivityIntentState_;
    }

    @SuppressWarnings("WeakerAccess")
    void setIsReferrable(boolean isReferrable) {
        if (isReferrable) {
            prefHelper_.setIsReferrable();
        } else {
            prefHelper_.clearIsReferrable();
        }
    }

    boolean isReferrable() {
        return prefHelper_.getIsReferrable() == 1;
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

    public static class InitSessionBuilder {
        private BranchReferralInitListener callback;
        private int delay;
        private Uri uri;
        private Boolean ignoreIntent;
        private Boolean isReferrable;
        private boolean isReInitializing;

        private InitSessionBuilder(Activity activity) {
            Branch branch = Branch.getInstance();
            if (activity != null && (branch.getCurrentActivity() == null ||
                    !branch.getCurrentActivity().getLocalClassName().equals(activity.getLocalClassName()))) {
                // currentActivityReference_ is set in onActivityCreated (before initSession), which should happen if
                // users follow Android guidelines and call super.onStart as the first thing in Activity.onStart,
                // however, if they don't, we try to set currentActivityReference_ here too.
                branch.currentActivityReference_ = new WeakReference<>(activity);
            }
        }

        /**
         * <p> Add callback to Branch initialization to retrieve referring params attached to the
         * Branch link via the dashboard. User eventually decides how to use the referring params but
         * they are primarily meant to be used for navigating to specific content within the app.
         * Use only one withCallback() method.</p>
         *
         * @param callback     A {@link BranchUniversalReferralInitListener} instance that will be called
         *                     following successful (or unsuccessful) initialisation of the session
         *                     with the Branch API.
         */
        @SuppressWarnings("WeakerAccess")
        public InitSessionBuilder withCallback(BranchUniversalReferralInitListener callback) {
            this.callback = new BranchUniversalReferralInitWrapper(callback);
            return this;
        }

        /**
         * <p> Delay session initialization by certain time (used when other async or otherwise time
         * consuming ops need to be completed prior to session initialization).</p>
         *
         * @param delayMillis  An {@link Integer} indicating the length of the delay in milliseconds.
         */
        @SuppressWarnings("WeakerAccess")
        public InitSessionBuilder withDelay(int delayMillis) {
            this.delay = delayMillis;
            return this;
        }

        /**
         * <p> Add callback to Branch initialization to retrieve referring params attached to the
         * Branch link via the dashboard. User eventually decides how to use the referring params but
         * they are primarily meant to be used for navigating to specific content within the app.
         * Use only one withCallback() method.</p>
         *
         * @param callback     A {@link BranchReferralInitListener} instance that will be called
         *                     following successful (or unsuccessful) initialisation of the session
         *                     with the Branch API.
         */
        @SuppressWarnings("WeakerAccess")
        public InitSessionBuilder withCallback(BranchReferralInitListener callback) {
            this.callback = callback;
            return this;
        }

        /**
         * <p> Specify a {@link Uri} variable containing the details of the source link that led to
         * this initialisation action.</p>
         *
         * @param uri A {@link  Uri} variable from the intent.
         */
        @SuppressWarnings("WeakerAccess")
        public InitSessionBuilder withData(Uri uri) {
            this.uri = uri;
            return this;
        }

        /**
         * <p> Specify whether the initialisation can count as a referrable action. </p>
         *
         * @param isReferrable A {@link Boolean} value indicating whether initialising a session on this Branch instance
         *                     should be considered as potentially referrable or not. By default, a session is only referrable
         *                     if it is a fresh install resulting from clicking on a Branch link. Overriding this gives you
         *                     control of who is referrable.
         */
        @SuppressWarnings("WeakerAccess")
        public InitSessionBuilder isReferrable(boolean isReferrable) {
            this.isReferrable = isReferrable;
            return this;
        }

        /**
         * <p> Use this method cautiously, it is meant to enable the ability to start a session before
         * the user even opens the app.
         *
         * The use case explained:
         * Users are expected to initialize session from Activity.onStart. However, by default, Branch actually
         * waits until Activity.onResume to start session initialization, so as to ensure that the latest intent
         * data is available (e.g. when activity is launched from stack via onNewIntent). Setting this flag to true
         * will bypass waiting for intent, so session could technically be initialized from a background service
         * or otherwise before the application is even opened.
         *
         * Note however that if the flag is not reset during normal app boot up, the SDK behavior is undefined
         * in certain cases. See also Branch.bypassWaitingForIntent(boolean). </p>
         *
         * @param ignore       a {@link Boolean} indicating if SDK should wait for onResume to retrieve
         *                     the most up recent intent data before firing the session initialization request.
         */
        @SuppressWarnings("WeakerAccess")
        public InitSessionBuilder ignoreIntent(boolean ignore) {
            ignoreIntent = ignore;
            return this;
        }

        /**
         * <p>Initialises a session with the Branch API, registers the passed in Activity, callback
         * and configuration variables, then initializes session.</p>
         */
        public void init() {
            final Branch branch = Branch.getInstance();
            if (branch == null) {
                PrefHelper.LogAlways("Branch is not setup properly, make sure to call getAutoInstance" +
                        " in your application class or declare BranchApp in your manifest.");
                return;
            }
            if (isReferrable != null) {
                branch.setIsReferrable(isReferrable);
            }
            if (ignoreIntent != null) {
                Branch.bypassWaitingForIntent(ignoreIntent);
            }

            Activity activity = branch.getCurrentActivity();
            Intent intent = activity != null ? activity.getIntent() : null;
            if (uri != null) {
                branch.readAndStripParam(uri, activity);
            } else if (isReInitializing && branch.isRestartSessionRequested(intent)) {
                branch.readAndStripParam(intent != null ? intent.getData() : null, activity);
            } else if (isReInitializing) {
                // User called reInit but isRestartSessionRequested = false, meaning the new intent was
                // not initiated by Branch and should not be considered a "new session", return early
                return;
            }

            // readAndStripParams (above) may set isInstantDeepLinkPossible to true
            if (branch.isInstantDeepLinkPossible) {
                // reset state
                branch.isInstantDeepLinkPossible = false;
                // invoke callback returning LatestReferringParams, which were parsed out inside readAndStripParam
                // from either intent extra "branch_data", or as parameters attached to the referring app link
                callback.onInitFinished(branch.getLatestReferringParams(), null);
                // mark this session as IDL session
                branch.addExtraInstrumentationData(Defines.Jsonkey.InstantDeepLinkSession.getKey(), "true");
                // potentially routes the user to the Activity configured to consume this particular link
                branch.checkForAutoDeepLinkConfiguration();
                // we already invoked the callback for let's set it to null, make will still make the
                // init session request but for analytics purposes only
                callback = null;
            }

            if (delay > 0) {
                expectDelayedSessionInitialization(true);
            }

            branch.initializeSession(callback, delay);
        }

        /**
         * <p> Re-Initialize a session. Call from Activity.onNewIntent().
         * This solves a very specific use case, whereas the app is already in the foreground and a new
         * intent with a Uri is delivered to the foregrounded activity.
         *
         * Note that the Uri can also be stored as an extra in the field under the key `IntentKeys.BranchURI.getKey()` (i.e. "branch").
         *
         * Note also, that the since the method is expected to be called from Activity.onNewIntent(),
         * the implementation assumes the intent will be non-null and will contain a Branch link in
         * either the URI or in the the extra.</p>
         *
         */
        @SuppressWarnings("WeakerAccess")
        public void reInit() {
            isReInitializing = true;
            init();
        }
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
        return BuildConfig.VERSION_NAME;
    }

    //-------------------------- DEPRECATED --------------------------------------//

    /**
     * <p> Legacy class for building a share link dialog. Use {@link BranchShareSheetBuilder} instead. </p>
     */
    @Deprecated public static class ShareLinkBuilder extends BranchShareSheetBuilder {
        @Deprecated public ShareLinkBuilder(Activity activity, JSONObject parameters) { super(activity, parameters); }
        @Deprecated public ShareLinkBuilder(Activity activity, BranchShortLinkBuilder shortLinkBuilder) { super(activity, shortLinkBuilder); }
        public ShareLinkBuilder setMessage(String message) { super.setMessage(message); return this; }
        public ShareLinkBuilder setSubject(String subject) { super.setSubject(subject); return this; }
        public ShareLinkBuilder addTag(String tag) { super.addTag(tag); return this; }
        public ShareLinkBuilder addTags(ArrayList<String> tags) { super.addTags(tags); return this; }
        public ShareLinkBuilder setFeature(String feature) { super.setFeature(feature); return this; }
        public ShareLinkBuilder setStage(String stage) { super.setStage(stage); return this; }
        public ShareLinkBuilder setCallback(BranchLinkShareListener callback) { super.setCallback(callback); return this; }
        public ShareLinkBuilder setChannelProperties(IChannelProperties channelPropertiesCallback) { super.setChannelProperties(channelPropertiesCallback); return this; }
        public ShareLinkBuilder addPreferredSharingOption(SharingHelper.SHARE_WITH preferredOption) { super.addPreferredSharingOption(preferredOption); return this; }
        public ShareLinkBuilder addPreferredSharingOptions(ArrayList<SharingHelper.SHARE_WITH> preferredOptions) { super.addPreferredSharingOptions(preferredOptions); return this; }
        public ShareLinkBuilder addParam(String key, String value) { super.addParam(key, value); return this; }
        public ShareLinkBuilder setDefaultURL(String url) { super.setDefaultURL(url); return this; }
        public ShareLinkBuilder setMoreOptionStyle(Drawable icon, String label) { super.setMoreOptionStyle(icon, label); return this; }
        public ShareLinkBuilder setMoreOptionStyle(int drawableIconID, int stringLabelID) { super.setMoreOptionStyle(drawableIconID, stringLabelID); return this; }
        public ShareLinkBuilder setCopyUrlStyle(Drawable icon, String label, String message) { super.setCopyUrlStyle(icon, label, message); return this; }
        public ShareLinkBuilder setCopyUrlStyle(int drawableIconID, int stringLabelID, int stringMessageID) { super.setCopyUrlStyle(drawableIconID, stringLabelID, stringMessageID); return this; }
        public ShareLinkBuilder setAlias(String alias) { super.setAlias(alias); return this; }
        public ShareLinkBuilder setMatchDuration(int matchDuration) { super.setMatchDuration(matchDuration); return this; }
        public ShareLinkBuilder setAsFullWidthStyle(boolean setFullWidthStyle) { super.setAsFullWidthStyle(setFullWidthStyle); return this; }
        public ShareLinkBuilder setDialogThemeResourceID(@StyleRes int styleResourceID) { super.setDialogThemeResourceID(styleResourceID); return this; }
        public ShareLinkBuilder setDividerHeight(int height) { super.setDividerHeight(height); return this; }
        public ShareLinkBuilder setSharingTitle(String title) { super.setSharingTitle(title); return this; }
        public ShareLinkBuilder setSharingTitle(View titleView) { super.setSharingTitle(titleView); return this; }
        public ShareLinkBuilder setIconSize(int iconSize) { super.setIconSize(iconSize); return this; }
        public ShareLinkBuilder excludeFromShareSheet(@NonNull String packageName) { super.excludeFromShareSheet(packageName); return this; }
        public ShareLinkBuilder excludeFromShareSheet(@NonNull String[] packageName) { super.excludeFromShareSheet(packageName); return this; }
        public ShareLinkBuilder excludeFromShareSheet(@NonNull List<String> packageNames) { super.excludeFromShareSheet(packageNames); return this; }
        public ShareLinkBuilder includeInShareSheet(@NonNull String packageName) { super.includeInShareSheet(packageName); return this; }
        public ShareLinkBuilder includeInShareSheet(@NonNull String[] packageName) { super.includeInShareSheet(packageName); return this; }
        public ShareLinkBuilder includeInShareSheet(@NonNull List<String> packageNames) { super.includeInShareSheet(packageNames); return this; }
    }
    /** @deprecated use Branch.sessionBuilder(null).withCallback(callback).init(); */
    public boolean initSession(BranchUniversalReferralInitListener callback) { Branch.sessionBuilder(null).withCallback(callback).init();return true; }
    /** @deprecated use Branch.sessionBuilder(null).withCallback(callback).init(); */
    public boolean initSession(BranchReferralInitListener callback) { Branch.sessionBuilder(null).withCallback(callback).init();return true; }
    /** @deprecated use Branch.sessionBuilder(activity).withCallback(callback).init(); */
    public boolean initSession(BranchUniversalReferralInitListener callback, Activity activity) { Branch.sessionBuilder(activity).withCallback(callback).init();return true; }
    /** @deprecated use Branch.sessionBuilder(activity).withCallback(callback).init(); */
    public boolean initSession(BranchReferralInitListener callback, Activity activity) {Branch.sessionBuilder(activity).withCallback(callback).init();return true; }
    /** @deprecated use Branch.sessionBuilder(null).withCallback(callback).withData(data).init(); */
    public boolean initSession(BranchUniversalReferralInitListener callback, Uri data) {Branch.sessionBuilder(null).withCallback(callback).withData(data).init();return true; }
    /** @deprecated use Branch.sessionBuilder(null).withCallback(callback).withData(data).init(); */
    public boolean initSession(BranchReferralInitListener callback, Uri data) {Branch.sessionBuilder(null).withCallback(callback).withData(data).init();return true; }
    /** @deprecated use Branch.sessionBuilder(activity).withCallback(callback).withData(data).init(); */
    public boolean initSession(BranchUniversalReferralInitListener callback, Uri data, Activity activity) { Branch.sessionBuilder(activity).withCallback(callback).withData(data).init();return true; }
    /** @deprecated use Branch.sessionBuilder(activity).withCallback(callback).withData(data).init(); */
    public boolean initSession(BranchReferralInitListener callback, Uri data, Activity activity) { Branch.sessionBuilder(activity).withCallback(callback).withData(data).init();return true; }
    /** @deprecated use Branch.sessionBuilder(null).init(); */
    public boolean initSession() {Branch.sessionBuilder(null).init();return true; }
    /** @deprecated use Branch.sessionBuilder(activity).init(); */
    public boolean initSession(Activity activity) {Branch.sessionBuilder(activity).init();return true; }
    /** @deprecated use Branch.sessionBuilder(null).ignoreIntent(true).withCallback(callback).init(); */
    public boolean initSessionForced(BranchReferralInitListener callback) { Branch.sessionBuilder(null).ignoreIntent(true).withCallback(callback).init();return true; }
    /** @deprecated use Branch.sessionBuilder(null).withData(data).init(); */
    public boolean initSessionWithData(Uri data) {Branch.sessionBuilder(null).withData(data).init();return true; }
    /** @deprecated use Branch.sessionBuilder(activity).withData(data).init(); */
    public boolean initSessionWithData(Uri data, Activity activity) {Branch.sessionBuilder(activity).withData(data).init();return true; }
    /** @deprecated use Branch.sessionBuilder(null).isReferrable(isReferrable).init(); */
    public boolean initSession(boolean isReferrable) {Branch.sessionBuilder(null).isReferrable(isReferrable).init();return true; }
    /** @deprecated use Branch.sessionBuilder(activity).isReferrable(isReferrable).init(); */
    public boolean initSession(boolean isReferrable, @NonNull Activity activity) {Branch.sessionBuilder(activity).isReferrable(isReferrable).init();return true; }
    /** @deprecated use Branch.sessionBuilder(null).withCallback(callback).isReferrable(isReferrable).withData(data).init(); */
    public boolean initSession(BranchUniversalReferralInitListener callback, boolean isReferrable, Uri data) {Branch.sessionBuilder(null).withCallback(callback).isReferrable(isReferrable).withData(data).init();return true; }
    /** @deprecated use Branch.sessionBuilder(null).withCallback(callback).isReferrable(isReferrable).withData(data).init(); */
    public boolean initSession(BranchReferralInitListener callback, boolean isReferrable, Uri data) {Branch.sessionBuilder(null).withCallback(callback).isReferrable(isReferrable).withData(data).init();return true; }
    /** @deprecated use Branch.sessionBuilder(activity).withCallback(callback).isReferrable(isReferrable).withData(data).init(); */
    public boolean initSession(BranchUniversalReferralInitListener callback, boolean isReferrable, Uri data, Activity activity) {Branch.sessionBuilder(activity).withCallback(callback).isReferrable(isReferrable).withData(data).init();return true; }
    /** @deprecated use Branch.sessionBuilder(activity).withCallback(callback).isReferrable(isReferrable).withData(data).init(); */
    public boolean initSession(BranchReferralInitListener callback, boolean isReferrable, Uri data, Activity activity) {Branch.sessionBuilder(activity).withCallback(callback).isReferrable(isReferrable).withData(data).init();return true; }
    /** @deprecated use Branch.sessionBuilder(null).withCallback(callback).isReferrable(isReferrable).init(); */
    public boolean initSession(BranchUniversalReferralInitListener callback, boolean isReferrable) {Branch.sessionBuilder(null).withCallback(callback).isReferrable(isReferrable).init();return true; }
    /** @deprecated use Branch.sessionBuilder(null).withCallback(callback).isReferrable(isReferrable).init(); */
    public boolean initSession(BranchReferralInitListener callback, boolean isReferrable) { Branch.sessionBuilder(null).withCallback(callback).isReferrable(isReferrable).init();return true; }
    /** @deprecated use Branch.sessionBuilder(activity).withCallback(callback).isReferrable(isReferrable).init(); */
    public boolean initSession(BranchUniversalReferralInitListener callback, boolean isReferrable, Activity activity) {Branch.sessionBuilder(activity).withCallback(callback).isReferrable(isReferrable).init();return true; }
    /** @deprecated use Branch.sessionBuilder(activity).withCallback(callback).isReferrable(isReferrable).init(); */
    public boolean initSession(BranchReferralInitListener callback, boolean isReferrable, Activity activity) {Branch.sessionBuilder(activity).withCallback(callback).isReferrable(isReferrable).init();return true; }
    /** @deprecated use Branch.sessionBuilder(activity).withCallback(callback).reInit(); */
    public boolean reInitSession(Activity activity, BranchUniversalReferralInitListener callback) {Branch.sessionBuilder(activity).withCallback(callback).reInit();return activity == null || activity.getIntent() == null; }
    /** @deprecated use Branch.sessionBuilder(activity).withCallback(callback).reInit();*/
    public boolean reInitSession(Activity activity, BranchReferralInitListener callback) {Branch.sessionBuilder(activity).withCallback(callback).reInit();return activity == null || activity.getIntent() == null; }

    /**
     * @deprecated setDebug is deprecated and all functionality has been disabled. If you wish to enable
     * logging, please invoke enableLogging. If you wish to simulate installs, please see add a Test Device
     * (https://help.branch.io/using-branch/docs/adding-test-devices) then reset your test device's data
     * (https://help.branch.io/using-branch/docs/adding-test-devices#section-resetting-your-test-device-data).
     * If you wish to use the test key rather than the live key, please invoke enableTestMode.
     */
    public void setDebug() {
        PrefHelper.LogAlways("setDebug is deprecated and all functionality has been disabled. " +
                "If you wish to enable logging, please invoke enableLogging. If you wish to simulate installs," +
                " please see add a Test Device (https://help.branch.io/using-branch/docs/adding-test-devices) " +
                "then reset your test device's data (https://help.branch.io/using-branch/docs/adding-test-devices#section-resetting-your-test-device-data). " +
                "If you wish to use the test key rather than the live key, please invoke enableTestMode.");
    }

    /**
     * @deprecated enableDebugMode is deprecated and all functionality has been disabled. If you wish to enable
     * logging, please invoke enableLogging. If you wish to simulate installs, please see add a Test Device
     * (https://help.branch.io/using-branch/docs/adding-test-devices) then reset your test device's data
     * (https://help.branch.io/using-branch/docs/adding-test-devices#section-resetting-your-test-device-data).
     * If you wish to use the test key rather than the live key, please invoke enableTestMode.
     */
    public static void enableDebugMode() {
        PrefHelper.LogAlways("enableDebugMode is deprecated and all functionality has been disabled. " +
                "If you wish to enable logging, please invoke enableLogging. If you wish to simulate installs," +
                " please see add a Test Device (https://help.branch.io/using-branch/docs/adding-test-devices) " +
                "then reset your test device's data (https://help.branch.io/using-branch/docs/adding-test-devices#section-resetting-your-test-device-data). " +
                "If you wish to use the test key rather than the live key, please invoke enableTestMode.");
    }

    /** @deprecated (deprecated and all functionality has been disabled, see enableDebugMode for more information) */
    public static void disableDebugMode() {}

    /**
     * @deprecated enableSimulateInstalls is deprecated and all functionality has been disabled. If
     * you wish to simulate installs, please see add a Test Device (https://help.branch.io/using-branch/docs/adding-test-devices)
     * then reset your test device's data (https://help.branch.io/using-branch/docs/adding-test-devices#section-resetting-your-test-device-data).
     * */
    public static void enableSimulateInstalls() {
        PrefHelper.LogAlways("enableSimulateInstalls is deprecated and all functionality has been disabled. " +
                "If you wish to simulate installs, please see add a Test Device (https://help.branch.io/using-branch/docs/adding-test-devices) " +
                "then reset your test device's data (https://help.branch.io/using-branch/docs/adding-test-devices#section-resetting-your-test-device-data).");
    }

    /** @deprecated see enableSimulateInstalls() for more information */
    public static void disableSimulateInstalls() { }
}
