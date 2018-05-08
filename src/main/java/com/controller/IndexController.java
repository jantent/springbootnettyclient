package com.controller;

import com.client.NettyTcpClient;
import com.domain.bo.RestResponseBo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
public class IndexController {

    @Resource
    NettyTcpClient nettyTcpClient;

    @GetMapping(value = "")
    public String index(){
        return "login";
    }

    @PostMapping("/postnetty")
    @ResponseBody
    public RestResponseBo postNetty(@RequestParam String title,
                                    @RequestParam String author,
                                    HttpServletRequest request,
                                    HttpServletResponse response){
        String msg = title+author;
        nettyTcpClient.sendMessage(msg);
        return RestResponseBo.ok();
    }

}
