package com.smartcommunity.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smartcommunity.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("base_parking")
public class BaseParking extends BaseEntity {

    private String parkingNo;
    private Integer parkingType;
    private String area;
    private Integer status;
    private BigDecimal price;
    private BigDecimal monthlyRent;
}