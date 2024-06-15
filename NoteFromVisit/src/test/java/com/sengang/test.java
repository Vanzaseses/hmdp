package com.sengang;

import cn.hutool.core.util.StrUtil;
import com.sengang.entity.Shop;
import com.sengang.service.UserService;
import com.sengang.service.impl.ShopServiceImpl;
import com.sengang.utils.RedisConstants;
import com.sengang.utils.RedisIdWorker;
import com.sengang.utils.SystemConstants;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.io.File;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@SpringBootTest
public class test {
    @Resource
    private ShopServiceImpl shopService;
    @Resource
    private RedisIdWorker redisIdWorker;

    private ExecutorService executorService = Executors.newFixedThreadPool(500);

    @Resource
    private UserService userService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Test
    public void test() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(300);
        long current = System.currentTimeMillis();
        for (int i = 0; i < 300; i++) {
            executorService.submit(()->{
                for (int j = 0; j < 100; j++) {
                    Long test = redisIdWorker.nextId("test");
                    System.out.println("id="+test);
                }
                latch.countDown();
            });
        }
        latch.await();
        long after = System.currentTimeMillis();
        System.out.println(after-current);
    }
    @Test
    public void createNewFileName() {
        String originalFilename = "niubi.png";
        // 获取后缀
        String suffix = StrUtil.subAfter(originalFilename, ".", true);
        // 生成目录
        String name = UUID.randomUUID().toString();
        int hash = name.hashCode();
        int d1 = hash & 0xF;
        int d2 = (hash >> 4) & 0xF;
        // 判断目录是否存在
        File dir = new File(SystemConstants.IMAGE_UPLOAD_DIR, StrUtil.format("/blogs/{}/{}", d1, d2));
        if (!dir.exists()) {
            dir.mkdirs();
        }
        // 生成文件名
        String format = StrUtil.format("/test/{}/{}/{}.{}", d1, d2, name, suffix);
    }

    @Test
    public void loadShopData(){
        List<Shop> list = shopService.list();
        //店铺分组,typeId为key,shopId为value,坐标为score
        Map<Long, List<Shop>> map = list.stream().collect(Collectors.groupingBy(Shop::getTypeId));
        for (Map.Entry<Long, List<Shop>> entry : map.entrySet()) {
            //相同店铺类型的店铺列表
            List<Shop> values = entry.getValue();
            Long typeId = entry.getKey();
            String key = RedisConstants.SHOP_GEO_KEY+typeId;
            List<RedisGeoCommands.GeoLocation<String>> locations = new ArrayList<>(values.size());

            //遍历店铺,每个店铺存入redis
            for (Shop value : values) {
                Long id = value.getId();
//                stringRedisTemplate.opsForGeo().add(key.toString(),new Point(value.getX(),value.getY()),value.getId().toString());
                locations.add(new RedisGeoCommands.GeoLocation<>(value.getId().toString(),new Point(value.getX(),value.getY())));
            }
            //存入redis
            stringRedisTemplate.opsForGeo().add(key,locations);
        }
    }
    @Test
    public void testHyperLoglog(){
        String[] values = new String[1000];
        int j = 0;
        for (int i = 0; i < 1000000; i++) {
            j=i%1000;
            values[j]="user_"+i;
            if(j==999)
                stringRedisTemplate.opsForHyperLogLog().add("hl2",values);
        }
        Long size = stringRedisTemplate.opsForHyperLogLog().size("hl2");
        System.out.println("count="+size);
    }
}
