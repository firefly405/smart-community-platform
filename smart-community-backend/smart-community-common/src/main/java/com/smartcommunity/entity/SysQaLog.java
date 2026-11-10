package com.smartcommunity.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smartcommunity.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_qa_log")
public class SysQaLog extends BaseEntity {
    private String question;
    private String answer;
    private String sources;
    private Long userId;
    private String userName;
}
