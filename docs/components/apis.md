# APIs

APIs in Synapse provide RESTful interfaces for clients to interact with the integration engine. Each API defines a set of resources with associated processing sequences.

## Overview

An API consists of:
- A unique name
- A context path
- One or more resources
- Optional CORS configuration
- Position information for error reporting

## Implementation

```java
public class API {
    private String context;
    private String name;
    private List<Resource> resources;
    private Position position;
    private CORSConfig corsConfig;
    
    public API(String context, String name, List<Resource> resources, Position position) {
        this.context = context;
        this.name = name;
        this.resources = resources;
        this.position = position;
    }
}
```
## Resources

Each API contains resources that map to specific HTTP methods and URI templates:

```java
public class Resource {
    private String methods;
    private String uriTemplate;
    private Sequence inSequence;
    private Sequence faultSequence;

    public Resource(String methods, String uriTemplate, Sequence inSequence, Sequence faultSequence) {
        this.methods = methods;
        this.uriTemplate = uriTemplate;
        this.inSequence = inSequence;
        this.faultSequence = faultSequence;
    }
}
```

## CORS Configuration

APIs can include CORS (Cross-Origin Resource Sharing) configuration:

```java
private CORSConfig unmarshalCORSConfig(OMElement apiElement) {
    OMElement corsElement = apiElement.getFirstChildWithName(new QName("cors"));
    if (corsElement == null) {
        return null;
    }

    CORSConfig corsConfig = new CORSConfig();
    
    // Parse CORS attributes
    OMAttribute enabledAttr = corsElement.getAttribute(new QName("enabled"));
    if (enabledAttr != null) {
        corsConfig.setEnabled(Boolean.parseBoolean(enabledAttr.getAttributeValue()));
    }
    
    OMAttribute allowOriginsAttr = corsElement.getAttribute(new QName("allow-origins"));
    if (allowOriginsAttr != null) {
        String origins = allowOriginsAttr.getAttributeValue();
        if (origins != null && !origins.trim().isEmpty()) {
            String[] originsArray = origins.split("\\s*,\\s*");
            corsConfig.setAllowOrigins(originsArray);
        }
    }
        
    return corsConfig;
}
```