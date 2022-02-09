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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.transport.netty.BridgeConstants;
import org.apache.synapse.transport.util.ConfigurationBuilderUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * This class encapsulates netty transport tuning configurations specified via a
 * configurations file or system properties.
 */
public class NettyConfiguration {

    private static final Log LOG = LogFactory.getLog(NettyConfiguration.class);

    /**
     * Default tuning parameter values.
     */
    public static final int DEFAULT_WORKER_POOL_SIZE_CORE = 400;
    public static final int DEFAULT_WORKER_POOL_SIZE_MAX = 400;
    public static final int DEFAULT_WORKER_THREAD_KEEPALIVE_SEC = 60;
    public static final int DEFAULT_WORKER_POOL_QUEUE_LENGTH = -1;
    public static final int DEFAULT_HTTP_SOCKET_TIMEOUT = 180000;
    public static final int DEFAULT_CONNECTION_POOLING_MAX_IDLE_CONNECTIONS = 100;
    public static final int DEFAULT_CONNECTION_POOLING_MAX_ACTIVE_CONNECTIONS = -1;
    public static final int DEFAULT_CONNECTION_POOLING_WAIT_TIME = 30;
    public static final int DEFAULT_CLIENT_ENDPOINT_SOCKET_TIMEOUT = 60;
    public static final int DEFAULT_MAX_STATUS_LINE_LENGTH = -1;
    public static final int DEFAULT_MAX_HEADER_SIZE = -1;
    public static final int DEFAULT_MAX_ENTITY_BODY_SIZE = -1;
    public static final int DEFAULT_MAX_CLIENT_REQUEST_STATUS_LINE_LENGTH = -1;
    public static final int DEFAULT_MAX_CLIENT_REQUEST_HEADER_SIZE = -1;
    public static final int DEFAULT_MAX_CLIENT_REQUEST_ENTITY_BODY_SIZE = -1;
    public static final String HTTP_WORKER_THREAD_GROUP_NAME = "HTTP Worker Thread Group";
    public static final String HTTP_WORKER_THREAD_ID = "HTTPWorker";
    public static final String REVERSE_PROXY_MODE_SYSTEM_PROPERTY = "reverseProxyMode";

    private Boolean isKeepAliveDisabled = null;

    private Boolean reverseProxyMode = null;

    private Properties props;

    private static final NettyConfiguration instance = new NettyConfiguration();

    private NettyConfiguration() {

        try {
            props = loadNettyProperties();
        } catch (Exception ignored) {
            // ignore
        }
    }

    public static NettyConfiguration getInstance() {

        return instance;
    }

    public int getWorkerPoolCoreSize() {

        return ConfigurationBuilderUtil.getIntProperty(NettyConfigPropertyNames.WORKER_POOL_SIZE_CORE,
                DEFAULT_WORKER_POOL_SIZE_CORE, props);
    }

    public int getWorkerPoolMaxSize() {

        return ConfigurationBuilderUtil.getIntProperty(NettyConfigPropertyNames.WORKER_POOL_SIZE_MAX,
                DEFAULT_WORKER_POOL_SIZE_MAX, props);
    }

    public int getWorkerThreadKeepaliveSec() {

        return ConfigurationBuilderUtil.getIntProperty(NettyConfigPropertyNames.WORKER_THREAD_KEEP_ALIVE_SEC,
                DEFAULT_WORKER_THREAD_KEEPALIVE_SEC, props);
    }

    public int getWorkerPoolQueueLen() {

        return ConfigurationBuilderUtil.getIntProperty(NettyConfigPropertyNames.WORKER_POOL_QUEUE_LENGTH,
                DEFAULT_WORKER_POOL_QUEUE_LENGTH, props);
    }

    public boolean isRequestLimitsValidationEnabled() {

        return ConfigurationBuilderUtil.getBooleanProperty(NettyConfigPropertyNames.REQUEST_LIMIT_VALIDATION,
                false, props);
    }

    public int getMaxStatusLineLength() {

        return ConfigurationBuilderUtil.getIntProperty(NettyConfigPropertyNames.MAX_STATUS_LINE_LENGTH,
                DEFAULT_MAX_STATUS_LINE_LENGTH, props);
    }

    public int getMaxHeaderSize() {

        return ConfigurationBuilderUtil.getIntProperty(NettyConfigPropertyNames.MAX_HEADER_SIZE,
                DEFAULT_MAX_HEADER_SIZE, props);
    }

    public int getMaxEntityBodySize() {

        return ConfigurationBuilderUtil.getIntProperty(NettyConfigPropertyNames.MAX_ENTITY_BODY_SIZE,
                DEFAULT_MAX_ENTITY_BODY_SIZE, props);
    }

    public boolean isClientRequestLimitsValidationEnabled() {

        return ConfigurationBuilderUtil.getBooleanProperty(NettyConfigPropertyNames.CLIENT_REQUEST_LIMIT_VALIDATION,
                false, props);
    }

    public int getClientRequestMaxStatusLineLength() {

        return ConfigurationBuilderUtil.getIntProperty(NettyConfigPropertyNames.MAX_CLIENT_REQUEST_STATUS_LINE_LENGTH,
                DEFAULT_MAX_CLIENT_REQUEST_STATUS_LINE_LENGTH, props);
    }

    public int getClientRequestMaxHeaderSize() {

        return ConfigurationBuilderUtil.getIntProperty(NettyConfigPropertyNames.MAX_CLIENT_REQUEST_HEADER_SIZE,
                DEFAULT_MAX_CLIENT_REQUEST_HEADER_SIZE, props);
    }

    public int getClientRequestMaxEntityBodySize() {

        return ConfigurationBuilderUtil.getIntProperty(NettyConfigPropertyNames.MAX_CLIENT_REQUEST_ENTITY_BODY_SIZE,
                DEFAULT_MAX_CLIENT_REQUEST_ENTITY_BODY_SIZE, props);
    }

    public int getSocketTimeout() {

        return ConfigurationBuilderUtil.getIntProperty(NettyConfigPropertyNames.HTTP_SOCKET_TIMEOUT,
                DEFAULT_HTTP_SOCKET_TIMEOUT, props);
    }

    public boolean isCustomConnectionPoolConfigsEnabled() {

        return ConfigurationBuilderUtil.getBooleanProperty(
                NettyConfigPropertyNames.ENABLE_CUSTOM_CONNECTION_POOL_CONFIG, false, props);
    }

    public int getConnectionPoolingMaxActiveConnections() {

        return ConfigurationBuilderUtil.getIntProperty(
                NettyConfigPropertyNames.CONNECTION_POOLING_MAX_ACTIVE_CONNECTIONS,
                DEFAULT_CONNECTION_POOLING_MAX_ACTIVE_CONNECTIONS, props);
    }

    public int getConnectionPoolingMaxIdleConnections() {

        return ConfigurationBuilderUtil.getIntProperty(
                NettyConfigPropertyNames.CONNECTION_POOLING_MAX_IDLE_CONNECTIONS,
                DEFAULT_CONNECTION_POOLING_MAX_IDLE_CONNECTIONS, props);
    }

    public int getConnectionPoolingWaitTime() {

        return ConfigurationBuilderUtil.getIntProperty(NettyConfigPropertyNames.CONNECTION_POOLING_WAIT_TIME,
                DEFAULT_CONNECTION_POOLING_WAIT_TIME, props);
    }

    public int getClientEndpointSocketTimeout() {

        return ConfigurationBuilderUtil.getIntProperty(NettyConfigPropertyNames.CLIENT_ENDPOINT_SOCKET_TIMEOUT,
                DEFAULT_CLIENT_ENDPOINT_SOCKET_TIMEOUT, props);
    }

    public boolean isKeepAliveDisabled() {

        if (isKeepAliveDisabled == null) {
            isKeepAliveDisabled = ConfigurationBuilderUtil
                    .getBooleanProperty(NettyConfigPropertyNames.DISABLE_KEEPALIVE, false, props);
        }
        return isKeepAliveDisabled;
    }

    public String getServerHostname() {

        return ConfigurationBuilderUtil
                .getStringProperty(NettyConfigPropertyNames.HTTP_SERVER_HOSTNAME, null, props);

    }

    public String getHttpGetRequestProcessorClass() {

        return ConfigurationBuilderUtil
                .getStringProperty(NettyConfigPropertyNames.HTTP_GET_REQUEST_PROCESSOR, "", props);

    }

    public String getHttpTransportMediationInterceptorClass() {

        return ConfigurationBuilderUtil
                .getStringProperty(NettyConfigPropertyNames.HTTP_TRANSPORT_MEDIATION_INTERCEPTOR, "", props);

    }

    public boolean isPreserveUserAgentHeader() {

        return ConfigurationBuilderUtil.getBooleanProperty(NettyConfigPropertyNames.USER_AGENT_HEADER_PRESERVE,
                false, props);
    }

    public boolean isPreserveServerHeader() {

        return ConfigurationBuilderUtil.getBooleanProperty(NettyConfigPropertyNames.SERVER_HEADER_PRESERVE,
                false, props);
    }

    public String getPreserveHttpHeaders() {

        return ConfigurationBuilderUtil.getStringProperty(NettyConfigPropertyNames.HTTP_HEADERS_PRESERVE,
                "", props);
    }

    public String isServiceListBlocked() {

        return ConfigurationBuilderUtil.getStringProperty(NettyConfigPropertyNames.HTTP_BLOCK_SERVICE_LIST,
                "false", props);
    }

    public String getResponsePreserveHttpHeaders() {

        return ConfigurationBuilderUtil.getStringProperty(NettyConfigPropertyNames.HTTP_RESPONSE_HEADERS_PRESERVE,
                "", props);
    }

    /**
     * Check for reverse proxy mode.
     *
     * @return whether reverse proxy mode is enabled
     */
    public boolean isReverseProxyMode() {
        if (reverseProxyMode == null) {
            reverseProxyMode = Boolean.parseBoolean(System.getProperty(REVERSE_PROXY_MODE_SYSTEM_PROPERTY));
        }
        return reverseProxyMode;
    }

    public boolean isForcedMessageBuildEnabled() {

        return ConfigurationBuilderUtil.getBooleanProperty(NettyConfigPropertyNames.FORCE_MESSAGE_BUILDER,
                true, props);
    }

    public boolean isForcedXmlMessageValidationEnabled() {

        return ConfigurationBuilderUtil.getBooleanProperty(NettyConfigPropertyNames.FORCE_XML_MESSAGE_VALIDATION,
                false, props);
    }

    public boolean isForcedJSONMessageValidationEnabled() {

        return ConfigurationBuilderUtil.getBooleanProperty(NettyConfigPropertyNames.FORCE_JSON_MESSAGE_VALIDATION,
                false, props);
    }

    /**
     * Load the properties from the netty.properties file.
     *
     * @return Properties loaded from netty.properties file
     */
    private static Properties loadNettyProperties() {

        String filePath = "netty.properties";

        Properties properties = new Properties();
        ClassLoader cl = Thread.currentThread().getContextClassLoader();

        if (LOG.isDebugEnabled()) {
            LOG.debug("Loading the file '" + filePath + "' from classpath");
        }

        InputStream in = null;

        //if we reach to this assume that the we may have to looking to the customer provided external location for the
        //given properties
        if (System.getProperty(BridgeConstants.CONF_LOCATION) != null) {
            try {
                in = new FileInputStream(System.getProperty(BridgeConstants.CONF_LOCATION)
                        + File.separator + filePath);
            } catch (FileNotFoundException e) {
                String msg = "Error loading properties from a file at from the System defined location: " + filePath;
                LOG.warn(msg);
            }
        }

        if (in == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Unable to load file  '" + filePath + "'");
            }

            filePath = "conf" + File.separatorChar + filePath;
            if (LOG.isDebugEnabled()) {
                LOG.debug("Loading the file '" + filePath + "'");
            }

            in = cl.getResourceAsStream(filePath);
            if (in == null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Unable to load file  '" + filePath + "'");
                }
            }
        }
        if (in != null) {
            try {
                properties.load(in);
            } catch (IOException e) {
                String msg = "Error loading properties from a file at : " + filePath;
                LOG.error(msg, e);
            }
        }
        return properties;
    }

}
