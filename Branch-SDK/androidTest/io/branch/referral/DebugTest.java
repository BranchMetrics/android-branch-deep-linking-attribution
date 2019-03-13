package io.branch.referral;

import android.support.test.runner.AndroidJUnit4;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class DebugTest extends BranchTest {

    @Test
    public void testDebugState() {
        /*
        DebugMode         TestMode          isTestModeEnabled     isDebugModeEnabled
           0                  0                false                 false
           0                  1                true                  true
           1                  0                false                 true
           1                  1                true                  true
        */

        Assert.assertFalse(BranchUtil.isDebugEnabled());
        Assert.assertFalse(BranchUtil.isTestModeEnabled());

        // Debug Mode
        Branch.enableDebugMode();
        Assert.assertTrue(BranchUtil.isDebugEnabled());
        Assert.assertFalse(BranchUtil.isTestModeEnabled());

        Branch.disableDebugMode();
        Assert.assertFalse(BranchUtil.isDebugEnabled());
        Assert.assertFalse(BranchUtil.isTestModeEnabled());

        // Test Mode
        Branch.enableTestMode();
        Assert.assertTrue(BranchUtil.isDebugEnabled());
        Assert.assertTrue(BranchUtil.isTestModeEnabled());

        Branch.disableTestMode();
        Assert.assertFalse(BranchUtil.isDebugEnabled());
        Assert.assertFalse(BranchUtil.isTestModeEnabled());

        // Both Debug and Test Mode (variation A, debug before test)
        Branch.enableDebugMode();
        Branch.enableTestMode();
        Assert.assertTrue(BranchUtil.isDebugEnabled());
        Assert.assertTrue(BranchUtil.isTestModeEnabled());

        Branch.disableDebugMode();
        Assert.assertTrue(BranchUtil.isDebugEnabled());     // Per Javadoc, debug is enabled if either debug or test is enabled.
        Assert.assertTrue(BranchUtil.isTestModeEnabled());

        Branch.disableTestMode();
        Assert.assertFalse(BranchUtil.isDebugEnabled());
        Assert.assertFalse(BranchUtil.isTestModeEnabled());

        // Both Debug and Test Mode (variation B, test before debug)
        Branch.enableTestMode();
        Branch.enableDebugMode();
        Assert.assertTrue(BranchUtil.isDebugEnabled());
        Assert.assertTrue(BranchUtil.isTestModeEnabled());

        Branch.disableTestMode();
        Assert.assertFalse(BranchUtil.isTestModeEnabled());
        Assert.assertFalse(BranchUtil.isDebugEnabled());    // Per Javadoc, turning off test mode also turns off debug mode.
    }
}
