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
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.rest.RESTConstants;

import java.util.Map;
import java.util.Set;

/**
 * This class provides util functions for all CORS related activities.
 */
public class CORSHelper {

    private static final Log log = LogFactory.getLog(CORSHelper.class);

    /**
     * Function to retrieve allowed origin header string
     *
     * @param origin         Received origin
     * @param allowedOrigins allowed origin set
     * @return
     */
    public static String getAllowedOrigins(String origin, Set<String> allowedOrigins) {

        if (allowedOrigins.contains("*")) {
            return "*";
        } else if (allowedOrigins.contains(origin)) {
            return origin;
        } else {
            return null;
        }
    }

    /**
     * Functions to handle CORS Headers
     *
     * @param synCtx Synapse message context
     * @param corsConfiguration of the API
     * @param supportedMethods
     * @param updateHeaders Boolean
     */
    public static void handleCORSHeaders(CORSConfiguration corsConfiguration, MessageContext synCtx, String supportedMethods, boolean updateHeaders) {

        if (corsConfiguration.isEnabled()) {
            org.apache.axis2.context.MessageContext msgCtx = ((Axis2MessageContext) synCtx).getAxis2MessageContext();
            Map<String, String> transportHeaders = (Map<String, String>) msgCtx.getProperty(
                    org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
            if (transportHeaders != null) {
                String allowedOrigin = getAllowedOrigins(transportHeaders.get(RESTConstants.CORS_HEADER_ORIGIN),
                        corsConfiguration.getAllowedOrigins());
                if (updateHeaders) {
                    transportHeaders.put(RESTConstants.CORS_HEADER_ACCESS_CTL_ALLOW_METHODS, supportedMethods);
                    transportHeaders.put(RESTConstants.CORS_HEADER_ACCESS_CTL_ALLOW_ORIGIN, allowedOrigin);
                    transportHeaders.put(RESTConstants.CORS_HEADER_ACCESS_CTL_ALLOW_HEADERS,
                            corsConfiguration.getAllowedHeaders());
                }

                synCtx.setProperty(RESTConstants.INTERNAL_CORS_HEADER_ACCESS_CTL_ALLOW_METHODS, supportedMethods);
                synCtx.setProperty(RESTConstants.INTERNAL_CORS_HEADER_ACCESS_CTL_ALLOW_ORIGIN, allowedOrigin);
                synCtx.setProperty(RESTConstants.INTERNAL_CORS_HEADER_ACCESS_CTL_ALLOW_HEADERS,
                        corsConfiguration.getAllowedHeaders());
                synCtx.setProperty(RESTConstants.INTERNAL_CORS_HEADER_ORIGIN,
                        transportHeaders.get(RESTConstants.CORS_HEADER_ORIGIN));
            }
        }

    }

    /**
     * Function to set CORS headers to response message transport headers extracting from synapse message context
     *
     * @param synCtx
     * @param corsConfiguration of the API
     */
    public static void handleCORSHeadersForResponse(CORSConfiguration corsConfiguration, MessageContext synCtx) {

        if (corsConfiguration.isEnabled()) {
            org.apache.axis2.context.MessageContext msgCtx = ((Axis2MessageContext) synCtx).getAxis2MessageContext();
            Map<String, String> transportHeaders = (Map<String, String>) msgCtx.getProperty(
                    org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
            if (transportHeaders != null) {
                if (synCtx.getProperty(RESTConstants.INTERNAL_CORS_HEADER_ACCESS_CTL_ALLOW_METHODS) != null) {
                    transportHeaders.put(RESTConstants.CORS_HEADER_ACCESS_CTL_ALLOW_METHODS,
                            (String) synCtx.getProperty(RESTConstants.INTERNAL_CORS_HEADER_ACCESS_CTL_ALLOW_METHODS));
                }

                if (synCtx.getProperty(RESTConstants.INTERNAL_CORS_HEADER_ACCESS_CTL_ALLOW_ORIGIN) != null) {
                    transportHeaders.put(RESTConstants.CORS_HEADER_ACCESS_CTL_ALLOW_ORIGIN,
                            (String) synCtx.getProperty(RESTConstants.INTERNAL_CORS_HEADER_ACCESS_CTL_ALLOW_ORIGIN));
                }

                if (synCtx.getProperty(RESTConstants.INTERNAL_CORS_HEADER_ACCESS_CTL_ALLOW_HEADERS) != null) {
                    transportHeaders.put(RESTConstants.CORS_HEADER_ACCESS_CTL_ALLOW_HEADERS,
                            (String) synCtx.getProperty(RESTConstants.INTERNAL_CORS_HEADER_ACCESS_CTL_ALLOW_HEADERS));
                }

                if (synCtx.getProperty(RESTConstants.INTERNAL_CORS_HEADER_ORIGIN) != null) {
                    transportHeaders.put(RESTConstants.CORS_HEADER_ORIGIN,
                            (String) synCtx.getProperty(RESTConstants.INTERNAL_CORS_HEADER_ORIGIN));
                }
            }
        }
    }

}
