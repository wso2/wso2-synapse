# Sequences

Sequences are a fundamental component in the Synapse engine that define the flow of message processing. A sequence consists of an ordered list of mediators that are executed in order.

## Overview

Sequences implement the `Mediator` interface, allowing them to be nested within other sequences or used directly for message processing. Each sequence has:

- A list of mediators
- A name
- A position (for error reporting)

## Implementation

```java
public class Sequence implements Mediator {
    private List<Mediator> mediatorList;
    private Position position;
    private String name;

    public Sequence(List<Mediator> mediatorList, Position position, String name) {
        this.mediatorList = mediatorList;
        this.position = position;
        this.name = name;
    }

    public Sequence() {
        // Default constructor
    }

    @Override
    public boolean execute(MsgContext context) {
        for (Mediator mediator : mediatorList) {
            try {
                boolean result = mediator.execute(context);
                if (!result) {
                    return false;
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
        return true;
    }
}
```