package com.smartcommunity.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartcommunity.common.result.Result;
import com.smartcommunity.entity.PropCleaning;
import com.smartcommunity.service.PropCleaningService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "保洁管理")
@RestController
@RequestMapping("/api/v1/cleaning")
@RequiredArgsConstructor
public class CleaningController {

    private final PropCleaningService cleaningService;

    @GetMapping("/list")
    public Result<Page<PropCleaning>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long cleanerId,
            @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<PropCleaning> qw = new LambdaQueryWrapper<>();
        if (cleanerId != null) qw.eq(PropCleaning::getCleanerId, cleanerId);
        if (status != null) qw.eq(PropCleaning::getStatus, status);
        qw.orderByDesc(PropCleaning::getTaskDate);
        return Result.success(cleaningService.page(new Page<>(page, pageSize), qw));
    }

    @PostMapping
    public Result<Void> create(@RequestBody PropCleaning cleaning) {
        cleaningService.save(cleaning);
        return Result.success();
    }

    @PutMapping
    public Result<Void> update(@RequestBody PropCleaning cleaning) {
        cleaningService.updateById(cleaning);
        return Result.success();
    }

    @PutMapping("/{id}/check")
    public Result<Void> check(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        PropCleaning cleaning = cleaningService.getById(id);
        if (cleaning != null) {
            if (body.get("score") != null) cleaning.setQualityScore(((Number) body.get("score")).intValue());
            cleaning.setStatus(2);
            cleaningService.updateById(cleaning);
        }
        return Result.success();
    }
}