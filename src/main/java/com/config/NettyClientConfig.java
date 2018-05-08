package com.config;

import com.client.handler.ClientInitializer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class NettyClientConfig {
    @Autowired
    private ClientInitializer clientInitializer;

    @Bean(name = "clientbootstrap")
    public Bootstrap clientBootSrap() throws Exception {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(clientGroup())
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(clientInitializer);
        return bootstrap;
    }

    @Bean(name = "clientGroup", destroyMethod = "shutdownGracefully")
    public NioEventLoopGroup clientGroup() {
        return new NioEventLoopGroup(2);
    }
}
