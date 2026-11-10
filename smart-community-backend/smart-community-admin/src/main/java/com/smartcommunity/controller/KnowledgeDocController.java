package com.smartcommunity.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.smartcommunity.common.result.Result;
import com.smartcommunity.entity.SysKnowledgeDoc;
import com.smartcommunity.service.KnowledgeDocService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "AI知识库管理")
@RestController
@RequestMapping("/api/v1/qa/knowledge")
@RequiredArgsConstructor
public class KnowledgeDocController {

    private final KnowledgeDocService knowledgeDocService;

    @GetMapping("/list")
    public Result<IPage<SysKnowledgeDoc>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String category) {
        return Result.success(knowledgeDocService.page(page, pageSize, title, category));
    }

    @GetMapping("/{id}")
    public Result<SysKnowledgeDoc> getById(@PathVariable Long id) {
        return Result.success(knowledgeDocService.getById(id));
    }

    @Operation(summary = "新增知识文档")
    @PostMapping
    public Result<Void> create(@RequestBody SysKnowledgeDoc doc) {
        knowledgeDocService.save(doc);
        return Result.success();
    }

    @PutMapping
    public Result<Void> update(@RequestBody SysKnowledgeDoc doc) {
        knowledgeDocService.update(doc);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        knowledgeDocService.delete(id);
        return Result.success();
    }

    @Operation(summary = "重建向量索引")
    @PostMapping("/reload")
    public Result<Void> reload() {
        knowledgeDocService.reloadVectorStore();
        return Result.success();
    }
}
