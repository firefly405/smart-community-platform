package com.smartcommunity.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.smartcommunity.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("social_post")
public class SocialPost extends BaseEntity {
    private String postNo;
    private String title;
    private String content;
    private String category;
    private String images;
    private Long userId;
    @TableField(exist = false)
    private String userName;
    private String tags;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private Integer isTop;
    private Integer status;
}
