package com.smartcommunity.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smartcommunity.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("guard_camera")
public class GuardCamera extends BaseEntity {

    private String cameraNo;
    private String cameraName;
    private Integer cameraType;
    private String location;
    private String ipAddress;
    private Integer port;
    private String username;
    private String password;
    private String streamUrl;
    private String resolution;
    private Integer status;
    private LocalDate installDate;
}