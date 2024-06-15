package com.sengang.service;

import com.sengang.dto.Result;
import com.sengang.entity.Shop;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author SenGang
* @description 针对表【tb_shop】的数据库操作Service
* @createDate 2024-04-08 15:22:44
*/
public interface ShopService extends IService<Shop> {

    Result queryById(Long id);

    Result updateShop(Shop shop);

    Result queryShopByType(Integer typeId, Integer current, Double x, Double y);
}
