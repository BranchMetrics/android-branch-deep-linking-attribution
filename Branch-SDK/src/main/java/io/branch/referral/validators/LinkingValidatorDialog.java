package io.branch.referral.validators;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.List;

import io.branch.referral.R;

public class LinkingValidatorDialog extends Dialog implements AdapterView.OnItemSelectedListener {

    public LinkingValidatorDialog(final Context context) {
        super(context);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.setContentView(R.layout.dialog_linking_validator);

        Spinner spinner = findViewById(R.id.spinner);
        List<String> choices = new ArrayList<>();
        choices.add("$canonical_url");
        choices.add("$deeplink_path");
        choices.add("other (custom)");
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, choices);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(dataAdapter);
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }
}
