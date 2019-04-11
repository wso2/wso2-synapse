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

package org.apache.synapse.commons.emulator.http.producer;

import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.cookie.ClientCookieEncoder;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.commons.emulator.http.dsl.HttpProducerContext;
import org.apache.synapse.commons.emulator.http.dsl.dto.Cookie;
import org.apache.synapse.commons.emulator.http.dsl.dto.Header;
import org.apache.synapse.commons.emulator.http.dsl.dto.producer.IncomingMessage;

import java.net.URI;

public class HttpRequestInformationProcessor {
    private static final Log log = LogFactory.getLog(HttpRequestInformationProcessor.class);

    public HttpRequest populateHttpRequest(HttpProducerContext producerContext, IncomingMessage incomingMessage)
            throws Exception {

        String uri = getURI(producerContext.getHost(), producerContext.getPort(), incomingMessage);
        URI requestUri = new URI(uri);
        producerContext.host(requestUri.getHost());
        String scheme = requestUri.getScheme();

        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            log.error("Only HTTP(S) is supported.");
            //Need to log
        }
        HttpRequest request = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1, incomingMessage.getMethod(), requestUri.getRawPath());
        populateHeader(request, requestUri.getHost(), incomingMessage);
        populateCookies(request, incomingMessage);
        return request;
    }

    private void populateHeader(HttpRequest request, String host, IncomingMessage incomingMessage) {
        request.headers().set(HttpHeaders.Names.HOST, host);
        request.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.CLOSE);
        request.headers().set(HttpHeaders.Names.ACCEPT_ENCODING, HttpHeaders.Values.GZIP);
        if (incomingMessage.getBody() != null) {
            request.headers().set(HttpHeaders.Names.CONTENT_LENGTH, incomingMessage.getBody().getBytes()
                    .length);
        }
        if (incomingMessage.getHeaders() != null) {
            for (Header header : incomingMessage.getHeaders()) {
                request.headers().set(header.getName(), header.getValue());
            }
        }
    }

    private void populateCookies(HttpRequest request, IncomingMessage incomingMessage) {
        if (incomingMessage.getCookies() != null) {
            DefaultCookie[] cookies = new DefaultCookie[incomingMessage.getCookies().size()];
            int i = 0;
            for (Cookie cookie : incomingMessage.getCookies()) {
                cookies[i++] = new DefaultCookie(cookie.getName(), cookie.getValue());
            }
            request.headers().set(
                    HttpHeaders.Names.COOKIE,
                    ClientCookieEncoder.STRICT.encode(cookies));
        }
    }


    private String getURI(String host, int port, IncomingMessage incomingMessage) {
        String path = incomingMessage.getPath();
        String uri = host + ":" + port;
        if (path.startsWith("/")) {
            uri = uri + path;
        } else {
            uri = uri + "/" + path;
        }
        return uri;
    }
}
