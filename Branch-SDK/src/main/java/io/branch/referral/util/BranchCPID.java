package io.branch.referral.util;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

import io.branch.referral.BranchLogger;
import io.branch.referral.Defines;

public class BranchCPID {

    JSONObject cpidData;
    private static final String key_cross_platform_id = "cross_platform_id";
    private static final String key_past_cross_platform_id = "past_cross_platform_ids";
    private static final String key_prob_cross_platform_ids = "prob_cross_platform_ids";
    private static final String key_developer_identity = "developer_identity";

    public BranchCPID() {
    }

    public BranchCPID(JSONObject cpidData) {
        this.cpidData = cpidData;
    }

    public String getCrossPlatformID() {
        if (cpidData != null && cpidData.length() != 0) {
            try {
                return cpidData.getJSONObject(Defines.Jsonkey.UserData.getKey()).
                        getString(key_cross_platform_id);
            } catch (JSONException e) {
                BranchLogger.d(Objects.requireNonNull(e.getMessage()));
            }
        }
        return null;
    }

    public JSONArray getPastCrossPlatformIds() {
        if (cpidData != null && cpidData.length() != 0) {
            try {
                return cpidData.getJSONObject(Defines.Jsonkey.UserData.getKey())
                        .getJSONArray(key_past_cross_platform_id);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public JSONArray getProbabilisticCrossPlatformIds() {
        if (cpidData != null && cpidData.length() != 0) {
            try {
                JSONArray probCPIDs = cpidData.getJSONObject(Defines.Jsonkey.UserData.getKey())
                        .getJSONArray(key_prob_cross_platform_ids);

                JSONArray finalCPIDsArray = new JSONArray();
                for (int i = 0, size = probCPIDs.length(); i < size; i++) {
                    finalCPIDsArray.put(new ProbabilisticCPID(probCPIDs.getString(i),
                            probCPIDs.getDouble(i)));
                }
                return finalCPIDsArray;
            } catch (JSONException e) {
                BranchLogger.d(Objects.requireNonNull(e.getMessage()));
            }

        }
        return null;
    }

    public String getDeveloperIdentity() {
        if (cpidData != null && cpidData.length() != 0) {
            try {
                return cpidData.getJSONObject(Defines.Jsonkey.UserData.getKey()).
                        getString(key_developer_identity);
            } catch (JSONException e) {
                BranchLogger.d(Objects.requireNonNull(e.getMessage()));
            }
        }
        return null;
    }

    public class ProbabilisticCPID {

        public String id;

        public Double probability;

        public ProbabilisticCPID(String id, Double probability) {
            this.id = id;
            this.probability = probability;
        }

        public String getCPID() {
            if (!TextUtils.isEmpty(id)) {
                return id;
            }
            return null;
        }

        public Double getCPIDProbablity() {
            if (probability != null) {
                return probability;
            }
            return null;
        }
    }

}