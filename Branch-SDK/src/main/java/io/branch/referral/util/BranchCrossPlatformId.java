package io.branch.referral.util;

import io.branch.referral.BranchError;

public class BranchCrossPlatformId {

    // this may look like stupid design but it's an attempt to hotfix a bug,
    // when we released v4.2.1, we changed the BranchCrossPlatformIdListener signature from
    // from io.branch.referral.util.BranchCrossPlatformId.BranchCrossPlatformIdListener
    // to io.branch.referral.ServerRequestGetCPID.BranchCrossPlatformIdListener
    // and plugins that dynamically pick up the latest SDK version started breaking, so now we have
    // duplicated BranchCrossPlatformIdListener classes

    public interface BranchCrossPlatformIdListener {
        void onDataFetched(BranchCPID branchCPID, BranchError error);
    }
}
