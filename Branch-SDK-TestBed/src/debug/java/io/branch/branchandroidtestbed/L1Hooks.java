package io.branch.branchandroidtestbed;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import io.branch.referral.BranchConfiguration;
import io.branch.referral.Defines;

/**
 * Debug-only switches for the L1 install-referrer scenarios, read from files/l1_launch.properties,
 * which scripts/run_l1_referrer.sh writes before the app's first launch. Without the file, nothing
 * changes.
 */
final class L1Hooks {
    private static final String FILE = "l1_launch.properties";

    private L1Hooks() {
    }

    /** The file's branch_key when it has one, else defaultKey. */
    static String branchKey(Context context, String defaultKey) {
        return read(context).getProperty("branch_key", defaultKey);
    }

    /** Applies the file's attribution_level: NONE, MINIMAL, REDUCED or FULL. */
    static void applyLaunchConfig(Context context, BranchConfiguration.Builder config) {
        String level = read(context).getProperty("attribution_level");
        if (level != null) {
            config.setAttributionLevel(Defines.BranchAttributionLevel.valueOf(level));
        }
    }

    /** True for deep_link_call=uri: the app passes its intent's data instead of itself. */
    static boolean useUriCall(Context context) {
        return "uri".equals(read(context).getProperty("deep_link_call"));
    }

    private static Properties read(Context context) {
        Properties properties = new Properties();
        File file = new File(context.getFilesDir(), FILE);
        if (file.exists()) {
            try (FileInputStream in = new FileInputStream(file)) {
                properties.load(in);
            } catch (Exception e) {
                Log.w("BranchTestbed", "Unreadable " + FILE + ": " + e);
            }
        }
        return properties;
    }
}
