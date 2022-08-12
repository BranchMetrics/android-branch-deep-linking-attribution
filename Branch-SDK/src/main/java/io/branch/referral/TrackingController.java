package io.branch.referral;

import android.content.Context;

import androidx.annotation.NonNull;

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
    
    void disableTracking(Context context, boolean disableTracking) {
        if (trackingDisabled != disableTracking) {
            trackingDisabled = disableTracking;
            if (disableTracking) {
                onTrackingDisabled(context);
            } else {
                onTrackingEnabled();
            }
            PrefHelper.getInstance(context).setBool(PrefHelper.KEY_TRACKING_STATE, disableTracking);
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
        prefHelper.saveLastStrongMatchTime(0);
    }
    
    private void onTrackingEnabled() {
        Branch branch = Branch.getInstance();
        if (branch != null) {
            branch.registerAppInit(branch.getInstallOrOpenRequest(null, true), true);
        }
    }
}
