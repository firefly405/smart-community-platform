package com.smartcommunity.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.smartcommunity.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("base_vehicle")
public class BaseVehicle extends BaseEntity {

    private Long ownerId;
    private Long parkingId;
    private String plateNo;
    private String vehicleType;
    private String brand;
    private String color;
    private Integer status;

    /** 展示字段：业主姓名（非表字段） */
    @TableField(exist = false)
    private String ownerName;

    /** 展示字段：车位编号（非表字段） */
    @TableField(exist = false)
    private String parkingNo;
}
