package io.branch.referral;

import android.content.pm.PackageManager;
import android.os.Build;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.ConcurrentHashMap;

import io.branch.referral.test.mock.MockRemoteInterface;

@RunWith(AndroidJUnit4.class)
public class InstallerAppStoreTest extends BranchTest {

    private final ConcurrentHashMap<String, JSONObject> sentPayloads = new ConcurrentHashMap<>();

    @Test
    public void installSendsInstallerAppStore() {
        initBranchInstance();
        branch.setBranchRemoteInterface(new RecordingRemoteInterface());

        initSessionResumeActivity(null, null);

        assertAppStore(Defines.RequestPath.RegisterInstall);
    }

    @Test
    public void openSendsInstallerAppStore() {
        initBranchInstance();
        PrefHelper.getInstance(getTestContext()).setRandomizedBundleToken("880938553226608667");
        branch.setBranchRemoteInterface(new RecordingRemoteInterface());

        initSessionResumeActivity(null, null);

        assertAppStore(Defines.RequestPath.RegisterOpen);
    }

    @Test
    public void installAfterReferrerTimeoutSendsInstallerAppStore() {
        initBranchInstance();
        branch.setInstallReferrerTimeout(1);
        branch.setBranchRemoteInterface(new RecordingRemoteInterface());

        initSessionResumeActivity(null, null);

        assertAppStore(Defines.RequestPath.RegisterInstall);
    }

    private void assertAppStore(Defines.RequestPath path) {
        JSONObject payload = null;
        for (String url : sentPayloads.keySet()) {
            if (url.contains(path.getPath())) {
                payload = sentPayloads.get(url);
            }
        }
        Assert.assertNotNull(path.getPath() + " was not sent", payload);

        String expected = expectedAppStore();
        if (expected == null) {
            Assert.assertFalse(payload.has(Defines.Jsonkey.App_Store.getKey()));
        } else {
            Assert.assertEquals(expected, payload.optString(Defines.Jsonkey.App_Store.getKey()));
        }
    }

    // Separate copy of the installer mapping, so a change to the SDK's table fails this test.
    private String expectedAppStore() {
        String installer;
        try {
            PackageManager packageManager = getTestContext().getPackageManager();
            String packageName = getTestContext().getPackageName();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                installer = packageManager.getInstallSourceInfo(packageName).getInstallingPackageName();
            } else {
                installer = packageManager.getInstallerPackageName(packageName);
            }
        } catch (Exception e) {
            installer = null;
        }

        if (installer == null) {
            return null;
        }
        switch (installer) {
            case "com.android.vending":
                return "PlayStore";
            case "com.huawei.appmarket":
                return "AppGallery";
            case "com.sec.android.app.samsungapps":
                return "GalaxyStore";
            case "com.xiaomi.mipicks":
            case "com.xiaomi.market":
                return "GetApps";
            default:
                return null;
        }
    }

    private class RecordingRemoteInterface extends MockRemoteInterface {
        @Override
        public BranchResponse doRestfulPost(String url, JSONObject payload) throws BranchRemoteException {
            sentPayloads.put(url, payload);
            return super.doRestfulPost(url, payload);
        }
    }
}
