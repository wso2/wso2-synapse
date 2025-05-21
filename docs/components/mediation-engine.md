# Mediation Engine

The Mediation Engine is the central component responsible for executing sequences in response to inbound messages. It implements the `InboundMessageMediator` interface.

## Overview

The Mediation Engine:

1. Accepts messages from inbound endpoints
2. Looks up the appropriate sequence
3. Executes the sequence with the message context
4. Handles any errors that occur during processing

## Implementation

```java
public class MediationEngine implements InboundMessageMediator {
    
    private final ConfigContext configContext;
    private final ExecutorService executorService;

    private static final Logger log = LogManager.getLogger(MediationEngine.class);

    public MediationEngine(ConfigContext configContext) {
        this.configContext = configContext;
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
    }

    @Override
    public void mediateInboundMessage(String seqName, MsgContext msgContext) {
        executorService.submit(() -> {
            try {
                var sequence = configContext.getSequenceMap().get(seqName);
                if (sequence == null) {
                    log.error("Sequence {} not found", seqName);
                    return;
                }

                sequence.execute(msgContext);
            } catch (Exception e) {
                log.error("Error executing sequence {}", seqName, e);
            }
        });
    }
}
```