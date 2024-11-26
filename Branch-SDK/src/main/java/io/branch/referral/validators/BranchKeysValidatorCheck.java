package io.branch.referral.validators;

import static io.branch.referral.validators.IntegrationValidatorConstants.branchKeysMoreInfoDocsLink;

import android.content.Context;
import android.text.TextUtils;

import io.branch.referral.BranchUtil;

public class BranchKeysValidatorCheck extends IntegrationValidatorCheck {

    String name = "Branch Keys";
    String errorMessage = "Unable to read Branch keys from your application. Did you forget to add Branch keys in your application?.";
    String moreInfoLink = branchKeysMoreInfoDocsLink;

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
