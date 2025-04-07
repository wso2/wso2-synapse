package com.synapse.core.deployers;


import com.synapse.core.artifacts.mediators.LogMediator;
import com.synapse.core.artifacts.Mediator;
import com.synapse.core.artifacts.utils.Position;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMAttribute;

import javax.xml.namespace.QName;

public class LogMediatorDeployer implements MediatorDeployer {

    @Override
    public Mediator unmarshal(OMElement element, Position position) throws Exception {
        if (!"log".equals(element.getLocalName())) {
            throw new Exception("Invalid element, expected <log> but got <" + element.getLocalName() + ">");
        }

        OMAttribute categoryAttr = element.getAttribute(new QName("category"));
        String category = (categoryAttr != null) ? categoryAttr.getAttributeValue() : "";

        OMElement messageElement = element.getFirstChildWithName(new QName("message"));
        String message = (messageElement != null) ? messageElement.getText() : "";

        position.setHierarchy(position.getHierarchy() + "->log");

        return new LogMediator(category, message, position);
    }

}
