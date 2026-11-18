package io.branch.referral;

import static io.branch.referral.Defines.RequestPath.RegisterInstall;

import android.content.Context;
import android.os.SystemClock;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import io.branch.referral.test.mock.MockRemoteInterface;

@RunWith(AndroidJUnit4.class)
public class InstallAdvertisingIdWaitTest extends BranchTest {
    private static final String TEST_AID = "11111111-2222-3333-4444-555555555555";
    private static final long AD_ID_DELAY_MS = 1000;

    @Test
    public void installWaitsForAdvertisingIdWhenReferrerFetchReturnsAtOnce() throws Exception {
        initBranchInstance();
        CapturingRemoteInterface remoteInterface = new CapturingRemoteInterface();
        branch.setBranchRemoteInterface(remoteInterface);
        replaceSystemObserver(new InlineReferrerSlowAdIdObserver());

        initSessionResumeActivity(null, null);

        Assert.assertEquals(1, remoteInterface.installPayloads.size());
        JSONObject install = remoteInterface.installPayloads.get(0);
        JSONObject advertisingIds = install.optJSONObject(Defines.Jsonkey.AdvertisingIDs.getKey());
        Assert.assertNotNull("install was sent without advertising_ids: " + install, advertisingIds);
        Assert.assertEquals(TEST_AID, advertisingIds.getString(Defines.Jsonkey.AAID.getKey()));
    }

    // DeviceInfo creates its SystemObserver itself, so the test swaps it in place.
    private static void replaceSystemObserver(SystemObserver observer) throws Exception {
        Field field = DeviceInfo.class.getDeclaredField("systemObserver_");
        field.setAccessible(true);
        field.set(DeviceInfo.getInstance(), observer);
    }

    /**
     * Returns the install referrer at once, on the calling thread, and the advertising ID a second later.
     */
    private static class InlineReferrerSlowAdIdObserver extends SystemObserver {
        @Override
        public void fetchInstallReferrer(Context context, InstallReferrerFetchEvents callback) {
            callback.onInstallReferrersFinished();
        }

        @Override
        public void fetchAdId(Context context, AdsParamsFetchEvents callback) {
            new Thread(() -> {
                SystemClock.sleep(AD_ID_DELAY_MS);
                setLAT(0);
                setGAID(TEST_AID);
                callback.onAdsParamsFetchFinished();
            }).start();
        }
    }

    private static class CapturingRemoteInterface extends MockRemoteInterface {
        final List<JSONObject> installPayloads = new CopyOnWriteArrayList<>();

        @Override
        public BranchResponse doRestfulPost(String url, JSONObject payload) throws BranchRemoteException {
            if (url.contains(RegisterInstall.getPath())) {
                installPayloads.add(payload);
            }
            return super.doRestfulPost(url, payload);
        }
    }
}
