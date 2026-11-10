package com.smartcommunity.payment;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 支付模块自动配置：启用 {@link PaymentProperties}。
 * Gateway 实现通过 {@code @Component} 扫描注册。
 */
@Configuration
@EnableConfigurationProperties(PaymentProperties.class)
public class PaymentAutoConfiguration {
}
