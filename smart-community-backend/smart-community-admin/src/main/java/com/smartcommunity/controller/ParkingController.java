package com.smartcommunity.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartcommunity.common.result.Result;
import com.smartcommunity.entity.BaseParking;
import com.smartcommunity.service.BaseParkingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "车位管理")
@RestController
@RequestMapping("/api/v1/parking")
@RequiredArgsConstructor
public class ParkingController {

    private final BaseParkingService parkingService;

    @Operation(summary = "车位列表")
    @GetMapping("/list")
    public Result<Page<BaseParking>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String parkingNo,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer parkingType) {
        LambdaQueryWrapper<BaseParking> qw = new LambdaQueryWrapper<>();
        if (parkingNo != null) qw.like(BaseParking::getParkingNo, parkingNo);
        if (status != null) qw.eq(BaseParking::getStatus, status);
        if (parkingType != null) qw.eq(BaseParking::getParkingType, parkingType);
        qw.orderByAsc(BaseParking::getParkingNo);
        return Result.success(parkingService.page(new Page<>(page, pageSize), qw));
    }

    @Operation(summary = "新增车位")
    @PostMapping
    public Result<Void> create(@RequestBody BaseParking parking) {
        parkingService.save(parking);
        return Result.success();
    }

    @Operation(summary = "修改车位")
    @PutMapping
    public Result<Void> update(@RequestBody BaseParking parking) {
        parkingService.updateById(parking);
        return Result.success();
    }

    @Operation(summary = "删除车位")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        parkingService.removeById(id);
        return Result.success();
    }
}