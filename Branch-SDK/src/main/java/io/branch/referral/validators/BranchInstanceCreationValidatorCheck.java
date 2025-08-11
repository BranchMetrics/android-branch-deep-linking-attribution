package io.branch.referral.validators;

import static io.branch.referral.validators.IntegrationValidatorConstants.branchInstanceCreationMoreInfoDocsLink;

import android.content.Context;

import io.branch.referral.Branch;

public class BranchInstanceCreationValidatorCheck extends IntegrationValidatorCheck {

    String name = "Branch instance";
    String errorMessage = "Branch is not initialised from your Application class. Please add `Branch.getInstance();` to your Application#onCreate() method.";
    String moreInfoLink = branchInstanceCreationMoreInfoDocsLink;

    public BranchInstanceCreationValidatorCheck() {
        super.name = name;
        super.errorMessage = errorMessage;
        super.moreInfoLink = moreInfoLink;
    }

    @Override
    public boolean RunTests(Context context) {
        return Branch.getInstance() != null;
    }

    @Override
    public String GetOutput(Context context, boolean didTestSucceed) {
        didTestSucceed = RunTests(context);
        return super.GetOutput(context, didTestSucceed);
    }

    @Override
    public String GetMoreInfoLink() {
        return moreInfoLink;
    }
}
