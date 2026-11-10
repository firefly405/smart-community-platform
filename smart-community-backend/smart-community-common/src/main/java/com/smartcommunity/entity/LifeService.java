package com.smartcommunity.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smartcommunity.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("life_service")
public class LifeService extends BaseEntity {
    private String serviceName;
    private Integer serviceType;
    private BigDecimal price;
    private String unit;
    private String description;
    private String serviceImage;
    private Integer isOnSale;
    private Integer staffCount;
    private Integer sort;
}