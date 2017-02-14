package io.branch.referral;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;

/**
 * <p> Class for listening installation referrer params. Add this class to your manifest in order to get a referrer info </p>
 *
 * <p> Add to InstallListener to manifest as follows
  <!--   <receiver android:name="io.branch.referral.InstallListener" android:exported="true">
         <intent-filter>
         <action android:name="com.android.vending.INSTALL_REFERRER" />
         </intent-filter>
     </receiver> -->
 </p>
 */
public class InstallListener extends BroadcastReceiver {

    /* Link identifier on installing app from play store. */
    private static String installID_ = PrefHelper.NO_STRING_VALUE;
    private static IInstallReferrerEvents callback_ = null;

    /* URL identifier from an ad click on Google Search. */
    private static String googleSearchInstallReferrerID_ = PrefHelper.NO_STRING_VALUE;

    @Override
    public void onReceive(Context context, Intent intent) {
        String rawReferrerString = intent.getStringExtra("referrer");
        if (rawReferrerString != null) {
            try {
                rawReferrerString = URLDecoder.decode(rawReferrerString, "UTF-8");
                HashMap<String, String> referrerMap = new HashMap<>();
                String[] referralParams = rawReferrerString.split("&");

                for (String referrerParam : referralParams) {
                    String[] keyValue = referrerParam.split("=");
                    if (keyValue.length > 1) { // To make sure that there is one key value pair in referrer
                        referrerMap.put(URLDecoder.decode(keyValue[0], "UTF-8"), URLDecoder.decode(keyValue[1], "UTF-8"));
                    }
                }

                if (referrerMap.containsKey(Defines.Jsonkey.LinkClickID.getKey())) {
                    installID_ = referrerMap.get(Defines.Jsonkey.LinkClickID.getKey());
                    if ( callback_ != null ) {
                        callback_.onInstallReferrerEventsFinished(installID_);
                    }
                } else {
                    if ( callback_ != null ) {
                        callback_.onInstallReferrerEventsFinished(rawReferrerString);
                    }
                }

                if (referrerMap.containsKey(Defines.Jsonkey.GoogleSearchInstallReferrer.getKey())) {
                    googleSearchInstallReferrerID_ = referrerMap.get(Defines.Jsonkey.GoogleSearchInstallReferrer.getKey());
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

    public static void setListener(IInstallReferrerEvents installReferrerFetch) {
        callback_ = installReferrerFetch;
    }

    interface IInstallReferrerEvents {
        void onInstallReferrerEventsFinished(String linkClickId);
    }

    public static String getGoogleSearchInstallReferrerID() {
        return googleSearchInstallReferrerID_;
    }
}
