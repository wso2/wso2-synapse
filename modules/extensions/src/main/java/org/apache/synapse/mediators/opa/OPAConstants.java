/*
 *  Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.mediators.opa;

/**
 * This class represents the constants that are used for OPA mediator
 */
public final class OPAConstants {

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String CONTENT_TYPE_HEADER = "Content-Type";
    public static final String APPLICATION_JSON = "application/json";
    public static final String STRICT = "Strict";
    public static final String ALLOW_ALL = "AllowAll";
    public static final String HOST_NAME_VERIFIER = "httpclient.hostnameVerifier";
    public static final String HTTPS = "https";
    public static final String TRUST_STORE_PASSWORD_SYSTEM_PROPERTY = "javax.net.ssl.trustStorePassword";
    public static final String TRUST_STORE_LOCATION_SYSTEM_PROPERTY = "javax.net.ssl.trustStore";
    public static final String HTTP_METHOD_STRING = "HTTP_METHOD";
    public static final String API_BASEPATH_STRING = "TransportInURL";
    public static final String HTTP_RESPONSE_STATUS_CODE = "HTTP_RESPONSE_STATUS_CODE";

    // Additional properties
    public static final String MAX_OPEN_CONNECTIONS_PARAMETER = "maxOpenConnections";
    public static final String MAX_PER_ROUTE_PARAMETER = "maxPerRoute";
    public static final String CONNECTION_TIMEOUT_PARAMETER = "connectionTimeout";
    public static final String ADDITIONAL_MC_PROPERTY_PARAMETER = "additionalMCProperties";
    public static final String ADDITIONAL_MC_PROPERTY_DIVIDER = ",";
    public static final String OPA_POLICY_FAILURE_HANDLER_PARAMETER = "opaPolicyFailureHandler";

    //OPA output fields
    public static final String INPUT_KEY = "input";
    public static final String REQUEST_ORIGIN_KEY = "requestOrigin";
    public static final String REQUEST_METHOD_KEY = "method";
    public static final String REQUEST_PATH_KEY = "path";
    public static final String REQUEST_TRANSPORT_HEADERS_KEY = "transportHeaders";

    //OPA response
    public static final String EMPTY_OPA_RESPONSE = "{}";
    public static final String OPA_RESPONSE_RESULT_KEY = "result";
    public static final String OPA_RESPONSE_DEFAULT_RULE = "allow";
}
