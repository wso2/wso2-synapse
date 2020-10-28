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
        return getIntProperty(PassThroughConfigPNames.WORKER_POOL_SIZE_CORE,
                DEFAULT_WORKER_POOL_SIZE_CORE);
    }

    public int getWorkerPoolMaxSize() {
        return getIntProperty(PassThroughConfigPNames.WORKER_POOL_SIZE_MAX,
                DEFAULT_WORKER_POOL_SIZE_MAX);
    }

    public int getWorkerThreadKeepaliveSec() {
        return getIntProperty(PassThroughConfigPNames.WORKER_THREAD_KEEP_ALIVE_SEC,
                DEFAULT_WORKER_THREAD_KEEPALIVE_SEC);
    }

    public int getWorkerPoolQueueLen() {
        return getIntProperty(PassThroughConfigPNames.WORKER_POOL_QUEUE_LENGTH,
                DEFAULT_WORKER_POOL_QUEUE_LENGTH);
    }

    public int getIOThreadsPerReactor() {
        return getIntProperty(PassThroughConfigPNames.IO_THREADS_PER_REACTOR,
                DEFAULT_IO_THREADS_PER_REACTOR);
    }

    public int getIOBufferSize() {
        return getIntProperty(PassThroughConfigPNames.IO_BUFFER_SIZE,
                DEFAULT_IO_BUFFER_SIZE);
    }

    public boolean isKeepAliveDisabled() {
        if (isKeepAliveDisabled == null) {
            isKeepAliveDisabled = getBooleanProperty(PassThroughConfigPNames.DISABLE_KEEPALIVE, false);
        }
        return isKeepAliveDisabled.booleanValue();
    }

    public int getMaxActiveConnections() {
        return getIntProperty(PassThroughConfigPNames.C_MAX_ACTIVE, DEFAULT_MAX_ACTIVE_CON);
    }
    public int getListenerShutdownWaitTime() {
        return getIntProperty(PassThroughConfigPNames.TRANSPORT_LISTENER_SHUTDOWN_WAIT_TIME_SEC,
                DEFAULT_LISTENER_SHUTDOWN_WAIT_TIME)*1000;
    }

    public boolean isPreserveUserAgentHeader() {
        return getBooleanProperty(PassThroughConfigPNames.USER_AGENT_HEADER_PRESERVE, false);
    }

    public boolean isPreserveServerHeader() {
        return getBooleanProperty(PassThroughConfigPNames.SERVER_HEADER_PRESERVE, false);
    }

    public boolean isForcedXmlMessageValidationEnabled() {
        return getBooleanProperty(PassThroughConstants.FORCE_XML_MESSAGE_VALIDATION, false);
    }

    public boolean isForcedJSONMessageValidationEnabled() {
        return getBooleanProperty(PassThroughConstants.FORCE_JSON_MESSAGE_VALIDATION, false);
    }

    public String getPreserveHttpHeaders() {
        return getStringProperty(PassThroughConfigPNames.HTTP_HEADERS_PRESERVE, "");
    }

    public String getResponsePreseveHttpHeaders() {
        return getStringProperty(PassThroughConfigPNames.HTTP_RESPONSE_HEADERS_PRESERVE, "");
    }

    public int getConnectionIdleTime() {

        int idleTime;
        // Giving higher priority for grace time if it is configured, than for the configured idle time
        if (isIntPropertyConfigured(PassThroughConfigPNames.CONNECTION_GRACE_TIME)) {
            idleTime = getIdleTimeFromGraceTime();
        } else {
            // Setting idle time if it is configured, if not, using the default grace time to calculate idle time
            idleTime = getIntProperty(PassThroughConfigPNames.CONNECTION_IDLE_TIME, getIdleTimeFromGraceTime());
        }

        if (idleTime < 0) {
            return 0;
        }
        return idleTime;
    }
    public int getMaximumConnectionLifespan() {
        return getIntProperty(PassThroughConfigPNames.MAXIMUM_CONNECTION_LIFESPAN, Integer.MAX_VALUE);
    }
    public int getConnectionGraceTime() {
        return getIntProperty(PassThroughConfigPNames.CONNECTION_GRACE_TIME, DEFAULT_CONNECTION_GRACE_TIME);
    }

    /**
     * For the default value, grace time is reduced to avoid connection being used at the moment it is being closed
     * @return default connection idle time
     */
    private int getIdleTimeFromGraceTime(){
        return getIntProperty(HttpConnectionParams.SO_TIMEOUT, 60000) - getConnectionGraceTime();
    }

    public String getCorrelationHeaderName() {
        return getStringProperty(PassThroughConfigPNames.CORRELATION_HEADER_NAME_PROPERTY,
                PassThroughConstants.CORRELATION_DEFAULT_HEADER);
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

    /**
     * Get an int property that tunes pass-through http transport. Prefer system properties
     *
     * @param name name of the system/config property
     * @param def  default value to return if the property is not set
     * @return the value of the property to be used
     */
    public Integer getIntProperty(String name, Integer def) {
        String val = System.getProperty(name);
        if (val == null) {
            val = props.getProperty(name);
        }

        if (val != null) {
            int intVal;
            try {
                intVal = Integer.valueOf(val);
            } catch (NumberFormatException e) {
                log.warn("Invalid pass-through http tuning property value. " + name +
                        " must be an integer");
                return def;
            }
            if (log.isDebugEnabled()) {
                log.debug("Using pass-through http tuning parameter : " + name + " = " + val);
            }
            return intVal;
        }

        return def;
    }

    /**
     * Return true if user has configured an int property that tunes pass-through http transport
     * @param name  name of the system/config property
     * @return      true if property is configured
     */
    private boolean isIntPropertyConfigured(String name) {

        String val = System.getProperty(name);
        if (val == null) {
            val = props.getProperty(name);
        }

        if (val != null) {
            try {
                Integer.parseInt(val);
            } catch (NumberFormatException e) {
                log.warn("Incorrect pass-through http tuning property value. " + name +
                        " must be an integer");
                return false;
            }
            if (log.isDebugEnabled()) {
                log.debug("Using configured pass-through http tuning property value for : " + name);
            }
            return true;
        }

        return false;
    }

    /**
     * Get an int property that tunes pass-through http transport. Prefer system properties
     *
     * @param name name of the system/config property
     * @return the value of the property, null if the property is not found
     */
    public Integer getIntProperty(String name) {
        return getIntProperty(name, null);
    }

    /**
     * Get a boolean property that tunes pass-through http transport. Prefer system properties
     *
     * @param name name of the system/config property
     * @param def  default value to return if the property is not set
     * @return the value of the property to be used
     */
    public Boolean getBooleanProperty(String name, Boolean def) {
        String val = System.getProperty(name);
        if (val == null) {
            val = props.getProperty(name);
        }

        if (val != null) {
            if (log.isDebugEnabled()) {
                log.debug("Using pass-through http tuning parameter : " + name + " = " + val);
            }
            return Boolean.valueOf(val);
        }

        return def;
    }

    /**
     * Get a Boolean property that tunes pass-through http transport. Prefer system properties
     *
     * @param name name of the system/config property
     * @return the value of the property, null if the property is not found
     */
    public Boolean getBooleanProperty(String name) {
        return getBooleanProperty(name, null);
    }

    /**
     * Get a String property that tunes pass-through http transport. Prefer system properties
     *
     * @param name name of the system/config property
     * @param def  default value to return if the property is not set
     * @return the value of the property to be used
     */
    public String getStringProperty(String name, String def) {
        String val = System.getProperty(name);
        if (val == null) {
            val = props.getProperty(name);
        }

        return val == null ? def : val;
    }


    public String getRESTDispatchService() {
        return getStringProperty(REST_DISPATCHER_SERVICE,"");
    }

    public String getRestUriApiRegex() {
        return getStringProperty(REST_URI_API_REGEX, "");
    }

    public String getRestUriProxyRegex() {
        return getStringProperty(REST_URI_PROXY_REGEX, "");
    }

    public boolean isListeningIOReactorShared(){
        return getBooleanProperty(PassThroughConfigPNames.HTTP_LISTENING_IO_REACTOR_SHARING_ENABLE, false);
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
