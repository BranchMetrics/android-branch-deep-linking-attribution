package io.branch.referral;

import androidx.annotation.NonNull;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BranchPartnerParameters {
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, String>> partnerParameters = new ConcurrentHashMap<>();

    void clearAllParameters() {
        partnerParameters.clear();
    }

    @NonNull ConcurrentHashMap<String, String> parametersForPartner(@NonNull String key) {
        ConcurrentHashMap<String, String> res = partnerParameters.get(key);
        if (res == null) {
            res = new ConcurrentHashMap<>();
            partnerParameters.put(key, res);
        }
        return res;
    }

    private void addParameterWithName(@NonNull String key, @NonNull String value, @NonNull String partnerName) {
        parametersForPartner(partnerName).put(key, value);
    }

    void addFacebookParameter(@NonNull String key, @NonNull String value) {
        if (isSha256Hashed(value)) {
            addParameterWithName(key, value, "fb");
        } else {
            BranchLogger.w("Invalid partner parameter passed. Value must be a SHA 256 hash.");
        }
    }

    void addSnapParameter(@NonNull String key, @NonNull String value) {
        if (isSha256Hashed(value)) {
            addParameterWithName(key, value, "snap");
        } else {
            BranchLogger.w("Invalid partner parameter passed. Value must be a SHA 256 hash.");
        }
    }

    boolean isSha256Hashed(String value) {
        return value != null && value.length() == 64 && isHexadecimal(value);
    }

    private static final Pattern HEXADECIMAL_PATTERN = Pattern.compile("\\p{XDigit}+");
    boolean isHexadecimal(String input) {
        if (input == null) return false;
        if (input.length() == 0) return true;
        final Matcher matcher = HEXADECIMAL_PATTERN.matcher(input);
        return matcher.matches();
    }

    ConcurrentHashMap<String, ConcurrentHashMap<String, String>> allParams() {
        return partnerParameters;
    }
}
