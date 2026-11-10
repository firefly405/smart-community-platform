package com.smartcommunity.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smartcommunity.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_message")
public class SysMessage extends BaseEntity {
    private Long senderId;
    private Long receiverId;
    private Integer msgType;
    private String title;
    private String content;
    private Integer isRead;
    private LocalDateTime readTime;
}