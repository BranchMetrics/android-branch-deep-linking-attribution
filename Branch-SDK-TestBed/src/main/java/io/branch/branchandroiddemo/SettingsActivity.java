package io.branch.branchandroiddemo;

import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Switch;

import io.branch.referral.Branch;
import io.branch.referral.Defines;
import io.branch.referral.PrefHelper;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

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
}
