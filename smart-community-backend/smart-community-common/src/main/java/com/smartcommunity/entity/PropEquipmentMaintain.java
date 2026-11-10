package com.smartcommunity.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smartcommunity.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("prop_equipment_maintain")
public class PropEquipmentMaintain extends BaseEntity {

    private Long equipmentId;
    private Integer maintainType;
    private LocalDate maintainDate;
    private String maintainerName;
    private String maintainContent;
    private String maintainResult;
    private BigDecimal cost;
    private LocalDate nextMaintainDate;
    private Integer status;
}