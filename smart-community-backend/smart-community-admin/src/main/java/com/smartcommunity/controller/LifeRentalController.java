package com.smartcommunity.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartcommunity.common.result.Result;
import com.smartcommunity.entity.LifeRental;
import com.smartcommunity.service.LifeRentalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "生活服务-房屋租售")
@RestController
@RequestMapping("/api/v1/rental")
@RequiredArgsConstructor
public class LifeRentalController {

    private final LifeRentalService rentalService;

    @GetMapping("/list")
    public Result<Page<LifeRental>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<LifeRental> qw = new LambdaQueryWrapper<>();
        if (title != null) qw.like(LifeRental::getTitle, title);
        if (status != null) qw.eq(LifeRental::getStatus, status);
        qw.orderByDesc(LifeRental::getId);
        return Result.success(rentalService.page(new Page<>(page, pageSize), qw));
    }

    @GetMapping("/{id}")
    public Result<LifeRental> getById(@PathVariable Long id) {
        return Result.success(rentalService.getById(id));
    }

    @PostMapping
    public Result<Void> create(@RequestBody LifeRental entity) {
        rentalService.save(entity);
        return Result.success();
    }

    @PutMapping
    public Result<Void> update(@RequestBody LifeRental entity) {
        rentalService.updateById(entity);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        rentalService.removeById(id);
        return Result.success();
    }
}