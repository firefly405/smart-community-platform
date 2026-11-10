package com.smartcommunity.payment;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 支付模块配置。
 * <pre>
 * payment:
 *   mode: mock          # mock | real
 *   mock:
 *     enabled: true     # false 时拒绝一切模拟支付（可用于预发/生产）
 *     trade-no-prefix: MOCK
 *     default-pay-method: 9
 * </pre>
 */
@Data
@ConfigurationProperties(prefix = "payment")
public class PaymentProperties {

    /**
     * 支付模式：
     * <ul>
     *   <li>mock - 使用本地模拟通道（默认，演示环境）</li>
     *   <li>real - 使用真实支付通道（尚未实现，启用将拒绝支付）</li>
     * </ul>
     */
    private String mode = "mock";

    private Mock mock = new Mock();

    public boolean isMockMode() {
        return "mock".equalsIgnoreCase(mode);
    }

    public boolean isRealMode() {
        return "real".equalsIgnoreCase(mode);
    }

    @Data
    public static class Mock {
        /** 是否允许模拟支付。生产环境应设为 false。 */
        private boolean enabled = true;
        /** 模拟交易号前缀 */
        private String tradeNoPrefix = "MOCK";
        /**
         * 模拟支付默认 payMethod 编码。
         * 9 = 模拟支付（与业务侧 1微信/2支付宝/3现金 等区分）。
         */
        private Integer defaultPayMethod = 9;
    }
}
