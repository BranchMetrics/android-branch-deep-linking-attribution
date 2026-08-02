package io.branch.referral;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/**
 * EMT-3863: 5.x source implemented the LATD callback via the nested
 * ServerRequestGetLATD.BranchLastAttributedTouchDataListener. The beta moved it to
 * Branch.BranchLastAttributedTouchDataListener with no alias at the old location, so that source
 * stopped compiling on upgrade (reported by Ana, 5.16.3 -> beta). This pins source compatibility:
 * the old nested type still resolves, is usable exactly as Ana wrote it, and is @Deprecated.
 *
 * The anonymous implementation below is the compile-time proof — it only builds when the alias
 * exists, which is precisely the acceptance criterion.
 */
@RunWith(RobolectricTestRunner.class)
public class LatdListenerApiCompatTest {

    @Test
    @SuppressWarnings("deprecation")
    public void oldNestedListenerIsSourceCompatible() {
        // Ana's 5.x code, verbatim shape.
        ServerRequestGetLATD.BranchLastAttributedTouchDataListener legacyListener =
                new ServerRequestGetLATD.BranchLastAttributedTouchDataListener() {
                    @Override
                    public void onDataFetched(JSONObject jsonObject, BranchError error) {
                        // no-op
                    }
                };
        // The alias must satisfy the relocated callsite type with no cast.
        Branch.BranchLastAttributedTouchDataListener asNewType = legacyListener;
        assertNotNull(asNewType);
    }

    @Test
    public void oldNestedListenerIsDeprecated() {
        assertTrue("the restored alias must be @Deprecated so upgraders are nudged to the new type",
                ServerRequestGetLATD.BranchLastAttributedTouchDataListener.class
                        .isAnnotationPresent(Deprecated.class));
    }
}
