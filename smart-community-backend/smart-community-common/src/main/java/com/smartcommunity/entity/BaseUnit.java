package com.smartcommunity.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smartcommunity.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("base_unit")
public class BaseUnit extends BaseEntity {

    private Long buildingId;
    private String unitNo;
    private Integer floorCount;
}