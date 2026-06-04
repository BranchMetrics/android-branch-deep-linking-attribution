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
import java.util.ArrayList;
import java.util.List;

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
            List<String> lines = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(logFile)))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
            } catch (Exception e) {
                e.printStackTrace();
                lines.add("Error reading log file.");
            }
            StringBuilder logContent = new StringBuilder();
            for (int i = lines.size() - 1; i >= 0; i--) {
                logContent.append(lines.get(i)).append("\n");
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
