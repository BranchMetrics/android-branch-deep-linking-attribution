package io.branch.referral.validators;

import static org.junit.Assert.assertEquals;

import android.content.Context;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import io.branch.referral.PrefHelper;

/**
 * EMT-4471 regression test: BranchIntegrationModel must source its Branch key from
 * PrefHelper (the value Branch.initialize(context, config) writes), not AndroidManifest
 * meta-data, which is the pre-6.0 behavior this ticket removes.
 */
@RunWith(RobolectricTestRunner.class)
public class BranchIntegrationModelTest {

    @Test
    public void constructor_sourcesBranchKeyFromPrefHelper() {
        Context context = RuntimeEnvironment.getApplication();
        PrefHelper.getInstance(context).setBranchKey("key_live_from_config_block");

        BranchIntegrationModel model = new BranchIntegrationModel(context);

        assertEquals("key_live_from_config_block", model.branchKey);
    }

    @Test
    public void constructor_reflectsBranchKeyChangesAcrossInstances() {
        Context context = RuntimeEnvironment.getApplication();

        PrefHelper.getInstance(context).setBranchKey("key_live_first");
        assertEquals("key_live_first", new BranchIntegrationModel(context).branchKey);

        PrefHelper.getInstance(context).setBranchKey("key_live_second");
        assertEquals("key_live_second", new BranchIntegrationModel(context).branchKey);
    }
}
