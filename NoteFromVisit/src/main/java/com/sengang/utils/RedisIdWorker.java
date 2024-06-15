package com.sengang.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class RedisIdWorker {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    //基准时间戳
    private static final long BEGIN_TIMESTAMP = 1704067200L;

    private final int COUNT_BITS = 32 ;

    //当前线程对应的时间
    private String date;

    /***
     * @description: TODO :利用redis自增长完成全局唯一id，返回值long，8个字节64位
     * @params: [keyPrefix]
     * @return: java.lang.Long
     * @author: SenGang
     */
    public Long nextId(String keyPrefix){
        // TODO: 基本思想 :将long类型的64位分成1位符号+31位时间戳+32位序列号,用位运算进行拼接
        // 1. 生成时间戳，以2024.1.1 00:00:00为基准,TODO:当前时间-基准时间=时间戳
        LocalDateTime now = LocalDateTime.now();
        long curTime = now.toEpochSecond(ZoneOffset.UTC);//将时间用秒转为long
        long timeStamp = curTime - BEGIN_TIMESTAMP;//获得时间戳

        //2. 生成序列号 TODO:序列号key=icr:keyPredix:yyyy:MM:dd,redis用:区分层级,便于统计某天某月的订单
        date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        Long increment = stringRedisTemplate.opsForValue().increment("icr:" + keyPrefix + date);//自增长,value就是实际id的个数,不一定准确

        //3. 用位运算返回序列号 TODO:时间戳左移<<32位,序列号直接或运算
        return timeStamp<<COUNT_BITS|increment;
    }

    public void delPreId(String keyPrefix){
        // 订单添加失败,总数-1
        stringRedisTemplate.opsForValue().increment("icr:" + keyPrefix + date,-1L);
    }
}
