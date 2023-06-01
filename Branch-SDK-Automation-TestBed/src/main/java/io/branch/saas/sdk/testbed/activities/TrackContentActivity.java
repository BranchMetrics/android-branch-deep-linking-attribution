package io.branch.saas.sdk.testbed.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;

import io.branch.referral.util.BRANCH_STANDARD_EVENT;
import io.branch.saas.sdk.testbed.Common;
import io.branch.saas.sdk.testbed.Constants;
import io.branch.saas.sdk.testbed.R;

public class TrackContentActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    private Spinner trackContentSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_content);

        trackContentSpinner = findViewById(R.id.track_content_spinner);
        trackContentSpinner.setOnItemSelectedListener(this);
        ArrayAdapter ad
                = new ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                getResources().getStringArray(R.array.track_content_items));

        // set simple layout resource file
        // for each item of spinner
        ad.setDropDownViewResource(
                android.R.layout
                        .simple_spinner_dropdown_item);

        // Set the ArrayAdapter (ad) data on the
        // Spinner which binds data to spinner
        trackContentSpinner.setAdapter(ad);
        findViewById(R.id.bt_submit).setOnClickListener(view -> {
            Intent intent = new Intent(TrackContentActivity.this, BUOReferenceActivity.class);
            intent.putExtra(Constants.TYPE, Constants.TRACK_CONTENT);
            startActivity(intent);
            finish();
        });
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        Common.getInstance().branchStandardEvent = BRANCH_STANDARD_EVENT.valueOf(trackContentSpinner.getSelectedItem().toString());

    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }
}