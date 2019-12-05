package io.branch.referral.util;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class BranchCPID {

    JSONObject cpidData;

    public BranchCPID() {
    }

    public BranchCPID(JSONObject cpidData) {
        this.cpidData = cpidData;
    }

    public String getCrossPlatformID() {
        if (cpidData != null && cpidData.length() != 0) {
            try {
                return cpidData.getJSONObject("user_data").getString("cross_platform_id");
            } catch (JSONException e) {
                return null;
            }
        }
        return null;
    }

    public JSONArray getPastCrossPlatformIds() {
        if (cpidData != null && cpidData.length() != 0) {
            try {
                return cpidData.getJSONObject("user_data")
                        .getJSONArray("past_cross_platform_ids");
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
        return null;
    }

    public JSONArray getProbabilisticCrossPlatformIds() {
        if (cpidData != null && cpidData.length() != 0) {
            try {
                JSONArray probCPIDs = cpidData.getJSONObject("user_data")
                        .getJSONArray("prob_cross_platform_ids");

                JSONArray finalCPIDsArray = new JSONArray();
                for (int i = 0, size = probCPIDs.length(); i < size; i++) {
                    finalCPIDsArray.put(new ProbabilisticCPID(probCPIDs.getString(i),
                            probCPIDs.getDouble(i)));
                }
                return finalCPIDsArray;
            } catch (JSONException e) {
                return null;
            }

        }
        return null;
    }

    public String getDeveloperIdentity() {
        if (cpidData != null && cpidData.length() != 0) {
            try {
                return cpidData.getJSONObject("user_data").getString("developer_identity");
            } catch (JSONException e) {
                return null;
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