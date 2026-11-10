package com.smartcommunity.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartcommunity.common.result.Result;
import com.smartcommunity.entity.GuardAccessLog;
import com.smartcommunity.service.BizDisplayService;
import com.smartcommunity.service.GuardAccessLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Tag(name = "通行记录管理")
@RestController
@RequestMapping("/api/v1/access/log")
@RequiredArgsConstructor
public class AccessLogController {

    private final GuardAccessLogService guardAccessLogService;
    private final BizDisplayService bizDisplayService;

    @Operation(summary = "通行记录列表")
    @GetMapping("/list")
    public Result<Page<GuardAccessLog>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long accessId) {
        LambdaQueryWrapper<GuardAccessLog> qw = new LambdaQueryWrapper<>();
        if (accessId != null) qw.eq(GuardAccessLog::getAccessId, accessId);
        qw.orderByDesc(GuardAccessLog::getAccessTime);
        Page<GuardAccessLog> result = guardAccessLogService.page(new Page<>(page, pageSize), qw);
        bizDisplayService.fillAccessLogs(result.getRecords());
        return Result.success(result);
    }

    @Operation(summary = "模拟通行写入（演示用）")
    @PostMapping
    public Result<Void> create(@RequestBody GuardAccessLog log) {
        if (log.getAccessTime() == null) {
            log.setAccessTime(LocalDateTime.now());
        }
        if (log.getAccessResult() == null) {
            log.setAccessResult(1);
        }
        if (log.getAccessType() == null) {
            log.setAccessType(1);
        }
        if (log.getAccessDirection() == null) {
            log.setAccessDirection(1);
        }
        if (log.getUserType() == null) {
            log.setUserType(1);
        }
        if (log.getCreateTime() == null) {
            log.setCreateTime(LocalDateTime.now());
        }
        if (log.getAccessId() == null || log.getAccessId() <= 0) {
            return Result.fail("请指定门禁设备ID");
        }
        guardAccessLogService.save(log);
        return Result.success();
    }
}
