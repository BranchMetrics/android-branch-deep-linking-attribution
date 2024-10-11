package io.branch.referral.validators;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import io.branch.referral.R;

public class LinkingValidatorDialog extends Dialog implements AdapterView.OnItemSelectedListener {

    private enum ROUTING_TYPE { CANONICAL_URL, DEEPLINK_PATH, CUSTOM }
    private ROUTING_TYPE routingType;
    private Button ctaButton;
    private Spinner linkingValidatorDropdownMenu;
    private TextView linkingValidatorText;
    private EditText linkingValidatorEditText;
    private LinearLayout customKVPField;
    private int step = 1;

    public LinkingValidatorDialog(final Context context) {
        super(context);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.setContentView(R.layout.dialog_linking_validator);

        linkingValidatorDropdownMenu = findViewById(R.id.linkingValidatorDropdownMenu);
        List<String> choices = new ArrayList<>();
        choices.add("$canonical_url");
        choices.add("$deeplink_path");
        choices.add("other (custom)");
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, choices);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        linkingValidatorDropdownMenu.setAdapter(dataAdapter);
        linkingValidatorDropdownMenu.setOnItemSelectedListener(this);

        ctaButton = findViewById(R.id.linkingValidatorButton);
        ctaButton.setText(LinkingValidatorConstants.step1ButtonText);
        ctaButton.setOnClickListener(view -> {
            if(step == 1) {
                LoadStep2Screen();
            } else {
                GenerateBranchLinks();
            }
        });

        linkingValidatorText = findViewById(R.id.linkingValidatorText);
        linkingValidatorEditText = findViewById(R.id.linkingValidatorEditText);
        customKVPField = findViewById(R.id.customKVPField);

        linkingValidatorEditText.setVisibility(View.GONE);
        customKVPField.setVisibility(View.GONE);

        routingType = ROUTING_TYPE.CANONICAL_URL;
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        String item = adapterView.getItemAtPosition(i).toString();
        switch (item) {
            case "$canonical_url":
                routingType = ROUTING_TYPE.CANONICAL_URL;
                break;
            case "$deeplink_path":
                routingType = ROUTING_TYPE.DEEPLINK_PATH;
                break;
            case "other (custom)":
                routingType = ROUTING_TYPE.CUSTOM;
                break;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    void LoadStep2Screen() {
        step++;
        ctaButton.setText(LinkingValidatorConstants.step2ButtonText);
        linkingValidatorDropdownMenu.setVisibility(View.GONE);

        switch(routingType) {
            case CANONICAL_URL:
                linkingValidatorEditText.setVisibility(View.VISIBLE);
                linkingValidatorText.setText(LinkingValidatorConstants.canonicalURLPromptText);
                break;
            case DEEPLINK_PATH:
                linkingValidatorEditText.setVisibility(View.VISIBLE);
                linkingValidatorText.setText(LinkingValidatorConstants.deeplinkPathPromptText);
                break;
            case CUSTOM:
                customKVPField.setVisibility(View.VISIBLE);
                linkingValidatorText.setText(LinkingValidatorConstants.customKeyPromptText);
                break;
        }
    }

    void GenerateBranchLinks() {

    }
}
