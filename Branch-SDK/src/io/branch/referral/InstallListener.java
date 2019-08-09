package io.branch.referral;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Class for listening for installation referrer params. Install params are captured by either of the following methods
 * <p>
 * 1) Add the install referrer library to your application "com.android.installreferrer:installreferrer" (Recommended)
 *   dependencies {
 *     compile 'com.android.installreferrer:installreferrer:1.0'
 *   }
 * </p><p>
 * 2) Add to a broadcast listener to manifest as follows to receive Install Referrer
 *   <receiver android:name="io.branch.referral.InstallListener" android:exported="true">
 *     <intent-filter>
 *     <action android:name="com.android.vending.INSTALL_REFERRER" />
 *     </intent-filter>
 *   </receiver>
 * </p>
 */
public class InstallListener extends BroadcastReceiver {
    
    /* Link identifier on installing app from play store. */
    private static String installID_ = PrefHelper.NO_STRING_VALUE;
    private static IInstallReferrerEvents callback_ = null;

    private static boolean isWaitingForReferrer;

    /* Specifies if the install referrer client is available */
    private static boolean isReferrerClientAvailable;

    // PRS : In case play store referrer get reported really fast as google fix bugs , this implementation will let the referrer parsed and stored
    //       This will be reported when SDK ask for it
    static boolean unReportedReferrerAvailable;
    
    public void captureInstallReferrer(Context context, final long maxWaitTime, IInstallReferrerEvents installReferrerFetch) {
        callback_ = installReferrerFetch;
        if (unReportedReferrerAvailable) {
            reportInstallReferrer();
        } else {
            isWaitingForReferrer = true;
            InstallReferrerClientWrapper referrerClientWrapper = new InstallReferrerClientWrapper(context);
            isReferrerClientAvailable = referrerClientWrapper.getReferrerUsingReferrerClient(installReferrerWrapperListener);
            new Timer().schedule(new TimerTask() {
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
        processReferrerInfo(context, rawReferrerString, 0, 0);
        if (isWaitingForReferrer && !isReferrerClientAvailable) { // Still wait for referrer client to get the time stamps if it is active
            reportInstallReferrer();
        }
    }

    InstallReferrerClientWrapper.InstallReferrerWrapperListener installReferrerWrapperListener = new InstallReferrerClientWrapper.InstallReferrerWrapperListener() {
        @Override
        public void onReferrerClientFinished(Context context, String rawReferrerString, long clickTS, long InstallBeginTS) {
            PrefHelper.Debug("onReferrerClientFinished()");
            processReferrerInfo(context, rawReferrerString, clickTS, InstallBeginTS);
            if (isWaitingForReferrer) {
                reportInstallReferrer();
            }
        }

        @Override
        public void onReferrerClientError() {
            PrefHelper.Debug("onReferrerClientError()");
            isReferrerClientAvailable = false;
        }
    };

    private static void processReferrerInfo(Context context, String rawReferrerString, long referrerClickTS, long installClickTS) {
        PrefHelper prefHelper = PrefHelper.getInstance(context);
        if (referrerClickTS > 0) {
            prefHelper.setLong(PrefHelper.KEY_REFERRER_CLICK_TS, referrerClickTS);
        }
        if (installClickTS > 0) {
            prefHelper.setLong(PrefHelper.KEY_INSTALL_BEGIN_TS, installClickTS);
        }
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
                
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                PrefHelper.Debug("Illegal characters in url encoded string");
            }
        }
    }
    
    public static String getInstallationID() {
        return installID_;
    }
    
    private static void reportInstallReferrer() {
        unReportedReferrerAvailable = true;
        if (callback_ != null) {
            callback_.onInstallReferrerEventsFinished();
            callback_ = null;
            unReportedReferrerAvailable = false;
            isWaitingForReferrer = false;
            isReferrerClientAvailable = false;
        }
    }
    
    interface IInstallReferrerEvents {
        void onInstallReferrerEventsFinished();
    }
    
}
