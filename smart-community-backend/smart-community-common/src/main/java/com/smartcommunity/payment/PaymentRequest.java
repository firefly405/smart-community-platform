package com.smartcommunity.payment;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 统一支付下单请求。由业务编排层构造，交给 {@link PaymentGateway}。
 */
@Data
@Builder
public class PaymentRequest {

    /** 业务类型 */
    private PaymentBizType bizType;

    /** 业务单 ID（账单 ID / 订单 ID） */
    private Long bizId;

    /** 业务单号（账单号 / 订单号，便于对账展示） */
    private String bizNo;

    /** 支付金额 */
    private BigDecimal amount;

    /**
     * 支付方式（业务自定义编码，如 1=微信 2=支付宝 3=现金 4=模拟）。
     * 模拟通道会在结果中回传，并可能强制标记为模拟方式。
     */
    private Integer payMethod;

    /** 发起人用户 ID（可空） */
    private Long operatorId;

    /** 附加说明 */
    private String remark;
}
