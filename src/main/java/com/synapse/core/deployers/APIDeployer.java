//package com.synapse.core.deployers;
//
//
//import com.synapse.core.artifacts.api.API;
//import com.synapse.core.artifacts.api.Resource;
//import com.synapse.core.artifacts.utils.Position;
//import org.apache.axiom.om.OMAttribute;
//import org.apache.axiom.om.OMElement;
//import org.apache.axiom.om.OMException;
//import org.apache.axiom.om.OMXMLParserWrapper;
//import org.apache.axiom.om.impl.builder.StAXOMBuilder;
//
//import javax.xml.namespace.QName;
//import javax.xml.stream.XMLStreamException;
//import java.io.FileNotFoundException;
//import java.io.StringReader;
//import java.util.ArrayList;
//import java.util.Iterator;
//import java.util.List;
//
//public class APIDeployer {
//
//    public API unmarshal(String xmlData, Position position) throws XMLStreamException, OMException, FileNotFoundException {
//        OMXMLParserWrapper builder = new StAXOMBuilder(String.valueOf(new StringReader(xmlData)));
//        OMElement root = builder.getDocumentElement();
//
//        if (!"api".equals(root.getLocalName())) {
//            throw new OMException("Root element is not <api>");
//        }
//
//        API newAPI = new API();
//        newAPI.setPosition(position);
//
//        OMAttribute contextAttr = root.getAttribute(new QName("context"));
//        OMAttribute nameAttr = root.getAttribute(new QName("name"));
//
//        if (contextAttr != null) newAPI.setContext(contextAttr.getAttributeValue());
//        if (nameAttr != null) {
//            newAPI.setName(nameAttr.getAttributeValue());
//            newAPI.getPosition().setHierarchy(nameAttr.getAttributeValue());
//        }
//
//        List<Resource> resources = new ArrayList<>();
//        Iterator<OMElement> resourceElements = root.getChildrenWithName(new QName("resource"));
//
//        while (resourceElements.hasNext()) {
//            OMElement resourceElement = resourceElements.next();
//            ResourceDeployer resourceDeployer = new ResourceDeployer();
//            Resource res = resourceDeployer.unmarshal(resourceElement, newAPI.getPosition());
//            resources.add(res);
//        }
//
//        newAPI.setResources(resources);
//        return newAPI;
//    }
//}

//package com.synapse.core.deployers;
//
//import com.synapse.core.artifacts.api.API;
//import com.synapse.core.artifacts.api.Resource;
//import com.synapse.core.artifacts.Mediator;
//import com.synapse.core.artifacts.utils.Position;
//import org.apache.axiom.om.OMAttribute;
//import org.apache.axiom.om.OMElement;
//import org.apache.axiom.om.util.AXIOMUtil;
//
//import javax.xml.namespace.QName;
//import javax.xml.stream.XMLStreamException;
//import java.util.ArrayList;
//import java.util.Iterator;
//import java.util.List;
//
//public class APIDeployer {
//
//    public API unmarshal(String xmlData, Position position) throws XMLStreamException {
//        OMElement apiElement = AXIOMUtil.stringToOM(xmlData);
//        if (apiElement == null || !apiElement.getLocalName().equals("api")) {
//            return null;
//        }
//        OMAttribute nameAttr = apiElement.getAttribute(new javax.xml.namespace.QName("name"));
//        OMAttribute contextAttr = apiElement.getAttribute(new javax.xml.namespace.QName("context"));
//
//        if (nameAttr != null && contextAttr != null) {
//            String apiName = nameAttr.getAttributeValue();
//            String apiContext = contextAttr.getAttributeValue();
//
//            Position newPosition = new Position(1, position.getFileName(), apiName);
//
//            //API newApi = unmarshalResource();
//
//            List<Resource> resourceList = new ArrayList<>();
//
//            for (Iterator it = apiElement.getChildrenWithName(new QName("resource")); it.hasNext(); ) {
//                OMElement resourceElement = (OMElement) it.next();
//                try {
//                    resourceList.add(unmarshalResource(resourceElement));
//                } catch (Exception e) {
//                    // Handle error appropriately
//                    e.printStackTrace();
//                }
//            }
//
//            API api = new API(apiContext, apiName, resourceList, newPosition);
//
//            return api;
//
//        }
//        return null;
//    }
//
//    public Resource unmarshalResource(OMElement resourceElement) {
//        OMAttribute methodsAttr = resourceElement.getAttribute(new javax.xml.namespace.QName("methods"));
//        OMAttribute uriTemplateAttr = resourceElement.getAttribute(new javax.xml.namespace.QName("uri-template"));
//
//        if (methodsAttr != null && uriTemplateAttr != null) {
//            Resource newResource = new Resource();
//            newResource.setMethods(methodsAttr.getAttributeValue());
//            newResource.setUriTemplate(uriTemplateAttr.getAttributeValue());
//            return newResource;
//        }
//
//        return null;
//    }
//}

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
