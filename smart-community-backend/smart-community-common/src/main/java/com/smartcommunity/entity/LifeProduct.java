package com.smartcommunity.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smartcommunity.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("life_product")
public class LifeProduct extends BaseEntity {
    private String productName;
    private Long categoryId;
    private BigDecimal price;
    private BigDecimal originPrice;
    private Integer stock;
    private Integer sales;
    private String productImage;
    private String description;
    private Integer isOnSale;
    private Integer isRecommend;
    private Integer sort;
    private Integer status;
}