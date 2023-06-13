package io.branch.referral;

import org.json.JSONObject;

import io.branch.indexing.BranchUniversalObject;
import io.branch.referral.util.LinkProperties;

/**
 * Class for converting init session callback with {@link BranchReferralInitListener} to {@link BranchUniversalReferralInitListener}
 */
class BranchUniversalReferralInitWrapper implements BranchReferralInitListener {
    private final BranchUniversalReferralInitListener universalReferralInitListener_;

    public BranchUniversalReferralInitWrapper(BranchUniversalReferralInitListener universalReferralInitListener) {
        this.universalReferralInitListener_ = universalReferralInitListener;
    }

    @Override
    public void onInitFinished(JSONObject referringParams, BranchError error) {
        if (universalReferralInitListener_ != null) {
            if (error != null) {
                universalReferralInitListener_.onInitFinished(null, null, error);
            } else {
                BranchUniversalObject branchUniversalObject = BranchUniversalObject.getReferredBranchUniversalObject();
                LinkProperties linkProperties = LinkProperties.getReferredLinkProperties();
                universalReferralInitListener_.onInitFinished(branchUniversalObject, linkProperties, error);
            }
        }
    }
}