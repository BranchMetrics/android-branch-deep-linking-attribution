package io.branch.referral;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Browser;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.jar.JarFile;

/**
 * Created by sojanpr on 9/15/17.
 */

public class IntegrationValidator {

    public void validateSDKIntegration (Context context){
        Log.d("BranchSDK", "** Initiating Branch integration verification **");
        Log.d("BranchSDK", "-------------------------------------------------");
        Log.d("BranchSDK", "----- checking for package name correctness -----");
        BranchIntegrationModel integrationModel = new BranchIntegrationModel();
        integrationModel.packageName = context.getPackageName();
        ApplicationInfo appInfo = null;
        try {
            appInfo = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            if (appInfo.metaData != null) {
                integrationModel.branchKeyLive = appInfo.metaData.getString("io.branch.sdk.BranchKey");
                integrationModel.branchKeyTest = appInfo.metaData.getString("io.branch.sdk.BranchKey.test");
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        updateIntegrationModel(context, integrationModel);

        JSONObject serverSideAppConfig = Branch.getInstance().generateAppConfigInternal();

        if (serverSideAppConfig != null) {
            try {
                String logLine = "";
                if (!serverSideAppConfig.get("android_package_name").equals(integrationModel.packageName)) {
                    logLine = "ERROR: ";
                } else {
                    logLine = "PASS: ";
                }
                logLine = logLine + "Dashboard Link Settings page '" + serverSideAppConfig.getString("android_package_name") + "' compared to client side '"  + integrationModel.packageName + "'";
                Log.d("BranchSDK", logLine);

                Log.d("BranchSDK", " ----- checking for URI scheme correctness -----");

                if (!serverSideAppConfig.getString("android_uri_scheme").replace("://", "").equals(integrationModel.deeplinkUriScheme)) {
                    logLine = "ERROR: ";
                } else {
                    logLine = "PASS: ";
                }
                logLine = logLine + "Dashboard Link Settings page '" + serverSideAppConfig.getString("android_uri_scheme").replace("://", "") + "' compared to client side '"  + integrationModel.deeplinkUriScheme + "'";
                Log.d("BranchSDK", logLine);

                if (integrationModel.applinkScheme == null || integrationModel.applinkScheme.isEmpty()) {
                    Log.d("BranchSDK", "ERROR: Could not find any App Link hosts to support Android App Links");
                } else {
                    boolean found = false;

                    if (serverSideAppConfig.getString("short_url_domain").length() > 0) {
                        Log.d("BranchSDK", " ----- looking for custom domain App Links intent filter -----");
                        for (String host : integrationModel.applinkScheme) {
                            if (host.equals(serverSideAppConfig.getString("short_url_domain"))) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            Log.d("BranchSDK", "ERROR: Could not find custom domain '" + serverSideAppConfig.getString("short_url_domain") + "' in App Link hosts.");
                        } else {
                            Log.d("BranchSDK", "PASS: Successfully found '" + serverSideAppConfig.getString("short_url_domain") + "' in App Link hosts.");
                        }
                    }

                    Log.d("BranchSDK", " ----- looking for default link domain App Links intent filter -----");
                    found = false;
                    for (String host : integrationModel.applinkScheme) {
                        if (host.equals(serverSideAppConfig.getString("default_short_url_domain"))) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        Log.d("BranchSDK", "ERROR: Could not find default link domain '" + serverSideAppConfig.getString("default_short_url_domain") + "' in App Link hosts.");
                    } else {
                        Log.d("BranchSDK", "PASS: Successfully found '" + serverSideAppConfig.getString("default_short_url_domain") + "' in App Link hosts.");
                    }

                    Log.d("BranchSDK", " ----- looking for alternate link domain App Links intent filter -----");
                    found = false;
                    for (String host : integrationModel.applinkScheme) {
                        if (host.equals(serverSideAppConfig.getString("alternate_short_url_domain"))) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        Log.d("BranchSDK", "ERROR: Could not find default link domain '" + serverSideAppConfig.getString("alternate_short_url_domain") + "' in App Link hosts.");
                    } else {
                        Log.d("BranchSDK", "PASS: Successfully found '" + serverSideAppConfig.getString("alternate_short_url_domain") + "' in App Link hosts.");
                    }
                }
                Log.d("BranchSDK", "-------------------------------------------------");
                Log.d("BranchSDK", "** Branch integration verification complete **");

            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            Log.d("BranchSDK", "ERROR: Unable to read Dashboard config. Please confirm that your Branch key is properly added to the manifest");
            Log.d("BranchSDK", "ERROR: To fix your Dashboard settings head over to https://branch.app.link/link-settings-page");
        }
    }

    public void validateDeeplinkRouting(final JSONObject validate_json,final WeakReference<Activity> currentActivityReference_) {
        Activity current_activity = currentActivityReference_.get();
        AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(current_activity, android.R.style.Theme_Material_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(current_activity);
        }
        builder.setTitle("Branch Deeplinking Routing")
                .setMessage("Did the Deeplink route you to the correct content?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // Test Succeeded
                        String launch_link = attachTestResults(validate_json,"g");
                        launchTestTemplate(currentActivityReference_,launch_link);
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // Test Failed
                        String launch_link = attachTestResults(validate_json,"r");
                        launchTestTemplate(currentActivityReference_,launch_link);
                    }
                })
                .setNeutralButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // Do nothing
                    }
                })
                .setCancelable(false)
                .setIcon(android.R.drawable.sym_def_app_icon)
                .show();
    }

    public void launchTestTemplate(WeakReference<Activity> activity,String url){
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        i.putExtra(Browser.EXTRA_APPLICATION_ID, activity.get().getPackageName());
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.setPackage("com.android.chrome");
        try {
            activity.get().startActivity(i);
        } catch (ActivityNotFoundException e) {
            // Chrome is probably not installed
            // Try with the default browser
            i.setPackage(null);
            activity.get().startActivity(i);
        }
    }

    private void updateIntegrationModel(Context context, BranchIntegrationModel integrationModel) {

        if (!isLowOnMemory(context)) {
            PackageManager pm = context.getPackageManager();
            try {
                ApplicationInfo ai = pm.getApplicationInfo(integrationModel.packageName, 0);
                String sourceApk = ai.publicSourceDir;
                JarFile jf = null;
                InputStream is = null;
                byte[] xml;
                try {
                    jf = new JarFile(sourceApk);
                    is = jf.getInputStream(jf.getEntry("AndroidManifest.xml"));
                    xml = new byte[is.available()];
                    //noinspection ResultOfMethodCallIgnored
                    is.read(xml);
                    JSONObject obj = new ApkParser().decompressXML(xml);
                    if (obj.has("scheme")) {
                        integrationModel.deeplinkUriScheme = obj.getString("scheme");
                    }
                    if (obj.has("hosts")) {
                        integrationModel.applinkScheme = new ArrayList<String>();
                        JSONArray jsonHosts = obj.getJSONArray("hosts");
                        for (int i = 0; i<jsonHosts.length(); i++){
                            integrationModel.applinkScheme.add(jsonHosts.getString(i));
                        }
                    }
                } catch (Exception ignored) {
                } finally {
                    try {
                        if (is != null) {
                            is.close();
                            // noinspection unused
                            is = null;
                        }
                        if (jf != null) {
                            jf.close();
                        }
                    } catch (IOException ignored) {
                    }
                }
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        }
    }

    private String attachTestResults(JSONObject blob,String result) {
        String link = "";
        try{
            link = blob.getString("~referring_link");
            link = link.split("\\?")[0];
        } catch (Exception e) {
            Log.e("BRANCH SDK","Failed to get referring link");
        }
        link += "?validate=true";
        link += "&$uri_redirect_mode=2";
        try {
            link += blob.getString("ct").equals("t1")? "&t1="+result: "&t1="+blob.getString("t1");
            link += blob.getString("ct").equals("t2")? "&t2="+result: "&t2="+blob.getString("t2");
            link += blob.getString("ct").equals("t3")? "&t3="+result: "&t3="+blob.getString("t3");
            link += blob.getString("ct").equals("t4")? "&t4="+result: "&t4="+blob.getString("t4");
            link += blob.getString("ct").equals("t5")? "&t5="+result: "&t5="+blob.getString("t5");
        } catch (Exception e) {
            e.printStackTrace();
        }
        link += "&os=android";
        return link;
    }
    
    private boolean isLowOnMemory(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(mi);
        return mi.lowMemory;
    }
    
    private class BranchIntegrationModel {
        private String deeplinkUriScheme;
        private String branchKeyTest;
        private String branchKeyLive;
        private ArrayList<String> applinkScheme;
        private String packageName;
    }
}
