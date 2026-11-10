package com.smartcommunity.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smartcommunity.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fee_item")
public class FeeItem extends BaseEntity {

    private String itemName;
    private String itemCode;
    private Integer itemType;
    private BigDecimal unitPrice;
    private String unit;
    private Integer calcType;
    private Integer status;
}