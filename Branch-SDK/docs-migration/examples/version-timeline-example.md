# Branch SDK Version Timeline: Practical Usage Examples

**Document Type:** Practical Examples and Use Cases  
**Created:** June 2025  
**Last Updated:** June 2025  
**Version:** 1.0  
**Author:** Branch SDK Team  

---

## Document Purpose

This document provides detailed practical examples of how to use the API-specific versioning system to plan releases, generate migration reports, and integrate with CI/CD pipelines. It includes complete code examples and real-world usage scenarios.

## Overview

This document demonstrates how to use the API-specific versioning system to plan releases and communicate changes to developers.

## Practical Examples

### 1. API Configuration with Different Timelines

```kotlin
class ApiRegistrationExample {
    fun registerExampleApis(registry: PublicApiRegistry) {
        // Critical APIs - Extended Timeline
        registry.registerApi(
            methodName = "getInstance",
            signature = "Branch.getInstance()",
            usageImpact = UsageImpact.CRITICAL,
            complexity = MigrationComplexity.SIMPLE,
            removalTimeline = "Q2 2025",
            modernReplacement = "ModernBranchCore.getInstance()",
            deprecationVersion = "5.0.0",
            removalVersion = "7.0.0" // Extended support
        )
        
        // Problematic APIs - Accelerated Removal
        registry.registerApi(
            methodName = "getFirstReferringParamsSync",
            signature = "Branch.getFirstReferringParamsSync()",
            usageImpact = UsageImpact.MEDIUM,
            complexity = MigrationComplexity.COMPLEX,
            removalTimeline = "Q1 2025",
            modernReplacement = "dataManager.getFirstReferringParamsAsync()",
            breakingChanges = listOf("Converted from synchronous to asynchronous operation"),
            deprecationVersion = "4.0.0", // Early deprecation
            removalVersion = "5.0.0"     // Fast removal due to performance impact
        )
        
        // Standard APIs - Normal Timeline
        registry.registerApi(
            methodName = "setIdentity",
            signature = "Branch.setIdentity(String)",
            usageImpact = UsageImpact.HIGH,
            complexity = MigrationComplexity.SIMPLE,
            removalTimeline = "Q3 2025",
            modernReplacement = "identityManager.setIdentity(String)",
            deprecationVersion = "5.0.0",
            removalVersion = "6.0.0" // Standard timeline
        )
    }
}
```

### 2. Timeline Report Generation

```kotlin
class ReleaseManager {
    fun generateReleaseReport(context: Context) {
        val preservationManager = BranchApiPreservationManager.getInstance(context)
        
        // Complete timeline report
        val timelineReport = preservationManager.generateVersionTimelineReport()
        
        println("=== BRANCH SDK VERSION TIMELINE ===")
        println("Total versions with changes: ${timelineReport.totalVersions}")
        println("Busiest version: ${timelineReport.summary.busiestVersion}")
        println()
        
        // Details by version
        timelineReport.versionDetails.forEach { versionDetail ->
            println("Version ${versionDetail.version}:")
            
            if (versionDetail.deprecatedApis.isNotEmpty()) {
                println("  📢 APIs Deprecated (${versionDetail.deprecatedApis.size}):")
                versionDetail.deprecatedApis.forEach { api ->
                    println("    - ${api.methodName} (${api.usageImpact})")
                }
            }
            
            if (versionDetail.removedApis.isNotEmpty()) {
                println("  🚨 APIs Removed (${versionDetail.removedApis.size}):")
                versionDetail.removedApis.forEach { api ->
                    println("    - ${api.methodName} → ${api.modernReplacement}")
                    if (api.breakingChanges.isNotEmpty()) {
                        println("      ⚠️  Breaking: ${api.breakingChanges.joinToString()}")
                    }
                }
            }
            
            if (versionDetail.hasBreakingChanges) {
                println("  ⚡ BREAKING CHANGES IN THIS VERSION")
            }
            
            println()
        }
    }
    
    fun generateVersionSpecificReport(context: Context, targetVersion: String) {
        val preservationManager = BranchApiPreservationManager.getInstance(context)
        
        println("=== CHANGES IN VERSION $targetVersion ===")
        
        // APIs being deprecated in this version
        val deprecatedApis = preservationManager.getApisForDeprecationInVersion(targetVersion)
        if (deprecatedApis.isNotEmpty()) {
            println("\n📢 APIs Deprecated in $targetVersion:")
            deprecatedApis.forEach { api ->
                println("  - ${api.signature}")
                println("    Impact: ${api.usageImpact}, Complexity: ${api.migrationComplexity}")
                println("    Modern Alternative: ${api.modernReplacement}")
                if (api.removalVersion != targetVersion) {
                    println("    Will be removed in: ${api.removalVersion}")
                }
            }
        }
        
        // APIs being removed in this version
        val removedApis = preservationManager.getApisForRemovalInVersion(targetVersion)
        if (removedApis.isNotEmpty()) {
            println("\n🚨 APIs Removed in $targetVersion:")
            removedApis.forEach { api ->
                println("  - ${api.signature}")
                println("    Deprecated since: ${api.deprecationVersion}")
                println("    Migration: ${api.modernReplacement}")
                if (api.breakingChanges.isNotEmpty()) {
                    println("    Breaking Changes:")
                    api.breakingChanges.forEach { change ->
                        println("      • $change")
                    }
                }
            }
        }
        
        if (deprecatedApis.isEmpty() && removedApis.isEmpty()) {
            println("No API changes in version $targetVersion")
        }
    }
}
```

### 3. Report Output Example

```
=== BRANCH SDK VERSION TIMELINE ===
Total versions with changes: 6
Busiest version: 5.0.0

Version 4.0.0:
  📢 APIs Deprecated (1):
    - getFirstReferringParamsSync (MEDIUM)

Version 4.5.0:
  📢 APIs Deprecated (1):
    - enableTestMode (MEDIUM)

Version 5.0.0:
  📢 APIs Deprecated (3):
    - getInstance (CRITICAL)
    - initSession (CRITICAL)
    - setIdentity (HIGH)
  🚨 APIs Removed (1):
    - getFirstReferringParamsSync → dataManager.getFirstReferringParamsAsync()
      ⚠️  Breaking: Converted from synchronous to asynchronous operation
  ⚡ BREAKING CHANGES IN THIS VERSION

Version 5.5.0:
  🚨 APIs Removed (1):
    - enableTestMode → configManager.enableTestMode()

Version 6.0.0:
  🚨 APIs Removed (3):
    - resetUserSession → sessionManager.resetSession()
    - setIdentity → identityManager.setIdentity(String)
    - logout → identityManager.logout()
  ⚡ BREAKING CHANGES IN THIS VERSION

Version 6.5.0:
  🚨 APIs Removed (2):
    - initSession → sessionManager.initSession()
    - logEvent → eventManager.logEvent()
  ⚡ BREAKING CHANGES IN THIS VERSION

Version 7.0.0:
  🚨 APIs Removed (2):
    - getInstance → ModernBranchCore.getInstance()
    - generateShortUrl → linkManager.createShortLink()
  ⚡ BREAKING CHANGES IN THIS VERSION
```

### 4. CI/CD Integration

```kotlin
class ContinuousIntegration {
    fun validateReleaseChanges(context: Context, plannedVersion: String) {
        val preservationManager = BranchApiPreservationManager.getInstance(context)
        
        // Check for breaking changes in the planned version
        val removedApis = preservationManager.getApisForRemovalInVersion(plannedVersion)
        
        if (removedApis.isNotEmpty()) {
            println("⚠️  WARNING: Version $plannedVersion contains ${removedApis.size} breaking changes")
            
            // Check if it's a major version (can have breaking changes)
            val isMajorVersion = plannedVersion.split(".")[0].toInt() > 
                                getCurrentVersion().split(".")[0].toInt()
            
            if (!isMajorVersion) {
                throw IllegalStateException(
                    "Breaking changes detected in non-major version $plannedVersion"
                )
            }
        }
        
        // Generate automatic changelog
        generateChangelogForVersion(preservationManager, plannedVersion)
    }
    
    private fun generateChangelogForVersion(
        manager: BranchApiPreservationManager, 
        version: String
    ) {
        val deprecated = manager.getApisForDeprecationInVersion(version)
        val removed = manager.getApisForRemovalInVersion(version)
        
        val changelog = buildString {
            appendLine("# Changelog for Version $version")
            appendLine()
            
            if (removed.isNotEmpty()) {
                appendLine("## 🚨 Breaking Changes")
                removed.forEach { api ->
                    appendLine("- **REMOVED**: `${api.signature}`")
                    appendLine("  - **Migration**: Use `${api.modernReplacement}` instead")
                    api.breakingChanges.forEach { change ->
                        appendLine("  - **Breaking**: $change")
                    }
                    appendLine()
                }
            }
            
            if (deprecated.isNotEmpty()) {
                appendLine("## 📢 Deprecated APIs")
                deprecated.forEach { api ->
                    appendLine("- **DEPRECATED**: `${api.signature}`")
                    appendLine("  - **Alternative**: Use `${api.modernReplacement}`")
                    appendLine("  - **Removal**: Scheduled for version ${api.removalVersion}")
                    appendLine()
                }
            }
        }
        
        // Save changelog
        writeChangelogToFile(changelog, version)
    }
}
```

## System Benefits

1. **Flexibility**: Each API can have its own timeline
2. **Planning**: Detailed reports for release planning
3. **Communication**: Clear information for developers
4. **Automation**: Integration with CI/CD pipelines
5. **Gradual Migration**: Enables smooth and controlled migration 