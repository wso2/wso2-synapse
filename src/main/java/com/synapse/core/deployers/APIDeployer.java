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
import com.synapse.core.artifacts.api.CORSConfig;
import com.synapse.core.artifacts.api.Resource;
import com.synapse.core.artifacts.Sequence;
import com.synapse.core.artifacts.Mediator;
import com.synapse.core.artifacts.utils.Position;
import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class APIDeployer {

    private static final Logger log = LogManager.getLogger(APIDeployer.class);

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
                log.debug("Warning: No <resource> elements found in API: {}", apiName);
                return null; // Prevent returning an API without resources
            }

            API newApi = new API(apiContext, apiName, resourceList, newPosition);

            CORSConfig corsConfig = unmarshalCORSConfig(apiElement);
            
            if (corsConfig != null) {
                newApi.setCorsConfig(corsConfig);
            }
            
            return newApi;
            
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
            log.debug("Warning: No mediators found in sequence: {}", sequenceElement.getLocalName());
        }

        return new Sequence(mediatorList, sequencePosition, "inline");
    }

    private CORSConfig unmarshalCORSConfig(OMElement apiElement) {
        
        OMElement corsElement = apiElement.getFirstChildWithName(new QName("cors"));
        if (corsElement == null) {
            return null;
        }

        CORSConfig corsConfig = new CORSConfig();
        
        OMAttribute enabledAttr = corsElement.getAttribute(new QName("enabled"));
        
        if (enabledAttr != null) {
            corsConfig.setEnabled(Boolean.parseBoolean(enabledAttr.getAttributeValue()));
        }
        
        OMAttribute allowOriginsAttr = corsElement.getAttribute(new QName("allow-origins"));
        
        if (allowOriginsAttr != null) {

            String origins = allowOriginsAttr.getAttributeValue();

            if (origins != null && !origins.trim().isEmpty()) {
                String[] originsArray = origins.split("\\s*,\\s*");
                corsConfig.setAllowOrigins(originsArray);
            }
        }
        
        OMAttribute allowMethodsAttr = corsElement.getAttribute(new QName("allow-methods"));
        
        if (allowMethodsAttr != null) {

            String methods = allowMethodsAttr.getAttributeValue();
            
            if (methods != null && !methods.trim().isEmpty()) {
                String[] methodsArray = methods.split("\\s*,\\s*");
                corsConfig.setAllowMethods(methodsArray);
            }
        }
        
        OMAttribute allowHeadersAttr = corsElement.getAttribute(new QName("allow-headers"));
        
        if (allowHeadersAttr != null) {

            String headers = allowHeadersAttr.getAttributeValue();
            
            if (headers != null && !headers.trim().isEmpty()) {
                String[] headersArray = headers.split("\\s*,\\s*");
                corsConfig.setAllowHeaders(headersArray);
            }
        }
        
        OMAttribute exposeHeadersAttr = corsElement.getAttribute(new QName("expose-headers"));
        
        if (exposeHeadersAttr != null) {

            String exposeHeaders = exposeHeadersAttr.getAttributeValue();
            if (exposeHeaders != null && !exposeHeaders.trim().isEmpty()) {
                String[] exposeHeadersArray = exposeHeaders.split("\\s*,\\s*");
                corsConfig.setExposeHeaders(exposeHeadersArray);
            }
        }
        
        OMAttribute allowCredentialsAttr = corsElement.getAttribute(new QName("allow-credentials"));
        
        if (allowCredentialsAttr != null) {
            corsConfig.setAllowCredentials(Boolean.parseBoolean(allowCredentialsAttr.getAttributeValue()));
        }
        
        OMAttribute maxAgeAttr = corsElement.getAttribute(new QName("max-age"));
        
        if (maxAgeAttr != null) {
            corsConfig.setMaxAge(Integer.parseInt(maxAgeAttr.getAttributeValue()));
        }
        
        return corsConfig;
    }

}
