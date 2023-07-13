package io.branch.referral;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.identifier.AdvertisingIdClient;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.branch.referral.coroutines.DataCoroutinesKt;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;

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

                DataCoroutinesKt.getAdvertisingInfoObject(context, new Continuation<AdvertisingIdClient.Info>() {
                    @NonNull
                    @Override
                    public CoroutineContext getContext() {
                        return EmptyCoroutineContext.INSTANCE;
                    }

                    @Override
                    public void resumeWith(Object o) {
                        PrefHelper.Debug("info object resumeWith " + o);
                        if(o != null){
                            AdvertisingIdClient.Info info = (AdvertisingIdClient.Info) o;

                            DeviceInfo di = DeviceInfo.getInstance();
                            if (di == null) di = new DeviceInfo(context);

                            SystemObserver so = di.getSystemObserver();
                            if (so != null) {
                                boolean latEnabled = info.isLimitAdTrackingEnabled();
                                so.setLAT(latEnabled ? 1 : 0);

                                // if limit ad tracking is enabled, null any advertising id
                                if(latEnabled){
                                    so.setGAID(null);
                                }
                                else{
                                    so.setGAID(info.getId());
                                }
                            }
                        }
                    }
                });
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
}