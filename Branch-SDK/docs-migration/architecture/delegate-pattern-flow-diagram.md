# Branch SDK Architecture: Delegate Pattern Flow Diagrams

**Document Type:** Technical Flow Diagrams  
**Created:** June 2025  
**Last Updated:** June 2025  
**Version:** 1.0  
**Author:** Branch SDK Team  

---

## Document Purpose

This document provides detailed visual diagrams that illustrate how the delegate pattern works in practice within the Branch SDK modernization architecture. The diagrams cover everything from legacy API calls to modern core processing, including error flows, performance monitoring, and configuration.

## Legacy API Call Flow

This diagram shows the complete flow of a legacy API call through the delegation system:

```mermaid
sequenceDiagram
    participant Client as Client App
    participant Legacy as Legacy API
    participant Wrapper as API Wrapper
    participant Manager as Preservation Manager
    participant Analytics as Usage Analytics
    participant Registry as API Registry
    participant Modern as Modern Core
    participant StateFlow as Reactive State

    Note over Client: Developer calls legacy API
    Client->>Legacy: Branch.getInstance().setIdentity("user123")
    
    Note over Legacy,Wrapper: API Preservation Layer
    Legacy->>Wrapper: LegacyBranchWrapper.setIdentity()
    Wrapper->>Manager: handleLegacyApiCall("setIdentity", ["user123"])
    
    Note over Manager: Coordination Phase
    Manager->>Analytics: recordApiUsage("setIdentity", params)
    Manager->>Registry: getApiInfo("setIdentity")
    Registry-->>Manager: ApiMethodInfo (deprecation details)
    Manager->>Manager: logDeprecationWarning()
    
    Note over Manager,Modern: Delegation Phase
    Manager->>Modern: delegateToModernCore("setIdentity", params)
    Modern->>Modern: identityManager.setIdentity("user123")
    Modern->>StateFlow: Update currentUser StateFlow
    
    Note over Modern,Client: Response Flow
    Modern-->>Manager: Success/Failure Result
    Manager-->>Wrapper: Processed Result
    Wrapper-->>Legacy: Legacy-compatible Response
    Legacy-->>Client: Boolean/Original Return Type
    
    Note over Analytics: Background Analytics
    Analytics->>Analytics: Update usage statistics
    Analytics->>Analytics: Track performance metrics
```

## Component Interaction Detail

```mermaid
graph LR
    subgraph "Client Layer"
        A[Legacy Client Code]
    end
    
    subgraph "Preservation Layer"
        B[PreservedBranchApi]
        C[LegacyBranchWrapper]
        D[Callback Adapters]
    end
    
    subgraph "Coordination Layer"
        E[BranchApiPreservationManager]
        F[Usage Analytics]
        G[API Registry]
        H[Version Config]
    end
    
    subgraph "Modern Layer"
        I[ModernBranchCore]
        J[SessionManager]
        K[IdentityManager]
        L[LinkManager]
        M[EventManager]
    end
    
    A --> B
    A --> C
    B --> E
    C --> E
    E --> F
    E --> G
    E --> H
    E --> I
    I --> J
    I --> K
    I --> L
    I --> M
    C --> D
    D --> E
```

## API Lifecycle States

```mermaid
stateDiagram-v2
    [*] --> Active: API is current
    Active --> Deprecated: Deprecation version reached
    Deprecated --> Warning: Usage triggers warnings
    Warning --> Removed: Removal version reached
    Removed --> [*]
    
    Active: ✅ Full Support
    Deprecated: ⚠️ Deprecated with warnings
    Warning: 🚨 Enhanced warnings
    Removed: ❌ No longer available
    
    note right of Deprecated
        API-specific timelines:
        - Critical: 5.0.0 → 7.0.0
        - Standard: 5.0.0 → 6.0.0  
        - Problematic: 4.0.0 → 5.0.0
    end note
```

## Migration Timeline Visualization

```mermaid
gantt
    title Branch SDK API Migration Timeline
    dateFormat X
    axisFormat %s
    
    section Critical APIs
    getInstance()           :active, critical1, 0, 4
    initSession()           :active, critical2, 0, 4
    generateShortUrl()      :active, critical3, 0, 4
    
    section Standard APIs  
    setIdentity()           :standard1, 0, 3
    resetUserSession()      :standard2, 0, 3
    logout()               :standard3, 0, 3
    
    section Problematic APIs
    getFirstReferringParamsSync() :crit, problem1, 0, 2
    enableTestMode()        :problem2, 0, 2
    
    section Modern APIs
    ModernBranchCore        :modern1, 1, 5
    Reactive StateFlow      :modern2, 1, 5
    Coroutine Operations    :modern3, 2, 5
```

## Data Flow Architecture

```mermaid
flowchart TD
    A[Legacy API Call] --> B{API Type?}
    
    B -->|Static| C[PreservedBranchApi]
    B -->|Instance| D[LegacyBranchWrapper]
    
    C --> E[BranchApiPreservationManager]
    D --> E
    
    E --> F[Record Usage]
    E --> G[Check Deprecation Status]
    E --> H[Log Warnings]
    E --> I[Delegate to Modern Core]
    
    I --> J{Operation Type?}
    
    J -->|Session| K[SessionManager]
    J -->|Identity| L[IdentityManager]  
    J -->|Links| M[LinkManager]
    J -->|Events| N[EventManager]
    J -->|Data| O[DataManager]
    J -->|Config| P[ConfigManager]
    
    K --> Q[Update StateFlow]
    L --> Q
    M --> Q
    N --> Q
    O --> Q
    P --> Q
    
    Q --> R[Return to Legacy Format]
    R --> S[Client Receives Response]
    
    F --> T[Analytics Database]
    G --> U[Version Registry]
    H --> V[Deprecation Logs]
```

## Performance Monitoring Flow

```mermaid
sequenceDiagram
    participant Call as API Call
    participant Manager as Preservation Manager
    participant Analytics as Analytics Engine
    participant Registry as API Registry
    participant Reports as Reporting System

    Call->>Manager: handleLegacyApiCall()
    Note over Manager: Start timing
    
    Manager->>Analytics: recordApiCall(start_time)
    Manager->>Registry: getApiInfo()
    Manager->>Manager: Execute delegation
    
    Note over Manager: End timing
    Manager->>Analytics: recordApiCallCompletion(duration, success)
    
    Analytics->>Analytics: Update metrics
    Analytics->>Reports: Generate usage reports
    
    Note over Reports: Background processing
    Reports->>Reports: Calculate trends
    Reports->>Reports: Identify hot paths
    Reports->>Reports: Flag performance issues
```

## Error Handling Flow

```mermaid
flowchart TD
    A[Legacy API Call] --> B[Preservation Manager]
    B --> C{Validation}
    
    C -->|Valid| D[Delegate to Modern Core]
    C -->|Invalid| E[Log Error & Return Default]
    
    D --> F{Modern Core Response}
    
    F -->|Success| G[Record Success Metrics]
    F -->|Error| H[Handle Error Gracefully]
    
    G --> I[Return Legacy-Compatible Result]
    H --> J[Log Error Details]
    H --> K[Return Legacy-Compatible Error]
    
    I --> L[Client Receives Success]
    K --> L
    
    J --> M[Analytics Database]
    M --> N[Error Tracking Reports]
```

## Configuration Loading Flow

```mermaid
sequenceDiagram
    participant App as Application
    participant Manager as Preservation Manager
    participant Factory as Version Config Factory
    participant Config as Properties Config
    participant Assets as Assets Folder

    App->>Manager: getInstance(context)
    Manager->>Factory: createConfiguration(context)
    Factory->>Config: getInstance(context)
    
    Config->>Assets: load("branch_version_config.properties")
    
    alt File exists
        Assets-->>Config: Properties loaded
        Config->>Config: Parse configuration
    else File missing
        Config->>Config: Use default values
    end
    
    Config-->>Factory: VersionConfiguration
    Factory-->>Manager: Configured instance
    Manager-->>App: Ready for use
    
    Note over Config: Configuration includes:
    Note over Config: - deprecation.version
    Note over Config: - removal.version  
    Note over Config: - migration.guide.url
```

This diagram system shows how the delegate pattern works in practice, from the initial call to the final result, passing through all layers of preservation, coordination, and modern execution. 