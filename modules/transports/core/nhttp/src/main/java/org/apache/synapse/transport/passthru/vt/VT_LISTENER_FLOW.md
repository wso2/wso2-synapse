```mermaid
flowchart TD
    A[Client] --> B[ServerSocket]
    B --> C[Accept Loop]
    C --> D[Acquire Permit]
    D --> E[Accept Socket]
    E --> F[Set Socket Options]
    F --> G[Virtual Thread Executor]
    G --> H[Virtual Thread]
    H --> I[handleConnection]
    I --> J[DefaultBHttpServerConnection]
    J --> K[HttpService.handleRequest]
    K --> L[VTBlockingServerWorker]
    L --> M[Synapse / Axis2]
    M --> N[HTTP Response]
    N --> A

    C -.-> O[Next Client]
    O --> C

    K -. keep-alive .-> K
```

## VT Streaming Cases

```mermaid
flowchart TD
    A[Inbound request] --> B{Has request body?}
    B -- No --> C[Set NO_ENTITY_BODY]
    B -- Yes --> D[Store body as VT_STREAM_PIPE]

    C --> E[Call mediator blocking backend call]
    D --> F{Message built before call?}

    F -- No --> G[Stream request body to backend from VT_STREAM_PIPE]
    F -- Yes --> H[Serialize built AXIOM message to backend]

    E --> I[Backend response]
    G --> I
    H --> I

    I --> J{Backend response has body?}
    J -- Yes --> K[Store backend body as VT_STREAM_PIPE]
    J -- No --> L[Set NO_ENTITY_BODY]

    K --> M{Response content-aware mediation?}
    M -- No --> N[Respond streams VT_STREAM_PIPE to client]
    M -- Yes --> O[Build message from VT_STREAM_PIPE, then format response]
    L --> P[Respond with status and headers only]
```

| Case | Request | Response |
|---|---|---|
| `GET` / `DELETE` without body | Not streamed | Streamed |
| `GET` / `DELETE` with body | Not streamed | Streamed |
| `POST` / `PUT` / `PATCH` with body, not built | Streamed | Streamed |
| `POST` / `PUT` / `PATCH` with body, already built | Not streamed | Streamed |
| `POST` / `PUT` / `PATCH` without body | Not streamed | Streamed |

## Streaming Rules

| Flag / property | Meaning |
|---|---|
| `VT_STREAM_PIPE` | Raw body stream is available for pass-through. On request it is the client body; on response it is the backend body. |
| `MESSAGE_BUILDER_INVOKED` | A content-aware mediator already built the body. Request streaming stops and the built message is serialized instead. |
| `NO_ENTITY_BODY` | Current message has no body entity. Response path should send status and headers only unless a new response pipe is later set. |
| `VT_BACKEND_CALL` | Marks outbound call as VT backend call so `VTHttpSender` uses the VT streaming response setup. |

## End-To-End Streaming

End-to-end streaming is preserved when:

1. The inbound request body remains in `VT_STREAM_PIPE`.
2. No content-aware mediator builds the request before the blocking call.
3. The backend response is stored as `VT_STREAM_PIPE`.
4. No response-side mediator, such as Call mediator target enrichment, calls `buildMessage()` before `<respond/>`.
