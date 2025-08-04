package io.branch.referral;

import android.content.Context;
import android.text.TextUtils;
import androidx.annotation.NonNull;

/**
 * Modular configuration manager for Branch SDK initialization.
 * 
 * This class handles all configuration loading that was previously done in the
 * getAutoInstance method, ensuring proper separation of concerns and modularity.
 * 
 * Follows Single Responsibility Principle by focusing solely on configuration management.
 * Follows Dependency Inversion Principle by depending on abstractions (BranchJsonConfig).
 */
public class BranchConfigurationManager {
    
    private static final String TAG = "BranchConfigurationManager";
    
    /**
     * Loads all configuration settings from various sources.
     * 
     * This method centralizes all configuration loading logic that was previously
     * scattered across the getAutoInstance method, ensuring proper initialization
     * order and error handling.
     * 
     * @param context Android context for resource access
     * @param branchInstance The Branch instance to configure
     */
    public static void loadConfiguration(@NonNull Context context, @NonNull Branch branchInstance) {
        try {
            // Load logging configuration
            loadLoggingConfiguration(context);
            
            // Load plugin runtime configuration
            loadPluginRuntimeConfiguration(context);
            
            // Load API configuration
            loadApiConfiguration(context);
            
            // Load Facebook configuration
            loadFacebookConfiguration(context);
            
            // Load consumer protection configuration
            loadConsumerProtectionConfiguration(context, branchInstance);
            
            // Load test mode configuration
            loadTestModeConfiguration(context);
            
            // Load preinstall system data
            loadPreinstallSystemData(branchInstance, context);
            
            BranchLogger.v("Branch configuration loaded successfully");
            
        } catch (Exception e) {
            BranchLogger.e("Failed to load Branch configuration: " + e.getMessage());
            // Continue with default configuration to avoid breaking initialization
        }
    }
    
    /**
     * Loads logging configuration from branch.json.
     * 
     * @param context Android context for resource access
     */
    private static void loadLoggingConfiguration(@NonNull Context context) {
        if (BranchUtil.getEnableLoggingConfig(context)) {
            Branch.enableLogging();
            BranchLogger.v("Logging enabled from configuration");
        }
    }
    
    /**
     * Loads plugin runtime configuration from branch.json.
     * 
     * @param context Android context for resource access
     */
    private static void loadPluginRuntimeConfiguration(@NonNull Context context) {
        boolean deferInitForPluginRuntime = BranchUtil.getDeferInitForPluginRuntimeConfig(context);
        Branch.deferInitForPluginRuntime(deferInitForPluginRuntime);
        
        if (deferInitForPluginRuntime) {
            BranchLogger.v("Plugin runtime initialization deferred from configuration");
        }
    }
    
    /**
     * Loads API configuration from branch.json.
     * 
     * @param context Android context for resource access
     */
    private static void loadApiConfiguration(@NonNull Context context) {
        BranchUtil.setAPIBaseUrlFromConfig(context);
        BranchLogger.v("API configuration loaded from branch.json");
    }
    
    /**
     * Loads Facebook configuration from branch.json.
     * 
     * @param context Android context for resource access
     */
    private static void loadFacebookConfiguration(@NonNull Context context) {
        BranchUtil.setFbAppIdFromConfig(context);
        BranchLogger.v("Facebook configuration loaded from branch.json");
    }
    
    /**
     * Loads consumer protection configuration from branch.json.
     * 
     * @param context Android context for resource access
     * @param branchInstance The Branch instance to configure
     */
    private static void loadConsumerProtectionConfiguration(@NonNull Context context, @NonNull Branch branchInstance) {
        BranchUtil.setCPPLevelFromConfig(context);
        BranchLogger.v("Consumer protection configuration loaded from branch.json");
    }
    
    /**
     * Loads test mode configuration from branch.json and manifest.
     * 
     * @param context Android context for resource access
     */
    private static void loadTestModeConfiguration(@NonNull Context context) {
        BranchUtil.setTestMode(BranchUtil.checkTestMode(context));
        BranchLogger.v("Test mode configuration loaded from configuration");
    }
    
    /**
     * Loads preinstall system data if available.
     * 
     * @param branchInstance The Branch instance to configure
     * @param context Android context for resource access
     */
    private static void loadPreinstallSystemData(@NonNull Branch branchInstance, @NonNull Context context) {
        BranchPreinstall.getPreinstallSystemData(branchInstance, context);
        BranchLogger.v("Preinstall system data loaded");
    }
    
    /**
     * Validates that essential configuration is present.
     * 
     * @param context Android context for resource access
     * @return true if essential configuration is valid, false otherwise
     */
    public static boolean validateConfiguration(@NonNull Context context) {
        String branchKey = BranchUtil.readBranchKey(context);
        if (TextUtils.isEmpty(branchKey)) {
            BranchLogger.w("Branch key is missing from configuration");
            return false;
        }
        
        BranchLogger.v("Configuration validation passed");
        return true;
    }
    
    /**
     * Resets all configuration to default values.
     * 
     * This method is useful for testing or when configuration needs to be cleared.
     */
    public static void resetConfiguration() {
        Branch.disableTestMode();
        Branch.disableLogging();
        Branch.deferInitForPluginRuntime(false);
        BranchLogger.v("Configuration reset to default values");
    }
} 