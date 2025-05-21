# Hexagonal Architecture 

## Core Principles of Hexagonal Architecture
Hexagonal Architecture (also known as Ports and Adapters pattern) is an architectural pattern that separates the core application logic from external concerns. It organizes code into three main areas:

1. Core Domain (inside the hexagon): Contains the business logic and entities
2. Ports (edges of the hexagon): Define interfaces for how the core communicates with the outside world
3. Adapters (outside the hexagon): Implement the ports to connect with specific technologies

## Synapse Implementation
The Synapse project structure is deliberately designed to implement the hexagonal architecture pattern:

1. Core Domain (Center)
The core domain contains the essential business logic and entities of Synapse:
```plaintext
com.synapse.core.artifacts/
├── API.java
├── ConfigContext.java
├── Mediator.java
├── Sequence.java
├── endpoint/
│   └── Endpoint.java
├── inbound/
│   └── Inbound.java
├── api/
│   ├── Resource.java
│   └── CORSConfig.java
└── utils/
    └── Position.java
```
2. Ports (Inner Boundaries)
Ports define how the core domain interacts with the outside world through interfaces:
```plaintext
com.synapse.core.ports/
├── InboundEndpoint.java
├── InboundMessageMediator.java
└── EndpointProvider.java
```
3. Adapters (Outer Layer)
Adapters implement the ports to connect the core domain with specific technologies:
```plaintext
com.synapse.adapters.inbound/
├── FileInboundEndpoint.java
├── HttpInboundEndpoint.java
└── InboundFactory.java
```

