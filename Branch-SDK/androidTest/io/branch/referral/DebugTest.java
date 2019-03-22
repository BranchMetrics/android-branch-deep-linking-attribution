package io.branch.referral;

import android.support.test.runner.AndroidJUnit4;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class DebugTest extends BranchTest {
    /*
    DebugMode         TestMode          isTestModeEnabled     isDebugModeEnabled
       0                  0                false                 false
       0                  1                true                  true
       1                  0                false                 true
       1                  1                true                  true
    */

    @Test
    public void testDebugState() {
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

    @Test
    public void testTestModeA() {
        // Test Mode should be off to start
        Assert.assertFalse(BranchUtil.isTestModeEnabled());

        // Test Mode should still be off after this check (check has a side effect)
        Assert.assertFalse(BranchUtil.checkTestMode(getTestContext()));

        // Enable Test Mode after check
        Branch.enableTestMode();
        Assert.assertTrue(BranchUtil.isTestModeEnabled());

        // Assert that checking for test mode is also now true
        Assert.assertTrue(BranchUtil.checkTestMode(getTestContext()));
    }

    @Test
    public void testTestModeB() {
        // Test Mode should be off to start
        Assert.assertFalse(BranchUtil.isTestModeEnabled());

        // Enable Test Mode before check
        Branch.enableTestMode();
        Assert.assertTrue(BranchUtil.isTestModeEnabled());

        // Test Mode should still be on after this check (check has a side effect)
        Assert.assertTrue(BranchUtil.checkTestMode(getTestContext()));

        // Assert that this test is also still true
        Assert.assertTrue(BranchUtil.isTestModeEnabled());

        // Now turn off test mode and check
        Branch.disableTestMode();

        Assert.assertFalse(BranchUtil.isTestModeEnabled());
        Assert.assertFalse(BranchUtil.checkTestMode(getTestContext()));
    }
}
