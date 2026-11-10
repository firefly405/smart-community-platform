package com.smartcommunity.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartcommunity.common.result.Result;
import com.smartcommunity.entity.GuardAlarm;
import com.smartcommunity.service.GuardAlarmService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@Tag(name = "报警管理")
@RestController
@RequestMapping("/api/v1/alarm")
@RequiredArgsConstructor
public class AlarmController {

    private final GuardAlarmService guardAlarmService;

    @Operation(summary = "报警列表")
    @GetMapping("/list")
    public Result<Page<GuardAlarm>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<GuardAlarm> qw = new LambdaQueryWrapper<>();
        if (status != null) qw.eq(GuardAlarm::getStatus, status);
        qw.orderByDesc(GuardAlarm::getAlarmTime);
        return Result.success(guardAlarmService.page(new Page<>(page, pageSize), qw));
    }

    @Operation(summary = "处理报警")
    @PutMapping("/{id}/handle")
    public Result<Void> handle(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        GuardAlarm alarm = guardAlarmService.getById(id);
        if (alarm == null) return Result.fail("报警记录不存在");

        alarm.setStatus(2); // 已处理
        alarm.setHandleResult(params.get("remark") != null ? params.get("remark").toString() : null);
        alarm.setHandlerId(params.get("handlerId") != null ? Long.valueOf(params.get("handlerId").toString()) : null);
        alarm.setHandlerName(params.get("handlerName") != null ? params.get("handlerName").toString() : null);
        alarm.setHandleTime(LocalDateTime.now());
        guardAlarmService.updateById(alarm);
        return Result.success();
    }
}