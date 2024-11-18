package io.branch.referral;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONObject;

/**
 * Created by sojanpr on 3/7/18.
 * <p>
 * Class for handling the SDK user data or state tracking features
 * If tracking disabled SDK will not track any user data or state.
 * SDK will not send any network calls when tracking is disabled
 * </p>
 */

public class TrackingController {
    /* Flag for controlling the user data tracking state. If disabled SDK will not track any user data or state. SDK will not send any network calls when tracking is disabled*/
    private boolean trackingDisabled = true;
    
    TrackingController(Context context) {
        updateTrackingState(context);
    }

    void disableTracking(Context context, boolean disableTracking, @Nullable Branch.TrackingStateCallback callback) {
        BranchLogger.v("disableTracking context: " + context + " disableTracking: " + disableTracking + " callback: " + callback);
        // If the tracking state is already set to the desired state, then return instantly
        if (trackingDisabled == disableTracking) {
            if (callback != null) {
                BranchLogger.v("Tracking state is already set to " + disableTracking + ". Returning the same to the callback");
                callback.onTrackingStateChanged(trackingDisabled, Branch.getInstance().getFirstReferringParams(), null);
            }
            return;
        }

        trackingDisabled = disableTracking;
        PrefHelper.getInstance(context).setBool(PrefHelper.KEY_TRACKING_STATE, disableTracking);

        if (disableTracking) {
            BranchLogger.v("Tracking disabled. Clearing all pending requests");
            onTrackingDisabled(context);
            if (callback != null) {
                callback.onTrackingStateChanged(true, null, null);
            }
        } else {
            BranchLogger.v("Tracking enabled. Registering app init");
            onTrackingEnabled((referringParams, error) -> {
                if (callback != null) {
                    callback.onTrackingStateChanged(false, referringParams, error);
                }
            });
        }
    }

    boolean isTrackingDisabled() {
        return trackingDisabled;
    }

    public static boolean isTrackingDisabled(@NonNull Context context) {
        return PrefHelper.getInstance(context).getBool(PrefHelper.KEY_TRACKING_STATE);
    }
    
    void updateTrackingState(Context context) {
        trackingDisabled = PrefHelper.getInstance(context).getBool(PrefHelper.KEY_TRACKING_STATE);
    }
    
    private void onTrackingDisabled(Context context) {
        // Clear all pending requests
        Branch.getInstance().clearPendingRequests();
        
        // Clear  any tracking specific preference items
        PrefHelper prefHelper = PrefHelper.getInstance(context);
        prefHelper.setSessionID(PrefHelper.NO_STRING_VALUE);
        prefHelper.setLinkClickID(PrefHelper.NO_STRING_VALUE);
        prefHelper.setLinkClickIdentifier(PrefHelper.NO_STRING_VALUE);
        prefHelper.setAppLink(PrefHelper.NO_STRING_VALUE);
        prefHelper.setInstallReferrerParams(PrefHelper.NO_STRING_VALUE);
        prefHelper.setAppStoreReferrer(PrefHelper.NO_STRING_VALUE);
        prefHelper.setAppStoreSource(PrefHelper.NO_STRING_VALUE);
        prefHelper.setGoogleSearchInstallIdentifier(PrefHelper.NO_STRING_VALUE);
        prefHelper.setInitialReferrer(PrefHelper.NO_STRING_VALUE);
        prefHelper.setExternalIntentUri(PrefHelper.NO_STRING_VALUE);
        prefHelper.setExternalIntentExtra(PrefHelper.NO_STRING_VALUE);
        prefHelper.setSessionParams(PrefHelper.NO_STRING_VALUE);
        prefHelper.setAnonID(PrefHelper.NO_STRING_VALUE);
        prefHelper.setReferringUrlQueryParameters(new JSONObject());
        Branch.getInstance().clearPartnerParameters();
    }
    
    private void onTrackingEnabled(Branch.BranchReferralInitListener callback) {
        BranchLogger.v("onTrackingEnabled callback: " + callback);
        Branch branch = Branch.getInstance();
        if (branch != null) {
            branch.registerAppInit(branch.getInstallOrOpenRequest(callback, true), false);
        }
    }
}
