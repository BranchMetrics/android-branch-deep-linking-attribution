package io.branch.referral.util;

import org.json.JSONObject;

import io.branch.referral.BranchError;

public class BranchLastAttributedTouchData {

    // BranchLastAttributedTouchDataListener is duplicated for backwards compatibility (fixes SDK-786)

    public interface BranchLastAttributedTouchDataListener {
        void onDataFetched(JSONObject jsonObject, BranchError error);
    }
}
