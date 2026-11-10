package com.smartcommunity.payment;

import com.smartcommunity.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * 本地模拟支付通道。
 * <p><b>明确隔离声明：</b>本类是唯一允许“无真实资金即返回成功”的支付实现。
 * 业务 Controller 不得再自行 setStatus=已支付 来冒充支付成功。
 * <p>仅在 {@code payment.mode=mock}（默认）时生效。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "payment", name = "mode", havingValue = "mock", matchIfMissing = true)
public class MockPaymentGateway implements PaymentGateway {

    private final PaymentProperties properties;

    @Override
    public PaymentChannel channel() {
        return PaymentChannel.MOCK;
    }

    @Override
    public PaymentResult charge(PaymentRequest request) {
        assertMockEnabled();
        if (request == null) {
            return PaymentResult.fail("支付请求不能为空");
        }
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return PaymentResult.fail("支付金额必须大于 0");
        }
        if (request.getBizType() == null || request.getBizId() == null) {
            return PaymentResult.fail("业务单标识不完整");
        }

        String tradeNo = buildTradeNo(request);
        Integer payMethod = request.getPayMethod() != null
                ? request.getPayMethod()
                : properties.getMock().getDefaultPayMethod();

        log.info("[MOCK-PAY] charge ok bizType={} bizId={} bizNo={} amount={} tradeNo={}",
                request.getBizType(), request.getBizId(), request.getBizNo(),
                request.getAmount(), tradeNo);

        return PaymentResult.mockSuccess(tradeNo, request.getAmount(), payMethod);
    }

    @Override
    public PaymentResult handleCallback(Map<String, Object> payload) {
        assertMockEnabled();
        if (payload == null || payload.isEmpty()) {
            return PaymentResult.fail("回调参数为空");
        }

        String tradeNo = stringVal(payload.get("trade_no"), payload.get("tradeNo"), payload.get("payTradeNo"));
        if (tradeNo == null || tradeNo.isBlank()) {
            // 模拟回调允许没有先验 tradeNo 时现场签发
            tradeNo = properties.getMock().getTradeNoPrefix() + "CB"
                    + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        }

        BigDecimal amount = null;
        Object amountObj = payload.get("amount");
        if (amountObj != null) {
            try {
                amount = new BigDecimal(amountObj.toString());
            } catch (Exception e) {
                return PaymentResult.fail("回调金额格式错误");
            }
        }

        Integer payMethod = properties.getMock().getDefaultPayMethod();
        Object pm = payload.get("payMethod");
        if (pm != null) {
            try {
                payMethod = Integer.valueOf(pm.toString());
            } catch (Exception ignored) {
                // keep default
            }
        }

        log.info("[MOCK-PAY] callback accepted tradeNo={} amount={}", tradeNo, amount);
        return PaymentResult.mockSuccess(tradeNo, amount, payMethod);
    }

    private void assertMockEnabled() {
        if (!properties.isMockMode()) {
            throw new BusinessException(503, "当前支付模式不是 mock，无法使用模拟支付通道");
        }
        if (!properties.getMock().isEnabled()) {
            throw new BusinessException(403,
                    "模拟支付已关闭（payment.mock.enabled=false）。生产环境请接入真实支付通道。");
        }
    }

    private String buildTradeNo(PaymentRequest request) {
        String prefix = properties.getMock().getTradeNoPrefix();
        if (prefix == null || prefix.isBlank()) {
            prefix = "MOCK";
        }
        String biz = request.getBizType() != null ? request.getBizType().name() : "UNK";
        String rand = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        return prefix + "-" + biz + "-" + request.getBizId() + "-" + rand;
    }

    private static String stringVal(Object... candidates) {
        for (Object c : candidates) {
            if (c != null && !String.valueOf(c).isBlank()) {
                return String.valueOf(c);
            }
        }
        return null;
    }
}
