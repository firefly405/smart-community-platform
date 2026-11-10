package com.smartcommunity.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smartcommunity.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("guard_access")
public class GuardAccess extends BaseEntity {

    private String accessNo;
    private String accessName;
    private Integer accessType;
    private String location;
    private String deviceSn;
    private String ipAddress;
    private Integer status;
    private LocalDate installDate;
}