package com.smartcommunity.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartcommunity.common.result.Result;
import com.smartcommunity.entity.LifeProduct;
import com.smartcommunity.service.LifeProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "生活服务-商品")
@RestController
@RequestMapping("/api/v1/product")
@RequiredArgsConstructor
public class LifeProductController {

    private final LifeProductService productService;

    @GetMapping("/list")
    public Result<Page<LifeProduct>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String name) {
        LambdaQueryWrapper<LifeProduct> qw = new LambdaQueryWrapper<>();
        if (name != null) qw.like(LifeProduct::getProductName, name);
        qw.orderByDesc(LifeProduct::getId);
        return Result.success(productService.page(new Page<>(page, pageSize), qw));
    }

    @GetMapping("/{id}")
    public Result<LifeProduct> getById(@PathVariable Long id) {
        return Result.success(productService.getById(id));
    }

    @PostMapping
    public Result<Void> create(@RequestBody LifeProduct entity) {
        productService.save(entity);
        return Result.success();
    }

    @PutMapping
    public Result<Void> update(@RequestBody LifeProduct entity) {
        productService.updateById(entity);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    public Result<Void> toggleStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        Integer status = body.get("status");
        LifeProduct product = productService.getById(id);
        if (product != null && status != null) {
            product.setStatus(status);
            productService.updateById(product);
        }
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        productService.removeById(id);
        return Result.success();
    }
}