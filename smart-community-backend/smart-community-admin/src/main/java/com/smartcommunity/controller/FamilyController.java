package com.smartcommunity.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartcommunity.common.result.Result;
import com.smartcommunity.entity.BaseFamily;
import com.smartcommunity.service.BaseFamilyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "家属管理")
@RestController
@RequestMapping("/api/v1/family")
@RequiredArgsConstructor
public class FamilyController {

    private final BaseFamilyService familyService;

    @Operation(summary = "按业主查家属")
    @GetMapping("/owner/{ownerId}")
    public Result<List<BaseFamily>> getByOwnerId(@PathVariable Long ownerId) {
        return Result.success(familyService.getByOwnerId(ownerId));
    }

    @Operation(summary = "家属详情")
    @GetMapping("/{id}")
    public Result<BaseFamily> getById(@PathVariable Long id) {
        return Result.success(familyService.getById(id));
    }

    @Operation(summary = "新增家属")
    @PostMapping
    public Result<Void> create(@RequestBody BaseFamily family) {
        familyService.save(family);
        return Result.success();
    }

    @Operation(summary = "修改家属")
    @PutMapping
    public Result<Void> update(@RequestBody BaseFamily family) {
        familyService.updateById(family);
        return Result.success();
    }

    @Operation(summary = "删除家属")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        familyService.removeById(id);
        return Result.success();
    }
}