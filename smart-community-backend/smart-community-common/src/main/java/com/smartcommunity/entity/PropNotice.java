package com.smartcommunity.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smartcommunity.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("prop_notice")
public class PropNotice extends BaseEntity {

    private String noticeNo;
    private String title;
    private String content;
    private Integer noticeType;
    private String coverImage;
    private Integer isTop;
    private Integer isPublish;
    private java.time.LocalDateTime publishTime;
    private String publisher;
    private Integer viewCount;
    private Integer status;
}