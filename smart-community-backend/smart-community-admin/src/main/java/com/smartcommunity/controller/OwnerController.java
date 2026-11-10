package com.smartcommunity.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartcommunity.common.result.Result;
import com.smartcommunity.entity.BaseOwner;
import com.smartcommunity.service.BaseOwnerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "业主管理")
@RestController
@RequestMapping("/api/v1/owner")
@RequiredArgsConstructor
public class OwnerController {

    private final BaseOwnerService ownerService;

    @Operation(summary = "业主列表")
    @GetMapping("/list")
    public Result<Page<BaseOwner>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) String realName,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<BaseOwner> qw = new LambdaQueryWrapper<>();
        if (id != null) qw.eq(BaseOwner::getId, id);
        if (realName != null) qw.like(BaseOwner::getRealName, realName);
        if (phone != null) qw.like(BaseOwner::getPhone, phone);
        if (status != null) qw.eq(BaseOwner::getStatus, status);
        qw.orderByDesc(BaseOwner::getId);
        return Result.success(ownerService.page(new Page<>(page, pageSize), qw));
    }

    @Operation(summary = "业主详情")
    @GetMapping("/{id}")
    public Result<BaseOwner> getById(@PathVariable Long id) {
        return Result.success(ownerService.getById(id));
    }

    @Operation(summary = "新增业主")
    @PostMapping
    public Result<Void> create(@RequestBody BaseOwner owner) {
        ownerService.save(owner);
        return Result.success();
    }

    @Operation(summary = "修改业主")
    @PutMapping
    public Result<Void> update(@RequestBody BaseOwner owner) {
        ownerService.updateById(owner);
        return Result.success();
    }

    @Operation(summary = "删除业主")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        ownerService.removeById(id);
        return Result.success();
    }

    @Operation(summary = "获取业主房屋ID列表")
    @GetMapping("/{id}/houses")
    public Result<List<Long>> getHouseIds(@PathVariable Long id) {
        return Result.success(ownerService.getHouseIds(id));
    }

    @Operation(summary = "绑定业主房屋")
    @PutMapping("/{id}/houses")
    public Result<Void> bindHouses(@PathVariable Long id, @RequestBody Map<String, List<Long>> body) {
        ownerService.bindHouses(id, body.get("houseIds"));
        return Result.success();
    }
}
