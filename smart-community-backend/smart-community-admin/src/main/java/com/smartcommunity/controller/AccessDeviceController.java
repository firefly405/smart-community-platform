package com.smartcommunity.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartcommunity.common.result.Result;
import com.smartcommunity.entity.GuardAccess;
import com.smartcommunity.entity.GuardAccessPermission;
import com.smartcommunity.service.GuardAccessPermissionService;
import com.smartcommunity.service.GuardAccessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Tag(name = "门禁设备管理")
@RestController
@RequestMapping("/api/v1/access")
@RequiredArgsConstructor
public class AccessDeviceController {

    private final GuardAccessService guardAccessService;
    private final GuardAccessPermissionService permissionService;

    @Operation(summary = "门禁设备列表")
    @GetMapping("/device/list")
    public Result<Page<GuardAccess>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<GuardAccess> qw = new LambdaQueryWrapper<>();
        if (status != null) qw.eq(GuardAccess::getStatus, status);
        qw.orderByAsc(GuardAccess::getAccessNo);
        return Result.success(guardAccessService.page(new Page<>(page, pageSize), qw));
    }

    @Operation(summary = "新增门禁设备")
    @PostMapping("/device")
    public Result<Void> create(@RequestBody GuardAccess access) {
        guardAccessService.save(access);
        return Result.success();
    }

    @Operation(summary = "修改门禁设备")
    @PutMapping("/device")
    public Result<Void> update(@RequestBody GuardAccess access) {
        guardAccessService.updateById(access);
        return Result.success();
    }

    @Operation(summary = "删除门禁设备")
    @DeleteMapping("/device/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        guardAccessService.removeById(id);
        // 同步清理权限
        permissionService.remove(new LambdaQueryWrapper<GuardAccessPermission>()
                .eq(GuardAccessPermission::getAccessId, id));
        return Result.success();
    }

    @Operation(summary = "查询门禁权限")
    @GetMapping("/{deviceId}/permissions")
    public Result<Map<String, Object>> getPermissions(@PathVariable Long deviceId) {
        List<GuardAccessPermission> list = permissionService.list(
                new LambdaQueryWrapper<GuardAccessPermission>()
                        .eq(GuardAccessPermission::getAccessId, deviceId)
                        .orderByDesc(GuardAccessPermission::getId));
        Map<String, Object> data = new HashMap<>();
        data.put("deviceId", deviceId);
        data.put("userIds", list.stream().map(GuardAccessPermission::getUserId).collect(Collectors.toList()));
        data.put("permissions", list);
        return Result.success(data);
    }

    @Operation(summary = "设置门禁权限")
    @PutMapping("/{deviceId}/permissions")
    @SuppressWarnings("unchecked")
    public Result<Void> setPermissions(@PathVariable Long deviceId, @RequestBody Map<String, Object> params) {
        GuardAccess device = guardAccessService.getById(deviceId);
        if (device == null) {
            return Result.fail("门禁设备不存在");
        }

        List<Long> userIds = new ArrayList<>();
        Object raw = params.get("userIds");
        if (raw instanceof List) {
            for (Object o : (List<?>) raw) {
                if (o == null) continue;
                userIds.add(Long.valueOf(o.toString()));
            }
        }

        LocalDateTime startTime = parseDateTime(params.get("validFrom"), true);
        LocalDateTime endTime = parseDateTime(params.get("validTo"), false);
        Integer userType = params.get("userType") != null
                ? Integer.valueOf(params.get("userType").toString()) : 1;

        // 全量覆盖：先删后写，保证刷新后与前端一致
        permissionService.remove(new LambdaQueryWrapper<GuardAccessPermission>()
                .eq(GuardAccessPermission::getAccessId, deviceId));

        LocalDateTime now = LocalDateTime.now();
        List<GuardAccessPermission> batch = new ArrayList<>();
        for (Long userId : userIds) {
            if (userId == null) continue;
            GuardAccessPermission p = new GuardAccessPermission();
            p.setAccessId(deviceId);
            p.setUserId(userId);
            p.setUserType(userType);
            p.setStartTime(startTime);
            p.setEndTime(endTime);
            p.setCreateTime(now);
            batch.add(p);
        }
        if (!batch.isEmpty()) {
            permissionService.saveBatch(batch);
        }
        return Result.success();
    }

    private LocalDateTime parseDateTime(Object value, boolean startOfDay) {
        if (value == null || value.toString().isBlank()) {
            return null;
        }
        String s = value.toString().trim();
        try {
            if (s.length() <= 10) {
                LocalDate d = LocalDate.parse(s.substring(0, 10));
                return startOfDay ? d.atStartOfDay() : d.atTime(LocalTime.MAX).withNano(0);
            }
            return LocalDateTime.parse(s.replace(" ", "T").substring(0, Math.min(s.length(), 19)));
        } catch (Exception e) {
            return null;
        }
    }
}
