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
@TableName("base_tenant")
public class BaseTenant extends BaseEntity {

    private Long ownerId;
    private Long houseId;
    private String realName;
    private String phone;
    private String idCard;
    private Integer gender;
    private LocalDate leaseStartDate;
    private LocalDate leaseEndDate;
    private BigDecimal monthlyRent;
    private BigDecimal deposit;
    private Integer status;

    @TableField(exist = false)
    private String ownerName;
    @TableField(exist = false)
    private String houseNo;
}
