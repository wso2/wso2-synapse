# Sequence Execution

Sequence execution is a core process in Synapse that handles message processing through a chain of mediators.

## Execution Flow
 
1. The MediationEngine receives a message from an inbound endpoint
2. It looks up the requested sequence from the ConfigContext
3. The sequence executes each mediator in its list
4. If any mediator returns false, the sequence halts execution and returns false

```java
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
```

## Deployment

Sequences are deployed by the SequenceDeployer, which parses XML and creates Sequence objects:

```java
public Sequence unmarshal(String xmlData, Position position) throws XMLStreamException {
    OMElement sequenceElement = AXIOMUtil.stringToOM(xmlData);

    if (sequenceElement == null || !sequenceElement.getLocalName().equals("sequence")) {
        return null;
    }
    
    OMAttribute nameAttr = sequenceElement.getAttribute(new QName("name"));
    if (nameAttr != null) {
        String sequenceName = nameAttr.getAttributeValue();
        Position newPosition = new Position(1, position.getFileName(), sequenceName);
        Sequence newSequence = unmarshalSequence(sequenceElement, newPosition);
        newSequence.setName(sequenceName);
        return newSequence;
    }
    return null;
}
```