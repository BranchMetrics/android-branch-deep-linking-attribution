package io.branch.referral;

import android.content.Context;

import androidx.annotation.NonNull;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * Async task to fetch GAID and LAT value.
 * This task fetch the GAID and LAT in background. The Background task times out after GAID_FETCH_TIME_OUT.
 * </p>
 */
public class GAdsPrefetchTask extends BranchAsyncTask<Void, Void, Void> {
    private static final int GAID_FETCH_TIME_OUT = 1500;

    private WeakReference<Context> contextRef_;
    private final SystemObserver.AdsParamsFetchEvents callback_;

    GAdsPrefetchTask(Context context, SystemObserver.AdsParamsFetchEvents callback) {
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
                    android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
                    Object adInfoObj = getAdInfoObject(context);

                    DeviceInfo di = DeviceInfo.getInstance();
                    if (di == null) di = DeviceInfo.initialize(context);// some tests complete early and garbage collect DeviceInfo singleton before this point is reached

                    SystemObserver so = di.getSystemObserver();
                    if (so != null) {
                        setGoogleLATWithAdvertisingIdClient(so, adInfoObj);
                        // LAT value determines whether we store GAID value or not
                        if (so.getLATVal() == 1) {
                            so.setGAID(null);
                        } else {
                            setGAIDWithAdvertisingIdClient(so, adInfoObj);
                        }
                    }
                }
                latch.countDown();
            }
        }).start();

        try {
            //Wait GAID_FETCH_TIME_OUT milli sec max to receive the GAID and LAT
            latch.await(GAID_FETCH_TIME_OUT, TimeUnit.MILLISECONDS);
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
     * Returns an instance of com.google.android.gms.ads.identifier.AdvertisingIdClient class  to be used
     * for getting GAId and LAT value
     *
     * @param context Context.
     * @return {@link Object} instance of AdvertisingIdClient class
     */
    private Object getAdInfoObject(Context context) {
        Object adInfoObj = null;
        if (context != null) {
            try {
                Class<?> advertisingIdClientClass = Class.forName("com.google.android.gms.ads.identifier.AdvertisingIdClient");
                Method getAdvertisingIdInfoMethod = advertisingIdClientClass.getMethod("getAdvertisingIdInfo", Context.class);
                adInfoObj = getAdvertisingIdInfoMethod.invoke(null, context);
            } catch (Throwable ignore) {
                PrefHelper.Debug("Either class com.google.android.gms.ads.identifier.AdvertisingIdClient " +
                        "or its method, getAdvertisingIdInfo, was not found");
            }
        }
        return adInfoObj;
    }

    /**
     * <p>Set the limit-ad-tracking status of the advertising identifier.</p>
     * <p>Check the Google Play services to for LAT enabled or disabled and return the LAT value as an integer.</p>
     *
     * @param adInfoObj AdvertisingIdClient.
     * @see <a href="https://developers.google.com/android/reference/com/google/android/gms/ads/identifier/AdvertisingIdClient.Info.html#isLimitAdTrackingEnabled()">
     * Android Developers - Limit Ad Tracking</a>
     */
    private void setGoogleLATWithAdvertisingIdClient(@NonNull SystemObserver so, Object adInfoObj) {
        try {
            Method getLatMethod = adInfoObj.getClass().getMethod("isLimitAdTrackingEnabled");
            Object latEnabled = getLatMethod.invoke(adInfoObj);
            if (latEnabled instanceof Boolean) {
                so.setLAT((Boolean) latEnabled ? 1 : 0);
            }
        } catch (Exception ignore) {
            PrefHelper.Debug("isLimitAdTrackingEnabled method not found");
        }
    }


    /**
     * <p>Google now requires that all apps use a standardised Advertising ID for all ad-based
     * actions within Android apps.</p>
     * <p>The Google Play services APIs expose the advertising tracking ID as UUID such as this:</p>
     * <pre>38400000-8cf0-11bd-b23e-10b96e40000d</pre>
     *
     * @param adInfoObj AdvertisingIdClient.
     * @see <a href="https://developer.android.com/google/play-services/id.html"> Android Developers - Advertising ID</a>
     */
    private void setGAIDWithAdvertisingIdClient(@NonNull SystemObserver so, Object adInfoObj) {
        try {
            Method getIdMethod = adInfoObj.getClass().getMethod("getId");
            so.setGAID((String) getIdMethod.invoke(adInfoObj));
        } catch (Exception ignore) {
        }
    }
}