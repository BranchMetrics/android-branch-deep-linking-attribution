package io.branch.saas.sdk.testbed.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import io.branch.indexing.BranchUniversalObject;
import io.branch.referral.util.BranchEvent;
import io.branch.referral.util.ContentMetadata;
import io.branch.saas.sdk.testbed.Common;
import io.branch.saas.sdk.testbed.Constants;
import io.branch.saas.sdk.testbed.R;
import io.branch.saas.sdk.testbed.listeners.DialogClickListener;

public class BUOReferenceActivity extends AppCompatActivity implements View.OnClickListener {
    private EditText etTitle, etContentDescription, etImageUrl, etCanonicalIdentifier;
    private String clickType;
    private Button btSubmit, btAddMeta;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buoreference);
        clickType = getIntent().getStringExtra(Constants.TYPE);
        getSupportActionBar().setTitle("Create BUO");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        etCanonicalIdentifier = findViewById(R.id.et_canonicalIdentifier);
        etTitle = findViewById(R.id.et_title);
        etContentDescription = findViewById(R.id.et_content_description);
        etImageUrl = findViewById(R.id.et_image_url);
        btSubmit = findViewById(R.id.bt_submit);
        btAddMeta = findViewById(R.id.bt_add_metadata);
        btSubmit.setOnClickListener(this);
        btAddMeta.setOnClickListener(this);
    }

    // this event will enable the back
    // function to the button on press
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            this.finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View view) {
        /*Intent intent = new Intent();
        Log.e("data",""+etTitle.getText()+"   "+etContentDescription.getText() +"  "+etImageUrl.getText());
        intent.putExtra(Constants.CONTENT_TITLE, etTitle.getText().toString());
        intent.putExtra(Constants.CONTENT_DESC, etContentDescription.getText().toString());
        intent.putExtra(Constants.IMAGE_URL, etImageUrl.getText().toString());
        setResult(RESULT_OK, intent);
        finish();*/
        if (view == btSubmit) {
            String canStr = etCanonicalIdentifier.getText().toString();
            String title = etTitle.getText().toString();
            String desc = etContentDescription.getText().toString();
            String imgUrl = etImageUrl.getText().toString();
            Common.branchUniversalObject = new BranchUniversalObject()
                    .setCanonicalIdentifier(TextUtils.isEmpty(canStr) ? null : canStr)
                    .setTitle(TextUtils.isEmpty(title) ? null : title)
                    .setContentDescription(TextUtils.isEmpty(desc) ? null : desc)
                    .setContentImageUrl(TextUtils.isEmpty(imgUrl) ? null : imgUrl)
                    .setContentIndexingMode(BranchUniversalObject.CONTENT_INDEX_MODE.PUBLIC)
                    .setLocalIndexMode(BranchUniversalObject.CONTENT_INDEX_MODE.PUBLIC)
                    .setContentMetadata(Common.getInstance().contentMetadata == null ? new ContentMetadata().addCustomMetadata("key1", "value1") : Common.getInstance().contentMetadata);
//        Common.branchUniversalObject.listOnGoogleSearch(this);
//        new BranchEvent(BRANCH_STANDARD_EVENT.VIEW_ITEM).addContentItems(Common.branchUniversalObject).logEvent(this);

            if (Common.branchUniversalObject == null) {
                Common.getInstance().showDialogBox("Fail", "Failed to create Content reference", BUOReferenceActivity.this, new DialogClickListener() {
                    @Override
                    public void onDialogDismissed() {

                    }
                });
            } else {
                Common.getInstance().clearLog();
                if (!TextUtils.isEmpty(clickType) && clickType.equals(Constants.TRACK_CONTENT)) {
                    boolean logEvent = new BranchEvent(Common.getInstance().branchStandardEvent).addContentItems(Common.branchUniversalObject).logEvent(this);
                    Intent intent = new Intent(this, LogDataActivity.class);
                    intent.putExtra(Constants.TYPE, Constants.TRACK_CONTENT_DATA);
                    if (logEvent) {
                        intent.putExtra(Constants.STATUS, Constants.SUCCESS);
                    } else {
                        intent.putExtra(Constants.STATUS, Constants.FAIL);
                    }
                    startActivity(intent);
                    finish();
                    return;
                }
//                Common.branchUniversalObject.listOnGoogleSearch(this);
                Common.getInstance().showDialogBox("Success", "BranchUniversalObject reference created", BUOReferenceActivity.this, new DialogClickListener() {
                    @Override
                    public void onDialogDismissed() {
                        if (clickType.equals(Constants.BUO_REFERENCE_AND_CREATE_DEP_LINK)
                                || clickType.equals(Constants.CREATE_SHARE_LINK)
                                || clickType.equals(Constants.CREATE_SEND_NOTIFICATION)
                                || clickType.equals(Constants.CREATE_SEND_READ_DEEP_LINK)
                                || clickType.equals(Constants.NAVIGATE_TO_CONTENT)
                                || clickType.equals(Constants.TRACK_CONTENT)
                                || clickType.equals(Constants.HANDLE_LINKS)
                                || clickType.equals(Constants.CREATE_QR_CODE)) {
                            Intent intent = new Intent(BUOReferenceActivity.this, GenerateUrlActivity.class);
                            intent.putExtra(Constants.TYPE, clickType);
                            startActivity(intent);
                        }
                        finish();
                    }
                });
            }
        } else if (view == btAddMeta){
            Intent intent = new Intent(BUOReferenceActivity.this, MetadataActivity.class);
            startActivity(intent);
        }
    }
}