package com.sengang.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisConfig {
    @Bean
    public RedissonClient redissonClient(){
        //配置类
        Config config = new Config();
        //配置配置类
        config.useSingleServer().setAddress("redis://192.168.112.128:6379").setPassword("123456");
        //创建Redisson客户端
        return Redisson.create(config);
    }
}
