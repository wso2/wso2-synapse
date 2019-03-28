/*
 Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.apache.synapse.unittest.data.classes;

import javafx.util.Pair;
import java.util.List;

public class MockService {

    private int port;
    private String serviceName;
    private String context;
    private String method;
    private String requestPayload;
    private String responsePayload;
    private List<Pair<String,String>> requestHeaders;
    private List<Pair<String,String>> responseHeaders;

    /**
     * Get mock service port.
     *
     * @return mock service port as in descriptor data
     */
    public int getPort() {
        return port;
    }

    /**
     * Get mock services name.
     *
     * @return mock services stored index
     */
    public String getServiceName() {
        return serviceName;
    }

    /**
     * Get mock service context.
     *
     * @return mock service context as in descriptor data
     */
    public String getContext() {
        return context;
    }

    /**
     * Get mock service type.
     *
     * @return mock service type as in descriptor data
     */
    public String getMethod() {
        return method;
    }

    /**
     * Get mock service input payload.
     *
     * @return mock service input payload as in descriptor data
     */
    public String getRequestPayload() {
        return requestPayload;
    }

    /**
     * Get mock service response.
     *
     * @return mock service response as in descriptor data
     */
    public String getResponsePayload() {
        return responsePayload;
    }

    /**
     * Get mock service request headers.
     *
     * @return mock service request headers as in descriptor data
     */
    public List<Pair<String, String>> getRequestHeaders() {
        return requestHeaders;
    }

    /**
     * Get mock service response headers.
     *
     * @return mock service response headers as in descriptor data
     */
    public List<Pair<String, String>> getResponseHeaders() {
        return responseHeaders;
    }

    /**
     * Add mock service port inside the ArrayList.
     *
     * @param port service port as in descriptor data
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * Add mock service name inside the map.
     *
     * @param serviceName service name as key
     */
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    /**
     * Add mock service path inside the ArrayList.
     *
     * @param context service path as in descriptor data
     */
    public void setContext(String context) {
        this.context = context;
    }

    /**
     * Add mock service type inside the ArrayList.
     *
     * @param method service type as in descriptor data
     */
    public void setMethod(String method) {
        this.method = method;
    }

    /**
     * Add mock service input payload inside the ArrayList.
     *
     * @param requestPayload service input payload as in descriptor data
     */
    public void setRequestPayload(String requestPayload) {
        this.requestPayload = requestPayload;
    }

    /**
     * Add mock service response inside the ArrayList.
     *
     * @param responsePayload service response as in descriptor data
     */
    public void setResponsePayload(String responsePayload) {
        this.responsePayload = responsePayload;
    }

    /**
     * Get mock service headers.
     *
     * @param requestHeaders service request headers
     */
    public void setRequestHeaders(List<Pair<String, String>> requestHeaders) {
        this.requestHeaders = requestHeaders;
    }

    /**
     * Get mock service headers.
     *
     * @param responseHeaders service request headers
     */
    public void setResponseHeaders(List<Pair<String, String>> responseHeaders) {
        this.responseHeaders = responseHeaders;
    }
}
