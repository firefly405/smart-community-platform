package com.smartcommunity.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smartcommunity.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("prop_repair_item")
public class PropRepairItem extends BaseEntity {
    private Long repairId;
    private String itemName;
    private BigDecimal materialCost;
    private BigDecimal laborCost;
    private Integer quantity;
    private BigDecimal totalCost;
    private String remark;
}