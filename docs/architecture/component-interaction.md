# Component Interaction
This is how the key components in Synapse interact with each other during message processing.

## Overview
The Synapse architecture is built on clear component boundaries with well-defined interactions. The following diagram shows the typical message flow:

```plaintext
[Inbound Endpoint] → [Mediation Engine] → [Sequence] → [Mediators]
```

## Key Interactions

1. **Inbound Endpoint to Mediation Engine**

    When an inbound endpoint receives a message (HTTP request, file, etc.), it creates a MsgContext and passes it to the Mediation Engine:

    ```java
    // From FileInboundEndpoint
    MsgContext msgContext = new MsgContext();
    Message message = new Message(content, "application/xml");
    msgContext.setMessage(message);

    // Add context properties
    msgContext.setProperties(Map.of(
        "source.file.name", file.getName(),
        "source.file.path", file.getAbsolutePath()
    ));

    // Call the mediator to process the message
    mediator.mediateInboundMessage(config.getSequence(), msgContext);
    ```
    The inbound endpoint doesn't know or care about how the message will be processed—it simply passes it to the mediator interface.

2. **Mediation Engine to Sequence**

    The Mediation Engine looks up the appropriate sequence from the ConfigContext and executes it:

    ```java
    // From MediationEngine
    @Override
    public void mediateInboundMessage(String seqName, MsgContext msgContext) {
        executorService.submit(() -> {
            var sequence = configContext.getSequenceMap().get(seqName);
            if (sequence == null) {
                log.error("Sequence {} not found", seqName);
                return;
            }
            sequence.execute(msgContext);
        });
    }
    ```
    Key points:

    - The Mediation Engine uses Virtual Threads for efficient concurrent processing
    - It retrieves sequences from the central ConfigContext registry
    - It handles errors that might occur during sequence execution

3. **Sequence to Mediators**

    A sequence is essentially a container for mediators that are executed in order:

    ```java
    // From Sequence
    @Override
    public boolean execute(MsgContext context) {
        for (Mediator mediator : mediatorList) {
            try {
                boolean result = mediator.execute(context);
                if (!result) {
                    return false;  // Stop if a mediator fails
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
        return true;
    }
    ```
    This simple design allows for:

    - Flexible composition of mediator chains
    - Early termination if a mediator fails
    - Clean separation of responsibilities