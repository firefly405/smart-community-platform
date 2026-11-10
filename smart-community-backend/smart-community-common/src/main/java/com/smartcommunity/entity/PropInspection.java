package com.smartcommunity.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smartcommunity.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("prop_inspection")
public class PropInspection extends BaseEntity {

    private Long planId;
    private Long inspectorId;
    private String inspectorName;
    private LocalDate inspectionDate;
    private String area;
    private String items;
    private String result;
    private String anomalies;
    private String images;
    private BigDecimal gpsLat;
    private BigDecimal gpsLng;
    private LocalDateTime checkTime;
    private Integer status;
}