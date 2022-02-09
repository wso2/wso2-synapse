/*
 * Copyright (c) 2022. WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
package org.apache.synapse.transport.netty.util;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.impl.llom.soap11.SOAP11Factory;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.builder.ApplicationXMLBuilder;
import org.apache.axis2.builder.Builder;
import org.apache.axis2.builder.BuilderUtil;
import org.apache.axis2.builder.MIMEBuilder;
import org.apache.axis2.builder.MTOMBuilder;
import org.apache.axis2.builder.SOAPBuilder;
import org.apache.axis2.builder.XFormURLEncodedBuilder;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.transport.MessageFormatter;
import org.apache.axis2.transport.TransportUtils;
import org.apache.axis2.transport.http.ApplicationXMLFormatter;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.axis2.transport.http.MultipartFormDataFormatter;
import org.apache.axis2.transport.http.SOAPMessageFormatter;
import org.apache.axis2.transport.http.XFormURLEncodedFormatter;
import org.apache.axis2.util.MessageProcessorSelector;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.transport.netty.BridgeConstants;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import javax.xml.stream.XMLStreamException;

/**
 * Class DeferredMessageBuilder contains the tools required to build the payload.
 */
public class DeferredMessageBuilder {

    private static final Log LOG = LogFactory.getLog(DeferredMessageBuilder.class);

    public static final String RELAY_FORMATTERS_MAP = "__RELAY_FORMATTERS_MAP";

    private Map<String, Builder> builders = new HashMap<String, Builder>();
    private Map<String, MessageFormatter> formatters = new HashMap<String, MessageFormatter>();

    public DeferredMessageBuilder() {
        // first initialize with the default builders
        builders.put("multipart/related", new MIMEBuilder());
        builders.put("application/soap+xml", new SOAPBuilder());
        builders.put("text/xml", new SOAPBuilder());
        builders.put("application/xop+xml", new MTOMBuilder());
        builders.put("application/xml", new ApplicationXMLBuilder());
        builders.put("application/x-www-form-urlencoded",
                new XFormURLEncodedBuilder());

        // initialize the default formatters
        formatters.put("application/x-www-form-urlencoded", new XFormURLEncodedFormatter());
        formatters.put("multipart/form-data", new MultipartFormDataFormatter());
        formatters.put("application/xml", new ApplicationXMLFormatter());
        formatters.put("text/xml", new SOAPMessageFormatter());
        formatters.put("application/soap+xml", new SOAPMessageFormatter());
    }

    public Map<String, Builder> getBuilders() {

        return builders;
    }

    public void addBuilder(String contentType, Builder builder) {

        builders.put(contentType, builder);
    }

    public void addFormatter(String contentType, MessageFormatter messageFormatter) {

        formatters.put(contentType, messageFormatter);
    }

    public Map<String, MessageFormatter> getFormatters() {

        return formatters;
    }

    public OMElement getDocument(MessageContext msgCtx, InputStream in) throws
            XMLStreamException, IOException {

        // HTTP Delete requests may contain entity body or not. Hence if the request is a HTTP DELETE, we have to verify
        // that the payload stream is empty or not.
        if (HTTPConstants.HEADER_DELETE.equals(msgCtx.getProperty(Constants.Configuration.HTTP_METHOD)) &&
                MessageUtils.isEmptyPayloadStream(in)) {
            msgCtx.setProperty(BridgeConstants.NO_ENTITY_BODY, Boolean.TRUE);
            return TransportUtils.createSOAPEnvelope(null);
        }

        String contentType = (String) msgCtx.getProperty(Constants.Configuration.CONTENT_TYPE);
        String contentType1 = getContentType(contentType, msgCtx);

        Map transportHeaders = (Map) msgCtx.getProperty(MessageContext.TRANSPORT_HEADERS);

        String contentLength = null;
        String transferEncoded;
        if (transportHeaders != null) {
            contentLength = (String) transportHeaders.get(BridgeConstants.CONTENT_LEN);
            transferEncoded = (String) transportHeaders.get(BridgeConstants.TRANSFER_ENCODING);

            if (contentType.equals(BridgeConstants.CONTENT_TYPE_APPLICATION_OCTET_STREAM)
                    && (contentLength == null || Integer.parseInt(contentLength) == 0)
                    && transferEncoded == null) {
                msgCtx.setProperty(BridgeConstants.NO_ENTITY_BODY, true);
                msgCtx.setProperty(Constants.Configuration.CONTENT_TYPE, "");
                return new SOAP11Factory().getDefaultEnvelope();
            }
        }

        OMElement element = null;
        Builder builder;
        if (contentType != null) {
            // loading builder from externally..
            // builder = configuration.getMessageBuilder(_contentType,useFallbackBuilder);
            builder = MessageProcessorSelector.getMessageBuilder(contentType1, msgCtx);
            if (builder != null) {
                try {
                    if ("0".equals(contentLength)) {
                        element = new SOAP11Factory().getDefaultEnvelope();
                        //since we are setting an empty envelop to achieve the empty body, we have to set a different
                        //content-type other than text/xml, application/soap+xml or any other content-type which will
                        //invoke the soap builder, otherwise soap builder will get hit and an empty envelope
                        // will be send out
                        msgCtx.setProperty(Constants.Configuration.MESSAGE_TYPE, "application/xml");
                    } else {
                        element = builder.processDocument(in, contentType, msgCtx);
                    }
                } catch (AxisFault axisFault) {
                    LOG.error("Error building message", axisFault);
                    throw axisFault;
                }
            }
        }

        if (element == null) {
            if (msgCtx.isDoingREST()) {
                try {
                    element = BuilderUtil.getPOXBuilder(in, null).getDocumentElement();
                } catch (XMLStreamException e) {
                    LOG.error("Error building message using POX Builder", e);
                    throw e;
                }
            } else {
                // switch to default
                builder = new SOAPBuilder();
                try {
                    if ("0".equals(contentLength)) {
                        element = new SOAP11Factory().getDefaultEnvelope();
                        //since we are setting an empty envelop to achieve the empty body, we have to set a different
                        //content-type other than text/xml, application/soap+xml or any other content-type which will
                        //invoke the soap builder, otherwise soap builder will get hit and an empty envelope
                        // will be send out
                        msgCtx.setProperty(Constants.Configuration.MESSAGE_TYPE, "application/xml");
                    } else {
                        element = builder.processDocument(in, contentType, msgCtx);
                    }
                } catch (AxisFault axisFault) {
                    LOG.error("Error building message using SOAP builder");
                    throw axisFault;
                }
            }
        }

        // build the soap headers and body
        if (element instanceof SOAPEnvelope) {
            SOAPEnvelope env = (SOAPEnvelope) element;
            env.hasFault();
        }

        // setting up original contentType (resetting the content type)
        if (contentType != null && !contentType.isEmpty()) {
            msgCtx.setProperty(Constants.Configuration.CONTENT_TYPE, contentType);
        }
        return element;
    }

    /**
     * This method is from org.apache.axis2.transport.TransportUtils - it was a hack placed in Axis2 Transport to enable
     * responses with text/xml to be processed using the ApplicationXMLBuilder (which is technically wrong, it should be
     * the duty of the backend service to send the correct content type, which makes the most sense (refer RFC 1049),
     * alas, tis not the way of the World).
     *
     * @param contentType content type
     * @param msgContext  message context
     * @return MIME content type.
     */
    public static String getContentType(String contentType, MessageContext msgContext) {

        String type;
        int index = contentType.indexOf(';');
        if (index > 0) {
            type = contentType.substring(0, index);
        } else {
            int commaIndex = contentType.indexOf(',');
            if (commaIndex > 0) {
                type = contentType.substring(0, commaIndex);
            } else {
                type = contentType;
            }
        }
        // Some services send REST responses as text/xml. We should convert it to
        // application/xml if its a REST response, if not it will try to use the SOAPMessageBuilder.
        // isDoingREST should already be properly set by HTTPTransportUtils.initializeMessageContext
        if (null != msgContext.getProperty(BridgeConstants.INVOKED_REST)
                && msgContext.getProperty(BridgeConstants.INVOKED_REST).equals(true)
                && HTTPConstants.MEDIA_TYPE_TEXT_XML.equals(type)) {
            type = HTTPConstants.MEDIA_TYPE_APPLICATION_XML;
        }
        return type;
    }
}
