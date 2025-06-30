# Branch SDK Version Timeline Example

## Overview

Este documento demonstra como usar o sistema de versionamento específico por API para planejar releases e comunicar mudanças aos desenvolvedores.

## Exemplo Prático

### 1. Configuração de APIs com Diferentes Cronogramas

```kotlin
class ApiRegistrationExample {
    fun registerExampleApis(registry: PublicApiRegistry) {
        // APIs Críticas - Cronograma Estendido
        registry.registerApi(
            methodName = "getInstance",
            signature = "Branch.getInstance()",
            usageImpact = UsageImpact.CRITICAL,
            complexity = MigrationComplexity.SIMPLE,
            removalTimeline = "Q2 2025",
            modernReplacement = "ModernBranchCore.getInstance()",
            deprecationVersion = "5.0.0",
            removalVersion = "7.0.0" // Suporte estendido
        )
        
        // APIs Problemáticas - Remoção Acelerada
        registry.registerApi(
            methodName = "getFirstReferringParamsSync",
            signature = "Branch.getFirstReferringParamsSync()",
            usageImpact = UsageImpact.MEDIUM,
            complexity = MigrationComplexity.COMPLEX,
            removalTimeline = "Q1 2025",
            modernReplacement = "dataManager.getFirstReferringParamsAsync()",
            breakingChanges = listOf("Converted from synchronous to asynchronous operation"),
            deprecationVersion = "4.0.0", // Depreciação precoce
            removalVersion = "5.0.0"     // Remoção rápida devido ao impacto na performance
        )
        
        // APIs Padrão - Cronograma Normal
        registry.registerApi(
            methodName = "setIdentity",
            signature = "Branch.setIdentity(String)",
            usageImpact = UsageImpact.HIGH,
            complexity = MigrationComplexity.SIMPLE,
            removalTimeline = "Q3 2025",
            modernReplacement = "identityManager.setIdentity(String)",
            deprecationVersion = "5.0.0",
            removalVersion = "6.0.0" // Cronograma padrão
        )
    }
}
```

### 2. Geração de Relatórios de Timeline

```kotlin
class ReleaseManager {
    fun generateReleaseReport(context: Context) {
        val preservationManager = BranchApiPreservationManager.getInstance(context)
        
        // Relatório completo de timeline
        val timelineReport = preservationManager.generateVersionTimelineReport()
        
        println("=== BRANCH SDK VERSION TIMELINE ===")
        println("Total versions with changes: ${timelineReport.totalVersions}")
        println("Busiest version: ${timelineReport.summary.busiestVersion}")
        println()
        
        // Detalhes por versão
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
        
        // APIs sendo depreciadas nesta versão
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
        
        // APIs sendo removidas nesta versão
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

### 3. Exemplo de Saída do Relatório

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
  📢 APIs Deprecated (4):
    - getInstance (CRITICAL)
    - getAutoInstance (CRITICAL)
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
  🚨 APIs Removed (3):
    - getInstance → ModernBranchCore.getInstance()
    - getAutoInstance → ModernBranchCore.initialize(Context)
    - generateShortUrl → linkManager.createShortLink()
  ⚡ BREAKING CHANGES IN THIS VERSION
```

### 4. Integração com CI/CD

```kotlin
class ContinuousIntegration {
    fun validateReleaseChanges(context: Context, plannedVersion: String) {
        val preservationManager = BranchApiPreservationManager.getInstance(context)
        
        // Verificar se há mudanças breaking na versão planejada
        val removedApis = preservationManager.getApisForRemovalInVersion(plannedVersion)
        
        if (removedApis.isNotEmpty()) {
            println("⚠️  WARNING: Version $plannedVersion contains ${removedApis.size} breaking changes")
            
            // Verificar se é uma versão major (pode ter breaking changes)
            val isMajorVersion = plannedVersion.split(".")[0].toInt() > 
                                getCurrentVersion().split(".")[0].toInt()
            
            if (!isMajorVersion) {
                throw IllegalStateException(
                    "Breaking changes detected in non-major version $plannedVersion"
                )
            }
        }
        
        // Gerar changelog automático
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
        
        // Salvar changelog
        writeChangelogToFile(changelog, version)
    }
}
```

## Benefícios do Sistema

1. **Flexibilidade**: Cada API pode ter seu próprio cronograma
2. **Planejamento**: Relatórios detalhados para planning de releases
3. **Comunicação**: Informações claras para desenvolvedores
4. **Automação**: Integração com pipelines de CI/CD
5. **Gradualidade**: Permite migração suave e controlada 