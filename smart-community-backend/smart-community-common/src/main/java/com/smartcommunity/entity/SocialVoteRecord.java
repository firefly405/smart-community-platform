package com.smartcommunity.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smartcommunity.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("social_vote_record")
public class SocialVoteRecord extends BaseEntity {
    private Long voteId;
    private Long optionId;
    private Long userId;
    private String userName;
    private LocalDateTime voteTime;
}