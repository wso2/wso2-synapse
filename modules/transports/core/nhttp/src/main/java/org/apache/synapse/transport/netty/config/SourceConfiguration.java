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
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.TransportInDescription;
import org.apache.axis2.transport.base.ParamUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.commons.handlers.MessagingHandler;
import org.apache.synapse.transport.http.conn.Scheme;
import org.apache.synapse.transport.netty.BridgeConstants;

import java.net.UnknownHostException;
import java.util.List;
import java.util.Objects;

/**
 * This class stores the configurations specific to the Listener.
 */
public class SourceConfiguration extends BaseConfiguration {

    private static final Log LOG = LogFactory.getLog(SourceConfiguration.class);

    private int port;
    private String host;
    private Scheme scheme;
    private String protocol = "2.0";

    private final TransportInDescription inDescription;

    /**
     * WSDL processor for GET requests.
     */
    private HttpGetRequestProcessor httpGetRequestProcessor = null;

    /**
     * The list of handlers to handle the inbound HTTP requests.
     */
    private List<MessagingHandler> messagingHandlers;

    public SourceConfiguration(ConfigurationContext configurationContext, TransportInDescription inDescription,
                               Scheme scheme, List<MessagingHandler> messagingHandler) {

        super(configurationContext);
        this.inDescription = inDescription;
        this.scheme = scheme;
        this.messagingHandlers = messagingHandlers;
    }

    public void build() throws AxisFault {

        super.build();

        populatePort();

        populateHostname();

        populateHTTPProtocol();

        populateHTTPGETRequestProcessor();

        populatePreserveHTTPHeaders(conf.getResponsePreserveHttpHeaders());
    }

    private void populatePort() throws AxisFault {
        port = ParamUtils.getRequiredParamInt(inDescription, BridgeConstants.PORT_PARAM);
        if (port == 0) {
            throw new AxisFault("Listener port is not defined!");
        }
    }

    private void populateHostname() {
        Parameter hostParameter = inDescription.getParameter(BridgeConstants.HOSTNAME_PARAM);
        if (Objects.nonNull(hostParameter) && Objects.nonNull(hostParameter.getValue())) {
            host = ((String) hostParameter.getValue()).trim();
        } else if (conf.getServerHostname() != null) {
            host = conf.getServerHostname().trim();
        } else {
            try {
                host = java.net.InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                LOG.warn("Unable to lookup local host name. Hence, using 'localhost'");
                host = BridgeConstants.HTTP_DEFAULT_HOST;
            }
        }
    }

    private void populateHTTPProtocol() {
        Parameter protocolParameter = inDescription.getParameter(BridgeConstants.HTTP_PROTOCOL_VERSION_PARAM);
        if (Objects.nonNull(protocolParameter) && Objects.nonNull(protocolParameter.getValue())) {
            String protocol = ((String) protocolParameter.getValue()).trim();
            if (!protocol.isEmpty()) {
                this.protocol = protocol;
            }
        }
    }

    private void populateHTTPGETRequestProcessor() throws AxisFault {
        String httpGetRequestProcessorClass = conf.getHttpGetRequestProcessorClass();
        httpGetRequestProcessor = createHttpGetProcessor(httpGetRequestProcessorClass);
        if (httpGetRequestProcessor == null) {
            handleException("Cannot create HttpGetRequestProcessor");
        }
    }

    private HttpGetRequestProcessor createHttpGetProcessor(String clss) throws AxisFault {

        Object obj = null;
        try {
            obj = Class.forName(clss).newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            handleException("Error occurred while creating HTTPGETRequestProcessor", e);
        }

        if (obj instanceof HttpGetRequestProcessor) {
            return (HttpGetRequestProcessor) obj;
        } else {
            handleException("Error occurred while creating HTTPGETRequestProcessor. The provided class should be" +
                    "an implementation of interface org.apache.synapse.transport.netty.config.HttpGetRequestProcessor");
        }
        return null;
    }

    private void handleException(String msg, Exception e) throws AxisFault {

        LOG.error(msg, e);
        throw new AxisFault(msg, e);
    }

    private void handleException(String msg) throws AxisFault {

        LOG.error(msg);
        throw new AxisFault(msg);
    }

    public int getPort() {

        return port;
    }

    public void setPort(int port) {

        this.port = port;
    }

    public String getHost() {

        return host;
    }

    public void setHost(String host) {

        this.host = host;
    }

    public Scheme getScheme() {

        return scheme;
    }

    public void setScheme(Scheme scheme) {

        this.scheme = scheme;
    }

    public HttpGetRequestProcessor getHttpGetRequestProcessor() {

        return httpGetRequestProcessor;
    }

    public List<MessagingHandler> getMessagingHandlers() {

        return messagingHandlers;
    }

    public void setMessagingHandlers(List<MessagingHandler> messagingHandlers) {

        this.messagingHandlers = messagingHandlers;
    }

    public String getProtocol() {

        return protocol;
    }

    public void setProtocol(String protocol) {

        this.protocol = protocol;
    }

    public TransportInDescription getInDescription() {

        return inDescription;
    }
}
