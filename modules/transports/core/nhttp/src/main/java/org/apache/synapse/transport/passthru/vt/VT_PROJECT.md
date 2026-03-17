# Virtual Thread (VT) Pass-Through HTTP Transport

> **AI Context Document** — comprehensive reference for the VT transport module.
> This file is for AI assistants and developers working on this codebase.

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Threading Model](#threading-model)
4. [File Inventory](#file-inventory)
5. [Class Details](#class-details)
6. [Request Lifecycle](#request-lifecycle)
7. [Response Lifecycle](#response-lifecycle)
8. [Keep-Alive Support](#keep-alive-support)
9. [Axis2 Integration](#axis2-integration)
10. [axis2.xml Configuration](#axis2xml-configuration)
11. [Configuration Parameters](#configuration-parameters)
12. [SSL / HTTPS](#ssl--https)
13. [Key Design Decisions](#key-design-decisions)
14. [Known Caveats & Risks](#known-caveats--risks)
15. [Relationship to Original Pass-Through Transport](#relationship-to-original-pass-through-transport)
16. [Package Dependencies](#package-dependencies)

---

## Overview

This module provides an **alternative HTTP transport** for Apache Synapse / WSO2
that replaces the NIO/reactor-based pass-through transport with a **blocking I/O
model powered by Java 21+ Virtual Threads (Project Loom)**.

**Key idea:** Instead of multiplexing thousands of connections over a few NIO
selector threads and dispatching work to a platform thread pool, each inbound
TCP connection is handled by its own lightweight Virtual Thread doing plain
blocking `Socket` I/O. This eliminates the complexity of async state machines
while preserving high concurrency — virtual threads are cheap (~1 KB stack)
and scheduled by the JVM, not the OS.

**Package:** `org.apache.synapse.transport.passthru.vt`

**Location:** `modules/transports/core/nhttp/src/main/java/org/apache/synapse/transport/passthru/vt/`

**Minimum Java version:** 21 (for `Thread.ofVirtual()` and `Executors.newThreadPerTaskExecutor()`)

---

## Architecture

```
                          ┌─────────────────────────────┐
                          │    VTPassThroughHttpListener │
                          │    (TransportListener)       │
                          │                              │
                          │  ServerSocket.accept() loop  │
                          │  on 1 platform thread        │
                          └───────────┬─────────────────┘
                                      │ per connection
                                      ▼
                          ┌───────────────────────────────┐
                          │   vtExecutor (VT-per-task)     │
                          │   Executors.newThreadPerTask   │
                          │   ExecutorService              │
                          │                                │
                          │   Also wrapped as Axis2        │
                          │   WorkerPool via               │
                          │   VirtualThreadWorkerPool      │
                          └───────────┬────────────────────┘
                                      │ each VT runs:
                                      ▼
                          ┌───────────────────────────────┐
                          │   VTBlockingServerWorker       │
                          │   (Runnable + OutTransportInfo)│
                          │                                │
                          │   1. Parse HTTP request        │
                          │   2. Build Axis2 MessageContext│
                          │   3. AxisEngine.receive()      │  ← same VT
                          │   4. Synapse mediation         │  ← same VT
                          │   5. VTPassThroughHttpSender   │  ← same VT
                          │      .invoke()                 │
                          │   6. Backend call (blocking)   │  ← same VT
                          │   7. VTBlockingClientWorker    │  ← same VT
                          │      .run() inline             │
                          │   8. AxisEngine.receive()      │  ← same VT
                          │      (response flow)           │
                          │   9. submitResponse() writes   │  ← same VT
                          │      HTTP response to client   │
                          │  10. Loop for keep-alive       │
                          └───────────────────────────────┘

                          ┌───────────────────────────────┐
                          │   VTPassThroughHttpSender      │
                          │   (TransportSender)            │
                          │                                │
                          │   Uses Apache HttpClient 4.x   │
                          │   with PoolingHttpClient       │
                          │   ConnectionManager            │
                          │                                │
                          │   .execute() is blocking —     │
                          │   cheap in a Virtual Thread    │
                          └───────────────────────────────┘
```

---

## Threading Model

### Single Virtual Thread Per Connection

```
Platform Thread: accept-loop
  └─► vtExecutor.submit(VTBlockingServerWorker)   → spawns VT-1
        VT-1: parse request
        VT-1: AxisEngine.receive(msgCtx)            — Synapse IN flow
        VT-1: VTPassThroughHttpSender.invoke()      — OUT flow
        VT-1: httpClient.execute(req)               — blocking backend call
        VT-1: VTBlockingClientWorker.run()          — inline, NOT submitted to pool
        VT-1: AxisEngine.receive(responseMsgCtx)    — response IN flow
        VT-1: serverWorker.submitResponse()         — write HTTP response
        VT-1: loop back (keep-alive) or exit
```

### Shared Virtual Thread Executor

A single `ExecutorService` created via `Executors.newThreadPerTaskExecutor(Thread.ofVirtual().factory())` is shared between:

1. **Accept loop** — submits `VTBlockingServerWorker` per accepted socket
2. **Axis2 WorkerPool** — wrapped as `VirtualThreadWorkerPool` so any Axis2 internal `workerPool.execute()` also runs on virtual threads from the same executor

This is configured in `VTPassThroughHttpListener.init()`:
```java
vtExecutor = Executors.newThreadPerTaskExecutor(
    Thread.ofVirtual()
        .name(VTConstants.VT_THREAD_PREFIX + namePrefix.toLowerCase() + "-", 0)
        .factory()
);
WorkerPool workerPool = new VirtualThreadWorkerPool(vtExecutor);
sourceConfiguration = new SourceConfiguration(cfgCtx, transportIn, scheme, workerPool, metrics);
```

### When Does a NEW Virtual Thread Spawn?

| Event | Same VT? | Notes |
|-------|----------|-------|
| HTTP request parsing | VT-1 | Inline in ServerWorker |
| AxisEngine.receive() (IN flow) | VT-1 | Inline |
| Synapse mediators (Sequence, Log, etc.) | VT-1 | Inline |
| VTPassThroughHttpSender.invoke() | VT-1 | Inline |
| Backend HTTP call (httpClient.execute) | VT-1 | Blocking, inline |
| VTBlockingClientWorker.run() | VT-1 | Called with `.run()`, not submitted |
| AxisEngine.receive() (response flow) | VT-1 | Inline |
| submitResponse() | VT-1 | Inline |
| Clone/Iterate mediator `workerPool.execute()` | **VT-2** | New VT from same executor |
| Any explicit `workerPool.execute()` call | **VT-N** | New VT from same executor |

---

## File Inventory

| File | Role | Lines |
|------|------|-------|
| `VTPassThroughHttpListener.java` | Transport listener (server-side inbound). Accepts connections, manages lifecycle. Implements `TransportListener`. | ~409 |
| `VTPassThroughHttpSender.java` | Transport sender (client-side outbound). Uses Apache HttpClient 4.x. Extends `AbstractHandler`, implements `TransportSender`. | ~380 |
| `VTBlockingServerWorker.java` | Per-connection worker. Runs entirely in one VT. Handles keep-alive loop, request parsing, Axis2 dispatch, response writing. Implements `Runnable` + `OutTransportInfo`. | ~835 |
| `VTBlockingClientWorker.java` | Processes backend HTTP response. Builds response `MessageContext` and feeds into Axis2 engine. Called inline (`.run()`), not submitted to pool. | ~375 |
| `VTSourceRequest.java` | Parses incoming HTTP request (request line + headers) from socket's `InputStream`. Provides body `InputStream`. Supports keep-alive via shared buffered streams. | ~298 |
| `VTSourceResponse.java` | Builds and writes HTTP response status line + headers to socket `OutputStream`. | ~186 |
| `VTTargetResponse.java` | Wraps Apache HttpClient's `CloseableHttpResponse`. Provides headers, status, body `InputStream`. | ~160 |
| `VTInputStreamPipe.java` | Adapter: wraps a plain `InputStream` as a "Pipe" for `PassThroughConstants.PASS_THROUGH_PIPE` property. | ~53 |
| `VTRequestResponseTransport.java` | `RequestResponseTransport` impl. In blocking model, `acknowledge` and `awaitResponse` are no-ops. | ~73 |
| `ContentLengthInputStream.java` | `FilterInputStream` that reads exactly `Content-Length` bytes then returns EOF. Does NOT close underlying stream (keep-alive). Has `drain()` method. | ~118 |
| `VTConstants.java` | All constants: message context keys, param names, defaults, hop-by-hop headers, HTTP strings. | ~119 |
| `VirtualThreadWorkerPool.java` | Adapts `ExecutorService` → Axis2 `WorkerPool`. `execute()` delegates to `executor.submit()`. | ~77 |

---

## Class Details

### VTPassThroughHttpListener

**Implements:** `org.apache.axis2.transport.TransportListener`

**Key fields:**
- `ServerSocket serverSocket` — plain blocking server socket
- `ExecutorService vtExecutor` — `newThreadPerTaskExecutor` with virtual thread factory
- `SourceConfiguration sourceConfiguration` — reused from existing PT transport
- `AtomicBoolean running` — accept-loop flag
- `Thread acceptThread` — single platform thread running the accept loop

**Lifecycle:**
1. `init()` — reads parameters, creates `vtExecutor`, wraps it as `VirtualThreadWorkerPool`, builds `SourceConfiguration`, sets up service tracker
2. `start()` — opens `ServerSocket`, starts accept-loop platform thread
3. `acceptLoop()` — `while(running) { socket = serverSocket.accept(); vtExecutor.submit(new VTBlockingServerWorker(socket, ...)); }`
4. `stop()` — sets `running=false`, closes server socket, shuts down executor

### VTBlockingServerWorker

**Implements:** `Runnable`, `OutTransportInfo`

**Key fields:**
- `Socket clientSocket` — the accepted client connection
- `VTSourceRequest sourceRequest` — parsed HTTP request (recreated per request in keep-alive loop)
- `MessageContext msgContext` — Axis2 message context (recreated per request)
- `boolean responseSent` — tracks whether response has been written for current request
- `boolean currentKeepAlive` — per-request keep-alive decision
- `int keepAliveTimeout` — idle timeout between requests on keep-alive connection

**Keep-alive loop structure:**
```
run() {
  sharedIn = new BufferedInputStream(socket.getInputStream());
  sharedOut = new BufferedOutputStream(socket.getOutputStream());
  while (keepAlive) {
    reset per-request state
    adjust socket timeout (keepAliveTimeout for idle waits)
    sourceRequest = new VTSourceRequest(socket, sharedIn, sharedOut);
    sourceRequest.parse();
    restore request timeout
    create MessageContext
    dispatch to AxisEngine.receive()
    // finally block: write response if not yet sent, drain body, cleanup
  }
  closeSocket();
}
```

**Response writing:** `submitResponse(MessageContext)` is called by `VTPassThroughHttpSender` → serializes body to byte array → writes `Content-Length` header → writes to socket. Hop-by-hop headers from backend are stripped.

### VTPassThroughHttpSender

**Extends:** `AbstractHandler`, **Implements:** `TransportSender`

**Key fields:**
- `CloseableHttpClient httpClient` — shared Apache HC4 client with connection pool (`PoolingHttpClientConnectionManager`, 200 per route, 1000 max total)
- `TargetConfiguration targetConfiguration`

**invoke() flow:**
1. Determine if sending to backend (EPR present) or responding to client
2. **Backend:** `sendToBackend()` → build `RequestBuilder`, copy headers (skip restricted), serialize body → `httpClient.execute(req)` (blocking) → wrap response as `VTTargetResponse` → `new VTBlockingClientWorker(...).run()` (inline, same VT)
3. **Client response:** find `VTBlockingServerWorker` from `OUT_TRANSPORT_INFO` → `submitResponse()`

### VTBlockingClientWorker

**Implements:** `Runnable`

**Constructor:** builds response `MessageContext` from `VTTargetResponse` — copies headers, status, properties, sets up `VTInputStreamPipe`.

**run():** Sets envelope, status code, content-type → `AxisEngine.receive(responseMsgCtx)` → finally block guarantees `serverWorker.submitResponse()` is called.

### VTSourceRequest

Parses raw HTTP from `BufferedInputStream`:
- Request line: `METHOD URI HTTP/1.1`
- Headers: case-insensitive `TreeMap`, excess headers in `LinkedHashMap`
- Body: `getBodyInputStream()` returns `ContentLengthInputStream` for Content-Length requests, raw stream otherwise
- `isKeepAlive()`: considers HTTP version, Connection header, and body demarcation
- `drainBody()`: consumes unread bytes before next keep-alive request

### VTSourceResponse

Simple builder for HTTP response:
- Status code + reason phrase
- Header map (`LinkedHashMap` preserves order)
- `writeHeadersTo(OutputStream)`: writes status line + headers + CRLF, does NOT flush

### ContentLengthInputStream

`FilterInputStream` that:
- Reads exactly N bytes then returns -1
- `drain()` consumes remaining bytes (positions stream at next request boundary)
- `close()` calls `drain()` but does NOT close underlying stream (keep-alive safe)

### VirtualThreadWorkerPool

Adapts `ExecutorService` to Axis2 `WorkerPool`:
- `execute(Runnable)` → `executor.submit(task)` (spawns new VT)
- `getActiveCount()` → -1 (not trackable)
- `getQueueSize()` → 0 (no queue)
- `shutdown(int timeout)` → `executor.shutdown()` + `awaitTermination()`

---

## Request Lifecycle

```
1. Client connects → serverSocket.accept()
2. vtExecutor.submit(VTBlockingServerWorker) → VT starts
3. VTBlockingServerWorker.run():
   a. Create shared BufferedInputStream/BufferedOutputStream
   b. Enter keep-alive loop:
      i.   VTSourceRequest.parse() — reads request line + headers
      ii.  Create Axis2 MessageContext
      iii. Set VT_SOURCE_REQUEST, VT_SOURCE_CONFIGURATION, OUT_TRANSPORT_INFO
      iv.  Determine REST vs SOAP
      v.   Call processEntityEnclosingRequest() or processNonEntityEnclosingRESTHandler()
      vi.  Inside those methods: AxisEngine.receive(msgContext)
      vii. Axis2 IN flow → Synapse meditation sequence → IN/OUT mediation
      viii. OUT flow reaches VTPassThroughHttpSender.invoke()
```

## Response Lifecycle

```
1. VTPassThroughHttpSender.invoke() decides: backend call or client response

2a. Backend call path:
    i.   sendToBackend() — build Apache HC4 request
    ii.  httpClient.execute() — blocking, in same VT
    iii. Wrap as VTTargetResponse
    iv.  VTBlockingClientWorker(targetConfig, msgCtx, response).run() — inline
    v.   ClientWorker builds response MessageContext
    vi.  AxisEngine.receive(responseMsgCtx) — response IN flow
    vii. Response flow reaches VTPassThroughHttpSender.invoke() again
    viii. This time OUT_TRANSPORT_INFO is VTBlockingServerWorker → submitResponse()
    ix.  Finally block in ClientWorker guarantees submitResponse() if not yet called

2b. Direct response path (no backend):
    i.   VTPassThroughHttpSender.invoke() finds VTBlockingServerWorker
    ii.  Calls serverWorker.submitResponse(msgCtx)

3. submitResponse():
   i.   Build VTSourceResponse (status, headers)
   ii.  Strip hop-by-hop headers
   iii. Set Connection: keep-alive / close
   iv.  Serialize body to byte array → set Content-Length
   v.   Write status line + headers + body to BufferedOutputStream
   vi.  Flush
   vii. Set responseSent = true

4. Back in keep-alive loop:
   i.   Drain unread request body
   ii.  Loop to next request or exit
```

---

## Keep-Alive Support

- HTTP/1.1 default: keep-alive. HTTP/1.0: only if `Connection: keep-alive`.
- Entity-enclosing requests without `Content-Length` → keep-alive disabled (can't demarcate body)
- Between requests: `VTSourceRequest.drainBody()` consumes unread bytes via `ContentLengthInputStream.drain()`
- Shared `BufferedInputStream`/`BufferedOutputStream` are reused across requests on the same connection
- Idle timeout: `keepAliveTimeout` (default 115s) controls `setSoTimeout()` while waiting for next request
- Request processing timeout: restored to `requestSoTimeout` after request line is parsed
- Error responses always set `Connection: close`
- If response not sent, finally block writes a last-resort response

---

## Axis2 Integration

### Transport Names
- Listener: `vt-http`
- Sender: `vt-http`

### Message Context Properties

| Property Key | Type | Set By | Description |
|---|---|---|---|
| `VT_SOURCE_REQUEST` | `VTSourceRequest` | ServerWorker | Parsed HTTP request |
| `VT_SOURCE_CONFIGURATION` | `SourceConfiguration` | ServerWorker | Listener config |
| `VT_SOURCE_CONNECTION` | `Socket` | ServerWorker | Client socket |
| `VT_TARGET_RESPONSE` | `VTTargetResponse` | Sender | Backend response |
| `VT_TARGET_CONFIGURATION` | `TargetConfiguration` | Sender | Sender config |
| `OUT_TRANSPORT_INFO` | `VTBlockingServerWorker` | ServerWorker | For response writing |
| `PASS_THROUGH_PIPE` | `VTInputStreamPipe` | ServerWorker/ClientWorker | Request/response body stream |

### Transport In/Out Mapping

The `VTBlockingServerWorker.createMessageContext()` method sets:
- **incomingTransportName** = `"http"` or `"https"` (NOT `"vt-http"`) — so Axis2 dispatch works (services are exposed on `http`/`https`)
- **transportOut** = VT sender (`"vt-http"`) — so response goes through `VTPassThroughHttpSender`

### WorkerPool

- `VirtualThreadWorkerPool` wraps the same `ExecutorService` as the accept loop
- Passed to `SourceConfiguration` constructor so `SourceConfiguration.build()` does NOT create a platform thread pool
- Published to `ConfigurationContext` via `PASS_THROUGH_TRANSPORT_WORKER_POOL` property
- The sender also reads this property — so sender-side Axis2 dispatch also uses virtual threads

---

## axis2.xml Configuration

```xml
<!-- Listener (HTTP) -->
<transportReceiver name="vt-http"
    class="org.apache.synapse.transport.passthru.vt.VTPassThroughHttpListener">
    <parameter name="port">8290</parameter>
    <!-- Optional -->
    <parameter name="bind-address">0.0.0.0</parameter>
    <parameter name="hostname">localhost</parameter>
    <parameter name="so_timeout">60000</parameter>
    <parameter name="backlog">1024</parameter>
    <parameter name="tcp_nodelay">true</parameter>
    <parameter name="keep_alive_timeout">115000</parameter>
</transportReceiver>

<!-- Sender (HTTP) -->
<transportSender name="vt-http"
    class="org.apache.synapse.transport.passthru.vt.VTPassThroughHttpSender"/>
```

---

## Configuration Parameters

### Listener Parameters (VTConstants)

| Parameter | Constant | Default | Description |
|-----------|----------|---------|-------------|
| `port` | `PARAM_PORT` | — | Listening port (required) |
| `bind-address` | `PARAM_BIND_ADDRESS` | all interfaces | Bind address |
| `hostname` | `PARAM_HOSTNAME` | — | Hostname for EPR generation |
| `so_timeout` | `PARAM_SO_TIMEOUT` | 60000 ms | Socket read timeout during request processing |
| `backlog` | `PARAM_BACKLOG` | 1024 | ServerSocket backlog |
| `tcp_nodelay` | `PARAM_TCP_NODELAY` | true | TCP_NODELAY (disable Nagle) |
| `keep_alive_timeout` | `PARAM_KEEP_ALIVE_TIMEOUT` | 115000 ms | Idle timeout between keep-alive requests |

### Sender Parameters

| Parameter | Constant | Default | Description |
|-----------|----------|---------|-------------|
| `connect_timeout` | `PARAM_CONNECT_TIMEOUT` | 10000 ms | Backend connection timeout |
| `so_timeout` | `PARAM_SO_TIMEOUT` | 60000 ms | Backend read timeout |

### Other Constants

| Constant | Value | Description |
|----------|-------|-------------|
| `STREAM_BUFFER_SIZE` | 8192 | Buffer size for socket stream wrapping |
| `DEFAULT_CONTENT_TYPE` | `application/octet-stream` | Fallback content type |
| `VT_THREAD_PREFIX` | `vt-passthru-` | Virtual thread name prefix |
| `HOP_BY_HOP_HEADERS` | Set of 9 | Headers stripped when forwarding responses |

---

## Key Design Decisions

1. **Blocking I/O over NIO:** Virtual threads make blocking I/O as scalable as NIO while being simpler. No IOReactor, no async callbacks, no Pipe buffers.

2. **Single VT per connection:** The entire request-response cycle (including backend calls) runs in one virtual thread. This avoids context-switching overhead and simplifies error handling.

3. **VTBlockingClientWorker.run() called inline:** The sender calls `.run()` not `.submit()` — stays on the same VT. Only explicit `workerPool.execute()` calls (Clone, Iterate mediators) spawn new VTs.

4. **Reuse SourceConfiguration/TargetConfiguration:** The VT transport reuses the existing pass-through configuration classes. This maximizes compatibility with existing Synapse code that reads configuration from these objects.

5. **Transport name aliasing:** Incoming transport name is set to `"http"`/`"https"` (not `"vt-http"`) so services exposed on standard transports are reachable. Outgoing transport is set to the VT sender name.

6. **Apache HttpClient 4.x for outbound:** Uses a shared `CloseableHttpClient` with connection pooling (`PoolingHttpClientConnectionManager`). Blocking `execute()` is cheap in a VT.

7. **Content-Length based framing:** Responses are serialized to a byte array buffer first to determine Content-Length. No chunked encoding on the response side. This simplifies keep-alive.

8. **Hop-by-hop header stripping:** Backend response headers like `Transfer-Encoding`, `Connection`, `Keep-Alive` are stripped before writing to client. The VT transport sets its own framing headers.

---

## Known Caveats & Risks

1. **ThreadLocal usage:** Axis2 and some mediators use `ThreadLocal`. Virtual threads may be scheduled on different carrier threads, potentially causing issues with `ThreadLocal`-dependent code.

2. **synchronized blocks:** `synchronized` in Axis2/mediator code can **pin** virtual threads to carrier threads, reducing scalability. Monitor with `-Djdk.tracePinnedThreads=short`.

3. **No chunked response:** Responses are buffered entirely in memory (byte array) to compute Content-Length. Large responses will consume heap.

4. **Connection pool sizing:** Apache HC4 pool defaults (200/route, 1000 total). May need tuning for high concurrency.

5. **WorkerPool.getActiveCount() returns -1:** JMX / monitoring tools expecting real thread pool metrics will get -1 for active count and 0 for queue size.

6. **Port offset:** Uses `System.getProperty("portOffset", "0")` — same mechanism as the original transport.

---

## Relationship to Original Pass-Through Transport

| Original (NIO) | VT Equivalent |
|----------------|---------------|
| `PassThroughHttpListener` | `VTPassThroughHttpListener` |
| `PassThroughHttpSender` | `VTPassThroughHttpSender` |
| `ServerWorker` | `VTBlockingServerWorker` |
| `ClientWorker` | `VTBlockingClientWorker` |
| `SourceRequest` | `VTSourceRequest` |
| `SourceResponse` | `VTSourceResponse` |
| `TargetResponse` | `VTTargetResponse` |
| `Pipe` (NIO buffers) | `VTInputStreamPipe` (plain InputStream) |
| `NativeWorkerPool` (platform threads) | `VirtualThreadWorkerPool` (virtual threads) |
| `DefaultListeningIOReactor` | `ServerSocket` + accept loop |
| `IOReactorExceptionHandler` | try/catch in accept loop |
| `SourceHandler` (NIO event handler) | Part of `VTBlockingServerWorker` |
| `TargetHandler` (NIO event handler) | Part of `VTPassThroughHttpSender.sendToBackend()` |

### Reused from original transport (not copied):
- `SourceConfiguration` — listener configuration
- `TargetConfiguration` — sender configuration
- `PassThroughConstants` — shared constants
- `PassThroughConfiguration` — properties file config
- `PassThroughTransportMetricsCollector` — JMX metrics
- `PassThroughTransportUtils` — header/EPR utilities
- `SessionContextUtil` — session context helper
- `WorkerState` — enum for worker lifecycle states

---

## Package Dependencies

### Internal (same project)
- `org.apache.synapse.transport.passthru` — `PassThroughConstants`, `WorkerState`, configs
- `org.apache.synapse.transport.passthru.config` — `SourceConfiguration`, `TargetConfiguration`, `PassThroughConfiguration`
- `org.apache.synapse.transport.passthru.jmx` — `PassThroughTransportMetricsCollector`
- `org.apache.synapse.transport.passthru.util` — `PassThroughTransportUtils`, `SessionContextUtil`
- `org.apache.synapse.transport.nhttp` — `NhttpConstants`
- `org.apache.synapse.transport.nhttp.util` — `MessageFormatterDecoratorFactory`, `NhttpUtil`, `RESTUtil`
- `org.apache.synapse.transport.http.conn` — `Scheme`

### External
- `org.apache.axis2` — Core engine, `MessageContext`, `AxisEngine`, `TransportListener`, `TransportSender`, `WorkerPool`
- `org.apache.axiom` — SOAP/OM model
- `org.apache.http` (HttpClient 4.x) — `CloseableHttpClient`, `RequestBuilder`, `PoolingHttpClientConnectionManager`
- `org.apache.commons.logging` — Logging
