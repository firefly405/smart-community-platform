package com.smartcommunity.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.smartcommunity.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("life_service_order")
public class LifeServiceOrder extends BaseEntity {
    private String orderNo;
    private Long userId;
    private Long serviceId;
    private String serviceName;
    private LocalDate appointmentDate;
    private String appointmentTime;
    private String contactName;
    private String contactPhone;
    private String address;
    private BigDecimal amount;
    private Integer status;
    private Long staffId;
    private String staffName;
    private Integer evaluateScore;
    private String evaluateContent;

    /** 展示兼容字段 */
    @TableField(exist = false)
    private String userName;
    @TableField(exist = false)
    private String phone;
    @TableField(exist = false)
    private BigDecimal price;
}
