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

package org.apache.synapse.commons.emulator.http.dsl.dto.producer;

import io.netty.handler.codec.http.HttpResponseStatus;
import org.apache.synapse.commons.emulator.http.dsl.dto.Header;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OutgoingMessage {

    private static OutgoingMessage outgoing;
    private HttpResponseStatus statusCode;
    private List<Header> headers;
    private String body;

    private static OutgoingMessage getInstance() {
        outgoing = new OutgoingMessage();
        return outgoing;
    }

    public static OutgoingMessage response() {
        return getInstance();
    }

    public OutgoingMessage withStatusCode(HttpResponseStatus statusCode) {
        this.statusCode = statusCode;
        return this;
    }

    public OutgoingMessage withHeader(String name, String value) {
        if (headers == null) {
            this.headers = new ArrayList<Header>();
        }
        headers.add(new Header(name, value));
        return this;
    }

    public OutgoingMessage withHeaders(Header... headers) {
        if (this.headers == null) {
            this.headers = new ArrayList<Header>();
        }

        if (headers != null && headers.length > 0) {
            this.headers.addAll(Arrays.asList(headers));
        }
        return this;
    }

    public OutgoingMessage withBody(String body) {
        this.body = body;
        return this;
    }

    public HttpResponseStatus getStatusCode() {
        return statusCode;
    }

    public List<Header> getHeaders() {
        return headers;
    }

    public String getBody() {
        return body;
    }
}
