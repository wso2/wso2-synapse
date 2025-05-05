# Synapse Java Project

## Overview

The Synapse Java project is a lightweight integration engine designed to process and mediate messages between various systems. It is inspired by the Apache Synapse project and provides a modular, extensible, and highly configurable architecture for handling inbound and outbound messages, sequences, APIs, and mediators.

This project is built using Java 21 and Maven, leveraging modern Java features such as virtual threads for concurrency and modular design principles.

---

## Table of Contents

1. [Architecture](#architecture)
2. [Project Structure](#project-structure)
3. [Core Components](#core-components)
4. [Inbound Endpoints](#inbound-endpoints)
5. [Sequences and Mediators](#sequences-and-mediators)
6. [APIs](#apis)
7. [Configuration](#configuration)
8. [Logging](#logging)
<!-- 9. [Building and Running](#building-and-running)
10. [Testing](#testing)
11. [Future Enhancements](#future-enhancements) -->

---

## Architecture

The Synapse Java project follows a modular architecture based on the **Ports and Adapters** (Hexagonal) design pattern. This ensures that the core business logic is decoupled from external systems, making the system highly testable and extensible.

### Key Architectural Components:

1. **Core Domain**: Contains the core business logic and models (e.g., `Inbound`, `Sequence`, `API`).
2. **Ports**: Define interfaces for inbound and outbound communication (e.g., `InboundEndpoint`, `InboundMessageMediator`).
3. **Adapters**: Implement the ports to interact with specific protocols or systems (e.g., `FileInboundEndpoint`, `HttpInboundEndpoint`).
4. **Deployers**: Handle the deployment of artifacts such as sequences, APIs, and inbound endpoints.
5. **Mediation Engine**: The central component that mediates messages between inbound endpoints and sequences.

---

## Project Structure

```plaintext
synapse-java/
├── artifacts/
│ ├── Inbounds/ # XML inbound endpoint definitions
│ ├── Sequences/ # XML sequences
| └── APIs/ # XML apis
├── src/
│ ├── main/
│ │ └── java/
│ │ ├── com.synapse.adapters.inbound/ # File/HTTP endpoints
│ │ ├── com.synapse.adapters.mediation/ # Engine
│ │ ├── com.synapse.core.ports/ # Interfaces
│ │ ├── com.synapse.core.domain/ # API, Sequence, Mediator
│ │ ├── com.synapse.core.deployers/ # XML loaders
│ │ └── com.synapse.synapse/ # App entry point
├── pom.xml
└── README.md
```

## Core Components

### 1. **ConfigContext**
- A singleton class that acts as the central registry for all deployed artifacts (e.g., APIs, sequences, inbound endpoints).
- Provides methods to add and retrieve artifacts.

### 2. **Inbound Endpoints**
- Represent entry points for messages into the system.
- Implement the `InboundEndpoint` interface.
- Examples:
  - `FileInboundEndpoint`: Polls files from a directory.
  - `HttpInboundEndpoint`: Listens for HTTP requests.

### 3. **Sequences**
- Define a series of mediators to process messages.
- Implement the `Mediator` interface.
- Can contain multiple mediators such as `LogMediator`.

### 4. **APIs**
- Represent RESTful APIs with resources and sequences.
- Each resource can have an `inSequence` and a `faultSequence`.

### 5. **Mediation Engine**
- Implements the `InboundMessageMediator` interface.
- Responsible for executing sequences based on inbound messages.

---

## Inbound Endpoints

Inbound endpoints are responsible for receiving messages from external systems. They are configured using XML files and deployed dynamically.

### Supported Inbound Protocols:
1. **File**: Polls files from a directory.
2. **HTTP**: Listens for HTTP requests.

### Example Configuration:
#### File Inbound Endpoint (`artifacts/Inbounds/fileInbound.xml`):
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

#### File Inbound Endpoint (`artifacts/Inbounds/fileInbound.xml`):
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

## Sequences and Mediators

## Sequences
A sequence is a collection of mediators that process a message.
Defined in XML files and deployed dynamically.

### Example Sequence (`artifacts/Sequences/inboundSeq.xml`):
```xml
<sequence name="inboundSeq" trace="disable">
    <log category="INFO">
        <message>message from inbound</message>
    </log>
</sequence>
```
## Mediators
Mediators are components that perform specific actions on messages.
Example: LogMediator logs messages to the console.

## APIs
APIs are defined using XML files and consist of resources. Each resource can have an inSequence and a faultSequence.

## Configuration
Logging Configuration
Logging is configured using log4j2.properties located in src/main/resources/

### Example Configuration
```java
appenders = console
appender.console.type = Console
appender.console.name = STDOUT
appender.console.layout.type = PatternLayout
appender.console.layout.pattern = [%d{yyyy-MM-dd HH:mm:ss.SSS}] %-5level {%C{1}} - %msg%n

rootLogger.level = debug
rootLogger.appenderRefs = stdout
rootLogger.appenderRef.stdout.ref = STDOUT
```
