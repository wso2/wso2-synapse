/*
 * Copyright (c) 2025, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.synapse.core.deployers;

import com.synapse.core.artifacts.api.API;
import com.synapse.core.artifacts.api.Resource;
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

public class APIDeployer {

    public API unmarshal(String xmlData, Position position) throws XMLStreamException {
        OMElement apiElement = AXIOMUtil.stringToOM(xmlData);
        if (apiElement == null || !"api".equals(apiElement.getLocalName())) {
            return null;
        }

        OMAttribute nameAttr = apiElement.getAttribute(new QName("name"));
        OMAttribute contextAttr = apiElement.getAttribute(new QName("context"));

        if (nameAttr != null && contextAttr != null) {
            String apiName = nameAttr.getAttributeValue();
            String apiContext = contextAttr.getAttributeValue();

            Position newPosition = new Position(1, position.getFileName(), apiName);
            List<Resource> resourceList = new ArrayList<>();

            for (Iterator it = apiElement.getChildrenWithName(new QName("resource")); it.hasNext(); ) {
                OMElement resourceElement = (OMElement) it.next();
                try {
                    Resource resource = unmarshalResource(resourceElement, newPosition);
                    if (resource != null) {
                        resourceList.add(resource);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (resourceList.isEmpty()) {
                System.out.println("Warning: No <resource> elements found in API: " + apiName);
                return null; // Prevent returning an API without resources
            }

            return new API(apiContext, apiName, resourceList, newPosition);
        }
        return null;
    }


    private Resource unmarshalResource(OMElement resourceElement, Position parentPosition) {
        OMAttribute methodsAttr = resourceElement.getAttribute(new QName("methods"));
        OMAttribute uriTemplateAttr = resourceElement.getAttribute(new QName("uri-template"));

        if (methodsAttr == null || uriTemplateAttr == null) {
            return null;
        }

        String methods = methodsAttr.getAttributeValue();
        String uriTemplate = uriTemplateAttr.getAttributeValue();
        Position newPosition = new Position(parentPosition.getLineNo(), parentPosition.getFileName(),
                parentPosition.getHierarchy() + "->" + uriTemplate);

        Sequence inSequence = null, faultSequence = null;

        for (Iterator it = resourceElement.getChildElements(); it.hasNext(); ) {
            OMElement childElement = (OMElement) it.next();
            String elementName = childElement.getLocalName();

            if ("inSequence".equals(elementName)) {
                inSequence = unmarshalSequence(childElement, newPosition, "inSequence");
            } else if ("faultSequence".equals(elementName)) {
                faultSequence = unmarshalSequence(childElement, newPosition, "faultSequence");
            }
        }

        return new Resource(methods, uriTemplate, inSequence, faultSequence);
    }
    private Sequence unmarshalSequence(OMElement sequenceElement, Position parentPosition, String sequenceType) {
        Position sequencePosition = new Position(
                parentPosition.getLineNo(),
                parentPosition.getFileName(),
                parentPosition.getHierarchy() + "->" + sequenceType);

        List<Mediator> mediatorList = new ArrayList<>();

        if ("inSequence".equals(sequenceElement.getLocalName()) || "faultSequence".equals(sequenceElement.getLocalName())) {
            OMElement innerSequence = sequenceElement.getFirstChildWithName(new QName("sequence"));
            if (innerSequence != null) {
                sequenceElement = innerSequence;
            }
        }

        for (Iterator it = sequenceElement.getChildElements(); it.hasNext(); ) {
            OMElement mediatorElement = (OMElement) it.next();
            if ("log".equals(mediatorElement.getLocalName())) {
                LogMediatorDeployer logMediatorDeployer = new LogMediatorDeployer();
                try {
                    mediatorList.add(logMediatorDeployer.unmarshal(mediatorElement, sequencePosition));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            // Add other mediator types here as needed.
        }

        if (mediatorList.isEmpty()) {
            System.out.println("Warning: No mediators found in sequence: " + sequenceType);
        }

        return new Sequence(mediatorList, sequencePosition, "inline");
    }


}
