package com.synapse.core.deployers;

import com.synapse.core.artifacts.inbound.Inbound;
import com.synapse.core.artifacts.inbound.Parameter;
import com.synapse.core.artifacts.utils.Position;
import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class InboundDeployer {

    private static final String SYNAPSE_NAMESPACE = "http://ws.apache.org/ns/synapse";

    public Inbound unmarshal(String xmlData, Position position) throws XMLStreamException {
        OMElement inboundElement = AXIOMUtil.stringToOM(xmlData);

        if (inboundElement == null || !"inboundEndpoint".equals(inboundElement.getLocalName())) {
            return null;
        }

        Inbound newInbound = new Inbound();
        newInbound.setPosition(position);

        OMAttribute nameAttr = inboundElement.getAttribute(new QName("name"));
        OMAttribute sequenceAttr = inboundElement.getAttribute(new QName("sequence"));
        OMAttribute protocolAttr = inboundElement.getAttribute(new QName("protocol"));
        OMAttribute suspendAttr = inboundElement.getAttribute(new QName("suspend"));
        OMAttribute onErrorAttr = inboundElement.getAttribute(new QName("onError"));

        if (nameAttr != null) newInbound.setName(nameAttr.getAttributeValue());
        if (sequenceAttr != null) newInbound.setSequence(sequenceAttr.getAttributeValue());
        if (protocolAttr != null) newInbound.setProtocol(protocolAttr.getAttributeValue());
        if (suspendAttr != null) newInbound.setSuspend(String.valueOf(Boolean.parseBoolean(suspendAttr.getAttributeValue())));
        if (onErrorAttr != null) newInbound.setOnError(onErrorAttr.getAttributeValue());

        QName parametersQName = new QName(SYNAPSE_NAMESPACE, "parameters");
        QName parameterQName = new QName(SYNAPSE_NAMESPACE, "parameter");

        OMElement parametersElement = inboundElement.getFirstChildWithName(parametersQName);
        if (parametersElement != null) {
            List<Parameter> parameters = new ArrayList<>();
            Iterator<?> it = parametersElement.getChildrenWithName(parameterQName);

            while (it.hasNext()) {
                OMElement paramElement = (OMElement) it.next();
                OMAttribute paramNameAttr = paramElement.getAttribute(new QName("name"));
                if (paramNameAttr != null) {
                    String paramName = paramNameAttr.getAttributeValue();
                    String paramValue = paramElement.getText().trim();
                    parameters.add(new Parameter(paramName, paramValue));
//                    System.out.println("Added parameter: " + paramName + " = " + paramValue);
                }
            }
            newInbound.setParameters(parameters);
        } else {
//            System.out.println("No <parameters> found in XML.");
        }

        return newInbound;
    }
}
