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
