package com.smartcommunity.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartcommunity.entity.SysKnowledgeDoc;
import com.smartcommunity.mapper.SysKnowledgeDocMapper;
import com.smartcommunity.service.KnowledgeDocService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeDocServiceImpl extends ServiceImpl<SysKnowledgeDocMapper, SysKnowledgeDoc>
        implements KnowledgeDocService {

    private final ObjectProvider<SimpleVectorStore> vectorStoreProvider;

    @Override
    public IPage<SysKnowledgeDoc> page(int page, int pageSize, String title, String category) {
        LambdaQueryWrapper<SysKnowledgeDoc> qw = new LambdaQueryWrapper<>();
        qw.eq(SysKnowledgeDoc::getStatus, 1);
        if (title != null) qw.like(SysKnowledgeDoc::getTitle, title);
        if (category != null) qw.eq(SysKnowledgeDoc::getCategory, category);
        qw.orderByDesc(SysKnowledgeDoc::getCreateTime);
        return baseMapper.selectPage(new Page<>(page, pageSize), qw);
    }

    @Override
    public SysKnowledgeDoc getById(Long id) {
        return baseMapper.selectById(id);
    }

    @Override
    public boolean save(SysKnowledgeDoc doc) {
        return baseMapper.insert(doc) > 0;
    }

    @Override
    public void update(SysKnowledgeDoc doc) {
        baseMapper.updateById(doc);
    }

    @Override
    public void delete(Long id) {
        baseMapper.deleteById(id);
    }

    @Override
    public void reloadVectorStore() {
        SimpleVectorStore vectorStore = vectorStoreProvider.getIfAvailable();
        if (vectorStore == null) {
            throw new IllegalStateException("AI服务未启用，跳过知识库向量索引重建");
        }

        log.info("开始重建向量索引...");
        List<SysKnowledgeDoc> docs = baseMapper.selectList(
                new LambdaQueryWrapper<SysKnowledgeDoc>().eq(SysKnowledgeDoc::getStatus, 1));
        List<Document> documents = docs.stream()
                .map(d -> new Document(d.getTitle() + "\n" + d.getContent()))
                .toList();
        if (!documents.isEmpty()) {
            vectorStore.add(documents);
        }
        log.info("向量索引重建完成，共处理 {} 条知识文档", documents.size());
    }
}
