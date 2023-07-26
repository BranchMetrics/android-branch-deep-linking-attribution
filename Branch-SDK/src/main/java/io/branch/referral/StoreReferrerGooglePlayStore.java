package io.branch.referral;

import android.content.Context;
import android.os.RemoteException;

import androidx.annotation.NonNull;

import com.android.installreferrer.api.InstallReferrerClient;
import com.android.installreferrer.api.InstallReferrerStateListener;
import com.android.installreferrer.api.ReferrerDetails;

import java.util.Timer;
import java.util.TimerTask;

import io.branch.coroutines.DataCoroutinesKt;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;

public class StoreReferrerGooglePlayStore extends AppStoreReferrer{
    static boolean hasBeenUsed = false;
    static boolean erroredOut = false;

    static Long clickTimestamp = Long.MIN_VALUE;
    static Long installBeginTimestamp = Long.MIN_VALUE;
    static String rawReferrer = null;

    public static void fetch(final Context context) {
        hasBeenUsed = true;

        DataCoroutinesKt.getGooglePlayStoreReferrerDetails(context, new Continuation<ReferrerDetails>() {
            @NonNull
            @Override
            public CoroutineContext getContext() {
                return EmptyCoroutineContext.INSTANCE;
            }

            @Override
            public void resumeWith(Object o) {
                if (o != null) {
                    ReferrerDetails referrerDetails = (ReferrerDetails) o;

                    try {
                        rawReferrer = referrerDetails.getInstallReferrer();
                        clickTimestamp = referrerDetails.getReferrerClickTimestampSeconds();
                        installBeginTimestamp = referrerDetails.getInstallBeginTimestampSeconds();
                    }
                    catch (Exception e) {
                        PrefHelper.Debug(e.getMessage());
                        erroredOut = true;
                    }
                    finally {
                        Branch.getInstance().requestQueue_.unlockProcessWait(ServerRequest.PROCESS_WAIT_LOCK.GOOGLE_INSTALL_REFERRER_FETCH_WAIT_LOCK);
                        Branch.getInstance().waitingForGoogleInstallReferrer = false;
                        Branch.getInstance().tryProcessNextQueueItemAfterInstallReferrer();
                    }
                }
            }
        });
    }
}
