package com.smartcommunity.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartcommunity.common.result.Result;
import com.smartcommunity.entity.BaseUnit;
import com.smartcommunity.service.BaseUnitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "单元管理")
@RestController
@RequestMapping("/api/v1/unit")
@RequiredArgsConstructor
public class UnitController {

    private final BaseUnitService unitService;

    @Operation(summary = "单元列表")
    @GetMapping("/list")
    public Result<Page<BaseUnit>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long buildingId) {
        LambdaQueryWrapper<BaseUnit> qw = new LambdaQueryWrapper<>();
        if (buildingId != null) qw.eq(BaseUnit::getBuildingId, buildingId);
        qw.orderByAsc(BaseUnit::getUnitNo);
        return Result.success(unitService.page(new Page<>(page, pageSize), qw));
    }

    @Operation(summary = "全部单元")
    @GetMapping("/all")
    public Result<List<BaseUnit>> all(@RequestParam(required = false) Long buildingId) {
        LambdaQueryWrapper<BaseUnit> qw = new LambdaQueryWrapper<>();
        if (buildingId != null) qw.eq(BaseUnit::getBuildingId, buildingId);
        qw.orderByAsc(BaseUnit::getUnitNo);
        return Result.success(unitService.list(qw));
    }

    @Operation(summary = "新增单元")
    @PostMapping
    public Result<Void> create(@RequestBody BaseUnit unit) {
        unitService.save(unit);
        return Result.success();
    }

    @Operation(summary = "修改单元")
    @PutMapping
    public Result<Void> update(@RequestBody BaseUnit unit) {
        unitService.updateById(unit);
        return Result.success();
    }

    @Operation(summary = "删除单元")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        unitService.removeById(id);
        return Result.success();
    }
}