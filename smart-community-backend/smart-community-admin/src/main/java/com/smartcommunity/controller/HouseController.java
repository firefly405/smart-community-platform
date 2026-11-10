package com.smartcommunity.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartcommunity.common.result.Result;
import com.smartcommunity.entity.BaseHouse;
import com.smartcommunity.service.BaseHouseService;
import com.smartcommunity.service.BizDisplayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "房屋管理")
@RestController
@RequestMapping("/api/v1/house")
@RequiredArgsConstructor
public class HouseController {

    private final BaseHouseService houseService;
    private final BizDisplayService bizDisplayService;

    @Operation(summary = "房屋列表")
    @GetMapping("/list")
    public Result<Page<BaseHouse>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long buildingId,
            @RequestParam(required = false) Long unitId,
            @RequestParam(required = false) Long ownerId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String houseNo) {
        LambdaQueryWrapper<BaseHouse> qw = new LambdaQueryWrapper<>();
        if (buildingId != null) qw.eq(BaseHouse::getBuildingId, buildingId);
        if (unitId != null) qw.eq(BaseHouse::getUnitId, unitId);
        if (ownerId != null) qw.eq(BaseHouse::getOwnerId, ownerId);
        if (status != null) qw.eq(BaseHouse::getStatus, status);
        if (houseNo != null) qw.like(BaseHouse::getHouseNo, houseNo);
        qw.orderByAsc(BaseHouse::getBuildingId, BaseHouse::getUnitId, BaseHouse::getHouseNo);
        Page<BaseHouse> result = houseService.page(new Page<>(page, pageSize), qw);
        bizDisplayService.fillHouses(result.getRecords());
        return Result.success(result);
    }

    @Operation(summary = "房屋详情")
    @GetMapping("/{id}")
    public Result<BaseHouse> getById(@PathVariable Long id) {
        BaseHouse house = houseService.getById(id);
        if (house != null) {
            bizDisplayService.fillHouses(java.util.List.of(house));
        }
        return Result.success(house);
    }

    @Operation(summary = "新增房屋")
    @PostMapping
    public Result<Void> create(@RequestBody BaseHouse house) {
        houseService.save(house);
        return Result.success();
    }

    @Operation(summary = "修改房屋")
    @PutMapping
    public Result<Void> update(@RequestBody BaseHouse house) {
        houseService.updateById(house);
        return Result.success();
    }

    @Operation(summary = "删除房屋")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        houseService.removeById(id);
        return Result.success();
    }

    @Operation(summary = "按楼栋查房屋")
    @GetMapping("/building/{buildingId}")
    public Result<List<BaseHouse>> getByBuildingId(@PathVariable Long buildingId) {
        return Result.success(houseService.getByBuildingId(buildingId));
    }

    @Operation(summary = "批量导入房屋(JSON)")
    @PostMapping("/import")
    public Result<Map<String, Object>> importHouses(@RequestBody List<BaseHouse> houses) {
        int success = 0;
        int fail = 0;
        for (BaseHouse house : houses) {
            try {
                houseService.save(house);
                success++;
            } catch (Exception e) {
                fail++;
            }
        }
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("total", houses.size());
        result.put("success", success);
        result.put("fail", fail);
        return Result.success(result);
    }

    @Operation(summary = "导出全部房屋")
    @GetMapping("/export")
    public Result<List<BaseHouse>> exportHouses() {
        return Result.success(houseService.list());
    }
}
