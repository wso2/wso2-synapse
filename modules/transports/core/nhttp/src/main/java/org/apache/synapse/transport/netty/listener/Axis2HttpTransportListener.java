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
package org.apache.synapse.transport.netty.listener;

import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.SessionContext;
import org.apache.axis2.description.TransportInDescription;
import org.apache.axis2.transport.TransportListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.commons.handlers.MessagingHandler;
import org.apache.synapse.transport.http.conn.Scheme;
import org.apache.synapse.transport.netty.config.SourceConfiguration;
import org.apache.synapse.transport.netty.util.RequestResponseUtils;
import org.wso2.transport.http.netty.contract.HttpWsConnectorFactory;
import org.wso2.transport.http.netty.contract.ServerConnector;
import org.wso2.transport.http.netty.contract.ServerConnectorFuture;
import org.wso2.transport.http.netty.contract.config.ListenerConfiguration;
import org.wso2.transport.http.netty.contract.config.ServerBootstrapConfiguration;
import org.wso2.transport.http.netty.contractimpl.DefaultHttpWsConnectorFactory;

import java.util.HashMap;
import java.util.List;

/**
 * {@code Axis2HttpTransportListener} is the Axis2 Transport Listener implementation for HTTP transport based on Netty.
 */
public class Axis2HttpTransportListener implements TransportListener {

    private static final Log LOG = LogFactory.getLog(Axis2HttpTransportListener.class);

    private ServerConnector serverConnector;
    private HttpWsConnectorFactory httpWsConnectorFactory;
    protected SourceConfiguration sourceConfiguration = null;
    protected List<MessagingHandler> messagingHandlers;
    protected TransportInDescription transportInDescription;

    @Override
    public void init(ConfigurationContext configurationContext, TransportInDescription transportInDescription)
            throws AxisFault {

        Scheme scheme = initScheme();
        this.transportInDescription = transportInDescription;

        // build source configuration
        sourceConfiguration = new SourceConfiguration(configurationContext, transportInDescription, scheme,
                messagingHandlers);
        sourceConfiguration.build();

        sourceConfiguration.getHttpGetRequestProcessor().init(sourceConfiguration.getConfigurationContext());

        ListenerConfiguration listenerConfiguration = initListenerConfiguration();

        httpWsConnectorFactory = new DefaultHttpWsConnectorFactory();
        this.serverConnector = httpWsConnectorFactory
                .createServerConnector(new ServerBootstrapConfiguration(new HashMap<>()), listenerConfiguration);
    }

    @Override
    public void start() throws AxisFault {
        ServerConnectorFuture serverConnectorFuture = serverConnector.start();
        serverConnectorFuture.setHttpConnectorListener(new Axis2HttpConnectorListener(sourceConfiguration));
        try {
            serverConnectorFuture.sync();
        } catch (InterruptedException e) {
            LOG.warn("Interrupted while waiting for server connector to start", e);
        }
    }

    @Override
    public void stop() throws AxisFault {
        LOG.info("Stopping " + transportInDescription.getName() + " Listener..");
        serverConnector.stop();
        try {
            httpWsConnectorFactory.shutdown();
        } catch (InterruptedException e) {
            LOG.error("Error occurred while shutting down the listener..", e);
        }
    }

    @Override
    public EndpointReference getEPRForService(String s, String s1) {
        return null;
    }

    @Override
    public EndpointReference[] getEPRsForService(String s, String s1) {
        return new EndpointReference[0];
    }

    @Override
    public SessionContext getSessionContext(MessageContext messageContext) {
        return null;
    }

    @Override
    public void destroy() {
        LOG.info("Destroying " + transportInDescription.getName() + " Listener..");
    }

    protected Scheme initScheme() {
        return new Scheme("http", 80, false);
    }

    protected ListenerConfiguration initListenerConfiguration()
            throws AxisFault {
        return RequestResponseUtils.getListenerConfig(sourceConfiguration, false);
    }

    public void setMessagingHandlers(List<MessagingHandler> messagingHandlers) {

        this.messagingHandlers = messagingHandlers;
    }
}
