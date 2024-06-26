package io.branch.branchandroiddemo.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import io.branch.branchandroiddemo.Common;
import io.branch.branchandroiddemo.R;

public class ShowQRCodeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_qrcode);
        ImageView ivQrCode = findViewById(R.id.iv_qr_code);
        if (Common.qrCodeImage == null)
            ivQrCode.setVisibility(View.GONE);
        else {
            ivQrCode.setVisibility(View.VISIBLE);
            ivQrCode.setImageBitmap(Common.qrCodeImage);
        }
        findViewById(R.id.bt_close).setOnClickListener(v -> finish());
    }
}