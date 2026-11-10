package com.smartcommunity.payment;

/**
 * 业务支付类型。支付通道只关心业务单标识，不直接耦合具体实体。
 */
public enum PaymentBizType {
    /** 物业账单缴费 */
    FEE_BILL,
    /** 生活服务商品订单 */
    LIFE_ORDER
}
