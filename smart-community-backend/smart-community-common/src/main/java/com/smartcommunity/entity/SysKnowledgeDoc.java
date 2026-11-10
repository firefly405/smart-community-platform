package com.smartcommunity.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smartcommunity.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_knowledge_doc")
public class SysKnowledgeDoc extends BaseEntity {
    private String title;
    private String content;
    private String category;
    private String embeddingId;
    private Integer status;
}
