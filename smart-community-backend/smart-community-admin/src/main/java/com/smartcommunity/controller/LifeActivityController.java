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

@Tag(name = "生活服务-社区活动")
@RestController
@RequestMapping("/api/v1/activity")
@RequiredArgsConstructor
public class LifeActivityController {

    private final LifeActivityService activityService;
    private final LifeActivityJoinService joinService;

    @GetMapping("/list")
    public Result<Page<LifeActivity>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<LifeActivity> qw = new LambdaQueryWrapper<>();
        if (title != null) qw.like(LifeActivity::getTitle, title);
        if (status != null) qw.eq(LifeActivity::getStatus, status);
        qw.orderByDesc(LifeActivity::getId);
        return Result.success(activityService.page(new Page<>(page, pageSize), qw));
    }

    @GetMapping("/{id}")
    public Result<LifeActivity> getById(@PathVariable Long id) {
        return Result.success(activityService.getById(id));
    }

    @PostMapping
    public Result<Void> create(@RequestBody LifeActivity entity) {
        if (entity.getCurrentParticipants() == null) {
            entity.setCurrentParticipants(0);
        }
        activityService.save(entity);
        return Result.success();
    }

    @PutMapping
    public Result<Void> update(@RequestBody LifeActivity entity) {
        activityService.updateById(entity);
        return Result.success();
    }

    @Operation(summary = "活动报名")
    @PostMapping("/{id}/join")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> join(@PathVariable Long id, @RequestBody(required = false) LifeActivityJoin join) {
        LifeActivity activity = activityService.getById(id);
        if (activity == null) {
            throw new BusinessException(404, "活动不存在");
        }
        int current = activity.getCurrentParticipants() == null ? 0 : activity.getCurrentParticipants();
        Integer max = activity.getMaxParticipants();
        if (max != null && max > 0 && current >= max) {
            throw new BusinessException("报名人数已满");
        }
        if (join == null) {
            join = new LifeActivityJoin();
        }
        join.setId(null);
        join.setActivityId(id);
        if (join.getJoinTime() == null) {
            join.setJoinTime(LocalDateTime.now());
        }
        if (join.getStatus() == null) {
            join.setStatus(1);
        }
        if (join.getUserName() == null || join.getUserName().isBlank()) {
            join.setUserName(join.getUserId() != null ? ("用户" + join.getUserId()) : "访客");
        }
        // 简单防重复：同 userId 不可重复报名
        if (join.getUserId() != null) {
            long dup = joinService.count(new LambdaQueryWrapper<LifeActivityJoin>()
                    .eq(LifeActivityJoin::getActivityId, id)
                    .eq(LifeActivityJoin::getUserId, join.getUserId()));
            if (dup > 0) {
                throw new BusinessException("您已报名该活动");
            }
        }
        joinService.save(join);
        activity.setCurrentParticipants(current + 1);
        activityService.updateById(activity);
        return Result.success();
    }

    @GetMapping("/{id}/joins")
    public Result<Page<LifeActivityJoin>> joins(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        LambdaQueryWrapper<LifeActivityJoin> qw = new LambdaQueryWrapper<>();
        qw.eq(LifeActivityJoin::getActivityId, id);
        qw.orderByDesc(LifeActivityJoin::getId);
        return Result.success(joinService.page(new Page<>(page, pageSize), qw));
    }

    @Operation(summary = "取消报名")
    @DeleteMapping("/join/{joinId}")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> cancelJoin(@PathVariable Long joinId) {
        LifeActivityJoin join = joinService.getById(joinId);
        if (join == null) {
            throw new BusinessException(404, "报名记录不存在");
        }
        joinService.removeById(joinId);
        LifeActivity activity = activityService.getById(join.getActivityId());
        if (activity != null) {
            int current = activity.getCurrentParticipants() == null ? 0 : activity.getCurrentParticipants();
            activity.setCurrentParticipants(Math.max(0, current - 1));
            activityService.updateById(activity);
        }
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        activityService.removeById(id);
        return Result.success();
    }
}
