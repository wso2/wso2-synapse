# Inbound Endpoints

Inbound endpoints are entry points for messages into the system. They implement the `InboundEndpoint` interface and are responsible for receiving messages from external systems, converting them to a common `MsgContext` format, and passing them to the `MediationEngine` for further processing.

## Interface Definition

The `InboundEndpoint` interface defines the contract for all inbound endpoints:

```java
public interface InboundEndpoint {
    void start(InboundMessageMediator mediator) throws Exception;
    void stop() throws Exception;
}
```
Common Functionality
All inbound endpoints:

Receive data from external systems
Convert the data into a MsgContext object
Pass the message to the MediationEngine via the InboundMessageMediator interface
Handle the lifecycle (start/stop) of the endpoint
Available Implementations
FileInboundEndpoint
The FileInboundEndpoint polls files from a directory and processes them.

Available Implementations
FileInboundEndpoint
The FileInboundEndpoint polls files from a directory and processes them.

```java
@Override
public void start(InboundMessageMediator mediator) throws Exception {
    this.mediator = mediator;
    isRunning.set(true);

    validateConfig(); // Ensures required parameters like 'interval' and 'FileURI' are present.
    int interval = getIntervalParameterValue(); // Retrieves the polling interval.

    scheduler.scheduleAtFixedRate(() -> {
        if (!isRunning.get()) return;

        try {
            Thread.ofVirtual().start(() -> {
                String fileUri = config.getParameters().get("transport.vfs.FileURI");
                FileObject folder = fsManager.resolveFile(fileUri);
                pollFiles(folder); // Polls files from the directory.
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }, 0, interval, TimeUnit.MILLISECONDS);
}
```

HttpInboundEndpoint
The HttpInboundEndpoint listens for HTTP requests and processes them.

```java
@Override
public void start(InboundMessageMediator mediator) throws Exception {
    this.mediator = mediator;
    isRunning.set(true);
    
    port = Integer.parseInt(config.getParameters().getOrDefault("inbound.http.port", "8280"));
    context = config.getParameters().getOrDefault("inbound.http.context", "/");
    
    server = HttpServer.create(new InetSocketAddress(port), 0);
    HttpContext httpContext = server.createContext(context, this::handleRequest);
    
    server.start();
    log.info("HTTP Inbound started on port {}, context: {}", port, context);
}
```

Configuration
Inbound endpoints are configured using XML files in the Inbounds directory. Example:

```xml
<inboundEndpoint xmlns="http://ws.apache.org/ns/synapse"
                 name="file" sequence="inboundSeq"
                 onError="fault"
                 protocol="file"
                 suspend="false">
    <parameters>
        <parameter name="interval">2000</parameter>
        <parameter name="sequential">true</parameter>
        <parameter name="transport.vfs.FileURI">file:///path/to/directory</parameter>
        <parameter name="transport.vfs.FileNamePattern">.*\.xml</parameter>
    </parameters>
</inboundEndpoint>
```