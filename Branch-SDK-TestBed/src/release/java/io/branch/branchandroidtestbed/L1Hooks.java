package io.branch.branchandroidtestbed;

import android.content.Context;

import io.branch.referral.BranchConfiguration;

/** Release builds have no L1 switches. */
final class L1Hooks {
    private L1Hooks() {
    }

    static String branchKey(Context context, String defaultKey) {
        return defaultKey;
    }

    static void applyLaunchConfig(Context context, BranchConfiguration.Builder config) {
    }

    static boolean useUriCall(Context context) {
        return false;
    }
}
