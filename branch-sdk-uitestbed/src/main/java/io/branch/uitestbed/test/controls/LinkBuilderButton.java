package io.branch.uitestbed.test.controls;

import android.content.Context;
import android.text.TextUtils;

import io.branch.referral.Branch;
import io.branch.referral.BranchError;
import io.branch.uitestbed.test.data.MockDataObjects;
import io.branch.uitestbed.test.data.TestResponse;

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
        if (error != null) {
            onActionResponse(TestResponse.getFailedResponse(error));
        } else if (TextUtils.isEmpty(url)) {
            onActionResponse(TestResponse.getFailedResponseUnknownError("URL creation failed, Returned URL is empty"));
        }
        onActionResponse(TestResponse.getSuccessResponse());
    }
}
