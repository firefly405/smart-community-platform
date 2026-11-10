package com.smartcommunity.service;

import com.smartcommunity.payment.PaymentResult;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 账单支付业务编排：调用支付网关 → 成功后更新账单并写缴费流水。
 * <p>与 {@link com.smartcommunity.payment} 解耦，Controller 只调本服务。
 */
public interface FeeBillPaymentService {

    /**
     * 发起账单缴费。
     *
     * @param billId    账单 ID
     * @param amount    本次支付金额（null 则按剩余应缴）
     * @param payMethod 支付方式编码（可空，模拟通道会给默认值）
     * @return 支付结果（含 tradeNo、是否模拟）
     */
    PaymentResult pay(Long billId, BigDecimal amount, Integer payMethod);

    /**
     * 处理支付回调并入账。
     * 回调体至少需要 trade_no（或能定位账单的字段）与 amount。
     */
    PaymentResult handleCallback(Map<String, Object> payload);
}
