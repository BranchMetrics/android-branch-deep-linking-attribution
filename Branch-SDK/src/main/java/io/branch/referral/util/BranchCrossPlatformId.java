package io.branch.referral.util;

import io.branch.referral.BranchError;

public class BranchCrossPlatformId {

    // BranchCrossPlatformIdListener is duplicated for backwards compatibility (fixes SDK-786)

    public interface BranchCrossPlatformIdListener {
        void onDataFetched(BranchCPID branchCPID, BranchError error);
    }
}
