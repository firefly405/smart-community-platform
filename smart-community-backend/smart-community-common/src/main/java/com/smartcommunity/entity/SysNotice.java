package com.smartcommunity.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smartcommunity.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_notice")
public class SysNotice extends BaseEntity {
    private String title;
    private String content;
    private Integer noticeType;
    private Long receiverId;
    private Integer isRead;
    private LocalDateTime readTime;
    private Integer status;
}