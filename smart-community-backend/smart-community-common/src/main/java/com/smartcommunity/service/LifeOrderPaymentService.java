package com.smartcommunity.service;

import com.smartcommunity.payment.PaymentResult;

/**
 * 生活订单支付业务编排：调用支付网关 → 成功后更新订单状态。
 */
public interface LifeOrderPaymentService {

    /**
     * 支付订单（仅待支付 status=0 可操作）。
     *
     * @param orderId   订单 ID
     * @param payMethod 支付方式（可空）
     */
    PaymentResult pay(Long orderId, Integer payMethod);
}
