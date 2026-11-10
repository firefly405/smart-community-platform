package com.smartcommunity.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("sys_log")
public class SysLog implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private String username;
    private String operation;
    private String method;
    private String url;
    private String ip;
    private String location;
    private String params;
    private String result;
    private Integer status;
    private String errorMsg;
    private Integer executeTime;
    private LocalDateTime createTime;
}