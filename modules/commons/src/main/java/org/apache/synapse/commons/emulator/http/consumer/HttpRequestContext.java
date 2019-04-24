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

package org.apache.synapse.commons.emulator.http.consumer;


import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import org.apache.synapse.commons.emulator.RequestProcessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpRequestContext {

    private Map<String, List<String>> headerParameters;
    private Map<String, List<String>> queryParameters;
    private StringBuilder requestBody;
    private String uri;
    private HttpMethod httpMethod;
    private HttpVersion httpVersion;
    private boolean isKeepAlive;

    void addHeaderParameter(String key, String value) {
        if (headerParameters == null) {
            this.headerParameters = new HashMap<>();
        }

        List<String> headerValues = this.headerParameters.get(key);
        if (headerValues == null) {
            headerValues = new ArrayList<>();
        }
        headerValues.add(value);
        this.headerParameters.put(key, headerValues);
    }

    public Map<String, List<String>> getQueryParameters() {
        return queryParameters;
    }

    void setQueryParameters(Map<String, List<String>> queryParameters) {
        this.queryParameters = queryParameters;
    }

    public Map<String, List<String>> getHeaderParameters() {
        return headerParameters;
    }

    void appendResponseContent(Object content) {
        if (requestBody == null) {
            this.requestBody = new StringBuilder();
        }
        this.requestBody.append(content);
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public HttpMethod getHttpMethod() {
        return httpMethod;
    }

    void setHttpMethod(HttpMethod httpMethod) {
        this.httpMethod = httpMethod;
    }

    public HttpVersion getHttpVersion() {
        return httpVersion;
    }

    void setHttpVersion(HttpVersion httpVersion) {
        this.httpVersion = httpVersion;
    }

    boolean isKeepAlive() {
        return isKeepAlive;
    }

    void setKeepAlive(boolean isKeepAlive) {
        this.isKeepAlive = isKeepAlive;
    }

    public String getRequestBody() {
        if (requestBody == null) {
            return null;
        }

        return RequestProcessor.trimStrings(requestBody.toString());
    }
}
