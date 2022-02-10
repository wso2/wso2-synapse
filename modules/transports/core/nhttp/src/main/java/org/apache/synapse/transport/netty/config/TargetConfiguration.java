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
package org.apache.synapse.transport.netty.config;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.synapse.commons.handlers.MessagingHandler;
import org.apache.synapse.transport.netty.sender.ClientSSLConfigurationBuilder;
import org.apache.synapse.transport.netty.util.RequestResponseUtils;

import java.util.List;

/**
 * This class stores the configurations specific to the Sender.
 */
public class TargetConfiguration extends BaseConfiguration {

    private TransportOutDescription transportOutDescription;

    private List<MessagingHandler> messagingHandlers;

    private boolean httpTraceLogEnabled = false;

    private boolean isRequestLimitsValidationEnabled;
    private int clientRequestMaxStatusLineLength;
    private int clientRequestMaxHeaderSize;
    private int clientRequestMaxEntityBodySize;
    private int socketTimeout;

    private ClientSSLConfigurationBuilder clientSSLConfigurationBuilder;

    public TargetConfiguration(ConfigurationContext configurationContext,
                               TransportOutDescription transportOutDescription) {

        super(configurationContext);
        this.transportOutDescription = transportOutDescription;
    }

    public void build() throws AxisFault {

        super.build();
        preserveUserAgentHeader = conf.isPreserveUserAgentHeader();
        preserveServerHeader = conf.isPreserveServerHeader();
        populatePreserveHTTPHeaders(conf.getPreserveHttpHeaders());

        if (RequestResponseUtils.isHTTPTraceLoggerEnabled()) {
            httpTraceLogEnabled = true;
        }

        // Set Request validation limits.
        isRequestLimitsValidationEnabled = conf.isClientRequestLimitsValidationEnabled();
        if (isRequestLimitsValidationEnabled) {
            clientRequestMaxStatusLineLength = conf.getClientRequestMaxStatusLineLength();
            clientRequestMaxHeaderSize = conf.getClientRequestMaxHeaderSize();
            clientRequestMaxEntityBodySize = conf.getClientRequestMaxEntityBodySize();
        }

        socketTimeout = NettyConfiguration.getInstance().getClientEndpointSocketTimeout();
        if (socketTimeout < 0) {
            // When the socketTimeout is 0, transport-http will use 5 * 60000ms as the default value for socketTimeout
            socketTimeout = 0;
        }
    }

    public TransportOutDescription getTransportOutDescription() {

        return transportOutDescription;
    }

    public void setTransportOutDescription(TransportOutDescription transportOutDescription) {

        this.transportOutDescription = transportOutDescription;
    }

    public boolean isHttpTraceLogEnabled() {

        return httpTraceLogEnabled;
    }

    public boolean isRequestLimitsValidationEnabled() {

        return isRequestLimitsValidationEnabled;
    }

    public void setRequestLimitsValidationEnabled(boolean requestLimitsValidationEnabled) {

        isRequestLimitsValidationEnabled = requestLimitsValidationEnabled;
    }

    public int getClientRequestMaxStatusLineLength() {

        return clientRequestMaxStatusLineLength;
    }

    public int getClientRequestMaxHeaderSize() {

        return clientRequestMaxHeaderSize;
    }

    public int getClientRequestMaxEntityBodySize() {

        return clientRequestMaxEntityBodySize;
    }

    public int getSocketTimeout() {

        return socketTimeout;
    }

    public ClientSSLConfigurationBuilder getClientSSLConfigurationBuilder() {

        return clientSSLConfigurationBuilder;
    }

    public void setClientSSLConfigurationBuilder(ClientSSLConfigurationBuilder clientSSLConfigurationBuilder) {

        this.clientSSLConfigurationBuilder = clientSSLConfigurationBuilder;
    }

    public List<MessagingHandler> getMessagingHandlers() {

        return messagingHandlers;
    }

    public void setMessagingHandlers(List<MessagingHandler> messagingHandlers) {

        this.messagingHandlers = messagingHandlers;
    }
}
