package com.sengang.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sengang.dto.Result;
import com.sengang.entity.Shop;
import com.sengang.service.ShopService;
import com.sengang.utils.SystemConstants;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RequestMapping("/shop")
@RestController
public class ShopController {
    @Resource
    private ShopService shopService;

    /**
     * TODO：根据id查询商铺信息
     * @param id 商铺id
     * @return 商铺详情数据
     */
    @GetMapping("/{id}")
    public Result queryShopById(@PathVariable("id") Long id) {
        //只有在redis中保存了商铺数据缓存才能查得到
        return shopService.queryById(id);
//        return Result.ok(shopService.getById(id));
    }
    /***
     * @description: TODO ：根据id修改商铺信息
     * @params:
     * @return:
     * @author: SenGang
     */
    @PutMapping
    public Result updateShop(@RequestBody Shop shop){

        return shopService.updateShop(shop);
    }
    /**
     * 根据商铺类型分页查询商铺信息,根据坐标的有无传回按照坐标远近排序
     * @param typeId 商铺类型
     * @param current 页码
     * @return 商铺列表
     */
    @GetMapping("/of/type")
    public Result queryShopByType(
            @RequestParam("typeId") Integer typeId,
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam(value = "x",required = false)Double x,
            @RequestParam(value = "y",required = false)Double y
    ) {
        return shopService.queryShopByType(typeId,current,x,y);

    }

    /**
     * 根据商铺名称关键字分页查询商铺信息
     * @param name 商铺名称关键字
     * @param current 页码
     * @return 商铺列表
     */
    @GetMapping("/of/name")
    public Result queryShopByName(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "current", defaultValue = "1") Integer current
    ) {
        // 根据类型分页查询
        Page<Shop> page = shopService.query()
                .like(StrUtil.isNotBlank(name), "name", name)
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 返回数据
        return Result.ok(page.getRecords());
    }

}
