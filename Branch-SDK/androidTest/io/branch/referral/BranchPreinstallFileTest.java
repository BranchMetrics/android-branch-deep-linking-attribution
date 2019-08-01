package io.branch.referral;

import android.support.test.runner.AndroidJUnit4;
import io.branch.referral.Defines.PreinstallKey;
import io.branch.referral.utils.AssetUtils;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class BranchPreinstallFileTest extends BranchEventTest {

    @Test
    public void testResultSuccess() throws Throwable {
        Branch branch = Branch.getInstance(getTestContext());
        initQueue(getTestContext());

        ServerRequestQueue queue = ServerRequestQueue.getInstance(getTestContext());
        Assert.assertEquals(1, queue.getSize());

        String branchFileData = AssetUtils
                .readJsonFile(getTestContext(), "io/assets/pre_install_apps.branch");
        Assert.assertTrue(branchFileData.length() > 0);

        JSONObject branchFileJson = new JSONObject(branchFileData);
        BranchPreinstall.getBranchFileContent(branchFileJson, branch,
                getTestContext());

        ServerRequest initRequest = queue.peekAt(0);
        doFinalUpdate(initRequest);
        doFinalUpdateOnMainThread(initRequest);

        Assert.assertTrue(hasV1InstallPreinstallCampaign(initRequest));
        Assert.assertTrue(hasV1InstallPreinstallPartner(initRequest));
        Assert.assertTrue(hasV1InstallPreinstallCustomData(initRequest));
    }

    @Test
    public void testResultNullFile() throws Throwable {
        String branchFileData = AssetUtils
                .readJsonFile(getTestContext(), "io/assets/pre_install_apps_null.branch");
        Assert.assertFalse(branchFileData.length() > 0);
    }

    @Test
    public void testResultPackageNameNotPresent() throws Throwable {
        Branch branch = Branch.getInstance(getTestContext());
        initQueue(getTestContext());

        ServerRequestQueue queue = ServerRequestQueue.getInstance(getTestContext());
        Assert.assertEquals(1, queue.getSize());

        String branchFileData = AssetUtils
                .readJsonFile(getTestContext(), "io/assets/pre_install_apps_no_package.branch");
        Assert.assertTrue(branchFileData.length() > 0);

        JSONObject branchFileJson = new JSONObject(branchFileData);
        BranchPreinstall.getBranchFileContent(branchFileJson, branch,
                getTestContext());

        ServerRequest initRequest = queue.peekAt(0);
        doFinalUpdate(initRequest);
        doFinalUpdateOnMainThread(initRequest);

        Assert.assertFalse(hasV1InstallPreinstallCampaign(initRequest));
        Assert.assertFalse(hasV1InstallPreinstallPartner(initRequest));
        Assert.assertFalse(hasV1InstallPreinstallCustomData(initRequest));
    }

    @Test
    public void testResultFileNotPresent() throws Throwable {
        String branchFileData = AssetUtils
                .readJsonFile(getTestContext(), "io/assets/pre_install_apps_not_present.branch");
        Assert.assertFalse(branchFileData.length() > 0);
    }

    // Check to see if the preinstall campaign is available (V1)
    private boolean hasV1InstallPreinstallCampaign(ServerRequest request) {
        JSONObject jsonObject = request.getGetParams();
        String preinstallCampaign = jsonObject.optString(PreinstallKey.partner.getKey());
        return (preinstallCampaign.length() > 0);
    }

    // Check to see if the preinstall partner is available (V1)
    private boolean hasV1InstallPreinstallPartner(ServerRequest request) {
        JSONObject jsonObject = request.getGetParams();
        String preinstallPartner = jsonObject.optString(PreinstallKey.partner.getKey());
        return (preinstallPartner.length() > 0);
    }

    // Check to see if the preinstall custom data is available (V1)
    private boolean hasV1InstallPreinstallCustomData(ServerRequest request) throws Throwable {
        JSONObject jsonObject = request.getGetParams().getJSONObject("metadata");
        String custom_key = jsonObject.optString("custom_key");
        return (custom_key.length() > 0);
    }
}
