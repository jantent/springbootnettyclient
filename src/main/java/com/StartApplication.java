package com;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan
@SpringBootApplication
public class StartApplication {
    public static void main(String args[]){
        SpringApplication.run(StartApplication.class,args);
    }
}
