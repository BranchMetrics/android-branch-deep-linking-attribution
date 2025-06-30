package io.branch.referral.modernization.core

import android.content.Context
import android.util.Log
import java.io.IOException
import java.util.Properties

/**
 * Configuration interface for API version management.
 * 
 * This interface follows the Interface Segregation Principle by providing
 * only the necessary methods for version configuration management.
 */
interface VersionConfiguration {
    fun getDeprecationVersion(): String
    fun getRemovalVersion(): String
    fun getMigrationGuideUrl(): String
    fun isConfigurationLoaded(): Boolean
}

/**
 * Implementation of version configuration that reads from properties files.
 * 
 * This class implements the Single Responsibility Principle by focusing solely
 * on configuration management and the Dependency Inversion Principle by
 * depending on abstractions rather than concrete implementations.
 */
class PropertiesVersionConfiguration private constructor(
    private val context: Context
) : VersionConfiguration {
    
    private val properties = Properties()
    private var configurationLoaded = false
    private val configFileName = "branch_version_config.properties"
    
    // Default fallback values
    private val defaultDeprecationVersion = "5.0.0"
    private val defaultRemovalVersion = "6.0.0"
    private val defaultMigrationGuideUrl = "https://branch.io/migration-guide"
    
    companion object {
        private const val TAG = "VersionConfiguration"
        private const val DEPRECATION_VERSION_KEY = "branch.api.deprecation.version"
        private const val REMOVAL_VERSION_KEY = "branch.api.removal.version"
        private const val MIGRATION_GUIDE_URL_KEY = "branch.migration.guide.url"
        
        @Volatile
        private var instance: PropertiesVersionConfiguration? = null
        
        /**
         * Get singleton instance with thread-safe initialization.
         */
        fun getInstance(context: Context): PropertiesVersionConfiguration {
            return instance ?: synchronized(this) {
                instance ?: PropertiesVersionConfiguration(context.applicationContext).also { 
                    instance = it 
                    it.loadConfiguration()
                }
            }
        }
    }
    
    /**
     * Load configuration from properties file in assets.
     */
    private fun loadConfiguration() {
        try {
            context.assets.open(configFileName).use { inputStream ->
                properties.load(inputStream)
                configurationLoaded = true
                Log.i(TAG, "Version configuration loaded successfully from $configFileName")
            }
        } catch (e: IOException) {
            Log.w(TAG, "Failed to load $configFileName, using default values", e)
            loadDefaultValues()
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error loading configuration", e)
            loadDefaultValues()
        }
    }
    
    /**
     * Load default configuration values when file is not available.
     */
    private fun loadDefaultValues() {
        properties.setProperty(DEPRECATION_VERSION_KEY, defaultDeprecationVersion)
        properties.setProperty(REMOVAL_VERSION_KEY, defaultRemovalVersion)
        properties.setProperty(MIGRATION_GUIDE_URL_KEY, defaultMigrationGuideUrl)
        configurationLoaded = true
    }
    
    override fun getDeprecationVersion(): String {
        return properties.getProperty(DEPRECATION_VERSION_KEY, defaultDeprecationVersion)
    }
    
    override fun getRemovalVersion(): String {
        return properties.getProperty(REMOVAL_VERSION_KEY, defaultRemovalVersion)
    }
    
    override fun getMigrationGuideUrl(): String {
        return properties.getProperty(MIGRATION_GUIDE_URL_KEY, defaultMigrationGuideUrl)
    }
    
    override fun isConfigurationLoaded(): Boolean = configurationLoaded
    
    /**
     * Get all configuration properties for debugging purposes.
     */
    fun getAllProperties(): Map<String, String> {
        return properties.entries.associate { (key, value) ->
            key.toString() to value.toString()
        }
    }
    
    /**
     * Reload configuration from file.
     * Useful for runtime configuration updates.
     */
    fun reloadConfiguration() {
        loadConfiguration()
    }
}

/**
 * Factory for creating version configuration instances.
 * 
 * This factory follows the Dependency Inversion Principle by allowing
 * different configuration implementations to be created based on requirements.
 */
object VersionConfigurationFactory {
    
    /**
     * Create a version configuration instance.
     * 
     * @param context Android context for resource access
     * @param configType Type of configuration to create
     * @return VersionConfiguration instance
     */
    fun createConfiguration(
        context: Context,
        configType: ConfigurationType = ConfigurationType.PROPERTIES
    ): VersionConfiguration {
        return when (configType) {
            ConfigurationType.PROPERTIES -> PropertiesVersionConfiguration.getInstance(context)
        }
    }
}

/**
 * Supported configuration types.
 */
enum class ConfigurationType {
    PROPERTIES
} 