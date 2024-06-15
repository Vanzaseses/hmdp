package com.sengang.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sengang.dto.Result;
import com.sengang.entity.VoucherOrder;
import com.sengang.service.SeckillVoucherService;
import com.sengang.service.VoucherOrderService;
import com.sengang.mapper.VoucherOrderMapper;
import com.sengang.service.VoucherService;
import com.sengang.utils.MqConstants;
import com.sengang.utils.RabbitMqHelper;
import com.sengang.utils.RedisIdWorker;
import com.sengang.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * @author SenGang
 * @description 针对表【tb_voucher_order】的数据库操作Service实现
 * @createDate 2024-04-10 14:48:45
 */
@Service
@Slf4j
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder>
        implements VoucherOrderService {
    @Resource
    private SeckillVoucherService seckillVoucherService;
    @Resource
    private RedisIdWorker redisIdWorker;
    @Resource
    private VoucherService voucherService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private RedissonClient redissonClient;
    @Resource
    private RabbitMqHelper rabbitMqHelper;
    //lua脚本
    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);//设置返回值类型
    }


    //创建线程池->单线程就行
    private static final ExecutorService SECKILL_ORDER_EXECUTOR = Executors.newSingleThreadExecutor();

    @PostConstruct//在本类初始化以后执行
    private void init() {
        SECKILL_ORDER_EXECUTOR.submit(new VoucherOrderHandler());
    }

    private VoucherOrderService proxy;
    //获取消息队列订单消息
    /***
     * @description: TODO :异步秒杀 1. 通过一个线程不断读取Redis中Stream中的订单信息并写入数据库 2. 用RabbitMQ代替Stream
     * @params:
     * @return:
     * @author: SenGang
     */
    private class VoucherOrderHandler implements Runnable {
        String queueName = "stream.orders";
        @Override
        public void run() {
            while (true) {
                try {
                    //从stream获取订单信息列表, XREADGROUP GROUP g1 c1 COUNT 1 BLOCK 2000 STREAMS stream.orders >
                    List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
                            Consumer.from("g1", "c1"),
                            StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2)),
                            StreamOffset.create(queueName, ReadOffset.lastConsumed())
                    );
                    //判断消息获取是否成功
                    if(list.isEmpty()||list==null){
                        //获取失败
                        continue;
                    }
                    //获得订单信息map
                    MapRecord<String, Object, Object> record = list.get(0);
                    Map<Object, Object> value = record.getValue();//获得订单信息
                    VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(value, new VoucherOrder(), true);
                    //创建订单
                    handleVoucherOrder(voucherOrder);
                    //ACK确认 SACK stream.order g1 id
                    stringRedisTemplate.opsForStream().acknowledge(queueName,"g1",record.getId());
                } catch (Exception e) {
                    //出异常，去pending-list处理
                    log.error("stream订单异常", e);
                    handlePendingList();
                }
            }
        }
        private void handlePendingList(){
            while (true){
                try {
                    // 获得pending-list中订单信息XREADGROUP GROUP g1 c1 COUNT 1 BLOCK 2000 STREAMS stream.orders 0 TODO: 最后不是">"代表读pending-list的第n条开始读
                    List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
                            Consumer.from("g1", "c1"),
                            StreamReadOptions.empty().count(1),
                            StreamOffset.create(queueName, ReadOffset.from("0"))
                    );
                    //判断消息获取是否成功
                    if(list.isEmpty()||list==null){
                        //获取失败,说明pending无消息,pending-list消息处理完了，继续回去处理stream的
                        break;
                    }
                    //获得订单信息map
                    MapRecord<String, Object, Object> record = list.get(0);
                    Map<Object, Object> value = record.getValue();//获得订单信息
                    VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(value, new VoucherOrder(), true);
                    //创建订单
                    handleVoucherOrder(voucherOrder);
                    //ACK确认 SACK stream.order g1 id
                    stringRedisTemplate.opsForStream().acknowledge(queueName,"g1",record.getId());
                } catch (Exception e) {
                    log.error("pending-list异常",e);
                }
            }
        }
    }

    //阻塞队列,阻塞队列没有元素，线程阻塞，有元素就执行
/*    private BlockingQueue<VoucherOrder> orderTasks = new ArrayBlockingQueue<VoucherOrder>(1024 * 1024);
    //内部类,完成线程执行任务->添加mysql订单记录
    private class VoucherOrderHandler implements Runnable {

        @Override
        public void run() {
            while (true) {
                try {
                    //从阻塞队列获取订单信息
                    VoucherOrder voucherOrder = orderTasks.take();
                    //创建订单
                    handleVoucherOrder(voucherOrder);
                } catch (Exception e) {
                    log.error("订单异常", e);
                }
            }
        }
    }*/

    /***
     * @description: TODO :创建数据库订单
     * @params: [voucherOrder]
     * @return: void
     * @author: SenGang
     */

    public void handleVoucherOrder(VoucherOrder voucherOrder) {
        Long userId = voucherOrder.getUserId();
        //获取分布式锁对象,TODO:redisson实现
        RLock lock = redissonClient.getLock("lock:order:" + userId);
        //尝试获取锁
        boolean isLock = lock.tryLock();
        //判断是否获取锁成功
        if (!isLock) {
            //获取锁失败
            log.error("获取锁失败");
            return;
        }
        try {
            // 7.写入数据库
            proxy.createVoucherOrder(voucherOrder);

        } catch (IllegalStateException e) {
            throw new RuntimeException(e);
        } finally {
//            simpleRedisLock.unLock();
            lock.unlock();
        }
    }

    @Override
    public Result seckillVoucher(Long voucherId) {
     // TODO 以下代码用lua脚本替代
        /*// 1.查询优惠券
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
        // 2.判断秒杀是否开始,TODO:根本不需要，因为如果秒杀不在时间范围内前端都不会显示
        boolean isAfter = voucher.getBeginTime().isAfter(LocalDateTime.now());//TODO:比LocalDateTime.now()后返回true,否则返回false
        if(isAfter)
            return Result.fail("秒杀未开始");
        // 3.判断秒杀是否结束 TODO:根本不需要，因为如果秒杀不在时间范围内前端都不会显示
        if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
            return Result.fail("秒杀已结束");
        }
        // 4.判断库存是否足够
        if (voucher.getStock()<1) {
            return Result.fail("库存不足");
        }
*/
        Long userId = UserHolder.getUser().getId();
        Long orderId = redisIdWorker.nextId("order");//即使购买失败也会生成在redis生成订单orderId
        //执行lua脚本 TODO:线程中没有加锁,所有线程都可以执行但是在redis中lua只能顺序执行,因为redis是单线程的
        //result = 0 成功 ；1 库存不足 ；2 用户已下单
        Long result = stringRedisTemplate.execute(SECKILL_SCRIPT, Collections.emptyList(), voucherId.toString(), userId.toString(),orderId.toString());
        int r = result.intValue();//long->int
        //购买失败
        if (r != 0) {
//            redisIdWorker.delPreId("order");//删除刚刚添加的订单数量,有线程安全问题
            return Result.fail(r == 1 ? "库存不足" : "不允许重复购买");
        }
        //购买成功
        //创建订单信息,现在可以 TODO: 通过lua脚本获取
/*
        VoucherOrder voucherOrder = new VoucherOrder();
        voucherOrder.setId(orderId);
        voucherOrder.setUserId(userId);
        voucherOrder.setVoucherId(voucherId);
        //TODO:保存到阻塞队列,！！！异步！！！进行数据库改写
        orderTasks.add(voucherOrder);//添加进阻塞队列
*/
        //获取代理对象给异步线程，实现事务，直接调用方法会使事务失效
        proxy = (VoucherOrderService) AopContext.currentProxy();

        // TODO: 添加到MQ中
        VoucherOrder voucherOrder = new VoucherOrder();
        voucherOrder.setId(orderId);
        voucherOrder.setUserId(userId);
        voucherOrder.setVoucherId(voucherId);
        rabbitMqHelper.sendMessage(MqConstants.SECKILL_EXCHANGE_NAME,MqConstants.SECKILL_ORDER_KEY,voucherOrder);
        return Result.ok(orderId);
        //用userId作为锁，集群会失效
/*        synchronized (userId.toString().intern()){}
* */
        //获得redis锁对象,不可重入
/*        SimpleRedisLock simpleRedisLock = new SimpleRedisLock(stringRedisTemplate,"order:" + userId);
        boolean isLock = simpleRedisLock.tryLock(1200L);*/
        //获得redisson锁，可重入
/*        RLock lock = redissonClient.getLock("lock:order:" + userId);
        boolean isLock = lock.tryLock();
        //判断是否获取锁成功
        if(!isLock) {
            //获取锁失败
            return Result.fail("不允许多机同时购买");
        }
        try {
            //获取代理对象，实现事务，直接调用方法会使事务失效
            VoucherOrderService o = (VoucherOrderService)AopContext.currentProxy();
            // 7.返回订单id
            return o.createVoucherOrder(voucherId);
        } catch (IllegalStateException e) {
            throw new RuntimeException(e);
        } finally {
//            simpleRedisLock.unLock();
            lock.unlock();
        }*/
    }

    /***
     * @description: TODO :加了Aop增强！！事务@Transaction用的是Aop增强！乐观锁CAS解决超卖
     * @params: [voucherId, userId]
     * @author: SenGang
     * @param voucherOrder
     */
    @Transactional
    public void createVoucherOrder(VoucherOrder voucherOrder) {
        Long userId = voucherOrder.getUserId();
        Long voucherId = voucherOrder.getVoucherId();
        //获得买该秒杀券的该用户的数量
        Integer count = query().eq("voucher_id", voucherId).eq("user_id", userId).count();
        //如果大于1，就拒绝购买
        if (count > 0) {
            log.error("用户重复购买");
            return ;
        }
        // 5.扣减库存
        // TODO:乐观锁CAS解决超卖
        boolean success = seckillVoucherService.update()
                .setSql("stock = stock-1")//对应sql -> set stock = stock - 1
                .eq("voucher_id", voucherId)// where voucher_id = #{voucherId}
                .gt("stock", 0)// and stock > 0 TODO：MySQL事务具有隔离性,当stock=1,读后写/写后读 只能有一个事务完成
                .update();
        if (!success){
            //库存不足
            log.error("库存不足");
            return ;
        }
        // 6.创建订单
        save(voucherOrder);
    }
}




