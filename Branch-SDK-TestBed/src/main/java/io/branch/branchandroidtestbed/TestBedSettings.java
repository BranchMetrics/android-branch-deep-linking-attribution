package io.branch.branchandroidtestbed;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Holds the pre-init values the settings screen can change.
 *
 * <p>{@link io.branch.referral.BranchConfiguration} is immutable once
 * {@link io.branch.referral.Branch#initialize} has run, so anything the user edits at runtime is
 * persisted here and read back into the builder on the next launch. This is the pattern an
 * integrator uses for settings that need to be operator-configurable but are pre-init only.</p>
 */
final class TestBedSettings {

    private static final String PREFS_NAME = "branch_testbed_settings";
    private static final String KEY_API_URL = "api_url";
    private static final String KEY_ENABLE_DMA_PARAMS = "enable_dma_params";

    private TestBedSettings() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /** @return the operator-set API base URL, or null to use the SDK default. */
    static String getApiUrl(Context context) {
        return prefs(context).getString(KEY_API_URL, null);
    }

    /** Takes effect on the next launch, when CustomBranchApp builds the configuration. */
    static void setApiUrl(Context context, String apiUrl) {
        prefs(context).edit().putString(KEY_API_URL, apiUrl).apply();
    }

    /**
     * Off by default, so a fresh checkout doesn't declare EEA with fixed consent values on every
     * launch — that would mislead anyone reading a TestBed log capture.
     */
    static boolean isDmaParamsEnabled(Context context) {
        return prefs(context).getBoolean(KEY_ENABLE_DMA_PARAMS, false);
    }

    /** Takes effect on the next launch, when CustomBranchApp builds the configuration. */
    static void setDmaParamsEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_ENABLE_DMA_PARAMS, enabled).apply();
    }
}
