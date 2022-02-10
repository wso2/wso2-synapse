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

package org.apache.synapse.transport.netty.api.config;

import org.apache.synapse.commons.handlers.MessagingHandler;

import java.util.List;

/**
 * {@code HttpWebSocketInboundEndpointConfiguration} encapsulates the configurations that are passed from the
 * inbound endpoints to be used when initializing and starting the listener.
 */
public class HttpWebSocketInboundEndpointConfiguration {

    private int port;
    private String hostname;
    private String endpointName;
    private String httpProtocolVersion;
    private SSLConfiguration sslConfiguration;
    private List<MessagingHandler> inboundEndpointHandlers;

    public HttpWebSocketInboundEndpointConfiguration(int port, String hostname, String endpointName,
                                                     String httpProtocolVersion,
                                                     List<MessagingHandler> inboundEndpointHandlers) {

        this.port = port;
        this.hostname = hostname;
        this.endpointName = endpointName;
        this.httpProtocolVersion = httpProtocolVersion;
        this.inboundEndpointHandlers = inboundEndpointHandlers;
    }

    public SSLConfiguration getSslConfiguration() {

        return sslConfiguration;
    }

    public void setSslConfiguration(SSLConfiguration sslConfiguration) {

        this.sslConfiguration = sslConfiguration;
    }

    public List<MessagingHandler> getInboundEndpointHandlers() {

        return inboundEndpointHandlers;
    }

    public void setInboundEndpointHandlers(List<MessagingHandler> inboundEndpointHandlers) {

        this.inboundEndpointHandlers = inboundEndpointHandlers;
    }

    public String getEndpointName() {

        return endpointName;
    }

    public void setEndpointName(String endpointName) {

        this.endpointName = endpointName;
    }

    public int getPort() {

        return port;
    }

    public void setPort(int port) {

        this.port = port;
    }

    public String getHostname() {

        return hostname;
    }

    public void setHostname(String hostname) {

        this.hostname = hostname;
    }

    public String getHttpProtocolVersion() {

        return httpProtocolVersion;
    }

    public void setHttpProtocolVersion(String httpProtocolVersion) {

        this.httpProtocolVersion = httpProtocolVersion;
    }
}
