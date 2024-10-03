package io.branch.referral.validators;

import android.content.Context;

public abstract class IntegrationValidatorCheck {
    String name;
    String errorMessage;
    String moreInfoLink;

    public abstract boolean RunTests(Context context);

    public String GetOutput(Context context, boolean didTestSucceed) {
        String symbol = RunTests(context) ? IntegrationValidatorConstants.checkmark : IntegrationValidatorConstants.xmark;
        return errorMessage;
    }

    public String GetTestName() {
        return name;
    }

    public String GetMoreInfoLink() {
        return moreInfoLink;
    }
}
