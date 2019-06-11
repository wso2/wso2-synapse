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

import org.apache.synapse.config.SynapsePropertiesLoader;
import org.apache.synapse.rest.RESTConstants;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * This class holds CORS configurations made in synapse.properties file
 */
public class CORSConfiguration {

    private static CORSConfiguration corsConfigs = null;

    private boolean enabled;
    private Set<String> allowedOrigins;
    private String allowedHeaders;

    private CORSConfiguration() {
        enabled = SynapsePropertiesLoader.getBooleanProperty(RESTConstants.CORS_CONFIGURATION_ENABLED, true);

        //Retrieve allowed origin list
        String allowedOriginListStr =
                SynapsePropertiesLoader.getPropertyValue(RESTConstants.CORS_CONFIGURATION_ACCESS_CTL_ALLOW_ORIGIN, "");
        allowedOrigins = new HashSet<>(Arrays.asList(allowedOriginListStr.split(",")));

        //Retrieve allowed headers
        allowedHeaders =
                SynapsePropertiesLoader.getPropertyValue(RESTConstants.CORS_CONFIGURATION_ACCESS_CTL_ALLOW_HEADERS, "");
    }

    public static CORSConfiguration getCORSConfig() {
        if (corsConfigs != null) {
            return corsConfigs;
        }
        //init CORS configurations
        corsConfigs = new CORSConfiguration();
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
