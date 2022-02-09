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

import com.google.gson.JsonParser;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMOutputFormat;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.AddressingConstants;
import org.apache.axis2.addressing.AddressingHelper;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.WSDL2Constants;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.engine.Handler;
import org.apache.axis2.engine.Phase;
import org.apache.axis2.transport.MessageFormatter;
import org.apache.axis2.transport.RequestResponseTransport;
import org.apache.axis2.transport.TransportUtils;
import org.apache.axis2.transport.http.ApplicationXMLFormatter;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.axis2.transport.http.SOAPMessageFormatter;
import org.apache.axis2.transport.http.XFormURLEncodedFormatter;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.transport.netty.BridgeConstants;
import org.apache.synapse.transport.netty.config.NettyConfiguration;
import org.wso2.transport.http.netty.message.HttpCarbonMessage;
import org.wso2.transport.http.netty.message.HttpMessageDataStreamer;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import javax.xml.stream.XMLStreamException;

/**
 * Class MessageUtils contains helper methods that are used to build the payload.
 */
public class MessageUtils {
    private static final Log LOG = LogFactory.getLog(MessageUtils.class);
    private static final DeferredMessageBuilder messageBuilder = new DeferredMessageBuilder();

    private static boolean noAddressingHandler = false;

    private static volatile Handler addressingInHandler = null;

    private static final Boolean forceMessageBuild;

    private static final boolean forceXmlValidation;

    private static final boolean forceJSONValidation;

    static {
        forceMessageBuild = NettyConfiguration.getInstance().isForcedMessageBuildEnabled();
        forceXmlValidation = NettyConfiguration.getInstance().isForcedXmlMessageValidationEnabled();
        forceJSONValidation = NettyConfiguration.getInstance().isForcedJSONMessageValidationEnabled();
    }

    public static void buildMessage(MessageContext msgCtx) throws IOException {

        buildMessage(msgCtx, false);
    }

    public static void buildMessage(MessageContext msgCtx, boolean earlyBuild) throws IOException {

        if (Boolean.TRUE.equals(msgCtx.getProperty(BridgeConstants.MESSAGE_BUILDER_INVOKED))) {
            return;
        }

        if (msgCtx.getProperty(Constants.Configuration.CONTENT_TYPE) == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Content Type is null and the message is not build");
            }
            msgCtx.setProperty(BridgeConstants.MESSAGE_BUILDER_INVOKED,
                    Boolean.TRUE);
            return;
        }

        if (!RequestResponseUtils.isHttpCarbonMessagePresent(msgCtx) || !forceMessageBuild) {
            return;
        }

        HttpCarbonMessage httpCarbonMessage =
                (HttpCarbonMessage) msgCtx.getProperty(BridgeConstants.HTTP_CARBON_MESSAGE);

        HttpMessageDataStreamer httpMessageDataStreamer = new HttpMessageDataStreamer(httpCarbonMessage);

        if (!HttpUtils.requestHasEntityBody(httpCarbonMessage)) {
            return;
        }

        InputStream in = httpMessageDataStreamer.getInputStream();

        ByteArrayOutputStream byteArrayOutputStream = null;
        if (forceXmlValidation || forceJSONValidation) {
            //read input stream to store raw data and create inputStream again.
            //then the raw data can be logged after an error while building the message.
            byteArrayOutputStream = new ByteArrayOutputStream();
            IOUtils.copy(in, byteArrayOutputStream);
            byteArrayOutputStream.flush();
            // Open new InputStreams using the recorded bytes and assign to in
            in =  new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        }

        BufferedInputStream bufferedInputStream = (BufferedInputStream) msgCtx
                .getProperty(BridgeConstants.BUFFERED_INPUT_STREAM);
        if (bufferedInputStream != null) {
            try {
                bufferedInputStream.reset();
                bufferedInputStream.mark(0);
            } catch (Exception e) {
                // just ignore the error
            }

        } else {
            bufferedInputStream = new BufferedInputStream(in);
            // Need to handle properly for the moment lets use around 100k
            // buffer.
            bufferedInputStream.mark(128 * 1024);
            msgCtx.setProperty(BridgeConstants.BUFFERED_INPUT_STREAM,
                    bufferedInputStream);
        }

        OMElement element;
        try {
            element = messageBuilder.getDocument(msgCtx, bufferedInputStream);
            if (element != null) {
                msgCtx.setEnvelope(TransportUtils.createSOAPEnvelope(element));
                msgCtx.setProperty(DeferredMessageBuilder.RELAY_FORMATTERS_MAP,
                        messageBuilder.getFormatters());
                msgCtx.setProperty(BridgeConstants.MESSAGE_BUILDER_INVOKED, Boolean.TRUE);

                earlyBuild = msgCtx.getProperty(BridgeConstants.RELAY_EARLY_BUILD) != null ? (Boolean) msgCtx
                        .getProperty(BridgeConstants.RELAY_EARLY_BUILD) : earlyBuild;

                if (!earlyBuild) {
                    processAddressing(msgCtx);
                }

                //force validation makes sure that the xml is well formed (not having multi root element), and the json
                // message is valid (not having any content after the final enclosing bracket)
                if (forceXmlValidation || forceJSONValidation) {
                    String rawData = null;
                    try {
                        String contentType = (String) msgCtx.getProperty(Constants.Configuration.CONTENT_TYPE);

                        if (BridgeConstants.JSON_CONTENT_TYPE.equals(getMIMEContentType(contentType))
                                && forceJSONValidation) {
                            rawData = byteArrayOutputStream.toString();
                            JsonParser jsonParser = new JsonParser();
                            jsonParser.parse(rawData);
                        } else {
                            msgCtx.getEnvelope().buildWithAttachments();
                            if (msgCtx.getEnvelope().getBody().getFirstElement() != null) {
                                msgCtx.getEnvelope().getBody().getFirstElement().buildNext();
                            }
                        }

                    } catch (Exception e) {
                        if (rawData == null) {
                            rawData = byteArrayOutputStream.toString();
                        }
                        LOG.error("Error while building the message.\n" + rawData);
                        msgCtx.setProperty(BridgeConstants.RAW_PAYLOAD, rawData);
                        throw e;
                    }
                }
            }
        } catch (IOException | XMLStreamException e) {
            msgCtx.setProperty(BridgeConstants.MESSAGE_BUILDER_INVOKED, Boolean.TRUE);
        }
    }

    /**
     * Get MIME content type out of content-type header.
     * @param contentType content type header value
     * @return MIME content type
     */
    public static String getMIMEContentType(String contentType) {
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
        return type;
    }

    /**
     * Function to check given inputstream is empty or not
     * Used to check whether content of the payload input stream is empty or not.
     *
     * @param inputStream target inputstream
     * @return true if it is a empty stream
     * @throws IOException
     */
    public static boolean isEmptyPayloadStream(InputStream inputStream) throws IOException {

        boolean isEmptyPayload = true;

        if (inputStream != null) {
            // read ahead few characters to see if the stream is valid.
            // Checks for all empty or all whitespace streams and if found  sets isEmptyPayload to false. The while
            // loop exits if found any character other than space or end of stream reached.
            int c = inputStream.read();
            while (c != -1) {
                if (c != 32) {
                    //if not a space, should be some character in entity body
                    isEmptyPayload = false;
                    break;
                }
                c = inputStream.read();
            }
            inputStream.reset();
        }

        return isEmptyPayload;
    }

    /**
     * This selects the formatter for a given message format based on the the content type of the received message.
     * content-type to builder mapping can be specified through the Axis2.xml.
     *
     * @param msgContext axis2 MessageContext
     * @return the formatter registered against the given content-type
     */
    public static MessageFormatter getMessageFormatter(MessageContext msgContext) {

        MessageFormatter messageFormatter = null;
        String messageFormatString = getMessageFormatterProperty(msgContext);
        messageFormatString = getContentTypeForFormatterSelection(messageFormatString, msgContext);
        if (messageFormatString != null) {
            messageFormatter = msgContext.getConfigurationContext()
                    .getAxisConfiguration().getMessageFormatter(messageFormatString);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Message format is: " + messageFormatString
                             + "; message formatter returned by AxisConfiguration: " + messageFormatter);
            }
        }
        if (messageFormatter == null) {
            messageFormatter = (MessageFormatter) msgContext.getProperty(Constants.Configuration.MESSAGE_FORMATTER);
            if (messageFormatter != null) {
                return messageFormatter;
            }
        }
        if (messageFormatter == null) {

            // If we are doing rest better default to Application/xml formatter
            if (msgContext.isDoingREST()) {
                String httpMethod = (String) msgContext.getProperty(Constants.Configuration.HTTP_METHOD);
                if (Constants.Configuration.HTTP_METHOD_GET.equals(httpMethod) ||
                    Constants.Configuration.HTTP_METHOD_DELETE.equals(httpMethod)) {
                    return new XFormURLEncodedFormatter();
                }
                return new ApplicationXMLFormatter();
            } else {
                // Lets default to SOAP formatter
                messageFormatter = new SOAPMessageFormatter();
            }
        }
        return messageFormatter;
    }

    private static String getMessageFormatterProperty(MessageContext msgContext) {
        String messageFormatterProperty = null;
        Object property = msgContext
                .getProperty(Constants.Configuration.MESSAGE_TYPE);
        if (property != null) {
            messageFormatterProperty = (String) property;
        }
        if (messageFormatterProperty == null) {
            Parameter parameter = msgContext
                    .getParameter(Constants.Configuration.MESSAGE_TYPE);
            if (parameter != null) {
                messageFormatterProperty = (String) parameter.getValue();
            }
        }
        return messageFormatterProperty;
    }

    private static String getContentTypeForFormatterSelection(String type, MessageContext msgContext) {
        /*
         * Handle special case where content-type : text/xml and SOAPAction = null consider as
         * POX (REST) message not SOAP 1.1.
         *
         * 1.) it's required use the Builder associate with "application/xml" here but should not
         * change content type of current message.
         */
        String cType = type;
        if (msgContext.isDoingREST() && HTTPConstants.MEDIA_TYPE_TEXT_XML.equals(type)) {
            cType = HTTPConstants.MEDIA_TYPE_APPLICATION_XML;
            msgContext.setProperty(Constants.Configuration.CONTENT_TYPE, HTTPConstants.MEDIA_TYPE_TEXT_XML);
        }
        return cType;
    }

    public static OMOutputFormat getOMOutputFormat(MessageContext msgContext) {

        OMOutputFormat format;
        if (msgContext.getProperty(BridgeConstants.MESSAGE_OUTPUT_FORMAT) != null) {
            format = (OMOutputFormat) msgContext.getProperty(BridgeConstants.MESSAGE_OUTPUT_FORMAT);
        } else {
            format = new OMOutputFormat();
        }

        msgContext.setDoingMTOM(TransportUtils.doWriteMTOM(msgContext));
        msgContext.setDoingSwA(TransportUtils.doWriteSwA(msgContext));
        msgContext.setDoingREST(TransportUtils.isDoingREST(msgContext));

        /*
         *  BridgeConstants.INVOKED_REST set to true here if isDoingREST is true -
         *  this enables us to check whether the original request to the endpoint was a
         * REST request inside DeferredMessageBuilder (which we need to convert
         * text/xml content type into application/xml if the request was not a SOAP
         * request.
         */
        if (msgContext.isDoingREST()) {
            msgContext.setProperty(BridgeConstants.INVOKED_REST, true);
        }
        format.setSOAP11(msgContext.isSOAP11());
        format.setDoOptimize(msgContext.isDoingMTOM());
        format.setDoingSWA(msgContext.isDoingSwA());

        format.setCharSetEncoding(TransportUtils.getCharSetEncoding(msgContext));
        Object mimeBoundaryProperty = msgContext.getProperty(Constants.Configuration.MIME_BOUNDARY);
        if (mimeBoundaryProperty != null) {
            format.setMimeBoundary((String) mimeBoundaryProperty);
        }

        return format;
    }

    private static void processAddressing(MessageContext messageContext) throws AxisFault {
        if (noAddressingHandler) {
            return;
        } else if (addressingInHandler == null) {
            synchronized (messageBuilder) {
                if (addressingInHandler == null) {
                    AxisConfiguration axisConfig = messageContext.getConfigurationContext()
                            .getAxisConfiguration();
                    List<Phase> phases = axisConfig.getInFlowPhases();
                    boolean handlerFound = false;
                    for (Phase phase : phases) {
                        if ("Addressing".equals(phase.getName())) {
                            List<Handler> handlers = phase.getHandlers();
                            for (Handler handler : handlers) {
                                if ("AddressingInHandler".equals(handler.getName())) {
                                    addressingInHandler = handler;
                                    handlerFound = true;
                                    break;
                                }
                            }
                            break;
                        }
                    }

                    if (!handlerFound) {
                        noAddressingHandler = true;
                        return;
                    }
                }
            }
        }

        if (messageContext.getProperty(AddressingConstants.DISABLE_ADDRESSING_FOR_IN_MESSAGES) == null) {
            messageContext.setProperty(AddressingConstants.DISABLE_ADDRESSING_FOR_IN_MESSAGES, "false");
        }
        Object disableAddressingForOutGoing = null;
        if (messageContext.getProperty(AddressingConstants.DISABLE_ADDRESSING_FOR_OUT_MESSAGES) != null) {
            disableAddressingForOutGoing = messageContext
                    .getProperty(AddressingConstants.DISABLE_ADDRESSING_FOR_OUT_MESSAGES);
        }
        addressingInHandler.invoke(messageContext);

        if (disableAddressingForOutGoing != null) {
            messageContext.setProperty(AddressingConstants.DISABLE_ADDRESSING_FOR_OUT_MESSAGES,
                    disableAddressingForOutGoing);
        }

        if (messageContext.getAxisOperation() == null) {
            return;
        }

        String mepString = messageContext.getAxisOperation().getMessageExchangePattern();

        if (isOneWay(mepString)) {
            Object requestResponseTransport = messageContext
                    .getProperty(RequestResponseTransport.TRANSPORT_CONTROL);
            if (requestResponseTransport != null) {

                Boolean disableAck = getDisableAck(messageContext);
                if (disableAck == null || !disableAck) {
                    ((RequestResponseTransport) requestResponseTransport)
                            .acknowledgeMessage(messageContext);
                }
            }
        } else if (AddressingHelper.isReplyRedirected(messageContext)
                && AddressingHelper.isFaultRedirected(messageContext)) {
            if (mepString.equals(WSDL2Constants.MEP_URI_IN_OUT)) {
                // OR, if 2 way operation but the response is intended to not
                // use the response channel of a 2-way transport
                // then we don't need to keep the transport waiting.

                Object requestResponseTransport = messageContext
                        .getProperty(RequestResponseTransport.TRANSPORT_CONTROL);
                if (requestResponseTransport != null) {

                    // We should send an early ack to the transport whenever
                    // possible, but some modules need
                    // to use the back channel, so we need to check if they have
                    // disabled this code.
                    Boolean disableAck = getDisableAck(messageContext);

                    if (disableAck == null || !disableAck) {
                        ((RequestResponseTransport) requestResponseTransport)
                                .acknowledgeMessage(messageContext);
                    }

                }
            }
        }
    }

    private static Boolean getDisableAck(MessageContext msgContext) {
        // We should send an early ack to the transport whenever possible, but
        // some modules need
        // to use the back channel, so we need to check if they have disabled
        // this code.
        Boolean disableAck = (Boolean) msgContext
                .getProperty(Constants.Configuration.DISABLE_RESPONSE_ACK);
        if (disableAck == null) {
            disableAck = (Boolean) (msgContext.getAxisService() != null ? msgContext
                    .getAxisService().getParameterValue(
                            Constants.Configuration.DISABLE_RESPONSE_ACK) : null);
        }

        return disableAck;
    }

    private static boolean isOneWay(String mepString) {
        return mepString.equals(WSDL2Constants.MEP_URI_IN_ONLY);
    }
}
