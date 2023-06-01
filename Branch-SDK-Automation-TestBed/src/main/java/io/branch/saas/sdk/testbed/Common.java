package io.branch.saas.sdk.testbed;

import android.content.Context;
import android.graphics.Bitmap;

import androidx.appcompat.app.AlertDialog;

import io.branch.saas.sdk.testbed.listeners.DialogClickListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import io.branch.indexing.BranchUniversalObject;
import io.branch.referral.util.BRANCH_STANDARD_EVENT;
import io.branch.referral.util.ContentMetadata;
import io.branch.referral.util.LinkProperties;

public class Common {
    public static LinkProperties lp;
    public static Bitmap qrCodeImage;
    private static Common common;
    public static BranchUniversalObject branchUniversalObject;
    public ContentMetadata contentMetadata;
    public BRANCH_STANDARD_EVENT branchStandardEvent;

    public static Common getInstance() {
        if (common == null)
            common = new Common();
        return common;
    }

    public void showDialogBox(String title, String message, Context context, DialogClickListener dialogClickListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        //Uncomment the below code to Set the message and title from the strings.xml file
//        builder.setMessage(message);// .setTitle(R.string.dialog_title);

        //Setting message manually and performing action on button click
        builder.setMessage(message)
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, id) -> {
                    dialog.dismiss();
                    dialogClickListener.onDialogDismissed();
                });
        //Creating dialog box
        AlertDialog alert = builder.create();
        //Setting the title manually
        alert.setTitle(title);
        alert.show();
    }

    public String readLogData() {
        Process process = null;
        try {
            process = Runtime.getRuntime().exec("logcat -d");
        } catch (IOException e) {
            e.printStackTrace();
        }
        ArrayList<String> dataList = printResults(process);
        StringBuilder dataStr = new StringBuilder();
        for (String s : dataList) {
            System.out.println("data-->" + s);
            if (s.contains("BranchSDK") || s.contains("BRANCH SDK")) {
                if (s.contains("posting to") || s.contains("track user")
                        || s.contains("Post value") || s.contains("returned")
                        || s.contains("sessionParams") || s.contains("installParams")) {
                    System.out.println("data if-->" + s);
                    dataStr.append(s);
                    dataStr.append("\n\n");
                }
            }
        }
        return dataStr.toString();
    }

    private ArrayList<String> printResults(Process process) {

        ArrayList<String> lineString = new ArrayList<>();
        String line;
        try {
            BufferedReader stdOut = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));

            // read the output from the command
            while ((line = stdOut.readLine()) != null) {
                lineString.add(line);
            }

            // read any errors from the attempted command
            while ((line = stdError.readLine()) != null) {
                lineString.add(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lineString;
    }

    public void clearLog() {
        try {
            Runtime.getRuntime().exec("logcat -c");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
