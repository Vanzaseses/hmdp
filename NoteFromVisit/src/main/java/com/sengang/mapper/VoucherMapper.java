package com.sengang.mapper;

import com.sengang.entity.Voucher;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

/**
* @author SenGang
* @description 针对表【tb_voucher】的数据库操作Mapper
* @createDate 2024-04-08 17:18:36
* @Entity com.sengang.entity.Voucher
*/
public interface VoucherMapper extends BaseMapper<Voucher> {

    List<Voucher> queryVoucherOfShop(Long shopId);
}




