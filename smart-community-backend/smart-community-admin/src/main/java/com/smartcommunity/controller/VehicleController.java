package com.smartcommunity.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartcommunity.common.result.Result;
import com.smartcommunity.entity.BaseParking;
import com.smartcommunity.entity.BaseVehicle;
import com.smartcommunity.service.BaseParkingService;
import com.smartcommunity.service.BaseVehicleService;
import com.smartcommunity.service.BizDisplayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Tag(name = "车辆管理")
@RestController
@RequestMapping("/api/v1/vehicle")
@RequiredArgsConstructor
public class VehicleController {

    private final BaseVehicleService vehicleService;
    private final BaseParkingService parkingService;
    private final BizDisplayService bizDisplayService;

    @Operation(summary = "车辆列表")
    @GetMapping("/list")
    public Result<Page<BaseVehicle>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String plateNo,
            @RequestParam(required = false) Long ownerId) {
        LambdaQueryWrapper<BaseVehicle> qw = new LambdaQueryWrapper<>();
        if (plateNo != null) qw.like(BaseVehicle::getPlateNo, plateNo);
        if (ownerId != null) qw.eq(BaseVehicle::getOwnerId, ownerId);
        qw.orderByDesc(BaseVehicle::getId);
        Page<BaseVehicle> result = vehicleService.page(new Page<>(page, pageSize), qw);
        bizDisplayService.fill(result.getRecords(),
                BaseVehicle::getOwnerId, v -> null,
                BaseVehicle::setOwnerName, (v, n) -> {});
        fillParkingNo(result.getRecords());
        return Result.success(result);
    }

    @Operation(summary = "车辆详情")
    @GetMapping("/{id}")
    public Result<BaseVehicle> getById(@PathVariable Long id) {
        BaseVehicle vehicle = vehicleService.getById(id);
        if (vehicle != null) {
            fillParkingNo(List.of(vehicle));
        }
        return Result.success(vehicle);
    }

    @Operation(summary = "新增车辆")
    @PostMapping
    public Result<Void> create(@RequestBody BaseVehicle vehicle) {
        vehicleService.createWithParking(vehicle);
        return Result.success();
    }

    @Operation(summary = "修改车辆")
    @PutMapping
    public Result<Void> update(@RequestBody BaseVehicle vehicle) {
        vehicleService.updateWithParking(vehicle);
        return Result.success();
    }

    @Operation(summary = "删除车辆")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        vehicleService.deleteWithParking(id);
        return Result.success();
    }

    private void fillParkingNo(List<BaseVehicle> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        List<Long> parkingIds = records.stream()
                .map(BaseVehicle::getParkingId)
                .filter(Objects::nonNull)
                .filter(id -> id > 0)
                .distinct()
                .collect(Collectors.toList());
        if (parkingIds.isEmpty()) {
            return;
        }
        Map<Long, String> noMap = new HashMap<>();
        for (BaseParking p : parkingService.listByIds(parkingIds)) {
            noMap.put(p.getId(), p.getParkingNo());
        }
        for (BaseVehicle v : records) {
            if (v.getParkingId() != null) {
                v.setParkingNo(noMap.get(v.getParkingId()));
            }
        }
    }
}
