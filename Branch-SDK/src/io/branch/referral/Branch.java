package io.branch.referral;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
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
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.SparseArray;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * The core object required when using Branch SDK. You should declare an object of this type at
 * the class-level of each Activity or Fragment that you wish to use Branch functionality within.
 * </p>
 * <p/>
 * <p>
 * Normal instantiation of this object would look like this:
 * </p>
 * <p/>
 * <pre style="background:#fff;padding:10px;border:2px solid silver;">
 * Branch.getInstance(this.getApplicationContext()) // from an Activity
 * <p/>
 * Branch.getInstance(getActivity().getApplicationContext())    // from a Fragment
 * </pre>
 */
public class Branch {

    private static final String TAG = "BranchSDK";

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
     * @see <a href="http://ogp.me/#metadata">Open Graph - Basic Metadata</a>
     */
    public static final String OG_TITLE = "$og_title";

    /**
     * The description of the object to appear in social media feeds that use
     * Facebook's Open Graph specification.
     * @see <a href="http://ogp.me/#metadata">Open Graph - Basic Metadata</a>
     */
    public static final String OG_DESC = "$og_description";

    /**
     * An image URL which should represent your object to appear in social media feeds that use
     * Facebook's Open Graph specification.
     * @see <a href="http://ogp.me/#metadata">Open Graph - Basic Metadata</a>
     */
    public static final String OG_IMAGE_URL = "$og_image_url";

    /**
     * A URL to a video file that complements this object.
     * @see <a href="http://ogp.me/#metadata">Open Graph - Basic Metadata</a>
     */
    public static final String OG_VIDEO = "$og_video";

    /**
     * The canonical URL of your object that will be used as its permanent ID in the graph.
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

    /**
     * <p>An {@link Integer} variable specifying the amount of time in milliseconds to keep a
     * connection alive before assuming a timeout condition.</p>
     *
     * @see <a href="http://developer.android.com/reference/java/util/Timer.html#schedule(java.util.TimerTask, long)">
     * Timer.schedule (TimerTask task, long delay)</a>
     */
    private static final int SESSION_KEEPALIVE = 2000;

    /**
     * <p>An {@link Integer} value defining the timeout period in milliseconds to wait during a
     * looping task before triggering an actual connection close during a session close action.</p>
     */
    private static final int PREVENT_CLOSE_TIMEOUT = 500;

    /**
     * <p>A {@link Branch} object that is instantiated on init and holds the singleton instance of
     * the class during application runtime.</p>
     */
    private static Branch branchReferral_;

    private BranchRemoteInterface kRemoteInterface_;
    private PrefHelper prefHelper_;
    private SystemObserver systemObserver_;
    private Context context_;

    final Object lock;

    private Timer closeTimer;
    private Timer rotateCloseTimer;
    private boolean keepAlive_;

    private Semaphore serverSema_;

    private ServerRequestQueue requestQueue_;

    private int networkCount_;

    private boolean hasNetwork_;
    private SparseArray<String> debugListenerInitHistory_;
    private OnTouchListener debugOnTouchListener_;
    private Handler debugHandler_;
    private boolean debugStarted_;

    private Map<BranchLinkData, String> linkCache_;

    private ScheduledFuture<?> appListingSchedule_;

    /* BranchActivityLifeCycleObserver instance. Should be initialised on creating Instance with Application object. */
    private BranchActivityLifeCycleObserver activityLifeCycleObserver_;

    /* Set to true when application is instantiating {@BranchApp} by extending or adding manifest entry. */
    private static boolean isAutoSessionMode_ = false;

    /* Set to true when {@link Activity} life cycle callbacks are registered. */
    private static boolean isActivityLifeCycleCallbackRegistered_ = false;

    /* Enumeration for defining session initialisation state. */
    private enum SESSION_STATE {INITIALISED, INITIALISING, UNINITIALISED}

    /* Holds the current Session state. Default is set to UNINITIALISED. */
    private SESSION_STATE initState_ = SESSION_STATE.UNINITIALISED;

    /* Instance  of share link manager to share links automatically with third party applications. */
    private ShareLinkManager shareLinkManager_;

    /*The current activity instance for the application.*/
    private Activity currentActivity_;

    /* Key for Auto Deep link param. The activities which need to automatically deep linked should define in this in the activity metadata. */
    private static final String AUTO_DEEP_LINK_KEY = "io.branch.sdk.auto_link_keys";

    /* Key for disabling auto deep link feature. Setting this to true in manifest will disable auto deep linking feature. */
    private static final String AUTO_DEEP_LINK_DISABLE = "io.branch.sdk.auto_link_disable";

    /*Key for defining a request code for an activity. should be added as a metadata for an activity. This is used as a request code for launching a an activity on auto deep link. */
    private final String AUTO_DEEP_LINK_REQ_CODE = "io.branch.sdk.auto_link_request_code";

    /* Request code  used to launch and activity on auto deep linking unless DEF_AUTO_DEEP_LINK_REQ_CODE is not specified for teh activity in manifest.*/
    private final int DEF_AUTO_DEEP_LINK_REQ_CODE = 1501;

    /**
     * <p>The main constructor of the Branch class is private because the class uses the Singleton
     * pattern.</p>
     * <p/>
     * <p>Use {@link #getInstance(Context) getInstance} method when instantiating.</p>
     *
     * @param context A {@link Context} from which this call was made.
     */
    private Branch(Context context) {
        prefHelper_ = PrefHelper.getInstance(context);
        kRemoteInterface_ = new BranchRemoteInterface(context);
        systemObserver_ = new SystemObserver(context);
        requestQueue_ = ServerRequestQueue.getInstance(context);
        serverSema_ = new Semaphore(1);
        closeTimer = new Timer();
        rotateCloseTimer = new Timer();
        lock = new Object();
        keepAlive_ = false;
        networkCount_ = 0;
        hasNetwork_ = true;
        debugListenerInitHistory_ = new SparseArray<String>();
        debugOnTouchListener_ = retrieveOnTouchListener();
        debugHandler_ = new Handler();
        debugStarted_ = false;
        linkCache_ = new HashMap<BranchLinkData, String>();

    }


    /**
     * <p>Singleton method to return the pre-initialised object of the type {@link Branch}.
     * Make sure your app is instantiating {@link BranchApp} before calling this method
     * or you have created an instance of Branch already by calling getInstance(Context ctx).</p>
     *
     * @return An initialised singleton {@link Branch} object
     *
     *
     */
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public static Branch getInstance() {
        /* Check if BranchApp is instantiated. */
        if (branchReferral_ == null ) {
            Log.e("BranchSDK", "Branch instance is not created yet. Make sure you have initialised Branch. [Consider Calling getInstance(Context ctx) if you still have issue.]");
        }
        else if(isAutoSessionMode_ == true){
            /* Check if Activity life cycle callbacks are set if in auto session mode. */
            if (isActivityLifeCycleCallbackRegistered_ == false) {
                Log.e("BranchSDK" ,"Branch instance is not properly initialised. Make sure your Application class is extending BranchApp class. " +
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
    public static Branch getInstance(Context context, String branchKey) {
        if (branchReferral_ == null) {
            branchReferral_ = Branch.initInstance(context);
        }
        branchReferral_.context_ = context.getApplicationContext();
        if (branchKey.startsWith("key_")) {
            boolean isNewBranchKeySet = branchReferral_.prefHelper_.setBranchKey(branchKey);
            //on setting a new key clear link cache and pending requests
            if (isNewBranchKeySet) {
                branchReferral_.linkCache_.clear();
                branchReferral_.requestQueue_.clear();
            }
        } else {
            branchReferral_.prefHelper_.setAppKey(branchKey);
        }
        return branchReferral_;
    }

    private static Branch getBranchInstance(Context context, boolean isLive) {
        if (branchReferral_ == null) {
            branchReferral_ = Branch.initInstance(context);

            String branchKey = branchReferral_.prefHelper_.readBranchKey(isLive);
            boolean isNewBranchKeySet = false;
            if (branchKey == null || branchKey.equalsIgnoreCase(PrefHelper.NO_STRING_VALUE)) {
                Log.i("BranchSDK", "Branch Warning: Please enter your branch_key in your project's Manifest file!");
                isNewBranchKeySet = branchReferral_.prefHelper_.setBranchKey(PrefHelper.NO_STRING_VALUE);
            } else {
                isNewBranchKeySet = branchReferral_.prefHelper_.setBranchKey(branchKey);
            }
            //on setting a new key clear link cache and pending requests
            if (isNewBranchKeySet) {
                branchReferral_.linkCache_.clear();
                branchReferral_.requestQueue_.clear();
            }
        }
        branchReferral_.context_ = context;

        /* If {@link Application} is instantiated register for activity life cycle events. */
        if (context instanceof BranchApp) {
            isAutoSessionMode_ = true;
            branchReferral_.setActivityLifeCycleObserver((Application) context);
        }

        return branchReferral_;
    }


    /**
     * <p>Singleton method to return the pre-initialised, or newly initialise and return, a singleton
     * object of the type {@link Branch}.</p>
     * <p/>
     * <p>Use this whenever you need to call a method directly on the {@link Branch} object.</p>
     *
     * @param context A {@link Context} from which this call was made.
     * @return An initialised {@link Branch} object, either fetched from a pre-initialised
     * instance within the singleton class, or a newly instantiated object where
     * one was not already requested during the current app lifecycle.
     */
    public static Branch getInstance(Context context) {
        return getBranchInstance(context, true);
    }

    /**
     * <p>If you configured the your Strings file according to the guide, you'll be able to use
     * the test version of your app by just calling this static method before calling initSession.</p>
     *
     * @param context A {@link Context} from which this call was made.
     * @return An initialised {@link Branch} object.
     */
    public static Branch getTestInstance(Context context) {
        return getBranchInstance(context, false);
    }

    /**
     * <p>Singleton method to return the pre-initialised, or newly initialise and return, a singleton
     * object of the type {@link Branch}.</p>
     * <p/>
     * <p>Use this whenever you need to call a method directly on the {@link Branch} object.</p>
     *
     * @param context A {@link Context} from which this call was made.
     * @return An initialised {@link Branch} object, either fetched from a pre-initialised
     * instance within the singleton class, or a newly instantiated object where
     * one was not already requested during the current app lifecycle.
     */
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public static Branch getAutoInstance(Context context) {
        isAutoSessionMode_ = true;
        getBranchInstance(context, true);
        branchReferral_.setActivityLifeCycleObserver((Application)context);
        return branchReferral_;
    }

    /**
     * <p>If you configured the your Strings file according to the guide, you'll be able to use
     * the test version of your app by just calling this static method before calling initSession.</p>
     *
     * @param context A {@link Context} from which this call was made.
     * @return An initialised {@link Branch} object.
     */

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public static Branch getAutoTestInstance(Context context) {
        isAutoSessionMode_ = true;
        getBranchInstance(context, false);
        branchReferral_.setActivityLifeCycleObserver((Application)context);
        return branchReferral_;
    }

    /**
     * <p>Initialises an instance of the Branch object.</p>
     *
     * @param context A {@link Context} from which this call was made.
     * @return An initialised {@link Branch} object.
     */
    private static Branch initInstance(Context context) {
        return new Branch(context.getApplicationContext());
    }

    /**
     * <p>Manually sets the {@link Boolean} value, that indicates that the Branch API connection has
     * been initialised, to false - forcing re-initialisation.</p>
     */
    public void resetUserSession() {
        initState_ = SESSION_STATE.UNINITIALISED;
    }

    /**
     * <p>Sets the number of times to re-attempt a timed-out request to the Branch API, before
     * considering the request to have failed entirely. Default 5.</p>
     *
     * @param retryCount An {@link Integer} specifying the number of times to retry before giving
     *                   up and declaring defeat.
     */
    public void setRetryCount(int retryCount) {
        if (prefHelper_ != null && retryCount > 0) {
            prefHelper_.setRetryCount(retryCount);
        }
    }

    /**
     * <p>Sets the amount of time in milliseconds to wait before re-attempting a timed-out request
     * to the Branch API. Default 3000 ms.</p>
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
     * <p>Sets the duration in milliseconds that the system should wait for a response before considering
     * any Branch API call to have timed out. Default 3000 ms.</p>
     * <p/>
     * <p>Increase this to perform better in low network speed situations, but at the expense of
     * responsiveness to error situation.</p>
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
     * <p>Sets the library to function in debug mode, enabling logging of all requests.</p>
     * <p>If you want to flag debug, call this <b>before</b> initUserSession</p>
     */
    public void setDebug() {
        prefHelper_.setExternDebug();
    }

    /**
     * <p>Calls the {@link PrefHelper#disableExternAppListing()} on the local instance to prevent
     * a list of installed apps from being returned to the Branch API.</p>
     */
    public void disableAppList() {
        prefHelper_.disableExternAppListing();
    }


    /**
     * <p>Calls the {@link PrefHelper#disableTouchDebugging()} ()} on the local instance to prevent
     * touch debugging feature.</p>
     */
    public void disableTouchDebugging() {
        prefHelper_.disableTouchDebugging();
    }

    /**
     * <p>If there's further Branch API call happening within the two seconds, we then don't close
     * the session; otherwise, we close the session after two seconds.</p>
     *
     * <p>Call this method if you don't want this smart session feature and would rather manage
     * the session yourself.</p>
     *
     * <p><b>Note:</b>  smart session - we keep session alive for two seconds</p>
     */
    public void disableSmartSession() {
        prefHelper_.disableSmartSession();
    }

    /**
     * <p>Initialises a session with the Branch API, assigning a {@link BranchReferralInitListener}
     * to perform an action upon successful initialisation.</p>
     *
     * @param callback A {@link BranchReferralInitListener} instance that will be called following
     *                 successful (or unsuccessful) initialisation of the session with the Branch API.
     * @return A {@link Boolean} value, indicating <i>false</i> if initialisation is
     * unsuccessful.
     */
    public boolean initSession(BranchReferralInitListener callback) {
        initSession(callback, (Activity) null);
        return false;
    }

    /**
     * <p>Initialises a session with the Branch API, passing the {@link Activity} and assigning a
     * {@link BranchReferralInitListener} to perform an action upon successful initialisation.</p>
     *
     * @param callback A {@link BranchReferralInitListener} instance that will be called
     *                 following successful (or unsuccessful) initialisation of the session
     *                 with the Branch API.
     * @param activity The calling {@link Activity} for context.
     * @return A {@link Boolean} value, indicating <i>false</i> if initialisation is
     * unsuccessful.
     */
    public boolean initSession(BranchReferralInitListener callback, Activity activity) {
        if (systemObserver_.getUpdateState(false) == 0 && !hasUser()) {
            prefHelper_.setIsReferrable();
        } else {
            prefHelper_.clearIsReferrable();
        }
        initUserSessionInternal(callback, activity);
        return false;
    }

    /**
     * <p>Initialises a session with the Branch API.</p>
     *
     * @param callback A {@link BranchReferralInitListener} instance that will be called
     *                 following successful (or unsuccessful) initialisation of the session
     *                 with the Branch API.
     * @param data     A {@link  Uri} variable containing the details of the source link that
     *                 led to this initialisation action.
     * @return A {@link Boolean} value that will return <i>false</i> if the supplied
     * <i>data</i> parameter cannot be handled successfully - i.e. is not of a
     * valid URI format.
     */
    public boolean initSession(BranchReferralInitListener callback, Uri data) {
        return initSession(callback, data, null);
    }

    /**
     * <p>Initialises a session with the Branch API.</p>
     *
     * @param callback A {@link BranchReferralInitListener} instance that will be called
     *                 following successful (or unsuccessful) initialisation of the session
     *                 with the Branch API.
     * @param data     A {@link  Uri} variable containing the details of the source link that
     *                 led to this initialisation action.
     * @param activity The calling {@link Activity} for context.
     * @return A {@link Boolean} value that will return <i>false</i> if the supplied
     * <i>data</i> parameter cannot be handled successfully - i.e. is not of a
     * valid URI format.
     */
    public boolean initSession(BranchReferralInitListener callback, Uri data, Activity activity) {
        boolean uriHandled = readAndStripParam(data, activity);
        initSession(callback, activity);
        return uriHandled;
    }

    /**
     * <p>Initialises a session with the Branch API, without a callback or {@link Activity}.</p>
     *
     * @return A {@link Boolean} value that returns <i>false</i> if unsuccessful.
     */
    public boolean initSession() {
        return initSession((Activity) null);
    }

    /**
     * <p>Initialises a session with the Branch API, without a callback or {@link Activity}.</p>
     *
     * @param activity The calling {@link Activity} for context.
     * @return A {@link Boolean} value that returns <i>false</i> if unsuccessful.
     */
    public boolean initSession(Activity activity) {
        return initSession(null, activity);
    }

    /**
     * <p>Initialises a session with the Branch API, with associated data from the supplied
     * {@link Uri}.</p>
     *
     * @param data A {@link  Uri} variable containing the details of the source link that
     *             led to this
     *             initialisation action.
     * @return A {@link Boolean} value that returns <i>false</i> if unsuccessful.
     */
    public boolean initSessionWithData(Uri data) {
        return initSessionWithData(data, null);
    }

    /**
     * <p>Initialises a session with the Branch API, with associated data from the supplied
     * {@link Uri}.</p>
     *
     * @param data     A {@link  Uri} variable containing the details of the source link that led to this
     *                 initialisation action.
     * @param activity The calling {@link Activity} for context.
     * @return A {@link Boolean} value that returns <i>false</i> if unsuccessful.
     */
    public boolean initSessionWithData(Uri data, Activity activity) {
        boolean uriHandled = readAndStripParam(data, activity);
        initSession(null, activity);
        return uriHandled;
    }

    /**
     * <p>Initialises a session with the Branch API, specifying whether the initialisation can count
     * as a referrable action.</p>
     *
     * @param isReferrable A {@link Boolean} value indicating whether this initialisation
     *                     session should be considered as potentially referrable or not.
     *                     By default, a user is only referrable if initSession results in a
     *                     fresh install. Overriding this gives you control of who is referrable.
     * @return A {@link Boolean} value that returns <i>false</i> if unsuccessful.
     */
    public boolean initSession(boolean isReferrable) {
        return initSession(null, isReferrable, (Activity)null);
    }

    /**
     * <p>Initialises a session with the Branch API, specifying whether the initialisation can count
     * as a referrable action, and supplying the calling {@link Activity} for context.</p>
     *
     * @param isReferrable A {@link Boolean} value indicating whether this initialisation
     *                     session should be considered as potentially referrable or not.
     *                     By default, a user is only referrable if initSession results in a
     *                     fresh install. Overriding this gives you control of who is referrable.
     * @param activity     The calling {@link Activity} for context.
     * @return A {@link Boolean} value that returns <i>false</i> if unsuccessful.
     */
    public boolean initSession(boolean isReferrable, Activity activity) {
        return initSession(null, isReferrable, activity);
    }

    /**
     * <p>Initialises a session with the Branch API.</p>
     *
     * @param callback     A {@link BranchReferralInitListener} instance that will be called
     *                     following successful (or unsuccessful) initialisation of the session
     *                     with the Branch API.
     * @param isReferrable A {@link Boolean} value indicating whether this initialisation
     *                     session should be considered as potentially referrable or not.
     *                     By default, a user is only referrable if initSession results in a
     *                     fresh install. Overriding this gives you control of who is referrable.
     * @param data         A {@link  Uri} variable containing the details of the source link that
     *                     led to this initialisation action.
     * @return A {@link Boolean} value that returns <i>false</i> if unsuccessful.
     */
    public boolean initSession(BranchReferralInitListener callback, boolean isReferrable, Uri data) {
        return initSession(callback, isReferrable, data, null);
    }

    /**
     * <p>Initialises a session with the Branch API.</p>
     *
     * @param callback     A {@link BranchReferralInitListener} instance that will be called
     *                     following successful (or unsuccessful) initialisation of the session
     *                     with the Branch API.
     * @param isReferrable A {@link Boolean} value indicating whether this initialisation
     *                     session should be considered as potentially referrable or not.
     *                     By default, a user is only referrable if initSession results in a
     *                     fresh install. Overriding this gives you control of who is referrable.
     * @param data         A {@link  Uri} variable containing the details of the source link that
     *                     led to this initialisation action.
     * @param activity     The calling {@link Activity} for context.
     * @return A {@link Boolean} value that returns <i>false</i> if unsuccessful.
     */
    public boolean initSession(BranchReferralInitListener callback, boolean isReferrable, Uri data, Activity activity) {
        boolean uriHandled = readAndStripParam(data, activity);
        initSession(callback, isReferrable, activity);
        return uriHandled;
    }

    /**
     * <p>Initialises a session with the Branch API.</p>
     *
     * @param callback     A {@link BranchReferralInitListener} instance that will be called
     *                     following successful (or unsuccessful) initialisation of the session
     *                     with the Branch API.
     * @param isReferrable A {@link Boolean} value indicating whether this initialisation
     *                     session should be considered as potentially referrable or not.
     *                     By default, a user is only referrable if initSession results in a
     *                     fresh install. Overriding this gives you control of who is referrable.
     * @return A {@link Boolean} value that returns <i>false</i> if unsuccessful.
     */
    public boolean initSession(BranchReferralInitListener callback, boolean isReferrable) {
        return initSession(callback, isReferrable, (Activity)null);
    }

    /**
     * <p>Initialises a session with the Branch API.</p>
     *
     * @param callback     A {@link BranchReferralInitListener} instance that will be called
     *                     following successful (or unsuccessful) initialisation of the session
     *                     with the Branch API.
     * @param isReferrable A {@link Boolean} value indicating whether this initialisation
     *                     session should be considered as potentially referrable or not.
     *                     By default, a user is only referrable if initSession results in a
     *                     fresh install. Overriding this gives you control of who is referrable.
     * @param activity     The calling {@link Activity} for context.
     * @return A {@link Boolean} value that returns <i>false</i> if unsuccessful.
     */
    public boolean initSession(BranchReferralInitListener callback, boolean isReferrable, Activity activity) {
        if (isReferrable) {
            this.prefHelper_.setIsReferrable();
        } else {
            this.prefHelper_.clearIsReferrable();
        }
        initUserSessionInternal(callback, activity);
        return false;
    }

    private void initUserSessionInternal(BranchReferralInitListener callback, Activity activity) {
        currentActivity_ = activity;
        //If already initialised
        if (hasUser() && hasSession() && initState_ == SESSION_STATE.INITIALISED) {
            if (callback != null)
                callback.onInitFinished(new JSONObject(), null);
            clearCloseTimer();
            keepAlive();
        }
        //If uninitialised or initialising
        else {
            //If initialising ,then set new callbacks.
            if (initState_ == SESSION_STATE.INITIALISING) {
                requestQueue_.setInstallOrOpenCallback(callback);
            }
            //if Uninitialised move request to the front if there is an existing request or create a new request.
            else {
                initState_ = SESSION_STATE.INITIALISING;
                initializeSession(callback);
            }
        }

        if (prefHelper_.getTouchDebugging()) {
            if (activity != null && debugListenerInitHistory_.get(System.identityHashCode(activity)) == null) {
                debugListenerInitHistory_.put(System.identityHashCode(activity), "init");
                View view = activity.getWindow().getDecorView().findViewById(android.R.id.content);
                if (view != null) {
                    view.setOnTouchListener(debugOnTouchListener_);
                }
            }
        }
    }

    /**
     * <p>Set the current activity window for the debug touch events. Only for internal usage.</p>
     *
     * @param activity The current activity.
     */
    private void setTouchDebugInternal(Activity activity){
        if (activity != null && debugListenerInitHistory_.get(System.identityHashCode(activity)) == null) {
            debugListenerInitHistory_.put(System.identityHashCode(activity), "init");
            activity.getWindow().setCallback(new BranchWindowCallback(activity.getWindow().getCallback()));
        }
    }

    private void clearTouchDebugInternal(Activity activity) {
        if (activity.getWindow().getCallback() instanceof BranchWindowCallback) {
            Window.Callback originalCallback =
                    ((BranchWindowCallback) activity.getWindow().getCallback()).callback_;
            activity.getWindow().setCallback(originalCallback);
            debugListenerInitHistory_.remove(System.identityHashCode(activity));
        }
    }

    private OnTouchListener retrieveOnTouchListener() {
        if (debugOnTouchListener_ == null) {
            debugOnTouchListener_ = new OnTouchListener() {
                class KeepDebugConnectionTask extends TimerTask {
                    public void run() {
                        if (!prefHelper_.keepDebugConnection()) {
                            debugHandler_.post(_longPressed);
                        }
                    }
                }

                Runnable _longPressed = new Runnable() {
                    private boolean started = false;
                    private Timer timer;

                    public void run() {
                        debugHandler_.removeCallbacks(_longPressed);
                        if (!started) {
                            Log.i("Branch Debug","======= Start Debug Session =======");
                            prefHelper_.setDebug();
                            timer = new Timer();
                            timer.scheduleAtFixedRate(new KeepDebugConnectionTask(), new Date(), 20000);
                        } else {
                            Log.i("Branch Debug","======= End Debug Session =======");
                            prefHelper_.clearDebug();
                            timer.cancel();
                            timer = null;
                        }
                        this.started = !this.started;
                    }
                };

                @Override
                public boolean onTouch(View v, MotionEvent ev) {
                    int pointerCount = ev.getPointerCount();
                    final int actionPerformed = ev.getAction();
                    switch (actionPerformed & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN:
                        if (systemObserver_.isSimulator()) {
                            debugHandler_.postDelayed(_longPressed, PrefHelper.DEBUG_TRIGGER_PRESS_TIME);
                        }
                        break;
                    case MotionEvent.ACTION_MOVE:
                        break;
                    case MotionEvent.ACTION_CANCEL:
                        debugHandler_.removeCallbacks(_longPressed);
                        break;
                    case MotionEvent.ACTION_UP:
                        v.performClick();
                        debugHandler_.removeCallbacks(_longPressed);
                        break;
                    case MotionEvent.ACTION_POINTER_DOWN:
                        if (pointerCount == PrefHelper.DEBUG_TRIGGER_NUM_FINGERS) {
                            debugHandler_.postDelayed(_longPressed, PrefHelper.DEBUG_TRIGGER_PRESS_TIME);
                        }
                        break;
                    }
                    return true;
                }
            };
        }
        return debugOnTouchListener_;
    }

    /**
     * <p>Closes the current session, dependent on the state of the
     * {@link PrefHelper#getSmartSession()} {@link Boolean} value. If <i>true</i>, take no action.
     * If false, close the sesion via the {@link #executeClose()} method.</p>
     * <p>Note that if smartSession is enabled, closeSession cannot be called within
     * a 2 second time span of another Branch action. This has to do with the method that
     * Branch uses to keep a session alive during Activity transitions</p>
     */
    public void closeSession() {
        if (isAutoSessionMode_) {
            /*
             * Ignore any session close request from user if session is managed
             * automatically.This is handle situation of closeSession() in
             * closed by developer by error while running in auto session mode.
             */
            return;
        }

        if (prefHelper_.getSmartSession()) {
            if (keepAlive_) {
                return;
            }

            // else, real close
            synchronized(lock) {
                clearCloseTimer();
                rotateCloseTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        executeClose();
                    }
                }, PREVENT_CLOSE_TIMEOUT);
            }
        } else {
            executeClose();
        }

        if (prefHelper_.getExternAppListing()) {
            if (appListingSchedule_ == null) {
                scheduleListOfApps();
            }
        }
        /* Close any opened sharing dialog.*/
        if(shareLinkManager_ != null) {
            shareLinkManager_.cancelShareLinkDialog();
        }
    }

    /*
     * <p>Closes the current session. Should be called by on getting the last actvity onStop() event.
     * </p>
     */
    private void closeSessionInternal(){
        executeClose();
        if (prefHelper_.getExternAppListing()) {
            if (appListingSchedule_ == null) {
                scheduleListOfApps();
            }
        }
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
                if (req != null && (req instanceof ServerRequestRegisterInstall) || (req instanceof ServerRequestRegisterOpen)) {
                    requestQueue_.dequeue();
                }
            } else {
                if (!requestQueue_.containsClose()) {
                    ServerRequest req = new ServerRequestRegisterClose(context_);
                    handleNewRequest(req);
                }
            }
            initState_ = SESSION_STATE.UNINITIALISED;
        }
    }

    private boolean readAndStripParam(Uri data, Activity activity) {
        if (data != null && data.isHierarchical() && activity != null) {
            if (data.getQueryParameter(Defines.Jsonkey.LinkClickID.getKey()) != null) {
                prefHelper_.setLinkClickIdentifier(data.getQueryParameter(Defines.Jsonkey.LinkClickID.getKey()));
                String paramString = "link_click_id=" + data.getQueryParameter(Defines.Jsonkey.LinkClickID.getKey());
                String uriString = activity.getIntent().getDataString();
                if (data.getQuery().length() == paramString.length()) {
                    paramString = "\\?" + paramString;
                } else if ((uriString.length()-paramString.length()) == uriString.indexOf(paramString)) {
                    paramString = "&" + paramString;
                } else {
                    paramString = paramString + "&";
                }
                Uri newData = Uri.parse(uriString.replaceFirst(paramString, ""));
                activity.getIntent().setData(newData);
                return true;
            }
        }
        return false;
    }

    /**
     * <p>Identifies the current user to the Branch API by supplying a unique identifier as a
     * {@link String} value. No callback.</p>
     *
     * @param userId A {@link String} value containing the unique identifier of the user.
     */
    public void setIdentity(String userId) {
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
    public void setIdentity(String userId, BranchReferralInitListener callback) {
        ServerRequest req = new ServerRequestIdentifyUserRequest(context_, callback, userId);
        if (!req.constructError_ && !req.handleErrors(context_)) {
            handleNewRequest(req);
        } else {
            if (((ServerRequestIdentifyUserRequest) req).isExistingID()) {
                ((ServerRequestIdentifyUserRequest) req).handleUserExist(branchReferral_);
            }
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
        ServerRequest req = new ServerRequestLogout(context_);
        if (!req.constructError_ && !req.handleErrors(context_)) {
            handleNewRequest(req);
        }
    }

    /**
     * <p>Fire-and-forget retrieval of action count for the current session. Without a callback.</p>
     */
    public void loadActionCounts() {
        loadActionCounts(null);
    }

    /**
     * <p>Gets the total action count, with a callback to perform a predefined
     * action following successful report of state change. You'll then need to
     * call getUniqueActions or getTotalActions in the callback to update the
     * totals in your UX.</p>
     *
     * @param callback A {@link BranchReferralStateChangedListener} callback instance that will
     *                 trigger actions defined therein upon a referral state change.
     */
    public void loadActionCounts(BranchReferralStateChangedListener callback) {
        ServerRequest req = new ServerRequestGetReferralCount(context_, callback);
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
     * <p>Gets the total number of times that the specified action has been carried out.</p>
     *
     * @param action A {@link String} value containing the name of the action to count.
     * @return An {@link Integer} value of the total number of times that an action has
     * been executed.
     */
    public int getTotalCountsForAction(String action) {
        return prefHelper_.getActionTotalCount(action);
    }

    /**
     * <p>Gets the number of unique times that the specified action has been carried out.</p>
     *
     * @param action A {@link String} value containing the name of the action to count.
     * @return An {@link Integer} value of the number of unique times that the
     * specified action has been carried out.
     */
    public int getUniqueCountsForAction(String action) {
        return prefHelper_.getActionUniqueCount(action);
    }

    /**
     * <p>Redeems the specified number of credits from the "default" bucket, if there are sufficient
     * credits within it. If the number to redeem exceeds the number available in the bucket, all of
     * the available credits will be redeemed instead.</p>
     *
     * @param count    A {@link Integer} specifying the number of credits to attempt to redeem from
     *                 the bucket.
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
     *
     * @param count  A {@link Integer} specifying the number of credits to attempt to redeem from
     *               the specified bucket.
     */
    public void redeemRewards(final String bucket, final int count) {
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
    public void redeemRewards(final String bucket, final int count, BranchReferralStateChangedListener callback) {
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
    public void getCreditHistory(final String bucket, BranchListResponseListener callback) {
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
     *                 <p/>
     *                 <p>Valid choices:</p>
     *                 <p/>
     *                 <ul>
     *                 <li>{@link CreditHistoryOrder#kMostRecentFirst}</li>
     *                 <li>{@link CreditHistoryOrder#kLeastRecentFirst}</li>
     *                 </ul>
     * @param callback A {@link BranchListResponseListener} callback instance that will trigger
     *                 actions defined therein upon receipt of a response to a create link request.
     */
    public void getCreditHistory(final String afterId, final int length, final CreditHistoryOrder order, BranchListResponseListener callback) {
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
     *                 <p/>
     *                 <p>Valid choices:</p>
     *                 <p/>
     *                 <ul>
     *                 <li>{@link CreditHistoryOrder#kMostRecentFirst}</li>
     *                 <li>{@link CreditHistoryOrder#kLeastRecentFirst}</li>
     *                 </ul>
     * @param callback A {@link BranchListResponseListener} callback instance that will trigger
     *                 actions defined therein upon receipt of a response to a create link request.
     */
    public void getCreditHistory(final String bucket, final String afterId, final int length, final CreditHistoryOrder order, BranchListResponseListener callback) {
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
    public void userCompletedAction(final String action, JSONObject metadata) {
        if (metadata != null)
            metadata = filterOutBadCharacters(metadata);

        ServerRequest req = new ServerRequestActionCompleted(context_, action, metadata);
        if (!req.constructError_ && !req.handleErrors(context_)) {
            handleNewRequest(req);
        }
    }

    /**
     * <p>A void call to indicate that the user has performed a specific action and for that to be
     * reported to the Branch API.</p>
     *
     * @param action A {@link String} value to be passed as an action that the user has carried
     *               out. For example "registered" or "logged in".
     */
    public void userCompletedAction(final String action) {
        userCompletedAction(action, null);
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
        return convertParamsStringToDictionary(storedParam);
    }

    /**
     * <p>Returns the parameters associated with the link that referred the session. If a user
     * clicks a link, and then opens the app, initSession will return the paramters of the link
     * and then set them in as the latest parameters to be retrieved by this method. By default,
     * sessions persist for the duration of time that the app is in focus. For example, if you
     * minimize the app, these parameters will be cleared when closeSession is called.</p>
     *
     * @return A {@link JSONObject} containing the latest referring parameters as
     * configured locally.
     */
    public JSONObject getLatestReferringParams() {
        String storedParam = prefHelper_.getSessionParams();
        return convertParamsStringToDictionary(storedParam);
    }

    /**
     * <p>Configures and requests a short URL to be generated by the Branch servers, via a synchronous
     * call; with a duration specified within which an app session should be matched to the link.</p>
     *
     * @return A {@link String} containing the resulting short URL.
     */
    public String getShortUrlSync() {
        return generateShortLink(null, LINK_TYPE_UNLIMITED_USE, 0, null, null, null, null, stringifyParams(null), null, false);
    }

    /**
     * <p>Configures and requests a short URL to be generated by the Branch servers, via a synchronous
     * call; with a duration specified within which an app session should be matched to the link.</p>
     *
     * @param params A {@link JSONObject} value containing the deep linked params associated with
     *               the link that will be passed into a new app session when clicked
     * @return A {@link String} containing the resulting short URL.
     */
    public String getShortUrlSync(JSONObject params) {
        return generateShortLink(null, LINK_TYPE_UNLIMITED_USE, 0, null, null, null, null, stringifyParams(filterOutBadCharacters(params)), null, false);
    }

    /**
     * <p>Configures and requests a referral URL to be generated by the Branch servers, via a synchronous
     * call; with a duration specified within which an app session should be matched to the link.</p>
     *
     * @param channel A {@link String} denoting the channel that the link belongs to. Should not
     *                exceed 128 characters.
     * @param params  A {@link JSONObject} value containing the deep linked params associated with
     *                the link that will be passed into a new app session when clicked
     * @return A {@link String} containing the resulting referral URL.
     */
    public String getReferralUrlSync(String channel, JSONObject params) {
        return generateShortLink(null, LINK_TYPE_UNLIMITED_USE, 0, null, channel, FEATURE_TAG_REFERRAL, null, stringifyParams(filterOutBadCharacters(params)), null, false);
    }

    /**
     * <p>Configures and requests a referral URL to be generated by the Branch servers, via a synchronous
     * call; with a duration specified within which an app session should be matched to the link.</p>
     *
     * @param tags    An iterable {@link Collection} of {@link String} tags associated with a deep
     *                link.
     * @param channel A {@link String} denoting the channel that the link belongs to. Should not
     *                exceed 128 characters.
     * @param params  A {@link JSONObject} value containing the deep linked params associated with
     *                the link that will be passed into a new app session when clicked
     * @return A {@link String} containing the resulting referral URL.
     */
    public String getReferralUrlSync(Collection<String> tags, String channel, JSONObject params) {
        return generateShortLink(null, LINK_TYPE_UNLIMITED_USE, 0, tags, channel, FEATURE_TAG_REFERRAL, null, stringifyParams(filterOutBadCharacters(params)), null, false);
    }

    /**
     * <p>Configures and requests a content URL (defined as feature = sharing) to be generated by the Branch servers, via a synchronous
     * call</p>
     *
     * @param channel A {@link String} denoting the channel that the link belongs to. Should not
     *                exceed 128 characters.
     * @param params  A {@link JSONObject} value containing the deep linked params associated with
     *                the link that will be passed into a new app session when clicked
     * @return A {@link String} containing the resulting content URL.
     */
    public String getContentUrlSync(String channel, JSONObject params) {
        return generateShortLink(null, LINK_TYPE_UNLIMITED_USE, 0, null, channel, FEATURE_TAG_SHARE, null, stringifyParams(filterOutBadCharacters(params)), null, false);
    }

    /**
     * <p>Configures and requests a content URL (defined as feature = sharing) to be generated by the Branch servers, via a synchronous
     * call</p>
     *
     * @param tags    An iterable {@link Collection} of {@link String} tags associated with a deep
     *                link.
     * @param channel A {@link String} denoting the channel that the link belongs to. Should not
     *                exceed 128 characters.
     * @param params  A {@link JSONObject} value containing the deep linked params associated with
     *                the link that will be passed into a new app session when clicked
     * @return A {@link String} containing the resulting content URL.
     */
    public String getContentUrlSync(Collection<String> tags, String channel, JSONObject params) {
        return generateShortLink(null, LINK_TYPE_UNLIMITED_USE, 0, tags, channel, FEATURE_TAG_SHARE, null, stringifyParams(filterOutBadCharacters(params)), null, false);
    }

    /**
     * <p>Configures and requests a short URL to be generated by the Branch servers, via a synchronous
     * call; with a duration specified within which an app session should be matched to the link.</p>
     *
     * @param channel A {@link String} denoting the channel that the link belongs to. Should not
     *                exceed 128 characters.
     * @param feature A {@link String} value identifying the feature that the link makes use of.
     *                Should not exceed 128 characters.
     * @param stage   A {@link String} value identifying the stage in an application or user flow process.
     *                Should not exceed 128 characters.
     * @param params  A {@link JSONObject} value containing the deep linked params associated with
     *                the link that will be passed into a new app session when clicked
     * @return A {@link String} containing the resulting short URL.
     */
    public String getShortUrlSync(String channel, String feature, String stage, JSONObject params) {
        return generateShortLink(null, LINK_TYPE_UNLIMITED_USE, 0, null, channel, feature, stage, stringifyParams(filterOutBadCharacters(params)), null, false);
    }

    /**
     * <p>Configures and requests a short URL to be generated by the Branch servers, via a synchronous
     * call; with a duration specified within which an app session should be matched to the link.</p>
     *
     * @param alias   Link 'alias' can be used to label the endpoint on the link.
     *                <p/>
     *                <p>
     *                For example:
     *                http://bnc.lt/AUSTIN28.
     *                Should not exceed 128 characters
     *                </p>
     * @param channel A {@link String} denoting the channel that the link belongs to. Should not
     *                exceed 128 characters.
     * @param feature A {@link String} value identifying the feature that the link makes use of.
     *                Should not exceed 128 characters.
     * @param stage   A {@link String} value identifying the stage in an application or user flow
     *                process. Should not exceed 128 characters.
     * @param params  A {@link JSONObject} value containing the deep linked params associated with
     *                the link that will be passed into a new app session when clicked
     * @return A {@link String} containing the resulting short URL.
     */
    public String getShortUrlSync(String alias, String channel, String feature, String stage, JSONObject params) {
        return generateShortLink(alias, LINK_TYPE_UNLIMITED_USE, 0, null, channel, feature, stage, stringifyParams(filterOutBadCharacters(params)), null, false);
    }

    /**
     * <p>Configures and requests a short URL to be generated by the Branch servers, via a synchronous
     * call; with a duration specified within which an app session should be matched to the link.</p>
     *
     * @param type    An {@link int} that can be used for scenarios where you want the link to
     *                only deep link the first time.
     * @param channel A {@link String} denoting the channel that the link belongs to. Should not
     *                exceed 128 characters.
     * @param feature A {@link String} value identifying the feature that the link makes use of.
     *                Should not exceed 128 characters.
     * @param stage   A {@link String} value identifying the stage in an application or user flow
     *                process. Should not exceed 128 characters.
     * @param params  A {@link JSONObject} value containing the deep linked params associated with
     *                the link that will be passed into a new app session when clicked
     * @return A {@link String} containing the resulting short URL.
     */
    public String getShortUrlSync(int type, String channel, String feature, String stage, JSONObject params) {
        return generateShortLink(null, type, 0, null, channel, feature, stage, stringifyParams(filterOutBadCharacters(params)), null, false);
    }

    /**
     * <p>Configures and requests a short URL to be generated by the Branch servers, via a synchronous
     * call; with a duration specified within which an app session should be matched to the link.</p>
     *
     * @param channel  A {@link String} denoting the channel that the link belongs to. Should not
     *                 exceed 128 characters.
     * @param feature  A {@link String} value identifying the feature that the link makes use of.
     *                 Should not exceed 128 characters.
     * @param stage    A {@link String} value identifying the stage in an application or user flow
     *                 process. Should not exceed 128 characters.
     * @param params   A {@link JSONObject} value containing the deep linked params associated with
     *                 the link that will be passed into a new app session when clicked
     * @param duration A {@link Integer} value specifying the time that Branch allows a click to
     *                 remain outstanding and be eligible to be matched with a new app session.
     * @return A {@link String} containing the resulting short URL.
     */
    public String getShortUrlSync(String channel, String feature, String stage, JSONObject params, int duration) {
        return generateShortLink(null, LINK_TYPE_UNLIMITED_USE, duration, null, channel, feature, stage, stringifyParams(filterOutBadCharacters(params)), null, false);
    }

    /**
     * <p>Configures and requests a short URL to be generated by the Branch servers, via a synchronous
     * call; with a duration specified within which an app session should be matched to the link.</p>
     *
     * @param tags    An iterable {@link Collection} of {@link String} tags associated with a deep
     *                link.
     * @param channel A {@link String} denoting the channel that the link belongs to. Should not
     *                exceed 128 characters.
     * @param feature A {@link String} value identifying the feature that the link makes use of.
     *                Should not exceed 128 characters.
     * @param stage   A {@link String} value identifying the stage in an application or user flow
     *                process. Should not exceed 128 characters.
     * @param params  A {@link JSONObject} value containing the deep linked params associated with
     *                the link that will be passed into a new app session when clicked
     * @return A {@link String} containing the resulting short URL.
     */
    public String getShortUrlSync(Collection<String> tags, String channel, String feature, String stage, JSONObject params) {
        return generateShortLink(null, LINK_TYPE_UNLIMITED_USE, 0, tags, channel, feature, stage, stringifyParams(filterOutBadCharacters(params)), null, false);
    }

    /**
     * <p>Configures and requests a short URL to be generated by the Branch servers, via a synchronous
     * call; with a duration specified within which an app session should be matched to the link.</p>
     *
     * @param alias   Link 'alias' can be used to label the endpoint on the link.
     *                <p/>
     *                <p>
     *                For example:
     *                http://bnc.lt/AUSTIN28.
     *                Should not exceed 128 characters
     *                </p>
     * @param tags    An iterable {@link Collection} of {@link String} tags associated with a deep
     *                link.
     * @param channel A {@link String} denoting the channel that the link belongs to. Should not
     *                exceed 128 characters.
     * @param feature A {@link String} value identifying the feature that the link makes use of.
     *                Should not exceed 128 characters.
     * @param stage   A {@link String} value identifying the stage in an application or user flow
     *                process. Should not exceed 128 characters.
     * @param params  A {@link JSONObject} value containing the deep linked params associated with
     *                the link that will be passed into a new app session when clicked
     * @return A {@link String} containing the resulting short URL.
     */
    public String getShortUrlSync(String alias, Collection<String> tags, String channel, String feature, String stage, JSONObject params) {
        return generateShortLink(alias, LINK_TYPE_UNLIMITED_USE, 0, tags, channel, feature, stage, stringifyParams(filterOutBadCharacters(params)), null, false);
    }

    /**
     * <p>Configures and requests a short URL to be generated by the Branch servers, via a synchronous
     * call; with a duration specified within which an app session should be matched to the link.</p>
     *
     * @param type    An {@link int} that can be used for scenarios where you want the link to
     *                only deep link the first time.
     * @param tags    An iterable {@link Collection} of {@link String} tags associated with a deep
     *                link.
     * @param channel A {@link String} denoting the channel that the link belongs to. Should not
     *                exceed 128 characters.
     * @param feature A {@link String} value identifying the feature that the link makes use of.
     *                Should not exceed 128 characters.
     * @param stage   A {@link String} value identifying the stage in an application or user flow
     *                process. Should not exceed 128 characters.
     * @param params  A {@link JSONObject} value containing the deep linked params associated with
     *                the link that will be passed into a new app session when clicked
     * @return A {@link String} containing the resulting short URL.
     */
    public String getShortUrlSync(int type, Collection<String> tags, String channel, String feature, String stage, JSONObject params) {
        return generateShortLink(null, type, 0, tags, channel, feature, stage, stringifyParams(filterOutBadCharacters(params)), null, false);
    }

    /**
     * <p>Configures and requests a short URL to be generated by the Branch servers, via a synchronous
     * call; with a duration specified within which an app session should be matched to the link.</p>
     *
     * @param tags     An iterable {@link Collection} of {@link String} tags associated with a deep
     *                 link.
     * @param channel  A {@link String} denoting the channel that the link belongs to. Should not
     *                 exceed 128 characters.
     * @param feature  A {@link String} value identifying the feature that the link makes use of.
     *                 Should not exceed 128 characters.
     * @param stage    A {@link String} value identifying the stage in an application or user flow
     *                 process. Should not exceed 128 characters.
     * @param params   A {@link JSONObject} value containing the deep linked params associated with
     *                 the link that will be passed into a new app session when clicked
     * @param duration A {@link Integer} value specifying the time that Branch allows a click to
     *                 remain outstanding and be eligible to be matched with a new app session.
     * @return A {@link String} containing the resulting short URL.
     */
    public String getShortUrlSync(Collection<String> tags, String channel, String feature, String stage, JSONObject params, int duration) {
        return generateShortLink(null, LINK_TYPE_UNLIMITED_USE, duration, tags, channel, feature, stage, stringifyParams(filterOutBadCharacters(params)), null, false);
    }

    /**
     * <p>Configures and requests a short URL to be generated by the Branch servers.</p>
     *
     * @param callback A {@link BranchLinkCreateListener} callback instance that will trigger
     *                 actions defined therein upon receipt of a response to a create link request.
     */
    public void getShortUrl(BranchLinkCreateListener callback) {
        generateShortLink(null, LINK_TYPE_UNLIMITED_USE, 0, null, null, null, null, stringifyParams(null), callback, true);
    }

    /**
     * <p>Configures and requests a short URL to be generated by the Branch servers.</p>
     *
     * @param params   A {@link JSONObject} value containing the deep linked params associated with
     *                 the link that will be passed into a new app session when clicked
     * @param callback A {@link BranchLinkCreateListener} callback instance that will trigger
     *                 actions defined therein upon receipt of a response to a create link request.
     * @see BranchLinkData
     * @see BranchLinkData#putParams(String)
     * @see BranchLinkCreateListener
     */
    public void getShortUrl(JSONObject params, BranchLinkCreateListener callback) {
        generateShortLink(null, LINK_TYPE_UNLIMITED_USE, 0, null, null, null, null, stringifyParams(filterOutBadCharacters(params)), callback, true);
    }

    /**
     * <p>Configures and requests a referral URL (feature = referral) to be generated by the Branch servers.</p>
     *
     * @param channel  A {@link String} denoting the channel that the link belongs to. Should not
     *                 exceed 128 characters.
     * @param params   A {@link JSONObject} value containing the deep linked params associated with
     *                 the link that will be passed into a new app session when clicked
     * @param callback A {@link BranchLinkCreateListener} callback instance that will trigger
     *                 actions defined therein upon receipt of a response to a create link request.
     * @see BranchLinkData
     * @see BranchLinkData#putChannel(String)
     * @see BranchLinkData#putParams(String)
     * @see BranchLinkCreateListener
     */
    public void getReferralUrl(String channel, JSONObject params, BranchLinkCreateListener callback) {
        generateShortLink(null, LINK_TYPE_UNLIMITED_USE, 0, null, channel, FEATURE_TAG_REFERRAL, null, stringifyParams(filterOutBadCharacters(params)), callback, true);
    }

    /**
     * <p>Configures and requests a referral URL (feature = referral) to be generated by the Branch servers.</p>
     *
     * @param tags     An iterable {@link Collection} of {@link String} tags associated with a deep
     *                 link.
     * @param channel  A {@link String} denoting the channel that the link belongs to. Should not
     *                 exceed 128 characters.
     * @param params   A {@link JSONObject} value containing the deep linked params associated with
     *                 the link that will be passed into a new app session when clicked
     * @param callback A {@link BranchLinkCreateListener} callback instance that will trigger
     *                 actions defined therein upon receipt of a response to a create link request.
     * @see BranchLinkData
     * @see BranchLinkData#putTags(Collection)
     * @see BranchLinkData#putChannel(String)
     * @see BranchLinkData#putParams(String)
     * @see BranchLinkCreateListener
     */
    public void getReferralUrl(Collection<String> tags, String channel, JSONObject params, BranchLinkCreateListener callback) {
        generateShortLink(null, LINK_TYPE_UNLIMITED_USE, 0, tags, channel, FEATURE_TAG_REFERRAL, null, stringifyParams(filterOutBadCharacters(params)), callback, true);
    }

    /**
     * <p>Configures and requests a content URL (defined as feature = sharing) to be generated by the Branch servers.</p>
     *
     * @param channel  A {@link String} denoting the channel that the link belongs to. Should not
     *                 exceed 128 characters.
     * @param params   A {@link JSONObject} value containing the deep linked params associated with
     *                 the link that will be passed into a new app session when clicked
     * @param callback A {@link BranchLinkCreateListener} callback instance that will trigger
     *                 actions defined therein upon receipt of a response to a create link request.
     * @see BranchLinkData
     * @see BranchLinkData#putChannel(String)
     * @see BranchLinkData#putParams(String)
     * @see BranchLinkCreateListener
     */
    public void getContentUrl(String channel, JSONObject params, BranchLinkCreateListener callback) {
        generateShortLink(null, LINK_TYPE_UNLIMITED_USE, 0, null, channel, FEATURE_TAG_SHARE, null, stringifyParams(filterOutBadCharacters(params)), callback, true);
    }

    /**
     * <p>Configures and requests a content URL (defined as feature = sharing) to be generated by the Branch servers.</p>
     *
     * @param tags     An iterable {@link Collection} of {@link String} tags associated with a deep
     *                 link.
     * @param channel  A {@link String} denoting the channel that the link belongs to. Should not
     *                 exceed 128 characters.
     * @param params   A {@link JSONObject} value containing the deep linked params associated with
     *                 the link that will be passed into a new app session when clicked
     * @param callback A {@link BranchLinkCreateListener} callback instance that will trigger
     *                 actions defined therein upon receipt of a response to a create link request.
     * @see BranchLinkData
     * @see BranchLinkData#putTags(Collection)
     * @see BranchLinkData#putChannel(String)
     * @see BranchLinkData#putParams(String)
     * @see BranchLinkCreateListener
     */
    public void getContentUrl(Collection<String> tags, String channel, JSONObject params, BranchLinkCreateListener callback) {
        generateShortLink(null, LINK_TYPE_UNLIMITED_USE, 0, tags, channel, FEATURE_TAG_SHARE, null, stringifyParams(filterOutBadCharacters(params)), callback, true);
    }

    /**
     * <p>Configures and requests a short URL to be generated by the Branch servers.</p>
     *
     * @param channel  A {@link String} denoting the channel that the link belongs to. Should not
     *                 exceed 128 characters.
     * @param feature  A {@link String} value identifying the feature that the link makes use of.
     *                 Should not exceed 128 characters.
     * @param stage    A {@link String} value identifying the stage in an application or user flow
     *                 process. Should not exceed 128 characters.
     * @param params   A {@link JSONObject} value containing the deep linked params associated with
     *                 the link that will be passed into a new app session when clicked
     * @param callback A {@link BranchLinkCreateListener} callback instance that will trigger
     *                 actions defined therein upon receipt of a response to a create link request.
     * @see BranchLinkData
     * @see BranchLinkData#putChannel(String)
     * @see BranchLinkData#putFeature(String)
     * @see BranchLinkData#putStage(String)
     * @see BranchLinkData#putParams(String)
     * @see BranchLinkCreateListener
     */
    public void getShortUrl(String channel, String feature, String stage, JSONObject params, BranchLinkCreateListener callback) {
        generateShortLink(null, LINK_TYPE_UNLIMITED_USE, 0, null, channel, feature, stage, stringifyParams(filterOutBadCharacters(params)), callback, true);
    }

    /**
     * <p>Configures and requests a short URL to be generated by the Branch servers.</p>
     *
     * @param alias    Link 'alias' can be used to label the endpoint on the link.
     *                 <p/>
     *                 <p>
     *                 For example:
     *                 http://bnc.lt/AUSTIN28.
     *                 Should not exceed 128 characters
     *                 </p>
     * @param channel  A {@link String} denoting the channel that the link belongs to. Should not
     *                 exceed 128 characters.
     * @param feature  A {@link String} value identifying the feature that the link makes use of.
     *                 Should not exceed 128 characters.
     * @param stage    A {@link String} value identifying the stage in an application or user flow process.
     *                 Should not exceed 128 characters.
     * @param params   A {@link JSONObject} value containing the deep linked params associated with
     *                 the link that will be passed into a new app session when clicked
     * @param callback A {@link BranchLinkCreateListener} callback instance that will trigger
     *                 actions defined therein upon receipt of a response to a create link request.
     * @see BranchLinkData
     * @see BranchLinkData#putAlias(String)
     * @see BranchLinkData#putChannel(String)
     * @see BranchLinkData#putFeature(String)
     * @see BranchLinkData#putStage(String)
     * @see BranchLinkData#putParams(String)
     * @see BranchLinkCreateListener
     */
    public void getShortUrl(String alias, String channel, String feature, String stage, JSONObject params, BranchLinkCreateListener callback) {
        generateShortLink(alias, LINK_TYPE_UNLIMITED_USE, 0, null, channel, feature, stage, stringifyParams(filterOutBadCharacters(params)), callback, true);
    }

    /**
     * <p>Configures and requests a short URL to be generated by the Branch servers.</p>
     *
     * @param type     An {@link int} that can be used for scenarios where you want the link to
     *                 only deep link the first time.
     * @param channel  A {@link String} denoting the channel that the link belongs to. Should not
     *                 exceed 128 characters.
     * @param feature  A {@link String} value identifying the feature that the link makes use of.
     *                 Should not exceed 128 characters.
     * @param stage    A {@link String} value identifying the stage in an application or user flow process.
     *                 Should not exceed 128 characters.
     * @param params   A {@link JSONObject} value containing the deep linked params associated with
     *                 the link that will be passed into a new app session when clicked
     * @param callback A {@link BranchLinkCreateListener} callback instance that will trigger
     *                 actions defined therein upon receipt of a response to a create link request.
     * @see BranchLinkData
     * @see BranchLinkData#putType(int)
     * @see BranchLinkData#putChannel(String)
     * @see BranchLinkData#putFeature(String)
     * @see BranchLinkData#putStage(String)
     * @see BranchLinkData#putParams(String)
     * @see BranchLinkCreateListener
     */
    public void getShortUrl(int type, String channel, String feature, String stage, JSONObject params, BranchLinkCreateListener callback) {
        generateShortLink(null, type, 0, null, channel, feature, stage, stringifyParams(filterOutBadCharacters(params)), callback, true);
    }

    /**
     * <p>Configures and requests a short URL to be generated by the Branch servers.</p>
     *
     * @param channel  A {@link String} denoting the channel that the link belongs to. Should not
     *                 exceed 128 characters.
     * @param feature  A {@link String} value identifying the feature that the link makes use of.
     *                 Should not exceed 128 characters.
     * @param stage    A {@link String} value identifying the stage in an application or user flow
     *                 process. Should not exceed 128 characters.
     * @param params   A {@link JSONObject} value containing the deep linked params associated with
     *                 the link that will be passed into a new app session when clicked
     * @param duration An {@link int} the time that Branch allows a click to remain outstanding and
     *                 be eligible to be matched with a new app session.
     * @param callback A {@link BranchLinkCreateListener} callback instance that will trigger
     *                 actions defined therein upon receipt of a response to a create link request.
     * @see BranchLinkData
     * @see BranchLinkData#putTags(Collection)
     * @see BranchLinkData#putChannel(String)
     * @see BranchLinkData#putFeature(String)
     * @see BranchLinkData#putStage(String)
     * @see BranchLinkData#putParams(String)
     * @see BranchLinkCreateListener
     */
    public void getShortUrl(String channel, String feature, String stage, JSONObject params, int duration, BranchLinkCreateListener callback) {
        generateShortLink(null, LINK_TYPE_UNLIMITED_USE, duration, null, channel, feature, stage, stringifyParams(filterOutBadCharacters(params)), callback, true);
    }

    /**
     * <p>Configures and requests a short URL to be generated by the Branch servers.</p>
     *
     * @param tags     An iterable {@link Collection} of {@link String} tags associated with a deep
     *                 link.
     * @param channel  A {@link String} denoting the channel that the link belongs to. Should not
     *                 exceed 128 characters.
     * @param feature  A {@link String} value identifying the feature that the link makes use of.
     *                 Should not exceed 128 characters.
     * @param stage    A {@link String} value identifying the stage in an application or user flow
     *                 process. Should not exceed 128 characters.
     * @param params   A {@link JSONObject} value containing the deep linked params associated with
     *                 the link that will be passed into a new app session when clicked
     * @param callback A {@link BranchLinkCreateListener} callback instance that will trigger
     *                 actions defined therein upon receipt of a response to a create link request.
     * @see BranchLinkData
     * @see BranchLinkData#putTags(Collection)
     * @see BranchLinkData#putChannel(String)
     * @see BranchLinkData#putFeature(String)
     * @see BranchLinkData#putStage(String)
     * @see BranchLinkData#putParams(String)
     * @see BranchLinkCreateListener
     */
    public void getShortUrl(Collection<String> tags, String channel, String feature, String stage, JSONObject params, BranchLinkCreateListener callback) {
        generateShortLink(null, LINK_TYPE_UNLIMITED_USE, 0, tags, channel, feature, stage, stringifyParams(filterOutBadCharacters(params)), callback, true);
    }

    /**
     * <p>Configures and requests a short URL to be generated by the Branch servers.</p>
     *
     * @param alias    Link 'alias' can be used to label the endpoint on the link.
     *                 Should not exceed 128 characters.
     *                 <p style="margin-left:40px;">
     *                 For example:
     *                 http://bnc.lt/AUSTIN28.</p>
     * @param tags     An iterable {@link Collection} of {@link String} tags associated with a deep
     *                 link.
     * @param channel  A {@link String} denoting the channel that the link belongs to. Should not
     *                 exceed 128 characters.
     * @param feature  A {@link String} value identifying the feature that the link makes use of.
     *                 Should not exceed 128 characters.
     * @param stage    A {@link String} value identifying the stage in an application or user flow
     *                 process. Should not exceed 128 characters.
     * @param params   A {@link JSONObject} value containing the deep linked params associated with
     *                 the link that will be passed into a new app session when clicked
     * @param callback A {@link BranchLinkCreateListener} callback instance that will trigger
     *                 actions defined therein upon receipt of a response to a create link request.
     * @see BranchLinkData
     * @see BranchLinkData#putAlias(String)
     * @see BranchLinkData#putTags(Collection)
     * @see BranchLinkData#putChannel(String)
     * @see BranchLinkData#putFeature(String)
     * @see BranchLinkData#putStage(String)
     * @see BranchLinkData#putParams(String)
     * @see BranchLinkCreateListener
     */
    public void getShortUrl(String alias, Collection<String> tags, String channel, String feature, String stage, JSONObject params, BranchLinkCreateListener callback) {
        generateShortLink(alias, LINK_TYPE_UNLIMITED_USE, 0, tags, channel, feature, stage, stringifyParams(filterOutBadCharacters(params)), callback, true);
    }

    /**
     * <p>Configures and requests a short URL to be generated by the Branch servers.</p>
     *
     * @param type     An {@link int} that can be used for scenarios where you want the link to
     *                 only deep link the first time.
     * @param tags     An iterable {@link Collection} of {@link String} tags associated with a deep
     *                 link.
     * @param channel  A {@link String} denoting the channel that the link belongs to. Should not
     *                 exceed 128 characters.
     * @param feature  A {@link String} value identifying the feature that the link makes use of.
     *                 Should not exceed 128 characters.
     * @param stage    A {@link String} value identifying the stage in an application or user flow
     *                 process. Should not exceed 128 characters.
     * @param params   A {@link JSONObject} value containing the deep linked params associated with
     *                 the link that will be passed into a new app session when clicked
     * @param callback A {@link BranchLinkCreateListener} callback instance that will trigger
     *                 actions defined therein upon receipt of a response to a create link request.
     * @see BranchLinkData
     * @see BranchLinkData#putType(int)
     * @see BranchLinkData#putTags(Collection)
     * @see BranchLinkData#putChannel(String)
     * @see BranchLinkData#putFeature(String)
     * @see BranchLinkData#putStage(String)
     * @see BranchLinkData#putParams(String)
     * @see BranchLinkCreateListener
     */
    public void getShortUrl(int type, Collection<String> tags, String channel, String feature, String stage, JSONObject params, BranchLinkCreateListener callback) {
        generateShortLink(null, type, 0, tags, channel, feature, stage, stringifyParams(filterOutBadCharacters(params)), callback, true);
    }

    /**
     * <p>Configures and requests a short URL to be generated by the Branch servers.</p>
     *
     * @param tags     An iterable {@link Collection} of {@link String} tags associated with a deep
     *                 link.
     * @param channel  A {@link String} denoting the channel that the link belongs to. Should not
     *                 exceed 128 characters.
     * @param feature  A {@link String} value identifying the feature that the link makes use of.
     *                 Should not exceed 128 characters.
     * @param stage    A {@link String} value identifying the stage in an application or user flow
     *                 process. Should not exceed 128 characters.
     * @param params   A {@link JSONObject} value containing the deep linked params associated with
     *                 the link that will be passed into a new app session when clicked
     * @param duration An {@link int} the time that Branch allows a click to remain outstanding
     *                 and be eligible to be matched with a new app session.
     * @param callback A {@link BranchLinkCreateListener} callback instance that will trigger
     *                 actions defined therein upon receipt of a response to a create link request.
     * @see BranchLinkData
     * @see BranchLinkData#putTags(Collection)
     * @see BranchLinkData#putChannel(String)
     * @see BranchLinkData#putFeature(String)
     * @see BranchLinkData#putStage(String)
     * @see BranchLinkData#putParams(String)
     * @see BranchLinkData#putDuration(int)
     * @see BranchLinkCreateListener
     */
    public void getShortUrl(Collection<String> tags, String channel, String feature, String stage, JSONObject params, int duration, BranchLinkCreateListener callback) {
        generateShortLink(null, LINK_TYPE_UNLIMITED_USE, duration, tags, channel, feature, stage, stringifyParams(filterOutBadCharacters(params)), callback, true);
    }

    /**
     * <p>Configures and requests a referral code to be generated by the Branch servers.</p>
     *
     * @param callback A {@link BranchReferralInitListener} callback instance that will trigger
     *                 actions defined therein upon receipt of a response to a referral code request.
     */
    public void getReferralCode(BranchReferralInitListener callback) {
        ServerRequest req = new ServerRequestGetReferralCode(context_, callback);
        if (!req.constructError_ && !req.handleErrors(context_)) {
            handleNewRequest(req);
        }
    }

    /**
     * <p>Configures and requests a referral code to be generated by the Branch servers.</p>
     *
     * @param amount   An {@link Integer} value of credits associated with this referral code.
     * @param callback A {@link BranchReferralInitListener} callback instance that will trigger
     *                 actions defined therein upon receipt of a response to a referral code request.
     */
    public void getReferralCode(final int amount, BranchReferralInitListener callback) {
        this.getReferralCode(null, amount, null, REFERRAL_BUCKET_DEFAULT, REFERRAL_CODE_AWARD_UNLIMITED, REFERRAL_CODE_LOCATION_REFERRING_USER, callback);
    }

    /**
     * <p>Configures and requests a referral code to be generated by the Branch servers.</p>
     *
     * @param prefix   A {@link String} containing the developer-specified prefix code to be applied
     *                 to the start of a referral code. e.g. for code OFFER4867, the prefix would
     *                 be "OFFER".
     * @param amount   An {@link Integer} value of credits associated with this referral code.
     * @param callback A {@link BranchReferralInitListener} callback instance that will trigger
     *                 actions defined therein upon receipt of a response to a referral code request.
     */
    public void getReferralCode(final String prefix, final int amount, BranchReferralInitListener callback) {
        this.getReferralCode(prefix, amount, null, REFERRAL_BUCKET_DEFAULT, REFERRAL_CODE_AWARD_UNLIMITED, REFERRAL_CODE_LOCATION_REFERRING_USER, callback);
    }

    /**
     * <p>Configures and requests a referral code to be generated by the Branch servers.</p>
     *
     * @param amount     An {@link Integer} value of credits associated with this referral code.
     * @param expiration Optional expiration {@link Date} of the offer code.
     * @param callback   A {@link BranchReferralInitListener} callback instance that will trigger
     *                   actions defined therein upon receipt of a response to a referral code
     *                   request.
     */
    public void getReferralCode(final int amount, final Date expiration, BranchReferralInitListener callback) {
        this.getReferralCode(null, amount, expiration, REFERRAL_BUCKET_DEFAULT, REFERRAL_CODE_AWARD_UNLIMITED, REFERRAL_CODE_LOCATION_REFERRING_USER, callback);
    }

    /**
     * <p>Configures and requests a referral code to be generated by the Branch servers.</p>
     *
     * @param prefix     A {@link String} containing the developer-specified prefix code to be
     *                   applied to the start of a referral code. e.g. for code OFFER4867, the
     *                   prefix would be "OFFER".
     * @param amount     An {@link Integer} value of credits associated with this referral code.
     * @param expiration Optional expiration {@link Date} of the offer code.
     * @param callback   A {@link BranchReferralInitListener} callback instance that will trigger
     *                   actions defined therein upon receipt of a response to a referral code
     *                   request.
     */
    public void getReferralCode(final String prefix, final int amount, final Date expiration, BranchReferralInitListener callback) {
        this.getReferralCode(prefix, amount, expiration, REFERRAL_BUCKET_DEFAULT, REFERRAL_CODE_AWARD_UNLIMITED, REFERRAL_CODE_LOCATION_REFERRING_USER, callback);
    }

    /**
     * <p>Configures and requests a referral code to be generated by the Branch servers.</p>
     *
     * @param prefix          A {@link String} containing the developer-specified prefix code to be
     *                        applied to the start of a referral code. e.g. for code OFFER4867, the
     *                        prefix would be "OFFER".
     * @param amount          An {@link Integer} value of credits associated with this referral code.
     * @param calculationType The type of referral calculation. i.e.
     *                        {@link #LINK_TYPE_UNLIMITED_USE} or
     *                        {@link #LINK_TYPE_ONE_TIME_USE}
     * @param location        The user to reward for applying the referral code.
     *                        <p/>
     *                        <p>Valid options:</p>
     *                        <p/>
     *                        <ul>
     *                        <li>{@link #REFERRAL_CODE_LOCATION_REFERREE}</li>
     *                        <li>{@link #REFERRAL_CODE_LOCATION_REFERRING_USER}</li>
     *                        <li>{@link #REFERRAL_CODE_LOCATION_BOTH}</li>
     *                        </ul>
     * @param callback        A {@link BranchReferralInitListener} callback instance that will
     *                        trigger actions defined therein upon receipt of a response to a
     *                        referral code request.
     */
    public void getReferralCode(final String prefix, final int amount, final int calculationType, final int location, BranchReferralInitListener callback) {
        this.getReferralCode(prefix, amount, null, REFERRAL_BUCKET_DEFAULT, calculationType, location, callback);
    }

    /**
     * <p>Configures and requests a referral code to be generated by the Branch servers.</p>
     *
     * @param prefix          A {@link String} containing the developer-specified prefix code to
     *                        be applied to the start of a referral code. e.g. for code OFFER4867,
     *                        the prefix would be "OFFER".
     * @param amount          An {@link Integer} value of credits associated with this referral code.
     * @param expiration      Optional expiration {@link Date} of the offer code.
     * @param bucket          A {@link String} value containing the name of the referral bucket
     *                        that the code will belong to.
     * @param calculationType The type of referral calculation. i.e.
     *                        {@link #LINK_TYPE_UNLIMITED_USE} or
     *                        {@link #LINK_TYPE_ONE_TIME_USE}
     * @param location        The user to reward for applying the referral code.
     *                        <p/>
     *                        <p>Valid options:</p>
     *                        <p/>
     *                        <ul>
     *                        <li>{@link #REFERRAL_CODE_LOCATION_REFERREE}</li>
     *                        <li>{@link #REFERRAL_CODE_LOCATION_REFERRING_USER}</li>
     *                        <li>{@link #REFERRAL_CODE_LOCATION_BOTH}</li>
     *                        </ul>
     * @param callback        A {@link BranchReferralInitListener} callback instance that will
     *                        trigger actions defined therein upon receipt of a response to a
     *                        referral code request.
     */
    public void getReferralCode(final String prefix, final int amount, final Date expiration, final String bucket, final int calculationType, final int location, BranchReferralInitListener callback) {
        String date = null;
        if (expiration != null)
            date = convertDate(expiration);
        ServerRequest req = new ServerRequestGetReferralCode(context_, prefix, amount, date, bucket,
                calculationType, location, callback);
        if (!req.constructError_ && !req.handleErrors(context_)) {
            handleNewRequest(req);
        }
    }

    /**
     * <p>Validates the supplied referral code on initialisation without applying it to the current
     * session.</p>
     *
     * @param code     A {@link String} object containing the referral code supplied.
     * @param callback A {@link BranchReferralInitListener} callback to handle the server response
     *                 of the referral submission request.
     */
    public void validateReferralCode(final String code, BranchReferralInitListener callback) {
        ServerRequest req = new ServerRequestValidateReferralCode(context_, callback, code);
        if (!req.constructError_ && !req.handleErrors(context_)) {
            handleNewRequest(req);
        }
    }

    /**
     * <p>Applies a supplied referral code to the current user session upon initialisation.</p>
     *
     * @param code     A {@link String} object containing the referral code supplied.
     * @param callback A {@link BranchReferralInitListener} callback to handle the server
     *                 response of the referral submission request.
     * @see BranchReferralInitListener
     */
    public void applyReferralCode(final String code, final BranchReferralInitListener callback) {
        ServerRequest req = new ServerRequestApplyReferralCode(context_, callback, code);
        if (!req.constructError_ && !req.handleErrors(context_)) {
            handleNewRequest(req);
        }
    }

    /**
     * <p>Creates options for sharing a link with other Applications. Creates a link with given attributes and shares with the
     * user selected clients.</p>
     *
     * @param builder A {@link io.branch.referral.Branch.ShareLinkBuilder} instance to build share link.
     */
    private void shareLink(ShareLinkBuilder builder) {
        //Cancel any existing sharing in progress.
        if (shareLinkManager_ != null) {
            shareLinkManager_.cancelShareLinkDialog();
        }
        shareLinkManager_ = new ShareLinkManager();
        shareLinkManager_.shareLink(builder);
    }

    /**
     * <p>Cancel current share link operation and Application selector dialog. If your app is not using auto session management, make sure you are
     * calling this method before your activity finishes inorder to prevent any window leak. </p>
     */
    public void cancelShareLinkDialog() {
        if (shareLinkManager_ != null) {
            shareLinkManager_.cancelShareLinkDialog();
        }
    }

    // PRIVATE FUNCTIONS

    private String convertDate(Date date) {
        return android.text.format.DateFormat.format("yyyy-MM-dd", date).toString();
    }

    private String stringifyParams(JSONObject params) {
        if (params == null) {
            params = new JSONObject();
        }

        try {
            params.put("source", "android");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return params.toString();
    }

    private String generateShortLink(final String alias, final int type, final int duration, final Collection<String> tags, final String channel, final String feature, final String stage, final String params, BranchLinkCreateListener callback, boolean async) {
        ServerRequestCreateUrl req = new ServerRequestCreateUrl(context_, alias, type, duration, tags,
                channel, feature, stage,
                params, callback, async);

        if (!req.constructError_ && !req.handleErrors(context_)) {
            if (linkCache_.containsKey(req.getLinkPost())) {
                String url = linkCache_.get(req.getLinkPost());
                if (callback != null) {
                    callback.onLinkCreate(url, null);
                }
                return url;
            } else {
                if (async) {
                    generateShortLinkAsync(req);
                } else {
                    return generateShortLinkSync(req);
                }
            }
        }

        return null;

    }

    private String generateShortLinkSync(ServerRequest req) {
        if (initState_ == SESSION_STATE.INITIALISED) {
            ServerResponse response = kRemoteInterface_.createCustomUrlSync(req.getPost());
            String url = prefHelper_.getUserURL();
            if (response.getStatusCode() == 200) {
                try {
                    url = response.getObject().getString("url");
                    if (response.getLinkData() != null) {
                        linkCache_.put(response.getLinkData(), url);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            return url;
        } else {
            Log.i("BranchSDK", "Branch Warning: User session has not been initialized");
        }
        return null;
    }

    private void generateShortLinkAsync(final ServerRequest req) {
        handleNewRequest(req);
    }

    private JSONObject filterOutBadCharacters(JSONObject inputObj) {
        JSONObject filteredObj = new JSONObject();
        if (inputObj != null) {
            Iterator<?> keys = inputObj.keys();
            while (keys.hasNext()) {
                String key = (String) keys.next();
                try {
                    if (inputObj.has(key) && inputObj.get(key).getClass().equals(String.class)) {
                        filteredObj.put(key, inputObj.getString(key).replace("\n", "\\n").replace("\r", "\\r").replace("\"", "\\\""));
                    } else if (inputObj.has(key)) {
                        filteredObj.put(key, inputObj.get(key));
                    }
                } catch(JSONException ignore) {

                }
            }
        }
        return filteredObj;
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

    /**
     * <p>Schedules a repeating threaded task to get the following details and report them to the
     * Branch API <b>once a week</b>:</p>
     * <p/>
     * <pre style="background:#fff;padding:10px;border:2px solid silver;">
     * int interval = 7 * 24 * 60 * 60;
     * appListingSchedule_ = scheduler.scheduleAtFixedRate(
     * periodicTask, (days * 24 + hours) * 60 * 60, interval, TimeUnit.SECONDS);</pre>
     * <p/>
     * <ul>
     * <li>{@link SystemObserver#getAppKey()}</li>
     * <li>{@link SystemObserver#getOS()}</li>
     * <li>{@link SystemObserver#getDeviceFingerPrintID()}</li>
     * <li>{@link SystemObserver#getListOfApps()}</li>
     * </ul>
     *
     * @see {@link SystemObserver}
     * @see {@link PrefHelper}
     */
    private void scheduleListOfApps() {
        ScheduledThreadPoolExecutor scheduler = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(1);
        Runnable periodicTask = new Runnable() {
            @Override
            public void run() {
                ServerRequest req = new ServerRequestSendAppList(context_);
                if (!req.constructError_ && !req.handleErrors(context_)) {
                    handleNewRequest(req);
                }
            }
        };

        Date date = new Date();
        Calendar calendar = GregorianCalendar.getInstance();
        calendar.setTime(date);

        int days = Calendar.SATURDAY - calendar.get(Calendar.DAY_OF_WEEK);    // days to Saturday
        int hours = 2 - calendar.get(Calendar.HOUR_OF_DAY);    // hours to 2am, can be negative
        if (days == 0 && hours < 0) {
            days = 7;
        }
        int interval = 7 * 24 * 60 * 60;

        appListingSchedule_ = scheduler.scheduleAtFixedRate(periodicTask, (days * 24 + hours) * 60 * 60, interval, TimeUnit.SECONDS);
    }

    private void processNextQueueItem() {
        try {
            serverSema_.acquire();
            if (networkCount_ == 0 && requestQueue_.getSize() > 0) {
                networkCount_ = 1;
                ServerRequest req = requestQueue_.peek();

                serverSema_.release();
                //All request except Install request need a valid IdentityID
                if (!(req instanceof ServerRequestRegisterInstall) && !hasUser()) {
                    Log.i("BranchSDK", "Branch Error: User session has not been initialized!");
                    networkCount_ = 0;
                    handleFailure(requestQueue_.getSize() - 1, BranchError.ERR_NO_SESSION);
                    return;
                }
                //All request except open and install need a session to execute
                else if (!req.isSessionInitRequest() && (!hasSession() || !hasDeviceFingerPrint())) {
                    networkCount_ = 0;
                    handleFailure(requestQueue_.getSize() - 1, BranchError.ERR_NO_SESSION);
                    return;
                } else {
                    BranchPostTask postTask = new BranchPostTask(req);
                    postTask.execute();
                }
            } else {
                serverSema_.release();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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
        req.handleFailure(statusCode);
    }

    private void updateAllRequestsInQueue() {
        try {
            for (int i = 0; i < requestQueue_.getSize(); i++) {
                ServerRequest req = requestQueue_.peekAt(i);
                if (req.getPost() != null) {
                    Iterator<?> keys = req.getPost().keys();
                    while (keys.hasNext()) {
                        String key = (String) keys.next();
                        if (key.equals(Defines.Jsonkey.SessionID.getKey())) {
                            req.getPost().put(key, prefHelper_.getSessionID());
                        } else if (key.equals(Defines.Jsonkey.IdentityID.getKey())) {
                            req.getPost().put(key, prefHelper_.getIdentityID());
                        }
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void clearCloseTimer() {
        if (rotateCloseTimer == null)
            return;
        rotateCloseTimer.cancel();
        rotateCloseTimer.purge();
        rotateCloseTimer = new Timer();
    }

    private void clearTimer() {
        if (closeTimer == null)
            return;
        closeTimer.cancel();
        closeTimer.purge();
        closeTimer = new Timer();
    }

    private void keepAlive() {
        keepAlive_ = true;
        synchronized(lock) {
            clearTimer();
            closeTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            keepAlive_ = false;
                        }
                    }).start();
                }
            }, SESSION_KEEPALIVE);
        }
    }

    private boolean hasSession() {
        return !prefHelper_.getSessionID().equals(PrefHelper.NO_STRING_VALUE);
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

    private void registerInstallOrOpen(ServerRequest req, BranchReferralInitListener callback) {
        // If there isn't already an Open / Install request, add one to the queue
        if (!requestQueue_.containsInstallOrOpen()) {
            insertRequestAtFront(req);
        }
        // If there is already one in the queue, make sure it's in the front.
        // Make sure a callback is associated with this request. This callback can
        // be cleared if the app is terminated while an Open/Install is pending.
        else {
            requestQueue_.moveInstallOrOpenToFront(req, networkCount_, callback);
        }

        processNextQueueItem();
    }

    private void initializeSession(BranchReferralInitListener callback) {
        if ((prefHelper_.getBranchKey() == null || prefHelper_.getBranchKey().equalsIgnoreCase(PrefHelper.NO_STRING_VALUE))
                && (prefHelper_.getAppKey() == null || prefHelper_.getAppKey().equalsIgnoreCase(PrefHelper.NO_STRING_VALUE))) {
            initState_ = SESSION_STATE.UNINITIALISED;
            //Report Key error on callback
            if (callback != null) {
                callback.onInitFinished(null, new BranchError("Trouble initializing Branch.", RemoteInterface.NO_BRANCH_KEY_STATUS));
            }
            Log.i("BranchSDK", "Branch Warning: Please enter your branch_key in your project's res/values/strings.xml!");
            return;
        } else if (prefHelper_.getBranchKey() != null && prefHelper_.getBranchKey().startsWith("key_test_")) {
            Log.i("BranchSDK", "Branch Warning: You are using your test app's Branch Key. Remember to change it to live Branch Key during deployment.");
        }

        if (hasUser()) {
            registerInstallOrOpen(new ServerRequestRegisterOpen(context_, callback, kRemoteInterface_.getSystemObserver()), callback);
        } else {
            registerInstallOrOpen(new ServerRequestRegisterInstall(context_, callback, kRemoteInterface_.getSystemObserver(), InstallListener.getInstallationID()), callback);
        }
    }

    /**
     * Handles execution of a new request other than open or install.
     * Checks for the session initialisation and adds a install/Open request in front of this request
     * if the request need session to execute.
     *
     * @param req The {@link ServerRequest} to execute
     */
    private void handleNewRequest(ServerRequest req) {
        //If not initialised put an open or install request in front of this request(only if this needs session)
        if (initState_ != SESSION_STATE.INITIALISED && req.isSessionInitRequest() == false) {
            if((req instanceof ServerRequestLogout)){
                Log.i(TAG, "Branch is not initialized, cannot logout");
                return;
            }
            if((req instanceof ServerRequestRegisterClose)){
                Log.i(TAG, "Branch is not initialized, cannot close session");
                return;
            }
            else{
                initializeSession(null);
            }
        }
        requestQueue_.enqueue(req);
        processNextQueueItem();
    }


    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private void setActivityLifeCycleObserver(Application application) {
        try {
            activityLifeCycleObserver_ = new BranchActivityLifeCycleObserver();
            /* Set an observer for activity life cycle events. */
            application.unregisterActivityLifecycleCallbacks(activityLifeCycleObserver_);
            application.registerActivityLifecycleCallbacks(activityLifeCycleObserver_);
            isActivityLifeCycleCallbackRegistered_ = true;

        } catch (NoSuchMethodError Ex) {
            isActivityLifeCycleCallbackRegistered_ = false;
            isAutoSessionMode_ = false;
            /* LifeCycleEvents are  available only from API level 14. */
            Log.w(TAG, new BranchError("",BranchError.ERR_API_LVL_14_NEEDED).getMessage());
        } catch (NoClassDefFoundError Ex) {
            isActivityLifeCycleCallbackRegistered_ = false;
            isAutoSessionMode_ = false;
            /* LifeCycleEvents are  available only from API level 14. */
            Log.w(TAG, new BranchError("",BranchError.ERR_API_LVL_14_NEEDED).getMessage());
        }
    }

    /**
     * <p>Class that observes activity life cycle events and determines when to start and stop
     * session.</p>
     */
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private class BranchActivityLifeCycleObserver implements Application.ActivityLifecycleCallbacks{
        private int activityCnt_ = 0; //Keep the count of live  activities.


        @Override
        public void onActivityCreated(Activity activity, Bundle bundle) {}

        @Override
        public void onActivityStarted(Activity activity) {
            if (activityCnt_ < 1) { // Check if this is the first Activity.If so start a session.
                initSessionWithData(activity.getIntent().getData(), activity); // indicate  starting of session.
            }
            activityCnt_++;
        }

        @Override
        public void onActivityResumed(Activity activity) {
            currentActivity_ = activity;
            //Set the activity for touch debug
            if (prefHelper_.getTouchDebugging()) {
                setTouchDebugInternal(activity);
            }
        }

        @Override
        public void onActivityPaused(Activity activity) {
            clearTouchDebugInternal(activity);
            /* Close any opened sharing dialog.*/
            if(shareLinkManager_ != null) {
                shareLinkManager_.cancelShareLinkDialog();
            }
        }

        @Override
        public void onActivityStopped(Activity activity) {
            activityCnt_--; // Check if this is the last activity.If so stop
                            // session.
            if (activityCnt_ < 1) {
                closeSessionInternal();
            }
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {}

        @Override
        public void onActivityDestroyed(Activity activity) {}

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
        public void onInitFinished(JSONObject referringParams, BranchError error);
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
        public void onStateChanged(boolean changed, BranchError error);
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
        public void onLinkCreate(String url, BranchError error);
    }

    /**
     * <p>An Interface class that is implemented by all classes that make use of
     * {@link BranchLinkShareListener}, defining methods to listen for link sharing status.</p>
     */
    public interface BranchLinkShareListener {
        /**
         *<p> Callback method to update the sharing status. Called on sharing completed or on error.</p>
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
         * @param channelName Name of the selected application to share the link.
         */
        void onChannelSelected(String channelName);
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
        public void onReceivingResponse(JSONArray list, BranchError error);
    }

    /**
     * <p>enum containing the sort options for return of credit history.</p>
     */
    public enum CreditHistoryOrder {
        kMostRecentFirst, kLeastRecentFirst
    }

    /**
     * Asynchronous task handling execution of server requests. Execute the network task on background
     * thread and request are  executed in sequential manner. Handles the request execution in
     * Synchronous-Asynchronous pattern. Should be invoked only form main thread and  the results are
     * published in the main thread.
     */
    private class BranchPostTask extends AsyncTask<Void, Void, ServerResponse> {
        int timeOut_ = 0;
        ServerRequest thisReq_;

        public BranchPostTask(ServerRequest request) {
            thisReq_ = request;
            timeOut_ = prefHelper_.getTimeout();
        }

        @Override
        protected ServerResponse doInBackground(Void... voids) {
            //Google ADs ID  and LAT value are updated using reflection. These method need background thread
            //So updating them for install and open on background thread.
            if(thisReq_.isSessionInitRequest()){
                thisReq_.updateGAdsParams(systemObserver_);
            }
            if (thisReq_.isGetRequest()) {
                return kRemoteInterface_.make_restful_get(thisReq_.getRequestUrl(), thisReq_.getRequestPath(), timeOut_);
            } else {
                return kRemoteInterface_.make_restful_post(thisReq_.getPost(), thisReq_.getRequestUrl(), thisReq_.getRequestPath(), timeOut_);
            }
        }

        @Override
        protected void onPostExecute(ServerResponse serverResponse) {
            super.onPostExecute(serverResponse);
            if (serverResponse != null) {
                try {
                    int status = serverResponse.getStatusCode();
                    hasNetwork_ = true;

                    //If the request is not succeeded
                    if (status != 200) {
                        //If failed request is an initialisation request then mark session not initialised
                        if (thisReq_.isSessionInitRequest()) {
                            initState_ = SESSION_STATE.UNINITIALISED;
                        }
                        //On a bad request continue processing
                        if (status == 409) {
                            if (thisReq_ instanceof ServerRequestCreateUrl) {
                                ((ServerRequestCreateUrl) thisReq_).handleDuplicateURLError();
                            } else {
                                Log.i("BranchSDK", "Branch API Error: Conflicting resource error code from API");
                                handleFailure(0, status);
                            }
                        }
                        //On Network error or Branch is down fail all the pending requests in the queue except
                        //for request which need to be replayed on failure.
                        else {
                            hasNetwork_ = false;
                            //Collect all request from the queue which need to be failed.
                            ArrayList<ServerRequest> requestToFail = new ArrayList<ServerRequest>();
                            for (int i = 0; i < requestQueue_.getSize(); i++) {
                                requestToFail.add(requestQueue_.peekAt(i));
                            }
                            //Remove the requests from the request queue first
                            for (ServerRequest req : requestToFail) {
                                if (!req.shouldRetryOnFail()) {
                                    requestQueue_.remove(req);
                                }
                            }
                            // Then, set the network count to zero, indicating that requests can be started again.
                            networkCount_ = 0;

                            //Finally call the request callback with the error.
                            for (ServerRequest req : requestToFail) {
                                req.handleFailure(status);
                                //If request need to be replayed, no need for the callbacks
                                if (req.shouldRetryOnFail())
                                    req.clearCallbacks();
                            }
                        }
                    }
                    //If the request succeeded
                    else {
                        hasNetwork_ = true;
                        //On create  new url cache the url.
                        if (thisReq_ instanceof ServerRequestCreateUrl) {
                            final String url = serverResponse.getObject().getString("url");
                            // cache the link
                            linkCache_.put(serverResponse.getLinkData(), url);
                        }
                        //On Logout clear the link cache and all pending requests
                        else if (thisReq_ instanceof ServerRequestLogout) {
                            linkCache_.clear();
                            requestQueue_.clear();
                        }
                        //On setting a new identity Id clear the link cache
                        else if (thisReq_ instanceof ServerRequestIdentifyUserRequest) {
                            try {
                                String new_Identity_Id = serverResponse.getObject().getString(Defines.Jsonkey.IdentityID.getKey());
                                if (!prefHelper_.getIdentityID().equals(new_Identity_Id)) {
                                    linkCache_.clear();
                                }
                            } catch (Exception ignore) {
                            }
                        }
                        //Publish success to listeners
                        thisReq_.onRequestSucceeded(serverResponse, branchReferral_);

                        //If this request changes a session update the session-id to queued requests.
                        if (thisReq_.isSessionInitRequest()) {
                            updateAllRequestsInQueue();
                            initState_ = SESSION_STATE.INITIALISED;
                            checkForAutoDeepLinkConfiguration();
                        }
                        requestQueue_.dequeue();
                    }
                    networkCount_ = 0;
                    if (hasNetwork_ && initState_ != SESSION_STATE.UNINITIALISED) {
                        processNextQueueItem();
                    }
                } catch (JSONException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    //-------------------Auto deep link feature-------------------------------------------//

    /**
     * <p>Checks if an activity is launched by Branch auto deep link feature. Branch launches activitie configured for auto deep link on seeing matching keys.
     * Keys for auto deep linking should be specified to each activity as a meta data in manifest.</p>
     * <p>
     * Configure your activity in your manifest to enable auto deep linking as follows
     * <activity android:name=".YourActivity">
     * <meta-data android:name="io.branch.sdk.auto_link" android:value="DeepLinkKey1","DeepLinkKey2" />
     * </activity>
     * </p>
     *
     * @param activity Instane of activity to check if launched on auto deep link.
     * @return A {Boolean} value whose value is true if this activity is launched by Branch auto deeplink feature.
     */
    public static boolean isAutoDeepLinkLaunch(Activity activity) {
        return (activity.getIntent().getStringExtra(AUTO_DEEP_LINK_KEY) != null);
    }

    private void checkForAutoDeepLinkConfiguration() {
        JSONObject latestParams = getLatestReferringParams();
        String deepLinkActivity = null;
        String deepLinkKey = null;

        try {
            //Check if the application is launched by clicking a Branch link.
            if (latestParams.has(Defines.Jsonkey.Clicked_Branch_Link.getKey()) == false
                    || latestParams.getBoolean(Defines.Jsonkey.Clicked_Branch_Link.getKey()) == false) {
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
                int deepLinkActivity_Req_Code = DEF_AUTO_DEEP_LINK_REQ_CODE;

                for (ActivityInfo activityInfo : activityInfos) {
                    if (activityInfo.metaData != null && activityInfo.metaData.getString(AUTO_DEEP_LINK_KEY) != null) {
                        String[] activityLinkKeys = activityInfo.metaData.getString(AUTO_DEEP_LINK_KEY).split(",");
                        for (String activityLinkKey : activityLinkKeys) {
                            if (latestParams.has(activityLinkKey)) {
                                deepLinkActivity = ((ActivityInfo) activityInfo).name;
                                deepLinkActivity_Req_Code = activityInfo.metaData.getInt(AUTO_DEEP_LINK_REQ_CODE, DEF_AUTO_DEEP_LINK_REQ_CODE);
                                deepLinkKey = activityLinkKey;
                                break;
                            }
                        }
                    }
                    if (deepLinkActivity != null) {
                        break;
                    }
                }
                if (deepLinkActivity != null && currentActivity_ != null) {
                    Intent intent = new Intent(currentActivity_, Class.forName(deepLinkActivity));
                    intent.putExtra(AUTO_DEEP_LINK_KEY, deepLinkKey);
                    currentActivity_.startActivityForResult(intent, deepLinkActivity_Req_Code);
                } 
            }
        } catch (final PackageManager.NameNotFoundException e) {
            Log.i("BranchSDK", "Branch Warning: Please make sure Activity names set for auto deep link are correct!");
        } catch (ClassNotFoundException e) {
            Log.i("BranchSDK", "Branch Warning: Please make sure Activity names set for auto deep link are correct! Error while looking for activity " + deepLinkActivity);
        } catch (JSONException ignore) {
        }
    }

    //--------------------Window callback handling for touch debug feature-----------------------//


    public class BranchWindowCallback implements Window.Callback {
        private Runnable longPressed_;
        private Window.Callback callback_;

        public BranchWindowCallback(Window.Callback callback) {
            callback_ = callback;

            if (longPressed_ == null) {
                longPressed_ = new Runnable() {
                    private Timer timer;

                    public void run() {
                        debugHandler_.removeCallbacks(longPressed_);
                        if (!debugStarted_) {
                            Log.i("Branch Debug","======= Start Debug Session =======");
                            prefHelper_.setDebug();
                            timer = new Timer();
                            timer.scheduleAtFixedRate(new KeepDebugConnectionTask(), new Date(), 20000);
                        } else {
                            Log.i("Branch Debug","======= End Debug Session =======");
                            prefHelper_.clearDebug();
                            if (timer != null) {
                                timer.cancel();
                                timer = null;
                            }
                        }
                        debugStarted_ = !debugStarted_;
                    }
                };
            }
        }

        class KeepDebugConnectionTask extends TimerTask {
            public void run() {
                if (debugStarted_ && !prefHelper_.keepDebugConnection() && longPressed_ != null) {
                    debugHandler_.post(longPressed_);
                }
            }
        }

        @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
        @Override
        public boolean dispatchGenericMotionEvent(MotionEvent event) {
            return callback_.dispatchGenericMotionEvent(event);
        }

        @Override
        public boolean dispatchKeyEvent(KeyEvent event) {
            return callback_.dispatchKeyEvent(event);
        }

        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        @Override
        public boolean dispatchKeyShortcutEvent(KeyEvent event) {
            return callback_.dispatchKeyShortcutEvent(event);
        }

        @Override
        public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
            return callback_.dispatchPopulateAccessibilityEvent(event);
        }

        @Override
        public boolean dispatchTouchEvent(MotionEvent event) {
            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                    if (systemObserver_.isSimulator()) {
                        debugHandler_.postDelayed(longPressed_, PrefHelper.DEBUG_TRIGGER_PRESS_TIME);
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    break;
                case MotionEvent.ACTION_CANCEL:
                    debugHandler_.removeCallbacks(longPressed_);
                    break;
                case MotionEvent.ACTION_UP:
                    debugHandler_.removeCallbacks(longPressed_);
                    break;
                case MotionEvent.ACTION_POINTER_DOWN:
                    if (event.getPointerCount() == PrefHelper.DEBUG_TRIGGER_NUM_FINGERS) {
                        debugHandler_.postDelayed(longPressed_, PrefHelper.DEBUG_TRIGGER_PRESS_TIME);
                    }
                    break;
                default:
                    break;
            }

            return callback_.dispatchTouchEvent(event);
        }

        @Override
        public boolean dispatchTrackballEvent(MotionEvent event) {
            return callback_.dispatchTrackballEvent(event);
        }

        @Override
        public void onActionModeFinished(ActionMode mode) {
            callback_.onActionModeFinished(mode);
        }

        @Override
        public void onActionModeStarted(ActionMode mode) {
            callback_.onActionModeStarted(mode);
        }

        @Override
        public void onAttachedToWindow() {
            callback_.onAttachedToWindow();
        }

        @Override
        public void onContentChanged() {
            callback_.onContentChanged();
        }

        @Override
        public boolean onCreatePanelMenu(int featureId, Menu menu) {
            return callback_.onCreatePanelMenu(featureId, menu);
        }

        @Override
        public View onCreatePanelView(int featureId) {
            return callback_.onCreatePanelView(featureId);
        }

        @SuppressLint("MissingSuperCall")
        @Override
        public void onDetachedFromWindow() {
            callback_.onDetachedFromWindow();
        }

        @Override
        public boolean onMenuItemSelected(int featureId, MenuItem item) {
            return callback_.onMenuItemSelected(featureId, item);
        }

        @Override
        public boolean onMenuOpened(int featureId, Menu menu) {
            return callback_.onMenuOpened(featureId, menu);
        }

        @Override
        public void onPanelClosed(int featureId, Menu menu) {
            callback_.onPanelClosed(featureId, menu);
        }

        @Override
        public boolean onPreparePanel(int featureId, View view, Menu menu) {
            return callback_.onPreparePanel(featureId, view, menu);
        }

        @Override
        public boolean onSearchRequested() {
            return callback_.onSearchRequested();
        }

        @Override
        public void onWindowAttributesChanged(WindowManager.LayoutParams attrs) {
            callback_.onWindowAttributesChanged(attrs);
        }

        @Override
        public void onWindowFocusChanged(boolean hasFocus) {
            callback_.onWindowFocusChanged(hasFocus);
        }

        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        @Override
        public ActionMode onWindowStartingActionMode(ActionMode.Callback callback) {
            return callback_.onWindowStartingActionMode(callback);
        }
    }

    //-------------------------- Branch Builders--------------------------------------//

    /**
     * <p> Class for building a share link dialog.This creates a chooser for selecting application for
     * sharing a link created with given parameters. </p>
     */
    public static class ShareLinkBuilder {

        private final Activity activity_;
        private final JSONObject linkCreationParams_;
        private final Branch branch_;

        private String shareMsg_;
        private Collection<String> tags_ = null;
        private String feature_ = "";
        private String stage_ = "";
        private Branch.BranchLinkShareListener callback_ = null;
        private ArrayList<SharingHelper.SHARE_WITH> preferredOptions_;
        private String defaultURL_;


        /**
         * <p>Creates options for sharing a link with other Applications. Creates a builder for sharing the link with
         * user selected clients</p>
         *
         * @param activity  The {@link Activity} to show the dialog for choosing sharing application.
         * @param parameters @param params  A {@link JSONObject} value containing the deep link params.
         */
        public ShareLinkBuilder(Activity activity, JSONObject parameters) {
            this.activity_ = activity;
            this.linkCreationParams_ = parameters;
            this.branch_ = branchReferral_;

            shareMsg_ = "";
            tags_ = new ArrayList<String>();
            feature_ = "";
            stage_ = "";
            callback_ = null;
            preferredOptions_ = new ArrayList<SharingHelper.SHARE_WITH>();
            defaultURL_ = null;
        }

        /**
         * <p>Sets the message to be shared with the link.</p>
         *
         * @param message A {@link String} to be shared with the link
         * @return A {@link io.branch.referral.Branch.ShareLinkBuilder} instance.
         */
        public ShareLinkBuilder setMessage(String message) {
            this.shareMsg_ = message;
            return this;
        }

        /**
         * <p>Adds the given tag an iterable {@link Collection} of {@link String} tags associated with a deep
         * link.</p>
         *
         * @param tag A {@link String} to be added to the iterable {@link Collection} of {@link String} tags associated with a deep
         *            link.
         * @return A {@link io.branch.referral.Branch.ShareLinkBuilder} instance.
         */
        public ShareLinkBuilder addTag(String tag) {
            this.tags_.add(tag);
            return this;
        }

        /**
         * <p>Adds a feature that make use of the link.</p>
         *
         * @param feature A {@link String} value identifying the feature that the link makes use of.
         *                Should not exceed 128 characters.
         * @return A {@link io.branch.referral.Branch.ShareLinkBuilder} instance.
         */
        public ShareLinkBuilder setFeature(String feature) {
            this.feature_ = feature;
            return this;
        }

        /**
         * <p>Adds a stage application or user flow associated with this link.</p>
         *
         * @param stage A {@link String} value identifying the stage in an application or user flow
         *              process. Should not exceed 128 characters.
         * @return A {@link io.branch.referral.Branch.ShareLinkBuilder} instance.
         */
        public ShareLinkBuilder setStage(String stage) {
            this.stage_ = stage;
            return this;
        }

        /**
         * <p>Adds a callback to get the sharing status.</p>
         *
         * @param callback A {@link BranchLinkShareListener} instance for getting sharing status.
         * @return A {@link io.branch.referral.Branch.ShareLinkBuilder} instance.
         */
        public ShareLinkBuilder setCallback(BranchLinkShareListener callback) {
            this.callback_ = callback;
            return this;
        }

        /**
         * <p>Adds application to the preferred list of applications which are shown on share dialog.
         * Only these options will be visible when the application selector dialog launches. Other options can be
         * accessed by clicking "More"</p>
         *
         * @param preferredOption A list of applications to be added as preferred options on the app chooser.
         *                        Preferred applications are defined in {@link io.branch.referral.SharingHelper.SHARE_WITH}.
         * @return A {@link io.branch.referral.Branch.ShareLinkBuilder} instance.
         */
        public ShareLinkBuilder addPreferredSharingOption(SharingHelper.SHARE_WITH preferredOption) {
            this.preferredOptions_.add(preferredOption);
            return this;
        }

        /**
         * Set a default url to share in case there is any error creating the deep link
         *
         * @param url A {@link String} with value of default url to be shared with the selected application in case deep link creation fails.
         * @return A {@link io.branch.referral.Branch.ShareLinkBuilder} instance.
         */
        public ShareLinkBuilder setDefaultURL(String url) {
            defaultURL_ = url;
            return this;
        }

        /**
         * <p>Creates an application selector dialog and share a link with user selected sharing option.
         * The link is created with the parameteres provided to the builder. </p>
         */
        public void shareLink() {
            branchReferral_.shareLink(this);
        }

        public Activity getActivity() {
            return activity_;
        }

        public ArrayList<SharingHelper.SHARE_WITH> getPreferredOptions() {
            return preferredOptions_;
        }

        public Branch getBranch() {
            return branch_;
        }

        public String getShareMsg() {
            return shareMsg_;
        }

        public BranchLinkShareListener getCallback() {
            return callback_;
        }

        public Collection<String> getTags() {
            return tags_;
        }

        public JSONObject getLinkCreationParams() {
            return linkCreationParams_;
        }

        public String getFeature() {
            return feature_;
        }

        public String getStage() {
            return stage_;
        }

        public String getDefaultURL() {
            return defaultURL_;
        }
    }

}
