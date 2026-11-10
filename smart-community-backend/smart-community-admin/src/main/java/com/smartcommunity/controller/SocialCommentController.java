package com.smartcommunity.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartcommunity.common.result.Result;
import com.smartcommunity.entity.SocialComment;
import com.smartcommunity.service.SocialCommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "社区互动-评论")
@RestController
@RequestMapping("/api/v1/comment")
@RequiredArgsConstructor
public class SocialCommentController {

    private final SocialCommentService commentService;

    @GetMapping("/list")
    public Result<Page<SocialComment>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long postId) {
        LambdaQueryWrapper<SocialComment> qw = new LambdaQueryWrapper<>();
        if (postId != null) qw.eq(SocialComment::getPostId, postId);
        qw.orderByDesc(SocialComment::getId);
        return Result.success(commentService.page(new Page<>(page, pageSize), qw));
    }

    @GetMapping("/post/{postId}")
    public Result<List<SocialComment>> getByPost(@PathVariable Long postId) {
        LambdaQueryWrapper<SocialComment> qw = new LambdaQueryWrapper<>();
        qw.eq(SocialComment::getPostId, postId);
        qw.orderByAsc(SocialComment::getId);
        return Result.success(commentService.list(qw));
    }

    @PostMapping
    public Result<Void> create(@RequestBody SocialComment entity) {
        commentService.save(entity);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        commentService.removeById(id);
        return Result.success();
    }
}