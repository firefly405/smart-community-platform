package com.smartcommunity.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("social_comment")
public class SocialComment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long postId;
    private Long parentId;
    private String content;
    private Long userId;
    @TableField(exist = false)
    private String userName;
    private Integer status;
    private LocalDateTime createTime;
    private Long createBy;
    @TableLogic(value = "0", delval = "1")
    private Integer deleted;
    private String remark;
}
