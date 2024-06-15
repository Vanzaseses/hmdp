package com.sengang.service;

import com.sengang.dto.Result;
import com.sengang.entity.VoucherOrder;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author SenGang
* @description 针对表【tb_voucher_order】的数据库操作Service
* @createDate 2024-04-10 14:48:45
*/
public interface VoucherOrderService extends IService<VoucherOrder> {

    Result seckillVoucher(Long voucherId);

    void createVoucherOrder(VoucherOrder voucherOrder);
    void handleVoucherOrder(VoucherOrder voucherOrder);
}
