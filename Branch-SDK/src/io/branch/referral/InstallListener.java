package io.branch.referral;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

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
        String rawReferrerString = intent.getStringExtra("referrer");
        if(rawReferrerString != null) {
            try {
                rawReferrerString = URLDecoder.decode(rawReferrerString, "UTF-8");
                String[] referralParams = rawReferrerString.split("&");

                for (String referrerParam : referralParams) {
                    // Check for "link_click_id-123456" format
                    if(referrerParam.startsWith(Defines.Jsonkey.LinkClickIDNonEncoded.getKey())){
                        installID_ = referrerParam.replace(Defines.Jsonkey.LinkClickIDNonEncoded.getKey(),"");
                        installID_ =  URLDecoder.decode(installID_, "UTF-8");
                        break;
                    }
                    // Check for "link_click_id=123456" format
                    else {
                        String[] keyValue = referrerParam.split("=");
                        if (keyValue.length > 1 && keyValue[0].equalsIgnoreCase(Defines.Jsonkey.LinkClickID.getKey())) {
                            installID_ = URLDecoder.decode(keyValue[1], "UTF-8");
                            break;
                        }
                    }

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
