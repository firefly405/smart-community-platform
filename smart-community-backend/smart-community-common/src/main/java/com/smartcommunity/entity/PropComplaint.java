package com.smartcommunity.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.smartcommunity.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("prop_complaint")
public class PropComplaint extends BaseEntity {

    private String complaintNo;
    private Long ownerId;
    private Long houseId;
    private Integer complaintType;
    private String title;
    private String content;
    private String images;
    private String contactName;
    private String contactPhone;
    private Integer status;
    private Long handlerId;
    private String handlerName;
    private String handleResult;
    private java.time.LocalDateTime handleTime;
    private String replyContent;
    private java.time.LocalDateTime replyTime;
    private Integer evaluateScore;
    private java.time.LocalDateTime evaluateTime;

    /** 展示字段：业主姓名（非表字段） */
    @TableField(exist = false)
    private String ownerName;
    /** 展示字段：房号（非表字段） */
    @TableField(exist = false)
    private String houseNo;
}
