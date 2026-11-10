package com.smartcommunity.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartcommunity.common.result.Result;
import com.smartcommunity.entity.GuardCamera;
import com.smartcommunity.service.GuardCameraService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "监控设备管理")
@RestController
@RequestMapping("/api/v1/camera")
@RequiredArgsConstructor
public class CameraController {

    private final GuardCameraService guardCameraService;

    @Operation(summary = "监控设备列表")
    @GetMapping("/list")
    public Result<Page<GuardCamera>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<GuardCamera> qw = new LambdaQueryWrapper<>();
        if (status != null) qw.eq(GuardCamera::getStatus, status);
        qw.orderByAsc(GuardCamera::getCameraNo);
        return Result.success(guardCameraService.page(new Page<>(page, pageSize), qw));
    }

    @Operation(summary = "新增监控设备")
    @PostMapping
    public Result<Void> create(@RequestBody GuardCamera camera) {
        guardCameraService.save(camera);
        return Result.success();
    }

    @Operation(summary = "修改监控设备")
    @PutMapping
    public Result<Void> update(@RequestBody GuardCamera camera) {
        guardCameraService.updateById(camera);
        return Result.success();
    }

    @Operation(summary = "删除监控设备")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        guardCameraService.removeById(id);
        return Result.success();
    }

    @Operation(summary = "获取视频流地址")
    @GetMapping("/{id}/stream")
    public Result<String> stream(@PathVariable Long id) {
        GuardCamera camera = guardCameraService.getById(id);
        if (camera == null) return Result.fail("监控设备不存在");
        return Result.success(camera.getStreamUrl());
    }
}