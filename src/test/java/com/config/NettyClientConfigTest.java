package com.config;

import com.client.NettyTcpClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

@RunWith(SpringRunner.class)
@SpringBootTest
public class NettyClientConfigTest {

    @Resource
    private NettyTcpClient client;

    @Test
    public void testClient()throws Exception{
        client.connect();
        client.sendMessage("123123");
    }
}