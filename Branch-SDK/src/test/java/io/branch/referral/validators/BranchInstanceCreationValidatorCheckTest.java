package io.branch.referral.validators;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import io.branch.referral.Branch;
import io.branch.referral.BranchConfiguration;

/**
 * EMT-4471 regression test: BranchInstanceCreationValidatorCheck must reflect the new
 * architecture, where Branch.getInstance() only returns non-null after
 * Branch.initialize(context, config) has run, and its remediation text must point at that
 * entry point rather than the removed "Branch.getInstance();"-in-onCreate() pattern.
 */
@RunWith(RobolectricTestRunner.class)
public class BranchInstanceCreationValidatorCheckTest {

    @Test
    public void runTests_afterInitialize_returnsTrue() {
        Context context = RuntimeEnvironment.getApplication();
        Branch.initialize(context, new BranchConfiguration.Builder("key_live_test123").build());

        assertTrue(new BranchInstanceCreationValidatorCheck().RunTests(context));
    }

    @Test
    public void errorMessage_referencesInitializeNotGetInstance() {
        String message = new BranchInstanceCreationValidatorCheck().errorMessage;

        assertFalse("stale remediation text must be gone",
                message.contains("Branch.getInstance();"));
        assertTrue("remediation text must point at the new entry point",
                message.contains("Branch.initialize(context, config)"));
    }
}
