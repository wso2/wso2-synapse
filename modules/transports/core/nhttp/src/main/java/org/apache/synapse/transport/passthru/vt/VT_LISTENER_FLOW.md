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


| Case | Request | Response |
|---|---|---|
| `GET` / `DELETE` without body | Not streamed | Streamed |
| `GET` / `DELETE` with body | Not streamed | Streamed |
| `POST` / `PUT` / `PATCH` with body| Streamed | Streamed |
| `POST` / `PUT` / `PATCH` without body | Not streamed | Streamed |

