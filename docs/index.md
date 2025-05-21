# Synapse Java Lightweight Threads Project

## Overview

The Synapse Java project is a lightweight integration engine designed to process and mediate messages between various systems. It is inspired by the Apache Synapse project and provides a modular, extensible, and highly configurable architecture for handling inbound and outbound messages, sequences, APIs, and mediators.

This project is built using Java 21 and Maven, leveraging modern Java features such as virtual threads for concurrency and modular design principles.

## Key Features

- **Modular Architecture**: Based on the Ports and Adapters (Hexagonal) design pattern.
- **Runtime Configuration**: Dynamic deployment of artifacts through XML files.
- **Multiple Inbound Protocols**: Support for file and HTTP inbound endpoints.
- **Message Mediation**: Configurable sequences with mediators for message processing.
- **REST API Support**: Define and expose APIs with resources and sequences.
- **Virtual Threads**: Uses Java 21 virtual threads for high concurrency and scalability.

## Project Structure

```plaintext
synapse-java/
├── artifacts/
│ ├── Inbounds/ # XML inbound endpoint definitions
│ ├── Sequences/ # XML sequences
| └── APIs/ # XML APIs
├── repository/
│ └── conf/ # Configuration files like axis2.xml
├── src/
│ ├── main/
│ │ └── java/
│ │ ├── com.synapse.adapters.inbound/ # File/HTTP endpoints
│ │ ├── com.synapse.adapters.mediation/ # Engine
│ │ ├── com.synapse.core.ports/ # Interfaces
│ │ ├── com.synapse.core.artifacts/ # API, Sequence, Mediator
│ │ ├── com.synapse.core.deployers/ # XML loaders
│ │ └── com.synapse.synapse/ # App entry point
├── pom.xml
└── README.md
```

## Quick Start

To build and run the Synapse Java project:

1. Clone the repository
2. Run mvn clean package to build the project
3. Create a folder called artifacts in the same directory as the jar file and add the xml files in the 4.suitable subfolders APIs, Inbounds or Sequences
4. Execute java -jar synapse-java-1.0-SNAPSHOT.jar to start the application