package com.smartcommunity.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smartcommunity.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("life_activity")
public class LifeActivity extends BaseEntity {
    private String title;
    private String content;
    private String images;
    private Integer activityType;
    private String location;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime signStartTime;
    private LocalDateTime signEndTime;
    private Integer maxParticipants;
    private Integer currentParticipants;
    private String contactName;
    private String contactPhone;
    private Integer isTop;
    private Integer viewCount;
    private Integer status;
}