# Mediators

Mediators are the building blocks of message processing in Synapse. Each mediator performs a specific action on a message, such as logging, transformation, or routing.

## Overview

All mediators implement the `Mediator` interface, which defines a single method:

```java
public interface Mediator {
    boolean execute(MsgContext context) throws Exception;
}
```