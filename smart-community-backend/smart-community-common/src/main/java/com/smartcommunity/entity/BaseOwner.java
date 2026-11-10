package com.smartcommunity.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smartcommunity.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("base_owner")
public class BaseOwner extends BaseEntity {

    private Long userId;
    private String realName;
    private String phone;
    private String idCard;
    private Integer gender;
    private LocalDate birthday;
    private String nation;
    private String nativePlace;
    private String occupation;
    private String workUnit;
    private String email;
    private String emergencyContact;
    private String emergencyPhone;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer status;
    private String tags;
}