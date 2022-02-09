/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.synapse.transport.netty.api;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.TransportInDescription;
import org.apache.axis2.transport.TransportListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.transport.netty.BridgeConstants;
import org.apache.synapse.transport.netty.api.config.HttpWebSocketInboundEndpointConfiguration;
import org.apache.synapse.transport.netty.api.config.SSLConfiguration;
import org.apache.synapse.transport.netty.listener.Axis2HttpSSLTransportListener;
import org.apache.synapse.transport.netty.listener.Axis2HttpTransportListener;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Objects;

/**
 * API class to access Pass-Through (HTTP) and WebSocket Core Inbound Endpoint management classes.
 */
public class HttpWebSocketInboundEndpointHandler {

    private static final Log LOG = LogFactory.getLog(HttpWebSocketInboundEndpointHandler.class);

    private static HashMap<Integer, TransportListener> transportListenerMap = new HashMap<>();

    /**
     * Default value for verification timeout to validate whether port is closed successfully.
     */
    private static final int DEFAULT_PORT_CLOSE_VERIFY_TIMEOUT = 10;

    /**
     * Start Endpoint Listener.
     *
     * @param configurationContext configuration context
     * @param httpWebSocketConfig  configuration that represents http and websocket inbound endpoint
     * @return whether the Endpoint started successfully or not
     */
    public static boolean startListener(ConfigurationContext configurationContext,
                                        HttpWebSocketInboundEndpointConfiguration httpWebSocketConfig) {

        Axis2HttpTransportListener transportListener = new Axis2HttpTransportListener();

        return startServer(transportListener, BridgeConstants.TRANSPORT_HTTPWS, httpWebSocketConfig,
                configurationContext);
    }

    /**
     * Start SSL Endpoint Listener.
     *
     * @param configurationContext configuration context
     * @param httpWebSocketConfig  configuration that represents http and websocket inbound endpoint
     * @return whether the Endpoint started successfully or not
     */
    public static boolean startSSLListener(ConfigurationContext configurationContext,
                                           HttpWebSocketInboundEndpointConfiguration httpWebSocketConfig) {

        Axis2HttpTransportListener transportListener = new Axis2HttpSSLTransportListener();

        return startServer(transportListener, BridgeConstants.TRANSPORT_HTTPSWSS, httpWebSocketConfig,
                configurationContext);
    }

    private static boolean startServer(Axis2HttpTransportListener transportListener, String transportName,
                                       HttpWebSocketInboundEndpointConfiguration httpWebSocketConfig,
                                       ConfigurationContext configurationContext) {

        TransportInDescription transportInDescription;
        try {
            // Generate the TransportInDescription and add it to the ConfigurationContext
            transportInDescription = createTransportInDescription(transportName, transportListener,
                    httpWebSocketConfig);
        } catch (AxisFault e) {
            logStartupError(e, "Error occurred while generating TransportInDescription. Hence, ",
                    httpWebSocketConfig, transportName);
            return false;
        }

        try {
            // Initialize the Axis2HttpTransportListener and set MessagingHandlers
            transportListener.init(configurationContext, transportInDescription);
            transportListener.setMessagingHandlers(httpWebSocketConfig.getInboundEndpointHandlers());
        } catch (AxisFault e) {
            logStartupError(e, "Error occurred while initializing the " + transportName
                    + " transport listener. Hence, ", httpWebSocketConfig, transportName);
            return false;
        }

        try {
            transportListener.start();
            // If the transport it started successfully, store it in the transportListenerMap against the port. This
            // map will be used when stopping the listener.
            transportListenerMap.put(httpWebSocketConfig.getPort(), transportListener);
        } catch (AxisFault e) {
            logStartupError(e, "Error occurred while generating TransportInDescription. Hence, ",
                    httpWebSocketConfig, transportName);
            return false;
        }
        return true;
    }

    private static void logStartupError(AxisFault e, String msg,
                                        HttpWebSocketInboundEndpointConfiguration httpWebSocketConfig,
                                        String transportName) {

        LOG.error(msg + " Could not start the " + transportName + " transport listener for endpoint : "
                + httpWebSocketConfig.getEndpointName() + " on port " + httpWebSocketConfig.getPort(), e);
    }

    private static TransportInDescription createTransportInDescription(
            String transportName, TransportListener transportListener,
            HttpWebSocketInboundEndpointConfiguration httpWebSocketConfig)
            throws AxisFault {

        TransportInDescription transportInDescription = new TransportInDescription(transportName);
        transportInDescription.setReceiver(transportListener);

        // populate parameters
        addParameter(transportInDescription, BridgeConstants.PORT_PARAM, httpWebSocketConfig.getPort());
        addParameter(transportInDescription, BridgeConstants.HOSTNAME_PARAM, httpWebSocketConfig.getHostname());
        addParameter(transportInDescription, BridgeConstants.HTTP_PROTOCOL_VERSION_PARAM,
                httpWebSocketConfig.getHttpProtocolVersion());

        SSLConfiguration sslConfiguration = httpWebSocketConfig.getSslConfiguration();
        // SSLConfiguration will be null for non-secured transport. Hence, need to do the null check here.
        if (Objects.nonNull(sslConfiguration)) {
            populateSSLParameters(sslConfiguration, transportInDescription);
        }

        return transportInDescription;
    }

    private static void populateSSLParameters(SSLConfiguration sslConfig, TransportInDescription transportIn)
            throws AxisFault {

        addParameter(transportIn, BridgeConstants.KEY_STORE, sslConfig.getKeyStoreElement());
        addParameter(transportIn, BridgeConstants.TRUST_STORE, sslConfig.getTrustStoreElement());
        addParameter(transportIn, BridgeConstants.SSL_VERIFY_CLIENT, sslConfig.getClientAuthElement());
        addParameter(transportIn, BridgeConstants.HTTPS_PROTOCOL, sslConfig.getHttpsProtocolElement());
        addParameter(transportIn, BridgeConstants.SSL_PROTOCOL, sslConfig.getSslProtocolElement());
        addParameter(transportIn, BridgeConstants.CLIENT_REVOCATION, sslConfig.getRevocationVerifierElement());
        addParameter(transportIn, BridgeConstants.PREFERRED_CIPHERS, sslConfig.getPreferredCiphersElement());
        addParameter(transportIn, BridgeConstants.SSL_SESSION_TIMEOUT, sslConfig.getSessionTimeoutElement());
        addParameter(transportIn, BridgeConstants.SSL_HANDSHAKE_TIMEOUT, sslConfig.getHandshakeTimeoutElement());
    }

    private static void addParameter(TransportInDescription transportInDescription, String name,
                                     OMElement parameterElement)
            throws AxisFault {

        Parameter parameter = new Parameter();
        parameter.setName(name);
        parameter.setParameterElement(parameterElement);
        transportInDescription.addParameter(parameter);
    }

    private static void addParameter(TransportInDescription transportInDescription, String name, Object value)
            throws AxisFault {

        Parameter parameter = new Parameter();
        parameter.setName(name);
        parameter.setValue(value);
        transportInDescription.addParameter(parameter);
    }

    /**
     * Close ListeningEndpoint running on the given port.
     *
     * @param port Port of  ListeningEndpoint to be closed
     * @return IS successfully closed
     */
    public static boolean closeEndpoint(int port) {

        LOG.info("Closing Endpoint Listener for port " + port);
        TransportListener listener = transportListenerMap.get(port);
        if (Objects.nonNull(listener)) {
            try {
                listener.stop();
            } catch (AxisFault e) {
                LOG.error("Cannot close Endpoint relevant to port " + port, e);
                return false;
            } finally {
                int portCloseVerifyTimeout = DEFAULT_PORT_CLOSE_VERIFY_TIMEOUT;
                if (isPortCloseSuccess(port, portCloseVerifyTimeout)) {
                    LOG.info("Successfully closed Endpoint Listener for port " + port);
                } else {
                    LOG.warn("Port close verify timeout " + portCloseVerifyTimeout + "s exceeded. "
                            + "Endpoint Listener for port " + port + " still bound to the ListenerEndpoint.");
                }
            }
        }
        return true;
    }

    private static boolean isPortCloseSuccess(int port, int portCloseVerifyTimeout) {

        boolean portCloseSuccess = false;

        for (int i = 0; i < portCloseVerifyTimeout; i++) {
            try {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Verify port [" + port + "] close status. Attempt: " + i);
                }

                ServerSocket ss = new ServerSocket(port);
                ss.close();
                ss = null;
                portCloseSuccess = true;
                break;
            } catch (IOException e) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("The port " + port + " is not closed yet, verify again after waiting 1s", e);
                }

                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException ex) {
                    //ignore
                }
            }
        }

        return portCloseSuccess;
    }

}
