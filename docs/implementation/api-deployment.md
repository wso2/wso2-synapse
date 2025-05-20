# API Deployment Implementation

The API deployment process in Synapse Java is responsible for taking XML API definitions and deploying them to the Axis2 runtime. This page explains the key components and process flow.

## Core Components

1. **APIDeployer**: Parses XML API definitions and creates API objects.
2. **APIDeploymentService**: Deploys APIs to Axis2.
3. **APIMessageReceiver**: Handles incoming requests to API endpoints.

## CORS Configuration

The `CORSConfig` class holds CORS configuration for APIs:

```java
public class CORSConfig {
    private boolean enabled;
    private String allowOrigins;
    private String allowMethods;
    private String allowHeaders;
    private String exposeHeaders;
    private boolean allowCredentials;
    private int maxAge;
    
    // Getters and setters
}
```
API Deployment Process

1. Parsing API XML 

When an API XML file is detected in the artifacts directory, APIDeployer parses it:

```java
public API unmarshal(String xmlData, Position position) throws XMLStreamException {
    OMElement documentElement = AXIOMUtil.stringToOM(xmlData);
    OMElement apiElement = documentElement;
    
    // Extract API attributes
    String name = apiElement.getAttributeValue(new QName("name"));
    String context = apiElement.getAttributeValue(new QName("context"));
    
    // Parse resources
    List<Resource> resources = new ArrayList<>();
    Iterator<OMElement> resourcesIter = apiElement.getChildrenWithName(new QName("resource"));
    while (resourcesIter.hasNext()) {
        OMElement resourceElem = resourcesIter.next();
        resources.add(unmarshalResource(resourceElem));
    }
    
    // Create API object
    API newApi = new API(context, name, resources, position);
    
    // Parse CORS configuration
    CORSConfig corsConfig = unmarshalCORSConfig(apiElement);
    newApi.setCorsConfig(corsConfig);
    
    return newApi;
}
```
2. Deploying to Axis2

The APIDeploymentService deploys the API to Axis2:

```java
public void deployAPI(API api) throws Exception {
    String serviceName = api.getName() + "Service";
    
    // Create a new AxisService
    AxisService service = new AxisService(serviceName);
    service.setClassLoader(this.getClass().getClassLoader());
    
    // Set service path to match API context
    service.setName(serviceName);
    service.addParameter(new Parameter("ServiceClass", APIMessageReceiver.class.getName()));
    service.addParameter(new Parameter("api-context", api.getContext()));
    
    // Add CORS configuration if present
    if (api.getCorsConfig() != null && api.getCorsConfig().isEnabled()) {
        addCORSConfiguration(service, api.getCorsConfig());
    }
    
    // For each resource, create an operation
    for (Resource resource : api.getResources()) {
        addResourceOperation(service, resource, api.getContext());
    }
    
    // Deploy the service
    AxisConfiguration axisConfig = axisConfigContext.getAxisConfiguration();
    axisConfig.addService(service);
    
    // Store the service for later reference
    deployedServices.put(api.getName(), service);
}
```
3. Setting Up Resource Operations

Each resource in an API is mapped to an Axis2 operation:

```java
private void addResourceOperation(AxisService service, Resource resource, String apiContext) throws Exception {
    // Create an operation based on the resource
    String operationName = generateOperationName(resource.getMethods(), resource.getUriTemplate());
    QName opName = new QName(operationName);
    
    // Create an operation and set its details
    AxisOperation operation = new InOutAxisOperation(opName);
    operation.setMessageReceiver(new APIMessageReceiver(resource, apiContext, mediator));
    
    // Set HTTP method binding
    for (String method : resource.getMethods().split(",")) {
        operation.addParameter(
            new Parameter("HTTP_METHOD_" + method.trim().toUpperCase(), resource.getUriTemplate())
        );
    }
    
    // Add the operation to the service
    service.addOperation(operation);
}
```
4. Handling API Requests

The APIMessageReceiver handles incoming requests to the API:

```java
@Override
public void invokeBusinessLogic(MessageContext messageContext) throws AxisFault {
    try {
        // Convert Axis2 MessageContext to our MsgContext
        MsgContext msgContext = convertToMsgContext(messageContext);
        
        // Execute the resource's inSequence
        if (resource.getInSequence() != null) {
            boolean success = resource.getInSequence().execute(msgContext);
            
            if (!success && resource.getFaultSequence() != null) {
                // If inSequence fails, execute faultSequence
                resource.getFaultSequence().execute(msgContext);
            }
        } else {
            log.warn("No inSequence defined for resource: {}", resource.getUriTemplate());
        }
        
        // Convert our MsgContext back to Axis2 MessageContext
        updateAxisMessageContext(messageContext, msgContext);
        
    } catch (Exception e) {
        log.error("Error processing request", e);
        try {
            if (resource.getFaultSequence() != null) {
                MsgContext errorContext = createErrorContext(messageContext, e);
                resource.getFaultSequence().execute(errorContext);
            }
        } catch (Exception ex) {
            log.error("Error in fault sequence", ex);
        }
        throw new AxisFault("Error processing request", e);
    }
}
```
CORS Support

CORS (Cross-Origin Resource Sharing) is configured at the API level:

```java
private void addCORSConfiguration(AxisService service, CORSConfig corsConfig) throws Exception {
    service.addParameter(new Parameter("cors-enabled", String.valueOf(corsConfig.isEnabled())));
    
    if (corsConfig.getAllowOrigins() != null) {
        service.addParameter(new Parameter("cors-allow-origins", corsConfig.getAllowOrigins()));
    }
    
    if (corsConfig.getAllowMethods() != null) {
        service.addParameter(new Parameter("cors-allow-methods", corsConfig.getAllowMethods()));
    }
    
    if (corsConfig.getAllowHeaders() != null) {
        service.addParameter(new Parameter("cors-allow-headers", corsConfig.getAllowHeaders()));
    }
    
    if (corsConfig.getExposeHeaders() != null) {
        service.addParameter(new Parameter("cors-expose-headers", corsConfig.getExposeHeaders()));
    }
    
    service.addParameter(new Parameter("cors-allow-credentials", String.valueOf(corsConfig.isAllowCredentials())));
    service.addParameter(new Parameter("cors-max-age", String.valueOf(corsConfig.getMaxAge())));
}
```
Example API XML

An example of an API XML definition:

```xml
<api context="/api" name="SampleAPIWithCORS">
    <!-- Configure CORS for this API -->
    <cors enabled="true"
          allow-origins="https://example.com,https://app.example.com"
          allow-methods="GET,POST,PUT,DELETE,PATCH,OPTIONS"
          allow-headers="Content-Type,Authorization,X-Requested-With,Accept"
          expose-headers="X-Request-ID,X-Response-Time"
          allow-credentials="true"
          max-age="3600" />
          
    <!-- API Resources -->
    <resource methods="GET" uri-template="/hello">
        <inSequence>
            <log level="full"/>
            <!-- Other mediators would go here -->
        </inSequence>
        <faultSequence>
            <!-- Error handling mediators would go here -->
        </faultSequence>
    </resource>
    
    <resource methods="POST" uri-template="/data">
        <inSequence>
            <log level="full"/>
            <!-- Other mediators would go here -->
        </inSequence>
        <faultSequence>
            <!-- Error handling mediators would go here -->
        </faultSequence>
    </resource>
</api>
```