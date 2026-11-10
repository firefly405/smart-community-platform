package com.smartcommunity.payment;

import java.util.Map;

/**
 * 支付网关抽象。
 * <p>业务模块（账单/订单）只依赖本接口，不直接写“改状态=已支付”。
 * 当前默认实现为 {@link MockPaymentGateway}；真实通道实现后通过配置切换。
 */
public interface PaymentGateway {

    /** 当前通道 */
    PaymentChannel channel();

    /**
     * 发起支付。
     * MOCK：同步直接成功并返回交易号。
     * REAL：应返回待支付/跳转信息（未来扩展）。
     */
    PaymentResult charge(PaymentRequest request);

    /**
     * 处理支付回调（通道异步通知）。
     * MOCK：用于演示回调入账；REAL：验签后返回结果。
     */
    PaymentResult handleCallback(Map<String, Object> payload);
}
