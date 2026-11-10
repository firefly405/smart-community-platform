package com.smartcommunity.payment;

import com.smartcommunity.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 支付网关路由：按 {@link PaymentProperties#getMode()} 选择通道实现。
 * <p>业务编排只注入本类，不直接依赖 Mock/Real 实现，便于后续扩展。
 */
@Component
@RequiredArgsConstructor
public class PaymentGatewayRouter {

    private final PaymentProperties properties;
    private final List<PaymentGateway> gateways;

    public PaymentGateway current() {
        PaymentChannel expected = properties.isRealMode() ? PaymentChannel.REAL : PaymentChannel.MOCK;
        return gateways.stream()
                .filter(g -> g.channel() == expected)
                .findFirst()
                .orElseThrow(() -> new BusinessException(503,
                        "未找到可用支付通道: mode=" + properties.getMode()
                                + "。当前仅实现 MOCK；real 模式需接入真实网关后才能支付。"));
    }

    public PaymentResult charge(PaymentRequest request) {
        return current().charge(request);
    }

    public PaymentResult handleCallback(Map<String, Object> payload) {
        return current().handleCallback(payload);
    }

    public PaymentChannel currentChannel() {
        return current().channel();
    }

    public boolean isSimulated() {
        return currentChannel() == PaymentChannel.MOCK;
    }
}
