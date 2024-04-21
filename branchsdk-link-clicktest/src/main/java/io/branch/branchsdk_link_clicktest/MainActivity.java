package io.branch.branchsdk_link_clicktest;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.net.URI;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView textView =(TextView)findViewById(R.id.et_URL);
        Button button = (Button) findViewById(R.id.bt_click);

        String testDataStr = getIntent().getStringExtra("testData");
        Log.d("Branch SDK", "Intent extra 'testData:'\n" + testDataStr);

        String urlString = "https://cq9pf.app.link/qDpG4gNuIIb";
        if (testDataStr != null) {
            JSONObject testDataObj = null;
            try {
                testDataObj = (JSONObject) new JSONParser().parse(testDataStr);
                if (testDataObj.containsKey("URL")) {
                    urlString = (String) testDataObj.get("URL");
                }
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }

        textView.setText(urlString);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    String url = String.valueOf(textView.getText());
                    Uri URI = Uri.parse(url);
                    if(URI.getHost() != null) {
                        Intent intent = new Intent(Intent.ACTION_VIEW).setData(URI);
                        startActivity(intent);
                    } else {
                        textView.setText("");
                        Toast.makeText(MainActivity.this, "Invalid URL", Toast.LENGTH_LONG).show();
                    }
                } catch (NullPointerException npExp){
                    textView.setText("");
                    Toast.makeText(MainActivity.this, "Invalid URL", Toast.LENGTH_LONG).show();
                }
            }
        });

    }
}