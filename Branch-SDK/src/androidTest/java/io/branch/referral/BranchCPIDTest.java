package io.branch.referral;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import io.branch.referral.Defines.RequestPath;
import io.branch.referral.util.BranchCrossPlatformId;
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
        Branch branch = Branch.getInstance(getTestContext());

        new BranchCrossPlatformId(null, getTestContext());

        ServerRequestQueue queue = ServerRequestQueue.getInstance(getTestContext());
        Assert.assertEquals(1, queue.getSize());

        ServerRequest cpidRequest = queue.peekAt(0);

        Assert.assertEquals(cpidRequest.getRequestPath(), RequestPath.GetCPID.getPath());
    }

    @Test
    public void testGetLATD() throws Throwable{
        Branch branch = Branch.getInstance(getTestContext());
        if (branch != null) {
            branch.getLastAttributedTouchData(null);
        }

        ServerRequestQueue queue = ServerRequestQueue.getInstance(getTestContext());
        Assert.assertEquals(1, queue.getSize());

        ServerRequest cpidRequest = queue.peekAt(0);

        Assert.assertEquals(cpidRequest.getRequestPath(), RequestPath.GetLATD.getPath());
    }

}
