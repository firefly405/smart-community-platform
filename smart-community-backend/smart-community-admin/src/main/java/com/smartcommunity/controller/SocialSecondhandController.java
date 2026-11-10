package com.smartcommunity.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartcommunity.common.result.Result;
import com.smartcommunity.entity.SocialSecondhand;
import com.smartcommunity.service.SocialSecondhandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "社区互动-二手交易")
@RestController
@RequestMapping("/api/v1/secondhand")
@RequiredArgsConstructor
public class SocialSecondhandController {

    private final SocialSecondhandService secondhandService;

    @GetMapping("/list")
    public Result<Page<SocialSecondhand>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String category) {
        LambdaQueryWrapper<SocialSecondhand> qw = new LambdaQueryWrapper<>();
        if (title != null) qw.like(SocialSecondhand::getTitle, title);
        if (category != null) qw.eq(SocialSecondhand::getCategory, category);
        qw.orderByDesc(SocialSecondhand::getId);
        return Result.success(secondhandService.page(new Page<>(page, pageSize), qw));
    }

    @GetMapping("/{id}")
    public Result<SocialSecondhand> getById(@PathVariable Long id) {
        return Result.success(secondhandService.getById(id));
    }

    @PostMapping
    public Result<Void> create(@RequestBody SocialSecondhand entity) {
        secondhandService.save(entity);
        return Result.success();
    }

    @PutMapping
    public Result<Void> update(@RequestBody SocialSecondhand entity) {
        secondhandService.updateById(entity);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        secondhandService.removeById(id);
        return Result.success();
    }
}