package com.smartcommunity.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartcommunity.common.result.Result;
import com.smartcommunity.entity.BaseTenant;
import com.smartcommunity.service.BaseTenantService;
import com.smartcommunity.service.BizDisplayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "租户管理")
@RestController
@RequestMapping("/api/v1/tenant")
@RequiredArgsConstructor
public class TenantController {

    private final BaseTenantService tenantService;
    private final BizDisplayService bizDisplayService;

    @Operation(summary = "租户列表")
    @GetMapping("/list")
    public Result<Page<BaseTenant>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String realName,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) Long houseId,
            @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<BaseTenant> qw = new LambdaQueryWrapper<>();
        if (realName != null) qw.like(BaseTenant::getRealName, realName);
        if (phone != null) qw.like(BaseTenant::getPhone, phone);
        if (houseId != null) qw.eq(BaseTenant::getHouseId, houseId);
        if (status != null) qw.eq(BaseTenant::getStatus, status);
        qw.orderByDesc(BaseTenant::getId);
        Page<BaseTenant> result = tenantService.page(new Page<>(page, pageSize), qw);
        bizDisplayService.fill(result.getRecords(),
                BaseTenant::getOwnerId, BaseTenant::getHouseId,
                BaseTenant::setOwnerName, BaseTenant::setHouseNo);
        return Result.success(result);
    }

    @Operation(summary = "租户详情")
    @GetMapping("/{id}")
    public Result<BaseTenant> getById(@PathVariable Long id) {
        return Result.success(tenantService.getById(id));
    }

    @Operation(summary = "新增租户")
    @PostMapping
    public Result<Void> create(@RequestBody BaseTenant tenant) {
        tenantService.save(tenant);
        return Result.success();
    }

    @Operation(summary = "修改租户")
    @PutMapping
    public Result<Void> update(@RequestBody BaseTenant tenant) {
        tenantService.updateById(tenant);
        return Result.success();
    }

    @Operation(summary = "删除租户")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        tenantService.removeById(id);
        return Result.success();
    }
}
