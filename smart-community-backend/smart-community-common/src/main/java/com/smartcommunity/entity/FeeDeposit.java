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
@TableName("fee_deposit")
public class FeeDeposit extends BaseEntity {

    private String depositNo;
    private Long ownerId;
    private Long houseId;
    private Integer depositType;
    private BigDecimal amount;
    private BigDecimal refundedAmount;
    private Integer status;
    private LocalDateTime receiveTime;

    /** 展示字段：业主姓名（非表字段） */
    @TableField(exist = false)
    private String ownerName;
    /** 展示字段：房号（非表字段） */
    @TableField(exist = false)
    private String houseNo;
}
