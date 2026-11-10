package com.smartcommunity.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartcommunity.common.result.Result;
import com.smartcommunity.entity.BaseDecoration;
import com.smartcommunity.service.BaseDecorationService;
import com.smartcommunity.service.BizDisplayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Tag(name = "装修管理")
@RestController
@RequestMapping("/api/v1/decoration")
@RequiredArgsConstructor
public class DecorationController {

    private final BaseDecorationService decorationService;
    private final BizDisplayService bizDisplayService;

    @Operation(summary = "装修申请列表")
    @GetMapping("/list")
    public Result<Page<BaseDecoration>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long ownerId,
            @RequestParam(required = false) Long houseId,
            @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<BaseDecoration> qw = new LambdaQueryWrapper<>();
        if (ownerId != null) qw.eq(BaseDecoration::getOwnerId, ownerId);
        if (houseId != null) qw.eq(BaseDecoration::getHouseId, houseId);
        if (status != null) qw.eq(BaseDecoration::getStatus, status);
        qw.orderByDesc(BaseDecoration::getId);
        Page<BaseDecoration> result = decorationService.page(new Page<>(page, pageSize), qw);
        bizDisplayService.fill(result.getRecords(),
                BaseDecoration::getOwnerId, BaseDecoration::getHouseId,
                BaseDecoration::setOwnerName, BaseDecoration::setHouseNo);
        return Result.success(result);
    }

    @Operation(summary = "装修详情")
    @GetMapping("/{id}")
    public Result<BaseDecoration> getById(@PathVariable Long id) {
        BaseDecoration decoration = decorationService.getById(id);
        if (decoration != null) {
            bizDisplayService.fill(java.util.List.of(decoration),
                    BaseDecoration::getOwnerId, BaseDecoration::getHouseId,
                    BaseDecoration::setOwnerName, BaseDecoration::setHouseNo);
        }
        return Result.success(decoration);
    }

    @Operation(summary = "新增装修申请")
    @PostMapping
    public Result<Void> create(@RequestBody BaseDecoration decoration) {
        if (decoration.getDecorationNo() == null || decoration.getDecorationNo().isBlank()) {
            decoration.setDecorationNo("DEC" + UUID.randomUUID().toString().replace("-", "").substring(0, 24));
        }
        decorationService.save(decoration);
        return Result.success();
    }

    @Operation(summary = "修改装修申请")
    @PutMapping
    public Result<Void> update(@RequestBody BaseDecoration decoration) {
        decorationService.updateById(decoration);
        return Result.success();
    }

    @Operation(summary = "审批装修申请")
    @PutMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Object statusObj = body.get("status");
        if (statusObj == null) {
            return Result.fail("请指定审批状态");
        }
        Integer status;
        try {
            status = Integer.valueOf(statusObj.toString());
        } catch (NumberFormatException e) {
            return Result.fail("审批状态无效");
        }
        String checkResult = body.get("checkResult") != null ? body.get("checkResult").toString() : null;
        decorationService.approve(id, status, checkResult);
        return Result.success();
    }

    @Operation(summary = "删除装修申请")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        decorationService.removeById(id);
        return Result.success();
    }
}
