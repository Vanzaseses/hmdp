package com.sengang.listener;
import com.sengang.entity.VoucherOrder;
import com.sengang.service.VoucherOrderService;
import com.sengang.utils.MqConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class seckillVoucherListener {
    private final VoucherOrderService voucherOrderService;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(MqConstants.SECKILL_ORDER_QUEUE_NAME),
            exchange = @Exchange(MqConstants.SECKILL_EXCHANGE_NAME),
            key = {MqConstants.SECKILL_ORDER_KEY}
    ))
    public void saveOrder(VoucherOrder voucherOrder){
        voucherOrderService.handleVoucherOrder(voucherOrder);
    }
}
