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
@TableName("base_house")
public class BaseHouse extends BaseEntity {

    private Long buildingId;
    private Long unitId;
    private String houseNo;
    private Integer floor;
    private String houseType;
    private BigDecimal area;
    private BigDecimal innerArea;
    private String orientation;
    private Integer status;
    private Integer propertyType;
    private Long ownerId;
    private LocalDate deliveryDate;

    /** 展示字段：楼栋名称 */
    @TableField(exist = false)
    private String buildingName;
    /** 展示字段：单元号 */
    @TableField(exist = false)
    private String unitName;
    /** 展示字段：业主姓名 */
    @TableField(exist = false)
    private String ownerName;
}
