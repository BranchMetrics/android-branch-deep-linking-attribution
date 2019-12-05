package io.branch.referral;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import io.branch.referral.Defines.RequestPath;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Created by --vbajpai on --2019-09-17 at --10:44 for --android-branch-deep-linking-attribution
 */

@RunWith(AndroidJUnit4.class)
public class BranchCPIDTest extends BranchEventTest {

    @Test
    public void testGetCPID() throws Throwable{
        Branch.getInstance(getTestContext()).getCrossPlatformIds(null);

        ServerRequestQueue queue = ServerRequestQueue.getInstance(getTestContext());
        Assert.assertEquals(1, queue.getSize());

        ServerRequest cpidRequest = queue.peekAt(0);

        Assert.assertEquals(cpidRequest.getRequestPath(), RequestPath.GetCPID.getPath());
    }

    @Test
    public void testGetLATD() throws Throwable{
        Branch.getInstance(getTestContext()).getLastAttributedTouchData(null);

        ServerRequestQueue queue = ServerRequestQueue.getInstance(getTestContext());
        Assert.assertEquals(1, queue.getSize());

        ServerRequest cpidRequest = queue.peekAt(0);

        Assert.assertEquals(cpidRequest.getRequestPath(), RequestPath.GetLATD.getPath());
    }

    @Test
    public void testGetLATDAttributionWindowDefault() throws Throwable {
        Branch branch = Branch.getInstance(getTestContext());
        PrefHelper prefHelper = PrefHelper.getInstance(getTestContext());

        // Defaults to ServerRequestGetLATD.defaultAttributionWindow
        int defValue = prefHelper.getLATDAttributionWindow();
        Assert.assertEquals(ServerRequestGetLATD.defaultAttributionWindow, defValue);

        // After request with custom attribution window, still defaults to ServerRequestGetLATD.defaultAttributionWindow
        branch.getLastAttributedTouchData(null, 80);
        int prefValueA = prefHelper.getLATDAttributionWindow();
        Assert.assertEquals(ServerRequestGetLATD.defaultAttributionWindow, prefValueA);
    }

    @Test
    public void testGetLATDAttributionWindowSetting() throws Throwable {
        //setup
        Branch branch = Branch.getInstance(getTestContext());
        PrefHelper prefHelper = PrefHelper.getInstance(getTestContext());

        // Defaults to ServerRequestGetLATD.defaultAttributionWindow
        int defValue = prefHelper.getLATDAttributionWindow();
        Assert.assertEquals(ServerRequestGetLATD.defaultAttributionWindow, defValue);

        // Setting new attribution window to 10
        prefHelper.setLATDAttributionWindow(10);
        int newValue = prefHelper.getLATDAttributionWindow();
        Assert.assertEquals(10, newValue);

        // make request, get its post body, check that the attribution window equals 10
        branch.getLastAttributedTouchData(null);
        ServerRequestQueue queue = ServerRequestQueue.getInstance(getTestContext());
        Assert.assertEquals(1, queue.getSize());

        ServerRequest latdRequest = queue.peekAt(0);
        Assert.assertTrue(latdRequest instanceof ServerRequestGetLATD);

        JSONObject postBody = latdRequest.getPost();
        Assert.assertNotNull(postBody);
        JSONObject userData = postBody.optJSONObject(Defines.Jsonkey.UserData.getKey());
        Assert.assertNotNull(userData);

        int atrWind = userData.optInt(Defines.Jsonkey.LATDAttributionWindow.getKey());
        Assert.assertEquals(10, atrWind);
    }
}
