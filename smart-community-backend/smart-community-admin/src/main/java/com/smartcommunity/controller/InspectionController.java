package com.smartcommunity.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartcommunity.common.exception.BusinessException;
import com.smartcommunity.common.result.Result;
import com.smartcommunity.entity.PropInspection;
import com.smartcommunity.service.PropInspectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Tag(name = "巡检管理")
@RestController
@RequestMapping("/api/v1/inspection")
@RequiredArgsConstructor
public class InspectionController {

    private final PropInspectionService inspectionService;

    /** 计划：planId 为空；记录：planId 指向计划 */
    @GetMapping("/plan/list")
    public Result<Page<PropInspection>> planList(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "pageSize", defaultValue = "10") int pageSize) {
        LambdaQueryWrapper<PropInspection> qw = new LambdaQueryWrapper<>();
        qw.and(w -> w.isNull(PropInspection::getPlanId).or().eq(PropInspection::getPlanId, 0));
        qw.orderByDesc(PropInspection::getId);
        return Result.success(inspectionService.page(new Page<>(page, pageSize), qw));
    }

    @PostMapping("/plan")
    public Result<Void> createPlan(@RequestBody PropInspection plan) {
        plan.setPlanId(null);
        if (plan.getStatus() == null) {
            plan.setStatus(0); // 计划启用
        }
        if (plan.getInspectorId() == null) {
            plan.setInspectorId(1L);
        }
        if (plan.getInspectionDate() == null) {
            plan.setInspectionDate(LocalDate.now());
        }
        inspectionService.save(plan);
        return Result.success();
    }

    @PutMapping("/plan")
    public Result<Void> updatePlan(@RequestBody PropInspection plan) {
        if (plan.getId() == null) {
            return Result.fail("计划ID不能为空");
        }
        plan.setPlanId(null);
        inspectionService.updateById(plan);
        return Result.success();
    }

    @DeleteMapping("/plan/{id}")
    public Result<Void> deletePlan(@PathVariable Long id) {
        inspectionService.removeById(id);
        return Result.success();
    }

    @Operation(summary = "根据计划生成今日巡检任务")
    @PostMapping("/plan/generate-today")
    public Result<Integer> generateToday() {
        List<PropInspection> plans = inspectionService.list(new LambdaQueryWrapper<PropInspection>()
                .and(w -> w.isNull(PropInspection::getPlanId).or().eq(PropInspection::getPlanId, 0))
                .and(w -> w.isNull(PropInspection::getStatus).or().ne(PropInspection::getStatus, -1)));
        LocalDate today = LocalDate.now();
        int created = 0;
        for (PropInspection plan : plans) {
            long exists = inspectionService.count(new LambdaQueryWrapper<PropInspection>()
                    .eq(PropInspection::getPlanId, plan.getId())
                    .eq(PropInspection::getInspectionDate, today));
            if (exists > 0) {
                continue;
            }
            PropInspection record = new PropInspection();
            record.setPlanId(plan.getId());
            record.setInspectorId(plan.getInspectorId());
            record.setInspectorName(plan.getInspectorName());
            record.setInspectionDate(today);
            record.setArea(plan.getArea());
            record.setItems(plan.getItems());
            record.setStatus(0); // 待执行
            inspectionService.save(record);
            created++;
        }
        return Result.success(created);
    }

    @GetMapping("/list")
    public Result<Page<PropInspection>> list(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "pageSize", defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long inspectorId) {
        LambdaQueryWrapper<PropInspection> qw = new LambdaQueryWrapper<>();
        qw.isNotNull(PropInspection::getPlanId).ne(PropInspection::getPlanId, 0);
        if (inspectorId != null) qw.eq(PropInspection::getInspectorId, inspectorId);
        qw.orderByDesc(PropInspection::getInspectionDate);
        return Result.success(inspectionService.page(new Page<>(page, pageSize), qw));
    }

    @PostMapping("/record")
    public Result<Void> record(@RequestBody PropInspection inspection) {
        if (inspection.getPlanId() == null || inspection.getPlanId() <= 0) {
            // 无关联计划时允许创建执行记录，planId 标记为 -1 表示独立记录
            inspection.setPlanId(-1L);
        }
        if (inspection.getInspectionDate() == null) {
            inspection.setInspectionDate(LocalDate.now());
        }
        if (inspection.getStatus() == null) {
            inspection.setStatus(0);
        }
        inspectionService.save(inspection);
        return Result.success();
    }

    @PutMapping("/record")
    public Result<Void> updateRecord(@RequestBody PropInspection inspection) {
        inspectionService.updateById(inspection);
        return Result.success();
    }

    @DeleteMapping("/record/{id}")
    public Result<Void> deleteRecord(@PathVariable Long id) {
        inspectionService.removeById(id);
        return Result.success();
    }

    @GetMapping("/record/list")
    public Result<Page<PropInspection>> recordList(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "pageSize", defaultValue = "10") int pageSize) {
        LambdaQueryWrapper<PropInspection> qw = new LambdaQueryWrapper<>();
        qw.isNotNull(PropInspection::getPlanId).ne(PropInspection::getPlanId, 0);
        qw.orderByDesc(PropInspection::getInspectionDate).orderByDesc(PropInspection::getId);
        return Result.success(inspectionService.page(new Page<>(page, pageSize), qw));
    }

    @Operation(summary = "完成巡检记录")
    @PutMapping("/record/{id}/complete")
    public Result<Void> completeRecord(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        PropInspection record = inspectionService.getById(id);
        if (record == null) {
            throw new BusinessException(404, "巡检记录不存在");
        }
        if (record.getPlanId() == null || record.getPlanId() == 0) {
            throw new BusinessException("不能对计划执行完成操作");
        }
        if (body != null) {
            if (body.get("result") != null) record.setResult(String.valueOf(body.get("result")));
            if (body.get("anomalies") != null) record.setAnomalies(String.valueOf(body.get("anomalies")));
            if (body.get("inspectorName") != null) record.setInspectorName(String.valueOf(body.get("inspectorName")));
        }
        if (record.getResult() == null || record.getResult().isBlank()) {
            record.setResult("正常");
        }
        record.setStatus(1); // 已完成
        record.setCheckTime(LocalDateTime.now());
        inspectionService.updateById(record);
        return Result.success();
    }
}
