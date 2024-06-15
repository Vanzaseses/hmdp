package com.sengang.controller;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.sengang.entity.ShopType;
import com.sengang.dto.Result;
import com.sengang.service.ShopTypeService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/shop-type")
public class ShopTypeController {
    @Resource
    private ShopTypeService service;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /***
     * @description: TODO ：返回首页最上边分类清单,添加了redis缓存
     * @params: []
     * @return: com.sengang.dto.Result
     * @author: SenGang
     */
    @GetMapping("list")
    public Result queryList(){
        // 1.先查redis,
        String typeListJson = stringRedisTemplate.opsForValue().get("typeList");
        // 2.redis 中存在直接返回
        if(StrUtil.isNotBlank(typeListJson))
            return Result.ok(JSONUtil.toList(typeListJson,ShopType.class));//String->List
        // 3.不存在查询数据库
        List<ShopType> typeList = service.query().orderByAsc("sort").list();
        // 4.数据库不存在
        if(typeList==null)
            return Result.fail("类型不存在");
        // 5.数据库中存在,加入到redis,List->String
        stringRedisTemplate.opsForValue().set("typeList",JSONUtil.toJsonStr(typeList));
        return Result.ok(typeList);
    }
}
