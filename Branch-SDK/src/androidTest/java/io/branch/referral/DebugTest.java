package io.branch.referral;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class DebugTest extends BranchTest {

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
