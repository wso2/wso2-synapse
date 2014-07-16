/*
 * Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.apache.synapse.config.xml.inbound;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.config.xml.XMLConfigConstants;
import org.apache.synapse.inbound.InboundEndpoint;
import org.apache.synapse.inbound.InboundEndpointConstants;

import java.util.Map;

public class InboundEndpointSerializer {

	private static final OMFactory fac = OMAbstractFactory.getOMFactory();

	protected static final OMNamespace nullNS = fac.createOMNamespace(XMLConfigConstants.NULL_NAMESPACE,
	                                                                  "");

	public static OMElement serializeInboundEndpoint(InboundEndpoint inboundEndpoint) {

		OMElement inboundEndpointElt = fac.createOMElement(InboundEndpointConstants.INBOUND_ENDPOINT,
		                                                   SynapseConstants.SYNAPSE_OMNAMESPACE);
		inboundEndpointElt.addAttribute(InboundEndpointConstants.INBOUND_ENDPOINT_NAME,
		                                inboundEndpoint.getName(), null);
		inboundEndpointElt.addAttribute(InboundEndpointConstants.INBOUND_ENDPOINT_INTERVAL,
		                                Long.toString(inboundEndpoint.getInterval()), null);
		inboundEndpointElt.addAttribute(InboundEndpointConstants.INBOUND_ENDPOINT_SEQUENCE,
		                                inboundEndpoint.getInjectingSeq(), null);
		inboundEndpointElt.addAttribute(InboundEndpointConstants.INBOUND_ENDPOINT_ERROR_SEQUENCE,
		                                inboundEndpoint.getOnErrorSeq(), null);
		if(inboundEndpoint.getProtocol() != null){
			inboundEndpointElt.addAttribute(InboundEndpointConstants.INBOUND_ENDPOINT_PROTOCOL,
                    inboundEndpoint.getProtocol(), null);			
		}else{
			inboundEndpointElt.addAttribute(InboundEndpointConstants.INBOUND_ENDPOINT_CLASS,
	                inboundEndpoint.getClassImpl(), null);
		}
		
		inboundEndpointElt.addAttribute(InboundEndpointConstants.INBOUND_ENDPOINT_SUSPEND,
		                                Boolean.toString(inboundEndpoint.isSuspend()), null);

		OMElement parametersElt = fac.createOMElement(InboundEndpointConstants.INBOUND_ENDPOINT_PARAMETERS,
		                                              SynapseConstants.SYNAPSE_OMNAMESPACE);

		for (Map.Entry<String, String> paramEntry : inboundEndpoint.getParametersMap().entrySet()) {
			OMElement parameter = fac.createOMElement(InboundEndpointConstants.INBOUND_ENDPOINT_PARAMETER,
			                                          SynapseConstants.SYNAPSE_OMNAMESPACE);
			parameter.addAttribute(InboundEndpointConstants.INBOUND_ENDPOINT_PARAMETER_NAME,
			                       paramEntry.getKey(), null);
			parameter.setText(paramEntry.getValue());
			parametersElt.addChild(parameter);
		}

		inboundEndpointElt.addChild(parametersElt);

		return inboundEndpointElt;
	}

	public static OMElement serializeInboundEndpoint(OMElement parent,
	                                                 InboundEndpoint inboundEndpoint) {
		OMElement inboundEndpointElt = serializeInboundEndpoint(inboundEndpoint);
		if (parent != null) {
			parent.addChild(inboundEndpointElt);
		}
		return inboundEndpointElt;
	}
}
