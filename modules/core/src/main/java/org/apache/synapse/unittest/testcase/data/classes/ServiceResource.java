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

package org.apache.synapse.unittest.testcase.data.classes;

import java.util.List;
import java.util.Map;

public class ServiceResource {

    private int statusCode;
    private String subContext;
    private String method;
    private String requestPayload;
    private String responsePayload;
    private List<Map.Entry<String,String>> requestHeaders;
    private List<Map.Entry<String,String>> responseHeaders;

    /**
     * Get mock service sub-context.
     *
     * @return mock service type as in descriptor data
     */
    public String getSubContext() {
        return subContext;
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
    public List<Map.Entry<String, String>> getRequestHeaders() {
        return requestHeaders;
    }

    /**
     * Get mock service response headers.
     *
     * @return mock service response headers as in descriptor data
     */
    public List<Map.Entry<String, String>> getResponseHeaders() {
        return responseHeaders;
    }

    /**
     * Get mock service response status code.
     *
     * @return mock service response status code as in descriptor data
     */
    public int getStatusCode() {
        return statusCode;
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
    public void setRequestHeaders(List<Map.Entry<String, String>> requestHeaders) {
        this.requestHeaders = requestHeaders;
    }

    /**
     * Get mock service headers.
     *
     * @param responseHeaders service request headers
     */
    public void setResponseHeaders(List<Map.Entry<String, String>> responseHeaders) {
        this.responseHeaders = responseHeaders;
    }

    /**
     * Set mock service sub-context.
     *
     * @param subContext service sub-context as in descriptor data
     */
    public void setSubContext(String subContext) {
        this.subContext = subContext;
    }

    /**
     * Set mock service response status code.
     *
     * @param statusCode service response status code as in descriptor data
     */
    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }
}
