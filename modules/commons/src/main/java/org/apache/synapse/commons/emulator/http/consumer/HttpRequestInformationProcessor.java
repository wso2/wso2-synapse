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

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.util.CharsetUtil;

import java.util.List;
import java.util.Map;

public class HttpRequestInformationProcessor {

    public void process(HttpRequest request, HttpRequestContext requestContext) {
        populateRequestHeaders(request, requestContext);
        populateQueryParameters(request, requestContext);
        populateRequestContext(request, requestContext);
        populateHttpMethod(request, requestContext);
        populateHttpVersion(request, requestContext);
        populateConnectionKeepAlive(request, requestContext);
    }

    private void populateRequestHeaders(HttpRequest request, HttpRequestContext requestContext) {
        HttpHeaders headers = request.headers();
        if (!headers.isEmpty()) {
            for (Map.Entry<String, String> entry : headers.entries()) {
                requestContext.addHeaderParameter(entry.getKey(), entry.getValue());
            }
        }
    }

    void appendDecoderResult(HttpRequestContext requestContext, HttpObject httpObject, ByteBuf content) {
        requestContext.appendResponseContent(content.toString(CharsetUtil.UTF_8));
        DecoderResult result = httpObject.getDecoderResult();
        if (result.isSuccess()) {
            return;
        }
        requestContext.appendResponseContent(result.cause());
    }


    private void populateQueryParameters(HttpRequest request, HttpRequestContext requestContext) {
        QueryStringDecoder queryStringDecoder = new QueryStringDecoder(request.getUri());
        Map<String, List<String>> params = queryStringDecoder.parameters();
        requestContext.setQueryParameters(params);
    }

    private void populateRequestContext(HttpRequest request, HttpRequestContext requestContext) {
        requestContext.setUri(request.getUri());
    }

    private void populateHttpMethod(HttpRequest request, HttpRequestContext requestContext) {
        requestContext.setHttpMethod(request.getMethod());
    }

    private void populateHttpVersion(HttpRequest request, HttpRequestContext requestContext) {
        requestContext.setHttpVersion(request.getProtocolVersion());
    }

    private void populateConnectionKeepAlive(HttpRequest request, HttpRequestContext requestContext) {
        requestContext.setKeepAlive(HttpHeaders.isKeepAlive(request));
    }

}
