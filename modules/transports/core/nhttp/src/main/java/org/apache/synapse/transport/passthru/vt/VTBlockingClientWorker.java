/**
 *  Copyright (c) 2024, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.synapse.transport.passthru.vt;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.impl.llom.soap11.SOAP11Factory;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.AddressingConstants;
import org.apache.axis2.builder.BuilderUtil;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.WSDL2Constants;
import org.apache.axis2.engine.AxisEngine;
import org.apache.axis2.util.JavaUtils;
import org.apache.axis2.wsdl.WSDLConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;
import org.apache.http.protocol.HTTP;
import org.apache.synapse.transport.nhttp.NhttpConstants;
import org.apache.synapse.transport.passthru.PassThroughConstants;
import org.apache.synapse.transport.passthru.WorkerState;
import org.apache.synapse.transport.passthru.config.TargetConfiguration;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

/**
 * Client-side worker that processes an HTTP response received from a backend server
 * using <b>blocking I/O</b> within a Virtual Thread.
 * <p>
 * This is the VT equivalent of {@link org.apache.synapse.transport.passthru.ClientWorker}.
 * It constructs a response {@link MessageContext} from the {@link VTTargetResponse} and
 * feeds it into the Axis2 engine via {@code AxisEngine.receive(responseMsgCtx)}.
 * Since this runs in a Virtual Thread, the blocking call is cheap.
 * </p>
 */
public class VTBlockingClientWorker implements Runnable {

    private static final Log log = LogFactory.getLog(VTBlockingClientWorker.class);

    private final TargetConfiguration targetConfiguration;
    private final MessageContext requestMessageContext;
    private final VTTargetResponse response;
    private MessageContext responseMsgCtx;
    private boolean expectEntityBody;
    private WorkerState state = WorkerState.CREATED;

    public VTBlockingClientWorker(TargetConfiguration targetConfiguration,
                                  MessageContext outMsgCtx,
                                  VTTargetResponse response) {
        this.targetConfiguration = targetConfiguration;
        this.requestMessageContext = outMsgCtx;
        this.response = response;
        this.expectEntityBody = response.isExpectResponseBody();

        Map<String, String> headers = response.getHeaders();

        // Handle Location header rewriting (same as original ClientWorker)
        String oriURL = headers.get(PassThroughConstants.LOCATION);
        if (oriURL != null && response.getStatus() != HttpStatus.SC_MOVED_TEMPORARILY
                && response.getStatus() != HttpStatus.SC_MOVED_PERMANENTLY
                && response.getStatus() != HttpStatus.SC_CREATED
                && response.getStatus() != HttpStatus.SC_SEE_OTHER
                && response.getStatus() != HttpStatus.SC_TEMPORARY_REDIRECT) {
            URL url;
            String urlContext = null;
            try {
                url = new URL(oriURL);
                urlContext = url.getFile();
            } catch (MalformedURLException e) {
                if (log.isDebugEnabled()) {
                    log.debug("Relative URL received for Location: " + oriURL, e);
                }
                urlContext = oriURL;
            }
            headers.remove(PassThroughConstants.LOCATION);
            String prefix = (String) outMsgCtx.getProperty(PassThroughConstants.SERVICE_PREFIX);
            if (prefix != null) {
                if (urlContext != null && urlContext.startsWith("/")) {
                    urlContext = urlContext.substring(1);
                }
                headers.put(PassThroughConstants.LOCATION, prefix + urlContext);
            }
        }

        // Build response message context
        try {
            responseMsgCtx = outMsgCtx.getOperationContext()
                    .getMessageContext(WSDL2Constants.MESSAGE_LABEL_IN);
            if (responseMsgCtx != null) {
                responseMsgCtx.setSoapAction("");
            }
        } catch (AxisFault af) {
            log.error("Error getting IN message context from the operation context", af);
            return;
        }

        if (responseMsgCtx == null) {
            if (outMsgCtx.getOperationContext().isComplete()) {
                if (log.isDebugEnabled()) {
                    log.debug("Error getting IN message context from the operation context. "
                            + "Possibly an RM terminate sequence message");
                }
                return;
            }
            responseMsgCtx = new MessageContext();
            responseMsgCtx.setOperationContext(outMsgCtx.getOperationContext());
        }

        // Defensive cleanup for keep-alive flows: when Axis2 returns an existing
        // IN MessageContext, stale response properties from a previous exchange
        // can survive and result in incorrect status/header propagation (e.g.
        // HTTP 405 with empty body). Reset all response-shaping properties
        // before repopulating this context from the current backend response.
        responseMsgCtx.removeProperty(PassThroughConstants.NO_ENTITY_BODY);
        responseMsgCtx.removeProperty(PassThroughConstants.HTTP_SC);
        responseMsgCtx.removeProperty(PassThroughConstants.HTTP_SC_DESC);
        responseMsgCtx.removeProperty(PassThroughConstants.FAULT_MESSAGE);
        responseMsgCtx.removeProperty(MessageContext.TRANSPORT_HEADERS);
        responseMsgCtx.removeProperty(NhttpConstants.EXCESS_TRANSPORT_HEADERS);
        responseMsgCtx.removeProperty(PassThroughConstants.PASS_THROUGH_PIPE);
        responseMsgCtx.removeProperty(VTConstants.VT_INPUT_STREAM_PIPE);
        responseMsgCtx.removeProperty(Constants.Configuration.CONTENT_TYPE);
        responseMsgCtx.removeProperty(Constants.Configuration.MESSAGE_TYPE);
        responseMsgCtx.removeProperty(Constants.Configuration.CHARACTER_SET_ENCODING);

        responseMsgCtx.setProperty("PRE_LOCATION_HEADER", oriURL);

        // Copy VT-specific properties for response routing
        responseMsgCtx.setProperty(VTConstants.VT_SOURCE_CONNECTION,
                outMsgCtx.getProperty(VTConstants.VT_SOURCE_CONNECTION));
        responseMsgCtx.setProperty(VTConstants.VT_SOURCE_CONFIGURATION,
                outMsgCtx.getProperty(VTConstants.VT_SOURCE_CONFIGURATION));
        responseMsgCtx.setProperty(AddressingConstants.DISABLE_ADDRESSING_FOR_IN_MESSAGES,
                outMsgCtx.getProperty(AddressingConstants.DISABLE_ADDRESSING_FOR_IN_MESSAGES));
        responseMsgCtx.setProperty(PassThroughConstants.INTERNAL_EXCEPTION_ORIGIN,
                outMsgCtx.getProperty(PassThroughConstants.INTERNAL_EXCEPTION_ORIGIN));

        responseMsgCtx.setServerSide(true);
        responseMsgCtx.setDoingREST(outMsgCtx.isDoingREST());
        responseMsgCtx.setProperty(MessageContext.TRANSPORT_IN,
                outMsgCtx.getProperty(MessageContext.TRANSPORT_IN));
        responseMsgCtx.setTransportIn(outMsgCtx.getTransportIn());
        responseMsgCtx.setTransportOut(outMsgCtx.getTransportOut());

        responseMsgCtx.setProperty(PassThroughConstants.INVOKED_REST, outMsgCtx.isDoingREST());
        responseMsgCtx.setProperty(PassThroughConstants.ORIGINAL_HTTP_SC, response.getStatus());
        responseMsgCtx.setProperty(PassThroughConstants.ORIGINAL_HTTP_REASON_PHRASE,
                response.getStatusLine());

        // Set transport headers
        Map<String, String> headerMap = new TreeMap<>(new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return o1.compareToIgnoreCase(o2);
            }
        });
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            headerMap.put(entry.getKey(), entry.getValue());
        }
        responseMsgCtx.setProperty(MessageContext.TRANSPORT_HEADERS, headerMap);
        responseMsgCtx.setProperty(NhttpConstants.EXCESS_TRANSPORT_HEADERS, response.getExcessHeaders());

        if (response.getStatus() == 202) {
            responseMsgCtx.setProperty(AddressingConstants.DISABLE_ADDRESSING_FOR_OUT_MESSAGES,
                    Boolean.TRUE);
            responseMsgCtx.setProperty(PassThroughConstants.MESSAGE_BUILDER_INVOKED, Boolean.FALSE);
            responseMsgCtx.setProperty(NhttpConstants.SC_ACCEPTED, Boolean.TRUE);
        }

        responseMsgCtx.setAxisMessage(outMsgCtx.getOperationContext().getAxisOperation()
                .getMessage(WSDLConstants.MESSAGE_LABEL_IN_VALUE));
        responseMsgCtx.setOperationContext(outMsgCtx.getOperationContext());
        responseMsgCtx.setConfigurationContext(outMsgCtx.getConfigurationContext());
        responseMsgCtx.setTo(null);

        // Wire up the response body as a pipe / input stream
        responseMsgCtx.setProperty(VTConstants.VT_INPUT_STREAM_PIPE,
                new VTInputStreamPipe(response.getBodyInputStream()));
        responseMsgCtx.setProperty(VTConstants.VT_TARGET_RESPONSE, response);

        // Propagate the OUT_TRANSPORT_INFO so the sender can route the response back
        responseMsgCtx.setProperty(Constants.OUT_TRANSPORT_INFO,
                outMsgCtx.getProperty(Constants.OUT_TRANSPORT_INFO));
    }

    @Override
    public void run() {
        if (responseMsgCtx == null) {
            return;
        }
        state = WorkerState.RUNNING;

        // Capture the server-worker reference up-front so the finally block
        // can always guarantee the response is written, even when exceptions
        // escape from AxisEngine or the error-recovery path.
        VTBlockingServerWorker serverWorker = null;
        Object outInfo = responseMsgCtx.getProperty(Constants.OUT_TRANSPORT_INFO);
        if (outInfo instanceof VTBlockingServerWorker) {
            serverWorker = (VTBlockingServerWorker) outInfo;
        }

        try {
            if (expectEntityBody) {
                String cType = response.getHeader(HTTP.CONTENT_TYPE);
                if (cType == null) {
                    cType = response.getHeader(HTTP.CONTENT_TYPE.toLowerCase());
                }
                String contentType = cType != null ? cType : inferContentType();

                responseMsgCtx.setProperty(Constants.Configuration.CONTENT_TYPE, contentType);

                String charSetEnc = BuilderUtil.getCharSetEncoding(contentType);
                if (charSetEnc == null) {
                    charSetEnc = MessageContext.DEFAULT_CHAR_SET_ENCODING;
                }
                if (contentType != null) {
                    responseMsgCtx.setProperty(
                            Constants.Configuration.CHARACTER_SET_ENCODING,
                            contentType.indexOf("charset") > 0
                                    ? charSetEnc : MessageContext.DEFAULT_CHAR_SET_ENCODING);
                    responseMsgCtx.removeProperty(PassThroughConstants.NO_ENTITY_BODY);
                }

                responseMsgCtx.setServerSide(false);
                SOAPFactory fac = OMAbstractFactory.getSOAP11Factory();
                SOAPEnvelope envelope = fac.getDefaultEnvelope();
                try {
                    responseMsgCtx.setEnvelope(envelope);
                } catch (AxisFault axisFault) {
                    log.error("Error setting SOAP envelope", axisFault);
                }
                responseMsgCtx.setServerSide(true);
            } else {
                responseMsgCtx.setProperty(PassThroughConstants.NO_ENTITY_BODY, Boolean.TRUE);
                responseMsgCtx.setEnvelope(new SOAP11Factory().getDefaultEnvelope());
            }

            int statusCode = response.getStatus();
            responseMsgCtx.setProperty(PassThroughConstants.HTTP_SC, statusCode);
            responseMsgCtx.setProperty(PassThroughConstants.HTTP_SC_DESC, response.getStatusLine());
            if (statusCode >= 400) {
                responseMsgCtx.setProperty(PassThroughConstants.FAULT_MESSAGE,
                        PassThroughConstants.TRUE);
            }
            // In the VT model we are blocking, so set NON_BLOCKING_TRANSPORT to false
            responseMsgCtx.setProperty(PassThroughConstants.NON_BLOCKING_TRANSPORT, false);

            // Process the response through the Axis2 engine.
            // The standard PassThroughHttpSender may be invoked during the OUT
            // flow because the axis2.xml may not have a "vt-http" sender
            // registered. Its sendUsingOutputStream() will fail, which is
            // expected — the finally block below guarantees the response is
            // written via submitResponse().
            try {
                AxisEngine.receive(responseMsgCtx);
            } catch (AxisFault af) {
                log.error("Fault processing response message through Axis2", af);
                String errorMessage = "Fault processing response message through Axis2: "
                        + af.getMessage();
                responseMsgCtx.setProperty(NhttpConstants.SENDING_FAULT, Boolean.TRUE);
                responseMsgCtx.setProperty(NhttpConstants.ERROR_CODE,
                        NhttpConstants.RESPONSE_PROCESSING_FAILURE);
                responseMsgCtx.setProperty(NhttpConstants.ERROR_MESSAGE,
                        errorMessage.split("\n")[0]);
                responseMsgCtx.setProperty(NhttpConstants.ERROR_DETAIL,
                        JavaUtils.stackToString(af));
                responseMsgCtx.setProperty(NhttpConstants.ERROR_EXCEPTION, af);
                try {
                    responseMsgCtx.getAxisOperation().getMessageReceiver().receive(responseMsgCtx);
                } catch (Exception nested) {
                    log.debug("Error in fault-recovery receive, "
                            + "response will be written by finally block", nested);
                }
            }

        } catch (AxisFault af) {
            log.error("Fault creating response SOAP envelope", af);
        } finally {
            // --------------------------------------------------------
            // Guarantee: always write the response back to the client.
            // This covers every exception path: standard sender failure,
            // error-recovery failure, envelope creation failure, etc.
            // --------------------------------------------------------
            if (serverWorker != null && !serverWorker.isResponseSent()) {
                try {
                    if (log.isDebugEnabled()) {
                        log.debug("Writing response from VTBlockingClientWorker "
                                + "finally block (sender did not call submitResponse)");
                    }
                    serverWorker.submitResponse(responseMsgCtx);
                } catch (Exception e) {
                    log.error("Failed to write response in finally block", e);
                }
            }
            state = WorkerState.FINISHED;
            MessageContext.destroyCurrentMessageContext();
        }
    }

    private String inferContentType() {
        Map<String, String> headers = response.getHeaders();
        for (String header : headers.keySet()) {
            if (HTTP.CONTENT_TYPE.equalsIgnoreCase(header)) {
                return headers.get(header);
            }
        }
        // Check content-type from target configuration
        Object cTypeProperty = responseMsgCtx.getProperty(PassThroughConstants.CONTENT_TYPE);
        if (cTypeProperty != null) {
            return cTypeProperty.toString();
        }

        // Check Content-Length to determine if there's a body
        String contentLengthHeader = headers.get(HTTP.CONTENT_LEN);
        boolean contentLengthPresent = false;
        if (contentLengthHeader != null) {
            contentLengthPresent = true;
        } else {
            for (String h : headers.keySet()) {
                if (HTTP.CONTENT_LEN.equalsIgnoreCase(h)) {
                    contentLengthHeader = headers.get(h);
                    contentLengthPresent = true;
                    break;
                }
            }
        }

        boolean transferEncodingPresent = headers.containsKey(HTTP.TRANSFER_ENCODING);
        if (!transferEncodingPresent) {
            for (String h : headers.keySet()) {
                if (HTTP.TRANSFER_ENCODING.equalsIgnoreCase(h)) {
                    transferEncodingPresent = true;
                    break;
                }
            }
        }

        if ((!contentLengthPresent && !transferEncodingPresent)
                || "0".equals(contentLengthHeader)) {
            responseMsgCtx.setProperty(PassThroughConstants.NO_ENTITY_BODY, Boolean.TRUE);
            return null;
        }

        return PassThroughConstants.DEFAULT_CONTENT_TYPE;
    }

    public WorkerState getWorkerState() {
        return state;
    }
}
