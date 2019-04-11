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
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import org.apache.log4j.Logger;
import org.apache.synapse.commons.emulator.http.dsl.HttpConsumerContext;

import static io.netty.handler.codec.http.HttpResponseStatus.CONTINUE;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class HttpResponseProcessHandler extends ChannelInboundHandlerAdapter {
    private static final Logger log = Logger.getLogger(HttpResponseProcessHandler.class);
    private HttpRequestContext httpRequestContext;
    private HttpRequestInformationProcessor httpRequestInformationProcessor;
    private HttpResponseProcessor httpResponseProcessor;
    private HttpConsumerContext consumerContext;

    public HttpResponseProcessHandler(HttpConsumerContext consumerContext) {
        this.consumerContext = consumerContext;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof HttpRequest) {
            readingDelay(consumerContext.getReadingDelay());
            this.httpRequestContext = new HttpRequestContext();
            this.httpRequestInformationProcessor = new HttpRequestInformationProcessor();
            this.httpResponseProcessor = new HttpResponseProcessor(consumerContext);
            HttpRequest httpRequest = (HttpRequest) msg;

            if (HttpHeaders.is100ContinueExpected(httpRequest)) {
                send100Continue(ctx);
            }
            httpRequestInformationProcessor.process(httpRequest, httpRequestContext);
        } else {
            if (msg instanceof HttpContent) {
                HttpContent httpContent = (HttpContent) msg;
                ByteBuf content = httpContent.content();
                if (content.isReadable()) {
                    httpRequestInformationProcessor.appendDecoderResult(httpRequestContext, httpContent, content);
                }
            }

            if (msg instanceof LastHttpContent) {
                ctx.fireChannelReadComplete();
            }
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        if (httpResponseProcessor != null) {
            waitingDelay(consumerContext.getWritingDelay());
            this.httpResponseProcessor.process(httpRequestContext, ctx);
        }
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Exception occurred while processing the response", cause);
        ctx.close();
    }

    private static void send100Continue(ChannelHandlerContext ctx) {
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, CONTINUE);
        ctx.write(response);
    }

    private void readingDelay(int delay) {
        try {
            if (delay > 0) {
                Thread.sleep(delay);
            }
        } catch (InterruptedException e) {
            log.error("Exception occurred while processing the reading delay", e);
        }
    }

    private void waitingDelay(int delay) {
        try {
            if (delay > 0) {
                Thread.sleep(delay);
            }
        } catch (InterruptedException e) {
            log.error("Exception occurred while processing the waiting delay", e);
        }
    }
}
