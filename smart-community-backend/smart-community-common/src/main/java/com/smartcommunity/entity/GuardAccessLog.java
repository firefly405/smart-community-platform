package com.smartcommunity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("guard_access_log")
public class GuardAccessLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long accessId;
    private Long userId;
    private Integer userType;
    private Integer accessType;
    private Integer accessDirection;
    private LocalDateTime accessTime;
    private String accessImage;
    private Integer accessResult;
    private String failReason;
    private String deviceSn;
    private String remark;
    private LocalDateTime createTime;

    /** 展示字段：门禁设备名称 */
    @TableField(exist = false)
    private String accessName;
    /** 展示字段：用户/访客姓名 */
    @TableField(exist = false)
    private String userName;
}
