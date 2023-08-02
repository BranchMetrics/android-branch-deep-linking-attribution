package io.branch.referral;

import android.content.Context;

import androidx.annotation.NonNull;

import com.miui.referrer.api.GetAppsReferrerDetails;
import io.branch.coroutines.InstallReferrersKt;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;

public class StoreReferrerXiaomiGetApps extends AppStoreReferrer{
    static boolean hasBeenUsed = false;
    static boolean erroredOut = false;

    static Long clickTimestamp = Long.MIN_VALUE;
    static Long installBeginTimestamp = Long.MIN_VALUE;
    static String rawReferrer = null;

    public static void fetch(final Context context) {
        hasBeenUsed = true;

        InstallReferrersKt.getXiaomiGetAppsReferrerDetails(context, new Continuation<GetAppsReferrerDetails>() {
            @NonNull
            @Override
            public CoroutineContext getContext() {
                return EmptyCoroutineContext.INSTANCE;
            }

            @Override
            public void resumeWith(@NonNull Object o) {
                try {
                    GetAppsReferrerDetails referrerDetails = (GetAppsReferrerDetails) o;
                    rawReferrer = referrerDetails.getInstallReferrer();
                    clickTimestamp = referrerDetails.getReferrerClickTimestampSeconds();
                    installBeginTimestamp = referrerDetails.getInstallBeginTimestampSeconds();
                }
                catch (Exception e) {
                    PrefHelper.Debug(e.getMessage());
                    erroredOut = true;
                }
                finally {
                    Branch.getInstance().requestQueue_.unlockProcessWait(ServerRequest.PROCESS_WAIT_LOCK.XIAOMI_INSTALL_REFERRER_FETCH_WAIT_LOCK);
                    Branch.getInstance().waitingForXiaomiInstallReferrer = false;
                    Branch.getInstance().tryProcessNextQueueItemAfterInstallReferrer();
                }
            }
        });
    }
}
