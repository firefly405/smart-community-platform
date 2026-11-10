package com.smartcommunity.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartcommunity.common.result.Result;
import com.smartcommunity.entity.FeeItem;
import com.smartcommunity.service.FeeItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "费用项目管理")
@RestController
@RequestMapping("/api/v1/fee/item")
@RequiredArgsConstructor
public class FeeItemController {

    private final FeeItemService feeItemService;

    @Operation(summary = "费用项目列表")
    @GetMapping("/list")
    public Result<Page<FeeItem>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String itemName) {
        LambdaQueryWrapper<FeeItem> qw = new LambdaQueryWrapper<>();
        if (itemName != null) qw.like(FeeItem::getItemName, itemName);
        qw.orderByAsc(FeeItem::getItemCode);
        return Result.success(feeItemService.page(new Page<>(page, pageSize), qw));
    }

    @Operation(summary = "全部费用项目")
    @GetMapping("/all")
    public Result<List<FeeItem>> all() {
        return Result.success(feeItemService.list(new LambdaQueryWrapper<FeeItem>().orderByAsc(FeeItem::getItemCode)));
    }

    @Operation(summary = "费用项目详情")
    @GetMapping("/{id}")
    public Result<FeeItem> getById(@PathVariable Long id) {
        return Result.success(feeItemService.getById(id));
    }

    @Operation(summary = "新增费用项目")
    @PostMapping
    public Result<Void> create(@RequestBody FeeItem item) {
        feeItemService.save(item);
        return Result.success();
    }

    @Operation(summary = "修改费用项目")
    @PutMapping
    public Result<Void> update(@RequestBody FeeItem item) {
        feeItemService.updateById(item);
        return Result.success();
    }

    @Operation(summary = "删除费用项目")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        feeItemService.removeById(id);
        return Result.success();
    }
}