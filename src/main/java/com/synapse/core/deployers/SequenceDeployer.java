//package com.synapse.core.deployers;
//
//import com.synapse.core.artifacts.Sequence;
//import com.synapse.core.artifacts.api.Resource;
//import com.synapse.core.artifacts.Mediator;
//import com.synapse.core.artifacts.utils.Position;
//
//import org.apache.axiom.om.OMElement;
//
//import java.util.ArrayList;
//import java.util.Iterator;
//import java.util.List;
//
//public class SequenceDeployer {
//
//    public Sequence unmarshal(OMElement sequenceElement, Position position, String sequenceType, Resource resource) {
//        List<Mediator> mediatorList = new ArrayList<>();
//
//        Position newPosition = new Position();
//        newPosition.setFileName(position.getFileName());
//        newPosition.setLineNo(sequenceElement.getLineNumber());
//        newPosition.setHierarchy(position.getHierarchy() + "->" + resource.getUriTemplate() + "->" + sequenceType);
//
//        Iterator<OMElement> mediators = sequenceElement.getChildElements();
//
//        while (mediators.hasNext()) {
//            OMElement mediatorElement = mediators.next();
//            if ("log".equals(mediatorElement.getLocalName())) {
//                LogMediatorDeployer logMediatorDeployer = new LogMediatorDeployer();
//                Mediator mediator = null;
//                try {
//                    mediator = logMediatorDeployer.unmarshal(mediatorElement, newPosition);
//                } catch (Exception e) {
//                    throw new RuntimeException(e);
//                }
//                mediatorList.add(mediator);
//            }
//        }
//
//        Sequence sequence = new Sequence();
//        sequence.setMediatorList(mediatorList);
//        sequence.setPosition(newPosition);
//        return sequence;
//    }
//}

package com.synapse.core.deployers;

import com.synapse.core.artifacts.Sequence;
import com.synapse.core.artifacts.Mediator;
import com.synapse.core.artifacts.utils.Position;
import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SequenceDeployer {

    public Sequence unmarshal(String xmlData, Position position) throws XMLStreamException {
        OMElement sequenceElement = AXIOMUtil.stringToOM(xmlData);

        if (sequenceElement == null || !sequenceElement.getLocalName().equals("sequence")) {
            return null;
        }
        OMAttribute nameAttr = sequenceElement.getAttribute(new javax.xml.namespace.QName("name"));
        if (nameAttr != null) {
                String sequenceName = nameAttr.getAttributeValue();

                Position newPosition = new Position(1, position.getFileName(), sequenceName);

                Sequence newSequence = unmarshalSequence(sequenceElement, newPosition);

                newSequence.setName(sequenceName);
                return newSequence;
        }
        return null;
    }
    private Sequence unmarshalSequence(OMElement sequenceElement, Position position) {
        List<Mediator> mediatorList = new ArrayList<>();

        for (Iterator it = sequenceElement.getChildrenWithName(new QName("log")); it.hasNext(); ) {
            OMElement mediatorElement = (OMElement) it.next();
            LogMediatorDeployer logMediatorDeployer = new LogMediatorDeployer();
            try {
                mediatorList.add(logMediatorDeployer.unmarshal(mediatorElement, position));
            } catch (Exception e) {
                // Handle error appropriately
                e.printStackTrace();
            }
        }

        Sequence sequence = new Sequence();
        sequence.setMediatorList(mediatorList);
        sequence.setPosition(position);
        return sequence;
    }
}