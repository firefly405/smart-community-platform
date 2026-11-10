package com.smartcommunity.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.smartcommunity.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("prop_repair")
public class PropRepair extends BaseEntity {

    private String repairNo;
    private Long ownerId;
    private Long houseId;
    private Integer repairType;
    private Integer urgency;
    private String title;
    private String content;
    private String images;
    private String contactName;
    private String contactPhone;
    private LocalDate appointmentDate;
    private String appointmentTime;
    private Integer status;
    private Long handlerId;
    private String handlerName;
    private String handleResult;
    private String handleImages;
    private LocalDateTime handleTime;
    private LocalDateTime finishTime;
    private Integer evaluateScore;
    private String evaluateContent;
    private LocalDateTime evaluateTime;

    /** 展示字段：业主姓名（非表字段） */
    @TableField(exist = false)
    private String ownerName;
    /** 展示字段：房号（非表字段） */
    @TableField(exist = false)
    private String houseNo;
}
