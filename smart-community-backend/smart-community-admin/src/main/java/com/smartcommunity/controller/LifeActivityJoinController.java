package com.smartcommunity.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartcommunity.common.exception.BusinessException;
import com.smartcommunity.common.result.Result;
import com.smartcommunity.entity.LifeActivity;
import com.smartcommunity.entity.LifeActivityJoin;
import com.smartcommunity.service.LifeActivityJoinService;
import com.smartcommunity.service.LifeActivityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Tag(name = "生活服务-活动报名")
@RestController
@RequestMapping("/api/v1/activity-join")
@RequiredArgsConstructor
public class LifeActivityJoinController {

    private final LifeActivityJoinService joinService;
    private final LifeActivityService activityService;

    @GetMapping("/list")
    public Result<Page<LifeActivityJoin>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long activityId) {
        LambdaQueryWrapper<LifeActivityJoin> qw = new LambdaQueryWrapper<>();
        if (activityId != null) qw.eq(LifeActivityJoin::getActivityId, activityId);
        qw.orderByDesc(LifeActivityJoin::getId);
        return Result.success(joinService.page(new Page<>(page, pageSize), qw));
    }

    @PostMapping
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> join(@RequestBody LifeActivityJoin entity) {
        if (entity.getActivityId() == null) {
            throw new BusinessException("活动ID不能为空");
        }
        LifeActivity activity = activityService.getById(entity.getActivityId());
        if (activity == null) {
            throw new BusinessException(404, "活动不存在");
        }
        int current = activity.getCurrentParticipants() == null ? 0 : activity.getCurrentParticipants();
        Integer max = activity.getMaxParticipants();
        if (max != null && max > 0 && current >= max) {
            throw new BusinessException("报名人数已满");
        }
        if (entity.getJoinTime() == null) {
            entity.setJoinTime(LocalDateTime.now());
        }
        if (entity.getStatus() == null) {
            entity.setStatus(1);
        }
        joinService.save(entity);
        activity.setCurrentParticipants(current + 1);
        activityService.updateById(activity);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> cancel(@PathVariable Long id) {
        LifeActivityJoin join = joinService.getById(id);
        if (join == null) {
            return Result.success();
        }
        joinService.removeById(id);
        LifeActivity activity = activityService.getById(join.getActivityId());
        if (activity != null) {
            int current = activity.getCurrentParticipants() == null ? 0 : activity.getCurrentParticipants();
            activity.setCurrentParticipants(Math.max(0, current - 1));
            activityService.updateById(activity);
        }
        return Result.success();
    }
}
