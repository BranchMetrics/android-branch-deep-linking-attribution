package io.branch.referral.util;

import org.json.JSONObject;

import io.branch.referral.BranchError;

public class BranchLastAttributedTouchData {

    // this may look like stupid design but it's an attempt to hotfix a bug,
    // when we released v4.2.1, we changed the BranchLastAttributedTouchDataListener signature from
    // from io.branch.referral.util.BranchCrossPlatformId.BranchLastAttributedTouchDataListener
    // to io.branch.referral.ServerRequestGetLATD.BranchLastAttributedTouchDataListener
    // and plugins that dynamically pick up the latest SDK version started breaking, so now we have
    // duplicated BranchLastAttributedTouchDataListener classes

    public interface BranchLastAttributedTouchDataListener {
        void onDataFetched(JSONObject jsonObject, BranchError error);
    }
}
