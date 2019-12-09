/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.rest.cors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;
import org.apache.synapse.config.SynapsePropertiesLoader;
import org.apache.synapse.rest.RESTConstants;

import java.net.InterfaceAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

/**
 * This class holds CORS configurations made in synapse.properties file
 */
public class SynapseCORSConfiguration implements CORSConfiguration {

    private static Log LOG = LogFactory.getLog(SynapseCORSConfiguration.class);
    private static SynapseCORSConfiguration corsConfigs = null;

    private boolean enabled;
    private Set<String> allowedOrigins = new HashSet<>();
    private String allowedHeaders;

    private SynapseCORSConfiguration() {
        enabled = SynapsePropertiesLoader.getBooleanProperty(RESTConstants.CORS_CONFIGURATION_ENABLED, true);

        //Retrieve allowed origin list
        String allowedOriginListStr =
                SynapsePropertiesLoader.getPropertyValue(RESTConstants.CORS_CONFIGURATION_ACCESS_CTL_ALLOW_ORIGIN, null);
        if (allowedOriginListStr != null) {
            String[] originList = allowedOriginListStr.split(",");
            for (String origin : originList) {
                String trimmedOrigin = origin.trim();
                allowedOrigins.add(trimmedOrigin);
                try {
                    URL url = new URL(trimmedOrigin);
                    if (url.getHost().equals("localhost")) {
                        // Add localhost IPs as allowed origin
                        Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
                        while (networkInterfaces.hasMoreElements()) {
                            NetworkInterface nInterface = networkInterfaces.nextElement();
                            for (InterfaceAddress iAddr : nInterface.getInterfaceAddresses()) {
                                URL localUrl =
                                        new URL(url.getProtocol(), iAddr.getAddress().getHostAddress(), url.getPort(), "");
                                allowedOrigins.add(localUrl.toString());
                            }
                        }
                    }
                } catch (MalformedURLException e) {
                    throw new SynapseException("Provided origin URL " + trimmedOrigin + " is malformed", e);
                } catch (SocketException e) {
                    throw new SynapseException("Error occurred while retrieving network interfaces", e);
                }
            }
        }

        //Retrieve allowed headers
        allowedHeaders =
                SynapsePropertiesLoader.getPropertyValue(RESTConstants.CORS_CONFIGURATION_ACCESS_CTL_ALLOW_HEADERS, "");
    }

    public static SynapseCORSConfiguration getInstance() {
        if (corsConfigs != null) {
            return corsConfigs;
        }
        //init CORS configurations
        corsConfigs = new SynapseCORSConfiguration();
        return corsConfigs;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Set<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public String getAllowedHeaders() {
        return allowedHeaders;
    }
}
