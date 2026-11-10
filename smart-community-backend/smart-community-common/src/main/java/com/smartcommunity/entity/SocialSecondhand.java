package com.smartcommunity.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smartcommunity.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("social_secondhand")
public class SocialSecondhand extends BaseEntity {
    private String itemNo;
    private Long userId;
    private String title;
    private String description;
    private String images;
    private Integer category;
    private BigDecimal price;
    private BigDecimal originPrice;
    private Integer conditionLevel;
    private String contactName;
    private String contactPhone;
    private Integer tradeMethod;
    private Integer viewCount;
    private Integer favoriteCount;
    private Integer status;
}