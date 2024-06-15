package com.sengang.service.impl;

import cn.hutool.cache.Cache;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sengang.dto.Result;
import com.sengang.entity.Shop;
import com.sengang.service.ShopService;
import com.sengang.mapper.ShopMapper;
import com.sengang.utils.CacheUtils;
import com.sengang.utils.RedisData;
import com.sengang.utils.SystemConstants;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.sengang.utils.RedisConstants.*;

/**
* @author SenGang
* @description 针对表【tb_shop】的数据库操作Service实现
* @createDate 2024-04-08 15:22:44
*/
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop>
    implements ShopService{
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private CacheUtils cacheUtils;

    @Override
    public Result queryById(Long id) {
        //用自己写的工具类解决缓存穿透
//        Shop shop = cacheUtils.queryWithPassThrough(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);
        //用自己写的工具类解决缓存击穿
        Shop shop = cacheUtils.queryWithLogicalExpire(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);

        //缓存穿透
//        Shop shop = queryWithPassThrough(id);

        //互斥锁解决缓存击穿
//        Shop shop = queryWithMutex(id);

        //用逻辑过期解决缓存击穿
//        Shop shop = queryWithLogicalExpire(id);

        if(shop==null)
            return Result.fail("商铺不存在");

        return Result.ok(shop);
    }

    /***
     * @description: TODO :用互斥锁解决缓存击穿,redis中setnx指令,锁名为商铺id
     * @params: [id]
     * @return: com.sengang.entity.Shop
     * @author: SenGang
     */
    public Shop queryWithMutex(Long id){
        // 1. 从redis中获得商铺缓存
        String shopJson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);
        // 2. 如果不为空就返回
        if(StrUtil.isNotBlank(shopJson)){
            //JSON转对象
            return JSONUtil.toBean(shopJson, Shop.class);
        }
        //如果为""就直接返回错误，说明是空字符串;如果为null说明redis也没有存空字符串
        if(shopJson!=null)
            return null;

        // 3. 为null就查数据库,热点key查询时间很长
        // 3.1 加锁，拿一个店铺id的锁
        Shop shop = null;
        try {
            boolean flag = tryLock(LOCK_SHOP_KEY + id);
            // 3.2 锁没拿到
            if(!flag){
                //休眠50ms重新查询
                Thread.sleep(50);
                //递归调用，取到就返回
                return queryWithMutex(id);
            }
            // 3.3 锁拿到了就去数据库查
            shop = getById(id);
//            Thread.sleep(30000);//模拟重建延迟
            // 4. 值为null就将空字符串""值写给redis再报错,TODO：防止缓存穿透
            if(shop==null){
                stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id,"",CACHE_NULL_TTL,TimeUnit.MINUTES);//写入空值,2mins过期
                return null;
            }
            // 5. 不为null也写入redis
            stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id,JSONUtil.toJsonStr(shop),
                    CACHE_SHOP_TTL+ RandomUtil.randomLong(10), TimeUnit.MINUTES);//对象转JSON,并设置超时时间防止与数据库数据不一致 TODO:不同的超时时间防止缓存雪崩
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }finally {
            // 3.4 释放锁
            unLock(LOCK_SHOP_KEY+id);
        }
        return shop;

    }



    /***
     * @description: TODO :修改数据的逻辑是，先改数据库，然后删除缓存
     * @params: [shop]
     * @return: com.sengang.dto.Result
     * @author: SenGang
     */
    @Override
    public Result updateShop(Shop shop) {
        //判断id是否为空
        Long id = shop.getId();
        if(id==null)
            return Result.fail("商铺id不能为空");
        shop.setUpdateTime(new Date());
        //修改数据库
        updateById(shop);
        
        //删除缓存
        stringRedisTemplate.delete(CACHE_SHOP_KEY+id);
        return Result.ok();
    }

    @Override
    public Result queryShopByType(Integer typeId, Integer current, Double x, Double y) {
        if (x==null||y==null){
            // 不需要坐标查询
            // 根据类型分页查询
            Page<Shop> page = query()
                    .eq("type_id", typeId)
                    .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
            // 返回数据
            return Result.ok(page.getRecords());
        }
        //需要坐标查询

        //计算分页参数
        int from = (current-1)*SystemConstants.DEFAULT_PAGE_SIZE;
        int end = current*SystemConstants.DEFAULT_PAGE_SIZE;

        //查询redis TODO:只能查找从0-end的数据,需要截取
        String key = SHOP_GEO_KEY+typeId;
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = stringRedisTemplate.opsForGeo()
                .search(key,
                        GeoReference.fromCoordinate(x, y),//当前用户的位置
                        new Distance(5000),//5km以内
                        RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs().includeDistance().limit(end)
                );
        if (results==null)
            return Result.ok(Collections.emptyList());

        //获得key对应的value和score集
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> list = results.getContent();
        List<Long> ids = new ArrayList<>(list.size());//shop集
        Map<String,Distance>  distanceMap = new HashMap<>(list.size());//distance集,key是shopId
        //截取from-end TODO：skip()
        list.stream().skip(from).forEach(result->{
            String shopId = result.getContent().getName();
            ids.add(Long.valueOf(shopId));//添加
            Distance distance = result.getDistance();
            distanceMap.put(shopId,distance);//添加
        });
        String join = StrUtil.join(",", ids);
        List<Shop> shops = query().in("id", ids).last("order by field (id," + join + ")").list();
        //给每个shop添加距离
        for (Shop shop : shops) {
            shop.setDistance(distanceMap.get(shop.getId().toString()).getValue());
        }
        return Result.ok(shops);
    }

    /***
     * @description: TODO :防止缓存穿透:添加空值到redis和缓存雪崩:随机过期时间 的查询;
     * @params: [id]
     * @return: 查到了返回com.sengang.entity.Shop，没查到返回null
     * @author: SenGang
     */
    @Deprecated//有自己写的工具类了
    public Shop queryWithPassThrough(Long id){
        // 1. 从redis中获得商铺缓存
        String shopJson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);
        // 2. 如果不为空就返回
        if(StrUtil.isNotBlank(shopJson)){
            //JSON转对象
            return JSONUtil.toBean(shopJson, Shop.class);
        }
        //如果为""就直接返回错误，说明是空字符串;如果为null说明redis也没有存空字符串
        if(shopJson!=null)
            return null;
        // 3. 为null就查数据库
        Shop shop = getById(id);
        // 4. 值为null就将空字符串""值写给redis再报错,TODO：防止缓存穿透
        if(shop==null){
            stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id,"",CACHE_NULL_TTL,TimeUnit.MINUTES);//写入空值,2mins过期
            return null;
        }

        // 5. 不为null也写入redis
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id,JSONUtil.toJsonStr(shop),
                CACHE_SHOP_TTL+ RandomUtil.randomLong(10), TimeUnit.MINUTES);//对象转JSON,并设置超时时间防止与数据库数据不一致 TODO:不同的超时时间防止缓存雪崩
        return shop;
    }

    /***
     * @description: TODO :尝试获取互斥锁,redis中setnx指令
     * @params: [lockName]
     * @return: boolean
     * @author: SenGang
     */
    @Deprecated //有自己的上锁工具类了
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
    @Deprecated //有自己的上锁工具类了
    private boolean unLock(String lockName){
        Boolean delete = stringRedisTemplate.delete(lockName);
        //不要直接返回Boolean，拆箱有可能是空指针
        return BooleanUtil.isTrue(delete);
    }

    /***
     * @description: TODO :将热点key拿出数据库并存储进redis,附加逻辑过期时间
     * @params: [id]:key ; [expireSeconds]:逻辑过期时间
     * @return: void
     * @author: SenGang
     */
    public void saveShopToRedis(Long id,Long expireSeconds){
        // 1. 查数据库
        Shop shop = getById(id);
        // 2. 将热点key内容和逻辑过期时间封装
        RedisData redisData = new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));//逻辑过期时间为当前时间+expireSeconds (s)
        // 3.写入redis,object->json,该key永久有效,通过代码判断逻辑过期
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY+id,JSONUtil.toJsonStr(redisData));
    }

}




