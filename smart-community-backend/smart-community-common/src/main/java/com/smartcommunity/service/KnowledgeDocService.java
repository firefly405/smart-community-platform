package com.smartcommunity.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.smartcommunity.entity.SysKnowledgeDoc;

public interface KnowledgeDocService {

    IPage<SysKnowledgeDoc> page(int page, int pageSize, String title, String category);

    SysKnowledgeDoc getById(Long id);

    boolean save(SysKnowledgeDoc doc);

    void update(SysKnowledgeDoc doc);

    void delete(Long id);

    void reloadVectorStore();
}
