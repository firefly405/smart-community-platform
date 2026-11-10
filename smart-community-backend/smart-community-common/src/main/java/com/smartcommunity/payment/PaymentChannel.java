package com.smartcommunity.payment;

/**
 * 支付通道类型。
 * <p>当前仅实现 MOCK；后续可扩展 WECHAT / ALIPAY 等真实通道，
 * 业务侧通过 {@link PaymentGateway} 接入，避免再把“假装支付成功”散落在 Controller。
 */
public enum PaymentChannel {
    /** 本地模拟支付（演示/开发专用，非真实资金） */
    MOCK,
    /** 预留：真实支付通道（尚未实现） */
    REAL
}
