package com.smartcommunity.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.smartcommunity.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fee_bill")
public class FeeBill extends BaseEntity {

    private String billNo;
    private Long ownerId;
    private Long houseId;
    private Long itemId;
    private String itemName;
    private Integer billType;
    private String billPeriod;
    private LocalDateTime billStartDate;
    private LocalDateTime billEndDate;
    private BigDecimal amount;
    private BigDecimal discountAmount;
    private BigDecimal lateFee;
    private BigDecimal payableAmount;
    private BigDecimal paidAmount;
    private Integer status;
    private LocalDateTime payTime;
    private Integer payMethod;
    private String payTradeNo;

    /** 展示字段：业主姓名（非表字段） */
    @TableField(exist = false)
    private String ownerName;
    /** 展示字段：房号（非表字段） */
    @TableField(exist = false)
    private String houseNo;
}
