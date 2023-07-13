package io.branch.referral

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.AsyncTask
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import androidx.core.app.ActivityCompat
import com.android.billingclient.api.Purchase
import io.branch.indexing.BranchUniversalObject
import io.branch.indexing.BranchUniversalObject.RegisterViewStatusListener
import io.branch.referral.BillingGooglePlay.*
import io.branch.referral.BillingGooglePlay.Companion.getInstance
import io.branch.referral.BranchViewHandler.IBranchViewEvents
import io.branch.referral.Defines.PreinstallKey
import io.branch.referral.ServerRequestGetCPID.BranchCrossPlatformIdListener
import io.branch.referral.ServerRequestGetLATD.BranchLastAttributedTouchDataListener
import io.branch.referral.StoreReferrerGooglePlayStore.IGoogleInstallReferrerEvents
import io.branch.referral.StoreReferrerHuaweiAppGallery.IHuaweiInstallReferrerEvents
import io.branch.referral.StoreReferrerSamsungGalaxyStore.ISamsungInstallReferrerEvents
import io.branch.referral.StoreReferrerXiaomiGetApps.IXiaomiInstallReferrerEvents
import io.branch.referral.SystemObserver.AdsParamsFetchEvents
import io.branch.referral.network.BranchRemoteInterface
import io.branch.referral.network.BranchRemoteInterfaceUrlConnection
import io.branch.referral.util.BRANCH_STANDARD_EVENT
import io.branch.referral.util.BranchEvent
import io.branch.referral.util.CommerceEvent
import io.branch.referral.util.LinkProperties
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.UnsupportedEncodingException
import java.lang.ref.WeakReference
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.util.concurrent.*

/**
 *
 *
 * The core object required when using Branch SDK. You should declare an object of this type at
 * the class-level of each Activity or Fragment that you wish to use Branch functionality within.
 *
 *
 *
 * Normal instantiation of this object would look like this:
 *
 *
 */
class Branch private constructor(context: Context) : IBranchViewEvents, AdsParamsFetchEvents,
    IGoogleInstallReferrerEvents, IHuaweiInstallReferrerEvents, ISamsungInstallReferrerEvents,
    IXiaomiInstallReferrerEvents {
    /* Json object containing key-value pairs for debugging deep linking */
    private var deeplinkDebugParams_: JSONObject? = null
    private var enableFacebookAppLinkCheck_ = false
    private var branchRemoteInterface_: BranchRemoteInterface
    val prefHelper: PrefHelper?
    val deviceInfo: DeviceInfo
    val branchPluginSupport: BranchPluginSupport
    val applicationContext: Context?
    val branchQRCodeCache: BranchQRCodeCache
    private val serverSema_ = Semaphore(1)
    @JvmField
    val requestQueue_: ServerRequestQueue?
    var networkCount_ = 0
    @JvmField
    val linkCache_ = ConcurrentHashMap<BranchLinkData, String?>()

    /* Enumeration for defining session initialisation state. */
    enum class SESSION_STATE {
        INITIALISED, INITIALISING, UNINITIALISED
    }

    enum class INTENT_STATE {
        PENDING, READY
    }

    /* Holds the current intent state. Default is set to PENDING. */
    private var intentState_ = INTENT_STATE.PENDING

    /* Holds the current Session state. Default is set to UNINITIALISED. */
    var initState = SESSION_STATE.UNINITIALISED

    /* Flag to indicate if the `v1/close` is expected by the server at the end of this session. */
    @JvmField
    var closeRequestNeeded = false

    /* Instance  of share link manager to share links automatically with third party applications. */
    var shareLinkManager: ShareLinkManager? = null
        private set

    /* The current activity instance for the application.*/
    @JvmField
    var currentActivityReference_: WeakReference<Activity>? = null
    val instrumentationExtraData_ = ConcurrentHashMap<String, String>()

    /* In order to get Google's advertising ID an AsyncTask is needed, however Fire OS does not require AsyncTask, so isGAParamsFetchInProgress_ would remain false */
    var isGAParamsFetchInProgress = false
    var getFirstReferringParamsLatch: CountDownLatch? = null
    var getLatestReferringParamsLatch: CountDownLatch? = null
    private var waitingForHuaweiInstallReferrer = false
    private var waitingForGoogleInstallReferrer = false
    private var waitingForSamsungInstallReferrer = false
    private var waitingForXiaomiInstallReferrer = false

    private var isInstantDeepLinkPossible = false
    private var activityLifeCycleObserver: BranchActivityLifecycleObserver? = null
    val trackingController: TrackingController
    private val deferredCallback: BranchReferralInitListener? = null
    private val deferredUri: Uri? = null
    private var deferredSessionBuilder: InitSessionBuilder? = null

    /**
     *
     * The main constructor of the Branch class is private because the class uses the Singleton
     * pattern.
     *
     * Use [.getAutoInstance] method when instantiating.
     *
     * @param context A [Context] from which this call was made.
     */
    init {
        applicationContext = context
        prefHelper = PrefHelper.getInstance(context)
        trackingController = TrackingController(context)
        branchRemoteInterface_ = BranchRemoteInterfaceUrlConnection(this)
        deviceInfo = DeviceInfo(context)
        branchPluginSupport = BranchPluginSupport(context)
        branchQRCodeCache = BranchQRCodeCache(context)
        requestQueue_ = ServerRequestQueue.getInstance(context)
        if (!trackingController.isTrackingDisabled) { // Do not get GAID when tracking is disabled
            isGAParamsFetchInProgress = deviceInfo.systemObserver.prefetchAdsParams(context, this)
        }
    }

    /**
     * Sets a custom Branch Remote interface for handling RESTful requests. Call this for implementing a custom network layer for handling communication between
     * Branch SDK and remote Branch server
     *
     * @param remoteInterface A instance of class extending [BranchRemoteInterface] with
     * implementation for abstract RESTful GET or POST methods, if null
     * is passed, the SDK will use its default.
     */
    var branchRemoteInterface: BranchRemoteInterface?
        get() = branchRemoteInterface_
        set(remoteInterface) {
            branchRemoteInterface_ = remoteInterface ?: BranchRemoteInterfaceUrlConnection(this)
        }

    /**
     * Disable (or re-enable) ad network callouts. This setting is persistent.
     *
     * @param disabled (@link Boolean) whether ad network callouts should be disabled.
     */
    fun disableAdNetworkCallouts(disabled: Boolean) {
        PrefHelper.getInstance(applicationContext).adNetworkCalloutsDisabled = disabled
    }

    /**
     * Method to change the Tracking state. If disabled SDK will not track any user data or state. SDK will not send any network calls except for deep linking when tracking is disabled
     */
    fun disableTracking(disableTracking: Boolean) {
        trackingController.disableTracking(applicationContext, disableTracking)
    }

    /**
     * Checks if tracking is disabled. See [.disableTracking]
     *
     * @return `true` if tracking is disabled
     */
    val isTrackingDisabled: Boolean
        get() = trackingController.isTrackingDisabled

    /**
     *
     * Manually sets the [Boolean] value, that indicates that the Branch API connection has
     * been initialised, to false - forcing re-initialisation.
     */
    fun resetUserSession() {
        initState = SESSION_STATE.UNINITIALISED
    }

    /**
     * Sets the max number of times to re-attempt a timed-out request to the Branch API, before
     * considering the request to have failed entirely. Default to 3. Note that the the network
     * timeout, as set in [.setNetworkTimeout], together with the retry interval value from
     * [.setRetryInterval] will determine if the max retry count will be attempted.
     *
     * @param retryCount An [Integer] specifying the number of times to retry before giving
     * up and declaring defeat.
     */
    fun setRetryCount(retryCount: Int) {
        if (prefHelper != null && retryCount >= 0) {
            prefHelper.retryCount = retryCount
        }
    }

    /**
     * Sets the amount of time in milliseconds to wait before re-attempting a timed-out request
     * to the Branch API. Default 1000 ms.
     *
     * @param retryInterval An [Integer] value specifying the number of milliseconds to
     * wait before re-attempting a timed-out request.
     */
    fun setRetryInterval(retryInterval: Int) {
        if (prefHelper != null && retryInterval > 0) {
            prefHelper.retryInterval = retryInterval
        }
    }

    /**
     *
     * Sets the duration in milliseconds that the system should wait for a response before timing
     * out any Branch API. Default 5500 ms. Note that this is the total time allocated for all request
     * retries as set in [.setRetryCount].
     *
     * @param timeout An [Integer] value specifying the number of milliseconds to wait before
     * considering the request to have timed out.
     */
    fun setNetworkTimeout(timeout: Int) {
        if (prefHelper != null && timeout > 0) {
            prefHelper.timeout = timeout
        }
    }

    /**
     *
     * Sets the duration in milliseconds that the system should wait for initializing a network
     * * request.
     *
     * @param connectTimeout An [Integer] value specifying the number of milliseconds to wait before
     * considering the initialization to have timed out.
     */
    fun setNetworkConnectTimeout(connectTimeout: Int) {
        if (prefHelper != null && connectTimeout > 0) {
            prefHelper.connectTimeout = connectTimeout
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
    fun setNoConnectionRetryMax(retryMax: Int) {
        if (prefHelper != null && retryMax > 0) {
            prefHelper.noConnectionRetryMax = retryMax
        }
    }

    /**
     * Sets the window for the referrer GCLID field. The GCLID will be persisted locally from the
     * time it is set + window in milliseconds. Thereafter, it will be deleted.
     *
     * By default, the window is set to 30 days, or 2592000000L in millseconds
     * Minimum of 0 milliseconds
     * Maximum of 3 years
     * @param window A [Long] value specifying the number of milliseconds to wait before
     * deleting the locally persisted GCLID value.
     */
    fun setReferrerGclidValidForWindow(window: Long) {
        if (prefHelper != null) {
            prefHelper.referrerGclidValidForWindow = window
        }
    }

    /**
     * Sets the key-value pairs for debugging the deep link. The key-value set in debug mode is given back with other deep link data on branch init session.
     * This method should be called from onCreate() of activity which listens to Branch Init Session callbacks
     *
     * @param debugParams A [JSONObject] containing key-value pairs for debugging branch deep linking
     */
    fun setDeepLinkDebugMode(debugParams: JSONObject?) {
        deeplinkDebugParams_ = debugParams
    }

    /**
     *
     *
     * Enable Facebook app link check operation during Branch initialisation.
     *
     */
    fun enableFacebookAppLinkCheck() {
        enableFacebookAppLinkCheck_ = true
    }

    /**
     * Enables or disables app tracking with Branch or any other third parties that Branch use internally
     *
     * @param isLimitFacebookTracking `true` to limit app tracking
     */
    fun setLimitFacebookTracking(isLimitFacebookTracking: Boolean) {
        prefHelper!!.setLimitFacebookTracking(isLimitFacebookTracking)
    }

    /**
     *
     * Add key value pairs to all requests
     */
    fun setRequestMetadata(key: String, value: String) {
        prefHelper!!.setRequestMetadata(key, value)
    }

    /**
     *
     *
     * This API allows to tag the install with custom attribute. Add any key-values that qualify or distinguish an install here.
     * Please make sure this method is called before the Branch init, which is on the onStartMethod of first activity.
     * A better place to call this  method is right after Branch#getAutoInstance()
     *
     */
    fun addInstallMetadata(key: String, value: String): Branch {
        prefHelper!!.addInstallMetadata(key, value)
        return this
    }

    /**
     *
     *
     * wrapper method to add the pre-install campaign analytics
     *
     */
    fun setPreinstallCampaign(preInstallCampaign: String): Branch {
        addInstallMetadata(PreinstallKey.campaign.key, preInstallCampaign)
        return this
    }

    /**
     *
     *
     * wrapper method to add the pre-install campaign analytics
     *
     */
    fun setPreinstallPartner(preInstallPartner: String): Branch {
        addInstallMetadata(PreinstallKey.partner.key, preInstallPartner)
        return this
    }

    /*
     * <p>Closes the current session. Should be called by on getting the last actvity onStop() event.
     * </p>
     */
    fun closeSessionInternal() {
        clearPartnerParameters()
        executeClose()
        prefHelper!!.externalIntentUri = null
        trackingController.updateTrackingState(applicationContext) // Update the tracking state for next cold start
    }

    /**
     * Clears all pending requests in the queue
     */
    fun clearPendingRequests() {
        requestQueue_!!.clear()
    }

    /**
     *
     * Perform the state-safe actions required to terminate any open session, and report the
     * closed application event to the Branch API.
     */
    private fun executeClose() {
        if (initState != SESSION_STATE.UNINITIALISED) {
            val req: ServerRequest = ServerRequestRegisterClose(applicationContext)
            if (closeRequestNeeded) {
                handleNewRequest(req)
            } else {
                req.onRequestSucceeded(null, null)
            }
            initState = SESSION_STATE.UNINITIALISED
        }
        closeRequestNeeded = false
    }

    private fun readAndStripParam(data: Uri?, activity: Activity?) {
        if (enableInstantDeepLinking) {

            // If activity is launched anew (i.e. not from stack), then its intent can be readily consumed.
            // Otherwise, we have to wait for onResume, which ensures that we will have the latest intent.
            // In the latter case, IDL works only partially because the callback is delayed until onResume.
            val activityHasValidIntent = intentState_ == INTENT_STATE.READY ||
                    !activityLifeCycleObserver!!.isCurrentActivityLaunchedFromStack

            // Skip IDL if intent contains an unused Branch link.
            val noUnusedBranchLinkInIntent = !isRestartSessionRequested(activity?.intent)
            if (activityHasValidIntent && noUnusedBranchLinkInIntent) {
                extractSessionParamsForIDL(data, activity)
            }
        }
        if (bypassCurrentActivityIntentState_) {
            intentState_ = INTENT_STATE.READY
        }
        if (intentState_ == INTENT_STATE.READY) {

            // Capture the intent URI and extra for analytics in case started by external intents such as google app search
            extractExternalUriAndIntentExtras(data, activity)

            // if branch link is detected we don't need to look for click ID or app link anymore and can terminate early
            if (extractBranchLinkFromIntentExtra(activity)) return

            // Check for link click id or app link
            if (!isActivityLaunchedFromHistory(activity)) {
                // if click ID is detected we don't need to look for app link anymore and can terminate early
                if (extractClickID(data, activity)) return

                // Check if the clicked url is an app link pointing to this app
                extractAppLink(data, activity)
            }
        }
    }

    fun unlockSDKInitWaitLock() {
        if (requestQueue_ == null) return
        requestQueue_.unlockProcessWait(ServerRequest.PROCESS_WAIT_LOCK.SDK_INIT_WAIT_LOCK)
        processNextQueueItem()
    }

    private fun isIntentParamsAlreadyConsumed(activity: Activity?): Boolean {
        return activity != null && activity.intent != null &&
                activity.intent.getBooleanExtra(Defines.IntentKeys.BranchLinkUsed.key, false)
    }

    private fun isActivityLaunchedFromHistory(activity: Activity?): Boolean {
        return activity != null && activity.intent != null && activity.intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0
    }

    /**
     * Package Private.
     * @return the link which opened this application session if opened by a link click.
     */
    val sessionReferredLink: String?
        get() {
            val link = prefHelper!!.externalIntentUri
            return if (link == PrefHelper.NO_STRING_VALUE) null else link
        }

    override fun onAdsParamsFetchFinished() {
        isGAParamsFetchInProgress = false
        requestQueue_!!.unlockProcessWait(ServerRequest.PROCESS_WAIT_LOCK.GAID_FETCH_WAIT_LOCK)

        processNextQueueItem()
    }

    override fun onGoogleInstallReferrerEventsFinished() {
        requestQueue_!!.unlockProcessWait(ServerRequest.PROCESS_WAIT_LOCK.GOOGLE_INSTALL_REFERRER_FETCH_WAIT_LOCK)
        waitingForGoogleInstallReferrer = false
        tryProcessNextQueueItemAfterInstallReferrer()
    }

    override fun onHuaweiInstallReferrerEventsFinished() {
        requestQueue_!!.unlockProcessWait(ServerRequest.PROCESS_WAIT_LOCK.HUAWEI_INSTALL_REFERRER_FETCH_WAIT_LOCK)
        waitingForHuaweiInstallReferrer = false
        tryProcessNextQueueItemAfterInstallReferrer()
    }

    override fun onSamsungInstallReferrerEventsFinished() {
        requestQueue_!!.unlockProcessWait(ServerRequest.PROCESS_WAIT_LOCK.SAMSUNG_INSTALL_REFERRER_FETCH_WAIT_LOCK)
        waitingForSamsungInstallReferrer = false
        tryProcessNextQueueItemAfterInstallReferrer()
    }

    override fun onXiaomiInstallReferrerEventsFinished() {
        requestQueue_!!.unlockProcessWait(ServerRequest.PROCESS_WAIT_LOCK.XIAOMI_INSTALL_REFERRER_FETCH_WAIT_LOCK)
        waitingForXiaomiInstallReferrer = false
        tryProcessNextQueueItemAfterInstallReferrer()
    }

    private fun tryProcessNextQueueItemAfterInstallReferrer() {
        if (!(waitingForGoogleInstallReferrer || waitingForHuaweiInstallReferrer || waitingForSamsungInstallReferrer || waitingForXiaomiInstallReferrer)) {
            val store = StoreReferrerUtils.getLatestValidReferrerStore()
            StoreReferrerUtils.writeLatestInstallReferrer(applicationContext, store)
            processNextQueueItem()
        }
    }

    /**
     * Branch collect the URLs in the incoming intent for better attribution. Branch SDK extensively check for any sensitive data in the URL and skip if exist.
     * However the following method provisions application to set SDK to collect only URLs in particular form. This method allow application to specify a set of regular expressions to white list the URL collection.
     * If whitelist is not empty SDK will collect only the URLs that matches the white list.
     *
     *
     * This method should be called immediately after calling [Branch.getAutoInstance]
     *
     * @param urlWhiteListPattern A regular expression with a URI white listing pattern
     * @return [Branch] instance for successive method calls
     */
    fun addWhiteListedScheme(urlWhiteListPattern: String?): Branch {
        if (urlWhiteListPattern != null) {
            UniversalResourceAnalyser.getInstance(applicationContext)
                .addToAcceptURLFormats(urlWhiteListPattern)
        }
        return this
    }

    /**
     * Branch collect the URLs in the incoming intent for better attribution. Branch SDK extensively check for any sensitive data in the URL and skip if exist.
     * However the following method provisions application to set SDK to collect only URLs in particular form. This method allow application to specify a set of regular expressions to white list the URL collection.
     * If whitelist is not empty SDK will collect only the URLs that matches the white list.
     *
     *
     * This method should be called immediately after calling [Branch.getAutoInstance]
     *
     * @param urlWhiteListPatternList [List] of regular expressions with URI white listing pattern
     * @return [Branch] instance for successive method calls
     */
    fun setWhiteListedSchemes(urlWhiteListPatternList: List<String?>?): Branch {
        if (urlWhiteListPatternList != null) {
            UniversalResourceAnalyser.getInstance(applicationContext)
                .addToAcceptURLFormats(urlWhiteListPatternList)
        }
        return this
    }

    /**
     * Branch collect the URLs in the incoming intent for better attribution. Branch SDK extensively check for any sensitive data in the URL and skip if exist.
     * This method allows applications specify SDK to skip any additional URL patterns to be skipped
     *
     *
     * This method should be called immediately after calling [Branch.getAutoInstance]
     *
     * @param urlSkipPattern [String] A URL pattern that Branch SDK should skip from collecting data
     * @return [Branch] instance for successive method calls
     */
    fun addUriHostsToSkip(urlSkipPattern: String?): Branch {
        if (!TextUtils.isEmpty(urlSkipPattern)) UniversalResourceAnalyser.getInstance(
            applicationContext
        ).addToSkipURLFormats(urlSkipPattern)
        return this
    }

    /**
     * Check and update the URL / URI Skip list in case an update is available.
     */
    fun updateSkipURLFormats() {
        UniversalResourceAnalyser.getInstance(applicationContext).checkAndUpdateSkipURLFormats(
            applicationContext
        )
    }

    /**
     *
     * Identifies the current user to the Branch API by supplying a unique identifier as a
     * [String] value. No callback.
     *
     * @param userId A [String] value containing the unique identifier of the user.
     */
    fun setIdentity(userId: String) {
        setIdentity(userId, null)
    }

    /**
     *
     * Identifies the current user to the Branch API by supplying a unique identifier as a
     * [String] value, with a callback specified to perform a defined action upon successful
     * response to request.
     *
     * @param userId   A [String] value containing the unique identifier of the user.
     * @param callback A [BranchReferralInitListener] callback instance that will return
     * the data associated with the user id being assigned, if available.
     */
    fun setIdentity(userId: String, callback: BranchReferralInitListener?) {
        installDeveloperId = userId
        val req = ServerRequestIdentifyUserRequest(applicationContext, callback, userId)
        if (!req.constructError_ && !req.handleErrors(applicationContext)) {
            handleNewRequest(req)
        } else {
            if (req.isExistingID) {
                req.handleUserExist(branchReferral_)
            }
        }
    }

    /**
     * Gets all available cross platform ids.
     *
     * @param callback An instance of [io.branch.referral.ServerRequestGetCPID.BranchCrossPlatformIdListener]
     * to callback with cross platform ids
     */
    fun getCrossPlatformIds(callback: BranchCrossPlatformIdListener?) {
        if (applicationContext != null) {
            handleNewRequest(ServerRequestGetCPID(applicationContext, callback))
        }
    }

    /**
     * Gets the available last attributed touch data. The attribution window is set to the value last
     * saved via PreferenceHelper.setLATDAttributionWindow(). If no value has been saved, Branch
     * defaults to a 30 day attribution window (SDK sends -1 to request the default from the server).
     *
     * @param callback An instance of [io.branch.referral.ServerRequestGetLATD.BranchLastAttributedTouchDataListener]
     * to callback with last attributed touch data
     */
    fun getLastAttributedTouchData(callback: BranchLastAttributedTouchDataListener?) {
        if (applicationContext != null) {
            handleNewRequest(
                ServerRequestGetLATD(
                    applicationContext,
                    Defines.RequestPath.GetLATD,
                    callback
                )
            )
        }
    }

    /**
     * Gets the available last attributed touch data with a custom set attribution window.
     *
     * @param callback An instance of [io.branch.referral.ServerRequestGetLATD.BranchLastAttributedTouchDataListener]
     * to callback with last attributed touch data
     * @param attributionWindow An [int] to bound the the window of time in days during which
     * the attribution data is considered valid. Note that, server side, the
     * maximum value is 90.
     */
    fun getLastAttributedTouchData(
        callback: BranchLastAttributedTouchDataListener?,
        attributionWindow: Int
    ) {
        if (applicationContext != null) {
            handleNewRequest(
                ServerRequestGetLATD(
                    applicationContext,
                    Defines.RequestPath.GetLATD,
                    callback,
                    attributionWindow
                )
            )
        }
    }

    /**
     * Indicates whether or not this user has a custom identity specified for them. Note that this is independent of installs.
     * If you call setIdentity, this device will have that identity associated with this user until logout is called.
     * This includes persisting through uninstalls, as we track device id.
     *
     * @return A [Boolean] value that will return *true* only if user already has an identity.
     */
    val isUserIdentified: Boolean
        get() = prefHelper!!.identity != PrefHelper.NO_STRING_VALUE
    /**
     *
     * This method should be called if you know that a different person is about to use the app. For example,
     * if you allow users to log out and let their friend use the app, you should call this to notify Branch
     * to create a new user for this device. This will clear the first and latest params, as a new session is created.
     *
     * @param callback An instance of [io.branch.referral.Branch.LogoutStatusListener] to callback with the logout operation status.
     */
    /**
     *
     * This method should be called if you know that a different person is about to use the app. For example,
     * if you allow users to log out and let their friend use the app, you should call this to notify Branch
     * to create a new user for this device. This will clear the first and latest params, as a new session is created.
     */
    @JvmOverloads
    fun logout(callback: LogoutStatusListener? = null) {
        val req: ServerRequest = ServerRequestLogout(applicationContext, callback)
        if (!req.constructError_ && !req.handleErrors(applicationContext)) {
            handleNewRequest(req)
        }
    }

    /**
     *
     * A void call to indicate that the user has performed a specific action and for that to be
     * reported to the Branch API, with additional app-defined meta data to go along with that action.
     *
     * @param action   A [String] value to be passed as an action that the user has carried
     * out. For example "registered" or "logged in".
     * @param metadata A [JSONObject] containing app-defined meta-data to be attached to a
     * user action that has just been completed.
     */
    @Deprecated(
        """Please use {@link BranchEvent} for your event tracking use cases.
                      Content, Lifecycle and Custom Events</a> for additional information."""
    )
    fun userCompletedAction(action: String, metadata: JSONObject?) {
        userCompletedAction(action, metadata, null)
    }

    /**
     *
     * A void call to indicate that the user has performed a specific action and for that to be
     * reported to the Branch API.
     *
     * @param action A [String] value to be passed as an action that the user has carried
     * out. For example "registered" or "logged in".
     */
    @Deprecated(
        """Please use {@link BranchEvent} for your event tracking use cases.
                    Content, Lifecycle and Custom Events</a> for additional information."""
    )
    fun userCompletedAction(action: String) {
        userCompletedAction(action, null, null)
    }

    /**
     *
     * A void call to indicate that the user has performed a specific action and for that to be
     * reported to the Branch API.
     *
     * @param action   A [String] value to be passed as an action that the user has carried
     * out. For example "registered" or "logged in".
     * @param callback instance of [BranchViewHandler.IBranchViewEvents] to listen Branch view events
     */
    @Deprecated(
        """Please use {@link BranchEvent} for your event tracking use cases.
                      Content, Lifecycle and Custom Events</a> for additional information."""
    )
    fun userCompletedAction(action: String, callback: IBranchViewEvents?) {
        userCompletedAction(action, null, callback)
    }

    /**
     *
     * A void call to indicate that the user has performed a specific action and for that to be
     * reported to the Branch API, with additional app-defined meta data to go along with that action.
     *
     * @param action   A [String] value to be passed as an action that the user has carried
     * out. For example "registered" or "logged in".
     * @param metadata A [JSONObject] containing app-defined meta-data to be attached to a
     * user action that has just been completed.
     * @param callback instance of [BranchViewHandler.IBranchViewEvents] to listen Branch view events
     */
    @Deprecated(
        """Please use {@link BranchEvent} for your event tracking use cases.
                      Content, Lifecycle and Custom Events</a> for additional information."""
    )
    fun userCompletedAction(
        action: String, metadata: JSONObject?,
        callback: IBranchViewEvents?
    ) {
        PrefHelper.LogAlways("'userCompletedAction' has been deprecated. Please use BranchEvent for your event tracking use cases.You can refer to  https://help.branch.io/developers-hub/docs/tracking-commerce-content-lifecycle-and-custom-events for additional information.")
        val req: ServerRequest = ServerRequestActionCompleted(
            applicationContext,
            action, null, metadata, callback
        )
        if (!req.constructError_ && !req.handleErrors(applicationContext)) {
            handleNewRequest(req)
        }
    }

    @Deprecated(
        """Please use {@link BranchEvent} for your event tracking use cases.You can refer to
                   Content, Lifecycle and Custom Events</a> for additional information."""
    )
    fun sendCommerceEvent(
        commerceEvent: CommerceEvent, metadata: JSONObject?,
        callback: IBranchViewEvents?
    ) {
        PrefHelper.LogAlways("'sendCommerceEvent' has been deprecated. Please use BranchEvent for your event tracking use cases.You can refer to  https://help.branch.io/developers-hub/docs/tracking-commerce-content-lifecycle-and-custom-events for additional information.")
        val req: ServerRequest = ServerRequestActionCompleted(
            applicationContext,
            BRANCH_STANDARD_EVENT.PURCHASE.getName(), commerceEvent, metadata, callback
        )
        if (!req.constructError_ && !req.handleErrors(applicationContext)) {
            handleNewRequest(req)
        }
    }

    @Deprecated(
        """Please use {@link BranchEvent} for your event tracking use cases.You can refer to
                   Content, Lifecycle and Custom Events</a> for additional information."""
    )
    fun sendCommerceEvent(commerceEvent: CommerceEvent) {
        sendCommerceEvent(commerceEvent, null, null)
    }

    /**
     *
     * Returns the parameters associated with the link that referred the user. This is only set once,
     * the first time the user is referred by a link. Think of this as the user referral parameters.
     * It is also only set if isReferrable is equal to true, which by default is only true
     * on a fresh install (not upgrade or reinstall). This will change on setIdentity (if the
     * user already exists from a previous device) and logout.
     *
     * @return A [JSONObject] containing the install-time parameters as configured
     * locally.
     */
    val firstReferringParams: JSONObject?
        get() {
            val storedParam = prefHelper!!.installParams
            var firstReferringParams: JSONObject? = convertParamsStringToDictionary(storedParam)
            firstReferringParams = appendDebugParams(firstReferringParams)
            return firstReferringParams
        }

    fun removeSessionInitializationDelay() {
        requestQueue_!!.unlockProcessWait(ServerRequest.PROCESS_WAIT_LOCK.USER_SET_WAIT_LOCK)
        processNextQueueItem()
    }

    /**
     *
     * This function must be called from a non-UI thread! If Branch has no install link data,
     * and this func is called, it will return data upon initializing, or until LATCH_WAIT_UNTIL.
     * Returns the parameters associated with the link that referred the user. This is only set once,
     * the first time the user is referred by a link. Think of this as the user referral parameters.
     * It is also only set if isReferrable is equal to true, which by default is only true
     * on a fresh install (not upgrade or reinstall). This will change on setIdentity (if the
     * user already exists from a previous device) and logout.
     *
     * @return A [JSONObject] containing the install-time parameters as configured
     * locally.
     */
    val firstReferringParamsSync: JSONObject?
        get() {
            getFirstReferringParamsLatch = CountDownLatch(1)
            if (prefHelper!!.installParams == PrefHelper.NO_STRING_VALUE) {
                try {
                    getFirstReferringParamsLatch!!.await(
                        LATCH_WAIT_UNTIL.toLong(),
                        TimeUnit.MILLISECONDS
                    )
                } catch (e: InterruptedException) {
                }
            }
            val storedParam = prefHelper.installParams
            var firstReferringParams: JSONObject? = convertParamsStringToDictionary(storedParam)
            firstReferringParams = appendDebugParams(firstReferringParams)
            getFirstReferringParamsLatch = null
            return firstReferringParams
        }

    /**
     *
     * Returns the parameters associated with the link that referred the session. If a user
     * clicks a link, and then opens the app, initSession will return the parameters of the link
     * and then set them in as the latest parameters to be retrieved by this method. By default,
     * sessions persist for the duration of time that the app is in focus. For example, if you
     * minimize the app, these parameters will be cleared when closeSession is called.
     *
     * @return A [JSONObject] containing the latest referring parameters as
     * configured locally.
     */
    val latestReferringParams: JSONObject?
        get() {
            val storedParam = prefHelper!!.sessionParams
            var latestParams: JSONObject? = convertParamsStringToDictionary(storedParam)
            latestParams = appendDebugParams(latestParams)
            return latestParams
        }

    /**
     *
     * This function must be called from a non-UI thread! If Branch has not been initialized
     * and this func is called, it will return data upon initialization, or until LATCH_WAIT_UNTIL.
     * Returns the parameters associated with the link that referred the session. If a user
     * clicks a link, and then opens the app, initSession will return the parameters of the link
     * and then set them in as the latest parameters to be retrieved by this method. By default,
     * sessions persist for the duration of time that the app is in focus. For example, if you
     * minimize the app, these parameters will be cleared when closeSession is called.
     *
     * @return A [JSONObject] containing the latest referring parameters as
     * configured locally.
     */
    val latestReferringParamsSync: JSONObject?
        get() {
            getLatestReferringParamsLatch = CountDownLatch(1)
            try {
                if (initState != SESSION_STATE.INITIALISED) {
                    getLatestReferringParamsLatch!!.await(
                        LATCH_WAIT_UNTIL.toLong(),
                        TimeUnit.MILLISECONDS
                    )
                }
            } catch (e: InterruptedException) {
            }
            val storedParam = prefHelper!!.sessionParams
            var latestParams: JSONObject? = convertParamsStringToDictionary(storedParam)
            latestParams = appendDebugParams(latestParams)
            getLatestReferringParamsLatch = null
            return latestParams
        }

    /**
     * Add a Partner Parameter for Facebook.
     * Once set, this parameter is attached to installs, opens and events until cleared or the app restarts.
     *
     * See Facebook's documentation for details on valid parameters
     */
    fun addFacebookPartnerParameterWithName(key: String, value: String) {
        if (!trackingController.isTrackingDisabled) {
            prefHelper!!.partnerParams_.addFacebookParameter(key, value)
        }
    }

    /**
     * Add a Partner Parameter for Snap.
     * Once set, this parameter is attached to installs, opens and events until cleared or the app restarts.
     *
     * See Snap's documentation for details on valid parameters
     */
    fun addSnapPartnerParameterWithName(key: String, value: String) {
        if (!trackingController.isTrackingDisabled) {
            prefHelper!!.partnerParams_.addSnapParameter(key, value)
        }
    }

    /**
     * Clears all Partner Parameters
     */
    fun clearPartnerParameters() {
        prefHelper!!.partnerParams_.clearAllParameters()
    }

    /**
     * Append the deep link debug params to the original params
     *
     * @param originalParams A [JSONObject] original referrer parameters
     * @return A new [JSONObject] with debug params appended.
     */
    private fun appendDebugParams(originalParams: JSONObject?): JSONObject? {
        try {
            if (originalParams != null && deeplinkDebugParams_ != null) {
                if (deeplinkDebugParams_!!.length() > 0) {
                    PrefHelper.Debug("You're currently in deep link debug mode. Please comment out 'setDeepLinkDebugMode' to receive the deep link parameters from a real Branch link")
                }
                val keys = deeplinkDebugParams_!!.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    originalParams.put(key, deeplinkDebugParams_!![key])
                }
            }
        } catch (ignore: Exception) {
        }
        return originalParams
    }

    val deeplinkDebugParams: JSONObject?
        get() {
            if (deeplinkDebugParams_ != null && deeplinkDebugParams_!!.length() > 0) {
                PrefHelper.Debug("You're currently in deep link debug mode. Please comment out 'setDeepLinkDebugMode' to receive the deep link parameters from a real Branch link")
            }
            return deeplinkDebugParams_
        }
    //-----------------Generate Short URL      -------------------------------------------//
    /**
     *
     *  Generates a shorl url for the given [ServerRequestCreateUrl] object
     *
     * @param req An instance  of [ServerRequestCreateUrl] with parameters create the short link.
     * @return A url created with the given request if the request is synchronous else null.
     * Note : This method can be used only internally. Use [BranchUrlBuilder] for creating short urls.
     */
    fun generateShortLinkInternal(req: ServerRequestCreateUrl): String? {
        if (!req.constructError_ && !req.handleErrors(applicationContext)) {
            if (linkCache_.containsKey(req.linkPost)) {
                val url = linkCache_[req.linkPost]
                req.onUrlAvailable(url)
                return url
            }
            if (req.isAsync) {
                handleNewRequest(req)
            } else {
                return generateShortLinkSync(req)
            }
        }
        return null
    }

    /**
     *
     * Creates options for sharing a link with other Applications. Creates a link with given attributes and shares with the
     * user selected clients.
     *
     * @param builder A [BranchShareSheetBuilder] instance to build share link.
     */
    fun shareLink(builder: BranchShareSheetBuilder?) {
        //Cancel any existing sharing in progress.
        if (shareLinkManager != null) {
            shareLinkManager!!.cancelShareLinkDialog(true)
        }
        shareLinkManager = ShareLinkManager()
        shareLinkManager!!.shareLink(builder)
    }

    /**
     *
     * Cancel current share link operation and Application selector dialog. If your app is not using auto session management, make sure you are
     * calling this method before your activity finishes inorder to prevent any window leak.
     *
     * @param animateClose A [Boolean] to specify whether to close the dialog with an animation.
     * A value of true will close the dialog with an animation. Setting this value
     * to false will close the Dialog immediately.
     */
    fun cancelShareLinkDialog(animateClose: Boolean) {
        if (shareLinkManager != null) {
            shareLinkManager!!.cancelShareLinkDialog(animateClose)
        }
    }

    // PRIVATE FUNCTIONS
    private fun generateShortLinkSync(req: ServerRequestCreateUrl): String? {
        if (trackingController.isTrackingDisabled) {
            return req.longUrl
        }
        if (initState == SESSION_STATE.INITIALISED) {
            var response: ServerResponse? = null
            try {
                val timeOut =
                    prefHelper!!.timeout + 2000 // Time out is set to slightly more than link creation time to prevent any edge case
                response = GetShortLinkTask().execute(req)[timeOut.toLong(), TimeUnit.MILLISECONDS]
            } catch (ignore: InterruptedException) {
            } catch (ignore: ExecutionException) {
            } catch (ignore: TimeoutException) {
            }
            var url: String? = null
            if (req.isDefaultToLongUrl) {
                url = req.longUrl
            }
            if (response != null && response.statusCode == HttpURLConnection.HTTP_OK) {
                try {
                    url = response.getObject().getString("url")
                    if (req.linkPost != null) {
                        linkCache_[req.linkPost] = url
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
            return url
        } else {
            PrefHelper.Debug("Warning: User session has not been initialized")
        }
        return null
    }

    private fun convertParamsStringToDictionary(paramString: String): JSONObject {
        return if (paramString == PrefHelper.NO_STRING_VALUE) {
            JSONObject()
        } else {
            try {
                JSONObject(paramString)
            } catch (e: JSONException) {
                val encodedArray = Base64.decode(paramString.toByteArray(), Base64.NO_WRAP)
                try {
                    JSONObject(String(encodedArray))
                } catch (ex: JSONException) {
                    ex.printStackTrace()
                    JSONObject()
                }
            }
        }
    }

    fun processNextQueueItem() {
        try {
            serverSema_.acquire()
            if (networkCount_ == 0 && requestQueue_!!.size > 0) {
                networkCount_ = 1
                val req = requestQueue_.peek()
                serverSema_.release()
                if (req != null) {
                    PrefHelper.Debug("processNextQueueItem, req " + req.javaClass.simpleName)
                    if (!req.isWaitingOnProcessToFinish) {
                        // All request except Install request need a valid RandomizedBundleToken
                        if (req !is ServerRequestRegisterInstall && !hasUser()) {
                            PrefHelper.Debug("Branch Error: User session has not been initialized!")
                            networkCount_ = 0
                            req.handleFailure(BranchError.ERR_NO_SESSION, "")
                        } else if (requestNeedsSession(req) && !isSessionAvailableForRequest) {
                            networkCount_ = 0
                            req.handleFailure(BranchError.ERR_NO_SESSION, "")
                        } else {
                            executeTimedBranchPostTask(req, prefHelper!!.taskTimeout)
                        }
                    } else {
                        networkCount_ = 0
                    }
                } else {
                    requestQueue_.remove(null) //In case there is any request nullified remove it.
                }
            } else {
                serverSema_.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun executeTimedBranchPostTask(req: ServerRequest, timeout: Int) {
        val latch = CountDownLatch(1)
        val postTask = BranchPostTask(req, latch)
        postTask.executeTask()
        if (Looper.myLooper() == Looper.getMainLooper()) {
            Thread { awaitTimedBranchPostTask(latch, timeout, postTask) }.start()
        } else {
            awaitTimedBranchPostTask(latch, timeout, postTask)
        }
    }

    private fun awaitTimedBranchPostTask(
        latch: CountDownLatch,
        timeout: Int,
        postTask: BranchPostTask
    ) {
        try {
            if (!latch.await(timeout.toLong(), TimeUnit.MILLISECONDS)) {
                postTask.cancel(true)
                postTask.onPostExecuteInner(
                    ServerResponse(
                        postTask.thisReq_.requestPath,
                        BranchError.ERR_BRANCH_TASK_TIMEOUT,
                        ""
                    )
                )
            }
        } catch (e: InterruptedException) {
            postTask.cancel(true)
            postTask.onPostExecuteInner(
                ServerResponse(
                    postTask.thisReq_.requestPath,
                    BranchError.ERR_BRANCH_TASK_TIMEOUT,
                    ""
                )
            )
        }
    }

    // Determine if a Request needs a Session to proceed.
    private fun requestNeedsSession(request: ServerRequest): Boolean {
        if (request is ServerRequestInitSession) {
            return false
        } else if (request is ServerRequestCreateUrl) {
            return false
        }

        // All other Request Types need a session.
        return true
    }

    // Determine if a Session is available for a Request to proceed.
    private val isSessionAvailableForRequest: Boolean
        get() = hasSession() && hasRandomizedDeviceToken()

    fun updateAllRequestsInQueue() {
        try {
            for (i in 0 until requestQueue_!!.size) {
                val req = requestQueue_.peekAt(i)
                if (req != null) {
                    val reqJson = req.post
                    if (reqJson != null) {
                        if (reqJson.has(Defines.Jsonkey.SessionID.key)) {
                            req.post.put(Defines.Jsonkey.SessionID.key, prefHelper!!.sessionID)
                        }
                        if (reqJson.has(Defines.Jsonkey.RandomizedBundleToken.key)) {
                            req.post.put(
                                Defines.Jsonkey.RandomizedBundleToken.key,
                                prefHelper!!.randomizedBundleToken
                            )
                        }
                        if (reqJson.has(Defines.Jsonkey.RandomizedDeviceToken.key)) {
                            req.post.put(
                                Defines.Jsonkey.RandomizedDeviceToken.key,
                                prefHelper!!.randomizedDeviceToken
                            )
                        }
                    }
                }
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    fun setIntentState(intentState: INTENT_STATE) {
        intentState_ = intentState
    }

    private fun hasSession(): Boolean {
        return prefHelper!!.sessionID != PrefHelper.NO_STRING_VALUE
    }

    fun setInstantDeepLinkPossible(instantDeepLinkPossible: Boolean) {
        isInstantDeepLinkPossible = instantDeepLinkPossible
    }

    fun isInstantDeepLinkPossible(): Boolean {
        return isInstantDeepLinkPossible
    }

    private fun hasRandomizedDeviceToken(): Boolean {
        return prefHelper!!.randomizedDeviceToken != PrefHelper.NO_STRING_VALUE
    }

    private fun hasUser(): Boolean {
        return prefHelper!!.randomizedBundleToken != PrefHelper.NO_STRING_VALUE
    }

    private fun insertRequestAtFront(req: ServerRequest) {
        if (networkCount_ == 0) {
            requestQueue_!!.insert(req, 0)
        } else {
            requestQueue_!!.insert(req, 1)
        }
    }

    private fun initializeSession(initRequest: ServerRequestInitSession, delay: Int) {
        if (prefHelper!!.branchKey == null || prefHelper.branchKey.equals(
                PrefHelper.NO_STRING_VALUE,
                ignoreCase = true
            )
        ) {
            initState = SESSION_STATE.UNINITIALISED
            //Report Key error on callback
            if (initRequest.callback_ != null) {
                initRequest.callback_.onInitFinished(
                    null,
                    BranchError("Trouble initializing Branch.", BranchError.ERR_BRANCH_KEY_INVALID)
                )
            }
            PrefHelper.Debug("Warning: Please enter your branch_key in your project's manifest")
            return
        } else if (BranchUtil.isTestModeEnabled()) {
            PrefHelper.Debug("Warning: You are using your test app's Branch Key. Remember to change it to live Branch Key during deployment.")
        }
        if (initState == SESSION_STATE.UNINITIALISED && sessionReferredLink == null && enableFacebookAppLinkCheck_) {
            // Check if opened by facebook with deferred install data
            val appLinkRqSucceeded = DeferredAppLinkDataHandler.fetchDeferredAppLinkData(
                applicationContext
            ) { nativeAppLinkUrl ->
                prefHelper.isAppLinkTriggeredInit =
                    true // callback returns when app link fetch finishes with success or failure. Report app link checked in both cases
                if (nativeAppLinkUrl != null) {
                    val appLinkUri = Uri.parse(nativeAppLinkUrl)
                    val bncLinkClickId =
                        appLinkUri.getQueryParameter(Defines.Jsonkey.LinkClickID.key)
                    if (!TextUtils.isEmpty(bncLinkClickId)) {
                        prefHelper.linkClickIdentifier = bncLinkClickId
                    }
                }
                requestQueue_!!.unlockProcessWait(ServerRequest.PROCESS_WAIT_LOCK.FB_APP_LINK_WAIT_LOCK)
                processNextQueueItem()
            }
            if (appLinkRqSucceeded) {
                initRequest.addProcessWaitLock(ServerRequest.PROCESS_WAIT_LOCK.FB_APP_LINK_WAIT_LOCK)
            }
        }
        if (delay > 0) {
            initRequest.addProcessWaitLock(ServerRequest.PROCESS_WAIT_LOCK.USER_SET_WAIT_LOCK)
            Handler().postDelayed({ removeSessionInitializationDelay() }, delay.toLong())
        }

        // Re 'forceBranchSession':
        // Check if new session is being forced. There are two use cases for setting the ForceNewBranchSession to true:
        // 1. Launch an activity via a push notification while app is in foreground but does not have
        // the particular activity in the backstack, in such cases, users can't utilize reInitSession() because
        // it's called from onNewIntent() which is never invoked
        // todo: this is tricky for users, get rid of ForceNewBranchSession if possible. (if flag is not set, the content from Branch link is lost)
        // 2. Some users navigate their apps via Branch links so they would have to set ForceNewBranchSession to true
        // which will blow up the session count in analytics but does the job.
        val intent = if (currentActivity != null) currentActivity!!.intent else null
        val forceBranchSession = isRestartSessionRequested(intent)
        if (initState == SESSION_STATE.UNINITIALISED || forceBranchSession) {
            if (forceBranchSession && intent != null) {
                intent.removeExtra(Defines.IntentKeys.ForceNewBranchSession.key) // SDK-881, avoid double initialization
            }
            registerAppInit(initRequest, false)
        } else if (initRequest.callback_ != null) {
            // Else, let the user know session initialization failed because it's already initialized.
            initRequest.callback_.onInitFinished(
                null,
                BranchError("Warning.", BranchError.ERR_BRANCH_ALREADY_INITIALIZED)
            )
        }
    }

    /**
     * Registers app init with params filtered from the intent. Unless ignoreIntent = true, this
     * will wait on the wait locks to complete any pending operations
     */
    fun registerAppInit(request: ServerRequestInitSession, ignoreWaitLocks: Boolean) {
        initState = SESSION_STATE.INITIALISING
        if (!ignoreWaitLocks) {
            // Single top activities can be launched from stack and there may be a new intent provided with onNewIntent() call.
            // In this case need to wait till onResume to get the latest intent. Bypass this if bypassWaitingForIntent_ is true.
            if (intentState_ != INTENT_STATE.READY && isWaitingForIntent) {
                request.addProcessWaitLock(ServerRequest.PROCESS_WAIT_LOCK.INTENT_PENDING_WAIT_LOCK)
            }

            // Google Play Referrer lib should only be used once, so we use GooglePlayStoreAttribution.hasBeenUsed flag
            // just in case user accidentally queues up a couple install requests at the same time. During later sessions
            // request instanceof ServerRequestRegisterInstall = false
            if (checkInstallReferrer_ && request is ServerRequestRegisterInstall) {

                // We may need to check if play store services exist, in the future
                // Obtain all needed locks before executing any fetches
                if (!StoreReferrerGooglePlayStore.hasBeenUsed) {
                    waitingForGoogleInstallReferrer = true
                    request.addProcessWaitLock(ServerRequest.PROCESS_WAIT_LOCK.GOOGLE_INSTALL_REFERRER_FETCH_WAIT_LOCK)
                }
                if (classExists("com.huawei.hms.ads.installreferrer.api.InstallReferrerClient")
                    && !StoreReferrerHuaweiAppGallery.hasBeenUsed
                ) {
                    waitingForHuaweiInstallReferrer = true
                    request.addProcessWaitLock(ServerRequest.PROCESS_WAIT_LOCK.HUAWEI_INSTALL_REFERRER_FETCH_WAIT_LOCK)
                }
                if (classExists("com.sec.android.app.samsungapps.installreferrer.api.InstallReferrerClient")
                    && !StoreReferrerSamsungGalaxyStore.hasBeenUsed
                ) {
                    waitingForSamsungInstallReferrer = true
                    request.addProcessWaitLock(ServerRequest.PROCESS_WAIT_LOCK.SAMSUNG_INSTALL_REFERRER_FETCH_WAIT_LOCK)
                }
                if (classExists("com.miui.referrer.api.GetAppsReferrerClient")
                    && !StoreReferrerXiaomiGetApps.hasBeenUsed
                ) {
                    waitingForXiaomiInstallReferrer = true
                    request.addProcessWaitLock(ServerRequest.PROCESS_WAIT_LOCK.XIAOMI_INSTALL_REFERRER_FETCH_WAIT_LOCK)
                }
                if (waitingForGoogleInstallReferrer) {
                    StoreReferrerGooglePlayStore.fetch(applicationContext, this)
                }
                if (waitingForHuaweiInstallReferrer) {
                    StoreReferrerHuaweiAppGallery.fetch(applicationContext, this)
                }
                if (waitingForSamsungInstallReferrer) {
                    StoreReferrerSamsungGalaxyStore.fetch(applicationContext, this)
                }
                if (waitingForXiaomiInstallReferrer) {
                    StoreReferrerXiaomiGetApps.fetch(applicationContext, this)
                }

                // StoreReferrer error are thrown synchronously, so we remove
                // *_INSTALL_REFERRER_FETCH_WAIT_LOCK manually
                if (StoreReferrerGooglePlayStore.erroredOut) {
                    request.removeProcessWaitLock(ServerRequest.PROCESS_WAIT_LOCK.GOOGLE_INSTALL_REFERRER_FETCH_WAIT_LOCK)
                }
                if (StoreReferrerHuaweiAppGallery.erroredOut) {
                    request.removeProcessWaitLock(ServerRequest.PROCESS_WAIT_LOCK.HUAWEI_INSTALL_REFERRER_FETCH_WAIT_LOCK)
                }
                if (StoreReferrerSamsungGalaxyStore.erroredOut) {
                    request.removeProcessWaitLock(ServerRequest.PROCESS_WAIT_LOCK.SAMSUNG_INSTALL_REFERRER_FETCH_WAIT_LOCK)
                }
                if (StoreReferrerXiaomiGetApps.erroredOut) {
                    request.removeProcessWaitLock(ServerRequest.PROCESS_WAIT_LOCK.XIAOMI_INSTALL_REFERRER_FETCH_WAIT_LOCK)
                }
            }
        }
        if (isGAParamsFetchInProgress) {
            request.addProcessWaitLock(ServerRequest.PROCESS_WAIT_LOCK.GAID_FETCH_WAIT_LOCK)
        }
        val r = requestQueue_!!.selfInitRequest
        if (r == null) {
            insertRequestAtFront(request)
            processNextQueueItem()
        } else {
            r.callback_ = request.callback_
        }
    }

    private fun classExists(className: String): Boolean {
        return try {
            Class.forName(className)
            true
        } catch (e: ClassNotFoundException) {
            PrefHelper.Debug("Could not find $className. If expected, import the dependency into your app.")
            false
        }
    }

    fun getInstallOrOpenRequest(
        callback: BranchReferralInitListener?,
        isAutoInitialization: Boolean
    ): ServerRequestInitSession {
        val request: ServerRequestInitSession
        request = if (hasUser()) {
            // If there is user this is open
            ServerRequestRegisterOpen(applicationContext, callback, isAutoInitialization)
        } else {
            // If no user this is an Install
            ServerRequestRegisterInstall(applicationContext, callback, isAutoInitialization)
        }
        return request
    }

    fun onIntentReady(activity: Activity) {
        setIntentState(INTENT_STATE.READY)
        requestQueue_!!.unlockProcessWait(ServerRequest.PROCESS_WAIT_LOCK.INTENT_PENDING_WAIT_LOCK)
        val grabIntentParams = activity.intent != null && initState != SESSION_STATE.INITIALISED
        if (grabIntentParams) {
            val intentData = activity.intent.data
            readAndStripParam(intentData, activity)
        }
        processNextQueueItem()
    }

    /**
     * Handles execution of a new request other than open or install.
     * Checks for the session initialisation and adds a install/Open request in front of this request
     * if the request need session to execute.
     *
     * @param req The [ServerRequest] to execute
     */
    fun handleNewRequest(req: ServerRequest) {
        // If Tracking is disabled fail all messages with ERR_BRANCH_TRACKING_DISABLED
        if (trackingController.isTrackingDisabled && !req.prepareExecuteWithoutTracking()) {
            PrefHelper.Debug("Requested operation cannot be completed since tracking is disabled [" + req.requestPath_.path + "]")
            req.handleFailure(BranchError.ERR_BRANCH_TRACKING_DISABLED, "")
            return
        }
        //If not initialised put an open or install request in front of this request(only if this needs session)
        if (initState != SESSION_STATE.INITIALISED && req !is ServerRequestInitSession) {
            if (req is ServerRequestLogout) {
                req.handleFailure(BranchError.ERR_NO_SESSION, "")
                PrefHelper.Debug("Branch is not initialized, cannot logout")
                return
            }
            if (req is ServerRequestRegisterClose) {
                PrefHelper.Debug("Branch is not initialized, cannot close session")
                return
            }
            if (requestNeedsSession(req)) {
                req.addProcessWaitLock(ServerRequest.PROCESS_WAIT_LOCK.SDK_INIT_WAIT_LOCK)
            }
        }
        requestQueue_!!.enqueue(req)
        req.onRequestQueued()
        processNextQueueItem()
    }

    /**
     * Notify Branch when network is available in order to process the next request in the queue.
     */
    fun notifyNetworkAvailable() {
        processNextQueueItem()
    }

    private fun setActivityLifeCycleObserver(application: Application) {
        try {
            activityLifeCycleObserver = BranchActivityLifecycleObserver()
            /* Set an observer for activity life cycle events. */application.unregisterActivityLifecycleCallbacks(
                activityLifeCycleObserver
            )
            application.registerActivityLifecycleCallbacks(activityLifeCycleObserver)
            isActivityLifeCycleCallbackRegistered_ = true
        } catch (Ex: NoSuchMethodError) {
            isActivityLifeCycleCallbackRegistered_ = false
            /* LifeCycleEvents are  available only from API level 14. */PrefHelper.Debug(
                BranchError(
                    "",
                    BranchError.ERR_API_LVL_14_NEEDED
                ).message
            )
        } catch (Ex: NoClassDefFoundError) {
            isActivityLifeCycleCallbackRegistered_ = false
            PrefHelper.Debug(BranchError("", BranchError.ERR_API_LVL_14_NEEDED).message)
        }
    }

    /*
     * Check for forced session restart. The Branch session is restarted if the incoming intent has branch_force_new_session set to true.
     * This is for supporting opening a deep link path while app is already running in the foreground. Such as clicking push notification while app (namely, LauncherActivity) is in foreground.
     */
    fun isRestartSessionRequested(intent: Intent?): Boolean {
        return checkIntentForSessionRestart(intent) || checkIntentForUnusedBranchLink(intent)
    }

    private fun checkIntentForSessionRestart(intent: Intent?): Boolean {
        var forceSessionIntentKeyPresent = false
        if (intent != null) {
            forceSessionIntentKeyPresent =
                intent.getBooleanExtra(Defines.IntentKeys.ForceNewBranchSession.key, false)
        }
        return forceSessionIntentKeyPresent
    }

    private fun checkIntentForUnusedBranchLink(intent: Intent?): Boolean {
        var hasUnusedBranchLink = false
        if (intent != null) {
            val hasBranchLink = intent.getStringExtra(Defines.IntentKeys.BranchURI.key) != null
            val branchLinkNotConsumedYet =
                !intent.getBooleanExtra(Defines.IntentKeys.BranchLinkUsed.key, false)
            hasUnusedBranchLink = hasBranchLink && branchLinkNotConsumedYet
        }
        return hasUnusedBranchLink
    }

    /**
     *
     * An Interface class that is implemented by all classes that make use of
     * [BranchReferralInitListener], defining a single method that takes a list of params in
     * [JSONObject] format, and an error message of [BranchError] format that will be
     * returned on failure of the request response.
     *
     * @see JSONObject
     *
     * @see BranchError
     */
    interface BranchReferralInitListener {
        fun onInitFinished(referringParams: JSONObject?, error: BranchError?)
    }

    /**
     *
     * An Interface class that is implemented by all classes that make use of
     * [BranchUniversalReferralInitListener], defining a single method that provides
     * [BranchUniversalObject], [LinkProperties] and an error message of [BranchError] format that will be
     * returned on failure of the request response.
     * In case of an error the value for [BranchUniversalObject] and [LinkProperties] are set to null.
     *
     * @see BranchUniversalObject
     *
     * @see LinkProperties
     *
     * @see BranchError
     */
    interface BranchUniversalReferralInitListener {
        fun onInitFinished(
            branchUniversalObject: BranchUniversalObject?,
            linkProperties: LinkProperties?,
            error: BranchError?
        )
    }

    /**
     *
     * An Interface class that is implemented by all classes that make use of
     * [BranchReferralStateChangedListener], defining a single method that takes a value of
     * [Boolean] format, and an error message of [BranchError] format that will be
     * returned on failure of the request response.
     *
     * @see Boolean
     *
     * @see BranchError
     */
    interface BranchReferralStateChangedListener {
        fun onStateChanged(changed: Boolean, error: BranchError?)
    }

    /**
     *
     * An Interface class that is implemented by all classes that make use of
     * [BranchLinkCreateListener], defining a single method that takes a URL
     * [String] format, and an error message of [BranchError] format that will be
     * returned on failure of the request response.
     *
     * @see String
     *
     * @see BranchError
     */
    interface BranchLinkCreateListener {
        fun onLinkCreate(url: String?, error: BranchError?)
    }

    /**
     *
     * An Interface class that is implemented by all classes that make use of
     * [BranchLinkShareListener], defining methods to listen for link sharing status.
     */
    interface BranchLinkShareListener {
        /**
         *
         *  Callback method to update when share link dialog is launched.
         */
        fun onShareLinkDialogLaunched()

        /**
         *
         *  Callback method to update when sharing dialog is dismissed.
         */
        fun onShareLinkDialogDismissed()

        /**
         *
         *  Callback method to update the sharing status. Called on sharing completed or on error.
         *
         * @param sharedLink    The link shared to the channel.
         * @param sharedChannel Channel selected for sharing.
         * @param error         A [BranchError] to update errors, if there is any.
         */
        fun onLinkShareResponse(sharedLink: String?, sharedChannel: String?, error: BranchError?)

        /**
         *
         * Called when user select a channel for sharing a deep link.
         * Branch will create a deep link for the selected channel and share with it after calling this
         * method. On sharing complete, status is updated by onLinkShareResponse() callback. Consider
         * having a sharing in progress UI if you wish to prevent user activity in the window between selecting a channel
         * and sharing complete.
         *
         * @param channelName Name of the selected application to share the link. An empty string is returned if unable to resolve selected client name.
         */
        fun onChannelSelected(channelName: String?)
    }

    /**
     *
     * An extended version of [BranchLinkShareListener] with callback that supports updating link data or properties after user select a channel to share
     * This will provide the extended callback [.onChannelSelected] only when sharing a link using Branch Universal Object.
     */
    interface ExtendedBranchLinkShareListener : BranchLinkShareListener {
        /**
         *
         *
         * Called when user select a channel for sharing a deep link.
         * This method allows modifying the link data and properties by providing the params  [BranchUniversalObject] and [LinkProperties]
         *
         *
         * @param channelName    The name of the channel user selected for sharing a link
         * @param buo            [BranchUniversalObject] BUO used for sharing link for updating any params
         * @param linkProperties [LinkProperties] associated with the sharing link for updating the properties
         * @return Return `true` to create link with any updates added to the data ([BranchUniversalObject]) or to the properties ([LinkProperties]).
         * Return `false` otherwise.
         */
        fun onChannelSelected(
            channelName: String?,
            buo: BranchUniversalObject?,
            linkProperties: LinkProperties?
        ): Boolean
    }

    /**
     *
     * An interface class for customizing sharing properties with selected channel.
     */
    interface IChannelProperties {
        /**
         * @param channel The name of the channel selected for sharing.
         * @return [String] with value for the message title for sharing the link with the selected channel
         */
        fun getSharingTitleForChannel(channel: String?): String?

        /**
         * @param channel The name of the channel selected for sharing.
         * @return [String] with value for the message body for sharing the link with the selected channel
         */
        fun getSharingMessageForChannel(channel: String?): String?
    }

    /**
     *
     * An Interface class that is implemented by all classes that make use of
     * [BranchListResponseListener], defining a single method that takes a list of
     * [JSONArray] format, and an error message of [BranchError] format that will be
     * returned on failure of the request response.
     *
     * @see JSONArray
     *
     * @see BranchError
     */
    interface BranchListResponseListener {
        fun onReceivingResponse(list: JSONArray?, error: BranchError?)
    }

    /**
     *
     *
     * Callback interface for listening logout status
     *
     */
    interface LogoutStatusListener {
        /**
         * Called on finishing the the logout process
         *
         * @param loggedOut A [Boolean] which is set to true if logout succeeded
         * @param error     An instance of [BranchError] to notify any error occurred during logout.
         * A null value is set if logout succeeded.
         */
        fun onLogoutFinished(loggedOut: Boolean, error: BranchError?)
    }

    /**
     *
     * enum containing the sort options for return of credit history.
     */
    enum class CreditHistoryOrder {
        kMostRecentFirst, kLeastRecentFirst
    }

    /**
     * Async Task to create  a short link for synchronous methods
     */
    private inner class GetShortLinkTask : AsyncTask<ServerRequest?, Void?, ServerResponse>() {
        override fun doInBackground(vararg serverRequests: ServerRequest?): ServerResponse {
            return branchRemoteInterface_.make_restful_post(
                serverRequests[0]!!.post,
                prefHelper!!.apiBaseUrl + Defines.RequestPath.GetURL.path,
                Defines.RequestPath.GetURL.path, prefHelper.branchKey
            )
        }
    }

    /**
     * Asynchronous task handling execution of server requests. Execute the network task on background
     * thread and request are  executed in sequential manner. Handles the request execution in
     * Synchronous-Asynchronous pattern. Should be invoked only form main thread and  the results are
     * published in the main thread.
     */
    private inner class BranchPostTask(var thisReq_: ServerRequest, val latch_: CountDownLatch?) :
        BranchAsyncTask<Void?, Void?, ServerResponse?>() {
        override fun onPreExecute() {
            super.onPreExecute()
            thisReq_.onPreExecute()
            thisReq_.doFinalUpdateOnMainThread()
        }

        override fun doInBackground(vararg voids: Void?): ServerResponse {
            // update queue wait time
            addExtraInstrumentationData(
                thisReq_.requestPath + "-" + Defines.Jsonkey.Queue_Wait_Time.key,
                thisReq_.queueWaitTime.toString()
            )
            thisReq_.doFinalUpdateOnBackgroundThread()
            if (isTrackingDisabled && !thisReq_.prepareExecuteWithoutTracking()) {
                return ServerResponse(
                    thisReq_.requestPath,
                    BranchError.ERR_BRANCH_TRACKING_DISABLED,
                    ""
                )
            }
            val branchKey = prefHelper!!.branchKey
            val result: ServerResponse
            result = if (thisReq_.isGetRequest) {
                branchRemoteInterface!!.make_restful_get(
                    thisReq_.requestUrl,
                    thisReq_.getParams,
                    thisReq_.requestPath,
                    branchKey
                )
            } else {
                branchRemoteInterface!!.make_restful_post(
                    thisReq_.getPostWithInstrumentationValues(
                        instrumentationExtraData_
                    ), thisReq_.requestUrl, thisReq_.requestPath, branchKey
                )
            }
            latch_?.countDown()
            return result
        }

        override fun onPostExecute(serverResponse: ServerResponse?) {
            super.onPostExecute(serverResponse)
            onPostExecuteInner(serverResponse)
        }

        fun onPostExecuteInner(serverResponse: ServerResponse?) {
            latch_?.countDown()
            if (serverResponse == null) {
                thisReq_.handleFailure(BranchError.ERR_BRANCH_INVALID_REQUEST, "Null response.")
                return
            }
            val status = serverResponse.statusCode
            if (status == 200) {
                onRequestSuccess(serverResponse)
            } else {
                onRequestFailed(serverResponse, status)
            }
            networkCount_ = 0

            // In rare cases where this method is called directly (eg. when network calls time out),
            // starting the next queue item can lead to stack over flow. Ensuring that this is
            // queued back to the main thread mitigates this.
            val handler = Handler(Looper.getMainLooper())
            handler.post { processNextQueueItem() }
        }

        private fun onRequestSuccess(serverResponse: ServerResponse) {
            // If the request succeeded
            val respJson = serverResponse.getObject()
            if (respJson == null) {
                thisReq_.handleFailure(500, "Null response json.")
            }
            if (thisReq_ is ServerRequestCreateUrl && respJson != null) {
                try {
                    // cache the link
                    val postBody = (thisReq_ as ServerRequestCreateUrl).linkPost
                    val url = respJson.getString("url")
                    linkCache_[postBody] = url
                } catch (ex: JSONException) {
                    ex.printStackTrace()
                }
            } else if (thisReq_ is ServerRequestLogout) {
                //On Logout clear the link cache and all pending requests
                linkCache_.clear()
                requestQueue_!!.clear()
            }
            if (thisReq_ is ServerRequestInitSession || thisReq_ is ServerRequestIdentifyUserRequest) {
                // If this request changes a session update the session-id to queued requests.
                var updateRequestsInQueue = false
                if (!isTrackingDisabled && respJson != null) {
                    // Update PII data only if tracking is disabled
                    try {
                        if (respJson.has(Defines.Jsonkey.SessionID.key)) {
                            prefHelper!!.sessionID =
                                respJson.getString(Defines.Jsonkey.SessionID.key)
                            updateRequestsInQueue = true
                        }
                        if (respJson.has(Defines.Jsonkey.RandomizedBundleToken.key)) {
                            val new_Randomized_Bundle_Token =
                                respJson.getString(Defines.Jsonkey.RandomizedBundleToken.key)
                            if (prefHelper!!.randomizedBundleToken != new_Randomized_Bundle_Token) {
                                //On setting a new Randomized Bundle Token clear the link cache
                                linkCache_.clear()
                                prefHelper.randomizedBundleToken = new_Randomized_Bundle_Token
                                updateRequestsInQueue = true
                            }
                        }
                        if (respJson.has(Defines.Jsonkey.RandomizedDeviceToken.key)) {
                            prefHelper!!.randomizedDeviceToken =
                                respJson.getString(Defines.Jsonkey.RandomizedDeviceToken.key)
                            updateRequestsInQueue = true
                        }
                        if (updateRequestsInQueue) {
                            updateAllRequestsInQueue()
                        }
                    } catch (ex: JSONException) {
                        ex.printStackTrace()
                    }
                }
                if (thisReq_ is ServerRequestInitSession) {
                    initState = SESSION_STATE.INITIALISED
                    if (!(thisReq_ as ServerRequestInitSession).handleBranchViewIfAvailable(
                            serverResponse
                        )
                    ) {
                        checkForAutoDeepLinkConfiguration()
                    }
                    // Count down the latch holding getLatestReferringParamsSync
                    if (getLatestReferringParamsLatch != null) {
                        getLatestReferringParamsLatch!!.countDown()
                    }
                    // Count down the latch holding getFirstReferringParamsSync
                    if (getFirstReferringParamsLatch != null) {
                        getFirstReferringParamsLatch!!.countDown()
                    }
                }
            }
            if (respJson != null) {
                thisReq_.onRequestSucceeded(serverResponse, branchReferral_)
                requestQueue_!!.remove(thisReq_)
            } else if (thisReq_.shouldRetryOnFail()) {
                // already called handleFailure above
                thisReq_.clearCallbacks()
            } else {
                requestQueue_!!.remove(thisReq_)
            }
        }

        fun onRequestFailed(serverResponse: ServerResponse, status: Int) {
            // If failed request is an initialisation request (but not in the intra-app linking scenario) then mark session as not initialised
            if (thisReq_ is ServerRequestInitSession && PrefHelper.NO_STRING_VALUE == prefHelper!!.sessionParams) {
                initState = SESSION_STATE.UNINITIALISED
            }

            // On a bad request or in case of a conflict notify with call back and remove the request.
            if ((status == 400 || status == 409) && thisReq_ is ServerRequestCreateUrl) {
                (thisReq_ as ServerRequestCreateUrl).handleDuplicateURLError()
            } else {
                //On Network error or Branch is down fail all the pending requests in the queue except
                //for request which need to be replayed on failure.
                networkCount_ = 0
                thisReq_.handleFailure(status, serverResponse.failReason)
            }
            val unretryableErrorCode =
                400 <= status && status <= 451 || status == BranchError.ERR_BRANCH_TRACKING_DISABLED
            // If it has an un-retryable error code, or it should not retry on fail, or the current retry count exceeds the max
            // remove it from the queue
            if (unretryableErrorCode || !thisReq_.shouldRetryOnFail() || thisReq_.currentRetryCount >= prefHelper!!.noConnectionRetryMax) {
                requestQueue_!!.remove(thisReq_)
            } else {
                // failure has already been handled
                // todo does it make sense to retry the request without a callback? (e.g. CPID, LATD)
                thisReq_.clearCallbacks()
            }
            thisReq_.currentRetryCount++
        }
    }

    fun checkForAutoDeepLinkConfiguration() {
        val latestParams = latestReferringParams
        var deepLinkActivity: String? = null
        try {
            //Check if the application is launched by clicking a Branch link.
            if (!latestParams!!.has(Defines.Jsonkey.Clicked_Branch_Link.key)
                || !latestParams.getBoolean(Defines.Jsonkey.Clicked_Branch_Link.key)
            ) {
                return
            }
            if (latestParams.length() > 0) {
                // Check if auto deep link is disabled.
                val appInfo = applicationContext!!.packageManager.getApplicationInfo(
                    applicationContext.packageName, PackageManager.GET_META_DATA
                )
                if (appInfo.metaData != null && appInfo.metaData.getBoolean(
                        AUTO_DEEP_LINK_DISABLE,
                        false
                    )
                ) {
                    return
                }
                val info = applicationContext.packageManager.getPackageInfo(
                    applicationContext.packageName,
                    PackageManager.GET_ACTIVITIES or PackageManager.GET_META_DATA
                )
                val activityInfos = info.activities
                var deepLinkActivityReqCode = DEF_AUTO_DEEP_LINK_REQ_CODE
                if (activityInfos != null) {
                    for (activityInfo in activityInfos) {
                        if (activityInfo?.metaData != null && (activityInfo.metaData.getString(
                                AUTO_DEEP_LINK_KEY
                            ) != null || activityInfo.metaData.getString(AUTO_DEEP_LINK_PATH) != null)
                        ) {
                            if (checkForAutoDeepLinkKeys(
                                    latestParams,
                                    activityInfo
                                ) || checkForAutoDeepLinkPath(latestParams, activityInfo)
                            ) {
                                deepLinkActivity = activityInfo.name
                                deepLinkActivityReqCode = activityInfo.metaData.getInt(
                                    AUTO_DEEP_LINK_REQ_CODE, DEF_AUTO_DEEP_LINK_REQ_CODE
                                )
                                break
                            }
                        }
                    }
                }
                if (deepLinkActivity != null && currentActivity != null) {
                    val currentActivity = currentActivity
                    val intent = Intent(currentActivity, Class.forName(deepLinkActivity))
                    intent.putExtra(Defines.IntentKeys.AutoDeepLinked.key, "true")

                    // Put the raw JSON params as extra in case need to get the deep link params as JSON String
                    intent.putExtra(Defines.Jsonkey.ReferringData.key, latestParams.toString())

                    // Add individual parameters in the data
                    val keys: Iterator<*> = latestParams.keys()
                    while (keys.hasNext()) {
                        val key = keys.next() as String
                        intent.putExtra(key, latestParams.getString(key))
                    }
                    currentActivity!!.startActivityForResult(intent, deepLinkActivityReqCode)
                } else {
                    // This case should not happen. Adding a safe handling for any corner case
                    PrefHelper.Debug("No activity reference to launch deep linked activity")
                }
            }
        } catch (e: PackageManager.NameNotFoundException) {
            PrefHelper.Debug("Warning: Please make sure Activity names set for auto deep link are correct!")
        } catch (e: ClassNotFoundException) {
            PrefHelper.Debug("Warning: Please make sure Activity names set for auto deep link are correct! Error while looking for activity $deepLinkActivity")
        } catch (ignore: Exception) {
            // Can get TransactionTooLarge Exception here if the Application info exceeds 1mb binder data limit. Usually results with manifest merge from SDKs
        }
    }

    private fun checkForAutoDeepLinkKeys(params: JSONObject?, activityInfo: ActivityInfo): Boolean {
        if (activityInfo.metaData.getString(AUTO_DEEP_LINK_KEY) != null) {
            val activityLinkKeys = activityInfo.metaData.getString(AUTO_DEEP_LINK_KEY)!!
                .split(",").toTypedArray()
            for (activityLinkKey in activityLinkKeys) {
                if (params!!.has(activityLinkKey)) {
                    return true
                }
            }
        }
        return false
    }

    private fun checkForAutoDeepLinkPath(params: JSONObject?, activityInfo: ActivityInfo): Boolean {
        var deepLinkPath: String? = null
        try {
            if (params!!.has(Defines.Jsonkey.AndroidDeepLinkPath.key)) {
                deepLinkPath = params.getString(Defines.Jsonkey.AndroidDeepLinkPath.key)
            } else if (params.has(Defines.Jsonkey.DeepLinkPath.key)) {
                deepLinkPath = params.getString(Defines.Jsonkey.DeepLinkPath.key)
            }
        } catch (ignored: JSONException) {
        }
        if (activityInfo.metaData.getString(AUTO_DEEP_LINK_PATH) != null && deepLinkPath != null) {
            val activityLinkPaths = activityInfo.metaData.getString(AUTO_DEEP_LINK_PATH)!!
                .split(",").toTypedArray()
            for (activityLinkPath in activityLinkPaths) {
                if (pathMatch(activityLinkPath.trim { it <= ' ' }, deepLinkPath)) {
                    return true
                }
            }
        }
        return false
    }

    private fun pathMatch(templatePath: String, path: String): Boolean {
        var matched = true
        val pathSegmentsTemplate =
            templatePath.split("\\?").toTypedArray()[0].split("/").toTypedArray()
        val pathSegmentsTarget = path.split("\\?").toTypedArray()[0].split("/").toTypedArray()
        if (pathSegmentsTemplate.size != pathSegmentsTarget.size) {
            return false
        }
        var i = 0
        while (i < pathSegmentsTemplate.size && i < pathSegmentsTarget.size) {
            val pathSegmentTemplate = pathSegmentsTemplate[i]
            val pathSegmentTarget = pathSegmentsTarget[i]
            if (pathSegmentTemplate != pathSegmentTarget && !pathSegmentTemplate.contains("*")) {
                matched = false
                break
            }
            i++
        }
        return matched
    }

    //------------------------ Content Indexing methods----------------------//
    fun registerView(
        branchUniversalObject: BranchUniversalObject?,
        callback: RegisterViewStatusListener?
    ) {
        if (applicationContext != null) {
            BranchEvent(BRANCH_STANDARD_EVENT.VIEW_ITEM)
                .addContentItems(branchUniversalObject)
                .logEvent(applicationContext)
        }
    }
    ///-------Instrumentation additional data---------------///
    /**
     * Update the extra instrumentation data provided to Branch
     *
     * @param instrumentationData A [HashMap] with key value pairs for instrumentation data.
     */
    fun addExtraInstrumentationData(instrumentationData: HashMap<String, String>?) {
        instrumentationExtraData_.putAll(instrumentationData!!)
    }

    /**
     * Update the extra instrumentation data provided to Branch
     *
     * @param key   A [String] Value for instrumentation data key
     * @param value A [String] Value for instrumentation data value
     */
    fun addExtraInstrumentationData(key: String, value: String) {
        instrumentationExtraData_[key] = value
    }

    //-------------------- Branch view handling--------------------//
    override fun onBranchViewVisible(action: String, branchViewID: String) {
        //No Implementation on purpose
    }

    override fun onBranchViewAccepted(action: String, branchViewID: String) {
        if (ServerRequestInitSession.isInitSessionAction(action)) {
            checkForAutoDeepLinkConfiguration()
        }
    }

    override fun onBranchViewCancelled(action: String, branchViewID: String) {
        if (ServerRequestInitSession.isInitSessionAction(action)) {
            checkForAutoDeepLinkConfiguration()
        }
    }

    override fun onBranchViewError(errorCode: Int, errorMsg: String, action: String) {
        if (ServerRequestInitSession.isInitSessionAction(action)) {
            checkForAutoDeepLinkConfiguration()
        }
    }

    /**
     * Interface for defining optional Branch view behaviour for Activities
     */
    interface IBranchViewControl {
        /**
         * Defines if an activity is interested to show Branch views or not.
         * By default activities are considered as Branch view enabled. In case of activities which are not interested to show a Branch view (Splash screen for example)
         * should implement this and return false. The pending Branch view will be shown with the very next Branch view enabled activity
         *
         * @return A [Boolean] whose value is true if the activity don't want to show any Branch view.
         */
        fun skipBranchViewsOnThisActivity(): Boolean
    }

    private fun extractSessionParamsForIDL(data: Uri?, activity: Activity?) {
        if (activity == null || activity.intent == null) return
        val intent = activity.intent
        try {
            if (data == null || isIntentParamsAlreadyConsumed(activity)) {
                // Considering the case of a deferred install. In this case the app behaves like a cold
                // start but still Branch can do probabilistic match. So skipping instant deep link feature
                // until first Branch open happens.
                if (prefHelper!!.installParams != PrefHelper.NO_STRING_VALUE) {
                    val nonLinkClickJson = JSONObject()
                    nonLinkClickJson.put(Defines.Jsonkey.IsFirstSession.key, false)
                    prefHelper.sessionParams = nonLinkClickJson.toString()
                    isInstantDeepLinkPossible = true
                }
            } else if (!TextUtils.isEmpty(intent.getStringExtra(Defines.IntentKeys.BranchData.key))) {
                // If not cold start, check the intent data to see if there are deep link params
                val rawBranchData = intent.getStringExtra(Defines.IntentKeys.BranchData.key)
                if (rawBranchData != null) {
                    // Make sure the data received is complete and in correct format
                    val branchDataJson = JSONObject(rawBranchData)
                    branchDataJson.put(Defines.Jsonkey.Clicked_Branch_Link.key, true)
                    prefHelper!!.sessionParams = branchDataJson.toString()
                    isInstantDeepLinkPossible = true
                }

                // Remove Branch data from the intent once used
                intent.removeExtra(Defines.IntentKeys.BranchData.key)
                activity.intent = intent
            } else if (data.isHierarchical && java.lang.Boolean.valueOf(
                    data.getQueryParameter(
                        Defines.Jsonkey.Instant.key
                    )
                )
            ) {
                // If instant key is true in query params, use them for instant deep linking
                val branchDataJson = JSONObject()
                for (key in data.queryParameterNames) {
                    branchDataJson.put(key, data.getQueryParameter(key))
                }
                branchDataJson.put(Defines.Jsonkey.Clicked_Branch_Link.key, true)
                prefHelper!!.sessionParams = branchDataJson.toString()
                isInstantDeepLinkPossible = true
            }
        } catch (ignored: JSONException) {
        }
    }

    private fun extractAppLink(data: Uri?, activity: Activity?) {
        if (data == null || activity == null) return
        val scheme = data.scheme
        val intent = activity.intent
        if (scheme != null && intent != null &&
            (scheme.equals("http", ignoreCase = true) || scheme.equals(
                "https",
                ignoreCase = true
            )) &&
            !TextUtils.isEmpty(data.host) &&
            !isIntentParamsAlreadyConsumed(activity)
        ) {
            val strippedUrl = UniversalResourceAnalyser.getInstance(applicationContext)
                .getStrippedURL(data.toString())
            if (data.toString().equals(strippedUrl, ignoreCase = true)) {
                // Send app links only if URL is not skipped.
                prefHelper!!.appLink = data.toString()
            }
            intent.putExtra(Defines.IntentKeys.BranchLinkUsed.key, true)
            activity.intent = intent
        }
    }

    private fun extractClickID(data: Uri?, activity: Activity?): Boolean {
        return try {
            if (data == null || !data.isHierarchical) return false
            val linkClickID =
                data.getQueryParameter(Defines.Jsonkey.LinkClickID.key) ?: return false
            prefHelper!!.linkClickIdentifier = linkClickID
            var paramString = "link_click_id=$linkClickID"
            val uriString = data.toString()
            paramString = if (paramString == data.query) {
                "\\?$paramString"
            } else if (uriString.length - paramString.length == uriString.indexOf(paramString)) {
                "&$paramString"
            } else {
                "$paramString&"
            }
            val uriWithoutClickID = Uri.parse(uriString.replaceFirst(paramString.toRegex(), ""))
            activity!!.intent.data = uriWithoutClickID
            activity.intent.putExtra(Defines.IntentKeys.BranchLinkUsed.key, true)
            true
        } catch (ignore: Exception) {
            false
        }
    }

    private fun extractBranchLinkFromIntentExtra(activity: Activity?): Boolean {
        //Check for any push identifier in case app is launched by a push notification
        try {
            if (activity != null && activity.intent != null && activity.intent.extras != null) {
                if (!isIntentParamsAlreadyConsumed(activity)) {
                    val `object` = activity.intent.extras!![Defines.IntentKeys.BranchURI.key]
                    var branchLink: String? = null
                    if (`object` is String) {
                        branchLink = `object`
                    } else if (`object` is Uri) {
                        branchLink = `object`.toString()
                    }
                    if (!TextUtils.isEmpty(branchLink)) {
                        prefHelper!!.pushIdentifier = branchLink
                        val thisIntent = activity.intent
                        thisIntent.putExtra(Defines.IntentKeys.BranchLinkUsed.key, true)
                        activity.intent = thisIntent
                        return true
                    }
                }
            }
        } catch (ignore: Exception) {
        }
        return false
    }

    private fun extractExternalUriAndIntentExtras(data: Uri?, activity: Activity?) {
        try {
            if (!isIntentParamsAlreadyConsumed(activity)) {
                val strippedUrl = UniversalResourceAnalyser.getInstance(applicationContext)
                    .getStrippedURL(data.toString())
                prefHelper!!.externalIntentUri = strippedUrl
                if (strippedUrl == data.toString()) {
                    val bundle = activity!!.intent.extras
                    val extraKeys = bundle!!.keySet()
                    if (extraKeys.isEmpty()) return
                    val extrasJson = JSONObject()
                    for (key in EXTERNAL_INTENT_EXTRA_KEY_WHITE_LIST) {
                        if (extraKeys.contains(key)) {
                            extrasJson.put(key, bundle[key])
                        }
                    }
                    if (extrasJson.length() > 0) {
                        prefHelper.externalIntentExtra = extrasJson.toString()
                    }
                }
            }
        } catch (ignore: Exception) {
        }
    }

    val currentActivity: Activity?
        get() = if (currentActivityReference_ == null) null else currentActivityReference_!!.get()

    class InitSessionBuilder(activity: Activity?) {
        private var callback: BranchReferralInitListener? = null
        private var isAutoInitialization = false
        private var delay = 0
        private var uri: Uri? = null
        private var ignoreIntent: Boolean? = null
        private var isReInitializing = false

        init {
            val branch = instance
            if (activity != null && (branch!!.currentActivity == null ||
                        branch.currentActivity!!.localClassName != activity.localClassName)
            ) {
                // currentActivityReference_ is set in onActivityCreated (before initSession), which should happen if
                // users follow Android guidelines and call super.onStart as the first thing in Activity.onStart,
                // however, if they don't, we try to set currentActivityReference_ here too.
                branch.currentActivityReference_ = WeakReference(activity)
            }
        }

        /**
         * Helps differentiating between sdk session auto-initialization and client driven session
         * initialization. For internal SDK use only.
         */
        fun isAutoInitialization(isAuto: Boolean): InitSessionBuilder {
            isAutoInitialization = isAuto
            return this
        }

        /**
         *
         *  Add callback to Branch initialization to retrieve referring params attached to the
         * Branch link via the dashboard. User eventually decides how to use the referring params but
         * they are primarily meant to be used for navigating to specific content within the app.
         * Use only one withCallback() method.
         *
         * @param callback     A [BranchUniversalReferralInitListener] instance that will be called
         * following successful (or unsuccessful) initialisation of the session
         * with the Branch API.
         */
        fun withCallback(callback: BranchUniversalReferralInitListener?): InitSessionBuilder {
            this.callback = BranchUniversalReferralInitWrapper(callback)
            return this
        }

        /**
         *
         *  Delay session initialization by certain time (used when other async or otherwise time
         * consuming ops need to be completed prior to session initialization).
         *
         * @param delayMillis  An [Integer] indicating the length of the delay in milliseconds.
         */
        fun withDelay(delayMillis: Int): InitSessionBuilder {
            delay = delayMillis
            return this
        }

        /**
         *
         *  Add callback to Branch initialization to retrieve referring params attached to the
         * Branch link via the dashboard. User eventually decides how to use the referring params but
         * they are primarily meant to be used for navigating to specific content within the app.
         * Use only one withCallback() method.
         *
         * @param callback     A [BranchReferralInitListener] instance that will be called
         * following successful (or unsuccessful) initialisation of the session
         * with the Branch API.
         */
        fun withCallback(callback: BranchReferralInitListener?): InitSessionBuilder {
            this.callback = callback
            return this
        }

        /**
         *
         *  Specify a [Uri] variable containing the details of the source link that led to
         * this initialisation action.
         *
         * @param uri A [Uri] variable from the intent.
         */
        fun withData(uri: Uri?): InitSessionBuilder {
            this.uri = uri
            return this
        }

        /**
         *
         *  Use this method cautiously, it is meant to enable the ability to start a session before
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
         * in certain cases. See also Branch.bypassWaitingForIntent(boolean).
         *
         * @param ignore       a [Boolean] indicating if SDK should wait for onResume to retrieve
         * the most up recent intent data before firing the session initialization request.
         */
        fun ignoreIntent(ignore: Boolean): InitSessionBuilder {
            ignoreIntent = ignore
            return this
        }

        /**
         *
         * Initialises a session with the Branch API, registers the passed in Activity, callback
         * and configuration variables, then initializes session.
         */
        fun init() {
            PrefHelper.Debug("Beginning session initialization")
            PrefHelper.Debug("Session uri is $uri")
            if (deferInitForPluginRuntime) {
                PrefHelper.Debug("Session init is deferred until signaled by plugin.")
                cacheSessionBuilder(this)
                return
            }
            val branch = instance
            if (branch == null) {
                PrefHelper.LogAlways(
                    "Branch is not setup properly, make sure to call getAutoInstance" +
                            " in your application class or declare BranchApp in your manifest."
                )
                return
            }
            if (ignoreIntent != null) {
                bypassWaitingForIntent(ignoreIntent!!)
            }
            val activity = branch.currentActivity
            val intent = activity?.intent
            if (activity != null && intent != null && ActivityCompat.getReferrer(activity) != null) {
                PrefHelper.getInstance(activity).initialReferrer =
                    ActivityCompat.getReferrer(activity).toString()
            }
            if (uri != null) {
                branch.readAndStripParam(uri, activity)
            } else if (isReInitializing && branch.isRestartSessionRequested(intent)) {
                branch.readAndStripParam(intent?.data, activity)
            } else if (isReInitializing) {
                // User called reInit but isRestartSessionRequested = false, meaning the new intent was
                // not initiated by Branch and should not be considered a "new session", return early
                if (callback != null) {
                    callback!!.onInitFinished(
                        null,
                        BranchError("", BranchError.ERR_IMPROPER_REINITIALIZATION)
                    )
                }
                return
            }

            // readAndStripParams (above) may set isInstantDeepLinkPossible to true
            if (branch.isInstantDeepLinkPossible) {
                // reset state
                branch.isInstantDeepLinkPossible = false
                // invoke callback returning LatestReferringParams, which were parsed out inside readAndStripParam
                // from either intent extra "branch_data", or as parameters attached to the referring app link
                if (callback != null) callback!!.onInitFinished(branch.latestReferringParams, null)
                // mark this session as IDL session
                branch.addExtraInstrumentationData(
                    Defines.Jsonkey.InstantDeepLinkSession.key,
                    "true"
                )
                // potentially routes the user to the Activity configured to consume this particular link
                branch.checkForAutoDeepLinkConfiguration()
                // we already invoked the callback for let's set it to null, we will still make the
                // init session request but for analytics purposes only
                callback = null
            }
            if (delay > 0) {
                expectDelayedSessionInitialization(true)
            }
            val initRequest = branch.getInstallOrOpenRequest(callback, isAutoInitialization)
            branch.initializeSession(initRequest, delay)
        }

        private fun cacheSessionBuilder(initSessionBuilder: InitSessionBuilder) {
            instance!!.deferredSessionBuilder = this
            PrefHelper.Debug(
                """
    Session initialization deferred until plugin invokes notifyNativeToInit()
    Caching Session Builder ${instance!!.deferredSessionBuilder}
    uri: ${instance!!.deferredSessionBuilder!!.uri}
    callback: ${instance!!.deferredSessionBuilder!!.callback}
    isReInitializing: ${instance!!.deferredSessionBuilder!!.isReInitializing}
    delay: ${instance!!.deferredSessionBuilder!!.delay}
    isAutoInitialization: ${instance!!.deferredSessionBuilder!!.isAutoInitialization}
    ignoreIntent: ${instance!!.deferredSessionBuilder!!.ignoreIntent}
    """.trimIndent()
            )
        }

        /**
         *
         *  Re-Initialize a session. Call from Activity.onNewIntent().
         * This solves a very specific use case, whereas the app is already in the foreground and a new
         * intent with a Uri is delivered to the foregrounded activity.
         *
         * Note that the Uri can also be stored as an extra in the field under the key `IntentKeys.BranchURI.getKey()` (i.e. "branch").
         *
         * Note also, that the since the method is expected to be called from Activity.onNewIntent(),
         * the implementation assumes the intent will be non-null and will contain a Branch link in
         * either the URI or in the the extra.
         *
         */
        fun reInit() {
            isReInitializing = true
            init()
        }
    }

    val isIDLSession: Boolean
        get() = java.lang.Boolean.parseBoolean(instrumentationExtraData_[Defines.Jsonkey.InstantDeepLinkSession.key])

    fun logEventWithPurchase(context: Context, purchase: Purchase) {
        BillingGooglePlay.getInstance().startBillingClient { succeeded: Boolean ->
            if (succeeded) {
                BillingGooglePlay.getInstance().logEventWithPurchase(context, purchase)
            } else {
                PrefHelper.LogException(
                    "Cannot log IAP event. Billing client setup failed",
                    Exception("Billing Client Setup Failed")
                )
            }
            null
        }
    }

    companion object {
        private val BRANCH_LIBRARY_VERSION = "io.branch.sdk.android:library:" + sdkVersionNumber
        private val GOOGLE_VERSION_TAG = "!SDK-VERSION-STRING!" + ":" + BRANCH_LIBRARY_VERSION

        /**
         * Hard-coded [String] that denotes a [BranchLinkData.tags]; applies to links that
         * are shared with others directly as a user action, via social media for instance.
         */
        const val FEATURE_TAG_SHARE = "share"

        /**
         * Hard-coded [String] that denotes a 'referral' tag; applies to links that are associated
         * with a referral program, incentivized or not.
         */
        const val FEATURE_TAG_REFERRAL = "referral"

        /**
         * The redirect URL provided when the link is handled by a desktop client.
         */
        const val REDIRECT_DESKTOP_URL = "\$desktop_url"

        /**
         * The redirect URL provided when the link is handled by an Android device.
         */
        const val REDIRECT_ANDROID_URL = "\$android_url"

        /**
         * The redirect URL provided when the link is handled by an iOS device.
         */
        const val REDIRECT_IOS_URL = "\$ios_url"

        /**
         * The redirect URL provided when the link is handled by a large form-factor iOS device such as
         * an iPad.
         */
        const val REDIRECT_IPAD_URL = "\$ipad_url"

        /**
         * The redirect URL provided when the link is handled by an Amazon Fire device.
         */
        const val REDIRECT_FIRE_URL = "\$fire_url"

        /**
         * Open Graph: The title of your object as it should appear within the graph, e.g., "The Rock".
         *
         * @see [Open Graph - Basic Metadata](http://ogp.me/.metadata)
         */
        const val OG_TITLE = "\$og_title"

        /**
         * The description of the object to appear in social media feeds that use
         * Facebook's Open Graph specification.
         *
         * @see [Open Graph - Basic Metadata](http://ogp.me/.metadata)
         */
        const val OG_DESC = "\$og_description"

        /**
         * An image URL which should represent your object to appear in social media feeds that use
         * Facebook's Open Graph specification.
         *
         * @see [Open Graph - Basic Metadata](http://ogp.me/.metadata)
         */
        const val OG_IMAGE_URL = "\$og_image_url"

        /**
         * A URL to a video file that complements this object.
         *
         * @see [Open Graph - Basic Metadata](http://ogp.me/.metadata)
         */
        const val OG_VIDEO = "\$og_video"

        /**
         * The canonical URL of your object that will be used as its permanent ID in the graph.
         *
         * @see [Open Graph - Basic Metadata](http://ogp.me/.metadata)
         */
        const val OG_URL = "\$og_url"

        /**
         * Unique identifier for the app in use.
         */
        const val OG_APP_ID = "\$og_app_id"

        /**
         * [String] value denoting the deep link path to override Branch's default one. By
         * default, Branch will use yourapp://open?link_click_id=12345. If you specify this key/value,
         * Branch will use yourapp://'$deeplink_path'?link_click_id=12345
         */
        const val DEEPLINK_PATH = "\$deeplink_path"

        /**
         * [String] value indicating whether the link should always initiate a deep link action.
         * By default, unless overridden on the dashboard, Branch will only open the app if they are
         * 100% sure the app is installed. This setting will cause the link to always open the app.
         * Possible values are "true" or "false"
         */
        const val ALWAYS_DEEPLINK = "\$always_deeplink"

        /**
         * An [Integer] value indicating the link type. In this case, the link can be used an
         * unlimited number of times.
         */
        const val LINK_TYPE_UNLIMITED_USE = 0

        /**
         * An [Integer] value indicating the link type. In this case, the link can be used only
         * once. After initial use, subsequent attempts will not validate.
         */
        const val LINK_TYPE_ONE_TIME_USE = 1

        /**
         * If true, instantiate a new webview instance ui thread to retrieve user agent string
         */
        var userAgentSync = false

        /**
         * Package private user agent string cached to save on repeated queries
         */
        @JvmField
        var _userAgentString = ""

        /**
         * Returns true if reading device id is disabled
         *
         * @return [Boolean] with value true to disable reading Andoid ID
         */
        @JvmStatic
        var isDeviceIDFetchDisabled = false
            private set
        var bypassWaitingForIntent_ = false
        private var bypassCurrentActivityIntentState_ = false
        @JvmField
        var disableAutoSessionInitialization = false
        var checkInstallReferrer_ = true
        private var playStoreReferrerWaitTime: Long = 1500
        const val NO_PLAY_STORE_REFERRER_WAIT: Long = 0
        @JvmStatic
        var isReferringLinkAttributionForPreinstalledAppsEnabled = false

        /**
         *
         * A [Branch] object that is instantiated on init and holds the singleton instance of
         * the class during application runtime.
         */
        private var branchReferral_: Branch? = null

        /* Set to true when {@link Activity} life cycle callbacks are registered. */
        private var isActivityLifeCycleCallbackRegistered_ = false

        /* */
        var deferInitForPluginRuntime = false

        /* Key for Auto Deep link param. The activities which need to automatically deep linked should define in this in the activity metadata. */
        private const val AUTO_DEEP_LINK_KEY = "io.branch.sdk.auto_link_keys"

        /* Path for $deeplink_path or $android_deeplink_path to auto deep link. The activities which need to automatically deep linked should define in this in the activity metadata. */
        private const val AUTO_DEEP_LINK_PATH = "io.branch.sdk.auto_link_path"

        /* Key for disabling auto deep link feature. Setting this to true in manifest will disable auto deep linking feature. */
        private const val AUTO_DEEP_LINK_DISABLE = "io.branch.sdk.auto_link_disable"

        /*Key for defining a request code for an activity. should be added as a metadata for an activity. This is used as a request code for launching a an activity on auto deep link. */
        private const val AUTO_DEEP_LINK_REQ_CODE = "io.branch.sdk.auto_link_request_code"

        /* Request code  used to launch and activity on auto deep linking unless DEF_AUTO_DEEP_LINK_REQ_CODE is not specified for teh activity in manifest.*/
        private const val DEF_AUTO_DEEP_LINK_REQ_CODE = 1501

        private const val LATCH_WAIT_UNTIL = 2500 //used for getLatestReferringParamsSync and getFirstReferringParamsSync, fail after this many milliseconds

        /* List of keys whose values are collected from the Intent Extra.*/
        private val EXTERNAL_INTENT_EXTRA_KEY_WHITE_LIST = arrayOf(
            "extra_launch_uri",  // Key for embedded uri in FB ads triggered intents
            "branch_intent" // A boolean that specifies if this intent is originated by Branch
        )
        @JvmField
        var installDeveloperId: String? = null

        /* Flag to turn on or off instant deeplinking feature. IDL is disabled by default */
        private var enableInstantDeepLinking = false

        /** Variables for reporting plugin type and version (some TUNE customers do that), plus helps
         * us make data driven decisions.  */
        @JvmStatic
        var pluginVersion: String? = null
            private set
        @JvmStatic
        var pluginName: String? = null
            private set

        /**
         *
         * Singleton method to return the pre-initialised object of the type [Branch].
         * Make sure your app is instantiating [BranchApp] before calling this method
         * or you have created an instance of Branch already by calling getInstance(Context ctx).
         *
         * @return An initialised singleton [Branch] object
         */
        @JvmStatic
        @get:Synchronized
        val instance: Branch?
            get() {
                if (branchReferral_ == null) {
                    PrefHelper.Debug("Branch instance is not created yet. Make sure you call getAutoInstance(Context).")
                }
                return branchReferral_
            }

        @Synchronized
        private fun initBranchSDK(context: Context, branchKey: String): Branch? {
            if (branchReferral_ != null) {
                PrefHelper.Debug("Warning, attempted to reinitialize Branch SDK singleton!")
                return branchReferral_
            }
            branchReferral_ = Branch(context.applicationContext)
            if (TextUtils.isEmpty(branchKey)) {
                PrefHelper.Debug("Warning: Please enter your branch_key in your project's Manifest file!")
                branchReferral_!!.prefHelper!!.branchKey =
                    PrefHelper.NO_STRING_VALUE
            } else {
                branchReferral_!!.prefHelper!!.branchKey = branchKey
            }

            /* If {@link Application} is instantiated register for activity life cycle events. */if (context is Application) {
                branchReferral_!!.setActivityLifeCycleObserver(
                    context
                )
            }

            // Cache the user agent from a webview instance if needed
            if (userAgentSync && DeviceInfo.getInstance() != null) {
                DeviceInfo.getInstance().getUserAgentStringSync(context)
            }
            return branchReferral_
        }

        /**
         *
         * Singleton method to return the pre-initialised, or newly initialise and return, a singleton
         * object of the type [Branch].
         *
         * Use this whenever you need to call a method directly on the [Branch] object.
         *
         * @param context A [Context] from which this call was made.
         * @return An initialised [Branch] object, either fetched from a pre-initialised
         * instance within the singleton class, or a newly instantiated object where
         * one was not already requested during the current app lifecycle.
         */
        @Synchronized
        @JvmStatic
        fun getAutoInstance(context: Context): Branch? {
            if (branchReferral_ == null) {
                if (BranchUtil.getEnableLoggingConfig(context)) {
                    enableLogging()
                }

                // Should only be set in json config
                deferInitForPluginRuntime(BranchUtil.getDeferInitForPluginRuntimeConfig(context))
                BranchUtil.setTestMode(BranchUtil.checkTestMode(context))
                branchReferral_ = initBranchSDK(context, BranchUtil.readBranchKey(context))
                BranchPreinstall.getPreinstallSystemData(branchReferral_, context)
            }
            return branchReferral_
        }

        /**
         *
         * Singleton method to return the pre-initialised, or newly initialise and return, a singleton
         * object of the type [Branch].
         *
         * Use this whenever you need to call a method directly on the [Branch] object.
         *
         * @param context   A [Context] from which this call was made.
         * @param branchKey A [String] value used to initialize Branch.
         * @return An initialised [Branch] object, either fetched from a pre-initialised
         * instance within the singleton class, or a newly instantiated object where
         * one was not already requested during the current app lifecycle.
         */
        @JvmStatic
        fun getAutoInstance(context: Context, branchKey: String): Branch? {
            var branchKey = branchKey
            if (branchReferral_ == null) {
                if (BranchUtil.getEnableLoggingConfig(context)) {
                    enableLogging()
                }

                // Should only be set in json config
                deferInitForPluginRuntime(BranchUtil.getDeferInitForPluginRuntimeConfig(context))
                BranchUtil.setTestMode(BranchUtil.checkTestMode(context))
                // If a Branch key is passed already use it. Else read the key
                if (!PrefHelper.isValidBranchKey(branchKey)) {
                    PrefHelper.Debug("Warning, Invalid branch key passed! Branch key will be read from manifest instead!")
                    branchKey = BranchUtil.readBranchKey(context)
                }
                branchReferral_ = initBranchSDK(context, branchKey)
                BranchPreinstall.getPreinstallSystemData(branchReferral_, context)
            }
            return branchReferral_
        }

        /**
         *
         *
         * Enables the test mode for the SDK. This will use the Branch Test Keys. This is same as setting
         * "io.branch.sdk.TestMode" to "True" in Manifest file.
         *
         * Note: As of v5.0.1, enableTestMode has been changed. It now uses the test key but will not log or randomize
         * the device IDs. If you wish to enable logging, please invoke enableLogging. If you wish to simulate
         * installs, please see add a Test Device (https://help.branch.io/using-branch/docs/adding-test-devices)
         * then reset your test device's data (https://help.branch.io/using-branch/docs/adding-test-devices#section-resetting-your-test-device-data).
         *
         */
        @JvmStatic
        fun enableTestMode() {
            BranchUtil.setTestMode(true)
            PrefHelper.LogAlways(
                "enableTestMode has been changed. It now uses the test key but will not" +
                        " log or randomize the device IDs. If you wish to enable logging, please invoke enableLogging." +
                        " If you wish to simulate installs, please see add a Test Device (https://help.branch.io/using-branch/docs/adding-test-devices)" +
                        " then reset your test device's data (https://help.branch.io/using-branch/docs/adding-test-devices#section-resetting-your-test-device-data)."
            )
        }

        /**
         *
         *
         * Disables the test mode for the SDK.
         *
         */
        @JvmStatic
        fun disableTestMode() {
            BranchUtil.setTestMode(false)
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
         * @param expectDelayedInit A [Boolean] to set the expectation flag.
         */
        @JvmStatic
        fun expectDelayedSessionInitialization(expectDelayedInit: Boolean) {
            disableAutoSessionInitialization = expectDelayedInit
        }

        /**
         *
         * Sets a custom base URL for all calls to the Branch API.  Requires https.
         * @param url The [String] URL base URL that the Branch API uses.
         */
        fun setAPIUrl(url: String?) {
            PrefHelper.setAPIUrl(url)
        }

        /**
         *
         * Sets a custom CDN base URL.
         * @param url The [String] base URL for CDN endpoints.
         */
        fun setCDNBaseUrl(url: String?) {
            PrefHelper.setCDNBaseUrl(url)
        }

        /**
         * Set timeout for Play Store Referrer library. Play Store Referrer library allows Branch to provide
         * more accurate tracking and attribution. This delays Branch initialization only the first time user opens the app.
         * This method allows to override the maximum wait time for play store referrer to arrive.
         *
         *
         *
         * @param delay [Long] Maximum wait time for install referrer broadcast in milli seconds. Set to [Branch.NO_PLAY_STORE_REFERRER_WAIT] if you don't want to wait for play store referrer
         */
        @JvmStatic
        fun setPlayStoreReferrerCheckTimeout(delay: Long) {
            checkInstallReferrer_ = delay > 0
            playStoreReferrerWaitTime = delay
        }

        /**
         *
         *
         * Disables or enables the instant deep link functionality.
         *
         *
         * @param disableIDL Value `true` disables the  instant deep linking. Value `false` enables the  instant deep linking.
         */
        fun disableInstantDeepLinking(disableIDL: Boolean) {
            enableInstantDeepLinking = !disableIDL
        }

        // Package Private
        // For Unit Testing, we need to reset the Branch state
        @JvmStatic
        fun shutDown() {
            ServerRequestQueue.shutDown()
            PrefHelper.shutDown()
            BranchUtil.shutDown()

            // BranchViewHandler.shutDown();
            // DeepLinkRoutingValidator.shutDown();
            // GooglePlayStoreAttribution.shutDown();
            // InstantAppUtil.shutDown();
            // IntegrationValidator.shutDown();
            // ShareLinkManager.shutDown();
            // UniversalResourceAnalyser.shutDown();

            // Release these contexts immediately.

            // Reset all of the statics.
            branchReferral_ = null
            bypassCurrentActivityIntentState_ = false
            enableInstantDeepLinking = false
            isActivityLifeCycleCallbackRegistered_ = false
            bypassWaitingForIntent_ = false
            checkInstallReferrer_ = true
        }

        /**
         * Method to control reading Android ID from device. Set this to true to disable reading the device id.
         * This method should be called from your [Application.onCreate] method before creating Branch auto instance by calling [Branch.getAutoInstance]
         *
         * @param deviceIdFetch [with value true to disable reading the Android id from device][Boolean]
         */
        @JvmStatic
        fun disableDeviceIDFetch(deviceIdFetch: Boolean) {
            isDeviceIDFetchDisabled = deviceIdFetch
        }

        /**
         * Enables referring url attribution for preinstalled apps.
         *
         * By default, Branch prioritizes preinstall attribution on preinstalled apps.
         * Some clients prefer the referring link, when present, to be prioritized over preinstall attribution.
         */
        fun setReferringLinkAttributionForPreinstalledAppsEnabled() {
            isReferringLinkAttributionForPreinstalledAppsEnabled = true
        }

        fun setIsUserAgentSync(sync: Boolean) {
            userAgentSync = sync
        }

        fun registerPlugin(name: String?, version: String?) {
            pluginName = name
            pluginVersion = version
        }
        //-------------------Auto deep link feature-------------------------------------------//
        /**
         *
         * Checks if an activity is launched by Branch auto deep link feature. Branch launches activity configured for auto deep link on seeing matching keys.
         * Keys for auto deep linking should be specified to each activity as a meta data in manifest.
         * Configure your activity in your manifest to enable auto deep linking as follows
         *
         *
         * @param activity Instance of activity to check if launched on auto deep link.
         * @return A {Boolean} value whose value is true if this activity is launched by Branch auto deeplink feature.
         */
        @JvmStatic
        fun isAutoDeepLinkLaunch(activity: Activity): Boolean {
            return activity.intent.getStringExtra(Defines.IntentKeys.AutoDeepLinked.key) != null
        }

        /**
         * Enable Logging, independent of Debug Mode.
         */
        @JvmStatic
        fun enableLogging() {
            PrefHelper.LogAlways(GOOGLE_VERSION_TAG)
            PrefHelper.enableLogging(true)
        }

        /**
         * Disable Logging, independent of Debug Mode.
         */
        fun disableLogging() {
            PrefHelper.enableLogging(false)
        }

        /**
         *
         *  Use this method cautiously, it is meant to enable the ability to start a session before
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
         * in certain cases.
         *
         * @param bypassIntent a [Boolean] indicating if SDK should wait for onResume in order to fire the
         * session initialization request.
         */
        fun bypassWaitingForIntent(bypassIntent: Boolean) {
            bypassWaitingForIntent_ = bypassIntent
        }

        /**
         * Returns true if session initialization should bypass waiting for intent (retrieved after onResume).
         *
         * @return [Boolean] with value true to enable forced session
         */
        val isWaitingForIntent: Boolean
            get() = !bypassWaitingForIntent_

        fun enableBypassCurrentActivityIntentState() {
            bypassCurrentActivityIntentState_ = true
        }

        @JvmStatic
        fun bypassCurrentActivityIntentState(): Boolean {
            return bypassCurrentActivityIntentState_
        }
        ///----------------- Instant App  support--------------------------//
        /**
         * Checks if this is an Instant app instance
         *
         * @param context Current [Context]
         * @return `true`  if current application is an instance of instant app
         */
        fun isInstantApp(context: Context): Boolean {
            return InstantAppUtil.isInstantApp(context)
        }

        /**
         * Method shows play store install prompt for the full app. Thi passes the referrer to the installed application. The same deep link params as the instant app are provided to the
         * full app up on Branch#initSession()
         *
         * @param activity    Current activity
         * @param requestCode Request code for the activity to receive the result
         * @return `true` if install prompt is shown to user
         */
        fun showInstallPrompt(activity: Activity, requestCode: Int): Boolean {
            var installReferrerString = ""
            if (instance != null) {
                val latestReferringParams = instance!!.latestReferringParams
                val referringLinkKey = "~" + Defines.Jsonkey.ReferringLink.key
                if (latestReferringParams != null && latestReferringParams.has(referringLinkKey)) {
                    var referringLink = ""
                    try {
                        referringLink = latestReferringParams.getString(referringLinkKey)
                        // Considering the case that url may contain query params with `=` and `&` with it and may cause issue when parsing play store referrer
                        referringLink = URLEncoder.encode(referringLink, "UTF-8")
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    } catch (e: UnsupportedEncodingException) {
                        e.printStackTrace()
                    }
                    if (!TextUtils.isEmpty(referringLink)) {
                        installReferrerString =
                            Defines.Jsonkey.IsFullAppConv.key + "=true&" + Defines.Jsonkey.ReferringLink.key + "=" + referringLink
                    }
                }
            }
            return InstantAppUtil.doShowInstallPrompt(activity, requestCode, installReferrerString)
        }

        /**
         * Method shows play store install prompt for the full app. Use this method only if you have custom parameters to pass to the full app using referrer else use
         * [.showInstallPrompt]
         *
         * @param activity    Current activity
         * @param requestCode Request code for the activity to receive the result
         * @param referrer    Any custom referrer string to pass to full app (must be of format "referrer_key1=referrer_value1%26referrer_key2=referrer_value2")
         * @return `true` if install prompt is shown to user
         */
        fun showInstallPrompt(activity: Activity, requestCode: Int, referrer: String?): Boolean {
            val installReferrerString = Defines.Jsonkey.IsFullAppConv.key + "=true&" + referrer
            return InstantAppUtil.doShowInstallPrompt(activity, requestCode, installReferrerString)
        }

        /**
         * Method shows play store install prompt for the full app. Use this method only if you want the full app to receive a custom [BranchUniversalObject] to do deferred deep link.
         * Please see [.showInstallPrompt]
         * NOTE :
         * This method will do a synchronous generation of Branch short link for the BUO. So please consider calling this method on non UI thread
         * Please make sure your instant app and full ap are using same Branch key in order for the deferred deep link working
         *
         * @param activity    Current activity
         * @param requestCode Request code for the activity to receive the result
         * @param buo         [BranchUniversalObject] to pass to the full app up on install
         * @return `true` if install prompt is shown to user
         */
        fun showInstallPrompt(
            activity: Activity,
            requestCode: Int,
            buo: BranchUniversalObject
        ): Boolean {
            val shortUrl = buo.getShortUrl(activity, LinkProperties())
            val installReferrerString = Defines.Jsonkey.ReferringLink.key + "=" + shortUrl
            return if (!TextUtils.isEmpty(installReferrerString)) {
                showInstallPrompt(
                    activity,
                    requestCode,
                    installReferrerString
                )
            } else {
                showInstallPrompt(activity, requestCode, "")
            }
        }

        /**
         *
         *  Create Branch session builder. Add configuration variables with the available methods
         * in the returned [InitSessionBuilder] class. Must be finished with init() or reInit(),
         * otherwise takes no effect.
         *
         * @param activity     The calling [Activity] for context.
         */
        @JvmStatic
        fun sessionBuilder(activity: Activity?): InitSessionBuilder {
            return InitSessionBuilder(activity)
        }

        /**
         * Method will return the current Branch SDK version number
         * @return String value representing the current SDK version number (e.g. 4.3.2)
         */
        @JvmStatic
        val sdkVersionNumber: String
            get() = BuildConfig.VERSION_NAME

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
        fun deferInitForPluginRuntime(isDeferred: Boolean) {
            PrefHelper.Debug("deferInitForPluginRuntime $isDeferred")
            deferInitForPluginRuntime = isDeferred
            if (isDeferred) {
                expectDelayedSessionInitialization(isDeferred)
            }
        }

        /**
         * Method to be invoked from plugin to initialize the session originally built by the user
         * Only invokes the last session built
         */
        @JvmStatic
        fun notifyNativeToInit() {
            PrefHelper.Debug("notifyNativeToInit deferredSessionBuilder " + instance!!.deferredSessionBuilder)
            val sessionState = instance!!.initState
            if (sessionState == SESSION_STATE.UNINITIALISED) {
                deferInitForPluginRuntime = false
                if (instance!!.deferredSessionBuilder != null) {
                    instance!!.deferredSessionBuilder!!.init()
                }
            } else {
                PrefHelper.Debug("notifyNativeToInit session is not uninitialized. Session state is $sessionState")
            }
        }
    }
}