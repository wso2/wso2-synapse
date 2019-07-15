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

package org.apache.synapse.rest;

public class RESTConstants {

    public static enum METHODS {
        GET, POST, PUT, DELETE, OPTIONS, HEAD, PATCH
    }

    public static final int PROTOCOL_HTTP_AND_HTTPS = 0;
    public static final int PROTOCOL_HTTP_ONLY      = 1;
    public static final int PROTOCOL_HTTPS_ONLY     = 2;

    public static final String REST_FULL_REQUEST_PATH = "REST_FULL_REQUEST_PATH";
    public static final String REST_SUB_REQUEST_PATH = "REST_SUB_REQUEST_PATH";
    public static final String REST_METHOD = "REST_METHOD";
    public static final String METHOD_OPTIONS = "OPTIONS";
    public static final String REST_ALL_SUPPORTED_METHODS = "GET, POST, PUT, DELETE";
    public static final String REST_URI_VARIABLE_PREFIX = "uri.var.";
    public static final String REST_QUERY_PARAM_PREFIX = "query.param.";
    public static final String REST_URL_PATTERN = "rest.url.pattern";

    public static final String SYNAPSE_RESOURCE = "SYNAPSE_RESOURCE";
    public static final String SYNAPSE_REST_API = "SYNAPSE_REST_API";
    public static final String SYNAPSE_REST_API_VERSION = "SYNAPSE_REST_API_VERSION";
    public static final String SYNAPSE_REST_API_VERSION_STRATEGY = "SYNAPSE_REST_API_VERSION_STRATEGY";
    public static final String SYNAPSE_REST_CONTEXT_VERSION_VARIABLE = "{version}";

    public static final String REST_API_CONTEXT = "REST_API_CONTEXT";
    public static final String REST_URL_PREFIX = "REST_URL_PREFIX";

    public static final String DEFAULT_ENCODING = "UTF-8";
    
    public static final String NO_MATCHING_RESOURCE_HANDLER = "_resource_mismatch_handler_";

    /**
     * Delimiter of an url  query string
     */
    public static final String QUERY_PARAM_DELIMITER = "&";

    public static final String CORS_HEADER_ACCESS_CTL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
    public static final String CORS_HEADER_ACCESS_CTL_ALLOW_METHODS = "Access-Control-Allow-Methods";
    public static final String CORS_HEADER_ACCESS_CTL_ALLOW_HEADERS = "Access-Control-Allow-Headers";
    public static final String CORS_HEADER_ORIGIN = "Origin";

    /**
     * CORS related configuration in synapse.properties
     */
    // enable/disable CORS support
    public static final String CORS_CONFIGURATION_ENABLED = "synapse.rest.CORSConfig.enabled";
    // List of allowed origins (comma separated)
    public static final String CORS_CONFIGURATION_ACCESS_CTL_ALLOW_ORIGIN =
                                                                "synapse.rest.CORSConfig.Access-Control-Allow-Origin";
    // List of allowed headers (comma separated)
    public static final String CORS_CONFIGURATION_ACCESS_CTL_ALLOW_HEADERS =
                                                                "synapse.rest.CORSConfig.Access-Control-Allow-Headers";

    /**
     * Constant prefix for rest related internal properties
     */
    public static final String _SYNAPSE_INTERNAL_ = "_SYNAPSE_INTERNAL_REST_";

    public static final String INTERNAL_CORS_HEADER_ACCESS_CTL_ALLOW_ORIGIN =
                                                                    _SYNAPSE_INTERNAL_ + "Access-Control-Allow-Origin";
    public static final String INTERNAL_CORS_HEADER_ACCESS_CTL_ALLOW_METHODS =
                                                                    _SYNAPSE_INTERNAL_+ "Access-Control-Allow-Methods";
    public static final String INTERNAL_CORS_HEADER_ACCESS_CTL_ALLOW_HEADERS =
                                                                    _SYNAPSE_INTERNAL_+ "Access-Control-Allow-Headers";
    public static final String INTERNAL_CORS_HEADER_ORIGIN = _SYNAPSE_INTERNAL_+ "Origin";

}
