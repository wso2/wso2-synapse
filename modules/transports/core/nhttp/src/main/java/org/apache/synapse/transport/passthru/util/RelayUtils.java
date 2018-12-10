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

package org.apache.synapse.transport.passthru.util;

import com.google.gson.JsonParser;
import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.AddressingConstants;
import org.apache.axis2.addressing.AddressingHelper;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.WSDL2Constants;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.engine.Handler;
import org.apache.axis2.engine.Phase;
import org.apache.axis2.transport.RequestResponseTransport;
import org.apache.axis2.transport.TransportUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.synapse.transport.nhttp.NhttpConstants;
import org.apache.synapse.transport.passthru.PassThroughConstants;
import org.apache.synapse.transport.passthru.Pipe;
import org.apache.synapse.transport.passthru.TargetRequest;
import org.apache.synapse.transport.passthru.config.PassThroughConfiguration;

import javax.xml.stream.XMLStreamException;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public class RelayUtils {

    private static final Log log = LogFactory.getLog(RelayUtils.class);

    private static final DeferredMessageBuilder messageBuilder = new DeferredMessageBuilder();

    private static volatile Handler addressingInHandler = null;
    private static boolean noAddressingHandler = false;

    private static Boolean forcePTBuild = null;

    private static boolean forceXmlValidation = false;

    private static boolean forceJSONValidation = false;

    static {
        if (forcePTBuild == null) {
            forcePTBuild = PassThroughConfiguration.getInstance().getBooleanProperty(
                    PassThroughConstants.FORCE_PASSTHROUGH_BUILDER);
            if (forcePTBuild == null) {
                forcePTBuild = true;
            }
            // this to keep track ignore the builder operation eventhough
            // content level is enable.
        }
        forceXmlValidation = PassThroughConfiguration.getInstance().isForcedXmlMessageValidationEnabled();
        forceJSONValidation = PassThroughConfiguration.getInstance().isForcedJSONMessageValidationEnabled();
    }

    public static void buildMessage(org.apache.axis2.context.MessageContext msgCtx)
            throws IOException, XMLStreamException {

        buildMessage(msgCtx, false);
    }

    public static void buildMessage(MessageContext messageContext, boolean earlyBuild)
            throws IOException, XMLStreamException {

        final Pipe pipe = (Pipe) messageContext.getProperty(PassThroughConstants.PASS_THROUGH_PIPE);

        if (messageContext.getProperty(Constants.Configuration.CONTENT_TYPE) != null) {
            if (log.isDebugEnabled()) {
                log.debug("Content Type is " + messageContext.getProperty(Constants.Configuration.CONTENT_TYPE));
            }

            if (pipe != null
                && !Boolean.TRUE.equals(messageContext
                                                .getProperty(PassThroughConstants.MESSAGE_BUILDER_INVOKED)) && forcePTBuild) {
                InputStream in = pipe.getInputStream();

                Object http_sc = messageContext.getProperty(NhttpConstants.HTTP_SC);
                if (http_sc != null && http_sc instanceof Integer && http_sc.equals(202)) {
                    if (in != null) {
                        InputStream bis = new ReadOnlyBIS(in);
                        int c = bis.read();
                        if (c == -1) {
                            messageContext.setProperty(PassThroughConstants.MESSAGE_BUILDER_INVOKED, Boolean.TRUE);
                            messageContext.setProperty(PassThroughConstants.NO_ENTITY_BODY, Boolean.TRUE);
                            return;
                        }
                        bis.reset();
                        in = bis;
                    }
                }

                builldMessage(messageContext, earlyBuild, in);
                return;
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Content Type is null and the message is not build");
            }
            messageContext.setProperty(PassThroughConstants.MESSAGE_BUILDER_INVOKED,
                    Boolean.TRUE);
            return;
        }
    }

    public static void builldMessage(MessageContext messageContext, boolean earlyBuild,
                                     InputStream in) throws IOException, AxisFault {
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
        //
        BufferedInputStream bufferedInputStream = (BufferedInputStream) messageContext
                .getProperty(PassThroughConstants.BUFFERED_INPUT_STREAM);
        if (bufferedInputStream != null) {
            try {
                bufferedInputStream.reset();
                bufferedInputStream.mark(0);
            } catch (IOException e) {
                handleException("Error while checking bufferedInputStream", e);
            }

        } else {
            bufferedInputStream = new BufferedInputStream(in);
            // TODO: need to handle properly for the moment lets use around 100k
            // buffer.
            bufferedInputStream.mark(128 * 1024);
            messageContext.setProperty(PassThroughConstants.BUFFERED_INPUT_STREAM,
                    bufferedInputStream);
        }

        OMElement element = null;
        try {
            element = messageBuilder.getDocument(messageContext,
                    bufferedInputStream != null ? bufferedInputStream : in);
            if (element != null) {
                messageContext.setEnvelope(TransportUtils.createSOAPEnvelope(element));
                messageContext.setProperty(DeferredMessageBuilder.RELAY_FORMATTERS_MAP,
                        messageBuilder.getFormatters());
                messageContext.setProperty(PassThroughConstants.MESSAGE_BUILDER_INVOKED, Boolean.TRUE);

                earlyBuild = messageContext.getProperty(PassThroughConstants.RELAY_EARLY_BUILD) != null ? (Boolean) messageContext
                        .getProperty(PassThroughConstants.RELAY_EARLY_BUILD) : earlyBuild;

                if (!earlyBuild) {
                    processAddressing(messageContext);
                }

                //force validation makes sure that the xml is well formed (not having multi root element), and the json
                // message is valid (not having any content after the final enclosing bracket)
                if (forceXmlValidation || forceJSONValidation) {
                    String rawData = null;
                    try {
                        String contentType = (String) messageContext.getProperty(Constants.Configuration.CONTENT_TYPE);

                        if (PassThroughConstants.JSON_CONTENT_TYPE.equals(contentType) && forceJSONValidation) {
                            rawData = byteArrayOutputStream.toString();
                            JsonParser jsonParser = new JsonParser();
                            jsonParser.parse(rawData);
                        }

                        messageContext.getEnvelope().buildWithAttachments();
                        if (messageContext.getEnvelope().getBody().getFirstElement() != null) {
                            messageContext.getEnvelope().getBody().getFirstElement().buildNext();
                        }
                    } catch (Exception e) {
                        if (rawData == null) {
                            rawData = byteArrayOutputStream.toString();
                        }
                        log.error("Error while building the message.\n" + rawData);
                        messageContext.setProperty(PassThroughConstants.RAW_PAYLOAD, rawData);
                        throw e;
                    }
                }
            }
        } catch (Exception e) {
            //Clearing the buffer when there is an exception occurred.
            consumeAndDiscardMessage(messageContext);
            messageContext.setProperty(PassThroughConstants.MESSAGE_BUILDER_INVOKED, Boolean.TRUE);
            handleException("Error while building Passthrough stream", e);
        }
        return;
    }


    /**
     * Function to check whether the processing request (enclosed within MessageContext) is a DELETE request without
     * entity body since we allow to have payload for DELETE requests, we treat same as POST. Hence this function can be
     * used to deviate DELETE requests without payloads
     * @param msgContext MessageContext
     * @return whether the request is a DELETE without payload
     */
    public static boolean isDeleteRequestWithoutPayload (MessageContext msgContext) throws AxisFault {
        if (PassThroughConstants.HTTP_DELETE.equals(msgContext.getProperty(Constants.Configuration.HTTP_METHOD))) {

            //If message builder not invoked (Passthrough may contain entity body) OR delete with payload
            if (!Boolean.TRUE.equals(msgContext.getProperty(PassThroughConstants.MESSAGE_BUILDER_INVOKED)) ||
                    !Boolean.TRUE.equals(msgContext.getProperty(PassThroughConstants.NO_ENTITY_BODY))) {
                //HTTP DELETE request with payload
                if (!isEmptyPayloadStream(msgContext)) {
                    return false;
                }
            }
            //Empty payload delete request
            return true;
        }
        //Not a HTTP DELETE request
        return false;
    }

    private static boolean isEmptyPayloadStream(MessageContext messageContext) throws AxisFault {

        boolean isEmpty = false;
        BufferedInputStream bufferedInputStream = (BufferedInputStream) messageContext
                .getProperty(PassThroughConstants.BUFFERED_INPUT_STREAM);
        if (bufferedInputStream != null) {
            try {
                bufferedInputStream.reset();
                bufferedInputStream.mark(0);
            } catch (Exception e) {
                // just ignore the error
            }

        } else {
            final Pipe pipe = (Pipe) messageContext.getProperty(PassThroughConstants.PASS_THROUGH_PIPE);
            if (pipe != null) {
                InputStream in = pipe.getInputStream();
                bufferedInputStream = new BufferedInputStream(in);
                // TODO: need to handle properly for the moment lets use around 100k
                // buffer.
                bufferedInputStream.mark(128 * 1024);
                messageContext.setProperty(PassThroughConstants.BUFFERED_INPUT_STREAM,
                        bufferedInputStream);
            }
        }
        try {
            isEmpty = RelayUtils.isEmptyPayloadStream(bufferedInputStream);
        } catch (IOException e) {
            handleException("Error while checking Message Payload Exists ", e);
        }
        return isEmpty;
    }

    /**
     * Check whether the we should overwrite the content type for the outgoing request.
     * @param msgContext MessageContext
     * @return whether to overwrite the content type for the outgoing request
     *
     */
    public static boolean shouldOverwriteContentType(MessageContext msgContext, TargetRequest request) {

        boolean builderInvoked = Boolean.TRUE.equals(msgContext
                .getProperty(PassThroughConstants.MESSAGE_BUILDER_INVOKED));

        boolean noEntityBodySet =
                Boolean.TRUE.equals(msgContext.getProperty(PassThroughConstants.NO_ENTITY_BODY));

        Map<String, LinkedHashSet<String>> headers = request.getHeaders();
        boolean contentTypeInRequest = false;
        if (headers.size() != 0 && (headers.get("Content-Type") != null || headers.get("content-type") != null)) {
            contentTypeInRequest = true;
        }
        
        // If builder is not invoked, which means the passthrough scenario, we should overwrite the content-type 
        // depending on the presence of the incoming content-type.
        // If builder is invoked and no entity body property is not set (which means there is a payload in the request)
        // we should consider overwriting the content-type.
        return (builderInvoked && !noEntityBodySet) || contentTypeInRequest;
    }

    /**
     * Function to check given inputstream is empty or not
     * Used to check whether content of the payload input stream is empty or not
     * @param inputStream target inputstream
     * @return true if it is a empty stream
     * @throws IOException
     */
    public static boolean isEmptyPayloadStream (InputStream inputStream) throws IOException {

        boolean isEmptyPayload = true;

        if (inputStream != null) {
            // read ahead few characters to see if the stream is valid.
            ReadOnlyBIS readOnlyStream = new ReadOnlyBIS(inputStream);

            /**
            * Checks for all empty or all whitespace streams and if found  sets isEmptyPayload to false. The while
            * loop exits if found any character other than space or end of stream reached.
            **/
            int c = readOnlyStream.read();
            while (c != -1) {
                if (c != 32) {
                    //if not a space, should be some character in entity body
                    isEmptyPayload = false;
                    break;
                }
                c = readOnlyStream.read();
            }
            readOnlyStream.reset();
            inputStream.reset();
        }

        return isEmptyPayload;
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

        messageContext.setProperty(AddressingConstants.DISABLE_ADDRESSING_FOR_IN_MESSAGES, "false");

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
                if (disableAck == null || disableAck.booleanValue() == false) {
                    ((RequestResponseTransport) requestResponseTransport)
                            .acknowledgeMessage(messageContext);
                }
            }
        } else if (AddressingHelper.isReplyRedirected(messageContext)
                && AddressingHelper.isFaultRedirected(messageContext)) {
            if (mepString.equals(WSDL2Constants.MEP_URI_IN_OUT)
                    || mepString.equals(WSDL2Constants.MEP_URI_IN_OUT)
                    || mepString.equals(WSDL2Constants.MEP_URI_IN_OUT)) {
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

                    if (disableAck == null || disableAck.booleanValue() == false) {
                        ((RequestResponseTransport) requestResponseTransport)
                                .acknowledgeMessage(messageContext);
                    }

                }
            }
        }
    }

    private static Boolean getDisableAck(MessageContext msgContext) throws AxisFault {
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
        return (mepString.equals(WSDL2Constants.MEP_URI_IN_ONLY)
                || mepString.equals(WSDL2Constants.MEP_URI_IN_ONLY) || mepString
                .equals(WSDL2Constants.MEP_URI_IN_ONLY));
    }

    /**
     * Perform an error log message to all logs @ ERROR and throws a AxisFault
     *
     * @param msg the log message
     * @param e   an Exception encountered
     * @throws AxisFault
     */
    private static void handleException(String msg, Exception e) throws AxisFault {
        log.error(msg, e);
        throw new AxisFault(msg, e);
    }

    /**
     * Consumes the data in pipe completely in the given message context and discard it
     *
     * @param msgContext Axis2 Message context which contains the data
     * @throws AxisFault
     */
    public static void consumeAndDiscardMessage(MessageContext msgContext) throws AxisFault {
        final Pipe pipe = (Pipe) msgContext.getProperty(PassThroughConstants.PASS_THROUGH_PIPE);
        if (pipe != null) {
            InputStream in = pipe.getInputStream();
            if (in != null) {
                try {
                    if (pipe.isConsumeRequired()) {
                        IOUtils.copy(in, new NullOutputStream());
                    }
                } catch (IOException exception) {
                    handleException("Error when consuming the input stream to discard ", exception);
                }
            }
        }
    }

    /**
     * An Un-closable, Read-Only, Reusable, BufferedInputStream
     */
    private static class ReadOnlyBIS extends BufferedInputStream {
        private static final String LOG_STREAM = "org.apache.synapse.transport.passthru.util.ReadOnlyStream";
        private static final Log logger = LogFactory.getLog(LOG_STREAM);

        public ReadOnlyBIS(InputStream inputStream) {
            super(inputStream);
            super.mark(Integer.MAX_VALUE);
            if (logger.isDebugEnabled()) {
                logger.debug("<init>");
            }
        }

        @Override
        public void close() throws IOException {
            super.reset();
            //super.mark(Integer.MAX_VALUE);
            if (logger.isDebugEnabled()) {
                logger.debug("#close");
            }
        }

        @Override
        public void mark(int readlimit) {
            if (logger.isDebugEnabled()) {
                logger.debug("#mark");
            }
        }

        @Override
        public boolean markSupported() {
            return true; //but we don't mark.
        }

        @Override
        public long skip(long n) {
            if (logger.isDebugEnabled()) {
                logger.debug("#skip");
            }
            return 0;
        }
    }
}
