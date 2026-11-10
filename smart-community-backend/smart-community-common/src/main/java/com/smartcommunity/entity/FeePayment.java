package com.smartcommunity.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("fee_payment")
public class FeePayment implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String paymentNo;
    private Long billId;
    private Long ownerId;
    private Long houseId;
    private BigDecimal payAmount;
    private Integer payMethod;
    private String payTradeNo;
    private Integer payStatus;
    private LocalDateTime payTime;
    private String invoiceUrl;
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 展示字段：业主姓名（非表字段） */
    @TableField(exist = false)
    private String ownerName;
    /** 展示字段：房号（非表字段） */
    @TableField(exist = false)
    private String houseNo;
}
