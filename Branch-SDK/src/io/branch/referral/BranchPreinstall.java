package io.branch.referral;

import android.content.Context;
import android.text.TextUtils;
import io.branch.referral.Defines.PreinstallKey;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by --vbajpai on --24/07/2019 at --13:44 for --android-branch-deep-linking-attribution
 */
class BranchPreinstall {

    private static final String SYSTEM_PROPERTIES_CLASS_KEY = "android.os.SystemProperties";
    private static final String BRANCH_PREINSTALL_PROP_KEY = "io.branch.preinstall.apps.path";

    public static void getPreinstallSystemData(Branch branchInstance, Context context) {
        if (branchInstance != null) {
            // check if the SystemProperties has the branch file path added
            String branchFilePath = checkForBranchPreinstallInSystem();
            if (!TextUtils.isEmpty(branchFilePath)) {
                // after getting the file path get the file contents
                readBranchFile(branchFilePath, branchInstance, context);
            }
        }
    }

    private static String checkForBranchPreinstallInSystem() {
        String path = null;
        try {
            path = (String) Class.forName(SYSTEM_PROPERTIES_CLASS_KEY)
                    .getMethod("get", String.class).invoke(null, BRANCH_PREINSTALL_PROP_KEY);
        } catch (Exception e) {
            return null;
        }
        return path;
    }

    private static void readBranchFile(final String branchFilePath, final Branch branchInstance,
            final Context context) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final StringBuilder branchFileContent = new StringBuilder();
                    JSONObject branchFileContentJson;
                    File branchFile = new File(branchFilePath);
                    BufferedReader br = new BufferedReader(new FileReader(branchFile));
                    String line;

                    while ((line = br.readLine()) != null) {
                        branchFileContent.append(line);
                    }
                    br.close();
                    branchFileContentJson = new JSONObject(branchFileContent.toString().trim());

                    if (!TextUtils.isEmpty(branchFileContentJson.toString())) {
                        getBranchFileContent(branchFileContentJson, branchInstance, context);
                    } else {
                        throw new FileNotFoundException();
                    }
                } catch (FileNotFoundException ignore) {
                } catch (IOException ignore) {
                } catch (JSONException ignore) {
                }
            }
        }).start();
    }

    public static void getBranchFileContent(JSONObject branchFileContentJson,
            Branch branchInstance, Context context) {
        // check if the current app package exists in the json
        Iterator<String> keys = branchFileContentJson.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            try {
                if (key.equals("apps") && branchFileContentJson
                        .get(key) instanceof JSONObject) {
                    if (branchFileContentJson.getJSONObject(key)
                            .get(SystemObserver.getPackageName(context)) != null) {
                        JSONObject branchPreinstallData = branchFileContentJson
                                .getJSONObject(key)
                                .getJSONObject(SystemObserver.getPackageName(context));

                        // find the preinstalls keys and any custom data
                        Iterator<String> preinstallDataKeys = branchPreinstallData
                                .keys();
                        while (preinstallDataKeys.hasNext()) {
                            String datakey = preinstallDataKeys.next();
                            if (datakey.equals(PreinstallKey.campaign.getKey()) && TextUtils.isEmpty(branchInstance.getInstallMetaData(PreinstallKey.campaign.getKey()))) {
                                branchInstance
                                        .setPreinstallCampaign(
                                                branchPreinstallData.get(datakey)
                                                        .toString());
                            } else if (datakey.equals(PreinstallKey.partner.getKey()) && TextUtils.isEmpty(branchInstance.getInstallMetaData(PreinstallKey.partner.getKey()))) {
                                branchInstance
                                        .setPreinstallPartner(
                                                branchPreinstallData.get(datakey)
                                                        .toString());
                            } else {
                                branchInstance.setRequestMetadata(datakey,
                                        branchPreinstallData.get(datakey).toString());
                            }
                        }
                    }
                }
            } catch (JSONException ignore) {
            }
        }
    }
}
