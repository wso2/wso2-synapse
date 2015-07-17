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
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.config.xml.XMLConfigConstants;
import org.apache.synapse.inbound.InboundEndpoint;
import org.apache.synapse.inbound.InboundEndpointConstants;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;

public class InboundEndpointSerializer {

	private static final OMFactory fac = OMAbstractFactory.getOMFactory();

	protected static final OMNamespace nullNS =
            fac.createOMNamespace(XMLConfigConstants.NULL_NAMESPACE, "");

	public static OMElement serializeInboundEndpoint(InboundEndpoint inboundEndpoint) {

		OMElement inboundEndpointElt = fac.createOMElement(InboundEndpointConstants.INBOUND_ENDPOINT,
		                                                   SynapseConstants.SYNAPSE_OMNAMESPACE);
		inboundEndpointElt.addAttribute(InboundEndpointConstants.INBOUND_ENDPOINT_NAME,
		                                inboundEndpoint.getName(), null);
		if (inboundEndpoint.getInjectingSeq() != null) {
			inboundEndpointElt.addAttribute(InboundEndpointConstants.INBOUND_ENDPOINT_SEQUENCE,
											inboundEndpoint.getInjectingSeq(), null);
		}
		if (inboundEndpoint.getOnErrorSeq() != null) {
			inboundEndpointElt.addAttribute(InboundEndpointConstants.INBOUND_ENDPOINT_ERROR_SEQUENCE,
											inboundEndpoint.getOnErrorSeq(), null);
		}

		if (inboundEndpoint.getProtocol() != null) {
			inboundEndpointElt.addAttribute(InboundEndpointConstants.INBOUND_ENDPOINT_PROTOCOL,
                    inboundEndpoint.getProtocol(), null);			
		} else {
			inboundEndpointElt.addAttribute(InboundEndpointConstants.INBOUND_ENDPOINT_CLASS,
	                inboundEndpoint.getClassImpl(), null);
		}
		
		inboundEndpointElt.addAttribute(InboundEndpointConstants.INBOUND_ENDPOINT_SUSPEND,
		                                Boolean.toString(inboundEndpoint.isSuspend()), null);

		OMElement parametersElt = fac.createOMElement(InboundEndpointConstants.INBOUND_ENDPOINT_PARAMETERS,
		                                              SynapseConstants.SYNAPSE_OMNAMESPACE);

		for (Map.Entry<String, String> paramEntry : inboundEndpoint.getParametersMap().entrySet()) {
			String strKey = paramEntry.getKey();
			OMElement parameter = fac.createOMElement(InboundEndpointConstants.INBOUND_ENDPOINT_PARAMETER,
			                                          SynapseConstants.SYNAPSE_OMNAMESPACE);
			parameter.addAttribute(InboundEndpointConstants.INBOUND_ENDPOINT_PARAMETER_NAME,
			                       strKey, null);

		if(inboundEndpoint.getParameterKey(strKey) != null){
         parameter.addAttribute(InboundEndpointConstants.INBOUND_ENDPOINT_PARAMETER_KEY,
                                inboundEndpoint.getParameterKey(strKey), null);
		}else if (isWellFormedXML(paramEntry.getValue())) {
				try {
					OMElement omElement = AXIOMUtil.stringToOM(paramEntry.getValue());
					parameter.addChild(omElement);
				} catch (XMLStreamException e) {
					String msg = "Error Parsing OMElement for value of " + paramEntry.getKey();
					throw new SynapseException(msg, e);
				}
			}else {
				parameter.setText(paramEntry.getValue());
			}
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

	private static  boolean isWellFormedXML(String value) {
		try {
			 XMLReader parser = XMLReaderFactory.createXMLReader();
			 parser.setErrorHandler(null);
			 InputSource source = new InputSource(new ByteArrayInputStream(value.getBytes()));
			 parser.parse(source);
			 } catch (SAXException e) {
			 return false;
			 } catch (IOException e) {
			return false;
			}
		 return true;
		 }


}
