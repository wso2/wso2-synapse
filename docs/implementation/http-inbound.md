# HTTP Inbound Endpoint

The HTTP Inbound Endpoint allows Synapse to listen for incoming HTTP requests and process them through mediation sequences.

## Implementation Overview

The HTTP inbound endpoint is implemented in `HttpInboundEndpoint`, which uses Java's built-in `HttpServer` for handling HTTP requests.

### Key Components

1. **HttpInboundEndpoint**: Main class that implements the `InboundEndpoint` interface
2. **HttpRequestHandler**: Inner class that processes incoming HTTP requests
3. **Virtual Threads**: Used for efficient request handling and concurrency

## Configuration Parameters

HTTP inbound endpoints support the following parameters:

| Parameter | Description | Required | Default |
|-----------|-------------|----------|---------|
| inbound.http.port | Port to listen on | Yes | 8080 |
| inbound.http.context | Context path for the endpoint | No | / |

## Implementation Details

### Initialization and Starting

The HTTP inbound endpoint starts an HTTP server on the configured port:

```java
@Override
public void start(InboundMessageMediator mediator) throws Exception {
    this.mediator = mediator;
    validateConfig();

    int port = Integer.parseInt(config.getParameters().getOrDefault("inbound.http.port", "8080"));
    String contextPath = config.getParameters().getOrDefault("inbound.http.context", "/");

    httpServer = HttpServer.create(new InetSocketAddress(port), 0);
    httpServer.createContext(contextPath, new HttpRequestHandler());
    httpServer.setExecutor(virtualExecutor);
    httpServer.start();
    isRunning.set(true);

    logger.info("HTTP Inbound Endpoint started at http://localhost:{}{}", port, contextPath);
}
```

### Configuration Example

```xml
<inboundEndpoint xmlns="http://ws.apache.org/ns/synapse"
                 name="http_inbound"
                 sequence="inboundSeq"
                 protocol="http">
    <parameters>
        <parameter name="inbound.http.port">8280</parameter>
        <parameter name="inbound.http.context">/inbound</parameter>
    </parameters>
</inboundEndpoint>
```