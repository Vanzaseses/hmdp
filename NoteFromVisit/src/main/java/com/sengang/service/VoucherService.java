package com.sengang.service;

import com.sengang.dto.Result;
import com.sengang.entity.Voucher;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author SenGang
* @description 针对表【tb_voucher】的数据库操作Service
* @createDate 2024-04-08 17:18:36
*/
public interface VoucherService extends IService<Voucher> {

    Result queryVoucherOfShop(Long shopId);

    void addSeckillVoucher(Voucher voucher);

}
