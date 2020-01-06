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
package org.apache.synapse.message.senders.blocking;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.OperationClient;
import org.apache.axis2.client.Options;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.context.ServiceContext;
import org.apache.axis2.context.ServiceGroupContext;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.AxisServiceGroup;
import org.apache.axis2.description.WSDL2Constants;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.axis2.wsdl.WSDLConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.SynapseHandler;
import org.apache.synapse.commons.json.JsonUtil;
import org.apache.synapse.continuation.ContinuationStackManager;
import org.apache.synapse.continuation.SeqContinuationState;
import org.apache.synapse.core.axis2.AnonymousServiceFactory;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.endpoints.AbstractEndpoint;
import org.apache.synapse.endpoints.Endpoint;
import org.apache.synapse.endpoints.EndpointDefinition;
import org.apache.synapse.endpoints.IndirectEndpoint;
import org.apache.synapse.endpoints.ResolvingEndpoint;
import org.apache.synapse.endpoints.TemplateEndpoint;
import org.apache.synapse.util.MessageHelper;
import org.apache.synapse.util.xpath.SynapseXPath;

import javax.xml.namespace.QName;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BlockingMsgSender {
    public final static String DEFAULT_CLIENT_REPO = "./repository/deployment/client";
    public final static String DEFAULT_AXIS2_XML;

    static {
        String confPath = System.getProperty("conf.location");
        if (confPath == null) {
            confPath = System.getProperty("carbon.config.dir.path");
            if (confPath == null) {
                confPath = Paths.get("repository", "conf").toString();
            }
        }
        DEFAULT_AXIS2_XML = Paths.get(confPath, "axis2", "axis2_blocking_client.xml").toString();
    }

    private static Log log = LogFactory.getLog(BlockingMsgSender.class);
    private String clientRepository = null;
    private String axis2xml = null;
    private ConfigurationContext configurationContext = null;
    boolean initClientOptions = true;

    private final static String LOCAL_ANON_SERVICE = "__LOCAL_ANON_SERVICE__";

    private Pattern errorMsgPattern = Pattern.compile("Transport error: \\d{3} .*");

    private Pattern statusCodePattern = Pattern.compile("\\d{3}");

    public void init() {
        try {
            if (configurationContext == null) {
                configurationContext
                        = ConfigurationContextFactory.createConfigurationContextFromFileSystem(
                        clientRepository != null ? clientRepository : DEFAULT_CLIENT_REPO,
                        axis2xml != null ? axis2xml : DEFAULT_AXIS2_XML);
            }
        } catch (AxisFault e) {
            handleException("Error initializing BlockingMessageSender", e);
        }
    }

    public MessageContext send(Endpoint endpoint, MessageContext synapseInMsgCtx)
            throws Exception {

        if (log.isDebugEnabled()) {
            log.debug("Start Sending the Message ");
        }

        if (endpoint instanceof  IndirectEndpoint) {
            String endpointKey = ((IndirectEndpoint) endpoint).getKey();
            endpoint = synapseInMsgCtx.getEndpoint(endpointKey);
        }
        if (endpoint instanceof TemplateEndpoint) {
            endpoint = ((TemplateEndpoint) endpoint).getRealEndpoint();
        }
        //If the Endpoint is a resolving endpoint, real endpoint details are required to
        // get the correct endpoint definition
        if (endpoint instanceof ResolvingEndpoint) {
            SynapseXPath keyExpression = ((ResolvingEndpoint) endpoint).getKeyExpression();
            String key = keyExpression.stringValueOf(synapseInMsgCtx);
            endpoint = ((ResolvingEndpoint) endpoint).loadAndInitEndpoint(((Axis2MessageContext) synapseInMsgCtx).
                    getAxis2MessageContext().getConfigurationContext(), key);
        }

        AbstractEndpoint abstractEndpoint = (AbstractEndpoint) endpoint;
        if (!abstractEndpoint.isLeafEndpoint()) {
            handleException("Endpoint Type not supported");
        }

        // clear the message context properties related to endpoint in last service invocation
        Set keySet = synapseInMsgCtx.getPropertyKeySet();
        if (keySet != null) {
            keySet.remove(EndpointDefinition.DYNAMIC_URL_VALUE);
        }

        abstractEndpoint.executeEpTypeSpecificFunctions(synapseInMsgCtx);
        EndpointDefinition endpointDefinition = abstractEndpoint.getDefinition();

        org.apache.axis2.context.MessageContext axisInMsgCtx =
                ((Axis2MessageContext) synapseInMsgCtx).getAxis2MessageContext();
        org.apache.axis2.context.MessageContext axisOutMsgCtx =
                new org.apache.axis2.context.MessageContext();

        String endpointReferenceValue = null;
        if (endpointDefinition.getAddress() != null) {
            endpointReferenceValue = endpointDefinition.getAddress();
        } else if (axisInMsgCtx.getTo() != null) {
            endpointReferenceValue = axisInMsgCtx.getTo().getAddress();
        } else {
            handleException("Service url, Endpoint or 'To' header is required");
        }
        EndpointReference epr = new EndpointReference(endpointReferenceValue);
        axisOutMsgCtx.setTo(epr);

        AxisService anonymousService;
        if (endpointReferenceValue != null &&
            endpointReferenceValue.startsWith(Constants.TRANSPORT_LOCAL)) {
            configurationContext = axisInMsgCtx.getConfigurationContext();
            anonymousService =
                    AnonymousServiceFactory.getAnonymousService(
                            configurationContext.getAxisConfiguration(),
                            LOCAL_ANON_SERVICE);
        } else {
            anonymousService =
                    AnonymousServiceFactory.getAnonymousService(
                            null,
                            configurationContext.getAxisConfiguration(),
                            endpointDefinition.isAddressingOn() | endpointDefinition.isReliableMessagingOn(),
                            endpointDefinition.isReliableMessagingOn(),
                            endpointDefinition.isSecurityOn(),
                            false);
        }

        axisOutMsgCtx.setConfigurationContext(configurationContext);
        axisOutMsgCtx.setEnvelope(axisInMsgCtx.getEnvelope());
        axisOutMsgCtx.setProperty(HTTPConstants.NON_ERROR_HTTP_STATUS_CODES,
                axisInMsgCtx.getProperty(HTTPConstants.NON_ERROR_HTTP_STATUS_CODES));
        axisOutMsgCtx.setProperty(HTTPConstants.ERROR_HTTP_STATUS_CODES,
                axisInMsgCtx.getProperty(HTTPConstants.ERROR_HTTP_STATUS_CODES));
		axisOutMsgCtx.setProperty(SynapseConstants.DISABLE_CHUNKING,
                axisInMsgCtx.getProperty(SynapseConstants.DISABLE_CHUNKING));
		axisOutMsgCtx.setProperty(SynapseConstants.NO_KEEPALIVE,
                axisInMsgCtx.getProperty(SynapseConstants.NO_KEEPALIVE));
        //Can't refer to the Axis2 constant 'NO_DEFAULT_CONTENT_TYPE' defined in 1.6.1.wso2v23-SNAPSHOT until
        //an API change is done.
        axisOutMsgCtx.setProperty(SynapseConstants.NO_DEFAULT_CONTENT_TYPE,
                axisInMsgCtx.getProperty(SynapseConstants.NO_DEFAULT_CONTENT_TYPE));
		// Fill MessageContext
        BlockingMsgSenderUtils.fillMessageContext(endpointDefinition, axisOutMsgCtx, synapseInMsgCtx);
        if (JsonUtil.hasAJsonPayload(axisInMsgCtx)) {
            JsonUtil.cloneJsonPayload(axisInMsgCtx, axisOutMsgCtx);
        }

        Options clientOptions;
        if (initClientOptions) {
            clientOptions = new Options();
        } else {
            clientOptions = axisInMsgCtx.getOptions();
            clientOptions.setTo(epr);
        }
        // Fill Client options
        BlockingMsgSenderUtils.fillClientOptions(endpointDefinition, clientOptions, synapseInMsgCtx);

        anonymousService.getParent().addParameter(SynapseConstants.HIDDEN_SERVICE_PARAM, "true");
        ServiceGroupContext serviceGroupContext =
                new ServiceGroupContext(configurationContext,
                                        (AxisServiceGroup) anonymousService.getParent());
        ServiceContext serviceCtx = serviceGroupContext.getServiceContext(anonymousService);
        axisOutMsgCtx.setServiceContext(serviceCtx);

        // Invoke
        boolean isOutOnly = isOutOnly(synapseInMsgCtx, axisOutMsgCtx);
        try {
            if (isOutOnly) {
                sendRobust(axisOutMsgCtx, clientOptions, anonymousService, serviceCtx, synapseInMsgCtx);
                final String httpStatusCode =
                                           String.valueOf(axisOutMsgCtx.getProperty(SynapseConstants.HTTP_SENDER_STATUSCODE))
                                                                  .trim();
                /*
                 * Though this is OUT_ONLY operation, we need to set the
                 * response Status code so that others can make use of it.
                 */
                axisInMsgCtx.setProperty(SynapseConstants.HTTP_SC, httpStatusCode);
            } else {
                org.apache.axis2.context.MessageContext result =
                sendReceive(axisOutMsgCtx, clientOptions, anonymousService, serviceCtx, synapseInMsgCtx);
                if(result.getEnvelope() != null) {
                    synapseInMsgCtx.setEnvelope(result.getEnvelope());
                    if (JsonUtil.hasAJsonPayload(result)) {
                        JsonUtil.cloneJsonPayload(result, ((Axis2MessageContext) synapseInMsgCtx).getAxis2MessageContext());
                    }
                }
                final String statusCode =
                                          String.valueOf(result.getProperty(SynapseConstants.HTTP_SENDER_STATUSCODE))
                                                .trim();
                /*
                 * We need to set the response status code so that users can
                 * fetch it later.
                 */
                axisInMsgCtx.setProperty(SynapseConstants.HTTP_SC, statusCode);
                if ("false".equals(synapseInMsgCtx.getProperty(
                        SynapseConstants.BLOCKING_SENDER_PRESERVE_REQ_HEADERS))) {
                    axisInMsgCtx.setProperty(
                            org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS,
                            result.getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS));
                }

                synapseInMsgCtx.setProperty(SynapseConstants.BLOCKING_SENDER_ERROR, "false");
                return synapseInMsgCtx;
            }
        } catch (Exception ex) {
            /*
             * Extract the HTTP status code from the Exception message.
             */
            final String errorStatusCode = extractStatusCodeFromException(ex);
            axisInMsgCtx.setProperty(SynapseConstants.HTTP_SC, errorStatusCode);
            if (!isOutOnly) {
                //axisOutMsgCtx.getTransportOut().getSender().cleanup(axisOutMsgCtx);
                synapseInMsgCtx.setProperty(SynapseConstants.BLOCKING_SENDER_ERROR, "true");
                synapseInMsgCtx.setProperty(SynapseConstants.ERROR_EXCEPTION, ex);
                if (ex instanceof AxisFault) {
                    AxisFault fault = (AxisFault) ex;
                    setErrorDetails(synapseInMsgCtx, fault);
                    org.apache.axis2.context.MessageContext faultMC = fault.getFaultMessageContext();
                    if (faultMC != null) {
                        Object statusCode = faultMC.getProperty(SynapseConstants.HTTP_SENDER_STATUSCODE);
                        synapseInMsgCtx.setProperty(SynapseConstants.HTTP_SC, statusCode);
                        axisInMsgCtx.setProperty(SynapseConstants.HTTP_SC, statusCode);
                        synapseInMsgCtx.setEnvelope(faultMC.getEnvelope());
                    }
                }
                return synapseInMsgCtx;
            } else {
                if (ex instanceof AxisFault) {
                    AxisFault fault = (AxisFault) ex;
                    setErrorDetails(synapseInMsgCtx, fault);
                }
            }
            handleException("Error sending Message to url : " +
                            ((AbstractEndpoint) endpoint).getDefinition().getAddress(), ex);
        }
        return null;
    }

    /**
     * Blocking Invocation
     *
     * @param endpointDefinition the endpoint being sent to.
     * @param synapseInMsgCtx the outgoing synapse message.
     * @throws AxisFault on errors.
     */
    public void send(EndpointDefinition endpointDefinition, MessageContext synapseInMsgCtx) throws AxisFault {

        if (log.isDebugEnabled()) {
            log.debug("Start Sending the Message ");
        }
        org.apache.axis2.context.MessageContext axisInMsgCtx =
                ((Axis2MessageContext) synapseInMsgCtx).getAxis2MessageContext();
        org.apache.axis2.context.MessageContext axisOutMsgCtx =
                new org.apache.axis2.context.MessageContext();

        String endpointReferenceValue = null;
        if (endpointDefinition.getAddress() != null) {
            endpointReferenceValue = endpointDefinition.getAddress();
        } else if (axisInMsgCtx.getTo() != null) {
            endpointReferenceValue = axisInMsgCtx.getTo().getAddress();
        } else {
            handleException("Service url, Endpoint or 'To' header is required");
        }
        EndpointReference epr = new EndpointReference(endpointReferenceValue);
        axisOutMsgCtx.setTo(epr);

        AxisService anonymousService;
        if (endpointReferenceValue != null &&
                endpointReferenceValue.startsWith(Constants.TRANSPORT_LOCAL)) {
            configurationContext = axisInMsgCtx.getConfigurationContext();
            anonymousService =
                    AnonymousServiceFactory.getAnonymousService(
                            configurationContext.getAxisConfiguration(),
                            LOCAL_ANON_SERVICE);
        } else {
            anonymousService =
                    AnonymousServiceFactory.getAnonymousService(
                            null,
                            configurationContext.getAxisConfiguration(),
                            endpointDefinition.isAddressingOn() | endpointDefinition.isReliableMessagingOn(),
                            endpointDefinition.isReliableMessagingOn(),
                            endpointDefinition.isSecurityOn(),
                            false);
        }

        axisOutMsgCtx.setConfigurationContext(configurationContext);
        axisOutMsgCtx.setEnvelope(axisInMsgCtx.getEnvelope());
        axisOutMsgCtx.setProperty(HTTPConstants.NON_ERROR_HTTP_STATUS_CODES,
                axisInMsgCtx.getProperty(HTTPConstants.NON_ERROR_HTTP_STATUS_CODES));
        axisOutMsgCtx.setProperty(HTTPConstants.ERROR_HTTP_STATUS_CODES,
                axisInMsgCtx.getProperty(HTTPConstants.ERROR_HTTP_STATUS_CODES));
        axisOutMsgCtx.setProperty(SynapseConstants.DISABLE_CHUNKING,
                axisInMsgCtx.getProperty(SynapseConstants.DISABLE_CHUNKING));
        axisOutMsgCtx.setProperty(SynapseConstants.NO_KEEPALIVE,
                axisInMsgCtx.getProperty(SynapseConstants.NO_KEEPALIVE));
        // Fill MessageContext
        BlockingMsgSenderUtils.fillMessageContext(endpointDefinition, axisOutMsgCtx, synapseInMsgCtx);
        if (JsonUtil.hasAJsonPayload(axisInMsgCtx)) {
            JsonUtil.cloneJsonPayload(axisInMsgCtx, axisOutMsgCtx);
        }

        Options clientOptions;
        if (initClientOptions) {
            clientOptions = new Options();
        } else {
            clientOptions = axisInMsgCtx.getOptions();
            clientOptions.setTo(epr);
        }
        // Fill Client options
        BlockingMsgSenderUtils.fillClientOptions(endpointDefinition, clientOptions, synapseInMsgCtx);

        anonymousService.getParent().addParameter(SynapseConstants.HIDDEN_SERVICE_PARAM, "true");
        ServiceGroupContext serviceGroupContext =
                new ServiceGroupContext(configurationContext,
                        (AxisServiceGroup) anonymousService.getParent());
        ServiceContext serviceCtx = serviceGroupContext.getServiceContext(anonymousService);
        axisOutMsgCtx.setServiceContext(serviceCtx);

        // Invoke
        boolean isOutOnly = isOutOnly(synapseInMsgCtx, axisOutMsgCtx);
        try {
            if (isOutOnly) {
                sendRobust(axisOutMsgCtx, clientOptions, anonymousService, serviceCtx, synapseInMsgCtx);
                final String httpStatusCode =
                        String.valueOf(axisOutMsgCtx.getProperty(SynapseConstants.HTTP_SENDER_STATUSCODE))
                                .trim();
                /*
                 * Though this is OUT_ONLY operation, we need to set the
                 * response Status code so that others can make use of it.
                 */
                axisInMsgCtx.setProperty(SynapseConstants.HTTP_SC, httpStatusCode);
                synapseInMsgCtx.setProperty(SynapseConstants.BLOCKING_SENDER_ERROR, "false");
            } else {
                org.apache.axis2.context.MessageContext result =
                        sendReceive(axisOutMsgCtx, clientOptions, anonymousService, serviceCtx, synapseInMsgCtx);
                synapseInMsgCtx.setEnvelope(result.getEnvelope());
                if (JsonUtil.hasAJsonPayload(result)) {
                    JsonUtil.cloneJsonPayload(result, ((Axis2MessageContext) synapseInMsgCtx).getAxis2MessageContext());
                }
                final String statusCode =
                        String.valueOf(result.getProperty(SynapseConstants.HTTP_SENDER_STATUSCODE))
                                .trim();
                /*
                 * We need to set the response status code so that users can
                 * fetch it later.
                 */
                axisInMsgCtx.setProperty(SynapseConstants.HTTP_SC, statusCode);
                if ("false".equals(synapseInMsgCtx.getProperty(
                        SynapseConstants.BLOCKING_SENDER_PRESERVE_REQ_HEADERS))) {
                    axisInMsgCtx.setProperty(
                            org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS,
                            result.getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS));
                }
                synapseInMsgCtx.setProperty(SynapseConstants.BLOCKING_SENDER_ERROR, "false");
            }
        } catch (Exception ex) {
            /*
             * Extract the HTTP status code from the Exception message.
             */
            final String errorStatusCode = extractStatusCodeFromException(ex);
            axisInMsgCtx.setProperty(SynapseConstants.HTTP_SC, errorStatusCode);
            synapseInMsgCtx.setProperty(SynapseConstants.BLOCKING_SENDER_ERROR, "true");
            synapseInMsgCtx.setProperty(SynapseConstants.ERROR_EXCEPTION, ex);
            if (ex instanceof AxisFault) {
                AxisFault fault = (AxisFault) ex;

                int errorCode = SynapseConstants.BLOCKING_SENDER_OPERATION_FAILED;
                if (fault.getFaultCode() != null && fault.getFaultCode().getLocalPart() != null &&
                        !"".equals(fault.getFaultCode().getLocalPart())) {
                    try {
                        errorCode = Integer.parseInt(fault.getFaultCode().getLocalPart());
                    } catch (NumberFormatException e) {
                        errorCode = SynapseConstants.BLOCKING_SENDER_OPERATION_FAILED;
                    }
                }
                synapseInMsgCtx.setProperty(SynapseConstants.ERROR_CODE, errorCode);

                synapseInMsgCtx.setProperty(SynapseConstants.ERROR_MESSAGE, fault.getMessage());
                synapseInMsgCtx.setProperty(SynapseConstants.ERROR_DETAIL,
                        fault.getDetail() != null ? fault.getDetail().getText() : "");
                if (!isOutOnly) {
                    org.apache.axis2.context.MessageContext faultMC = fault.getFaultMessageContext();
                    if (faultMC != null) {
                        Object statusCode = faultMC.getProperty(SynapseConstants.HTTP_SENDER_STATUSCODE);
                        synapseInMsgCtx.setProperty(SynapseConstants.HTTP_SC, statusCode);
                        axisInMsgCtx.setProperty(SynapseConstants.HTTP_SC, statusCode);
                        synapseInMsgCtx.setEnvelope(faultMC.getEnvelope());
                    }
                }
            }
        }
        // Check fault occure when send the request to endpoint
        if ("true".equals(synapseInMsgCtx.getProperty(SynapseConstants.BLOCKING_SENDER_ERROR))) {
            // Handle the fault
            synapseInMsgCtx.getFaultStack().pop().handleFault(synapseInMsgCtx,
                    (Exception) synapseInMsgCtx.getProperty(SynapseConstants.ERROR_EXCEPTION));
        } else {
            // If a message was successfully processed to give it a chance to clear up or reset its state to active
            Stack faultStack = synapseInMsgCtx.getFaultStack();
            if (faultStack != null && !faultStack.isEmpty()) {
                if (faultStack.peek() instanceof AbstractEndpoint) {
                    ((AbstractEndpoint) synapseInMsgCtx.getFaultStack().pop()).onSuccess();
                }
                // Remove all endpoint related fault handlers if any
                while (!faultStack.empty() && faultStack.peek() instanceof AbstractEndpoint) {
                    faultStack.pop();
                }
            }
        }
    }

    private void sendRobust(org.apache.axis2.context.MessageContext axisOutMsgCtx,
                            Options clientOptions, AxisService anonymousService,
                            ServiceContext serviceCtx, MessageContext synapseInMsgCtx) throws AxisFault {
        this.invokeHandlers(synapseInMsgCtx, false);
        AxisOperation axisAnonymousOperation =
                anonymousService.getOperation(new QName(AnonymousServiceFactory.OUT_ONLY_OPERATION));
        OperationClient operationClient =
                axisAnonymousOperation.createClient(serviceCtx, clientOptions);
        operationClient.addMessageContext(axisOutMsgCtx);
        axisOutMsgCtx.setAxisMessage(
                axisAnonymousOperation.getMessage(WSDLConstants.MESSAGE_LABEL_OUT_VALUE));
        operationClient.execute(true);
        axisOutMsgCtx.getTransportOut().getSender().cleanup(axisOutMsgCtx);

    }

    private org.apache.axis2.context.MessageContext sendReceive(
            org.apache.axis2.context.MessageContext axisOutMsgCtx,
            Options clientOptions,
            AxisService anonymousService,
            ServiceContext serviceCtx, MessageContext synapseInMsgCtx)
            throws AxisFault {

        AxisOperation axisAnonymousOperation =
                anonymousService.getOperation(new QName(AnonymousServiceFactory.OUT_IN_OPERATION));
        OperationClient operationClient =
                axisAnonymousOperation.createClient(serviceCtx, clientOptions);
        operationClient.addMessageContext(axisOutMsgCtx);
        axisOutMsgCtx.setAxisMessage(
                axisAnonymousOperation.getMessage(WSDLConstants.MESSAGE_LABEL_OUT_VALUE));
        this.invokeHandlers(synapseInMsgCtx, false);
        operationClient.execute(true);
        org.apache.axis2.context.MessageContext resultMsgCtx =
                operationClient.getMessageContext(WSDLConstants.MESSAGE_LABEL_IN_VALUE);

        org.apache.axis2.context.MessageContext returnMsgCtx =
                new org.apache.axis2.context.MessageContext();
        if (resultMsgCtx.getEnvelope() != null) {
            returnMsgCtx.setEnvelope(MessageHelper.cloneSOAPEnvelope(resultMsgCtx.getEnvelope()));
            if (JsonUtil.hasAJsonPayload(resultMsgCtx)) {
               JsonUtil.cloneJsonPayload(resultMsgCtx, returnMsgCtx);
            }
        } else {
            if (axisOutMsgCtx.isSOAP11()) {
                returnMsgCtx.setEnvelope(OMAbstractFactory.getSOAP11Factory().getDefaultEnvelope());
            } else {
                returnMsgCtx.setEnvelope(OMAbstractFactory.getSOAP12Factory().getDefaultEnvelope());
            }
        }
        returnMsgCtx.setProperty(SynapseConstants.HTTP_SENDER_STATUSCODE,
                                 resultMsgCtx.getProperty(SynapseConstants.HTTP_SENDER_STATUSCODE));
        axisOutMsgCtx.getTransportOut().getSender().cleanup(axisOutMsgCtx);
        returnMsgCtx.setProperty(
                org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS,
                resultMsgCtx.getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS));
        this.invokeHandlers(synapseInMsgCtx, true);
        return returnMsgCtx;
    }

    private boolean isOutOnly(MessageContext messageIn,
                              org.apache.axis2.context.MessageContext axis2Ctx) {
        return "true".equals(messageIn.getProperty(SynapseConstants.OUT_ONLY)) ||
               axis2Ctx.getOperationContext() != null &&
               WSDL2Constants.MEP_URI_IN_ONLY.equals(axis2Ctx.getOperationContext().
                       getAxisOperation().getMessageExchangePattern());
    }

    public void setClientRepository(String clientRepository) {
        this.clientRepository = clientRepository;
    }

    public void setAxis2xml(String axis2xml) {
        this.axis2xml = axis2xml;
    }

    public void setConfigurationContext(ConfigurationContext configurationContext) {
        this.configurationContext = configurationContext;
    }

    public void setInitClientOptions(boolean initClientOptions) {
        this.initClientOptions = initClientOptions;
    }

    private void handleException(String msg, Exception e) {
        log.error(msg, e);
        throw new SynapseException(msg, e);
    }

    private void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }

    private String extractStatusCodeFromException(Exception exception) {
        String responseStatusCode = "";
        Matcher errMsgMatcher = errorMsgPattern.matcher(exception.getMessage());
        Matcher statusCodeMatcher = statusCodePattern.matcher(exception.getMessage());
        while (errMsgMatcher.find() && statusCodeMatcher.find()) {
            responseStatusCode = statusCodeMatcher.group().trim();
            break;
        }
        return responseStatusCode;
    }

    /**
     * Invoke Synapse Handlers
     *
     * @param synCtx synapse message context
     * @param isResponse whether message is response path or not
     */
    private void invokeHandlers(MessageContext synCtx, boolean isResponse) {

        Iterator<SynapseHandler> iterator =
                synCtx.getEnvironment().getSynapseHandlers().iterator();

        if (iterator.hasNext()) {

            if (isResponse) {
                do {
                    SynapseHandler handler = iterator.next();
                    if (!handler.handleResponseInFlow(synCtx)) {
                        log.warn("Synapse not executed in the response in path");
                    }
                } while (iterator.hasNext());
            } else {
                do {
                    SynapseHandler handler = iterator.next();
                    if (!handler.handleRequestOutFlow(synCtx)) {
                        log.warn("Synapse not executed in the request out path");
                    }
                } while (iterator.hasNext());
            }
        }
    }

    private void setErrorDetails(MessageContext synapseInMsgCtx, AxisFault fault) {
        int errorCode = SynapseConstants.BLOCKING_SENDER_OPERATION_FAILED;

        if (fault.getFaultCode() != null && fault.getFaultCode().getLocalPart() != null &&
                !"".equals(fault.getFaultCode().getLocalPart())) {
            try {
                errorCode = Integer.parseInt(fault.getFaultCode().getLocalPart());
            } catch (NumberFormatException e) {
                errorCode = SynapseConstants.BLOCKING_SENDER_OPERATION_FAILED;
            }
        }
        synapseInMsgCtx.setProperty(SynapseConstants.ERROR_CODE, errorCode);
        synapseInMsgCtx.setProperty(SynapseConstants.ERROR_MESSAGE, fault.getMessage());
        synapseInMsgCtx.setProperty(SynapseConstants.ERROR_DETAIL,
                fault.getDetail() != null ? fault.getDetail().getText() : getStackTrace(fault));
    }

    private String getStackTrace(Throwable aThrowable) {
        final Writer result = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(result);
        aThrowable.printStackTrace(printWriter);
        return result.toString();
    }
}
