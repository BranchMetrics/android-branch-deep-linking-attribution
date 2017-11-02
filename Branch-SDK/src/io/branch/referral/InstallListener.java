package io.branch.referral;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;

/**
 * <p> Class for listening installation referrer params. Add this class to your manifest in order to get a referrer info </p>
 * <p>
 * <p> Add to InstallListener to manifest as follows
 * <!--   <receiver android:name="io.branch.referral.InstallListener" android:exported="true">
 * <intent-filter>
 * <action android:name="com.android.vending.INSTALL_REFERRER" />
 * </intent-filter>
 * </receiver> -->
 * </p>
 */
public class InstallListener extends BroadcastReceiver {
    
    /* Link identifier on installing app from play store. */
    private static String installID_ = PrefHelper.NO_STRING_VALUE;
    private static IInstallReferrerEvents callback_ = null;
    
    
    private static boolean isWaitingForReferrer;
    // PRS : In case play store referrer get reported really fast as google fix bugs , this implementation will let the referrer parsed and stored
    //       This will be reported when SDK ask for it
    private static boolean unReportedReferrerAvailable;
    
    public static void captureInstallReferrer(final long maxWaitTime) {
        if (unReportedReferrerAvailable) {
            reportInstallReferrer();
        } else {
            isWaitingForReferrer = true;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    reportInstallReferrer();
                }
            }, maxWaitTime);
        }
    }
    
    @Override
    public void onReceive(Context context, Intent intent) {
        String rawReferrerString = intent.getStringExtra("referrer");
        if (rawReferrerString != null) {
            try {
                rawReferrerString = URLDecoder.decode(rawReferrerString, "UTF-8");
                HashMap<String, String> referrerMap = new HashMap<>();
                String[] referralParams = rawReferrerString.split("&");
                
                for (String referrerParam : referralParams) {
                    if (!TextUtils.isEmpty(referrerParam)) {
                        String splitter = "=";
                        if (!referrerParam.contains("=") && referrerParam.contains("-")) {
                            splitter = "-";
                        }
                        String[] keyValue = referrerParam.split(splitter);
                        if (keyValue.length > 1) { // To make sure that there is one key value pair in referrer
                            referrerMap.put(URLDecoder.decode(keyValue[0], "UTF-8"), URLDecoder.decode(keyValue[1], "UTF-8"));
                        }
                    }
                }
                
                PrefHelper prefHelper = PrefHelper.getInstance(context);
                
                if (referrerMap.containsKey(Defines.Jsonkey.LinkClickID.getKey())) {
                    installID_ = referrerMap.get(Defines.Jsonkey.LinkClickID.getKey());
                    prefHelper.setLinkClickIdentifier(installID_);
                    
                }
                // Check for full app conversion
                if (referrerMap.containsKey(Defines.Jsonkey.IsFullAppConv.getKey())
                        && referrerMap.containsKey(Defines.Jsonkey.ReferringLink.getKey())) {
                    prefHelper.setIsFullAppConversion(Boolean.parseBoolean(referrerMap.get(Defines.Jsonkey.IsFullAppConv.getKey())));
                    prefHelper.setAppLink(referrerMap.get(Defines.Jsonkey.ReferringLink.getKey()));
                }
                
                if (referrerMap.containsKey(Defines.Jsonkey.GoogleSearchInstallReferrer.getKey())) {
                    prefHelper.setGoogleSearchInstallIdentifier(referrerMap.get(Defines.Jsonkey.GoogleSearchInstallReferrer.getKey()));
                    prefHelper.setGooglePlayReferrer(rawReferrerString);
                }
                unReportedReferrerAvailable = true;
                
                if (isWaitingForReferrer) {
                    reportInstallReferrer();
                }
                
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                Log.w("BranchSDK", "Illegal characters in url encoded string");
            }
            
        }
    }
    
    public static String getInstallationID() {
        return installID_;
    }
    
    private static void reportInstallReferrer() {
        if (callback_ != null) {
            callback_.onInstallReferrerEventsFinished();
            callback_ = null;
            unReportedReferrerAvailable = false;
        }
    }
    
    public static void setListener(IInstallReferrerEvents installReferrerFetch) {
        callback_ = installReferrerFetch;
    }
    
    interface IInstallReferrerEvents {
        void onInstallReferrerEventsFinished();
    }
    
    
}
