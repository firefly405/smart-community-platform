package com.smartcommunity.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartcommunity.common.result.Result;
import com.smartcommunity.entity.LifeService;
import com.smartcommunity.service.LifeServiceSrv;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "生活服务-便民服务")
@RestController
@RequestMapping("/api/v1/service")
@RequiredArgsConstructor
public class LifeServiceController {

    private final LifeServiceSrv serviceSrv;

    @GetMapping("/list")
    public Result<Page<LifeService>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String category) {
        LambdaQueryWrapper<LifeService> qw = new LambdaQueryWrapper<>();
        if (name != null) qw.like(LifeService::getServiceName, name);
        if (category != null) qw.eq(LifeService::getServiceType, Integer.parseInt(category));
        qw.orderByDesc(LifeService::getId);
        return Result.success(serviceSrv.page(new Page<>(page, pageSize), qw));
    }

    @GetMapping("/{id}")
    public Result<LifeService> getById(@PathVariable Long id) {
        return Result.success(serviceSrv.getById(id));
    }

    @PostMapping
    public Result<Void> create(@RequestBody LifeService entity) {
        serviceSrv.save(entity);
        return Result.success();
    }

    @PutMapping
    public Result<Void> update(@RequestBody LifeService entity) {
        serviceSrv.updateById(entity);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        serviceSrv.removeById(id);
        return Result.success();
    }
}