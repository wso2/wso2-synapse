/**
 *  Copyright (c) 2024, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.synapse.transport.passthru.vt;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Represents an HTTP response received from a backend server via Apache HttpClient 4.x.
 * Wraps a {@link CloseableHttpResponse} and provides a unified interface for headers,
 * status, and body access that is compatible with the rest of the VT transport.
 *
 * <p>This is the VT (Virtual Thread) equivalent of
 * {@link org.apache.synapse.transport.passthru.TargetResponse}.</p>
 */
public class VTTargetResponse {

    private final CloseableHttpResponse httpResponse;
    private final int status;
    private final String statusLine;
    private final Map<String, String> headers;
    private final Map<String, String> excessHeaders;

    /**
     * Construct a VTTargetResponse wrapping an Apache HC4 {@link CloseableHttpResponse}.
     *
     * @param httpResponse the response obtained from {@code CloseableHttpClient.execute()}
     */
    public VTTargetResponse(CloseableHttpResponse httpResponse) {
        this.httpResponse = httpResponse;
        this.status = httpResponse.getStatusLine().getStatusCode();
        this.statusLine = httpResponse.getStatusLine().getReasonPhrase();

        this.headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        this.excessHeaders = new LinkedHashMap<>();
        for (Header header : httpResponse.getAllHeaders()) {
            String name = header.getName();
            String value = header.getValue();
            if (headers.containsKey(name)) {
                excessHeaders.put(name, value);
            } else {
                headers.put(name, value);
            }
        }
    }

    // ---- Getters ----

    public CloseableHttpResponse getHttpResponse() {
        return httpResponse;
    }

    public int getStatus() {
        return status;
    }

    public String getStatusLine() {
        return statusLine;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public Map<String, String> getExcessHeaders() {
        return excessHeaders;
    }

    public String getHeader(String name) {
        return headers.get(name);
    }

    /**
     * Get the body input stream for reading the response body.
     */
    public InputStream getBodyInputStream() {
        HttpEntity entity = httpResponse.getEntity();
        if (entity != null) {
            try {
                return entity.getContent();
            } catch (IOException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Get Content-Length from the entity, or -1 if unknown.
     */
    public long getContentLength() {
        HttpEntity entity = httpResponse.getEntity();
        if (entity != null) {
            return entity.getContentLength();
        }
        return -1;
    }

    /**
     * Whether the response uses chunked transfer encoding.
     */
    public boolean isChunked() {
        HttpEntity entity = httpResponse.getEntity();
        return entity != null && entity.isChunked();
    }

    /**
     * Whether to expect a response body.
     */
    public boolean isExpectResponseBody() {
        return status != 204 && status != 304 && status >= 200;
    }

    /**
     * Close the underlying HTTP response (releases the connection back to the pool).
     */
    public void close() {
        try {
            httpResponse.close();
        } catch (Exception ignore) {
            // best effort
        }
    }

    @Override
    public String toString() {
        return "VTTargetResponse{" + status + " " + statusLine + "}";
    }
}
