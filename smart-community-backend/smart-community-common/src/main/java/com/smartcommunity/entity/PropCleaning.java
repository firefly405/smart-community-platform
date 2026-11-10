package com.smartcommunity.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smartcommunity.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("prop_cleaning")
public class PropCleaning extends BaseEntity {

    private Long cleanerId;
    private String cleanerName;
    private String area;
    private LocalDate taskDate;
    private Integer taskType;
    private String taskContent;
    private String images;
    private Integer qualityScore;
    private Integer status;
    private LocalDateTime checkTime;
}