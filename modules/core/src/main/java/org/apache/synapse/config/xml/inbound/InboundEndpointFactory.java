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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.config.xml.XMLConfigConstants;
import org.apache.synapse.inbound.InboundEndpoint;
import org.apache.synapse.inbound.InboundEndpointConstants;

import javax.xml.namespace.QName;
import java.util.Iterator;

public class InboundEndpointFactory {

    private static final Log log = LogFactory.getLog(InboundEndpointFactory.class);


    public static InboundEndpoint createInboundEndpoint(OMElement inboundEndpointElem) {
        InboundEndpoint inboundEndpoint = new InboundEndpoint();
        if (inboundEndpointElem.getAttributeValue(new QName(InboundEndpointConstants.INBOUND_ENDPOINT_NAME)) != null) {
            inboundEndpoint.setName(inboundEndpointElem.getAttributeValue(
                    new QName(InboundEndpointConstants.INBOUND_ENDPOINT_NAME)));
        } else {
            log.error("Inbound Endpoint name cannot be null");
        }
        inboundEndpoint.setProtocol(inboundEndpointElem.getAttributeValue(
                new QName(InboundEndpointConstants.INBOUND_ENDPOINT_PROTOCOL)) != null ? inboundEndpointElem.getAttributeValue
                (new QName(InboundEndpointConstants.INBOUND_ENDPOINT_PROTOCOL)) : null);
        inboundEndpoint.setClassImpl(inboundEndpointElem.getAttributeValue(
                new QName(InboundEndpointConstants.INBOUND_ENDPOINT_CLASS)) != null ?
                inboundEndpointElem.getAttributeValue(
                        new QName(InboundEndpointConstants.INBOUND_ENDPOINT_CLASS)) : null);
        if (inboundEndpointElem.getAttributeValue(new QName
                (InboundEndpointConstants.INBOUND_ENDPOINT_SUSPEND)) != null) {
            inboundEndpoint.setSuspend(Boolean.parseBoolean(inboundEndpointElem.getAttributeValue(
                    new QName(InboundEndpointConstants.INBOUND_ENDPOINT_SUSPEND))));
        } else {
            inboundEndpoint.setSuspend(false);
        }
        if (inboundEndpointElem.getAttributeValue(new QName
                (InboundEndpointConstants.INBOUND_ENDPOINT_SEQUENCE)) != null) {
            inboundEndpoint.setInjectingSeq(inboundEndpointElem.getAttributeValue
                    (new QName(InboundEndpointConstants.INBOUND_ENDPOINT_SEQUENCE)));
        } else {
            log.error("Injecting sequence cannot be null");
        }
        if (inboundEndpointElem.getAttributeValue(new QName
                (InboundEndpointConstants.INBOUND_ENDPOINT_ERROR_SEQUENCE)) != null) {
            inboundEndpoint.setOnErrorSeq(inboundEndpointElem.getAttributeValue
                    (new QName(InboundEndpointConstants.INBOUND_ENDPOINT_ERROR_SEQUENCE)));
        } else {
            log.error("On Error sequence cannot be null");
        }
        OMElement parametersElt = inboundEndpointElem.getFirstChildWithName
                (new QName(XMLConfigConstants.SYNAPSE_NAMESPACE,
                        InboundEndpointConstants.INBOUND_ENDPOINT_PARAMETERS));

        Iterator parameters = parametersElt.getChildrenWithName(new QName(
                XMLConfigConstants.SYNAPSE_NAMESPACE, InboundEndpointConstants.INBOUND_ENDPOINT_PARAMETER));

        while (parameters.hasNext()) {
            OMElement parameter = (OMElement) parameters.next();
            String paramName = parameter.getAttributeValue
                    (new QName(InboundEndpointConstants.INBOUND_ENDPOINT_PARAMETER_NAME));
            inboundEndpoint.addParameter(paramName, parameter.getText());
        }

        return inboundEndpoint;
    }


}
