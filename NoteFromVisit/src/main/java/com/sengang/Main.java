package com.sengang;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.web.bind.annotation.RequestMapping;

@SpringBootApplication
@MapperScan("com.sengang.mapper")//扫描包
@EnableAspectJAutoProxy(exposeProxy = true)
public class Main {
    public static void main(String[] args) {
        SpringApplication.run(Main.class,args);
    }
}