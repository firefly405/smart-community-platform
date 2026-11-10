package com.smartcommunity.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.smartcommunity.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("social_vote")
public class SocialVote extends BaseEntity {
    private String voteNo;
    private Long userId;
    private String title;
    private String description;
    /** 1-单选 2-多选 */
    private Integer voteType;
    private Integer isAnonymous;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer totalVotes;
    private Integer status;

    /** 创建时选项文本，每行一个 */
    @TableField(exist = false)
    private String options;
    /** 是否多选 0/1（前端兼容） */
    @TableField(exist = false)
    private Integer isMulti;
    @TableField(exist = false)
    private List<SocialVoteOption> optionList;
}
