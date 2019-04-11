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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.commons.emulator.http.dsl.dto.consumer;

import io.netty.handler.codec.http.HttpMethod;
import org.apache.synapse.commons.emulator.http.dsl.dto.Header;
import org.apache.synapse.commons.emulator.http.consumer.HttpRequestContext;
import org.apache.synapse.commons.emulator.http.dsl.dto.QueryParameter;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class IncomingMessage {

    private static IncomingMessage incoming;
    private HttpMethod method;
    private String path;
    private String body;
    private String context;
    private Pattern pathRegex;
    private Header header;
    private QueryParameter queryParameter;


    private static IncomingMessage getInstance() {
        incoming = new IncomingMessage();
        return incoming;
    }

    public static IncomingMessage request() {
        return getInstance();
    }

    public IncomingMessage withMethod(HttpMethod method) {
        this.method = method;
        return this;
    }

    public IncomingMessage withPath(String path) {
        this.path = path;
        return this;
    }

    public IncomingMessage withBody(String body) {
        this.body = body;
        return this;
    }

    public IncomingMessage withHeader(String name, String value) {
        header = new Header(name, value);
        return this;
    }

    public IncomingMessage withQueryParameter(String name, String value) {
        this.queryParameter = new QueryParameter(name, value);
        return this;
    }

    public boolean isMatch(HttpRequestContext requestContext) {
        if (isContextMatch(requestContext) && isHttpMethodMatch(requestContext) &&
                isRequestContentMatch(requestContext) &&
                isHeadersMatch(requestContext) && isQueryParameterMatch(requestContext)) {
            return true;
        }
        return false;
    }

    public void buildPathRegex(String context) {
        this.context = context;
        String regex = buildRegex(context, path);
        this.pathRegex = Pattern.compile(regex);
    }

    private boolean isContextMatch(HttpRequestContext requestContext) {
        this.context = extractContext(requestContext.getUri());
        return pathRegex.matcher(context).find();
    }


    private boolean isHttpMethodMatch(HttpRequestContext requestContext) {
        if (method == null) {
            return true;
        }

        if (method.equals(requestContext.getHttpMethod())) {
            return true;
        }
        return false;
    }

    private boolean isRequestContentMatch(HttpRequestContext requestContext) {
        if (body == null || body.isEmpty()) {
            return true;
        }

        if (body.equalsIgnoreCase(requestContext.getRequestBody())) {
            return true;
        }
        return false;
    }

    private boolean isHeadersMatch(HttpRequestContext requestContext) {
        if (header == null) {
            return true;
        }

        Map<String, List<String>> headerParameters = requestContext.getHeaderParameters();
        List<String> headerValues = headerParameters.get(header.getName());

        if (headerParameters == null || headerValues == null || headerValues.isEmpty()) {
            return false;
        }

        for (String value : headerValues) {
            if (value.equalsIgnoreCase(header.getValue())) {
                return true;
            }
        }
        return false;
    }

    private boolean isQueryParameterMatch(HttpRequestContext requestContext) {
        if (queryParameter == null) {
            return true;
        }

        Map<String, List<String>> queryParameters = requestContext.getQueryParameters();
        List<String> queryValues = queryParameters.get(queryParameter.getName());

        if (queryParameters == null || queryValues == null || queryValues.isEmpty()) {
            return false;
        }

        for (String value : queryValues) {
            if (value.equalsIgnoreCase(queryParameter.getValue())) {
                return true;
            }
        }
        return false;
    }

    private String buildRegex(String context, String path) {
        String fullPath = "";

        if ((context == null || context.isEmpty()) && (path == null || path.isEmpty())) {
            return ".*";
        }

        if (context != null && !context.isEmpty()) {
            fullPath = context;

            if (!fullPath.startsWith("/")) {
                fullPath = "/" + fullPath;
            }

            if (!fullPath.endsWith("/")) {
                fullPath = fullPath + "/";
            }
        } else {
            fullPath = ".*";
        }

        if (path != null && !path.isEmpty()) {
            if (fullPath.endsWith("/") && path.startsWith("/")) {
                fullPath = fullPath + path.substring(1);
            } else if (fullPath.endsWith("/") && !path.startsWith("/")) {
                fullPath = fullPath + path;
            } else if (!fullPath.endsWith("/") && path.startsWith("/")) {
                fullPath = fullPath + path;
            } else {
                fullPath = fullPath + "/" + path;
            }
        } else {
            fullPath = fullPath + ".*";
        }

        if (fullPath.endsWith("/")) {
            fullPath = fullPath.substring(0, fullPath.length() - 1);
        }

        return "^" + fullPath + "/$";
    }

    private String extractContext(String uri) {
        if (uri == null || uri.isEmpty()) {
            return null;
        }
        if (!uri.contains("?")) {
            if (!uri.endsWith("/")) {
                uri = uri + "/";
            }
            return uri;
        }
        uri = uri.split("\\?")[0];
        if (!uri.endsWith("/")) {
            uri = uri + "/";
        }
        return uri;
    }
}
