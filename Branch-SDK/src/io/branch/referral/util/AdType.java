package io.branch.referral.util;

import android.text.TextUtils;

/**
 * Ad Types
 */
public enum AdType {
    BANNER("banner"),
    INTERSTITIAL("interstitial"),
    REWARDED_VIDEO("rewarded_video"),
    NATIVE("native");

    private final String name;

    AdType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
