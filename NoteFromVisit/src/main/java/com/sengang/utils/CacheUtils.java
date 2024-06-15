package com.sengang.utils;

import cn.hutool.core.lang.func.Func;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.sengang.entity.Shop;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.sengang.utils.RedisConstants.*;

@Component
@Slf4j
public class CacheUtils {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    //缓存重建线程池
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    /***
     * @description: TODO :将object转为JSONStr并存入redis中的string结构，设置真实过期时间
     * @params: [key, value, time, unit]
     * @return: void
     * @author: SenGang
     */
    public void set(String key, Object value, Long time, TimeUnit unit){
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value),time,unit);
    }

    /***
     * @description: TODO :将object转为JSONStr并存入redis中的string结构，可以设置逻辑过期时间
     * TODO:存进redis的是RedisData对象
     * @params: [key, value, expireSeconds, unit]
     * @return: void
     * @author: SenGang
     */
    public void setWithLogicExpire(String key, Object value, Long expireTime, TimeUnit unit){
        //将逻辑时间和对象封装RedisData
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(expireTime)));//将expireTime+unit单位 转为秒
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }

    /***
     * @description: TODO :可以预防缓存穿透的获得数据的方法
     * @params: [keyPrefix:redis中key前缀, id:在数据库表中的id, type:查询表对应的entity, dbFallBack:查询数据库要调用的函数, time:过期时间, unit：单位]
     * @return: T
     * @author: SenGang
     */
    public <T,ID> T queryWithPassThrough(String keyPrefix, ID id, Class<T> type, Function<ID,T> dbFallBack, Long time, TimeUnit unit){
        //获得key
        String key = keyPrefix+id;
        //获得json
        String json = stringRedisTemplate.opsForValue().get(key);
        //JSON不为空就反序列化并返回
        if(StrUtil.isNotBlank(json)){
            return JSONUtil.toBean(json, type);
        }
        //如果为""就直接返回null TODO:预防缓存穿透
        if(json!=null){
            return null;
        }
        //TODO:通过函数对象来实现对应的数据库查表方法，由用户自己实现并传入。ID是参数，T是返回值
        T t = dbFallBack.apply(id);
        //空就传""  TODO:预防缓存穿透
        if(t==null){
            stringRedisTemplate.opsForValue().set(key,"",RedisConstants.CACHE_NULL_TTL,TimeUnit.MINUTES);
        }

//        set(key,t,time,unit);
        //不为空就传值,默认就是热点key了唉
        setWithLogicExpire(key,t,time,unit);
        return t;
    }
    //TODO :添加逻辑过期时间解决缓存击穿

    /***
     * @description: TODO :防止缓存击穿的查询
     * @params: [keyPrefix:redis中key前缀, id:在数据库表中的id, type:查询表对应的entity, dbFallBack:查询数据库要调用的函数, expireTime:过期时间, unit：单位]
     * @return: T
     * @author: SenGang
     */
    public <T,ID> T queryWithLogicalExpire(String keyPrefix, ID id, Class<T> type, Function<ID,T>dbFallBack,Long expireTime, TimeUnit unit){
        //前缀+id获得key
        String key = keyPrefix+id;
        // 1. 从redis中获得商铺缓存
        String redisDataJson = stringRedisTemplate.opsForValue().get(key);
        // 2. 如果redis为空就返回空
        if(StrUtil.isBlank(redisDataJson)){
            //JSON转对象
            return queryWithPassThrough(keyPrefix, id, type, dbFallBack, expireTime, unit);
        }
        // 3. 命中就查逻辑时间是否过期
        RedisData redisData = JSONUtil.toBean(redisDataJson, RedisData.class);//第一次反序列化得到逻辑时间以及 TODO:JSONObject data的json
        T t = JSONUtil.toBean((JSONObject) redisData.getData(), type);//TODO: 再次反序列化JSONObject 最后得到shop对象
        LocalDateTime logicalTime = redisData.getExpireTime();//获得逻辑时间
        // 3.1 逻辑时间未过期，直接返回shop
        if(logicalTime.isAfter(LocalDateTime.now())){
            return t;
        }
        // 3.2 逻辑时间已过期，尝试获得锁准备查数据库
        boolean isLock = tryLock(LOCK_SHOP_KEY + id);
        try {
            if(isLock){
                // 3.3 如果获得锁 新开一个线程执行查数据库并存进redis的操作,用线程池
                CACHE_REBUILD_EXECUTOR.submit(()->{
                    //查数据库
                    T newT = dbFallBack.apply(id);
                    //封装RedisData并存进redis
                    setWithLogicExpire(key,newT,expireTime,unit);
                });
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            //释放锁
            unLock(LOCK_SHOP_KEY + id);
        }
        //最后也返回旧shop
        return t;
    }



    /***
     * @description: TODO :尝试获取互斥锁,redis中setnx指令
     * @params: [lockName]
     * @return: boolean
     * @author: SenGang
     */
    private boolean tryLock(String lockName){
        Boolean ifAbsent = stringRedisTemplate.opsForValue().setIfAbsent(lockName, "1", LOCK_SHOP_TTL, TimeUnit.MILLISECONDS);//锁持续时间为10ms
        //不要直接返回Boolean，拆箱有可能是空指针
        return BooleanUtil.isTrue(ifAbsent);
    }

    /***
     * @description: TODO ：释放互斥锁
     * @params: [lockName]
     * @return: boolean
     * @author: SenGang
     */
    private boolean unLock(String lockName){
        Boolean delete = stringRedisTemplate.delete(lockName);
        //不要直接返回Boolean，拆箱有可能是空指针
        return BooleanUtil.isTrue(delete);
    }

}
