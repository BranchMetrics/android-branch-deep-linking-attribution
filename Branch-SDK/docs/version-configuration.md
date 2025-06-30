# Branch SDK Version Configuration

## Overview

The Branch SDK now supports configurable deprecation and removal timelines through external configuration files. This allows for flexible management of API lifecycle without requiring code changes.

## Configuration Files

### Location
Configuration files should be placed in `src/main/assets/` directory:
- `branch_version_config.properties` - Production configuration
- `branch_version_config.development.properties` - Development configuration (example)

### Configuration Properties

| Property | Description | Example |
|----------|-------------|---------|
| `branch.api.deprecation.version` | Version when APIs are marked as deprecated | `5.0.0` |
| `branch.api.removal.version` | Version when deprecated APIs will be removed | `6.0.0` |
| `branch.migration.guide.url` | URL to migration documentation | `https://branch.io/migration-guide` |

### Example Configuration

```properties
# Branch SDK Version Configuration
branch.api.deprecation.version=5.0.0
branch.api.removal.version=6.0.0
branch.migration.guide.url=https://branch.io/migration-guide
```

## Usage

### Initialization

The version configuration is automatically loaded when the `BranchApiPreservationManager` is initialized:

```kotlin
val preservationManager = BranchApiPreservationManager.getInstance(context)
```

### Accessing Configuration

```kotlin
val versionConfig = VersionConfigurationFactory.createConfiguration(context)
val deprecationVersion = versionConfig.getDeprecationVersion()
val removalVersion = versionConfig.getRemovalVersion()
val migrationGuideUrl = versionConfig.getMigrationGuideUrl()
```

### API-Specific Version Management

Each API can have its own deprecation and removal timeline:

```kotlin
// Register API with specific versions
publicApiRegistry.registerApi(
    methodName = "generateShortUrl",
    signature = "BranchUniversalObject.generateShortUrl()",
    usageImpact = UsageImpact.CRITICAL,
    complexity = MigrationComplexity.COMPLEX,
    removalTimeline = "Q4 2025",
    modernReplacement = "linkManager.createShortLink()",
    deprecationVersion = "5.0.0", // Specific deprecation version
    removalVersion = "7.0.0"      // Extended removal due to complexity
)

// Get APIs by version
val apisToDeprecateIn5_0 = preservationManager.getApisForDeprecationInVersion("5.0.0")
val apisToRemoveIn6_0 = preservationManager.getApisForRemovalInVersion("6.0.0")
```

### Version Timeline Reports

Generate comprehensive reports for release planning:

```kotlin
val timelineReport = preservationManager.generateVersionTimelineReport()

// Access timeline details
timelineReport.versionDetails.forEach { versionDetail ->
    println("Version ${versionDetail.version}:")
    println("  - ${versionDetail.deprecatedApis.size} APIs deprecated")
    println("  - ${versionDetail.removedApis.size} APIs removed")
    println("  - Breaking changes: ${versionDetail.hasBreakingChanges}")
}

// Access summary statistics
val summary = timelineReport.summary
println("Busiest version: ${summary.busiestVersion}")
println("Max removals in single version: ${summary.maxRemovalsInSingleVersion}")
```

## Architecture

### Components

1. **VersionConfiguration** - Interface for version configuration access
2. **PropertiesVersionConfiguration** - Implementation that reads from properties files
3. **VersionConfigurationFactory** - Factory for creating configuration instances
4. **PublicApiRegistry** - Uses configuration for API metadata
5. **BranchApiPreservationManager** - Coordinates configuration usage

### Benefits

- **Flexibility**: Change versions without code modifications
- **Environment-specific**: Different configurations for dev/staging/prod
- **Centralized**: Single source of truth for version information
- **Thread-safe**: Singleton pattern with proper synchronization
- **Fallback**: Default values when configuration file is missing

## Migration from Hardcoded Values

### Before
```kotlin
private const val DEPRECATION_VERSION = "5.0.0"
private const val REMOVAL_VERSION = "6.0.0"
```

### After
```kotlin
private val versionConfiguration: VersionConfiguration
val deprecationVersion = versionConfiguration.getDeprecationVersion()
val removalVersion = versionConfiguration.getRemovalVersion()
```

## Version Strategy Examples

### Conservative Approach (High Stability)
```kotlin
// Critical APIs - Extended timeline
deprecationVersion = "5.0.0"
removalVersion = "7.0.0"  // 2 major versions later

// High impact APIs - Standard timeline  
deprecationVersion = "5.0.0"
removalVersion = "6.0.0"  // 1 major version later
```

### Aggressive Approach (Fast Modernization)
```kotlin
// Performance-critical APIs - Accelerated removal
deprecationVersion = "4.0.0"
removalVersion = "5.0.0"  // Same major version

// Blocking APIs - Immediate removal
deprecationVersion = "4.5.0"
removalVersion = "5.0.0"  // Next major version
```

### Balanced Approach (Recommended)
```kotlin
// Categorize by impact and complexity:
// - Critical + Complex: 5.0.0 → 7.0.0 (extended)
// - High + Medium: 5.0.0 → 6.5.0 (standard+)
// - Medium + Simple: 5.0.0 → 6.0.0 (standard)
// - Low + Simple: 4.5.0 → 5.5.0 (accelerated)
```

## Best Practices

1. **Version Consistency**: Ensure all environments use consistent versioning schemes
2. **Impact-Based Scheduling**: Schedule based on usage impact and migration complexity
3. **Documentation**: Update migration guides when versions change
4. **Testing**: Test configuration loading in different scenarios
5. **Validation**: Validate version format and logical consistency
6. **Monitoring**: Track configuration loading success/failure
7. **Timeline Communication**: Clearly communicate timelines to developers
8. **Gradual Migration**: Provide overlapping support periods for smooth transitions

## Error Handling

The system gracefully handles configuration errors:
- Missing files: Falls back to default values
- Invalid format: Logs warning and uses defaults
- Access errors: Handles IOException gracefully

## Future Enhancements

Planned improvements:
- JSON configuration support
- Remote configuration loading
- Version validation rules
- Configuration hot-reloading 