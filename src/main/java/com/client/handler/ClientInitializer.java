package com.client.handler;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author tangj
 * @date 2018/5/6 22:54
 */
@Component
public class ClientInitializer extends ChannelInitializer<SocketChannel> {


    @Autowired
    ClientHandler clientHandler;

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast("encoder",new StringEncoder());
        pipeline.addLast("decoder",new StringDecoder());
        pipeline.addLast("handler",clientHandler);
    }
}
