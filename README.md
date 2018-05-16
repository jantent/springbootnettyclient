## 概述
 在实际的生产项目中，尤其是soa架构的系统。会用到网络传输的接口。首先会想到的springboot+netty的方式。springboot不用多说，火的不行，netty在很多实际项目中都有运用，非常好的一个框架，开发非常方便。
本文主要内容是：
1. springboot与netty的整合
2. netty简单的长连接

## pom文件
```
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>1.5.1.RELEASE</version>
        <relativePath/>
    </parent>

    <dependencies>
        <!-- netty -->
        <dependency>
            <groupId>io.netty</groupId>
            <artifactId>netty-all</artifactId>
            <version>4.1.17.Final</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-thymeleaf</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>

        <!-- spring boot 热部署-->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-devtools</artifactId>
            <optional>true</optional>
        </dependency>

    </dependencies>
```

## 服务端的配置'
### 配置文件
首先在application.yml中写入netty服务端依赖的配置.
这里配置了三个参数，一个是服务端的端口，group的线程池的大小。
如下所示：

```
tcp:
  port: 3018
boss:
  thread:
    count: 2
worker:
  thread:
    count: 2
```  
### 配置类

netty和springboot整合的时候，就需要将类加载交给spring的加载器,和平时写service类很类似。
首先先写handler类,这里这是很简单的示范，没有具体的业务实现，一般情况都是都是用json做数据，这里我偷懒了一下。

内容如下：

```
package com.server.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.springframework.stereotype.Component;

@Component
@ChannelHandler.Sharable
public class TcpHandler extends SimpleChannelInboundHandler<String>{

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        System.out.println("服务单收到消息如下："+msg);
        String remsg = "服务端的响应";
        ByteBuf byteBuf = Unpooled.wrappedBuffer(remsg.getBytes("utf-8"));
        ctx.writeAndFlush(byteBuf);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

}

```  
ServerInitializer类，这里集成pipeline
```
package com.server.handler;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ServerInitializer extends ChannelInitializer<SocketChannel>{

    @Autowired
    TcpHandler tcpHandler;

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast("decoder",new StringDecoder());
        pipeline.addLast("encoder",new StringEncoder());
        pipeline.addLast("handler",tcpHandler);
    }
}

```  

下面的内容是重点了，这里主要是利用springboot自带的application.yml配置文件，来记录配置信息。
使用：

```
    @Value("${boss.thread.count}")
    private int bossCount;

```  
的方式，就可以读出配置文件的内容，是不是很方便。
**下面是服务端具体配置的内容：**
```
package com.config;

import com.server.handler.ServerInitializer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetSocketAddress;

/**
 * @author janti
 * netty服务端配置类
 */
@Configuration
public class NettyServerConfig {

    @Value("${boss.thread.count}")
    private int bossCount;

    @Value("${worker.thread.count}")
    private int workerCount;

    // 服务端的端口
    @Value("${tcp.port}")
    private int tcpPort;

    @Autowired
    private ServerInitializer serverInitializer;

    /**
     * 将bootstarp由spring托管
     * @return
     */
    @Bean(name = "serverBootStarp")
    public ServerBootstrap bootstrap() {
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup(), workerGroup())
                .channel(NioServerSocketChannel.class)
                .childHandler(serverInitializer)
                // 保持连接
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true);

        return bootstrap;
    }

    @Bean(name = "bossGroup", destroyMethod = "shutdownGracefully")
    public NioEventLoopGroup bossGroup() {
        return new NioEventLoopGroup(bossCount);
    }

    @Bean(name = "bossGroup", destroyMethod = "shutdownGracefully")
    public NioEventLoopGroup workerGroup() {
        return new NioEventLoopGroup(workerCount);
    }

    @Bean
    public InetSocketAddress tcpPort() {
        return new InetSocketAddress(tcpPort);
    }
}

```  
好的配置完毕了，接下来服务端的启动和关闭了。
这里使用了@PostConstruct和@PreDestroy注解，
从Java EE 5规范开始，Servlet中增加了两个影响Servlet生命周期的注解（Annotion）；@PostConstruct和@PreDestroy。这两个注解被用来修饰一个非静态的void()方法。

启动类：

```
package com.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.net.InetSocketAddress;

@Component
public class NettyTcpServer {

    @Resource
    private ServerBootstrap bootstrap;

    @Resource
    private InetSocketAddress tcpPort;

    private ChannelFuture serverChannelFuture;

    /**
     * 在启动servlet的时候，启动netty
     * @throws Exception
     */
    @PostConstruct
    public void start() throws Exception{
        System.out.printf("netty服务器启动");
        serverChannelFuture = bootstrap.bind(tcpPort);
        serverChannelFuture.channel().closeFuture().sync();
    }

    /**
     * servlet关闭的时候，关闭netty服务
     * @throws Exception
     */
    @PreDestroy
    public void stop() throws Exception{
        serverChannelFuture.channel().closeFuture().sync();
    }
}
```  
好了，接下来启动springboot的启动类，即可以启动netty服务。你可以使用Telnet测试一下。
想要源码的可以戳这里，记得给个star
[服务端的代码](https://github.com/JayTange/springbootnetty.git)

## netty客户端

netty客户端的整合和服务端的是大同小异。这里我直接贴代码了。

handler类
```
package com.client.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.springframework.stereotype.Component;

/**
 * @author janti
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

```  
Initializer类
```
package com.client.handler;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author janti
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

```  
config类：

```
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

```  
启动类：
```
package com.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.UnsupportedEncodingException;

@Component
public class NettyTcpClient {
    @Resource
    private Bootstrap bootstrap;

    @Value("${tcp.port}")
    private int tcpPort;

    @Value(("${tcp.ip}"))
    private String serverIp;

    private Channel channel;

    @PostConstruct
    public void connect() throws Exception {
        // 发起异步连接
        ChannelFuture future = bootstrap.connect(serverIp, tcpPort).sync();
        channel = future.channel();
        System.out.println("客户端连接成功");
        if (!future.isSuccess()) {
            future.cause().printStackTrace();
        }
    }

    public void stop() throws Exception {
        channel.close();
    }

    /**
     * 发送消息 使用方法即可
     * @param msg
     */
    public void sendMessage(String msg) {
        try {
            for (int i = 0; i < 2; i++) {
                ByteBuf byteBuf = Unpooled.wrappedBuffer(msg.getBytes("utf-8"));
                channel.writeAndFlush(byteBuf);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } catch (UnsupportedEncodingException e) {
            System.out.println("数据发送失败");
        }

    }
}

```  
这样写的意图是，使用 @PostConstruct注解connect，在服务启动时，先获取连接并保持连接。
如果需要发送信息，直接使用sendMessage(String msg)即可。
[客户端代码](https://github.com/JayTange/springbootnettyclient.git)
