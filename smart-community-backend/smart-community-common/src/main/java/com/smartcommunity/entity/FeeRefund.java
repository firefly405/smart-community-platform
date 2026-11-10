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
@TableName("fee_refund")
public class FeeRefund extends BaseEntity {

    private String refundNo;
    private Long ownerId;
    private Long depositId;
    private Integer refundType;
    private BigDecimal amount;
    private Integer status;
    private String applyReason;
    private String rejectReason;
    private Long approverId;
    private LocalDateTime approveTime;
    private LocalDateTime refundTime;

    /** 展示字段：业主姓名（非表字段） */
    @TableField(exist = false)
    private String ownerName;
    /** 关联账单ID（费用退款时使用） */
    private Long billId;
}
