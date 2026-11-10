package com.smartcommunity.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartcommunity.common.result.Result;
import com.smartcommunity.entity.PropNotice;
import com.smartcommunity.service.PropNoticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "公告管理")
@RestController
@RequestMapping("/api/v1/notice")
@RequiredArgsConstructor
public class NoticeController {

    private final PropNoticeService noticeService;

    @GetMapping("/list")
    public Result<Page<PropNotice>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String title) {
        LambdaQueryWrapper<PropNotice> qw = new LambdaQueryWrapper<>();
        if (title != null) qw.like(PropNotice::getTitle, title);
        qw.orderByDesc(PropNotice::getIsTop, PropNotice::getId);
        return Result.success(noticeService.page(new Page<>(page, pageSize), qw));
    }

    @GetMapping("/{id}")
    public Result<PropNotice> getById(@PathVariable Long id) {
        return Result.success(noticeService.getById(id));
    }

    @PostMapping
    public Result<Void> create(@RequestBody PropNotice notice) {
        noticeService.save(notice);
        return Result.success();
    }

    @PutMapping
    public Result<Void> update(@RequestBody PropNotice notice) {
        noticeService.updateById(notice);
        return Result.success();
    }

    @PutMapping("/{id}/publish")
    public Result<Void> publish(@PathVariable Long id) {
        noticeService.publish(id);
        return Result.success();
    }

    @PutMapping("/{id}/revoke")
    public Result<Void> revoke(@PathVariable Long id) {
        noticeService.revoke(id);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        noticeService.removeById(id);
        return Result.success();
    }
}