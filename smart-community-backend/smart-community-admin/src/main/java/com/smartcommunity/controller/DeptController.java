package com.smartcommunity.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartcommunity.common.result.Result;
import com.smartcommunity.entity.SysDept;
import com.smartcommunity.service.SysDeptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "部门管理")
@RestController
@RequestMapping("/api/v1/dept")
@RequiredArgsConstructor
public class DeptController {

    private final SysDeptService deptService;

    @Operation(summary = "部门树")
    @GetMapping("/tree")
    public Result<List<SysDept>> tree() {
        return Result.success(deptService.getTree());
    }

    @Operation(summary = "全部部门")
    @GetMapping("/all")
    public Result<List<SysDept>> all() {
        return Result.success(deptService.list(new LambdaQueryWrapper<SysDept>().orderByAsc(SysDept::getSort)));
    }

    @Operation(summary = "部门详情")
    @GetMapping("/{id}")
    public Result<SysDept> getById(@PathVariable Long id) {
        return Result.success(deptService.getById(id));
    }

    @Operation(summary = "新增部门")
    @PostMapping
    public Result<Void> create(@RequestBody SysDept dept) {
        deptService.save(dept);
        return Result.success();
    }

    @Operation(summary = "修改部门")
    @PutMapping
    public Result<Void> update(@RequestBody SysDept dept) {
        deptService.updateById(dept);
        return Result.success();
    }

    @Operation(summary = "删除部门")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        deptService.removeById(id);
        return Result.success();
    }
}