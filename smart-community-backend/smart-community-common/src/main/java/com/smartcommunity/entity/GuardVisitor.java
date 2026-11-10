package com.smartcommunity.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.smartcommunity.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("guard_visitor")
public class GuardVisitor extends BaseEntity {

    private String visitorNo;
    private Long ownerId;
    private String visitorName;
    private String visitorPhone;
    private String visitorIdCard;
    private Integer visitorCount;
    private String plateNo;
    private String visitReason;
    private LocalDateTime visitStartTime;
    private LocalDateTime visitEndTime;
    private String qrcode;
    private Integer status;
    private LocalDateTime approveTime;
    private LocalDateTime checkinTime;
    private LocalDateTime checkoutTime;
    private Integer isBlacklist;

    /** 展示字段：业主姓名（非表字段） */
    @TableField(exist = false)
    private String ownerName;
}
