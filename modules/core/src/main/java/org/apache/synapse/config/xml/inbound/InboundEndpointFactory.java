/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/

package org.apache.synapse.config.xml.inbound;


import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMText;
import org.apache.axiom.om.impl.llom.OMTextImpl;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.config.xml.XMLConfigConstants;
import org.apache.synapse.inbound.InboundEndpoint;
import org.apache.synapse.inbound.InboundEndpointConstants;

import sun.util.logging.resources.logging;

import javax.xml.namespace.QName;
import java.util.Iterator;

public class InboundEndpointFactory {

    private static final Log log = LogFactory.getLog(InboundEndpointFactory.class);
    private static final QName ATT_NAME
            = new QName(InboundEndpointConstants.INBOUND_ENDPOINT_NAME);
    private static final QName ATT_PROTOCOL
            = new QName(InboundEndpointConstants.INBOUND_ENDPOINT_PROTOCOL);
    private static final QName ATT_ENDPOINT_CLASS
            = new QName(InboundEndpointConstants.INBOUND_ENDPOINT_CLASS);
    private static final QName ATT_ENDPOINT_SUSPEND
            = new QName(InboundEndpointConstants.INBOUND_ENDPOINT_SUSPEND);
    private static final QName ATT_SEQUENCE
            = new QName(InboundEndpointConstants.INBOUND_ENDPOINT_SEQUENCE);
    private static final QName ATT_ERROR_SEQUENCE
            = new QName(InboundEndpointConstants.INBOUND_ENDPOINT_ERROR_SEQUENCE);
    
    public static InboundEndpoint createInboundEndpoint(OMElement inboundEndpointElem, SynapseConfiguration config) {
        InboundEndpoint inboundEndpoint = new InboundEndpoint();
        if (inboundEndpointElem.getAttributeValue(ATT_NAME) != null) {
            inboundEndpoint.setName(inboundEndpointElem.getAttributeValue(ATT_NAME));
        } else {
            String msg = "Inbound Endpoint name cannot be null";
            log.error(msg);
            throw new SynapseException(msg);
        }
        if (inboundEndpointElem.getAttributeValue(ATT_PROTOCOL) != null) {
            inboundEndpoint.setProtocol(inboundEndpointElem.getAttributeValue(ATT_PROTOCOL));
        }
        if (inboundEndpointElem.getAttributeValue(ATT_ENDPOINT_CLASS) != null ) {
            inboundEndpoint.setClassImpl(inboundEndpointElem.getAttributeValue(ATT_ENDPOINT_CLASS));
        }
        if (inboundEndpointElem.getAttributeValue(ATT_ENDPOINT_SUSPEND) != null) {
            inboundEndpoint.setSuspend
                    (Boolean.parseBoolean(inboundEndpointElem.getAttributeValue(ATT_ENDPOINT_SUSPEND)));
        } else {
            inboundEndpoint.setSuspend(false);
        }
        if (inboundEndpointElem.getAttributeValue(ATT_SEQUENCE) != null) {
            inboundEndpoint.setInjectingSeq(inboundEndpointElem.getAttributeValue(ATT_SEQUENCE));
        }
        if (inboundEndpointElem.getAttributeValue(ATT_ERROR_SEQUENCE) != null) {
            inboundEndpoint.setOnErrorSeq(inboundEndpointElem.getAttributeValue(ATT_ERROR_SEQUENCE));
        }

        // Set parameters
        OMElement parametersElt = inboundEndpointElem.getFirstChildWithName(
                new QName(XMLConfigConstants.SYNAPSE_NAMESPACE,
                        InboundEndpointConstants.INBOUND_ENDPOINT_PARAMETERS));

        if (parametersElt != null) {
            Iterator parameters =
                    parametersElt.getChildrenWithName(
                            new QName(XMLConfigConstants.SYNAPSE_NAMESPACE,
                                      InboundEndpointConstants.INBOUND_ENDPOINT_PARAMETER));

            while (parameters.hasNext()) {
                OMElement parameter = (OMElement) parameters.next();
                String paramName = parameter.getAttributeValue(new QName(
                        InboundEndpointConstants.INBOUND_ENDPOINT_PARAMETER_NAME));
                String paramKey = parameter.getAttributeValue(new QName(
                        InboundEndpointConstants.INBOUND_ENDPOINT_PARAMETER_KEY));

                if (paramKey != null) {
                    Object obj = config.getEntry(paramKey);
                    if (obj == null) {
                        obj = config.getEntryDefinition(paramKey);
                        obj = config.getEntry(paramKey);
                    }
                    if (obj != null && obj instanceof OMTextImpl) {
                        OMText objText = (OMText) obj;
                        inboundEndpoint.addParameter(paramName, objText.getText(), paramKey);
                    } else {
                        String msg = "Error while deploying inbound endpoint "
                                + inboundEndpoint.getName() + ".Registry entry defined with key: "
                                + paramKey + " not found.";
                        log.error(msg);
                        throw new SynapseException(msg);
                    }
                } else if (parameter.getFirstElement() != null) {
                    inboundEndpoint.addParameter(paramName, parameter.getFirstElement().toString());
                } else {
                    inboundEndpoint.addParameter(paramName, parameter.getText());
                }
            }
        }
        inboundEndpoint.setFileName(inboundEndpointElem.getAttributeValue(new QName(InboundEndpointConstants.INBOUND_ENDPOINT_NAME))+".xml");
        return inboundEndpoint;
    }


}
