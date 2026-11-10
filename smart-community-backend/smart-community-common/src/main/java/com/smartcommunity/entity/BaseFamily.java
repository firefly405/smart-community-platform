package com.smartcommunity.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smartcommunity.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("base_family")
public class BaseFamily extends BaseEntity {

    private Long ownerId;
    private String realName;
    private String phone;
    private String idCard;
    private String relation;
    private Integer gender;
    private Integer status;
}