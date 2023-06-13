package io.branch.saas.sdk.testbed.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import io.branch.saas.sdk.testbed.Constants;
import io.branch.saas.sdk.testbed.R;

public class UrlPreviewActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_url_preview);
        getSupportActionBar().setTitle("Result URL");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        TextView tvUrl = findViewById(R.id.tv_url);
        String url = getIntent().getStringExtra(Constants.ANDROID_URL);
        tvUrl.setText(url);
        tvUrl.setMovementMethod(LinkMovementMethod.getInstance());
        findViewById(R.id.bt_read_deep_link).setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse(url));
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            intent.setPackage(getPackageName());
            startActivity(intent);
            finish();
        });
    }

    // this event will enable the back
    // function to the button on press
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}