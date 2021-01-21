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
            PrefHelper.Debug("Invalid partner parameter passed: " + value + ", must be hashed in sha 256.");
        }
    }

    private boolean isSha256Hashed(@NonNull String value) {
        return value.length() == 64 && isHexadecimal(value);
    }

    private static final Pattern HEXADECIMAL_PATTERN = Pattern.compile("\\p{XDigit}+");
    private boolean isHexadecimal(String input) {
        final Matcher matcher = HEXADECIMAL_PATTERN.matcher(input);
        return matcher.matches();
    }

    ConcurrentHashMap<String, ConcurrentHashMap<String, String>> allParams() {
        return partnerParameters;
    }
}
