package io.branch.branchandroiddemo.test;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.TextView;

import org.json.JSONException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;

import io.branch.branchandroiddemo.BuildConfig;
import io.branch.branchandroiddemo.R;
import io.branch.referral.Branch;
import io.branch.referral.BranchError;
import io.branch.referral.PrefHelper;
import io.branch.referral.ServerRequest;
import io.branch.referral.ServerResponse;

/**
 * Created by sojanpr on 3/11/18.
 * <p>
 * Activity for helping internal QA testing
 * </p>
 */

public class QAInternalActivity extends Activity implements Branch.IQAInternalEvents {
    
    private TextView logTxt;
    private StringBuilder logBuilder;
    private String KEY_KILL_ON_OPEN = "kill_on_open";
    PrefHelper prefHelper;
    private int testRunStateCnt = 0;
    private Handler timer;
    private final int TEST_INTERVAL = 500; // .5 sec interval between test cases
    QAInternalTestCases testCases = new QAInternalTestCases(this);
    TextView branchKeyTxtView;
    TextView apiURLTxtView;
    
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        logBuilder = new StringBuilder();
        prefHelper = PrefHelper.getInstance(this);
        setContentView(R.layout.qa_internal_activity);
        
        logTxt = (TextView) findViewById(R.id.log_view);
        logTxt.setMovementMethod(new ScrollingMovementMethod());
        timer = new Handler();
        
        findViewById(R.id.generate_test_docs).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                generateDocs();
            }
        });
        findViewById(R.id.run_tests).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runTest();
            }
        });
        branchKeyTxtView = ((TextView) findViewById(R.id.branch_api_url));
        apiURLTxtView = ((TextView) findViewById(R.id.branch_api_url));
        branchKeyTxtView.setHint(branchKeyTxtView.getHint() + "( Default: " + prefHelper.getBranchKey() + " )");
        apiURLTxtView.setHint(apiURLTxtView.getHint() + "( Default: " + prefHelper.getAPIBaseUrl() + " )");
        
    }
    
    
    private void generateDocs() {
        clearLog();
        testRunStateCnt = 0;
        Branch.getInstance().setToQAInternalMode(null, null, this);
        doRunTest();
    }
    
    private void runTest() {
        clearLog();
        testRunStateCnt = 0;
        String apiBaseUrl = ((TextView) findViewById(R.id.branch_api_url)).getText().toString();
        String branchKey = ((TextView) findViewById(R.id.branch_key_name)).getText().toString();
        
        apiBaseUrl = TextUtils.isEmpty(apiBaseUrl) ? prefHelper.getAPIBaseUrl() : apiBaseUrl;
        branchKey = TextUtils.isEmpty(branchKey) ? prefHelper.getBranchKey() : branchKey;
        
        Branch.getInstance().setToQAInternalMode(branchKey, apiBaseUrl, this);
        doRunTest();
    }
    
    private void doRunTest() {
        switch (testRunStateCnt) {
            case 0:
                testCases.simulateInstall();
                break;
            case 1:
                testCases.simulateOpen();
                break;
            case 2:
                testCases.simulateLinkCreate();
                break;
            case 3:
                testCases.simulateRegisterView();
                break;
            case 4:
                testCases.simulateBranchEvent();
                break;
            case 5:
                testCases.simulateCredit();
                break;
            case 6:
                testCases.simulateIdentifyUser();
                break;
            default:
                onTestFinished();
        }
    }
    
    private void onTestFinished() {
        updateLog("\n\n\n All test completed successfully");
        shareTestDataFile();
    }
    
    @Override
    public void onRequestCompleted(ServerRequest request, ServerResponse response) {
        try {
            updateLog((testRunStateCnt + 1) + ". " + request.getRequestPath());
            updateLog("\t\t\t" + request.getPost().toString(4));
            
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (response.getStatusCode() != HttpURLConnection.HTTP_OK) {
            updateLog("\n\n Test Finished with errors.");
            updateLog(new BranchError(response.getFailReason(), response.getStatusCode()).getMessage());
        } else {
            updateLog("\n------------------------------------------------------------\n");
            timer.postDelayed(new Runnable() {
                @Override
                public void run() {
                    testRunStateCnt++;
                    doRunTest();
                }
            }, TEST_INTERVAL);
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (getIntent().getBooleanExtra(KEY_KILL_ON_OPEN, false)) {
            finish();
        }
    }
    
    private void updateLog(String log) {
        logBuilder.append("\n" + log);
        logTxt.setText(logBuilder.toString());
    }
    
    private void clearLog() {
        logTxt.setText("");
        logBuilder = new StringBuilder();
    }
    
 
    
    private File shareTestDataFile( ) {
        final File path = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS + "/Branch/");
        if (!path.exists()) {
            path.mkdirs();
        }
        
        final File file = new File(path, "android_request_model_" + BuildConfig.VERSION_NAME+".txt");
        
        try {
            file.createNewFile();
            FileOutputStream fOut = new FileOutputStream(file);
            fOut.write(logBuilder.toString().getBytes());
            fOut.flush();
            fOut.close();
            Intent intentShareFile = new Intent(Intent.ACTION_SEND);
    
            if(file.exists()) {
                intentShareFile.setType("application/pdf");
                intentShareFile.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://"+file.getAbsolutePath()));
                intentShareFile.putExtra(Intent.EXTRA_SUBJECT,
                        "Sharing File...");
                intentShareFile.putExtra(Intent.EXTRA_TEXT, "Request demo file");
        
                startActivity(Intent.createChooser(intentShareFile, "Request demo file"));
            }
        } catch (IOException e) {
            return null;
        }
        return file;
    }
}
