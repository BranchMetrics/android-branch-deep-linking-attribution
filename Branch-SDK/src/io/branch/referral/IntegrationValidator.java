package io.branch.referral;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.jar.JarFile;

/**
 * Created by sojanpr on 9/15/17.
 */

public class IntegrationValidator {

    public void validateIntegration (Context context){
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
                logLine = logLine + "Dashboard setting of '" + serverSideAppConfig.getString("android_package_name") + "' compared to client side '"  + integrationModel.packageName + "'";
                Log.d("BranchSDK", logLine);

                Log.d("BranchSDK", " ----- checking for URI scheme correctness -----");

                if (!serverSideAppConfig.getString("android_uri_scheme").replace("://", "").equals(integrationModel.deeplinkUriScheme)) {
                    logLine = "ERROR: ";
                } else {
                    logLine = "PASS: ";
                }
                logLine = logLine + "Dashboard setting of '" + serverSideAppConfig.getString("android_uri_scheme").replace("://", "") + "' compared to client side '"  + integrationModel.deeplinkUriScheme + "'";
                Log.d("BranchSDK", logLine);

                if (integrationModel.applinkSheme.isEmpty()) {
                    Log.d("BranchSDK", "ERROR: Could not find any App Link hosts to support Android App Links");
                } else {
                    boolean found = false;

                    if (serverSideAppConfig.getString("short_url_domain").length() > 0) {
                        Log.d("BranchSDK", " ----- looking for custom domain App Links intent filter -----");
                        for (String host : integrationModel.applinkSheme) {
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
                    for (String host : integrationModel.applinkSheme) {
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
                    for (String host : integrationModel.applinkSheme) {
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
            Log.d("BranchSDK", "ERROR: Unable to read dashboard config. Please confirm that your Branch key is properly added to the manifest");
        }
    }

    void updateIntegrationModel(Context context, BranchIntegrationModel integrationModel) {
        
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
                        integrationModel.applinkSheme = new ArrayList<String>();
                        JSONArray jsonHosts = obj.getJSONArray("hosts");
                        for (int i = 0; i<jsonHosts.length(); i++){
                            integrationModel.applinkSheme.add(jsonHosts.getString(i));
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
        private ArrayList<String> applinkSheme;
        private String packageName;
        
    }
}
