package org.apache.synapse.unittest.mock.services.http.consumer;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import org.apache.synapse.unittest.mock.services.core.EmulatorType;
import org.apache.synapse.unittest.mock.services.http.ChannelPipelineInitializer;
import org.apache.synapse.unittest.mock.services.http.dsl.HttpConsumerContext;

public class HttpEmulatorConsumerInitializer {
    private static final boolean SSL = System.getProperty("ssl") != null;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private HttpConsumerContext consumerContext;

    public HttpEmulatorConsumerInitializer(HttpConsumerContext consumerContext) {
        this.consumerContext = consumerContext;
    }

    public void initialize() throws Exception {
        final SslContext sslCtx = null;
        /*if (SSL) {
            SelfSignedCertificate ssc = new SelfSignedCertificate();
            sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
        } else {
            sslCtx = null;
        }*/
        // Configure the server.
        bossGroup = new NioEventLoopGroup(getCPUCoreSize());
        workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            ChannelPipelineInitializer channelPipelineInitializer = new ChannelPipelineInitializer(sslCtx,
                                                                                                   EmulatorType.HTTP_CONSUMER);
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
