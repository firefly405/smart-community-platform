package com.smartcommunity.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartcommunity.common.result.Result;
import com.smartcommunity.entity.SysLoginLog;
import com.smartcommunity.service.SysLoginLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Tag(name = "登录日志")
@RestController
@RequestMapping("/api/v1/login-log")
@RequiredArgsConstructor
public class LoginLogController {

    private final SysLoginLogService loginLogService;

    @Operation(summary = "登录日志列表")
    @GetMapping("/list")
    public Result<Page<SysLoginLog>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime beginTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        LambdaQueryWrapper<SysLoginLog> qw = new LambdaQueryWrapper<>();
        if (username != null) qw.like(SysLoginLog::getUsername, username);
        if (status != null) qw.eq(SysLoginLog::getStatus, status);
        if (beginTime != null) qw.ge(SysLoginLog::getLoginTime, beginTime);
        if (endTime != null) qw.le(SysLoginLog::getLoginTime, endTime);
        qw.orderByDesc(SysLoginLog::getLoginTime);
        return Result.success(loginLogService.page(new Page<>(page, pageSize), qw));
    }
}
