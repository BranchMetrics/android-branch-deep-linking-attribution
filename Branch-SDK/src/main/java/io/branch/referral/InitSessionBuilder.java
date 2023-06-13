package io.branch.referral;

import static io.branch.referral.BranchError.ERR_IMPROPER_REINITIALIZATION;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import androidx.core.app.ActivityCompat;

import java.lang.ref.WeakReference;

public class InitSessionBuilder {
    private Branch.BranchReferralInitListener callback;
    private boolean isAutoInitialization;
    private int delay;
    private Uri uri;
    private Boolean ignoreIntent;
    private boolean isReInitializing;

    InitSessionBuilder(Activity activity) {
        Branch branch = Branch.getInstance();
        if (activity != null && (branch.getCurrentActivity() == null || !branch.getCurrentActivity().getLocalClassName().equals(activity.getLocalClassName()))) {
            // currentActivityReference_ is set in onActivityCreated (before initSession), which should happen if
            // users follow Android guidelines and call super.onStart as the first thing in Activity.onStart,
            // however, if they don't, we try to set currentActivityReference_ here too.
            branch.currentActivityReference_ = new WeakReference<>(activity);
        }
    }

    /**
     * Helps differentiating between sdk session auto-initialization and client driven session
     * initialization. For internal SDK use only.
     */
    InitSessionBuilder isAutoInitialization(boolean isAuto) {
        this.isAutoInitialization = isAuto;
        return this;
    }

    /**
     * <p> Add callback to Branch initialization to retrieve referring params attached to the
     * Branch link via the dashboard. User eventually decides how to use the referring params but
     * they are primarily meant to be used for navigating to specific content within the app.
     * Use only one withCallback() method.</p>
     *
     * @param callback A {@link Branch.BranchUniversalReferralInitListener} instance that will be called
     *                 following successful (or unsuccessful) initialisation of the session
     *                 with the Branch API.
     */
    @SuppressWarnings("WeakerAccess")
    public InitSessionBuilder withCallback(Branch.BranchUniversalReferralInitListener callback) {
        this.callback = new BranchUniversalReferralInitWrapper(callback);
        return this;
    }

    /**
     * <p> Delay session initialization by certain time (used when other async or otherwise time
     * consuming ops need to be completed prior to session initialization).</p>
     *
     * @param delayMillis An {@link Integer} indicating the length of the delay in milliseconds.
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
     * @param callback A {@link Branch.BranchReferralInitListener} instance that will be called
     *                 following successful (or unsuccessful) initialisation of the session
     *                 with the Branch API.
     */
    @SuppressWarnings("WeakerAccess")
    public InitSessionBuilder withCallback(Branch.BranchReferralInitListener callback) {
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
     * <p> Use this method cautiously, it is meant to enable the ability to start a session before
     * the user even opens the app.
     * <p>
     * The use case explained:
     * Users are expected to initialize session from Activity.onStart. However, by default, Branch actually
     * waits until Activity.onResume to start session initialization, so as to ensure that the latest intent
     * data is available (e.g. when activity is launched from stack via onNewIntent). Setting this flag to true
     * will bypass waiting for intent, so session could technically be initialized from a background service
     * or otherwise before the application is even opened.
     * <p>
     * Note however that if the flag is not reset during normal app boot up, the SDK behavior is undefined
     * in certain cases. See also Branch.bypassWaitingForIntent(boolean). </p>
     *
     * @param ignore a {@link Boolean} indicating if SDK should wait for onResume to retrieve
     *               the most up recent intent data before firing the session initialization request.
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
        PrefHelper.Debug("Beginning session initialization");
        PrefHelper.Debug("Session uri is " + uri);

        if (Branch.deferInitForPluginRuntime) {
            PrefHelper.Debug("Session init is deferred until signaled by plugin.");
            cacheSessionBuilder(this);
            return;
        }

        final Branch branch = Branch.getInstance();
        if (branch == null) {
            PrefHelper.LogAlways("Branch is not setup properly, make sure to call getAutoInstance" + " in your application class or declare BranchApp in your manifest.");
            return;
        }
        if (ignoreIntent != null) {
            Branch.bypassWaitingForIntent(ignoreIntent);
        }

        Activity activity = branch.getCurrentActivity();
        Intent intent = activity != null ? activity.getIntent() : null;

        if (activity != null && intent != null && ActivityCompat.getReferrer(activity) != null) {
            PrefHelper.getInstance(activity).setInitialReferrer(ActivityCompat.getReferrer(activity).toString());
        }

        if (uri != null) {
            branch.readAndStripParam(uri, activity);
        }
        else if (isReInitializing && branch.isRestartSessionRequested(intent)) {
            branch.readAndStripParam(intent != null ? intent.getData() : null, activity);
        }
        else if (isReInitializing) {
            // User called reInit but isRestartSessionRequested = false, meaning the new intent was
            // not initiated by Branch and should not be considered a "new session", return early
            if (callback != null) {
                callback.onInitFinished(null, new BranchError("", ERR_IMPROPER_REINITIALIZATION));
            }
            return;
        }

        // readAndStripParams (above) may set isInstantDeepLinkPossible to true
        if (branch.isInstantDeepLinkPossible) {
            // reset state
            branch.isInstantDeepLinkPossible = false;
            // invoke callback returning LatestReferringParams, which were parsed out inside readAndStripParam
            // from either intent extra "branch_data", or as parameters attached to the referring app link
            if (callback != null) {
                callback.onInitFinished(branch.getLatestReferringParams(), null);
            }
            // mark this session as IDL session
            branch.addExtraInstrumentationData(Defines.Jsonkey.InstantDeepLinkSession.getKey(), "true");
            // potentially routes the user to the Activity configured to consume this particular link
            branch.checkForAutoDeepLinkConfiguration();
            // we already invoked the callback for let's set it to null, we will still make the
            // init session request but for analytics purposes only
            callback = null;
        }

        if (delay > 0) {
            Branch.expectDelayedSessionInitialization(true);
        }

        ServerRequestInitSession initRequest = branch.getInstallOrOpenRequest(callback, isAutoInitialization);
        branch.initializeSession(initRequest, delay);
    }

    private void cacheSessionBuilder(InitSessionBuilder initSessionBuilder) {
        Branch.getInstance().deferredSessionBuilder = this;
        PrefHelper.Debug("Session initialization deferred until plugin invokes notifyNativeToInit()" + "\nCaching Session Builder " + Branch.getInstance().deferredSessionBuilder + "\nuri: " + Branch.getInstance().deferredSessionBuilder.uri + "\ncallback: " + Branch.getInstance().deferredSessionBuilder.callback + "\nisReInitializing: " + Branch.getInstance().deferredSessionBuilder.isReInitializing + "\ndelay: " + Branch.getInstance().deferredSessionBuilder.delay + "\nisAutoInitialization: " + Branch.getInstance().deferredSessionBuilder.isAutoInitialization + "\nignoreIntent: " + Branch.getInstance().deferredSessionBuilder.ignoreIntent);
    }

    /**
     * <p> Re-Initialize a session. Call from Activity.onNewIntent().
     * This solves a very specific use case, whereas the app is already in the foreground and a new
     * intent with a Uri is delivered to the foregrounded activity.
     * <p>
     * Note that the Uri can also be stored as an extra in the field under the key `IntentKeys.BranchURI.getKey()` (i.e. "branch").
     * <p>
     * Note also, that the since the method is expected to be called from Activity.onNewIntent(),
     * the implementation assumes the intent will be non-null and will contain a Branch link in
     * either the URI or in the the extra.</p>
     */
    @SuppressWarnings("WeakerAccess")
    public void reInit() {
        isReInitializing = true;
        init();
    }
}
