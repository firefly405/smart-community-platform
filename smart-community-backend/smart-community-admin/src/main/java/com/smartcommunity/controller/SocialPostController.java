package com.smartcommunity.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartcommunity.common.result.Result;
import com.smartcommunity.entity.SocialPost;
import com.smartcommunity.entity.SysUser;
import com.smartcommunity.service.SocialPostService;
import com.smartcommunity.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Tag(name = "社区互动-帖子")
@RestController
@RequestMapping("/api/v1/post")
@RequiredArgsConstructor
public class SocialPostController {

    private final SocialPostService postService;
    private final SysUserService userService;

    @GetMapping("/list")
    public Result<Page<SocialPost>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String category) {
        LambdaQueryWrapper<SocialPost> qw = new LambdaQueryWrapper<>();
        if (title != null) qw.like(SocialPost::getTitle, title);
        if (category != null) qw.eq(SocialPost::getCategory, category);
        qw.orderByDesc(SocialPost::getIsTop, SocialPost::getId);
        Page<SocialPost> result = postService.page(new Page<>(page, pageSize), qw);
        fillUserNames(result.getRecords());
        return Result.success(result);
    }

    @GetMapping("/{id}")
    public Result<SocialPost> getById(@PathVariable Long id) {
        SocialPost post = postService.getById(id);
        if (post != null) fillUserNames(List.of(post));
        return Result.success(post);
    }

    @PostMapping
    public Result<Void> create(@RequestBody SocialPost entity) {
        postService.save(entity);
        return Result.success();
    }

    @PutMapping
    public Result<Void> update(@RequestBody SocialPost entity) {
        postService.updateById(entity);
        return Result.success();
    }

    @PutMapping("/{id}/like")
    public Result<Void> like(@PathVariable Long id) {
        SocialPost post = postService.getById(id);
        if (post != null) {
            post.setLikeCount(post.getLikeCount() == null ? 1 : post.getLikeCount() + 1);
            postService.updateById(post);
        }
        return Result.success();
    }

    @PutMapping("/{id}/top")
    public Result<Void> top(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        SocialPost post = postService.getById(id);
        if (post != null && body != null) {
            post.setIsTop(body.getOrDefault("isTop", 1));
            postService.updateById(post);
        }
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        postService.removeById(id);
        return Result.success();
    }

    private void fillUserNames(List<SocialPost> posts) {
        List<Long> userIds = posts.stream()
                .map(SocialPost::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (userIds.isEmpty()) return;

        Map<Long, SysUser> users = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity()));
        posts.forEach(post -> {
            SysUser user = users.get(post.getUserId());
            if (user == null) {
                post.setUserName("未知用户");
            } else {
                post.setUserName(user.getRealName() == null || user.getRealName().isBlank()
                        ? user.getUsername() : user.getRealName());
            }
        });
    }
}
