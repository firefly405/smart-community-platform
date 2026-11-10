package com.smartcommunity.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smartcommunity.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("life_rental")
public class LifeRental extends BaseEntity {
    private String rentalNo;
    private Long ownerId;
    private Long houseId;
    private String title;
    private Integer rentalType;
    private String description;
    private String images;
    private BigDecimal monthlyRent;
    private BigDecimal deposit;
    private String paymentMethod;
    private String contactName;
    private String contactPhone;
    private Integer isRecommend;
    private LocalDateTime publishTime;
    private Integer status;
}