package io.branch.referral.validators;

import android.content.Context;
import android.text.TextUtils;

import io.branch.referral.BranchUtil;

public class BranchKeysValidatorCheck extends IntegrationValidatorCheck {

    String name = "Branch Keys";
    String errorMessage = "Unable to read Branch keys from your application. Did you forget to add Branch keys in your application?.";
    String moreInfoLink = "<a href=\"https://help.branch.io/developers-hub/docs/android-basic-integration#:~:text=%3C!%2D%2D%20REPLACE%20%60BranchKey%60%20with,%22key_test_XXX%22%20/%3E\">More info</a>";

    public BranchKeysValidatorCheck() {
        super.name = name;
        super.errorMessage = errorMessage;
        super.moreInfoLink = moreInfoLink;
    }

    @Override
    public boolean RunTests(Context context) {
        return !TextUtils.isEmpty(BranchUtil.readBranchKey(context));
    }

    @Override
    public String GetOutput(Context context, boolean didTestSucceed) {
        didTestSucceed = RunTests(context);
        return super.GetOutput(context, didTestSucceed);
    }
}
