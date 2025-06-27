package io.branch.referral.modernization.tools

import io.branch.referral.modernization.registry.ApiCategory
import io.branch.referral.modernization.registry.ApiCriticality
import java.io.File
import java.util.*

/**
 * Configuration system for selective API generation.
 * Allows fine-grained control over which APIs to include or exclude.
 */
data class ApiFilterConfig(
    val includeClasses: Set<String> = emptySet(),
    val excludeClasses: Set<String> = emptySet(),
    val includeApis: Set<String> = emptySet(),
    val excludeApis: Set<String> = emptySet(),
    val includeCategories: Set<ApiCategory> = emptySet(),
    val excludeCategories: Set<ApiCategory> = emptySet(),
    val includeCriticalities: Set<ApiCriticality> = emptySet(),
    val excludeCriticalities: Set<ApiCriticality> = emptySet(),
    val includePatterns: List<Regex> = emptyList(),
    val excludePatterns: List<Regex> = emptyList(),
    val minParameterCount: Int? = null,
    val maxParameterCount: Int? = null,
    val includeDeprecated: Boolean = true,
    val includeStatic: Boolean = false
) {
    
    /**
     * Checks if an API should be included based on this configuration.
     */
    fun shouldIncludeApi(api: ApiMethodInfo, category: ApiCategory, criticality: ApiCriticality): Boolean {
        // Apply exclusion filters first (more restrictive)
        if (shouldExcludeApi(api, category, criticality)) {
            return false
        }
        
        // If no inclusion filters are set, include by default
        if (hasNoInclusionFilters()) {
            return true
        }
        
        // Apply inclusion filters
        return shouldIncludeByFilters(api, category, criticality)
    }
    
    private fun shouldExcludeApi(api: ApiMethodInfo, category: ApiCategory, criticality: ApiCriticality): Boolean {
        // Exclude by class
        if (excludeClasses.isNotEmpty() && api.className in excludeClasses) {
            return true
        }
        
        // Exclude by API name
        if (excludeApis.isNotEmpty() && api.methodName in excludeApis) {
            return true
        }
        
        // Exclude by category
        if (excludeCategories.isNotEmpty() && category in excludeCategories) {
            return true
        }
        
        // Exclude by criticality
        if (excludeCriticalities.isNotEmpty() && criticality in excludeCriticalities) {
            return true
        }
        
        // Exclude by patterns
        if (excludePatterns.any { it.matches(api.methodName) }) {
            return true
        }
        
        // Exclude by parameter count
        if (minParameterCount != null && api.parameterTypes.size < minParameterCount) {
            return true
        }
        if (maxParameterCount != null && api.parameterTypes.size > maxParameterCount) {
            return true
        }
        
        // Exclude static methods if not included
        if (!includeStatic && api.isStatic) {
            return true
        }
        
        return false
    }
    
    private fun hasNoInclusionFilters(): Boolean {
        return includeClasses.isEmpty() && 
               includeApis.isEmpty() && 
               includeCategories.isEmpty() && 
               includeCriticalities.isEmpty() && 
               includePatterns.isEmpty()
    }
    
    private fun shouldIncludeByFilters(api: ApiMethodInfo, category: ApiCategory, criticality: ApiCriticality): Boolean {
        // Include by class
        if (includeClasses.isNotEmpty() && api.className in includeClasses) {
            return true
        }
        
        // Include by API name
        if (includeApis.isNotEmpty() && api.methodName in includeApis) {
            return true
        }
        
        // Include by category
        if (includeCategories.isNotEmpty() && category in includeCategories) {
            return true
        }
        
        // Include by criticality
        if (includeCriticalities.isNotEmpty() && criticality in includeCriticalities) {
            return true
        }
        
        // Include by patterns
        if (includePatterns.any { it.matches(api.methodName) }) {
            return true
        }
        
        return false
    }
    
    /**
     * Prints a summary of the current filter configuration.
     */
    fun printSummary() {
        println("=== API Filter Configuration ===")
        
        if (includeClasses.isNotEmpty()) {
            println("Include Classes: ${includeClasses.joinToString(", ")}")
        }
        if (excludeClasses.isNotEmpty()) {
            println("Exclude Classes: ${excludeClasses.joinToString(", ")}")
        }
        if (includeApis.isNotEmpty()) {
            println("Include APIs: ${includeApis.joinToString(", ")}")
        }
        if (excludeApis.isNotEmpty()) {
            println("Exclude APIs: ${excludeApis.joinToString(", ")}")
        }
        if (includeCategories.isNotEmpty()) {
            println("Include Categories: ${includeCategories.joinToString(", ")}")
        }
        if (excludeCategories.isNotEmpty()) {
            println("Exclude Categories: ${excludeCategories.joinToString(", ")}")
        }
        if (includeCriticalities.isNotEmpty()) {
            println("Include Criticalities: ${includeCriticalities.joinToString(", ")}")
        }
        if (excludeCriticalities.isNotEmpty()) {
            println("Exclude Criticalities: ${excludeCriticalities.joinToString(", ")}")
        }
        if (includePatterns.isNotEmpty()) {
            println("Include Patterns: ${includePatterns.map { it.pattern }.joinToString(", ")}")
        }
        if (excludePatterns.isNotEmpty()) {
            println("Exclude Patterns: ${excludePatterns.map { it.pattern }.joinToString(", ")}")
        }
        
        minParameterCount?.let { println("Min Parameters: $it") }
        maxParameterCount?.let { println("Max Parameters: $it") }
        println("Include Deprecated: $includeDeprecated")
        println("Include Static: $includeStatic")
    }
    
    companion object {
        /**
         * Creates a configuration from a properties file.
         */
        fun fromPropertiesFile(filePath: String): ApiFilterConfig {
            val props = Properties()
            File(filePath).inputStream().use { props.load(it) }
            return fromProperties(props)
        }
        
        /**
         * Creates a configuration from Properties object.
         */
        fun fromProperties(props: Properties): ApiFilterConfig {
            return ApiFilterConfig(
                includeClasses = props.getProperty("include.classes", "")
                    .split(",").filter { it.isNotBlank() }.toSet(),
                excludeClasses = props.getProperty("exclude.classes", "")
                    .split(",").filter { it.isNotBlank() }.toSet(),
                includeApis = props.getProperty("include.apis", "")
                    .split(",").filter { it.isNotBlank() }.toSet(),
                excludeApis = props.getProperty("exclude.apis", "")
                    .split(",").filter { it.isNotBlank() }.toSet(),
                includeCategories = props.getProperty("include.categories", "")
                    .split(",").filter { it.isNotBlank() }
                    .mapNotNull { runCatching { ApiCategory.valueOf(it.trim()) }.getOrNull() }.toSet(),
                excludeCategories = props.getProperty("exclude.categories", "")
                    .split(",").filter { it.isNotBlank() }
                    .mapNotNull { runCatching { ApiCategory.valueOf(it.trim()) }.getOrNull() }.toSet(),
                includeCriticalities = props.getProperty("include.criticalities", "")
                    .split(",").filter { it.isNotBlank() }
                    .mapNotNull { runCatching { ApiCriticality.valueOf(it.trim()) }.getOrNull() }.toSet(),
                excludeCriticalities = props.getProperty("exclude.criticalities", "")
                    .split(",").filter { it.isNotBlank() }
                    .mapNotNull { runCatching { ApiCriticality.valueOf(it.trim()) }.getOrNull() }.toSet(),
                includePatterns = props.getProperty("include.patterns", "")
                    .split(",").filter { it.isNotBlank() }
                    .map { Regex(it.trim()) },
                excludePatterns = props.getProperty("exclude.patterns", "")
                    .split(",").filter { it.isNotBlank() }
                    .map { Regex(it.trim()) },
                minParameterCount = props.getProperty("min.parameters")?.toIntOrNull(),
                maxParameterCount = props.getProperty("max.parameters")?.toIntOrNull(),
                includeDeprecated = props.getProperty("include.deprecated", "true").toBoolean(),
                includeStatic = props.getProperty("include.static", "false").toBoolean()
            )
        }
        
        /**
         * Predefined configurations for common scenarios.
         */
        object Presets {
            val CORE_APIS_ONLY = ApiFilterConfig(
                includeCategories = setOf(ApiCategory.CORE),
                includeCriticalities = setOf(ApiCriticality.HIGH)
            )
            
            val SESSION_MANAGEMENT = ApiFilterConfig(
                includeCategories = setOf(ApiCategory.SESSION, ApiCategory.IDENTITY)
            )
            
            val LINK_HANDLING = ApiFilterConfig(
                includeCategories = setOf(ApiCategory.LINK)
            )
            
            val EVENT_LOGGING = ApiFilterConfig(
                includeCategories = setOf(ApiCategory.EVENT)
            )
            
            val HIGH_PRIORITY_ONLY = ApiFilterConfig(
                includeCriticalities = setOf(ApiCriticality.HIGH)
            )
            
            val BRANCH_CLASS_ONLY = ApiFilterConfig(
                includeClasses = setOf("Branch")
            )
            
            val NO_SYNC_METHODS = ApiFilterConfig(
                excludePatterns = listOf(Regex(".*Sync$"))
            )
            
            val SIMPLE_METHODS = ApiFilterConfig(
                maxParameterCount = 2
            )
        }
    }
}

/**
 * Builder for creating filter configurations programmatically.
 */
class ApiFilterConfigBuilder {
    private var includeClasses = mutableSetOf<String>()
    private var excludeClasses = mutableSetOf<String>()
    private var includeApis = mutableSetOf<String>()
    private var excludeApis = mutableSetOf<String>()
    private var includeCategories = mutableSetOf<ApiCategory>()
    private var excludeCategories = mutableSetOf<ApiCategory>()
    private var includeCriticalities = mutableSetOf<ApiCriticality>()
    private var excludeCriticalities = mutableSetOf<ApiCriticality>()
    private var includePatterns = mutableListOf<Regex>()
    private var excludePatterns = mutableListOf<Regex>()
    private var minParameterCount: Int? = null
    private var maxParameterCount: Int? = null
    private var includeDeprecated = true
    private var includeStatic = false
    
    fun includeClass(className: String) = apply { includeClasses.add(className) }
    fun excludeClass(className: String) = apply { excludeClasses.add(className) }
    fun includeApi(apiName: String) = apply { includeApis.add(apiName) }
    fun excludeApi(apiName: String) = apply { excludeApis.add(apiName) }
    fun includeCategory(category: ApiCategory) = apply { includeCategories.add(category) }
    fun excludeCategory(category: ApiCategory) = apply { excludeCategories.add(category) }
    fun includeCriticality(criticality: ApiCriticality) = apply { includeCriticalities.add(criticality) }
    fun excludeCriticality(criticality: ApiCriticality) = apply { excludeCriticalities.add(criticality) }
    fun includePattern(pattern: String) = apply { includePatterns.add(Regex(pattern)) }
    fun excludePattern(pattern: String) = apply { excludePatterns.add(Regex(pattern)) }
    fun withMinParameters(min: Int) = apply { minParameterCount = min }
    fun withMaxParameters(max: Int) = apply { maxParameterCount = max }
    fun includeDeprecated(include: Boolean) = apply { includeDeprecated = include }
    fun includeStatic(include: Boolean) = apply { includeStatic = include }
    
    fun build() = ApiFilterConfig(
        includeClasses = includeClasses.toSet(),
        excludeClasses = excludeClasses.toSet(),
        includeApis = includeApis.toSet(),
        excludeApis = excludeApis.toSet(),
        includeCategories = includeCategories.toSet(),
        excludeCategories = excludeCategories.toSet(),
        includeCriticalities = includeCriticalities.toSet(),
        excludeCriticalities = excludeCriticalities.toSet(),
        includePatterns = includePatterns.toList(),
        excludePatterns = excludePatterns.toList(),
        minParameterCount = minParameterCount,
        maxParameterCount = maxParameterCount,
        includeDeprecated = includeDeprecated,
        includeStatic = includeStatic
    )
} 