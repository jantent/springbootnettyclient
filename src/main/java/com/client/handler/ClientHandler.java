package com.client.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.springframework.stereotype.Component;

/**
 * @author tangj
 * @date 2018/5/6 22:59
 */
@Component
public class ClientHandler extends SimpleChannelInboundHandler<String>{
    @Override
    public void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {

        System.out.println("客户端收到消息" + msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
