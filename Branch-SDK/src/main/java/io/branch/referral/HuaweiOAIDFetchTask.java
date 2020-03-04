package io.branch.referral;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static io.branch.referral.SystemObserver.UUID_EMPTY;

/**
 * <p>
 * Async task to fetch OAID and LAT value.
 * This task fetch the OAID and LAT in background. The Background task times out after OAID_FETCH_TIME_OUT.
 * </p>
 */
public class HuaweiOAIDFetchTask extends BranchAsyncTask<Void, Void, Void> {
    private static final int OAID_FETCH_TIME_OUT = 1500;

    private WeakReference<Context> contextRef_;
    private final SystemObserver.AdsParamsFetchEvents callback_;

    HuaweiOAIDFetchTask(Context context, SystemObserver.AdsParamsFetchEvents callback) {
        contextRef_ = new WeakReference<>(context);
        callback_ = callback;
    }

    @Override
    protected Void doInBackground(Void... params) {

        final CountDownLatch latch = new CountDownLatch(1);
        new Thread(new Runnable() {
            @Override
            public void run() {
                Context context = contextRef_.get();
                if (context != null) {
                    setOAID(context);
                }
                latch.countDown();
            }
        }).start();

        try {
            //Wait GAID_FETCH_TIME_OUT milli sec max to receive the GAID and LAT
            latch.await(OAID_FETCH_TIME_OUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        if (callback_ != null) {
            callback_.onAdsParamsFetchFinished();
        }
    }

    /**
     * Set advertising ID derived from Huawei Mobile Services (aka Open Advertising ID)
     *
     * @param context Context.
     */
    private void setOAID(@NonNull Context context) {
        //https://developer.huawei.com/consumer/en/doc/development/HMS-References/27462122
        try {
            // get Huawei AdvertisingIdClient
            Class HW_AdvertisingIdClient = Class.forName("com.huawei.hms.ads.identifier.AdvertisingIdClient");
            // get Huawei AdvertisingIdClient.Info
            Method HW_getAdvertisingIdInfo = HW_AdvertisingIdClient.getDeclaredMethod("getAdvertisingIdInfo", Context.class);
            Object HW_AdvertisingIdClient_Info = HW_getAdvertisingIdInfo.invoke(null, context);

            // get Huawei's ad id
            Method HW_getId = HW_AdvertisingIdClient_Info.getClass().getDeclaredMethod("getId");
            String HW_id = HW_getId.invoke(HW_AdvertisingIdClient_Info).toString();

            // get Huawei's lat
            Method HW_isLimitAdTrackingEnabled = HW_AdvertisingIdClient_Info.getClass().getDeclaredMethod("isLimitAdTrackingEnabled");
            Boolean HW_lat = (Boolean) HW_isLimitAdTrackingEnabled.invoke(HW_AdvertisingIdClient_Info);

            DeviceInfo di = DeviceInfo.getInstance();
            if (di == null) di = DeviceInfo.initialize(context);// some tests complete early and garbage collect DeviceInfo singleton before this point is reached

            SystemObserver so = di.getSystemObserver();
            so.setGAID(HW_id);

            so.setLAT(HW_lat ? 1 : 0);
            if (TextUtils.isEmpty(HW_id) || HW_id.equals(UUID_EMPTY) || HW_lat) {
                so.setGAID(null);
            }
        } catch (Throwable e) {
            PrefHelper.Debug("failed to retrieve OAID, error = " + e);
        }
    }
}