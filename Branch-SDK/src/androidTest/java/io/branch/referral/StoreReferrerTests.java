package io.branch.referral;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class StoreReferrerTests extends BranchTest {
    Context context;
    PrefHelper prefHelper;

    @Before
    public void init(){
        context = getTestContext();
        prefHelper = PrefHelper.getInstance(context);
    }

    @Test
    public void testReturnLatestInstallReferrer_AllValidReferrers(){
        // TODO: MockedStatic method references are not supported at language level '7'
        // MockedStatic<StoreReferrerHuaweiAppGallery> mockedStatic = Mockito.mockStatic(StoreReferrerHuaweiAppGallery.class);
        // mockedStatic.when(StoreReferrerHuaweiAppGallery::fetch).then()

        StoreReferrerGooglePlayStore.installBeginTimestamp = Long.MAX_VALUE-1;
        StoreReferrerGooglePlayStore.clickTimestamp = Long.MAX_VALUE-1;
        StoreReferrerGooglePlayStore.rawReferrer = "google";

        StoreReferrerHuaweiAppGallery.installBeginTimestamp = Long.MAX_VALUE;
        StoreReferrerHuaweiAppGallery.clickTimestamp = Long.MAX_VALUE;
        StoreReferrerHuaweiAppGallery.rawReferrer = "huawei";

        StoreReferrerSamsungGalaxyStore.installBeginTimestamp = Long.MAX_VALUE-2;
        StoreReferrerSamsungGalaxyStore.clickTimestamp = Long.MAX_VALUE-2;
        StoreReferrerSamsungGalaxyStore.rawReferrer = "samsung";

        StoreReferrerXiaomiGetApps.installBeginTimestamp = Long.MAX_VALUE-3;
        StoreReferrerXiaomiGetApps.clickTimestamp = Long.MAX_VALUE-3;
        StoreReferrerXiaomiGetApps.rawReferrer = "xiaomi";

        String result = StoreReferrerUtils.getLatestValidReferrerStore(context);

        Assert.assertEquals(Defines.Jsonkey.Huawei_App_Gallery.getKey(), result);

        StoreReferrerUtils.writeLatestInstallReferrer(context, result);
        Assert.assertEquals(Defines.Jsonkey.Huawei_App_Gallery.getKey(), prefHelper.getAppStoreSource());
    }

    @Test
    public void testReturnLatestInstallReferrer_NoReferrer(){
        StoreReferrerGooglePlayStore.installBeginTimestamp = Long.MIN_VALUE;
        StoreReferrerGooglePlayStore.clickTimestamp = Long.MIN_VALUE;
        StoreReferrerGooglePlayStore.rawReferrer = null;

        StoreReferrerHuaweiAppGallery.installBeginTimestamp = Long.MIN_VALUE;
        StoreReferrerHuaweiAppGallery.clickTimestamp = Long.MIN_VALUE;
        StoreReferrerHuaweiAppGallery.rawReferrer = null;

        StoreReferrerSamsungGalaxyStore.installBeginTimestamp = Long.MIN_VALUE;
        StoreReferrerSamsungGalaxyStore.clickTimestamp = Long.MIN_VALUE;
        StoreReferrerSamsungGalaxyStore.rawReferrer = null;

        StoreReferrerXiaomiGetApps.installBeginTimestamp = Long.MIN_VALUE;
        StoreReferrerXiaomiGetApps.clickTimestamp = Long.MIN_VALUE;
        StoreReferrerXiaomiGetApps.rawReferrer = null;

        String result = StoreReferrerUtils.getLatestValidReferrerStore(context);

        Assert.assertEquals("", result);

        StoreReferrerUtils.writeLatestInstallReferrer(context, result);
        Assert.assertEquals(PrefHelper.NO_STRING_VALUE, prefHelper.getAppStoreSource());
    }

    @Test
    public void testReturnLatestInstallReferrer_AllSameTimestamp_OneNotNullReferrerString(){
        StoreReferrerGooglePlayStore.installBeginTimestamp = 0L;
        StoreReferrerGooglePlayStore.clickTimestamp = 0L;
        StoreReferrerGooglePlayStore.rawReferrer = "utm_source=google-play&utm_medium=organic";

        StoreReferrerHuaweiAppGallery.installBeginTimestamp = 0L;
        StoreReferrerHuaweiAppGallery.clickTimestamp = 0L;
        StoreReferrerHuaweiAppGallery.rawReferrer = null;

        StoreReferrerSamsungGalaxyStore.installBeginTimestamp = 0L;
        StoreReferrerSamsungGalaxyStore.clickTimestamp = 0L;
        StoreReferrerSamsungGalaxyStore.rawReferrer = null;

        StoreReferrerXiaomiGetApps.installBeginTimestamp = 0L;
        StoreReferrerXiaomiGetApps.clickTimestamp = 0L;
        StoreReferrerXiaomiGetApps.rawReferrer = null;

        String result = StoreReferrerUtils.getLatestValidReferrerStore(context);

        Assert.assertEquals(Defines.Jsonkey.Google_Play_Store.getKey(), result);

        StoreReferrerUtils.writeLatestInstallReferrer(context, result);
        Assert.assertEquals(Defines.Jsonkey.Google_Play_Store.getKey(), prefHelper.getAppStoreSource());
    }

    @Test
    public void testWriteLatestInstallReferrer(){
        StoreReferrerGooglePlayStore.installBeginTimestamp = 0L;
        StoreReferrerGooglePlayStore.clickTimestamp = 0L;
        StoreReferrerGooglePlayStore.rawReferrer = "utm_source=google-play&utm_medium=organic";

        StoreReferrerHuaweiAppGallery.installBeginTimestamp = 0L;
        StoreReferrerHuaweiAppGallery.clickTimestamp = 0L;
        StoreReferrerHuaweiAppGallery.rawReferrer = null;

        StoreReferrerSamsungGalaxyStore.installBeginTimestamp = 0L;
        StoreReferrerSamsungGalaxyStore.clickTimestamp = 0L;
        StoreReferrerSamsungGalaxyStore.rawReferrer = null;

        StoreReferrerXiaomiGetApps.installBeginTimestamp = 0L;
        StoreReferrerXiaomiGetApps.clickTimestamp = 0L;
        StoreReferrerXiaomiGetApps.rawReferrer = null;

        String result = StoreReferrerUtils.getLatestValidReferrerStore(context);

        StoreReferrerUtils.writeLatestInstallReferrer(context, result);
        Assert.assertEquals(StoreReferrerGooglePlayStore.rawReferrer, PrefHelper.getInstance(context).getAppStoreReferrer());
        Assert.assertEquals(Defines.Jsonkey.Google_Play_Store.getKey(), prefHelper.getAppStoreSource());
    }
}
