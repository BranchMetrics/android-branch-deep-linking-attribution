package io.branch.referral;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;

/**
 * <p> Class for listening installation referrer params. Add this class to your manifest in order to get a referrer info </p>
 *
 * <p> Add to InstallListener to manifest as follows
     <receiver android:name="io.branch.referral.InstallListener" android:exported="true">
         <intent-filter>
         <action android:name="com.android.vending.INSTALL_REFERRER" />
         </intent-filter>
     </receiver></p>
 */
public class InstallListener extends BroadcastReceiver {

    /* Link identifier on installing app from play store. */
    private static String installID_ =  PrefHelper.NO_STRING_VALUE;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("installTest", "InstallListener :- Received install referrer event");
        String rawReferrerString = intent.getStringExtra("referrer");
        if(rawReferrerString != null) {
            try {
                rawReferrerString = URLDecoder.decode(rawReferrerString, "UTF-8");
                HashMap<String, String> referrerMap = new HashMap<String, String>();
                String[] referralParams = rawReferrerString.split("&");

                for (String referrerParam : referralParams) {
                    String[] keyValue  =  referrerParam.split("=");
                    referrerMap.put(URLDecoder.decode(keyValue[0]),URLDecoder.decode(keyValue[1]));
                    Log.d("installTest", keyValue[0] +" = " + keyValue[1]);
                }

                if(referrerMap.containsKey(Defines.Jsonkey.LinkClickID.getKey())){
                    installID_ = referrerMap.get(Defines.Jsonkey.LinkClickID.getKey());
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

        }
    }

    public static String getInstallationID(){
        return installID_;
    }
}
