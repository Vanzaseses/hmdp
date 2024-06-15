package com.sengang.utils;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.BooleanUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SimpleRedisLock implements ILock{

    private StringRedisTemplate stringRedisTemplate;
    private String lockName;
    private static final String KEY_PREFIX = "lock:";
    private static final String ID_PREFIX = UUID.randomUUID().toString(true)+"-";

    //TODO : 用静态代码块加载lua脚本,类初始化的时候就加载完了
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;
    static {
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
        UNLOCK_SCRIPT.setResultType(Long.class);//设置返回值类型
    }
    /***
     * @description: TODO ：尝试获得锁
     * @params: [expireTime]
     * @return: boolean
     * @author: SenGang
     */
    @Override
    public boolean tryLock(Long expireTime) {
        //获取当前线程ID,用UUID来判断是否来自同一台集群服务器
        String  threadId = ID_PREFIX+Thread.currentThread().getId();

        //获取锁 TODO :存入redis 锁的 key为lock:xxxx,value为当前线程id
        Boolean ifAbsent = stringRedisTemplate.opsForValue().setIfAbsent(KEY_PREFIX + lockName, threadId+"", expireTime, TimeUnit.SECONDS);//相当于set nx ex

        return BooleanUtil.isTrue(ifAbsent);//防止ifAbsent为空时自动拆箱时报空指针异常
    }
    @Override
    public void unLock() {
        //TODO: 通过调用lua脚本,完成判断和释放锁的原子性操作
        stringRedisTemplate.execute(UNLOCK_SCRIPT, Collections.singletonList(KEY_PREFIX+lockName),ID_PREFIX+Thread.currentThread().getId());
    }

/*    @Override
    public void unLock() {
        //获取线程标识
        String threadId = stringRedisTemplate.opsForValue().get(KEY_PREFIX + lockName);
        //判断锁的标识是否是当前线程的锁
        if((ID_PREFIX+Thread.currentThread().getId()).equals(threadId))
            stringRedisTemplate.delete(KEY_PREFIX + lockName);
    }*/
}
