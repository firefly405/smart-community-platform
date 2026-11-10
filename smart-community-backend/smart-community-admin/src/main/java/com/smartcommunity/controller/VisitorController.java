package com.smartcommunity.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartcommunity.common.result.Result;
import com.smartcommunity.entity.GuardAccess;
import com.smartcommunity.entity.GuardAccessLog;
import com.smartcommunity.entity.GuardVisitor;
import com.smartcommunity.service.BizDisplayService;
import com.smartcommunity.service.GuardAccessLogService;
import com.smartcommunity.service.GuardAccessService;
import com.smartcommunity.service.GuardVisitorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Tag(name = "访客管理")
@RestController
@RequestMapping("/api/v1/visitor")
@RequiredArgsConstructor
public class VisitorController {

    private final GuardVisitorService guardVisitorService;
    private final GuardAccessLogService accessLogService;
    private final GuardAccessService guardAccessService;
    private final BizDisplayService bizDisplayService;

    @Operation(summary = "访客列表")
    @GetMapping("/list")
    public Result<Page<GuardVisitor>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long ownerId,
            @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<GuardVisitor> qw = new LambdaQueryWrapper<>();
        if (ownerId != null) qw.eq(GuardVisitor::getOwnerId, ownerId);
        if (status != null) qw.eq(GuardVisitor::getStatus, status);
        qw.orderByDesc(GuardVisitor::getCreateTime);
        Page<GuardVisitor> result = guardVisitorService.page(new Page<>(page, pageSize), qw);
        bizDisplayService.fill(result.getRecords(),
                GuardVisitor::getOwnerId, v -> null,
                GuardVisitor::setOwnerName, (v, n) -> {});
        return Result.success(result);
    }

    @Operation(summary = "访客详情")
    @GetMapping("/{id}")
    public Result<GuardVisitor> detail(@PathVariable Long id) {
        GuardVisitor visitor = guardVisitorService.getById(id);
        if (visitor == null) return Result.fail("访客记录不存在");
        bizDisplayService.fill(java.util.List.of(visitor), GuardVisitor::getOwnerId, v -> null,
                GuardVisitor::setOwnerName, (v, n) -> {});
        return Result.success(visitor);
    }

    @Operation(summary = "新增访客")
    @PostMapping
    public Result<Void> create(@RequestBody GuardVisitor visitor) {
        visitor.setVisitorNo("VIS" + UUID.randomUUID().toString().replace("-", "").substring(0, 24));
        visitor.setStatus(0); // 待审核
        guardVisitorService.save(visitor);
        return Result.success();
    }

    @Operation(summary = "审核访客")
    @PutMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        GuardVisitor visitor = guardVisitorService.getById(id);
        if (visitor == null) return Result.fail("访客记录不存在");

        boolean approved = Boolean.TRUE.equals(params.get("approved"));
        visitor.setApproveTime(LocalDateTime.now());

        if (approved) {
            visitor.setStatus(1); // 已通过
            visitor.setQrcode("QR" + UUID.randomUUID().toString().replace("-", ""));
        } else {
            visitor.setStatus(2); // 已拒绝
        }
        guardVisitorService.updateById(visitor);
        return Result.success();
    }

    @Operation(summary = "获取访客二维码")
    @GetMapping("/{id}/qrcode")
    public Result<String> qrcode(@PathVariable Long id) {
        GuardVisitor visitor = guardVisitorService.getById(id);
        if (visitor == null || visitor.getQrcode() == null) return Result.fail("二维码不存在");
        return Result.success(visitor.getQrcode());
    }

    @Operation(summary = "验证访客二维码（核销并写通行记录）")
    @PostMapping("/verify")
    public Result<Void> verify(@RequestBody Map<String, Object> params) {
        String qrcode = params.get("qrcode") != null ? params.get("qrcode").toString().trim() : null;
        if (qrcode == null || qrcode.isEmpty()) {
            return Result.fail("请提供访客二维码");
        }

        Object accessIdObj = params.get("accessId");
        if (accessIdObj == null || accessIdObj.toString().isBlank()) {
            return Result.fail("请选择门禁设备");
        }
        Long accessId;
        try {
            accessId = Long.valueOf(accessIdObj.toString());
        } catch (NumberFormatException e) {
            return Result.fail("门禁设备参数无效");
        }
        if (accessId <= 0) {
            return Result.fail("请选择门禁设备");
        }
        GuardAccess device = guardAccessService.getById(accessId);
        if (device == null) {
            return Result.fail("门禁设备不存在");
        }

        LambdaQueryWrapper<GuardVisitor> qw = new LambdaQueryWrapper<>();
        qw.eq(GuardVisitor::getQrcode, qrcode);
        GuardVisitor visitor = guardVisitorService.getOne(qw);
        if (visitor == null) return Result.fail("无效的二维码");
        if (visitor.getStatus() == null || visitor.getStatus() != 1) {
            return Result.fail("访客未通过审核或已过期");
        }
        if (visitor.getVisitEndTime() != null && LocalDateTime.now().isAfter(visitor.getVisitEndTime())) {
            return Result.fail("通行时间已过期");
        }

        LocalDateTime now = LocalDateTime.now();
        visitor.setStatus(3); // 已签到
        visitor.setCheckinTime(now);
        guardVisitorService.updateById(visitor);

        // 数据流通：核销写入通行记录，便于通行记录模块展示可读名称
        GuardAccessLog log = new GuardAccessLog();
        log.setAccessId(accessId);
        log.setUserId(visitor.getId());
        log.setUserType(4); // 访客
        log.setAccessType(4); // 二维码
        log.setAccessDirection(1); // 进
        log.setAccessTime(now);
        log.setAccessResult(1);
        String deviceLabel = device.getAccessName() != null && !device.getAccessName().isBlank()
                ? device.getAccessName() : device.getAccessNo();
        log.setRemark("访客核销: " + nullToEmpty(visitor.getVisitorName()) + " / "
                + nullToEmpty(visitor.getVisitorNo()) + " @ " + nullToEmpty(deviceLabel));
        log.setCreateTime(now);
        accessLogService.save(log);

        return Result.success();
    }

    @Operation(summary = "黑名单列表")
    @GetMapping("/blacklist/list")
    public Result<Page<GuardVisitor>> blacklist(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        LambdaQueryWrapper<GuardVisitor> qw = new LambdaQueryWrapper<>();
        qw.eq(GuardVisitor::getIsBlacklist, 1);
        qw.orderByDesc(GuardVisitor::getCreateTime);
        return Result.success(guardVisitorService.page(new Page<>(page, pageSize), qw));
    }

    @Operation(summary = "加入黑名单")
    @PostMapping("/blacklist")
    public Result<Void> addBlacklist(@RequestBody Map<String, Object> params) {
        Long visitorId = params.get("visitorId") != null ? Long.valueOf(params.get("visitorId").toString()) : null;
        if (visitorId != null) {
            GuardVisitor visitor = guardVisitorService.getById(visitorId);
            if (visitor == null) return Result.fail("访客不存在");
            visitor.setIsBlacklist(1);
            guardVisitorService.updateById(visitor);
            return Result.success();
        }

        // 兼容前端按姓名/手机直接加黑
        String phone = params.get("phone") != null ? params.get("phone").toString() : null;
        String name = params.get("name") != null ? params.get("name").toString() : null;
        LambdaQueryWrapper<GuardVisitor> qw = new LambdaQueryWrapper<>();
        if (phone != null && !phone.isBlank()) {
            qw.eq(GuardVisitor::getVisitorPhone, phone);
        } else if (name != null && !name.isBlank()) {
            qw.eq(GuardVisitor::getVisitorName, name);
        } else {
            return Result.fail("请指定访客ID或姓名/手机号");
        }
        GuardVisitor visitor = guardVisitorService.getOne(qw.last("LIMIT 1"));
        if (visitor == null) {
            visitor = new GuardVisitor();
            visitor.setVisitorNo("BLK" + UUID.randomUUID().toString().replace("-", "").substring(0, 24));
            visitor.setVisitorName(name);
            visitor.setVisitorPhone(phone);
            visitor.setVisitorIdCard(params.get("idCard") != null ? params.get("idCard").toString() : null);
            visitor.setStatus(2);
            visitor.setIsBlacklist(1);
            visitor.setVisitReason(params.get("reason") != null ? params.get("reason").toString() : "黑名单");
            guardVisitorService.save(visitor);
            return Result.success();
        }
        visitor.setIsBlacklist(1);
        guardVisitorService.updateById(visitor);
        return Result.success();
    }

    @Operation(summary = "移除黑名单")
    @DeleteMapping("/blacklist/{id}")
    public Result<Void> removeBlacklist(@PathVariable Long id) {
        GuardVisitor visitor = guardVisitorService.getById(id);
        if (visitor == null) return Result.fail("访客不存在");
        visitor.setIsBlacklist(0);
        guardVisitorService.updateById(visitor);
        return Result.success();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
