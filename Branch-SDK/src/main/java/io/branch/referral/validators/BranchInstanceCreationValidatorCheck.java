package io.branch.referral.validators;

import android.content.Context;

import io.branch.referral.Branch;

public class BranchInstanceCreationValidatorCheck extends IntegrationValidatorCheck {

    String name = "Branch instance";
    String errorMessage = "Branch is not initialised from your Application class. Please add `Branch.getAutoInstance(this);` to your Application#onCreate() method.";
    String moreInfoLink = "<a href=\"https://help.branch.io/developers-hub/docs/android-basic-integration#:~:text=Branch.getAutoInstance(this)\">More info</a>";

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
