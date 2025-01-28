/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * you may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.data.connector;

import java.util.HashMap;
import java.util.Map;

public class DefaultConnectorResponse implements ConnectorResponse {

    private Object payload;
    private Map<String, Object> headers = new HashMap<>();
    private Map<String, Object> attributes = new HashMap<>();

    @Override
    public void setPayload(Object payload) {
        this.payload = payload;
    }

    @Override
    public Object getPayload() {
        return payload;
    }

    @Override
    public void setHeaders(Map<String, Object> headers) {
        if (headers != null) {
            this.headers = headers;
        }
    }

    @Override
    public Map<String, Object> getHeaders() {
        return headers;
    }

    @Override
    public void setAttributes(Map<String, Object> attributes) {
        if (attributes != null) {
            this.attributes = attributes;
        }
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public void addAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    @Override
    public void removeAttribute(String key) {
        attributes.remove(key);
    }

    @Override
    public void addHeader(String key, String value) {
        headers.put(key, value);
    }

    @Override
    public void removeHeader(String key) {
        headers.remove(key);
    }
}
