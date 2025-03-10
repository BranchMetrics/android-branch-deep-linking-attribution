package io.branch.referral.validators;

import static io.branch.referral.validators.IntegrationValidatorConstants.packageNameMoreInfoDocsLink;

import android.content.Context;

import org.json.JSONObject;

public class PackageNameCheck extends IntegrationValidatorCheck {

    String name = "Package Name";
    String errorMessage = "Incorrect package name in Branch dashboard. Please correct your package name in dashboard -> Configuration page.";
    String moreInfoLink = packageNameMoreInfoDocsLink;
    BranchIntegrationModel integrationModel;
    JSONObject branchAppConfig;

    public PackageNameCheck(BranchIntegrationModel integrationModel, JSONObject branchAppConfig) {
        super.name = name;
        super.errorMessage = errorMessage;
        super.moreInfoLink = moreInfoLink;
        this.integrationModel = integrationModel;
        this.branchAppConfig = branchAppConfig;
    }

    @Override
    public boolean RunTests(Context context) {
        String valueOnDashboard = integrationModel.packageName;
        String valueInManifest = branchAppConfig.optString("android_package_name");
        return valueOnDashboard.equals(valueInManifest);
    }

    @Override
    public String GetOutput(Context context, boolean didTestSucceed) {
        didTestSucceed = RunTests(context);
        return super.GetOutput(context, didTestSucceed);
    }
}
