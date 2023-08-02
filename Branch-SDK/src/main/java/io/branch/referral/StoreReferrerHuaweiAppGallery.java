package io.branch.referral;

import android.content.Context;

import androidx.annotation.NonNull;

import com.huawei.hms.ads.installreferrer.api.ReferrerDetails;

import io.branch.coroutines.InstallReferrersKt;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;

public class StoreReferrerHuaweiAppGallery extends AppStoreReferrer {
    static boolean hasBeenUsed = false;
    static boolean erroredOut = false;

    static long clickTimestamp = Long.MIN_VALUE;
    static long installBeginTimestamp = Long.MIN_VALUE;
    static String rawReferrer = null;

    public static void fetch(final Context context) {
        hasBeenUsed = true;

        InstallReferrersKt.getHuaweiAppGalleryReferrerDetails(context, new Continuation<ReferrerDetails>() {
            @NonNull
            @Override
            public CoroutineContext getContext() {
                return EmptyCoroutineContext.INSTANCE;
            }

            @Override
            public void resumeWith(@NonNull Object o) {
                try {
                    ReferrerDetails referrerDetails = (ReferrerDetails) o;
                    rawReferrer = referrerDetails.getInstallReferrer();
                    clickTimestamp = referrerDetails.getReferrerClickTimestampSeconds();
                    installBeginTimestamp = referrerDetails.getInstallBeginTimestampSeconds();
                }
                catch (Exception e) {
                    PrefHelper.Debug(e.getMessage());
                    erroredOut = true;
                }
                finally {
                    Branch.getInstance().requestQueue_.unlockProcessWait(ServerRequest.PROCESS_WAIT_LOCK.HUAWEI_INSTALL_REFERRER_FETCH_WAIT_LOCK);
                    Branch.getInstance().waitingForHuaweiInstallReferrer = false;
                    Branch.getInstance().tryProcessNextQueueItemAfterInstallReferrer();
                }
            }
        });
    }
}
