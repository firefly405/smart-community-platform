package com.smartcommunity.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartcommunity.common.result.Result;
import com.smartcommunity.entity.SocialPhone;
import com.smartcommunity.service.SocialPhoneService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "社区互动-便民电话")
@RestController
@RequestMapping("/api/v1/phone")
@RequiredArgsConstructor
public class SocialPhoneController {

    private final SocialPhoneService phoneService;

    @GetMapping("/list")
    public Result<Page<SocialPhone>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String category) {
        LambdaQueryWrapper<SocialPhone> qw = new LambdaQueryWrapper<>();
        if (name != null) qw.like(SocialPhone::getName, name);
        if (category != null) qw.eq(SocialPhone::getCategory, category);
        qw.orderByAsc(SocialPhone::getSort);
        return Result.success(phoneService.page(new Page<>(page, pageSize), qw));
    }

    @GetMapping("/{id}")
    public Result<SocialPhone> getById(@PathVariable Long id) {
        return Result.success(phoneService.getById(id));
    }

    @PostMapping
    public Result<Void> create(@RequestBody SocialPhone entity) {
        phoneService.save(entity);
        return Result.success();
    }

    @PutMapping
    public Result<Void> update(@RequestBody SocialPhone entity) {
        phoneService.updateById(entity);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        phoneService.removeById(id);
        return Result.success();
    }
}