package io.branch.referral.validators;

import android.content.Context;
import android.util.Log;
import android.view.WindowManager;

import org.json.JSONObject;

import java.util.Objects;

import io.branch.interfaces.IBranchLoggingCallbacks;
import io.branch.referral.Branch;

public class IntegrationValidator implements ServerRequestGetAppConfig.IGetAppConfigEvents {

    private static IntegrationValidator instance;
    private final BranchIntegrationModel integrationModel;
    private final String TAG = "BranchSDK_Doctor";
    private final StringBuilder branchLogsStringBuilder;

    Context context;

    boolean hasRan = false;
    boolean hasTestFailed = false;

    IntegrationValidatorDialog integrationValidatorDialog;

    private IntegrationValidator(Context context) {
        this.integrationModel = new BranchIntegrationModel(context);
        this.context = context;
        this.branchLogsStringBuilder = new StringBuilder();
    }

    public static void validate(Context context) {
        if (instance == null) {
            instance = new IntegrationValidator(context);
        }
        
        IBranchLoggingCallbacks iBranchLoggingCallbacks = new IBranchLoggingCallbacks() {
            @Override
            public void onBranchLog(String logMessage, String severityConstantName) {
                instance.branchLogsStringBuilder.append(logMessage);
            }
        };

        Branch.enableLogging(iBranchLoggingCallbacks);
        instance.validateSDKIntegration(context);
        instance.integrationValidatorDialog = new IntegrationValidatorDialog(context);
    }

    public static String getLogs() {
        return instance.branchLogsStringBuilder.toString();
    }

    private void validateSDKIntegration(Context context) {
        Branch.getInstance().requestQueue_.handleNewRequest(new ServerRequestGetAppConfig(context, IntegrationValidator.this));
    }

    private void doValidateWithAppConfig(JSONObject branchAppConfig) {
        //retrieve the Branch dashboard configurations from the server
        Branch.getInstance().requestQueue_.handleNewRequest(new ServerRequestGetAppConfig(context, this));

        logValidationProgress("\n\n------------------- Initiating Branch integration verification ---------------------------");

        // 1. Verify Branch Auto instance
        BranchInstanceCreationValidatorCheck branchInstanceCreationValidatorCheck = new BranchInstanceCreationValidatorCheck();
        boolean result = branchInstanceCreationValidatorCheck.RunTests(context);
        integrationValidatorDialog.setTestResult(1, branchInstanceCreationValidatorCheck.GetTestName(), result, branchInstanceCreationValidatorCheck.GetOutput(context, result), branchInstanceCreationValidatorCheck.GetMoreInfoLink());
        logOutputForTest(result, "1. Verifying Branch instance creation", "Branch is not initialised from your Application class. Please add `Branch.getAutoInstance(this);` to your Application#onCreate() method.", "https://help.branch.io/developers-hub/docs/android-basic-integration#section-load-branch");

        // 2. Verify Branch Keys
        BranchKeysValidatorCheck branchKeysValidatorCheck = new BranchKeysValidatorCheck();
        result = branchKeysValidatorCheck.RunTests(context);
        integrationValidatorDialog.setTestResult(2, branchKeysValidatorCheck.GetTestName(), result, branchKeysValidatorCheck.GetOutput(context, result), branchKeysValidatorCheck.GetMoreInfoLink());
        logOutputForTest(result, "2. Checking Branch keys", "Unable to read Branch keys from your application. Did you forget to add Branch keys in your application?.", "https://help.branch.io/developers-hub/docs/android-basic-integration#section-configure-app");

        // 3. Verify the package name of app with Branch dash board settings
        PackageNameCheck packageNameCheck = new PackageNameCheck(integrationModel, branchAppConfig);
        result = packageNameCheck.RunTests(context);
        integrationValidatorDialog.setTestResult(3, packageNameCheck.GetTestName(), result, packageNameCheck.GetOutput(context, result), packageNameCheck.GetMoreInfoLink());
        logOutputForTest(result, "3. Verifying application package name", packageNameCheck.errorMessage, "https://help.branch.io/developers-hub/docs/android-basic-integration#section-configure-branch-dashboard");

        // 4. Verify the URI scheme setup
        URISchemeCheck uriSchemeCheck = new URISchemeCheck(integrationModel, branchAppConfig);
        result = uriSchemeCheck.RunTests(context);
        integrationValidatorDialog.setTestResult(4, uriSchemeCheck.GetTestName(), result, uriSchemeCheck.GetOutput(context, result), uriSchemeCheck.GetMoreInfoLink());
        logOutputForTest(result, "4. Checking Android Manifest for URI based deep link config", uriSchemeCheck.errorMessage, "https://help.branch.io/developers-hub/docs/android-basic-integration#section-configure-app");

        //5. Check AndroidManifest for AppLink config
        AppLinksCheck appLinksCheck = new AppLinksCheck(integrationModel, branchAppConfig);
        result = appLinksCheck.RunTests(context);
        integrationValidatorDialog.setTestResult(5, appLinksCheck.GetTestName(), result, appLinksCheck.GetOutput(context, result), appLinksCheck.GetMoreInfoLink());
        logOutputForTest(result, "5. Checking AndroidManifest for AppLink config.", "Could not find any App Link hosts to support Android AppLinks. Please add intent filter for handling AppLinks in your Android Manifest file", "https://help.branch.io/using-branch/docs/android-app-links#section-add-intent-filter-to-manifest");

        //6. Look for any custom domains specified in the dash board and has matching intent filter
        CustomDomainCheck customDomainCheck = new CustomDomainCheck(integrationModel, branchAppConfig);
        result = customDomainCheck.RunTests(context);
        integrationValidatorDialog.setTestResult(6, customDomainCheck.GetTestName(), result, customDomainCheck.GetOutput(context, result), customDomainCheck.GetMoreInfoLink());
        logOutputForTest(result, "6. Verifying any supported custom link domains.", String.format("Could not find intent filter to support custom link domain '%s'. Please add intent filter for handling custom link domain in your Android Manifest file ", branchAppConfig.optString("short_url_domain")), "https://help.branch.io/developers-hub/docs/android-basic-integration#section-configure-app");

        // 7. Check for matching intent filter for default app link domains
        DefaultDomainsCheck defaultDomainsCheck = new DefaultDomainsCheck(integrationModel, branchAppConfig);
        result = defaultDomainsCheck.RunTests(context);
        integrationValidatorDialog.setTestResult(7, defaultDomainsCheck.GetTestName(), result, defaultDomainsCheck.GetOutput(context, result), defaultDomainsCheck.GetMoreInfoLink());
        logOutputForTest(result,  "7. Verifying default link domains integrations.", String.format("Could not find intent filter to support Branch default link domain '%s'. Please add intent filter for handling custom link domain in your Android Manifest file ", branchAppConfig.optString("default_short_url_domain")), "https://help.branch.io/developers-hub/docs/android-basic-integration#section-configure-app");

        // 8. Check for matching intent filter for alternate app link domains
        AlternateDomainsCheck alternateDomainsCheck = new AlternateDomainsCheck(integrationModel, branchAppConfig);
        result = alternateDomainsCheck.RunTests(context);
        integrationValidatorDialog.setTestResult(8, alternateDomainsCheck.GetTestName(), result, alternateDomainsCheck.GetOutput(context, result), alternateDomainsCheck.GetMoreInfoLink());
        logOutputForTest(result, "8. Verifying alternate link domains integrations.", String.format("Could not find intent filter to support alternate link domain '%s'. Please add intent filter for handling custom link domain in your Android Manifest file ", branchAppConfig.optString("alternate_short_url_domain")), "https://help.branch.io/developers-hub/docs/android-basic-integration#section-configure-app");

        finishTestingOutput();

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(Objects.requireNonNull(instance.integrationValidatorDialog.getWindow()).getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = 1500;
        instance.integrationValidatorDialog.show();
        instance.integrationValidatorDialog.getWindow().setAttributes(lp);
    }

    @Override
    public void onAppConfigAvailable(JSONObject branchAppConfig) {
        if (branchAppConfig != null && !hasRan) {
            hasRan = true;
            doValidateWithAppConfig(branchAppConfig);
        } else if(branchAppConfig == null){
            logIntegrationError("Unable to read Dashboard config. Please confirm that your Branch key is properly added to the manifest. Please fix your Dashboard settings.", "https://branch.app.link/link-settings-page");
        }
    }

    private void logIntegrationError(String message, String documentLink) {
        Log.d(TAG, "** ERROR ** : " + message + "\nPlease follow the link for more info " + documentLink);
    }

    private void logValidationProgress(String message) {
        Log.d(TAG, message + " ... ");
    }

    private void logValidationPassed() {
        Log.d(TAG, "Passed");
    }

    private void logOutputForTest(boolean result, String progressMessage, String errorMessage, String documentLink) {
        logValidationProgress(progressMessage);

        if(result) {
            logValidationPassed();
        } else {
            logIntegrationError(errorMessage, documentLink);
            hasTestFailed = true;
        }
    }

    private void finishTestingOutput() {
        if(!hasTestFailed) {
            Log.d(TAG, "--------------------------------------------\nSuccessfully completed Branch integration validation. Everything looks good!");
            Log.d(TAG, "\n         Great! Comment out the 'validateSDKIntegration' line in your app. Next check your deep link routing.\n" +
                    "         Append '?bnc_validate=true' to any of your app's Branch links and click it on your mobile device (not the Simulator!) to start the test.\n" +
                    "         For instance, to validate a link like:\n" +
                    "         https://<yourapp>.app.link/NdJ6nFzRbK\n" +
                    "         click on:\n" +
                    "         https://<yourapp>.app.link/NdJ6nFzRbK?bnc_validate=true");
        } else {
            Log.d(TAG, "--------------------------------------------\nCompleted Branch integration validation. Almost there! Please correct the issues identified for your Branch SDK implementation.");
        }
    }
}
