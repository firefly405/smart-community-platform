package com.smartcommunity.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartcommunity.common.result.Result;
import com.smartcommunity.entity.LifeExpress;
import com.smartcommunity.service.LifeExpressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "生活服务-快递")
@RestController
@RequestMapping("/api/v1/express")
@RequiredArgsConstructor
public class LifeExpressController {

    private final LifeExpressService expressService;

    @GetMapping("/list")
    public Result<Page<LifeExpress>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String trackingNo,
            @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<LifeExpress> qw = new LambdaQueryWrapper<>();
        if (trackingNo != null) qw.like(LifeExpress::getTrackingNo, trackingNo);
        if (status != null) qw.eq(LifeExpress::getStatus, status);
        qw.orderByDesc(LifeExpress::getId);
        return Result.success(expressService.page(new Page<>(page, pageSize), qw));
    }

    @GetMapping("/{id}")
    public Result<LifeExpress> getById(@PathVariable Long id) {
        return Result.success(expressService.getById(id));
    }

    @PostMapping
    public Result<Void> create(@RequestBody LifeExpress entity) {
        expressService.save(entity);
        return Result.success();
    }

    @PutMapping
    public Result<Void> update(@RequestBody LifeExpress entity) {
        expressService.updateById(entity);
        return Result.success();
    }

    @PutMapping("/{id}/pickup")
    public Result<Void> pickup(@PathVariable Long id) {
        LifeExpress express = expressService.getById(id);
        if (express != null) {
            express.setStatus(2);
            expressService.updateById(express);
        }
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        expressService.removeById(id);
        return Result.success();
    }
}