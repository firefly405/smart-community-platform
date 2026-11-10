package com.smartcommunity.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartcommunity.common.result.Result;
import com.smartcommunity.entity.SysLog;
import com.smartcommunity.service.SysLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Tag(name = "操作日志")
@RestController
@RequestMapping("/api/v1/log")
@RequiredArgsConstructor
public class LogController {

    private final SysLogService logService;

    @Operation(summary = "操作日志列表")
    @GetMapping("/list")
    public Result<Page<SysLog>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String operation,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime beginTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        LambdaQueryWrapper<SysLog> qw = new LambdaQueryWrapper<>();
        if (username != null) qw.like(SysLog::getUsername, username);
        if (operation != null) qw.eq(SysLog::getOperation, operation);
        if (beginTime != null) qw.ge(SysLog::getCreateTime, beginTime);
        if (endTime != null) qw.le(SysLog::getCreateTime, endTime);
        qw.orderByDesc(SysLog::getCreateTime);
        return Result.success(logService.page(new Page<>(page, pageSize), qw));
    }
}
