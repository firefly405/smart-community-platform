package com.smartcommunity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("guard_alarm")
public class GuardAlarm implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String alarmNo;
    private Integer alarmType;
    private Long deviceId;
    private String deviceName;
    private String location;
    private Integer alarmLevel;
    private LocalDateTime alarmTime;
    private String description;
    private Long handlerId;
    private String handlerName;
    private String handleResult;
    private LocalDateTime handleTime;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}