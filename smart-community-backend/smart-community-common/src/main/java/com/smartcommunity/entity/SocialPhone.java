package com.smartcommunity.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smartcommunity.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("social_phone")
public class SocialPhone extends BaseEntity {
    private String name;
    private String phone;
    private String category;
    private String address;
    private String workTime;
    private String description;
    private Integer isRecommend;
    private Integer favoriteCount;
    private Integer sort;
    private Integer status;
}