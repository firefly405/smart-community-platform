package com.smartcommunity.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smartcommunity.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("base_building")
public class BaseBuilding extends BaseEntity {

    private String buildingNo;
    private String buildingName;
    private Integer unitCount;
    private Integer floorCount;
}