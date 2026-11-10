package com.smartcommunity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("life_activity_join")
public class LifeActivityJoin implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long activityId;
    private Long userId;
    private String userName;
    private String userPhone;
    private Integer participantCount;
    private LocalDateTime signTime;
    private Integer signStatus;
    private LocalDateTime signTimeCheck;
    private String remark;

    /** ???? */
    @TableField(exist = false)
    private LocalDateTime joinTime;
    @TableField(exist = false)
    private Integer status;
}
