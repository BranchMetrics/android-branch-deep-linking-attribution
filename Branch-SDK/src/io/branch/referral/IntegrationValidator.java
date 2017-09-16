package io.branch.referral;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.jar.JarFile;

import javax.xml.parsers.DocumentBuilder;

import io.branch.referral.ApkParser;

/**
 * Created by sojanpr on 9/15/17.
 */

public class IntegrationValidator {
    public static void validateIntegration (Context context){
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
        Log.d("BranchSDK", "integrationModel params are:" + integrationModel.packageName + "\n"
                + integrationModel.branchKeyLive + "\n" + integrationModel.branchKeyTest + "\n"
                + integrationModel.deeplinkUriScheme
        );
    }

    public static void updateIntegrationModel(Context context, BranchIntegrationModel integrationModel) {
        
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
                    integrationModel.deeplinkUriScheme = new ApkParser().decompressXML(xml);
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
    
    private static boolean isLowOnMemory(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(mi);
        return mi.lowMemory;
    }
    
    private static class BranchIntegrationModel {
        private String deeplinkUriScheme;
        private String deeplinkUriPath;
        private String branchKeyTest;
        private String branchKeyLive;
        private ArrayList<String> applinkSheme;
        private String packageName;
        
    }
}
