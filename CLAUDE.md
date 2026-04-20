# WSO2 Synapse — Claude Code Notes

## Project Overview

This is the WSO2 Synapse mediation engine. The active development branch is
`VTPassTV1`, which introduces a **Virtual Thread (VT) blocking transport**
(`VTHttpSender`, `VTBlockingServerWorker`) as an alternative to the NIO
PassThrough transport.

## Active Branch: VTPassTV1

### VT Transport Architecture

The VT transport uses Java Virtual Threads (Project Loom) for truly blocking
I/O without pinning platform threads. Key classes:

| Class | Module | Role |
|---|---|---|
| `VTBlockingServerWorker` | nhttp | Handles inbound requests on VT transport. Sets `PASS_THROUGH_PIPE = VTInputStreamPipe` on the axis2 MessageContext. |
| `VTInputStreamPipe` | nhttp | Wraps the raw blocking `InputStream` from the client socket as a "pipe" object. |
| `VTHttpSender` | nhttp | Unified outbound HTTP transport sender (HttpClient 4.5.x). Handles both backend calls (blocking call path) and passthrough client responses (via `submitResponse()`). Registered in both `axis2.xml` and `axis2_blocking_client.xml`. |

### Blocking Call Path (the one we modify)

```
CallMediator.handleBlockingCall()
  → BlockingMsgSender.send()           [modules/core]
      → OperationClient.execute()
        → VTHttpSender.invoke()        [modules/transports/core/nhttp]
          → writeMessageWithCommons()
            → HTTPSender.send()        [Axis2 dep — serializes from OM tree]
```

`VTHttpSender` is the single unified sender for both this path and the passthrough response path.

---

## Changes Made in VTPassTV1

### 1. Request Body Streaming (bypass buildMessage)

**Goal**: Stream the raw request body from `VTInputStreamPipe` directly to the
backend via HttpClient 4.x `InputStreamEntity`, without parsing the body into
an AXIOM OM tree first. Mirrors NIO non-blocking pass-through behaviour.

**Trigger condition**: Streaming path is taken only when:
- `PASS_THROUGH_PIPE` on the axis2 MessageContext holds a `VTInputStreamPipe`
- `MESSAGE_BUILDER_INVOKED` is absent / false

If a content-aware mediator (e.g. `<log/>` at any level except `message_template`/
`custom`) precedes `<call/>` in the sequence, `AbstractListMediator` calls
`buildMessage()` before that mediator runs, sets `MESSAGE_BUILDER_INVOKED=true`,
and the sender falls back to the existing OM-tree path (`HTTPSender.send()`).

**Files changed**:

#### `modules/core/src/main/java/org/apache/synapse/mediators/builtin/CallMediator.java`
- `isContentAware()` now returns `false` (was `return blocking`).
- This prevents `AbstractListMediator` from calling `buildMessage()` purely
  because of the CallMediator. Matches NIO non-blocking behaviour where
  `blocking=false` already returned `false`.

#### `modules/core/src/main/java/org/apache/synapse/message/senders/blocking/BlockingMsgSender.java`
- After `axisOutMsgCtx.setEnvelope(...)`, copies these properties from
  `axisInMsgCtx` → `axisOutMsgCtx` (in both `send()` overloads):
  - `PassThroughConstants.PASS_THROUGH_PIPE`
  - `PassThroughConstants.MESSAGE_BUILDER_INVOKED`
  - `Constants.Configuration.CONTENT_TYPE`
  - `Constants.Configuration.HTTP_METHOD`
  - `MessageContext.TRANSPORT_HEADERS`
- Without this, the new `axisOutMsgCtx` created by `BlockingMsgSender` would
  not carry the pipe, making it invisible to `VTHttpSender`.

#### `modules/transports/core/nhttp/src/main/java/org/apache/synapse/transport/passthru/vt/VTHttpSender.java`
- `writeMessageWithCommons()`: checks for `VTInputStreamPipe` on
  `PASS_THROUGH_PIPE`; if present and `MESSAGE_BUILDER_INVOKED` is false,
  routes to new `sendStreamedRequest()` method instead of `HTTPSender.send()`.
- New `sendStreamedRequest(MessageContext, EndpointReference, VTInputStreamPipe)`:
  - Retrieves cached `CloseableHttpClient` from `ConfigurationContext` via
    `HTTPConstants.CACHED_HTTP_CLIENT` (set in `VTHttpSender.init()`).
  - Sends body via `InputStreamEntity(vtPipe.getInputStream(), contentType)` —
    no buffering, no OM parsing.
  - After response: calls `populateResponseOnMessageContext()`.
- New `populateResponseOnMessageContext(MessageContext, CloseableHttpResponse)`:
  - Sets `HTTPConstants.MC_HTTP_STATUS_CODE`, `"transport.http.statusCode"`,
    `TRANSPORT_HEADERS`, `CONTENT_TYPE`, `TRANSPORT_IN` (response body stream).
  - Stashes response under `"VT_HTTP_RESPONSE"` for connection cleanup.
- New `isRestrictedHeader(String)`: skips `content-length`, `host`,
  `connection`, `transfer-encoding` when copying headers to the outbound request.

---

## Key Behavioural Rules

### When does streaming happen?

Streaming (no OM build, raw bytes from client → backend) happens when the
sequence contains **no content-aware mediator** before `<call/>`.

| Sequence | Streams? |
|---|---|
| `<call/><respond/>` | **Yes** |
| `<property .../><call/>` (non-content-aware property) | **Yes** |
| `<log/><call/>` (any log level except message_template/custom without body expressions) | **No** (Log returns `isContentAware=true`) |
| `<log level="full"/><call/>` | **No** |

This is **identical** to NIO non-blocking pass-through behaviour.
`AbstractListMediator` (lines 113-116) calls `buildMessage()` before any
content-aware mediator, regardless of blocking/non-blocking mode.

### Deployment prerequisite

`axis2_blocking_client.xml` in the deployed MI must register `VTHttpSender`
as the HTTP transport sender with `cacheHttpClient=true`:

```xml
<transportSender name="http"
                 class="org.apache.synapse.transport.passthru.vt.VTHttpSender">
    <parameter name="PROTOCOL">HTTP/1.1</parameter>
    <parameter name="Transfer-Encoding">chunked</parameter>
    <parameter name="cacheHttpClient">true</parameter>
</transportSender>
```

Without `cacheHttpClient=true`, `VTHttpSender.init()` won't build the pooled
`CloseableHttpClient`, and `sendStreamedRequest()` throws an `AxisFault`.

---

## Module Build

```bash
# Compile only changed modules (fast)
mvn -pl modules/core,modules/transports/core/nhttp -am compile -DskipTests

# Full build
mvn clean install -DskipTests
```

## Important: What NOT to touch

- **`VTPassThroughHttpSender`** — deleted, merged into `VTHttpSender`.
- **`VTBlockingClientWorker`** — deleted, was only used by `VTPassThroughHttpSender`.
- **Commented-out VT path in `CallMediator`** (`handleVTBlockingCall`,
  `vtBlockingMsgSender` field, commented blocks in `mediate()` and `init()`) —
  leave as-is, not relevant to the current streaming change.
