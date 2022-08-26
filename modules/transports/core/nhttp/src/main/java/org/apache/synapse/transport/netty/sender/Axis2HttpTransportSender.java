/*
 *  Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.apache.synapse.transport.netty.sender;

import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.AddressingConstants;
import org.apache.axis2.addressing.AddressingHelper;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.axis2.handlers.AbstractHandler;
import org.apache.axis2.transport.TransportSender;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.transport.netty.BridgeConstants;
import org.apache.synapse.transport.netty.config.TargetConfiguration;
import org.apache.synapse.transport.netty.util.HttpUtils;
import org.apache.synapse.transport.netty.util.RequestResponseUtils;
import org.wso2.transport.http.netty.contract.HttpClientConnector;
import org.wso2.transport.http.netty.contract.HttpWsConnectorFactory;
import org.wso2.transport.http.netty.contractimpl.DefaultHttpWsConnectorFactory;
import org.wso2.transport.http.netty.contractimpl.sender.channel.BootstrapConfiguration;
import org.wso2.transport.http.netty.contractimpl.sender.channel.pool.ConnectionManager;
import org.wso2.transport.http.netty.message.Http2PushPromise;
import org.wso2.transport.http.netty.message.HttpCarbonMessage;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

/**
 * {@code Axis2HttpTransportSender} receives the outgoing axis2 {@code MessageContext}, convert it into a
 * {@code HttpCarbonMessage} and deliver it to the Http Client connector.
 */
public class Axis2HttpTransportSender extends AbstractHandler implements TransportSender {

    private static final Log LOG = LogFactory.getLog(Axis2HttpTransportSender.class);

    /**
     * The instance that handles connection pool management.
     */
    ConnectionManager connectionManager;

    /**
     * This instance can be used to create client connectors.
     */
    HttpWsConnectorFactory httpWsConnectorFactory;

    protected TargetConfiguration targetConfiguration;

    BootstrapConfiguration bootstrapConfiguration;

    @Override
    public void init(ConfigurationContext configurationContext, TransportOutDescription transportOutDescription)
            throws AxisFault {

        httpWsConnectorFactory = new DefaultHttpWsConnectorFactory();
        connectionManager = HttpUtils.getConnectionManager();
        bootstrapConfiguration = new BootstrapConfiguration(new HashMap<>());
        targetConfiguration = new TargetConfiguration(configurationContext, transportOutDescription);
        targetConfiguration.build();
    }

    @Override
    public InvocationResponse invoke(MessageContext msgCtx) throws AxisFault {

        if (AddressingHelper.isReplyRedirected(msgCtx)) {
            msgCtx.setProperty(BridgeConstants.IGNORE_SC_ACCEPTED, BridgeConstants.VALUE_TRUE);
        }

        EndpointReference destinationEPR = RequestResponseUtils.getDestinationEPR(msgCtx);
        if (isRequestToBackend(destinationEPR)) {
            try {
                URL destinationURL = new URL(destinationEPR.getAddress());
                sendRequestToBackendService(msgCtx, destinationURL);
            } catch (MalformedURLException e) {
                handleException("Malformed URL in the target EPR", e);
            } catch (IOException e) {
                handleException("Error while sending the request to the backend service "
                        + destinationEPR.getAddress(), e);
            }
        } else {
            HttpCarbonMessage clientRequest =
                    (HttpCarbonMessage) msgCtx.getProperty(BridgeConstants.HTTP_CLIENT_REQUEST_CARBON_MESSAGE);
            if (clientRequest == null) {
                LOG.warn("Unable to find the original client request to send the response");
                return InvocationResponse.ABORT;
            }
            if (isPushPromise(msgCtx)) {
                pushPromiseToClient(msgCtx, clientRequest);
            } else if (isServerPush(msgCtx)) {
                pushResponseToClient(msgCtx, clientRequest);
            } else {
                try {
                    sendResponseToClient(msgCtx, clientRequest);
                    if (msgCtx.getOperationContext() != null) {
                        msgCtx.getOperationContext().setProperty(Constants.RESPONSE_WRITTEN, Constants.VALUE_TRUE);
                    }
                } catch (IOException e) {
                    handleException("Error occurred while sending a response to the client", e);
                }
            }
        }

        return InvocationResponse.CONTINUE;
    }

    private boolean isRequestToBackend(EndpointReference destinationEPR) throws AxisFault {

        if (destinationEPR != null) {
            if (destinationEPR.hasNoneAddress()) {
                handleException("Cannot send the message to " + AddressingConstants.Final.WSA_NONE_URI);
            }
            return true;
        }
        return false;
    }

    /**
     * Checks whether the message is a response or server push response.
     *
     * @param msgCtx axis2 message context
     * @return boolean
     */
    private boolean isServerPush(MessageContext msgCtx) {

        return msgCtx.isPropertyTrue(BridgeConstants.SERVER_PUSH);
    }

    /**
     * Checks whether the message is a response or server push promise.
     *
     * @param msgCtx axis2 message context
     * @return boolean
     */
    private boolean isPushPromise(MessageContext msgCtx) {

        return msgCtx.isPropertyTrue(BridgeConstants.IS_PUSH_PROMISE);
    }

    /**
     * Submits a response back to the client.
     *
     * @param msgCtx        axis2 message context
     * @param clientRequest original HTTP carbon request
     * @throws AxisFault if something goes wrong when creating the outbound response or responding back to the client
     */
    private void sendResponseToClient(MessageContext msgCtx, HttpCarbonMessage clientRequest) throws AxisFault {

        HttpCarbonMessage outboundResponseMsg = SourceResponseHandler.createOutboundResponseMsg(msgCtx, clientRequest);
        SourceResponseHandler.sendResponse(msgCtx, clientRequest, outboundResponseMsg);
    }

    /**
     * Send the server push promise to client.
     *
     * @param msgCtx        axis2 message context
     * @param clientRequest HttpCarbonMessage
     * @throws AxisFault throws if error occurred while sending server pushes
     */
    private void pushPromiseToClient(MessageContext msgCtx, HttpCarbonMessage clientRequest) throws AxisFault {

        Http2PushPromise http2PushPromise = SourceResponseHandler.getPushPromise(msgCtx);
        SourceResponseHandler.pushPromise(http2PushPromise, clientRequest);
    }

    /**
     * Send the server push response to client.
     *
     * @param msgCtx        axis2 message context
     * @param clientRequest HttpCarbonMessage
     * @throws AxisFault throws if error occurred while sending server pushes
     */
    private void pushResponseToClient(MessageContext msgCtx, HttpCarbonMessage clientRequest) throws AxisFault {

        Http2PushPromise http2PushPromise = SourceResponseHandler.getPushPromise(msgCtx);
        HttpCarbonMessage outboundPushMsg = SourceResponseHandler.createOutboundResponseMsg(msgCtx, clientRequest);
        SourceResponseHandler.pushResponse(msgCtx, http2PushPromise, outboundPushMsg, clientRequest);
    }

    /**
     * Sends an outbound request to the backend service.
     *
     * @param msgCtx axis2 message context
     * @param url    request URL of the backend service
     * @throws IOException if something goes wrong when sending the outbound request to the backend service
     */
    private void sendRequestToBackendService(MessageContext msgCtx, URL url) throws IOException {

        HttpCarbonMessage outboundRequestMsg = TargetRequestHandler.createOutboundRequestMsg(url, msgCtx,
                targetConfiguration);
        HttpClientConnector clientConnector = TargetRequestHandler.createHttpClient(url, msgCtx,
                httpWsConnectorFactory, connectionManager, bootstrapConfiguration, targetConfiguration);
        TargetRequestHandler.sendRequest(clientConnector, outboundRequestMsg, msgCtx, targetConfiguration);
    }

    @Override
    public void cleanup(MessageContext messageContext) {

    }

    @Override
    public void stop() {

    }

    public void handleException(String s, Exception e) throws AxisFault {

        LOG.error(s, e);
        throw new AxisFault(s, e);
    }

    public void handleException(String msg) throws AxisFault {

        LOG.error(msg);
        throw new AxisFault(msg);
    }
}
