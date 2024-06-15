package com.sengang.utils;

import lombok.Data;

import java.time.LocalDateTime;
/***
 * @description: TODO :用于解决缓存击穿，用逻辑过期时间。保存各种想要存进缓存的对象，避免扩展
 * @params:
 * @return:
 * @author: SenGang
 */

@Data
public class RedisData {
    private LocalDateTime expireTime;//逻辑过期时间
    private Object data;//要存进缓存的对象
}
