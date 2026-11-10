package com.smartcommunity.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smartcommunity.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_sms_log")
public class SysSmsLog extends BaseEntity {
    private String phone;
    private String code;
    private String content;
    private Integer smsType;
    private Integer status;
    private String sendResult;
    private LocalDateTime sendTime;
}