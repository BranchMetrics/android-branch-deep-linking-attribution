package io.branch.referral;

import static io.branch.referral.Defines.Jsonkey.Google_Play_Store;
import static io.branch.referral.Defines.Jsonkey.Huawei_App_Gallery;
import static io.branch.referral.Defines.Jsonkey.Samsung_Galaxy_Store;
import static io.branch.referral.Defines.Jsonkey.Xiaomi_Get_Apps;

import android.content.Context;
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
        Long latestTimeStamp = 0L;

        if(StoreReferrerGooglePlayStore.installBeginTimestamp > latestTimeStamp){
            latestTimeStamp = StoreReferrerGooglePlayStore.installBeginTimestamp;
            result = Google_Play_Store.getKey();
        }

        if(StoreReferrerHuaweiAppGallery.installBeginTimestamp > latestTimeStamp){
            latestTimeStamp = StoreReferrerHuaweiAppGallery.installBeginTimestamp;
            result = Huawei_App_Gallery.getKey();
        }

        if(StoreReferrerSamsungGalaxyStore.installBeginTimestamp > latestTimeStamp){
            latestTimeStamp = StoreReferrerSamsungGalaxyStore.installBeginTimestamp;
            result = Samsung_Galaxy_Store.getKey();
        }

        if(StoreReferrerXiaomiGetApps.installBeginTimestamp > latestTimeStamp){
            result = Xiaomi_Get_Apps.getKey();
        }

        // iterate through non-null strings for cases like Google Play returning
        // "utm_source=google-play&utm_medium=organic" for organic installs
        if(result.isEmpty()){
            if(!TextUtils.isEmpty(StoreReferrerGooglePlayStore.rawReferrer)){
                result = Google_Play_Store.getKey();
            }

            if(!TextUtils.isEmpty(StoreReferrerHuaweiAppGallery.rawReferrer)){
                result = Huawei_App_Gallery.getKey();
            }

            if(!TextUtils.isEmpty(StoreReferrerSamsungGalaxyStore.rawReferrer)){
                result = Samsung_Galaxy_Store.getKey();
            }

            if(!TextUtils.isEmpty(StoreReferrerXiaomiGetApps.rawReferrer)){
                result = Xiaomi_Get_Apps.getKey();
            }
        }

        return result;
    }

    public static void writeLatestInstallReferrer(Context context_, String store) {
        if(store.equals(Defines.Jsonkey.Google_Play_Store.getKey())){
            AppStoreReferrer.processReferrerInfo(context_, StoreReferrerGooglePlayStore.rawReferrer, StoreReferrerGooglePlayStore.clickTimestamp, StoreReferrerGooglePlayStore.installBeginTimestamp, store);
        }
        if(store.equals(Defines.Jsonkey.Huawei_App_Gallery.getKey())){
            AppStoreReferrer.processReferrerInfo(context_, StoreReferrerHuaweiAppGallery.rawReferrer, StoreReferrerHuaweiAppGallery.clickTimestamp, StoreReferrerHuaweiAppGallery.installBeginTimestamp, store);
        }
        if(store.equals(Defines.Jsonkey.Samsung_Galaxy_Store.getKey())){
            AppStoreReferrer.processReferrerInfo(context_, StoreReferrerSamsungGalaxyStore.rawReferrer, StoreReferrerSamsungGalaxyStore.clickTimestamp, StoreReferrerSamsungGalaxyStore.installBeginTimestamp, store);
        }
        if(store.equals(Defines.Jsonkey.Xiaomi_Get_Apps.getKey())){
            AppStoreReferrer.processReferrerInfo(context_, StoreReferrerXiaomiGetApps.rawReferrer, StoreReferrerXiaomiGetApps.clickTimestamp, StoreReferrerXiaomiGetApps.installBeginTimestamp, store);
        }
    }
}
