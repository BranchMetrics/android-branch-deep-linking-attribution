package io.branch.referral;

import android.support.test.runner.AndroidJUnit4;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class KeyTest extends BranchTest {

    @Test
    public void testManifestKeys() {
        Assert.assertFalse(BranchUtil.isDebugEnabled());
        Assert.assertFalse(BranchUtil.isTestModeEnabled());

        String branchKey = BranchUtil.readBranchKey(getTestContext());
        Assert.assertTrue(branchKey.startsWith("key_live"));

        Branch.enableTestMode();
        branchKey = BranchUtil.readBranchKey(getTestContext());
        Assert.assertTrue(branchKey.startsWith("key_test"));
    }

    @Test
    public void testAutoInstanceWithKey() {
        final String expectedKey = "key_XXX";
        Branch branch = Branch.getAutoInstance(getTestContext(), expectedKey);

        final String actualKey = PrefHelper.getInstance(getTestContext()).getBranchKey();
        Assert.assertEquals(expectedKey, actualKey);
    }
}
