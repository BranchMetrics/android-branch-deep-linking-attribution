package io.branch.referral;

import static io.branch.referral.Defines.Jsonkey.GOOGLE_PLAY_STORE;
import static io.branch.referral.Defines.Jsonkey.HUAWEI_APP_GALLERY;
import static io.branch.referral.Defines.Jsonkey.SAMSUNG_GALAXY_STORE;
import static io.branch.referral.Defines.Jsonkey.XIAOMI_GET_APPS;

import android.text.TextUtils;

public class StoreReferrerUtils {

    /**
     * Algorithm to return latest valid store referrer information
     * Iterate through referrer's installation timestamps
     * If no store with a timestamp greater than min value is found
     * Then iterate for first non-null string.
     * @return Name of the store
     */
    public static String getLatestValidReferrerStore(){
        String result = "";
        Long latestTimeStamp = Long.MIN_VALUE;

        if(StoreReferrerGooglePlayStore.installBeginTimestamp > latestTimeStamp){
            latestTimeStamp = StoreReferrerGooglePlayStore.installBeginTimestamp;
            result = GOOGLE_PLAY_STORE.getKey();
        }

        if(StoreReferrerHuaweiAppGallery.installBeginTimestamp > latestTimeStamp){
            latestTimeStamp = StoreReferrerHuaweiAppGallery.installBeginTimestamp;
            result = HUAWEI_APP_GALLERY.getKey();
        }

        if(StoreReferrerSamsungGalaxyStore.installBeginTimestamp > latestTimeStamp){
            latestTimeStamp = StoreReferrerSamsungGalaxyStore.installBeginTimestamp;
            result = SAMSUNG_GALAXY_STORE.getKey();
        }

        if(StoreReferrerXiaomiGetApps.installBeginTimestamp > latestTimeStamp){
            result = XIAOMI_GET_APPS.getKey();
        }

        // iterate through non-null strings for cases like Google Play returning
        // "utm_source=google-play&utm_medium=organic" for organic installs
        if(result.isEmpty()){
            if(!TextUtils.isEmpty(StoreReferrerGooglePlayStore.rawReferrer)){
                result = GOOGLE_PLAY_STORE.getKey();
            }

            if(!TextUtils.isEmpty(StoreReferrerHuaweiAppGallery.rawReferrer)){
                result = HUAWEI_APP_GALLERY.getKey();
            }

            if(!TextUtils.isEmpty(StoreReferrerSamsungGalaxyStore.rawReferrer)){
                result = SAMSUNG_GALAXY_STORE.getKey();
            }

            if(!TextUtils.isEmpty(StoreReferrerXiaomiGetApps.rawReferrer)){
                result = XIAOMI_GET_APPS.getKey();
            }
        }

        return result;
    }
}
