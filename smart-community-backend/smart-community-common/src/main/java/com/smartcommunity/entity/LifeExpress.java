package com.smartcommunity.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smartcommunity.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("life_express")
public class LifeExpress extends BaseEntity {
    private String expressNo;
    private Long receiverId;
    private String expressCompany;
    private String trackingNo;
    private String packageInfo;
    private String pickupCode;
    private String shelfNo;
    private LocalDateTime arriveTime;
    private LocalDateTime pickupTime;
    private Integer status;
}