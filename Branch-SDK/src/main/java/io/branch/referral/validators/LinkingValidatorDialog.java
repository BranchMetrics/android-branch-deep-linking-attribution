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
    private LinearLayout linkingValidatorRowsLayout;
    private int step = 1;
    private String routingKey = "";
    private String routingValue = "";
    private EditText customKeyEditText;
    private EditText customValueEditText;
    private Context context;
    private LinkingValidatorDialogRowItem row1;
    private LinkingValidatorDialogRowItem row2;
    private LinkingValidatorDialogRowItem row3;
    private LinkingValidatorDialogRowItem row4;
    private LinkingValidatorDialogRowItem row5;
    private LinkingValidatorDialogRowItem row6;

    public LinkingValidatorDialog(final Context context) {
        super(context);
        this.context = context;
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.setContentView(R.layout.dialog_linking_validator);

        linkingValidatorDropdownMenu = findViewById(R.id.linkingValidatorDropdownMenu);
        List<String> choices = new ArrayList<>();
        choices.add(LinkingValidatorConstants.canonicalUrlKey);
        choices.add(LinkingValidatorConstants.deeplinkPathKey);
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
        linkingValidatorRowsLayout = findViewById(R.id.linkingValidatorRows);

        linkingValidatorEditText.setVisibility(View.GONE);
        customKVPField.setVisibility(View.GONE);
        linkingValidatorRowsLayout.setVisibility(View.GONE);

        routingType = ROUTING_TYPE.CANONICAL_URL;

        row1 = findViewById(R.id.linkingValidatorRow1);
        row2 = findViewById(R.id.linkingValidatorRow2);
        row3 = findViewById(R.id.linkingValidatorRow3);
        row4 = findViewById(R.id.linkingValidatorRow4);
        row5 = findViewById(R.id.linkingValidatorRow5);
        row6 = findViewById(R.id.linkingValidatorRow6);
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
                routingKey = LinkingValidatorConstants.canonicalUrlKey;
                routingValue = linkingValidatorEditText.getText().toString();
                break;
            case DEEPLINK_PATH:
                linkingValidatorEditText.setVisibility(View.VISIBLE);
                linkingValidatorText.setText(LinkingValidatorConstants.deeplinkPathPromptText);
                routingKey = LinkingValidatorConstants.deeplinkPathKey;
                routingValue = linkingValidatorEditText.getText().toString();
                break;
            case CUSTOM:
                customKVPField.setVisibility(View.VISIBLE);
                linkingValidatorText.setText(LinkingValidatorConstants.customKeyPromptText);
                break;
        }
    }

    void GenerateBranchLinks() {
        linkingValidatorEditText.setVisibility(View.GONE);
        linkingValidatorText.setVisibility(View.GONE);
        ctaButton.setText(LinkingValidatorConstants.step3ButtonText);
        linkingValidatorRowsLayout.setVisibility(View.VISIBLE);

        customKeyEditText = findViewById(R.id.keyEditText);
        customValueEditText = findViewById(R.id.valueEditText);

        //if routing key is empty, it is a custom key outside of $canonical_url and $deeplink_path
        if(routingKey.isEmpty()) {
            routingKey = customKeyEditText.getText().toString();
            routingValue = customValueEditText.getText().toString();
        }

        row1.InitializeRow(LinkingValidatorConstants.linkingValidatorRow1Title, LinkingValidatorConstants.infoButton1Copy, LinkingValidatorConstants.debugButton1Copy, routingKey, routingValue, "regularBranchLink", true, 0);
        row2.InitializeRow(LinkingValidatorConstants.linkingValidatorRow2Title, LinkingValidatorConstants.infoButton2Copy, LinkingValidatorConstants.debugButton2Copy, routingKey, routingValue, "uriFallbackBranchLink", true, 1, "$uri_redirect_mode", "2");
        row3.InitializeRow(LinkingValidatorConstants.linkingValidatorRow3Title, LinkingValidatorConstants.infoButton3Copy, LinkingValidatorConstants.debugButton3Copy, routingKey, routingValue, "webOnlyBranchLink", true, 2, "$web_only", "true");
        row4.InitializeRow(LinkingValidatorConstants.linkingValidatorRow4Title, LinkingValidatorConstants.infoButton4Copy, LinkingValidatorConstants.debugButton4Copy, routingKey, "", "missingDataBranchLink", true, 3);
        row5.InitializeRow(LinkingValidatorConstants.linkingValidatorRow5Title, LinkingValidatorConstants.infoButton5Copy, LinkingValidatorConstants.debugButton5Copy, routingKey, routingValue, "warmStartUseCase", false, 4);
        row6.InitializeRow(LinkingValidatorConstants.linkingValidatorRow6Title, LinkingValidatorConstants.infoButton6Copy, LinkingValidatorConstants.debugButton6Copy, routingKey, routingValue, "foregroundClickUseCase", false, 5);
    }
}
