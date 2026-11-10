package com.smartcommunity.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartcommunity.common.result.Result;
import com.smartcommunity.entity.PropRepair;
import com.smartcommunity.service.BizDisplayService;
import com.smartcommunity.service.PropRepairService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Tag(name = "报修管理")
@RestController
@RequestMapping("/api/v1/repair")
@RequiredArgsConstructor
public class RepairController {

    private final PropRepairService repairService;
    private final BizDisplayService bizDisplayService;

    @Operation(summary = "报修列表")
    @GetMapping("/list")
    public Result<Page<PropRepair>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long houseId,
            @RequestParam(required = false) Long ownerId,
            @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<PropRepair> qw = new LambdaQueryWrapper<>();
        if (houseId != null) qw.eq(PropRepair::getHouseId, houseId);
        if (ownerId != null) qw.eq(PropRepair::getOwnerId, ownerId);
        if (status != null) qw.eq(PropRepair::getStatus, status);
        qw.orderByDesc(PropRepair::getId);
        Page<PropRepair> result = repairService.page(new Page<>(page, pageSize), qw);
        bizDisplayService.fill(result.getRecords(),
                PropRepair::getOwnerId, PropRepair::getHouseId,
                PropRepair::setOwnerName, PropRepair::setHouseNo);
        return Result.success(result);
    }

    @Operation(summary = "报修详情")
    @GetMapping("/{id}")
    public Result<PropRepair> getById(@PathVariable Long id) {
        PropRepair repair = repairService.getById(id);
        if (repair != null) {
            bizDisplayService.fill(java.util.List.of(repair),
                    PropRepair::getOwnerId, PropRepair::getHouseId,
                    PropRepair::setOwnerName, PropRepair::setHouseNo);
        }
        return Result.success(repair);
    }

    @Operation(summary = "创建报修")
    @PostMapping
    public Result<Void> create(@RequestBody PropRepair repair) {
        if (repair.getRepairNo() == null || repair.getRepairNo().isBlank()) {
            String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            int rnd = ThreadLocalRandom.current().nextInt(1000, 9999);
            repair.setRepairNo("RP" + ts + rnd);
        }
        if (repair.getStatus() == null) {
            repair.setStatus(0);
        }
        repairService.save(repair);
        return Result.success();
    }

    @Operation(summary = "派单")
    @PutMapping("/{id}/dispatch")
    public Result<Void> dispatch(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long handlerId = body.get("handlerId") != null ? ((Number) body.get("handlerId")).longValue() : null;
        String handlerName = (String) body.get("handlerName");
        repairService.dispatch(id, handlerId, handlerName);
        return Result.success();
    }

    @Operation(summary = "处理报修")
    @PutMapping("/{id}/process")
    public Result<Void> process(@PathVariable Long id, @RequestBody Map<String, String> body) {
        repairService.process(id, body.get("result"), body.get("images"));
        return Result.success();
    }

    @Operation(summary = "完成报修")
    @PutMapping("/{id}/complete")
    public Result<Void> complete(@PathVariable Long id) {
        repairService.complete(id);
        return Result.success();
    }

    @Operation(summary = "评价报修")
    @PutMapping("/{id}/evaluate")
    public Result<Void> evaluate(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Integer score = body.get("score") != null ? ((Number) body.get("score")).intValue() : null;
        String content = (String) body.get("content");
        repairService.evaluate(id, score, content);
        return Result.success();
    }

    @Operation(summary = "取消报修")
    @PutMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        repairService.cancel(id);
        return Result.success();
    }
}
