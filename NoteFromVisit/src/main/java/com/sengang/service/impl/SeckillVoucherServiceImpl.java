package com.sengang.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sengang.entity.SeckillVoucher;
import com.sengang.service.SeckillVoucherService;
import com.sengang.mapper.SeckillVoucherMapper;
import org.springframework.stereotype.Service;

/**
* @author SenGang
* @description 针对表【tb_seckill_voucher(秒杀优惠券表，与优惠券是一对一关系)】的数据库操作Service实现
* @createDate 2024-04-10 14:20:54
*/
@Service
public class SeckillVoucherServiceImpl extends ServiceImpl<SeckillVoucherMapper, SeckillVoucher>
    implements SeckillVoucherService{

}




