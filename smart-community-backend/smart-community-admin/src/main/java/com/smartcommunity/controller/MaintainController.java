package com.smartcommunity.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartcommunity.common.result.Result;
import com.smartcommunity.common.exception.BusinessException;
import com.smartcommunity.service.PropEquipmentService;
import com.smartcommunity.entity.PropEquipmentMaintain;
import com.smartcommunity.service.PropEquipmentMaintainService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "设备维保管理")
@RestController
@RequestMapping("/api/v1/equipment/maintain")
@RequiredArgsConstructor
public class MaintainController {

    private final PropEquipmentMaintainService maintainService;
    private final PropEquipmentService equipmentService;

    @Operation(summary = "维保记录列表")
    @GetMapping("/list")
    public Result<Page<PropEquipmentMaintain>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long equipmentId,
            @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<PropEquipmentMaintain> qw = new LambdaQueryWrapper<>();
        if (equipmentId != null) qw.eq(PropEquipmentMaintain::getEquipmentId, equipmentId);
        if (status != null) qw.eq(PropEquipmentMaintain::getStatus, status);
        qw.orderByDesc(PropEquipmentMaintain::getId);
        return Result.success(maintainService.page(new Page<>(page, pageSize), qw));
    }

    @Operation(summary = "维保详情")
    @GetMapping("/{id}")
    public Result<PropEquipmentMaintain> getById(@PathVariable Long id) {
        return Result.success(maintainService.getById(id));
    }

    @Operation(summary = "创建维保记录")
    @PostMapping
    public Result<Void> create(@RequestBody PropEquipmentMaintain entity) {
        validate(entity);
        entity.setId(null);
        entity.setStatus(0);
        maintainService.save(entity);
        return Result.success();
    }

    @Operation(summary = "更新维保记录")
    @PutMapping
    public Result<Void> update(@RequestBody PropEquipmentMaintain entity) {
        if (entity.getId() == null || maintainService.getById(entity.getId()) == null) {
            throw new BusinessException(404, "维保记录不存在");
        }
        validate(entity);
        maintainService.updateById(entity);
        return Result.success();
    }

    @Operation(summary = "完成维保")
    @PutMapping("/{id}/complete")
    public Result<Void> complete(@PathVariable Long id) {
        PropEquipmentMaintain m = maintainService.getById(id);
        if (m == null) throw new BusinessException(404, "维保记录不存在");
        if (m != null) {
            m.setStatus(2);
            maintainService.updateById(m);
        }
        return Result.success();
    }

    @Operation(summary = "删除维保记录")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        maintainService.removeById(id);
        return Result.success();
    }
    private void validate(PropEquipmentMaintain entity) {
        if (entity.getEquipmentId() == null || equipmentService.getById(entity.getEquipmentId()) == null) {
            throw new BusinessException(400, "请选择有效设备");
        }
        if (entity.getMaintainContent() == null || entity.getMaintainContent().isBlank() || entity.getMaintainDate() == null) {
            throw new BusinessException(400, "请填写维保内容和日期");
        }
        if (entity.getCost() != null && entity.getCost().signum() < 0) {
            throw new BusinessException(400, "费用不能为负数");
        }
    }
}
