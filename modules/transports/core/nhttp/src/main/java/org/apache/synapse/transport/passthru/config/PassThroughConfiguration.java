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

package org.apache.synapse.transport.passthru.config;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.params.HttpConnectionParams;
import org.apache.synapse.transport.passthru.PassThroughConstants;
import org.apache.synapse.transport.util.ConfigurationBuilderUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * This class encapsulates pass-through http transport tuning configurations specified via a
 * configurations file or system properties.
 */
public class PassThroughConfiguration {

    /**
     * Default tuning parameter values
     */
    private static final int DEFAULT_WORKER_POOL_SIZE_CORE       = 40;
    private static final int DEFAULT_WORKER_POOL_SIZE_MAX        = 200;
    private static final int DEFAULT_WORKER_THREAD_KEEPALIVE_SEC = 60;
    private static final int DEFAULT_WORKER_POOL_QUEUE_LENGTH    = -1;
    private static final int DEFAULT_IO_BUFFER_SIZE              = 8 * 1024;
    private static final int DEFAULT_IO_THREADS_PER_REACTOR      =
                                                         Runtime.getRuntime().availableProcessors();
    private static final int DEFAULT_MAX_ACTIVE_CON = -1;
    private static final int DEFAULT_LISTENER_SHUTDOWN_WAIT_TIME = 0;
    private static final int DEFAULT_CONNECTION_GRACE_TIME = 10000;
    private Boolean isKeepAliveDisabled = null;

    //additional rest dispatch handlers
    private static final String REST_DISPATCHER_SERVICE="rest.dispatcher.service";
    // URI configurations that determine if it requires custom rest dispatcher
    private static final String REST_URI_API_REGEX = "rest_uri_api_regex";
    private static final String REST_URI_PROXY_REGEX = "rest_uri_proxy_regex";
    // properties which are allowed to be directly pass through from request context to response context explicitly
    private static final String ALLOWED_RESPONSE_PROPERTIES = "allowed_response_properties";

    /** Reverse proxy mode is enabled or not */
    private Boolean reverseProxyMode = null;

    /** Default Synapse service name */
    private String passThroughDefaultServiceName = null;

    private static final Log log = LogFactory.getLog(PassThroughConfiguration.class);

    private static PassThroughConfiguration _instance = new PassThroughConfiguration();

    private Properties props;

    private PassThroughConfiguration() {
        try {
            props = loadProperties("passthru-http.properties");
        } catch (Exception ignored) {}
    }

    public static PassThroughConfiguration getInstance() {
        return _instance;
    }

    public int getWorkerPoolCoreSize() {
        return ConfigurationBuilderUtil.getIntProperty(PassThroughConfigPNames.WORKER_POOL_SIZE_CORE,
                DEFAULT_WORKER_POOL_SIZE_CORE, props);
    }

    public int getWorkerPoolMaxSize() {
        return ConfigurationBuilderUtil.getIntProperty(PassThroughConfigPNames.WORKER_POOL_SIZE_MAX,
                DEFAULT_WORKER_POOL_SIZE_MAX, props);
    }

    public int getWorkerThreadKeepaliveSec() {
        return ConfigurationBuilderUtil.getIntProperty(PassThroughConfigPNames.WORKER_THREAD_KEEP_ALIVE_SEC,
                DEFAULT_WORKER_THREAD_KEEPALIVE_SEC, props);
    }

    public int getWorkerPoolQueueLen() {
        return ConfigurationBuilderUtil.getIntProperty(PassThroughConfigPNames.WORKER_POOL_QUEUE_LENGTH,
                DEFAULT_WORKER_POOL_QUEUE_LENGTH, props);
    }

    public int getIOThreadsPerReactor() {
        return ConfigurationBuilderUtil.getIntProperty(PassThroughConfigPNames.IO_THREADS_PER_REACTOR,
                DEFAULT_IO_THREADS_PER_REACTOR, props);
    }

    public int getIOBufferSize() {
        return ConfigurationBuilderUtil.getIntProperty(PassThroughConfigPNames.IO_BUFFER_SIZE,
                DEFAULT_IO_BUFFER_SIZE, props);
    }

    public boolean isKeepAliveDisabled() {
        if (isKeepAliveDisabled == null) {
            isKeepAliveDisabled =
                    ConfigurationBuilderUtil.getBooleanProperty(PassThroughConfigPNames.DISABLE_KEEPALIVE,
                            false, props);
        }
        return isKeepAliveDisabled;
    }

    public int getMaxActiveConnections() {
        return ConfigurationBuilderUtil.getIntProperty(PassThroughConfigPNames.C_MAX_ACTIVE, DEFAULT_MAX_ACTIVE_CON,
                props);
    }
    public int getListenerShutdownWaitTime() {
        return ConfigurationBuilderUtil.getIntProperty(PassThroughConfigPNames
                        .TRANSPORT_LISTENER_SHUTDOWN_WAIT_TIME_SEC, DEFAULT_LISTENER_SHUTDOWN_WAIT_TIME, props) * 1000;
    }

    public boolean isPreserveUserAgentHeader() {
        return ConfigurationBuilderUtil.getBooleanProperty(PassThroughConfigPNames.USER_AGENT_HEADER_PRESERVE,
                false, props);
    }

    public boolean isPreserveServerHeader() {
        return ConfigurationBuilderUtil.getBooleanProperty(PassThroughConfigPNames.SERVER_HEADER_PRESERVE,
                false, props);
    }

    public boolean isForcedXmlMessageValidationEnabled() {
        return ConfigurationBuilderUtil.getBooleanProperty(PassThroughConstants.FORCE_XML_MESSAGE_VALIDATION,
                false, props);
    }

    public boolean isForcedJSONMessageValidationEnabled() {
        return ConfigurationBuilderUtil.getBooleanProperty(PassThroughConstants.FORCE_JSON_MESSAGE_VALIDATION,
                false, props);
    }

    public String getPreserveHttpHeaders() {
        return ConfigurationBuilderUtil.getStringProperty(PassThroughConfigPNames.HTTP_HEADERS_PRESERVE,
                "", props);
    }

    public String getResponsePreseveHttpHeaders() {
        return ConfigurationBuilderUtil.getStringProperty(PassThroughConfigPNames.HTTP_RESPONSE_HEADERS_PRESERVE,
                "", props);
    }

    public boolean isServiceListBlocked() {
        return getBooleanProperty(PassThroughConfigPNames.BLOCK_SERVICE_LIST, true);
    }

    public int getConnectionIdleTime() {

        int idleTime;
        // Giving higher priority for grace time if it is configured, than for the configured idle time
        if (ConfigurationBuilderUtil.isIntPropertyConfigured(PassThroughConfigPNames.CONNECTION_GRACE_TIME, props)) {
            idleTime = getIdleTimeFromGraceTime();
        } else {
            // Setting idle time if it is configured, if not, using the default grace time to calculate idle time
            idleTime = ConfigurationBuilderUtil.getIntProperty(PassThroughConfigPNames.CONNECTION_IDLE_TIME,
                    getIdleTimeFromGraceTime(), props);
        }

        if (idleTime < 0) {
            return 0;
        }
        return idleTime;
    }
    public int getMaximumConnectionLifespan() {
        return ConfigurationBuilderUtil.getIntProperty(PassThroughConfigPNames.MAXIMUM_CONNECTION_LIFESPAN,
                Integer.MAX_VALUE, props);
    }
    public int getConnectionGraceTime() {
        return ConfigurationBuilderUtil.getIntProperty(PassThroughConfigPNames.CONNECTION_GRACE_TIME,
                DEFAULT_CONNECTION_GRACE_TIME, props);
    }

    /**
     * For the default value, grace time is reduced to avoid connection being used at the moment it is being closed.
     * @return default connection idle time
     */
    private int getIdleTimeFromGraceTime() {
        return ConfigurationBuilderUtil
                .getIntProperty(HttpConnectionParams.SO_TIMEOUT, 60000, props) - getConnectionGraceTime();
    }

    public String getCorrelationHeaderName() {
        return ConfigurationBuilderUtil.getStringProperty(PassThroughConfigPNames.CORRELATION_HEADER_NAME_PROPERTY,
                PassThroughConstants.CORRELATION_DEFAULT_HEADER, props);
    }

    /**
     * Loads the properties from a given property file path
     *
     * @param filePath Path of the property file
     * @return Properties loaded from given file
     */
    private static Properties loadProperties(String filePath) {

    	 Properties properties = new Properties();
         ClassLoader cl = Thread.currentThread().getContextClassLoader();

         if (log.isDebugEnabled()) {
             log.debug("Loading the file '" + filePath + "' from classpath");
         }

         InputStream in  = null;

         //if we reach to this assume that the we may have to looking to the customer provided external location for the
         //given properties
 		if (System.getProperty(PassThroughConstants.CONF_LOCATION) != null) {
 			try {
 				in = new FileInputStream(System.getProperty(PassThroughConstants.CONF_LOCATION) + File.separator + filePath);
 			} catch (FileNotFoundException e) {
 				String msg = "Error loading properties from a file at from the System defined location: " + filePath;
 				log.warn(msg);
 			}
 		}


         if (in == null) {
         	in = cl.getResourceAsStream(filePath);
             if (log.isDebugEnabled()) {
                 log.debug("Unable to load file  '" + filePath + "'");
             }

             filePath = "conf" + File.separatorChar + filePath;
             if (log.isDebugEnabled()) {
                 log.debug("Loading the file '" + filePath + "'");
             }

             in = cl.getResourceAsStream(filePath);
             if (in == null) {
                 if (log.isDebugEnabled()) {
                     log.debug("Unable to load file  '" + filePath + "'");
                 }
             }
         }
         if (in != null) {
             try {
                 properties.load(in);
             } catch (IOException e) {
                 String msg = "Error loading properties from a file at : " + filePath;
                 log.error(msg, e);
             }
         }
         return properties;
    }

    public Boolean getBooleanProperty(String name) {
        return ConfigurationBuilderUtil.getBooleanProperty(name, null, props);
    }

    public Boolean getBooleanProperty(String name, Boolean def) {
        return ConfigurationBuilderUtil.getBooleanProperty(name, def, props);
    }

    public Integer getIntProperty(String name, Integer def) {
        return ConfigurationBuilderUtil.getIntProperty(name, def, props);
    }

    public String getStringProperty(String name, String def) {
        return ConfigurationBuilderUtil.getStringProperty(name, def, props);
    }

    public String getRESTDispatchService() {
        return ConfigurationBuilderUtil.getStringProperty(REST_DISPATCHER_SERVICE, "", props);
    }

    public String getRestUriApiRegex() {
        return ConfigurationBuilderUtil.getStringProperty(REST_URI_API_REGEX, "", props);
    }

    public String getRestUriProxyRegex() {
        return ConfigurationBuilderUtil.getStringProperty(REST_URI_PROXY_REGEX, "", props);
    }

    public boolean isListeningIOReactorShared() {
        return ConfigurationBuilderUtil
                .getBooleanProperty(PassThroughConfigPNames.HTTP_LISTENING_IO_REACTOR_SHARING_ENABLE, false, props);
    }

    public String getAllowedResponseProperties() {
        return ConfigurationBuilderUtil.getStringProperty(ALLOWED_RESPONSE_PROPERTIES, null, props);
    }

    /**
     * Check for reverse proxy mode
     *
     * @return whether reverse proxy mode is enabled
     */
    public boolean isReverseProxyMode() {
        if (reverseProxyMode == null) {
            reverseProxyMode = Boolean.parseBoolean(System.getProperty("reverseProxyMode"));
        }
        return reverseProxyMode;
    }

    /**
     * Get the default synapse service name
     *
     * @return default synapse service name
     */
    public String getPassThroughDefaultServiceName() {
        if (passThroughDefaultServiceName == null) {
            passThroughDefaultServiceName = getStringProperty("passthru.default.service", "__SynapseService");
        }
        return passThroughDefaultServiceName;
    }
}
