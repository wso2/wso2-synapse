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

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.apache.synapse.commons.emulator.core.EmulatorType;
import org.apache.synapse.commons.emulator.http.ChannelPipelineInitializer;
import org.apache.synapse.commons.emulator.http.dsl.HttpConsumerContext;

public class HttpEmulatorConsumerInitializer {
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private HttpConsumerContext consumerContext;

    public HttpEmulatorConsumerInitializer(HttpConsumerContext consumerContext) {
        this.consumerContext = consumerContext;
    }

    public void initialize() throws InterruptedException{

        // Configure the server.
        bossGroup = new NioEventLoopGroup(getCPUCoreSize());
        workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            ChannelPipelineInitializer channelPipelineInitializer =
                    new ChannelPipelineInitializer(null, EmulatorType.HTTP_CONSUMER);
            channelPipelineInitializer.setConsumerContext(consumerContext);
            serverBootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 100)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(channelPipelineInitializer);
            ChannelFuture f = serverBootstrap.bind(consumerContext.getHost(), consumerContext.getPort())
                    .sync();
            f.channel().closeFuture().sync();


        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public void shutdown() {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }

    private int getCPUCoreSize() {
        return Runtime.getRuntime().availableProcessors();
    }
}
