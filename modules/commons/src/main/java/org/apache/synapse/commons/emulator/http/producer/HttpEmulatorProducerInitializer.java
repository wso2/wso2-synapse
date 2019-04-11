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

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.apache.synapse.commons.emulator.core.EmulatorType;
import org.apache.synapse.commons.emulator.http.ChannelPipelineInitializer;
import org.apache.synapse.commons.emulator.http.dsl.HttpProducerContext;
import org.apache.synapse.commons.emulator.http.dsl.dto.producer.IncomingMessage;
import org.apache.synapse.commons.emulator.http.dsl.dto.producer.OutgoingMessage;

import java.util.Map;

public class HttpEmulatorProducerInitializer {

    private HttpProducerContext producerContext;
    private EventLoopGroup group;

    public HttpEmulatorProducerInitializer(HttpProducerContext producerContext) {
        this.producerContext = producerContext;
    }

    public void initialize() throws Exception {

        for (Map.Entry<IncomingMessage, OutgoingMessage> entry : producerContext.getInOutCorrelation()
                .entrySet()) {
            sendMessage(entry.getKey(), entry.getValue());
        }
    }

    private void sendMessage(IncomingMessage incomingMessage, OutgoingMessage outgoingMessage) throws Exception {

        final boolean ssl = "https".equalsIgnoreCase(null);
        final SslContext sslCtx;
        if (ssl) {
            sslCtx = SslContextBuilder.forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE).build();
        } else {
            sslCtx = null;
        }
        HttpRequest httpRequest = new HttpRequestInformationProcessor().populateHttpRequest(producerContext,
                incomingMessage);
        group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            ChannelPipelineInitializer channelPipelineInitializer = new ChannelPipelineInitializer(sslCtx,
                    EmulatorType.HTTP_PRODUCER);
            channelPipelineInitializer.setProducerOutgoingMessage(outgoingMessage);
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(channelPipelineInitializer);

            Channel ch = b.connect(producerContext.getHost(), producerContext.getPort()).sync().channel();
            ch.writeAndFlush(httpRequest);

            ch.closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }

    }

    public void shutdown() {
        group.shutdownGracefully();
    }
}
