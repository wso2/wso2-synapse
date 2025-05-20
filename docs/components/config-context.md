# ConfigContext

ConfigContext serves as the central registry for all deployed artifacts in the Synapse Engine. It's implemented as a singleton pattern to ensure a single point of access across the application.

## Overview

The ConfigContext maintains collections of all deployed artifacts:
- APIs
- Endpoints
- Sequences
- Inbound Endpoints

These artifacts are stored in memory and can be accessed by name throughout the application lifecycle.

## Implementation

```java
public class ConfigContext implements EndpointProvider {
    private final Map<String, API> apiMap;
    private final Map<String, Endpoint> endpointMap;
    private final Map<String, Sequence> sequenceMap;
    private final Map<String, Inbound> inboundMap;

    // Singleton instance
    private static final ConfigContext INSTANCE = new ConfigContext();

    private ConfigContext() {
        this.apiMap = new HashMap<>();
        this.endpointMap = new HashMap<>();
        this.sequenceMap = new HashMap<>();
        this.inboundMap = new HashMap<>();
    }

    public static ConfigContext getInstance() {
        return INSTANCE;
    }
}
```