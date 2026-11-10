package com.smartcommunity.payment;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 统一支付结果。业务编排层仅在 {@link #isSuccess()} 为 true 时落业务账。
 */
@Data
@Builder
public class PaymentResult {

    private boolean success;
    private String message;

    /** 实际使用的通道 */
    private PaymentChannel channel;

    /** 是否模拟支付（true 表示无真实资金流转） */
    private boolean simulated;

    /** 通道侧交易号 */
    private String tradeNo;

    /** 实际支付金额 */
    private BigDecimal amount;

    /** 回传支付方式 */
    private Integer payMethod;

    private LocalDateTime paidAt;

    public static PaymentResult fail(String message) {
        return PaymentResult.builder()
                .success(false)
                .message(message)
                .channel(PaymentChannel.MOCK)
                .simulated(true)
                .build();
    }

    public static PaymentResult mockSuccess(String tradeNo, BigDecimal amount, Integer payMethod) {
        return PaymentResult.builder()
                .success(true)
                .message("模拟支付成功（非真实资金）")
                .channel(PaymentChannel.MOCK)
                .simulated(true)
                .tradeNo(tradeNo)
                .amount(amount)
                .payMethod(payMethod)
                .paidAt(LocalDateTime.now())
                .build();
    }
}
