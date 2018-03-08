package io.branch.referral;

import android.content.Context;

/**
 * Created by sojanpr on 3/7/18.
 * <p>
 * Class for handling the SDK user data or state tracking features
 * If tracking disabled SDK will not track any user data or state.
 * SDK will not send any network calls when tracking is disabled
 * </p>
 */

class TrackingController {
    /* Flag for controlling the user data tracking state. If disabled SDK will not track any user data or state. SDK will not send any network calls when tracking is disabled*/
    private boolean trackingDisabled = true;
    
    TrackingController(Context context) {
        updateTrackingState(context);
    }
    
    void disableTracking(Context context, boolean disableTracking) {
        if (disableTracking) {
            onTrackingDisabled(context);
        }
        PrefHelper.getInstance(context).setBool(PrefHelper.KEY_TRACKING_STATE, disableTracking);
    }
    
    boolean isTrackingDisabled() {
        return trackingDisabled;
    }
    
    void updateTrackingState(Context context) {
        trackingDisabled = PrefHelper.getInstance(context).getBool(PrefHelper.KEY_TRACKING_STATE);
    }
    
    private void onTrackingDisabled(Context context) {
        Branch.getInstance().closeSessionInternal(true);
        // Disabling tracking should take immediate effect where as tracking enable will take effect from next open
        trackingDisabled = true;
        
        // Clear  any tracking specific preference items
        PrefHelper prefHelper = PrefHelper.getInstance(context);
        prefHelper.clearBranchAnalyticsData();
        prefHelper.setDeviceFingerPrintID(PrefHelper.NO_STRING_VALUE);
        prefHelper.setSessionID(PrefHelper.NO_STRING_VALUE);
        prefHelper.setIdentityID(PrefHelper.NO_STRING_VALUE);
        prefHelper.setIdentity(PrefHelper.NO_STRING_VALUE);
        prefHelper.setLinkClickID(PrefHelper.NO_STRING_VALUE);
        prefHelper.setLinkClickIdentifier(PrefHelper.NO_STRING_VALUE);
        prefHelper.setAppLink(PrefHelper.NO_STRING_VALUE);
        prefHelper.setInstallReferrerParams(PrefHelper.NO_STRING_VALUE);
        prefHelper.setGooglePlayReferrer(PrefHelper.NO_STRING_VALUE);
        prefHelper.setGoogleSearchInstallIdentifier(PrefHelper.NO_STRING_VALUE);
        prefHelper.setExternalIntentUri(PrefHelper.NO_STRING_VALUE);
        prefHelper.setExternalIntentExtra(PrefHelper.NO_STRING_VALUE);
        prefHelper.setSessionParams(PrefHelper.NO_STRING_VALUE);
        prefHelper.setInstallParams(PrefHelper.NO_STRING_VALUE);
        prefHelper.saveLastStrongMatchTime(0);
        
        // Clear install time stamps
        prefHelper.setLong(PrefHelper.KEY_REFERRER_CLICK_TS, 0);
        prefHelper.setLong(PrefHelper.KEY_INSTALL_BEGIN_TS, 0);
        prefHelper.setLong(PrefHelper.KEY_LAST_KNOWN_UPDATE_TIME, 0);
        prefHelper.setLong(PrefHelper.KEY_PREVIOUS_UPDATE_TIME, 0);
        prefHelper.setLong(PrefHelper.KEY_ORIGINAL_INSTALL_TIME, 0);
        
    }
}
