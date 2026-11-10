package com.smartcommunity.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartcommunity.common.result.Result;
import com.smartcommunity.entity.PropEquipment;
import com.smartcommunity.service.PropEquipmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "设备管理")
@RestController
@RequestMapping("/api/v1/equipment")
@RequiredArgsConstructor
public class EquipmentController {

    private final PropEquipmentService equipmentService;

    @GetMapping("/list")
    public Result<Page<PropEquipment>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String equipmentName,
            @RequestParam(required = false) Integer equipmentType) {
        LambdaQueryWrapper<PropEquipment> qw = new LambdaQueryWrapper<>();
        if (equipmentName != null) qw.like(PropEquipment::getEquipmentName, equipmentName);
        if (equipmentType != null) qw.eq(PropEquipment::getEquipmentType, equipmentType);
        qw.orderByDesc(PropEquipment::getId);
        return Result.success(equipmentService.page(new Page<>(page, pageSize), qw));
    }

    @GetMapping("/{id}")
    public Result<PropEquipment> getById(@PathVariable Long id) {
        return Result.success(equipmentService.getById(id));
    }

    @PostMapping
    public Result<Void> create(@RequestBody PropEquipment equipment) {
        equipmentService.save(equipment);
        return Result.success();
    }

    @PutMapping
    public Result<Void> update(@RequestBody PropEquipment equipment) {
        equipmentService.updateById(equipment);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        equipmentService.removeById(id);
        return Result.success();
    }
}