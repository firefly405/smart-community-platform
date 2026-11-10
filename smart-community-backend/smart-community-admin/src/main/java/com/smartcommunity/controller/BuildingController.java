package com.smartcommunity.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartcommunity.common.result.Result;
import com.smartcommunity.entity.BaseBuilding;
import com.smartcommunity.service.BaseBuildingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "楼栋管理")
@RestController
@RequestMapping("/api/v1/building")
@RequiredArgsConstructor
public class BuildingController {

    private final BaseBuildingService buildingService;

    @Operation(summary = "楼栋列表")
    @GetMapping("/list")
    public Result<Page<BaseBuilding>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String buildingName) {
        LambdaQueryWrapper<BaseBuilding> qw = new LambdaQueryWrapper<>();
        if (buildingName != null) qw.like(BaseBuilding::getBuildingName, buildingName);
        qw.orderByAsc(BaseBuilding::getBuildingNo);
        return Result.success(buildingService.page(new Page<>(page, pageSize), qw));
    }

    @Operation(summary = "全部楼栋")
    @GetMapping("/all")
    public Result<List<BaseBuilding>> all() {
        return Result.success(buildingService.list(new LambdaQueryWrapper<BaseBuilding>().orderByAsc(BaseBuilding::getBuildingNo)));
    }

    @Operation(summary = "楼栋详情")
    @GetMapping("/{id}")
    public Result<BaseBuilding> getById(@PathVariable Long id) {
        return Result.success(buildingService.getById(id));
    }

    @Operation(summary = "新增楼栋")
    @PostMapping
    public Result<Void> create(@RequestBody BaseBuilding building) {
        buildingService.save(building);
        return Result.success();
    }

    @Operation(summary = "修改楼栋")
    @PutMapping
    public Result<Void> update(@RequestBody BaseBuilding building) {
        buildingService.updateById(building);
        return Result.success();
    }

    @Operation(summary = "删除楼栋")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        buildingService.removeById(id);
        return Result.success();
    }
}