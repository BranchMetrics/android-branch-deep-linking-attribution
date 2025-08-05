package io.branch.branchandroidtestbed;

import android.app.Activity;

import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import io.branch.referral.Branch;
import io.branch.referral.PrefHelper;

public class SettingsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);

        setupDisableAdNetworkCalloutsSwitch();
        setupPrepHelperView();
        setupRetryEditText();
        setupApiUrlText();
    }

    void setupRetryEditText() {
        final EditText retryEditText = findViewById(R.id.retries_edit_text);
        final PrefHelper prefHelper = PrefHelper.getInstance(this);
        int currentRetries = prefHelper.getRetryCount();

        retryEditText.setText(Integer.toString(currentRetries));

        retryEditText.setOnEditorActionListener((textView, i, keyEvent) -> {
            if (i == EditorInfo.IME_ACTION_DONE) {
                int retries = Integer.parseInt(textView.getText().toString());
                PrefHelper.getInstance(SettingsActivity.this).setRetryCount(retries);

                InputMethodManager imm = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(retryEditText.getWindowToken(), 0);

                Toast.makeText(getApplicationContext(), "Set Network Retries to " + retries, Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });
    }

    void setupApiUrlText() {
        final EditText apiUrlText = findViewById(R.id.api_url_text);
        final PrefHelper prefHelper = PrefHelper.getInstance(this);
        String currentApiUrl = prefHelper.getAPIBaseUrl(true);
        if (currentApiUrl != null) {
            apiUrlText.setText(currentApiUrl);
        }

        apiUrlText.setOnEditorActionListener((textView, i, keyEvent) -> {
            if (i == EditorInfo.IME_ACTION_DONE) {
                String newApiUrl = textView.getText().toString();
                Branch.setAPIUrl(newApiUrl);

                InputMethodManager imm = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(apiUrlText.getWindowToken(), 0);

                Toast.makeText(getApplicationContext(), "Set API Base URL to " + newApiUrl, Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });
    }

    void setupPrepHelperView() {
        final PrefHelper prefHelper = PrefHelper.getInstance(this);
        final TextView prefHelperTextView = findViewById(R.id.prefhelper_text_view);
        StringBuilder strBuilder = new StringBuilder("\n\nPref Helper:");
        strBuilder.append(String.format("\n\nBranch SDK Version: %s", Branch.getSdkVersionNumber()));
        strBuilder.append("\n\nSession ID: " + prefHelper.getSessionID());
        strBuilder.append("\n\nApp Version: " + prefHelper.getAppVersion());
        strBuilder.append("\n\nApp Link: " + prefHelper.getAppLink());
        strBuilder.append("\n\nApp Store Referrer: " + prefHelper.getAppStoreReferrer());
        strBuilder.append("\n\nApp Store Source: " + prefHelper.getAppStoreSource());
        strBuilder.append("\n\nBranch Key: " + prefHelper.getBranchKey());
        strBuilder.append("\n\nGoogle Search Install ID: " + prefHelper.getGoogleSearchInstallIdentifier());
        strBuilder.append("\n\nIdentity: " + prefHelper.getIdentity());
        strBuilder.append("\n\nInitial Referrer: " + prefHelper.getInitialReferrer());
        strBuilder.append("\n\nLink Click ID: " + prefHelper.getLinkClickID());
        strBuilder.append("\n\nRandomized Bundle Token: " + prefHelper.getRandomizedBundleToken());
        strBuilder.append("\n\nRandomized Device Token: " + prefHelper.getRandomizedDeviceToken());
        strBuilder.append("\n\nReferrer Gclid: " + prefHelper.getReferrerGclid());
        strBuilder.append("\n\nReferrer Gclid Valid For Window: " + prefHelper.getReferrerGclidValidForWindow());
        strBuilder.append("\n\nSession ID: " + prefHelper.getSessionID());
        strBuilder.append("\n\nSession Params: " + prefHelper.getSessionParams());
        strBuilder.append("\n\nUser URL: " + prefHelper.getUserURL());
        strBuilder.append("\n\nConnect Timeout: " + prefHelper.getConnectTimeout());
        strBuilder.append("\n\nInstall Metadata: " + prefHelper.getInstallMetadata());
        strBuilder.append("\n\nPush Identifier: " + prefHelper.getPushIdentifier());
        strBuilder.append("\n\nLATD Attribtution Window: " + prefHelper.getLATDAttributionWindow());
        strBuilder.append("\n\nExternal Intent URI: " + prefHelper.getExternalIntentUri());
        strBuilder.append("\n\nExternal Intent Extra: " + prefHelper.getExternalIntentExtra());

        prefHelperTextView.setText(strBuilder.toString());
    }

    void setupDisableAdNetworkCalloutsSwitch() {
        final Switch disableAdNetworkCalloutsSwitch = findViewById(R.id.disable_ad_network_callouts);

        /*
         * Initialize switch state from SharedPreferences.
         */
        final PrefHelper prefHelper = PrefHelper.getInstance(this);
        disableAdNetworkCalloutsSwitch.setChecked(prefHelper.getAdNetworkCalloutsDisabled());

        /*
         * Update the setting whenever the switch changes state.
         */
        disableAdNetworkCalloutsSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Branch.getInstance().disableAdNetworkCallouts(disableAdNetworkCalloutsSwitch.isChecked());
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("BranchSDK_Tester", "Branch initialization status:" + Branch.getInstance().getInitState());
    }
}
