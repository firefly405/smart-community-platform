package com.smartcommunity.payment;

import com.smartcommunity.common.exception.BusinessException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 真实支付通道占位实现。
 * <p>仅在 {@code payment.mode=real} 时注册；当前未对接微信/支付宝，
 * 任何调用都会明确失败，避免业务误以为“已接入真实支付”。
 */
@Component
@ConditionalOnProperty(prefix = "payment", name = "mode", havingValue = "real")
public class UnsupportedRealPaymentGateway implements PaymentGateway {

    private static final String MSG =
            "真实支付通道尚未接入。请将 payment.mode 设为 mock 使用模拟支付，或完成微信/支付宝对接后再启用 real。";

    @Override
    public PaymentChannel channel() {
        return PaymentChannel.REAL;
    }

    @Override
    public PaymentResult charge(PaymentRequest request) {
        throw new BusinessException(501, MSG);
    }

    @Override
    public PaymentResult handleCallback(Map<String, Object> payload) {
        throw new BusinessException(501, MSG);
    }
}
