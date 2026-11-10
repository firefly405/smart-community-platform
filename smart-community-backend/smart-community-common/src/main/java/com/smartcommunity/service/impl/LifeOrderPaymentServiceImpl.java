package com.smartcommunity.service.impl;

import com.smartcommunity.common.exception.BusinessException;
import com.smartcommunity.entity.LifeOrder;
import com.smartcommunity.payment.PaymentBizType;
import com.smartcommunity.payment.PaymentGatewayRouter;
import com.smartcommunity.payment.PaymentRequest;
import com.smartcommunity.payment.PaymentResult;
import com.smartcommunity.service.LifeOrderPaymentService;
import com.smartcommunity.service.LifeOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 生活订单支付编排。支付成功判定只来自支付网关。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LifeOrderPaymentServiceImpl implements LifeOrderPaymentService {

    private final LifeOrderService orderService;
    private final PaymentGatewayRouter paymentGatewayRouter;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentResult pay(Long orderId, Integer payMethod) {
        LifeOrder order = orderService.getById(orderId);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        if (order.getStatus() == null || order.getStatus() != 0) {
            throw new BusinessException(400, "当前状态不可支付");
        }

        BigDecimal amount = order.getPayableAmount() != null
                ? order.getPayableAmount()
                : (order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(400, "订单应付金额无效");
        }

        PaymentResult result = paymentGatewayRouter.charge(PaymentRequest.builder()
                .bizType(PaymentBizType.LIFE_ORDER)
                .bizId(order.getId())
                .bizNo(order.getOrderNo())
                .amount(amount)
                .payMethod(payMethod)
                .build());

        if (!result.isSuccess()) {
            throw new BusinessException(400, result.getMessage() != null ? result.getMessage() : "支付失败");
        }

        order.setStatus(1);
        order.setPayTime(result.getPaidAt() != null ? result.getPaidAt() : LocalDateTime.now());
        order.setPayMethod(result.getPayMethod() != null ? result.getPayMethod() : payMethod);
        order.setPayTradeNo(result.getTradeNo());
        if (result.isSimulated()) {
            String remark = order.getRemark();
            String tag = "[MOCK-PAY]";
            if (remark == null || !remark.contains(tag)) {
                order.setRemark(remark == null || remark.isBlank() ? tag : remark + " " + tag);
            }
        }
        orderService.updateById(order);

        log.info("[LIFE-ORDER-PAY] orderId={} amount={} tradeNo={} simulated={}",
                order.getId(), amount, result.getTradeNo(), result.isSimulated());
        return result;
    }
}
