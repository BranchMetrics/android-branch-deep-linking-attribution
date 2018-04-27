package io.branch.uitestbed.test.controls;

import android.content.Context;

import io.branch.referral.Branch;
import io.branch.referral.BranchError;
import io.branch.uitestbed.test.data.MockDataObjects;

public class LinkBuilderButton extends BasicButton implements Branch.BranchLinkCreateListener {
    public LinkBuilderButton(Context context, ITestEvents testEvents) {
        super(context, testEvents);
    }

    @Override
    public void onClicked() {
        MockDataObjects.buo.generateShortUrl(getContext(), MockDataObjects.linkProperties, this);
    }

    @Override
    public String getDisplayName() {
        return "Create Short Link";
    }

    @Override
    public String getTestDescription() {
        return "--- Testing Branch link creation ----";
    }

    @Override
    public void onLinkCreate(String url, BranchError error) {
        onActionResponse();
    }
}
