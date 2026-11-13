package io.branch.referral;

import static io.branch.referral.Defines.RequestPath.RegisterInstall;
import static io.branch.referral.Defines.RequestPath.RegisterOpen;

import android.os.SystemClock;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

import io.branch.referral.Defines.PreinstallKey;
import io.branch.referral.test.mock.MockRemoteInterface;
import io.branch.referral.test.mock.MockSamsungMapsProvider;

@RunWith(AndroidJUnit4.class)
public class SamsungMapsPreinstallInstrumentedTest extends BranchTest {
    private static final String APP_TRACKING_ID = "ABC123DEF456";

    private final CapturingRemoteInterface remoteInterface = new CapturingRemoteInterface();

    @Before
    public void resetProvider() {
        MockSamsungMapsProvider.reset();
    }

    @After
    public void releaseProvider() {
        MockSamsungMapsProvider.reset();
    }

    @Test
    public void installCarriesSamsungMapsPartnerAndMapsIdCampaign() {
        initBranchInstance();
        branch.setSamsungMapsAppTrackingId(APP_TRACKING_ID);
        branch.setBranchRemoteInterface(remoteInterface);

        initSessionResumeActivity(null, null);

        Assert.assertEquals(1, MockSamsungMapsProvider.queryCount.get());
        Assert.assertEquals(getTestContext().getPackageName(), MockSamsungMapsProvider.lastSelection);
        Assert.assertArrayEquals(new String[]{APP_TRACKING_ID}, MockSamsungMapsProvider.lastSelectionArgs);

        Assert.assertEquals(1, remoteInterface.installPayloads.size());
        JSONObject install = remoteInterface.installPayloads.get(0);
        Assert.assertEquals(BranchPreinstall.SAMSUNG_MAPS_PREINSTALL_PARTNER, install.optString(PreinstallKey.partner.getKey()));
        Assert.assertEquals("12345", install.optString(PreinstallKey.campaign.getKey()));
    }

    @Test
    public void blockedProviderDoesNotHoldInstallPastTheLimit() {
        MockSamsungMapsProvider.block = new CountDownLatch(1);
        initBranchInstance();
        branch.setInstallReferrerTimeout(1000);
        branch.setSamsungMapsAppTrackingId(APP_TRACKING_ID);
        branch.setBranchRemoteInterface(remoteInterface);

        long start = SystemClock.elapsedRealtime();
        initSessionResumeActivity(null, null);

        Assert.assertEquals(1, remoteInterface.installPayloads.size());
        long waited = remoteInterface.installSentAt - start;
        // The provider blocks for 30 s; the 1 s limit plus init overhead must stay well under that.
        Assert.assertTrue("install was held " + waited + " ms", waited < 5000);
        JSONObject install = remoteInterface.installPayloads.get(0);
        Assert.assertFalse(install.has(PreinstallKey.partner.getKey()));
        Assert.assertFalse(install.has(PreinstallKey.campaign.getKey()));
    }

    @Test
    public void openDoesNotQuerySamsungMaps() {
        initBranchInstance();
        PrefHelper.getInstance(getTestContext()).setRandomizedBundleToken("880938553226608667");
        branch.setSamsungMapsAppTrackingId(APP_TRACKING_ID);
        branch.setBranchRemoteInterface(remoteInterface);

        initSessionResumeActivity(null, null);

        Assert.assertEquals(0, remoteInterface.installPayloads.size());
        Assert.assertEquals(1, remoteInterface.openPayloads.size());
        Assert.assertEquals(0, MockSamsungMapsProvider.queryCount.get());
        Assert.assertFalse(remoteInterface.openPayloads.get(0).has(PreinstallKey.partner.getKey()));
    }

    private static class CapturingRemoteInterface extends MockRemoteInterface {
        final List<JSONObject> installPayloads = new CopyOnWriteArrayList<>();
        final List<JSONObject> openPayloads = new CopyOnWriteArrayList<>();
        volatile long installSentAt = 0;

        @Override
        public BranchResponse doRestfulPost(String url, JSONObject payload) throws BranchRemoteException {
            if (url.contains(RegisterInstall.getPath())) {
                installSentAt = SystemClock.elapsedRealtime();
                installPayloads.add(payload);
            } else if (url.contains(RegisterOpen.getPath())) {
                openPayloads.add(payload);
            }
            return super.doRestfulPost(url, payload);
        }
    }
}
