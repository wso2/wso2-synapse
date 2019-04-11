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

import io.netty.handler.codec.http.HttpMethod;
import org.apache.synapse.commons.emulator.http.dsl.dto.Cookie;
import org.apache.synapse.commons.emulator.http.dsl.dto.Header;
import org.apache.synapse.commons.emulator.http.dsl.dto.QueryParameter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class IncomingMessage {

    private static IncomingMessage incoming;
    private String path;
    private HttpMethod method;
    private String body;
    private List<Header> headers;
    private List<QueryParameter> queryParameters;
    private List<Cookie> cookies;


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
        if (headers == null) {
            this.headers = new ArrayList<Header>();
        }
        Header header = new Header(name, value);
        this.headers.add(header);
        return this;
    }

    public IncomingMessage withHeaders(Header... headers) {
        this.headers = Arrays.asList(headers);
        return this;
    }

    public IncomingMessage withQueryParameter(String name, String value) {
        if (queryParameters == null) {
            this.queryParameters = new ArrayList<QueryParameter>();
        }
        this.queryParameters.add(new QueryParameter(name, value));
        return this;
    }

    public IncomingMessage withQueryParameters(QueryParameter... queryParameters) {
        this.queryParameters = Arrays.asList(queryParameters);
        return this;
    }

    public IncomingMessage withCookie(String name, String value) {
        if (cookies == null) {
            this.cookies = new ArrayList<Cookie>();
        }
        this.cookies.add(new Cookie(name, value));
        return this;
    }

    public IncomingMessage withCookies(Cookie... cookies) {
        this.cookies = Arrays.asList(cookies);
        return this;
    }

    public List<Cookie> getCookies() {
        return cookies;
    }

    public List<Header> getHeaders() {
        return headers;
    }

    public String getPath() {
        return path;
    }

    public HttpMethod getMethod() {
        return method;
    }

    public String getBody() {
        return body;
    }

    public List<QueryParameter> getQueryParameters() {
        return queryParameters;
    }
}
