package io.branch.referral;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.app.Activity;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.lang.reflect.Method;

import io.branch.indexing.BranchUniversalObject;
import io.branch.referral.util.LinkProperties;
import io.branch.referral.util.ShareSheetStyle;

/**
 * EMT-3881: the public BranchUniversalObject.showShareSheet() overload that 5.x exposed was removed
 * in the beta, so integrations that presented the share sheet directly from a BranchUniversalObject
 * no longer compile. These checks pin the restored public contract: the overload exists again,
 * returns void, and is marked @Deprecated so the IDE steers upgraders toward the native
 * Branch.share(...) replacement.
 */
@RunWith(RobolectricTestRunner.class)
public class ShowShareSheetApiCompatTest {

    @Test
    public void showShareSheetOverloadExists() throws NoSuchMethodException {
        Method showShareSheet = BranchUniversalObject.class.getMethod(
                "showShareSheet",
                Activity.class,
                LinkProperties.class,
                ShareSheetStyle.class,
                Branch.BranchLinkShareListener.class);
        assertEquals("showShareSheet(...) must return void", void.class, showShareSheet.getReturnType());
    }

    @Test
    public void showShareSheetOverloadIsDeprecated() throws NoSuchMethodException {
        Method showShareSheet = BranchUniversalObject.class.getMethod(
                "showShareSheet",
                Activity.class,
                LinkProperties.class,
                ShareSheetStyle.class,
                Branch.BranchLinkShareListener.class);
        assertTrue("showShareSheet(...) must be @Deprecated to guide migration to Branch.share(...)",
                showShareSheet.isAnnotationPresent(Deprecated.class));
    }

    @Test
    public void nativeShareReplacementExists() throws NoSuchMethodException {
        // The documented replacement showShareSheet delegates to.
        Method share = Branch.class.getMethod(
                "share",
                Activity.class,
                BranchUniversalObject.class,
                LinkProperties.class,
                Branch.BranchNativeLinkShareListener.class,
                String.class,
                String.class);
        assertEquals("Branch.share(...) replacement must return void", void.class, share.getReturnType());
    }
}
