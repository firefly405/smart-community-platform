package com.smartcommunity.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.smartcommunity.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("base_decoration")
public class BaseDecoration extends BaseEntity {

    private Long ownerId;
    private Long houseId;
    private String decorationNo;
    private String companyName;
    private String companyPhone;
    private LocalDate startDate;
    private LocalDate expectEndDate;
    private LocalDate actualEndDate;
    private BigDecimal depositAmount;
    private Integer status;
    private String violationRecord;
    private String checkResult;

    /** 展示字段：业主姓名（非表字段） */
    @TableField(exist = false)
    private String ownerName;
    /** 展示字段：房号（非表字段） */
    @TableField(exist = false)
    private String houseNo;
}
