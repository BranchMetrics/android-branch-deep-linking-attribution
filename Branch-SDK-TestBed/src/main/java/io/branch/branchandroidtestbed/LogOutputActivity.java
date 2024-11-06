package io.branch.branchandroidtestbed;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

public class LogOutputActivity extends Activity {
    private TextView logOutputTextView;
    private File logFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_output);

        logOutputTextView = findViewById(R.id.logOutputTextView);
        logFile = new File(getFilesDir(), "branchlogs.txt");
        displayLogs();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_log_output, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_clear_logs) {
            clearLogs();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void displayLogs() {
        if (logFile.exists()) {
            StringBuilder logContent = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(logFile)))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logContent.append(line).append("\n");
                }
            } catch (Exception e) {
                e.printStackTrace();
                logContent.append("Error reading log file.");
            }

            logOutputTextView.setText(logContent.toString());
        } else {
            logOutputTextView.setText("Log file not found.");
        }
    }

    private void clearLogs() {
        if (logFile.exists()) {
            if (logFile.delete()) {
                Toast.makeText(this, "Logs cleared.", Toast.LENGTH_SHORT).show();
                logOutputTextView.setText("Logs cleared.");
                finish();
            } else {
                Toast.makeText(this, "Failed to clear logs.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "No log file to clear.", Toast.LENGTH_SHORT).show();
        }
    }
}
