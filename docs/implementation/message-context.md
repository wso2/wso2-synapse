# Message Context
The MsgContext is the central data structure that flows through the mediation pipeline.

```java
public class MsgContext {
    private Map<String, String> properties;
    private Message message;
    private Map<String, String> headers;

    public MsgContext() {
        this.properties = new HashMap<>();
        this.message = new Message();
        this.headers = new HashMap<>();
    }
    
    // Getters and setters
}
```
## Key Components

1. Properties: Key-value pairs used by mediators to store and retrieve data
2. Headers: HTTP and protocol-specific headers
3. Message: The actual content being processed, including payload and content type

## Creation and Usage

Message contexts are typically created by inbound endpoints:

```java
MsgContext context = new MsgContext();
Message msg = new Message(requestBody, contentType);
context.setMessage(msg);

Map<String, String> properties = Map.of(
        "isInbound", "true",
        "ARTIFACT_NAME", "httpinbound",
        "inboundEndpointName", "http",
        "ClientApiNonBlocking", "true"
);

context.setHeaders(Map.of(
        "HTTP_METHOD", exchange.getRequestMethod(),
        "REQUEST_URI", exchange.getRequestURI().toString()
));

context.setProperties(properties);
```
